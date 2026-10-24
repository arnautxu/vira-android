package cat.vira.android.features.progress

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ViraChallenges
import cat.vira.android.features.progress.logic.ViraDays
import cat.vira.android.features.progress.logic.ViraProgress
import cat.vira.android.features.progress.logic.ViraRewardOnce
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
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/// Marcar, desmarcar i tornar a marcar no torna a pagar (`ViraRewardOnceTests` de l'iPhone).
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraRewardOnceTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val now = LocalDate.of(2026, 1, 15).atTime(10, 0).atZone(zone).toInstant()
    private val preferences = ApplicationProvider.getApplicationContext<Application>()
        .getSharedPreferences("ViraRewardOnceTest", 0).also { it.edit().clear().commit() }
    private val once = ViraRewardOnce(preferences)
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())

    @After fun close() = db.close()

    @Test
    fun laMateixaClauNomesEsPremiaUnCop() {
        assertTrue(once.claim("plan-1", now))
        assertFalse(once.claim("plan-1", now))
        assertTrue(once.wasRewarded("plan-1"))
        assertTrue(once.claim("plan-2", now))
    }

    @Test
    fun unHabitEsPremiaUnCopPerDia() {
        val habit = UUID.randomUUID()
        val today = LocalDate.of(2026, 1, 15)
        assertTrue(once.claim(ViraRewardOnce.habitKey(habit, today, zone), now))
        assertFalse(once.claim(ViraRewardOnce.habitKey(habit, today, zone), now))
        assertTrue(once.claim(ViraRewardOnce.habitKey(habit, today.plusDays(1), zone), now))
    }

    @Test
    fun alCapDeSeixantaDiesLaClauSOblida() {
        once.claim("plan-1", now)
        once.claim("plan-2", now.plus(Duration.ofDays(61)))
        assertFalse(once.wasRewarded("plan-1"))
        assertTrue(once.wasRewarded("plan-2"))
    }

    @Test
    fun elPrefixEsDeVira() {
        assertTrue(ViraDataEraser.ownPrefixes.any { ViraRewardOnce.KEY.startsWith(it) })
    }

    @Test
    fun tornarAMarcarNoFaAvancarElsReptesNiPagaDuesVegades() = runBlocking {
        val store = ViraStore(db, clock = { now }, zone = { zone })
        val progress = ViraProgress(store, clock = { now }, zone = { zone })
        val key = ViraRewardOnce.habitKey(UUID.randomUUID(), LocalDate.of(2026, 1, 15), zone)
        val daily = ChallengeItem.catalog.first { it.key == "daily_habits" }

        val first = progress.recordOnce(CoinReason.HABIT, key, once, now)
        val again = progress.recordOnce(CoinReason.HABIT, key, once, now)
        val third = progress.recordOnce(CoinReason.HABIT, key, once, now)

        assertNotNull(first)
        assertNull(again)
        assertNull(third)
        val ledger = db.coinTransactions().all()
        assertEquals(1, ViraChallenges.progress(daily, ledger, now, ViraDays(zone, DayOfWeek.MONDAY)))
        assertEquals(first, db.progressCounters().all().single().coinBalance)
    }
}
