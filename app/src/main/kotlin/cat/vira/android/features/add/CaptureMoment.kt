package cat.vira.android.features.add

import cat.vira.android.features.plan.DayPeriod
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/// Els moments d'un toc del pas «Quan» (`QuickCaptureMoment`, Arnau, 2026-10-24, opció A + C): un
/// dia i una franja alhora, «Avui · Nit», «Demà · Matí». Un toc desa la tasca.
///
/// Per què tres i no més: la resta del pas ja hi és a sota (calendari i rellotge). Aprenen sense que
/// ningú ho configuri, com «Les de sempre»: els que la persona ha fet servir almenys dues vegades
/// passen al davant. Res surt del telèfon: es calcula del pla local.
data class CaptureMoment(
    /// 0 avui, 1 demà… Relatiu al dia en què s'apunta.
    val dayOffset: Int,
    val period: DayPeriod,
) {
    /// El dia i l'hora que es desaran.
    fun resolved(today: LocalDate): LocalDateTime =
        today.plusDays(dayOffset.toLong()).atTime(period.defaultHour, 0)

    companion object {
        /// Mitja hora de marge: «Avui · Matí» a les 8:50 ja no és un moment, és ara mateix.
        val MARGIN: Duration = Duration.ofMinutes(30)

        /// Els moments que encara no han passat, de més a prop a més lluny: el que queda d'avui i
        /// les tres franges de demà.
        fun upcoming(now: LocalDateTime): List<CaptureMoment> {
            val today = now.toLocalDate()
            return listOf(0, 1).flatMap { offset -> DayPeriod.entries.map { CaptureMoment(offset, it) } }
                .filter { Duration.between(now, it.resolved(today)) >= MARGIN }
        }

        /// Els tres a ensenyar. `history` són els moments triats abans (`of`), del més recent al més
        /// antic. Si la frase ja diu el dia (`fixedDayOffset`), els tres són les franges d'aquell dia.
        fun suggestions(
            now: LocalDateTime,
            history: List<CaptureMoment>,
            fixedDayOffset: Int? = null,
            limit: Int = 3,
        ): List<CaptureMoment> {
            val candidates = when {
                fixedDayOffset != null && fixedDayOffset > 1 -> DayPeriod.entries.map { CaptureMoment(fixedDayOffset, it) }
                fixedDayOffset != null -> upcoming(now).filter { it.dayOffset == fixedDayOffset }
                else -> upcoming(now)
            }
            val counts = history.groupingBy { it }.eachCount()
            fun learned(moment: CaptureMoment): Int = (counts[moment] ?: 0).takeIf { it >= 2 } ?: 0
            // Els apresos primer (dels més triats als menys); a igualtat, i per a la resta, de més a
            // prop a més lluny. `sortedByDescending` és estable.
            return candidates.sortedByDescending(::learned).take(limit)
        }

        /// El moment d'un ítem del pla, si n'era un: desat per a avui o demà del dia en què es va
        /// crear, i amb hora. L'hora exacta compta per la franja on cau.
        fun of(day: LocalDate, time: LocalDateTime?, createdOn: LocalDate): CaptureMoment? {
            if (time == null) return null
            val offset = ChronoUnit.DAYS.between(createdOn, day)
            if (offset !in 0..1) return null
            return CaptureMoment(offset.toInt(), DayPeriod.containing(time.hour))
        }
    }
}
