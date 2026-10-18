package cat.vira.android.features.today

import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.iosWeekday
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/// Les regles del pla que Avui fa servir i que a l'iPhone viuen a `Vira/Features/Plan/`
/// (`PlanItemOrder`, `DayPeriod`, `MedicationSchedule`, `PlanItemRecurrence`, `isScheduled`).
///
/// **Còpia de pont.** L'amo és el fil Pla (`features/plan`). Quan la seva versió sigui a `main`,
/// aquest fitxer desapareix i Avui la importa: dues còpies de l'ordre del dia és com comencen a
/// separar-se. Fins llavors, calca l'iPhone línia a línia.
internal object TodayPlanRules {

    // Franges (`DayPeriod`)

    enum class Period(val startHour: Int, val defaultHour: Int) {
        Morning(0, 9), Midday(13, 15), Night(20, 21);

        companion object {
            fun containing(hour: Int): Period = when {
                hour < 13 -> Morning
                hour < 20 -> Midday
                else -> Night
            }
        }
    }

    // Ordre del dia (`PlanItemOrder`)

    /// Les tasques sense hora van al final del Matí, just abans del migdia.
    val untimedRank: Int = (Period.Midday.startHour * 60) * 100 - 1

    /// Rang deduït de l'hora, en centèsimes de minut del dia. El `+1` reserva el zero com a «sense
    /// ordre manual».
    fun defaultRank(time: Instant?, zone: ZoneId): Int {
        time ?: return untimedRank
        val local = time.atZone(zone)
        return (local.hour * 60 + local.minute + 1) * 100
    }

    /// El manual si n'hi ha, i si no el de l'hora.
    fun rank(item: PlanItem, zone: ZoneId): Int =
        if (item.sortIndex != 0) item.sortIndex else defaultRank(item.time, zone)

    // Dies

    fun startOfDay(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).truncatedTo(ChronoUnit.DAYS).toInstant()

    fun addDays(day: Instant, days: Long, zone: ZoneId): Instant =
        day.atZone(zone).plusDays(days).toInstant()

    /// `weekdays` amb la numeració de l'iPhone (1 = diumenge); llista buida = cada dia.
    fun isScheduled(habit: Habit, day: Instant, zone: ZoneId): Boolean =
        habit.weekdays.isEmpty() || habit.weekdays.contains(day.atZone(zone).dayOfWeek.iosWeekday())

    fun isScheduled(medication: Medication, day: Instant, zone: ZoneId): Boolean {
        if (!medication.isActive) return false
        return medication.weekdays.isEmpty() || medication.weekdays.contains(day.atZone(zone).dayOfWeek.iosWeekday())
    }

    // Preses (`MedicationSchedule`)

    data class MedicationSlot(val medication: Medication, val scheduledAt: Instant, val intake: MedicationIntake?)

    /// Els espais de totes les pautes d'un dia, ordenats per hora. L'espai és la pauta; el registre
    /// només hi és quan l'usuari ha dit alguna cosa.
    fun medicationSlots(
        medications: List<Medication>,
        intakes: List<MedicationIntake>,
        day: Instant,
        zone: ZoneId,
    ): List<MedicationSlot> {
        val start = startOfDay(day, zone)
        return medications.flatMap { medication ->
            if (!isScheduled(medication, start, zone)) return@flatMap emptyList()
            medication.timesInMinutes.sorted().map { minutes ->
                val at = start.atZone(zone).plusMinutes(minutes.toLong()).toInstant()
                MedicationSlot(medication, at, intake(medication, intakes, at))
            }
        }.sortedBy { it.scheduledAt }
    }

    /// L'aparellament és al minut, com a l'iPhone.
    fun intake(medication: Medication, intakes: List<MedicationIntake>, at: Instant): MedicationIntake? {
        val minute = at.truncatedTo(ChronoUnit.MINUTES)
        return intakes.firstOrNull {
            it.medicationId == medication.id && it.scheduledAt.truncatedTo(ChronoUnit.MINUTES) == minute
        }
    }

    // Repeticions (`PlanItemRecurrence`)

    fun createsBatch(recurrence: PlanItemRecurrence): Boolean =
        recurrence == PlanItemRecurrence.DAILY_UNTIL_WEEK_END || recurrence == PlanItemRecurrence.DAILY_UNTIL_MONTH_END

    fun nextDay(recurrence: PlanItemRecurrence, after: Instant, zone: ZoneId): Instant? = when (recurrence) {
        PlanItemRecurrence.WEEKLY -> after.atZone(zone).plusWeeks(1).toInstant()
        PlanItemRecurrence.MONTHLY -> after.atZone(zone).plusMonths(1).toInstant()
        else -> null
    }

    /// Dies addicionals de la sèrie, sense repetir el primer. La setmana acaba on la fa acabar la
    /// configuració regional, com `Calendar.dateInterval(of: .weekOfYear)` de l'iPhone.
    fun additionalDays(recurrence: PlanItemRecurrence, start: Instant, zone: ZoneId, locale: Locale): List<Instant> {
        if (!createsBatch(recurrence)) return emptyList()
        val first: ZonedDateTime = start.atZone(zone).truncatedTo(ChronoUnit.DAYS)
        val last: ZonedDateTime = when (recurrence) {
            PlanItemRecurrence.DAILY_UNTIL_WEEK_END -> {
                val firstDay: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
                first.with(TemporalAdjusters.nextOrSame(firstDay.minus(1)))
            }
            else -> first.with(TemporalAdjusters.lastDayOfMonth())
        }
        val days = mutableListOf<Instant>()
        var current = first.plusDays(1)
        while (!current.isAfter(last)) {
            days += current.toInstant()
            current = current.plusDays(1)
        }
        return days
    }

    /// L'hora d'una còpia: la mateixa hora i minut, en un altre dia.
    fun sameTime(time: Instant?, day: Instant, zone: ZoneId): Instant? = time?.let {
        val original = it.atZone(zone)
        day.atZone(zone).withHour(original.hour).withMinute(original.minute).withSecond(0).withNano(0).toInstant()
    }
}
