package cat.vira.android.features.plan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/// Franges del dia (`DayPeriod` de l'iPhone, `CalendarDay.swift`).
///
/// El disseny original divideix el dia en Matí / Migdia / Nit en comptes d'hores exactes: per a un
/// adolescent amb TDAH, "al matí" és una unitat de planificació més útil i menys exigent que "a
/// les 10:15".
enum class DayPeriod(
    /// Hora d'inici de la franja.
    val startHour: Int,
    /// Hora que es proposa en afegir un ítem des d'aquesta franja.
    val defaultHour: Int,
    @StringRes val title: Int,
    @DrawableRes val pictogram: Int,
) {
    Morning(0, 9, R.string.period_morning, R.drawable.picto_sun),
    Midday(13, 15, R.string.period_midday, R.drawable.picto_calendar),
    Night(20, 21, R.string.period_night, R.drawable.picto_moon);

    companion object {
        /// Per hora solta. La fan servir les posicions de `PlanItemOrder`, que no són cap data:
        /// un ítem reendreçat a mà té posició dins del dia però pot no tenir hora.
        fun containing(hour: Int): DayPeriod = when {
            hour < 13 -> Morning
            hour < 20 -> Midday
            else -> Night
        }

        fun containing(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): DayPeriod =
            containing(instant.atZone(zone).hour)
    }
}

/// Dies tal com els desa l'iPhone: **l'inici del dia en hora local, com un instant**
/// (`Calendar.current.startOfDay`). Totes les comparacions de dia passen per aquí.
object PlanDay {
    fun start(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
        instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    fun start(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Instant = date.atStartOfDay(zone).toInstant()

    fun date(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalDate = instant.atZone(zone).toLocalDate()

    fun today(zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): LocalDate = date(now, zone)

    /// Un instant a una hora concreta d'un dia.
    fun at(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    /// La numeració de l'iPhone: 1 = diumenge … 7 = dissabte (`Calendar.weekday`).
    fun iosWeekday(date: LocalDate): Int = date.dayOfWeek.value % 7 + 1

    /// Primer dia de la setmana segons la configuració regional: a ca/es dilluns, en anglès americà
    /// diumenge (`calendar.firstWeekday`). No es pot fixar a mà.
    /// Sense país (només "ca" o "es") Java tria el diumenge dels Estats Units; el criteri de CLDR
    /// per al món, i el de l'iPhone en català i castellà, és el dilluns.
    fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
        if (locale.country.isEmpty()) DayOfWeek.MONDAY else WeekFields.of(locale).firstDayOfWeek
}

/// Els set dies de la setmana que conté una data (`CalendarWeek` de l'iPhone).
data class CalendarWeek(val days: List<LocalDate>, val reference: LocalDate) {

    fun shifted(weeks: Long, firstDay: DayOfWeek): CalendarWeek = containing(reference.plusWeeks(weeks), firstDay)

    companion object {
        fun containing(date: LocalDate, firstDay: DayOfWeek = PlanDay.firstDayOfWeek()): CalendarWeek {
            val offset = (date.dayOfWeek.value - firstDay.value + 7) % 7
            val start = date.minusDays(offset.toLong())
            return CalendarWeek((0L until 7L).map { start.plusDays(it) }, date)
        }
    }
}

/// Tria de dies de la setmana: **cada dia** o **dies concrets** (`WeekdayPicker` de l'iPhone).
/// Un conjunt buit vol dir "cada dia", als hàbits i a la medicació.
object WeekdaySelection {

    /// Ordre real de la setmana segons la configuració del dispositiu, no sempre dilluns. En
    /// numeració de l'iPhone (1 = diumenge).
    fun ordered(firstDay: DayOfWeek = PlanDay.firstDayOfWeek()): List<Int> {
        val first = firstDay.value % 7 + 1
        return (0 until 7).map { (first + it - 1) % 7 + 1 }
    }

    /// El conjunt buit és el mode diari, mai el resultat de desmarcar un dia.
    fun toggling(weekday: Int, isOn: Boolean, days: Set<Int>): Set<Int> {
        if (isOn) return days + weekday
        if (days.size <= 1) return days
        return days - weekday
    }

    /// Com es desa: set dies són "cada dia", i van ordenats.
    fun stored(days: Set<Int>): List<Int> = if (days.size == 7) emptyList() else days.sorted()

    /// `DayOfWeek` d'un dia en numeració de l'iPhone.
    fun dayOfWeek(iosWeekday: Int): DayOfWeek = DayOfWeek.of((iosWeekday + 5) % 7 + 1)
}
