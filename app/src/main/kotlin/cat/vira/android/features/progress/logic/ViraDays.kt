package cat.vira.android.features.progress.logic

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/// Dies de calendari tal com els fa servir l'iPhone (`Calendar.current.startOfDay`): un dia és
/// l'instant de les 00:00 a la zona del telèfon. Així `Streak.activeDays` i `protectedDays`
/// s'escriuen igual als dos telèfons i el mateix compte pinta el mateix calendari.
class ViraDays(
    val zone: ZoneId,
    /// Primer dia de la setmana del telèfon (`Calendar.firstWeekday`): dilluns a Europa.
    val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
) {
    fun date(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun start(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    fun startOfDay(instant: Instant): Instant = start(date(instant))

    /// Dies de calendari entre dos instants (`dateComponents([.day], from:to:)`).
    fun daysBetween(from: Instant, to: Instant): Long = ChronoUnit.DAYS.between(date(from), date(to))

    fun addDays(instant: Instant, days: Long): Instant = start(date(instant).plusDays(days))

    /// Primer dia de la setmana que conté `date` (`dateInterval(of: .weekOfYear)`).
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
}
