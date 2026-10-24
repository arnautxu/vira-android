package cat.vira.android.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// «Avui no» i «Uns dies» a l'avís de Viri (`ViraViriQuiet` de l'iPhone, Arnau, 2026-10-23).
///
/// Fa callar l'avís de Viri i el recordatori emocional que en fa les vegades. **Res més**: la
/// medicació i el que l'usuari ha programat segueixen igual. No toca la ratxa ni cap recompensa, i
/// no demana confirmació. Es desa el dia (inici del dia, en local) a partir del qual torna a parlar.
object ViraViriQuiet {
    /// «Uns dies»: avui i els dos següents.
    const val SEVERAL_DAYS = 3

    const val QUIET_UNTIL = "viri.quietUntil"

    fun until(p: SharedPreferences): Instant? =
        p.getLong(QUIET_UNTIL, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /// Calla `days` dies comptant avui. Mai escurça una pausa més llarga que ja hi hagi.
    fun quiet(p: SharedPreferences, days: Int, now: Instant, zone: ZoneId) {
        val end = quietEnd(days, now, zone)
        val current = until(p)
        if (current != null && !end.isAfter(current)) return
        p.edit { putLong(QUIET_UNTIL, end.toEpochMilli()) }
    }

    fun quietEnd(days: Int, now: Instant, zone: ZoneId): Instant =
        LocalDate.ofInstant(now, zone).plusDays(maxOf(days, 1).toLong()).atStartOfDay(zone).toInstant()

    /// Si el dia `day` (el seu inici) cau dins de la pausa.
    fun isQuiet(day: LocalDate, until: Instant?, zone: ZoneId): Boolean =
        until != null && day.atStartOfDay(zone).toInstant().isBefore(until)
}
