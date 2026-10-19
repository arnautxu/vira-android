package cat.vira.android.features.plan

import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/// Les repeticions d'una tasca (`PlanItemRecurrence` de l'iPhone, `Models/PlanItem.swift`).
object PlanItemSeries {

    @StringRes
    fun title(recurrence: PlanItemRecurrence): Int = when (recurrence) {
        PlanItemRecurrence.NONE -> R.string.plan_repeat_none
        PlanItemRecurrence.WEEKLY -> R.string.plan_repeat_weekly
        PlanItemRecurrence.MONTHLY -> R.string.plan_repeat_monthly
        PlanItemRecurrence.DAILY_UNTIL_WEEK_END -> R.string.plan_repeat_daily_until_week_end
        PlanItemRecurrence.DAILY_UNTIL_MONTH_END -> R.string.plan_repeat_daily_until_month_end
    }

    /// Les dues opcions de "cada dia" creen les instàncies en guardar; no són una regla que
    /// continuï després. Les setmanals i mensuals només creen la següent quan es completa l'actual.
    fun createsBatch(recurrence: PlanItemRecurrence): Boolean =
        recurrence == PlanItemRecurrence.DAILY_UNTIL_WEEK_END || recurrence == PlanItemRecurrence.DAILY_UNTIL_MONTH_END

    fun nextDay(recurrence: PlanItemRecurrence, after: LocalDate): LocalDate? = when (recurrence) {
        PlanItemRecurrence.WEEKLY -> after.plusWeeks(1)
        PlanItemRecurrence.MONTHLY -> after.plusMonths(1)
        else -> null
    }

    /// Dies addicionals de la sèrie, sense repetir el dia inicial. El final de la setmana/mes que
    /// conté el dia triat evita crear dies passats si la tasca s'afegeix a mig període.
    fun additionalDays(
        recurrence: PlanItemRecurrence,
        start: LocalDate,
        firstDay: DayOfWeek = PlanDay.firstDayOfWeek(),
    ): List<LocalDate> {
        val end = when (recurrence) {
            PlanItemRecurrence.DAILY_UNTIL_WEEK_END -> CalendarWeek.containing(start, firstDay).days.last()
            PlanItemRecurrence.DAILY_UNTIL_MONTH_END -> start.with(TemporalAdjusters.lastDayOfMonth())
            else -> return emptyList()
        }
        return generateSequence(start.plusDays(1)) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    }
}

/// Marcar, repetir i copiar ítems del pla (`PlanItemCompletion` de l'iPhone), sense interfície.
/// El "Desfer" i la recompensa no són aquí: els porta qui crida (`PlanHost`).
object PlanItemCompletion {

    /// Una tasca recurrent només materialitza el següent cop quan l'actual es tanca. Això manté el
    /// calendari curt i editable, i no tracta una instància futura com una promesa.
    fun nextOccurrence(item: PlanItem, zone: ZoneId = ZoneId.systemDefault()): PlanItem? {
        val next = PlanItemSeries.nextDay(item.recurrence, PlanDay.date(item.day, zone)) ?: return null
        return copy(item, next, zone).copy(recurrenceRaw = item.recurrenceRaw)
    }

    /// Materialitza una sèrie curta quan es crea o edita una tasca. El primer dia és l'ítem
    /// original; les còpies són independents i no es repeteixen en completar-les.
    ///
    /// Torna l'ítem amb la repetició que li toca desar i les còpies noves que cal crear.
    fun applyRecurrence(
        item: PlanItem,
        selected: PlanItemRecurrence,
        zone: ZoneId = ZoneId.systemDefault(),
        firstDay: DayOfWeek = PlanDay.firstDayOfWeek(),
    ): Pair<PlanItem, List<PlanItem>> {
        if (!PlanItemSeries.createsBatch(selected)) return item.copy(recurrenceRaw = selected.raw) to emptyList()
        val original = item.copy(recurrenceRaw = PlanItemRecurrence.NONE.raw)
        val copies = PlanItemSeries.additionalDays(selected, PlanDay.date(item.day, zone), firstDay)
            .map { copy(original, it, zone) }
        return original to copies
    }

    /// Mateix títol, notes, icona i avís; l'hora es trasllada al dia nou. Ordre i estat, de zero.
    private fun copy(item: PlanItem, day: LocalDate, zone: ZoneId): PlanItem {
        val time = item.time?.atZone(zone)?.let { PlanDay.at(day, it.hour, it.minute, zone) }
        val now = ViraTime.now()
        return PlanItem(
            title = item.title,
            notes = item.notes,
            day = PlanDay.start(day, zone),
            time = time,
            symbolName = item.symbolName,
            reminderOffsetMinutes = item.reminderOffsetMinutes,
            createdAt = now,
            updatedAt = now,
        )
    }
}
