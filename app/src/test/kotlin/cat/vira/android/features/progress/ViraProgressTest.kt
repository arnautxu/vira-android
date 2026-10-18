package cat.vira.android.features.progress

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.Streak
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ChallengePeriod
import cat.vira.android.features.progress.logic.StreakRules.DayRequirement
import cat.vira.android.features.progress.logic.ViraChallenges
import cat.vira.android.features.progress.logic.ViraDays
import cat.vira.android.features.progress.logic.ViraProgress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Les tres condicions del dia i la compra de dies de `ViraStreakTests`, i `ViraChallengesTests`.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraProgressTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val days = ViraDays(zone, DayOfWeek.MONDAY)
    private val day0 = LocalDate.of(2026, 1, 15)
    private fun day(offset: Long): Instant = day0.plusDays(offset).atTime(10, 0).atZone(zone).toInstant()

    private var now = day(0)
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db, clock = { now }, zone = { zone })
    private val activities = mutableListOf<Instant>()
    private val milestones = mutableListOf<Int>()
    private val progress = ViraProgress(
        store, clock = { now }, zone = { zone },
        onActivity = { activities += it }, onMilestone = { milestones += it },
    )

    @After fun close() = db.close()

    /// Deixa el dia amb les condicions que se li demanin, i cap més.
    private suspend fun fulfil(vararg requirements: DayRequirement, at: Instant = now) {
        if (DayRequirement.HABIT in requirements) {
            val habit = store.create(db.habits(), Habit(title = "Beure aigua"))
            store.create(db.habitLogs(), HabitLog(habitId = habit.id, day = days.startOfDay(at), completedAt = at))
        }
        if (DayRequirement.EMOTION in requirements) store.create(db.emotionEntries(), EmotionEntry(createdAt = at))
        if (DayRequirement.TASK in requirements) {
            store.create(db.planItems(), PlanItem(title = "Anglès", day = at, completedAt = at))
        }
    }

    @Test fun ambDuesDeLesTresElDiaEncaraNoCompta() = runBlocking {
        fulfil(DayRequirement.HABIT, DayRequirement.TASK)
        progress.record(CoinReason.HABIT)
        assertEquals(0, store.streak().currentDays)
        assertEquals(setOf(DayRequirement.EMOTION), progress.missingRequirements())
    }

    @Test fun ambLesTresElDiaComptaIDonaMonedes() = runBlocking {
        fulfil(DayRequirement.HABIT, DayRequirement.EMOTION, DayRequirement.TASK)
        assertEquals(10, progress.record(CoinReason.EMOTION))
        assertEquals(1, store.streak().currentDays)
        assertEquals(1, store.counter().daysActiveCount)
        assertTrue(progress.missingRequirements().isEmpty())
        assertEquals(listOf(now), activities)
        // La segona acció del dia no compta un altre dia.
        progress.record(CoinReason.HABIT)
        assertEquals(1, store.counter().daysActiveCount)
    }

    @Test fun unaAccioSoltaDescongelaViriEncaraQueElDiaNoCompti() = runBlocking {
        store.save(db.streaks(), store.streak().copy(isFrozen = true))
        fulfil(DayRequirement.HABIT)
        progress.record(CoinReason.HABIT)
        assertFalse(store.streak().isFrozen)
        assertEquals(0, store.streak().currentDays)
    }

    @Test fun unDiaIncompletNoRestaRes() = runBlocking {
        progress.countDay(day(0))
        now = day(1)
        fulfil(DayRequirement.EMOTION)
        progress.record(CoinReason.EMOTION)
        assertEquals(1, store.streak().currentDays)
        assertEquals(1, store.streak().bestDays)
    }

    @Test fun laTascaComptaPelDiaQueEsMarca() = runBlocking {
        store.create(db.planItems(), PlanItem(title = "Trucar a l'institut", day = day(0), completedAt = day(1)))
        assertTrue(DayRequirement.TASK in progress.missingRequirements(day(0)))
        assertFalse(DayRequirement.TASK in progress.missingRequirements(day(1)))
    }

    @Test fun laSetenaJornadaAvisaDeLaFita() = runBlocking {
        for (i in 0L until 7L) {
            now = day(i)
            fulfil(DayRequirement.HABIT, DayRequirement.EMOTION, DayRequirement.TASK)
            progress.record(CoinReason.PLAN_ITEM)
        }
        assertEquals(7, store.streak().currentDays)
        assertEquals(listOf(7), milestones)
        assertEquals("week_streak", progress.takeStreakMilestone())
        assertEquals(null, progress.takeStreakMilestone())
    }

    @Test fun obrirLAppGuardaElDiaPerdut() = runBlocking {
        progress.countDay(day(0))
        now = day(2)
        progress.reconcileOnLaunch()
        assertTrue(store.streak().isFrozen)
        assertEquals(1, store.streak().currentDays)
        assertEquals(1, store.streak().freezesAvailable)
    }

    @Test fun obrirLAppSenseCanvisNoDataLaRatxa() = runBlocking {
        val before = store.streak()
        progress.reconcileOnLaunch()
        assertEquals("una ratxa intacta no ha de trepitjar la del servidor", before.updatedAt, store.streak().updatedAt)
    }

    @Test fun comprarUnDiaGasta200Monedes() = runBlocking {
        store.save(db.progressCounters(), store.counter().copy(coinBalance = 500))
        store.save(db.streaks(), store.streak().copy(freezesAvailable = 1))
        assertTrue(progress.buyFreeze())
        assertEquals(2, store.streak().freezesAvailable)
        assertEquals(300, store.counter().coinBalance)
    }

    @Test fun senseMonedesNoEsCompraRes() = runBlocking {
        store.save(db.progressCounters(), store.counter().copy(coinBalance = 10))
        store.save(db.streaks(), store.streak().copy(freezesAvailable = 0))
        assertFalse(progress.buyFreeze())
        assertEquals(0, store.streak().freezesAvailable)
        assertEquals(10, store.counter().coinBalance)
    }

    @Test fun alSostreNoEsCompraEncaraQueHiHagiMonedes() = runBlocking {
        store.save(db.progressCounters(), store.counter().copy(coinBalance = 1_000))
        store.save(db.streaks(), store.streak().copy(freezesAvailable = 4))
        assertFalse(progress.buyFreeze())
        assertEquals(1_000, store.counter().coinBalance)
    }

    @Test fun enCanviarDeMesElsDiesGratuïtsArribenAbansDeComprar() = runBlocking {
        // Pantalla oberta del mes passat: la compra no pot cobrar un dia que el mes nou ja regala.
        store.save(db.progressCounters(), store.counter().copy(coinBalance = 500))
        store.save(db.streaks(), store.streak().copy(freezesAvailable = 2, lastGrantMonth = store.streak().lastGrantMonth - 1))
        assertFalse(progress.buyFreeze())
        assertEquals(Streak.MAX_FREEZES, store.streak().freezesAvailable)
        assertEquals(500, store.counter().coinBalance)
    }

    @Test fun elFocusDescongelaSenseComptarElDia() = runBlocking {
        store.save(db.streaks(), store.streak().copy(isFrozen = true))
        val session = FocusSession(actualSeconds = ViraRewards.FOCUS_MINIMUM_SECONDS, completed = true)
        assertEquals(15, progress.recordFocus(session))
        assertFalse(store.streak().isFrozen)
        assertEquals(0, store.streak().currentDays)
        assertEquals(1, store.counter().focusSessionsCount)
    }

    // Reptes

    private fun item(key: String) = ChallengeItem.catalog.first { it.key == key }

    private suspend fun grant(reason: CoinReason, count: Int, at: Instant) = repeat(count) {
        store.create(db.coinTransactions(), CoinTransaction(amount = 5, reasonRaw = reason.raw, createdAt = at))
    }

    @Test fun unPremiPendentTePrioritatALaPortada() = runBlocking {
        grant(CoinReason.HABIT, 3, now)
        val featured = ViraChallenges.featuredDaily(db.coinTransactions().all(), db.challengeClaims().all(), now, days)
        assertEquals("daily_habits", featured?.key)
    }

    @Test fun elPremiEsDesaINoEsPotRecollirDuesVegades() = runBlocking {
        val challenge = item("daily_habits")
        grant(CoinReason.HABIT, 3, now)
        assertTrue(progress.claim(challenge))
        assertFalse(progress.claim(challenge))
        assertTrue(ViraChallenges.isClaimed(challenge, db.challengeClaims().all(), now, days))
        assertEquals(challenge.reward, store.counter().coinBalance)
        assertEquals(challenge.reward, store.counter().totalXP)
        assertEquals(listOf(now), db.challengeClaims().all().map { it.claimedAt })
    }

    @Test fun recollirAbansDHoraNoCreaRes() = runBlocking {
        grant(CoinReason.HABIT, 2, now)
        assertFalse(progress.claim(item("daily_habits")))
        assertTrue(db.challengeClaims().all().isEmpty())
        assertTrue(db.progressCounters().all().isEmpty())
    }

    @Test fun elProgresNomesComptaElCicleActualFinsAra() = runBlocking {
        val start = ViraChallenges.cycleStart(ChallengePeriod.DAILY, now, days)
        grant(CoinReason.HABIT, 1, start.minusSeconds(1))
        grant(CoinReason.HABIT, 1, start)
        grant(CoinReason.HABIT, 1, now)
        grant(CoinReason.HABIT, 1, now.plusSeconds(1))
        grant(CoinReason.CHALLENGE, 1, now)
        store.create(db.coinTransactions(), CoinTransaction(amount = -5, reasonRaw = CoinReason.HABIT.raw, createdAt = now))
        assertEquals(2, ViraChallenges.progress(item("daily_habits"), db.coinTransactions().all(), now, days))
    }

    @Test fun unDiaNouPermetUnPremiNou() = runBlocking {
        val challenge = item("daily_habits")
        grant(CoinReason.HABIT, 3, now)
        assertTrue(progress.claim(challenge))
        val tomorrow = now.plus(Duration.ofDays(1))
        assertFalse(ViraChallenges.isClaimed(challenge, db.challengeClaims().all(), tomorrow, days))
        assertEquals(0, ViraChallenges.progress(challenge, db.coinTransactions().all(), tomorrow, days))
        grant(CoinReason.HABIT, 3, tomorrow)
        assertTrue(progress.claim(challenge, tomorrow))
        assertEquals(2 * challenge.reward, store.counter().coinBalance)
    }

    @Test fun elRepteSetmanalComencaElDilluns() {
        // Dijous 15 de gener del 2026: la setmana va del dilluns 12.
        val start = ViraChallenges.cycleStart(ChallengePeriod.WEEKLY, day(0), days)
        assertEquals(LocalDate.of(2026, 1, 12).atStartOfDay(zone).toInstant(), start)
    }

    @Test fun capRepteEsDeMedicacioNiDeReptes() {
        assertTrue(ChallengeItem.catalog.none { it.requirementReason == CoinReason.CHALLENGE })
        assertEquals(8, ChallengeItem.catalog.map { it.key }.toSet().size)
    }
}
