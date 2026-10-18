package cat.vira.android.features.focus

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneOffset

/// La sessió amb la base de dades de debò (`FocusBreakSuggestionTests` de l'iPhone): el descans que
/// es proposa és el del mode actiu, i el rellotge del telèfon és qui decideix el final.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FocusSessionControllerTest {

    private var now = Instant.parse("2026-10-18T10:00:00Z")
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db, clock = { now })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After fun close() {
        scope.cancel()
        db.close()
    }

    private fun controller() = FocusSessionController(store, scope, clock = { now }, zone = { ZoneOffset.UTC })

    @Test fun unModePersonalitzatProposaElSeuDescans() = runBlocking {
        val profile = FocusProfile(name = "Estudi llarg", focusMinutes = 30, breakMinutes = 15)
        store.create(db.focusProfiles(), profile)

        val interrupted = controller()
        interrupted.select(FocusMode.of(profile))
        interrupted.start()
        interrupted.stop()
        assertNull(interrupted.completion?.suggestedBreak)
        assertTrue(interrupted.completion!!.wasInterrupted)

        val done = controller()
        done.select(FocusMode.of(profile), seconds = 1)
        done.start()
        done.reconcile(now.plusSeconds(60))
        assertEquals(FocusKind.SHORT_BREAK, done.completion?.suggestedBreak)
        assertEquals(15, done.completion?.suggestedBreakMinutes)
    }

    @Test fun canviarElTempsNoPerdElMode() {
        val profile = FocusProfile(name = "Estudi llarg", breakMinutes = 15)
        val controller = controller()
        controller.select(FocusMode.of(profile))
        controller.select(FocusKind.FOCUS, 10)
        assertEquals(profile.id, controller.mode.profileID)
        assertEquals(15, controller.mode.breakMinutes(FocusKind.SHORT_BREAK))
    }

    @Test fun laQuartaSessioDelDiaProposaUnDescansLlarg() = runBlocking {
        repeat(3) {
            db.focusSessions().upsertRaw(listOf(FocusSession(startedAt = now.minusSeconds(3600L * (it + 1)), completed = true)))
        }
        val controller = controller()
        controller.select(FocusKind.FOCUS, 25 * 60)
        controller.start()
        controller.reconcile(now.plusSeconds(25 * 60L))
        assertEquals(FocusKind.LONG_BREAK, controller.completion?.suggestedBreak)
        assertEquals(15, controller.completion?.suggestedBreakMinutes)
    }

    @Test fun abansDelFinalReconciliarNoFaRes() = runBlocking {
        val controller = controller()
        controller.start()
        controller.reconcile(now.plusSeconds(60))
        assertEquals(FocusSessionController.Phase.RUNNING, controller.phase)
        assertNull(controller.completion)
    }

    @Test fun enPausaElTempsNoSEsgota() = runBlocking {
        val controller = controller()
        controller.start()
        now = now.plusSeconds(60)
        controller.pause()
        controller.reconcile(now.plusSeconds(3600))
        assertEquals(FocusSessionController.Phase.RUNNING, controller.phase)
    }

    @Test fun acabarAMitgesDesaLaSessioNoCompletada() = runBlocking {
        val controller = controller()
        controller.start()
        now = now.plusSeconds(5 * 60)
        controller.stop()
        val session = db.focusSessions().all().single()
        assertFalse(session.completed)
        assertEquals(5 * 60, session.actualSeconds)
        assertNotNull(session.endedAt)
        assertEquals(5, controller.completion?.minutesDone)
        assertEquals(0, controller.completion?.coinsEarned)
    }

    @Test fun elDescansProposatEsUnaSessioNova() = runBlocking {
        val controller = controller()
        controller.select(FocusKind.FOCUS, 25 * 60)
        controller.start()
        now = now.plusSeconds(25 * 60L)
        controller.reconcile()
        controller.startSuggestedBreak()
        assertEquals(FocusSessionController.Phase.RUNNING, controller.phase)
        assertEquals(FocusKind.SHORT_BREAK, controller.state.kind)
        assertEquals(5 * 60, controller.state.plannedSeconds)
        assertEquals(2, db.focusSessions().all().size)
    }
}
