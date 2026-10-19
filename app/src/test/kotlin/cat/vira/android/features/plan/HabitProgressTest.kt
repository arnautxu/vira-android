package cat.vira.android.features.plan

import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/// `HabitProgress`, `CalendarWeek`, `DayPeriod` i `WeekdayPicker` de l'iPhone. Només sumes de fets:
/// cap dia sense registre compta en contra.
class HabitProgressTest {

    private val zone = ZoneId.of("Europe/Madrid")

    /// Dimecres 5 d'agost del 2026.
    private val wednesday = LocalDate.of(2026, 8, 5)

    private fun log(habit: Habit, day: LocalDate) = HabitLog(day = PlanDay.start(day, zone), habitId = habit.id)

    private fun progress(vararg logs: HabitLog) = HabitProgress(logs.toList(), wednesday, zone, DayOfWeek.MONDAY)

    @Test fun `la setmana compta fins avui als programats`() {
        val habit = Habit(title = "Llegir")
        val week = progress(log(habit, wednesday.minusDays(1)), log(habit, wednesday)).week(habit)
        // Dilluns, dimarts i dimecres: 2 de 3, no 2 de 7.
        assertEquals(2 to 3, week)
    }

    @Test fun `un dia que no toca no entra al denominador`() {
        // Només dilluns (2 a la numeració de l'iPhone).
        val habit = Habit(title = "Córrer", weekdays = listOf(2))
        assertEquals(0 to 1, progress().week(habit))
        assertNull(progress().dayFraction(listOf(habit), wednesday))
    }

    @Test fun `la fraccio del dia nomes suma fets`() {
        val a = Habit(title = "A")
        val b = Habit(title = "B")
        assertEquals(0.5, progress(log(a, wednesday)).dayFraction(listOf(a, b), wednesday)!!, 0.0)
    }

    @Test fun `el mes i el total compten dies, no registres repetits`() {
        val habit = Habit(title = "Aigua")
        val p = progress(log(habit, wednesday), log(habit, wednesday), log(habit, LocalDate.of(2026, 7, 30)))
        assertEquals(1, p.daysThisMonth(habit))
        assertEquals(2, p.totalDays(habit))
    }

    @Test fun `la graella del mes comenca a la columna del dia 1`() {
        // L'1 d'agost del 2026 és dissabte: cinc buits abans, amb la setmana de dilluns.
        val grid = progress().monthGrid
        assertEquals(5, grid.takeWhile { it == null }.size)
        assertEquals(5 + 31, grid.size)
    }

    @Test fun `els que avui no toquen van al final sense moure la resta`() {
        val off = Habit(title = "Diumenge", weekdays = listOf(1))
        val a = Habit(title = "A")
        val b = Habit(title = "B")
        assertEquals(listOf("A", "B", "Diumenge"), progress().ordered(listOf(off, a, b)).map { it.title })
    }

    @Test fun `la setmana comenca pel primer dia local`() {
        val monday = CalendarWeek.containing(wednesday, DayOfWeek.MONDAY).days
        assertEquals(LocalDate.of(2026, 8, 3), monday.first())
        val sunday = CalendarWeek.containing(wednesday, DayOfWeek.SUNDAY).days
        assertEquals(LocalDate.of(2026, 8, 2), sunday.first())
        assertEquals(7, sunday.size)
    }

    @Test fun `les franges tallen a les 13 i a les 20`() {
        assertEquals(DayPeriod.Morning, DayPeriod.containing(12))
        assertEquals(DayPeriod.Midday, DayPeriod.containing(13))
        assertEquals(DayPeriod.Midday, DayPeriod.containing(19))
        assertEquals(DayPeriod.Night, DayPeriod.containing(20))
    }

    @Test fun `desmarcar l'ultim dia no deixa la llista buida`() {
        // `ProductAuditRegressionTests` de l'iPhone: el buit és "cada dia", mai el resultat de desmarcar.
        assertEquals(setOf(2), WeekdaySelection.toggling(2, isOn = false, days = setOf(2)))
        assertEquals(setOf(3), WeekdaySelection.toggling(2, isOn = false, days = setOf(2, 3)))
        assertEquals(setOf(2, 4), WeekdaySelection.toggling(4, isOn = true, days = setOf(2)))
    }

    @Test fun `l'ordre dels dies segueix el primer dia local, en numeracio de l'iPhone`() {
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 1), WeekdaySelection.ordered(DayOfWeek.MONDAY))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), WeekdaySelection.ordered(DayOfWeek.SUNDAY))
        assertEquals(DayOfWeek.SUNDAY, WeekdaySelection.dayOfWeek(1))
        assertEquals(DayOfWeek.MONDAY, WeekdaySelection.dayOfWeek(2))
        assertTrue(WeekdaySelection.stored((1..7).toSet()).isEmpty())
    }
}
