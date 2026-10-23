package cat.vira.android.features.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/// Els passos del full «Afegir», la frase que llegeix dia i hora, «Les de sempre» i les hores amb
/// − i +, com les proves de `ViraAddSheet` de l'iPhone.
class AddStepsTest {

    /// Dilluns 19 d'octubre del 2026.
    private val monday = LocalDate.of(2026, 10, 19)

    @Test
    fun taskSkipsWhatIsAlreadyKnown() {
        assertEquals(TaskAddStep.When, TaskAddStep.What.next(dayIsGiven = false, timeIsGiven = false))
        assertEquals(TaskAddStep.Time, TaskAddStep.What.next(dayIsGiven = true, timeIsGiven = false))
        assertEquals(TaskAddStep.Done, TaskAddStep.What.next(dayIsGiven = true, timeIsGiven = true))
        assertEquals(TaskAddStep.Done, TaskAddStep.When.next(dayIsGiven = true, timeIsGiven = true))
        assertEquals(TaskAddStep.What, TaskAddStep.Time.previous(dayIsGiven = true))
        assertEquals(TaskAddStep.When, TaskAddStep.Time.previous(dayIsGiven = false))
        assertNull(TaskAddStep.Done.previous(dayIsGiven = false))
        assertNull(TaskAddStep.Done.number)
    }

    @Test
    fun habitAndMedicationSteps() {
        assertEquals(HabitAddStep.Days, HabitAddStep.What.next)
        assertEquals(HabitAddStep.Done, HabitAddStep.Reminder.next)
        assertNull(HabitAddStep.Done.previous)
        assertEquals(MedicationAddStep.Time, MedicationAddStep.What.next)
        assertNull(MedicationAddStep.Reminder.next)
        assertNull(MedicationAddStep.What.previous)
        assertEquals(3, MedicationAddStep.Reminder.number)
    }

    @Test
    fun phraseReadsDayAndHour() {
        val tomorrow = CapturePhrase.detect("Dentista demà a les 17:30", monday)
        assertEquals("Dentista", tomorrow.title)
        assertEquals(monday.plusDays(1), tomorrow.day)
        assertEquals(LocalTime.of(17, 30), tomorrow.time)

        val spanish = CapturePhrase.detect("Llamar a Marta pasado mañana a las 5 de la tarde", monday)
        assertEquals("Llamar a Marta", spanish.title)
        assertEquals(monday.plusDays(2), spanish.day)
        assertEquals(LocalTime.of(17, 0), spanish.time)
    }

    @Test
    fun phraseTakesTheNextWeekdayAndLiteralHours() {
        val friday = CapturePhrase.detect("Examen divendres", monday)
        assertEquals("Examen", friday.title)
        assertEquals(LocalDate.of(2026, 10, 23), friday.day)
        assertNull(friday.time)
        // Avui és dilluns: «dilluns» és avui, no d'aquí a una setmana.
        assertEquals(monday, CapturePhrase.detect("Piscina dilluns", monday).day)
        assertEquals(LocalTime.of(8, 0), CapturePhrase.detect("Bus a les 8", monday).time)
        // «de la mañana» és una franja, no demà.
        assertNull(CapturePhrase.detect("Correr a las 7 de la mañana", monday).day)
    }

    @Test
    fun phraseWithoutDateKeepsTheTitle() {
        val plain = CapturePhrase.detect("Comprar pa", monday)
        assertEquals("Comprar pa", plain.title)
        assertNull(plain.day)
        assertNull(plain.time)
    }

    @Test
    fun usualNeedsTwoRepeats() {
        val entries = listOf("Treure el gos", "Deures", "treure el gos", "Dutxa", "Deures", "Deures")
            .map { CaptureUsual.Entry(it, "picto-flag") }
        assertEquals(listOf("Deures", "Treure el gos"), CaptureUsual.pick(entries).map { it.title })
        assertTrue(CaptureUsual.pick(entries.take(2)).isEmpty())
    }

    @Test
    fun clockWrapsAtMidnightAndRounds() {
        assertEquals(0, ClockMinutes.moved(23 * 60 + 45, 1))
        assertEquals(23 * 60 + 45, ClockMinutes.moved(0, -1))
        assertEquals(8 * 60 + 15, ClockMinutes.moved(8 * 60 + 10, 1))
    }

    @Test
    fun weekdayPresets() {
        assertEquals(WeekdayPreset.Everyday, WeekdayPreset.matching((1..7).toSet()))
        assertEquals(WeekdayPreset.Weekend, WeekdayPreset.matching(setOf(1, 7)))
        assertNull(WeekdayPreset.matching(setOf(2, 4)))
    }

    @Test
    fun medicationTimes() {
        val state = MedicationAddState(MedicationAddStep.Time, "La del matí", listOf(9 * 60))
        state.addTime()
        assertEquals(listOf(9 * 60, 11 * 60), state.times)
        state.remove(0)
        state.remove(0) // L'última no es treu.
        assertEquals(listOf(11 * 60), state.times)
        state.weekdays = setOf(2)
        state.togglePicksDays()
        state.togglePicksDays()
        assertTrue(state.weekdays.isEmpty())
        assertTrue(state.canAdvance)
        state.name = "  "
        assertTrue(!state.canAdvance)
    }
}
