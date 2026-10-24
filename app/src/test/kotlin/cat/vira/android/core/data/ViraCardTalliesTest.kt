package cat.vira.android.core.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.SkinOwnership
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/// `ViraCardTalliesTests` de l'iPhone: els recomptes només pugen, es refan amb les dades del telèfon
/// i els reptes d'emocions no hi compten.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraCardTalliesTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("tallies-test", Context.MODE_PRIVATE).also { it.edit().clear().commit() }
    private val tallies = ViraCardTallies(prefs)
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())

    @After fun close() = db.close()

    private fun day(offset: Long) = LocalDate.of(2026, 10, 23).plusDays(offset).atStartOfDay(ZoneOffset.UTC).toInstant()

    @Test
    fun pujarLoPerSotaDelQueJaHiHaNoElFaBaixar() {
        tallies.add(5, ViraCardTallies.Kind.TASKS_DONE)
        assertFalse(tallies.raise(ViraCardTallies.Kind.TASKS_DONE, 3))
        assertEquals(5, tallies.value(ViraCardTallies.Kind.TASKS_DONE))
        assertTrue(tallies.raise(ViraCardTallies.Kind.TASKS_DONE, 8))
        assertEquals(8, tallies.value(ViraCardTallies.Kind.TASKS_DONE))
    }

    @Test
    fun tornarComptaAPartirDe7DiesSenseQueElDiaCompti() {
        val utc = ZoneOffset.UTC
        assertEquals("6 dies entremig", 0, ViraCardTallies.comebacks(listOf(day(-10), day(-3)), utc))
        assertEquals("7 dies entremig", 1, ViraCardTallies.comebacks(listOf(day(-10), day(-2)), utc))
        assertEquals(2, ViraCardTallies.comebacks(listOf(day(-30), day(-20), day(-19), day(-5)), utc))
        assertEquals(0, ViraCardTallies.comebacks(emptyList(), utc))
    }

    @Test
    fun laReparacioRefaElsRecomptesINoComptaElsReptesDEmocions() = runBlocking {
        val done = PlanItem(title = "Deures", statusRaw = PlanItemStatus.DONE.raw)
        db.planItems().upsertRaw(listOf(done, PlanItem(title = "Encara no")))
        db.focusSessions().upsertRaw(listOf(FocusSession(plannedSeconds = 1_500, actualSeconds = 1_500, completed = true)))
        db.challengeClaims().upsertRaw(listOf(ChallengeClaim(key = "daily_emotion"), ChallengeClaim(key = "daily_habit")))
        db.skinOwnerships().upsertRaw(listOf(SkinOwnership(key = "pirate")))

        assertTrue(tallies.repair(db, ZoneOffset.UTC))
        assertEquals(1, tallies.value(ViraCardTallies.Kind.TASKS_DONE))
        assertEquals(1_500, tallies.value(ViraCardTallies.Kind.FOCUS_SECONDS))
        assertEquals(1, tallies.value(ViraCardTallies.Kind.CHALLENGES_DONE))
        assertEquals(1, tallies.value(ViraCardTallies.Kind.SKINS_OWNED))

        db.planItems().deleteAll()
        assertFalse("esborrar la tasca feta no fa baixar res", tallies.repair(db, ZoneOffset.UTC))
        assertEquals(1, tallies.value(ViraCardTallies.Kind.TASKS_DONE))
    }

    @Test
    fun canviarDeCompteElsEsborraAmbLaRestaDePreferenciesDeLesCartes() {
        assertTrue(ViraCardTallies.Kind.entries.all { kind -> ViraDataEraser.ownPrefixes.any { kind.key.startsWith(it) } })
    }
}
