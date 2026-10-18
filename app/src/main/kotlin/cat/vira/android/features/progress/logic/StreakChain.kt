package cat.vira.android.features.progress.logic

import cat.vira.android.core.data.model.Streak
import java.time.LocalDate
import java.time.YearMonth

/// Dies de ratxa amb «cadena» (`StreakChainSource.swift`): `StreakMonth` i `StreakWeek` calculen el
/// mateix sobre finestres diferents. Els dies actius surten de `Streak.activeDays`, la mateixa
/// llista que fa pujar `currentDays`: «compta» i «es veu pintat» han de ser la mateixa afirmació.
interface StreakChainSource {
    val activeDays: Set<LocalDate>
    val protectedDays: Set<LocalDate>

    fun isActive(day: LocalDate) = day in activeDays
    fun isProtected(day: LocalDate) = day in protectedDays

    /// Un dia guardat també és cadena: és tota la gràcia de guardar-lo.
    fun isChained(day: LocalDate) = isActive(day) || isProtected(day)

    /// Trams contigus d'una fila (índexs de columna): una sola càpsula per tram, no set pastilles.
    fun runs(days: List<LocalDate?>): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var start: Int? = null
        days.forEachIndexed { index, day ->
            val chained = day?.let(::isChained) ?: false
            if (chained && start == null) start = index
            if (!chained && start != null) {
                result += start!!..(index - 1)
                start = null
            }
        }
        start?.let { result += it..days.lastIndex }
        return result
    }
}

/// La setmana que conté avui, per a la franja de la ratxa (`StreakWeek.swift`).
data class StreakWeek(
    val days: List<LocalDate>,
    override val activeDays: Set<LocalDate>,
    override val protectedDays: Set<LocalDate>,
) : StreakChainSource {
    val runs: List<IntRange> get() = runs(days)

    companion object {
        fun of(streak: Streak?, today: LocalDate, days: ViraDays): StreakWeek {
            val start = days.weekStart(today)
            val week = (0L until 7L).map { start.plusDays(it) }
            val inWeek = { d: LocalDate -> d >= week.first() && d <= week.last() }
            return StreakWeek(
                days = week,
                activeDays = streak?.activeDays.orEmpty().map(days::date).filter(inWeek).toSet(),
                protectedDays = streak?.protectedDays.orEmpty().map(days::date).filter(inWeek).toSet(),
            )
        }
    }
}

/// Un mes de ratxa, llest per dibuixar (`StreakMonth.swift`): la graella amb les caselles buides
/// del principi i del final perquè el dia 1 caigui a la seva columna.
data class StreakMonth(
    val month: YearMonth,
    override val activeDays: Set<LocalDate>,
    override val protectedDays: Set<LocalDate>,
    val weeks: List<List<LocalDate?>>,
) : StreakChainSource {
    /// Dies del mes amb activitat real. Un comptador més dels que no baixen.
    val practiceDays: Int get() = activeDays.size

    companion object {
        fun of(month: YearMonth, activeDays: Set<LocalDate>, protectedDays: Set<LocalDate>, days: ViraDays): StreakMonth {
            val first = month.atDay(1)
            val leading = (first.dayOfWeek.value - days.firstDayOfWeek.value + 7) % 7
            val cells = MutableList<LocalDate?>(leading) { null }
            (1..month.lengthOfMonth()).forEach { cells += month.atDay(it) }
            while (cells.size % 7 != 0) cells += null
            val inMonth = { d: LocalDate -> YearMonth.from(d) == month }
            return StreakMonth(
                month = month,
                activeDays = activeDays.filter(inMonth).toSet(),
                protectedDays = protectedDays.filter(inMonth).toSet(),
                weeks = cells.chunked(7),
            )
        }

        fun of(streak: Streak?, month: YearMonth, days: ViraDays): StreakMonth = of(
            month,
            streak?.activeDays.orEmpty().map(days::date).toSet(),
            streak?.protectedDays.orEmpty().map(days::date).toSet(),
            days,
        )
    }
}
