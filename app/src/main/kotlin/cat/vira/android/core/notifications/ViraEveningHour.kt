package cat.vira.android.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// L'hora de l'avís de Viri, apresa al telèfon (`ViraEveningHour` de l'iPhone, Arnau, 2026-10-23).
/// No es tria a mà: «no m'agrada que puguin tocar l'hora».
///
/// - Cada dia que compta per a la ratxa, s'apunta l'hora en què el telèfon ho veu per primer cop.
/// - Amb **3 dies o més** dels últims 14, l'avís surt **una hora abans de la mitjana**, arrodonida
///   a quarts, i **sempre entre les 17:00 i les 21:00**.
/// - Amb menys, a l'hora de sempre (20:30).
///
/// Una hora passada la mitjanit compta com a tard, no com a d'hora. Tot queda al telèfon.
object ViraEveningHour {
    const val EARLIEST = 17 * 60
    const val LATEST = 21 * 60
    const val LEAD = 60
    const val MINIMUM_DAYS = 3
    const val KEPT_DAYS = 14L

    /// Com a l'iPhone, fora de `settings.`: és estat del telèfon.
    const val DAY_CLOSINGS = "viri.dayClosings"

    /// Minuts des de les 0:00 de l'avís, o `null` si encara no hi ha prou dies.
    fun learned(closings: Collection<Int>): Int? {
        if (closings.size < MINIMUM_DAYS) return null
        // Les hores de matinada són la continuació del vespre d'abans.
        val lateAware = closings.map { if (it < 4 * 60) it + 24 * 60 else it }
        val mean = lateAware.sum() / lateAware.size
        val rounded = Math.round((mean - LEAD) / 15.0).toInt() * 15
        return rounded.coerceIn(EARLIEST, LATEST)
    }

    /// L'hora de l'avís d'avui: l'apresa o la de sempre.
    fun time(p: SharedPreferences): ViraClock {
        val minutes = learned(closings(p).values)
            ?: (ViraNotificationPreferences.VIRI_DEFAULT_HOUR * 60 + ViraNotificationPreferences.VIRI_DEFAULT_MINUTE)
        return ViraClock(minutes / 60, minutes % 60)
    }

    /// El dia ja compta. Només s'apunta el primer cop de cada dia.
    fun noteDayClosed(p: SharedPreferences, now: Instant, zone: ZoneId) {
        val today = LocalDate.ofInstant(now, zone)
        val all = closings(p)
        if (all.containsKey(today)) return
        val clock = now.atZone(zone)
        all[today] = clock.hour * 60 + clock.minute
        val oldest = today.minusDays(KEPT_DAYS)
        val kept = all.filterKeys { it.isAfter(oldest) }
        p.edit { putString(DAY_CLOSINGS, kept.entries.joinToString(";") { "${it.key}=${it.value}" }) }
    }

    private fun closings(p: SharedPreferences): MutableMap<LocalDate, Int> =
        p.getString(DAY_CLOSINGS, null).orEmpty().split(';').mapNotNull { entry ->
            val (day, minutes) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return@mapNotNull null
            minutes.toIntOrNull()?.let { parsed to it }
        }.toMap(mutableMapOf())
}
