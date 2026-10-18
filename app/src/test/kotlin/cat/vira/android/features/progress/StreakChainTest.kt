package cat.vira.android.features.progress

import cat.vira.android.core.data.model.Streak
import cat.vira.android.features.progress.logic.StreakMilestone
import cat.vira.android.features.progress.logic.StreakMonth
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.logic.ViraDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/// `StreakWeekTests`, `StreakMonthTests` i `StreakMilestoneTests` de l'iPhone.
class StreakChainTest {

    private val days = ViraDays(ZoneOffset.UTC, DayOfWeek.MONDAY)
    private fun day(d: Int) = LocalDate.of(2026, 7, d)

    /// Dilluns 6 a diumenge 12 de juliol del 2026.
    private fun week(active: List<Int>, protected: List<Int> = emptyList()) =
        StreakWeek((6..12).map(::day), active.map(::day).toSet(), protected.map(::day).toSet())

    private fun month(active: List<Int>, protected: List<Int> = emptyList()) =
        StreakMonth.of(YearMonth.of(2026, 7), active.map(::day).toSet(), protected.map(::day).toSet(), days)

    // Setmana

    @Test fun laSetmanaTe7Dies() = assertEquals(7, week(emptyList()).days.size)

    @Test fun laSetmanaDAvuiComencaElPrimerDiaDelTelefon() {
        val streak = Streak(activeDays = listOf(days.start(day(8))))
        val w = StreakWeek.of(streak, day(9), days)
        assertEquals(day(6), w.days.first())
        assertTrue(w.isActive(day(8)))
    }

    @Test fun setmanaDiesConsecutiusSonUnTram() {
        val runs = week(listOf(6, 7, 8, 9, 10)).runs
        assertEquals(1, runs.size)
        assertEquals(5, runs[0].count())
    }

    @Test fun setmanaUnDiaPerdutPartElTram() = assertEquals(2, week(listOf(6, 7, 9, 10)).runs.size)

    @Test fun setmanaUnDiaGuardatMantéElTram() {
        val runs = week(listOf(6, 7, 9, 10), listOf(8)).runs
        assertEquals(1, runs.size)
        assertEquals(5, runs[0].count())
    }

    @Test fun elsDiesGuardatsNoSonActius() {
        val w = week(listOf(6, 7), listOf(8))
        assertFalse(w.isActive(day(8)))
        assertTrue(w.isProtected(day(8)))
        assertTrue(w.isChained(day(8)))
    }

    @Test fun setmanaBuidaSenseTrams() = assertTrue(week(emptyList()).runs.isEmpty())

    // Mes

    @Test fun laGraellaCobreixElMesSencer() {
        val m = month(emptyList())
        assertTrue(m.weeks.all { it.size == 7 })
        assertEquals(31, m.weeks.flatten().filterNotNull().size)
        // L'1 de juliol del 2026 és dimecres: dues caselles buides al davant.
        assertEquals(listOf(null, null, day(1)), m.weeks.first().take(3))
    }

    @Test fun mesDiesConsecutiusSonUnTram() {
        val m = month(listOf(6, 7, 8, 9, 10))
        val row = m.weeks.first { day(6) in it }
        assertEquals(listOf(0..4), m.runs(row))
    }

    @Test fun mesUnDiaGuardatMantéElTram() {
        val m = month(listOf(6, 7, 9, 10), listOf(8))
        assertEquals(1, m.runs(m.weeks.first { day(6) in it }).size)
    }

    @Test fun elsDiesGuardatsNoSonDiesDActivitat() {
        val m = month(listOf(6, 7), listOf(8))
        assertEquals(2, m.practiceDays)
        assertTrue(m.isChained(day(8)))
    }

    @Test fun mesBuitSenseTrams() {
        val m = month(emptyList())
        assertTrue(m.weeks.all { m.runs(it).isEmpty() })
        assertEquals(0, m.practiceDays)
    }

    @Test fun unDiaComptatSurtActiuAlCalendari() {
        val streak = Streak(activeDays = listOf(days.start(day(15))))
        assertTrue(StreakMonth.of(streak, YearMonth.of(2026, 7), days).isActive(day(15)))
    }

    // Fites

    @Test fun creuar7DiesMarcaLaMedallaSetmanal() {
        val (s, card) = StreakMilestone.checkAndMark(Streak(bestDays = 7))
        assertEquals(StreakMilestone.WEEK_CARD, card)
        assertTrue(s.hasCelebratedWeekStreak)
    }

    @Test fun creuar30DiesMarcaElTrofeuMensualIGuanyaLaGran() {
        val (s, card) = StreakMilestone.checkAndMark(Streak(bestDays = 30))
        assertEquals(StreakMilestone.MONTH_CARD, card)
        assertTrue(s.hasCelebratedMonthStreak)
        assertFalse(s.hasCelebratedWeekStreak)
    }

    @Test fun unCopCelebradaNoTornaADisparar() =
        assertNull(StreakMilestone.checkAndMark(Streak(bestDays = 7, hasCelebratedWeekStreak = true)).second)

    @Test fun ambLaRatxaAmagadaNiSenyalNiPantalla() {
        val (s, card) = StreakMilestone.checkAndMark(Streak(bestDays = 7, isVisible = false))
        assertNull(card)
        assertFalse(s.hasCelebratedWeekStreak)
    }
}
