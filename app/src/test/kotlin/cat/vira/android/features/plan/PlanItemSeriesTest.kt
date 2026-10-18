package cat.vira.android.features.plan

import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/// Repeticions (`PlanItemRecurrenceTests` de l'iPhone) i les còpies que creen.
class PlanItemSeriesTest {

    private val zone = ZoneId.of("Europe/Madrid")

    @Test fun `la serie setmanal crea un item per cada dia restant`() {
        val days = PlanItemSeries.additionalDays(PlanItemRecurrence.DAILY_UNTIL_WEEK_END, LocalDate.of(2026, 8, 12), DayOfWeek.MONDAY)
        assertEquals(listOf(13, 14, 15, 16), days.map { it.dayOfMonth })
    }

    @Test fun `la serie mensual no crea dies del mes seguent`() {
        val days = PlanItemSeries.additionalDays(PlanItemRecurrence.DAILY_UNTIL_MONTH_END, LocalDate.of(2026, 2, 27), DayOfWeek.MONDAY)
        assertEquals(listOf(28), days.map { it.dayOfMonth })
    }

    @Test fun `les series curtes no continuen en completar una copia`() {
        val day = LocalDate.of(2026, 8, 12)
        assertNull(PlanItemSeries.nextDay(PlanItemRecurrence.DAILY_UNTIL_WEEK_END, day))
        assertNull(PlanItemSeries.nextDay(PlanItemRecurrence.DAILY_UNTIL_MONTH_END, day))
    }

    @Test fun `una tasca setmanal fa la seguent en tancar-se, a la mateixa hora`() {
        val day = LocalDate.of(2026, 8, 12)
        val item = PlanItem(
            title = "Anglès",
            day = PlanDay.start(day, zone),
            time = PlanDay.at(day, 17, 30, zone),
            recurrenceRaw = PlanItemRecurrence.WEEKLY.raw,
        )
        val next = PlanItemCompletion.nextOccurrence(item, zone)!!
        assertEquals(PlanDay.start(day.plusWeeks(1), zone), next.day)
        assertEquals(PlanDay.at(day.plusWeeks(1), 17, 30, zone), next.time)
        assertEquals(PlanItemRecurrence.WEEKLY, next.recurrence)
        assertTrue(next.id != item.id)
    }

    @Test fun `una serie diaria queda sense repeticio i amb les copies fins diumenge`() {
        val day = LocalDate.of(2026, 8, 12)
        val item = PlanItem(title = "Llegir", day = PlanDay.start(day, zone))
        val (saved, copies) = PlanItemCompletion.applyRecurrence(item, PlanItemRecurrence.DAILY_UNTIL_WEEK_END, zone, DayOfWeek.MONDAY)
        assertEquals(PlanItemRecurrence.NONE, saved.recurrence)
        assertEquals(4, copies.size)
        assertTrue(copies.all { it.recurrence == PlanItemRecurrence.NONE && it.title == "Llegir" })
    }

    @Test fun `una repeticio setmanal es desa sense copies`() {
        val item = PlanItem(title = "Llegir", day = PlanDay.start(LocalDate.of(2026, 8, 12), zone))
        val (saved, copies) = PlanItemCompletion.applyRecurrence(item, PlanItemRecurrence.WEEKLY, zone)
        assertEquals(PlanItemRecurrence.WEEKLY, saved.recurrence)
        assertTrue(copies.isEmpty())
    }
}
