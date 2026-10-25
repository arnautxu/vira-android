package cat.vira.android.features.progress.logic

import android.content.SharedPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.json.JSONObject

/// Cada cosa marcada es premia **un sol cop**: un hàbit, una vegada per dia; una tasca, una vegada
/// (`ViraRewardOnce.swift` de l'iPhone).
///
/// Desmarcar mai resta res —l'XP mai baixa—, i això deixava una porta oberta: marcar, desmarcar i
/// tornar a marcar el mateix hàbit tornava a donar ViriCoins i XP cada vegada (Arnau, 2026-10-23).
/// Aquí es recorda què ja s'ha premiat; si es torna a marcar, queda marcat i compta per al dia
/// igual, però no paga dues vegades.
///
/// A les preferències i no a Room pel mateix motiu que a l'iPhone: una columna nova sense la seva
/// parella a Supabase faria petar la sincronització. Es guarda 60 dies.
class ViraRewardOnce(private val preferences: SharedPreferences) {

    fun wasRewarded(key: String): Boolean = stored().has(key)

    /// Apunta que `key` es premia ara. `false` si ja s'havia premiat abans: qui crida no ha de
    /// tornar a donar res. Llegir i escriure van sota un sol pany per a tot el procés: la cua de
    /// «Desfer» i un botó d'un avís poden reclamar alhora, i cap dels dos ha de trepitjar l'altre.
    fun claim(key: String, now: Instant): Boolean = synchronized(LOCK) {
        val entries = stored()
        if (entries.has(key)) return@synchronized false
        val cutoff = now.epochSecond - RETENTION_SECONDS
        val kept = JSONObject()
        entries.keys().forEach { stored -> if (entries.getDouble(stored) >= cutoff) kept.put(stored, entries.getDouble(stored)) }
        kept.put(key, now.epochSecond.toDouble())
        preferences.edit().putString(KEY, kept.toString()).apply()
        true
    }

    private fun stored(): JSONObject =
        preferences.getString(KEY, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()

    companion object {
        /// `ViraPreferences.rewardedCompletions` de l'iPhone. El prefix `rewards.` és de Vira: el
        /// buida l'esborrat total i el canvi de compte.
        const val KEY = "rewards.completions"
        const val RETENTION_SECONDS = 60L * 24 * 60 * 60
        private val LOCK = Any()

        /// La mateixa identitat que fa servir la fila per al «Desfer» (`PlanCompletionIds`, `TodayData`).
        fun habitKey(habitId: UUID, day: LocalDate, zone: ZoneId): String =
            "habit-$habitId-${day.atStartOfDay(zone).toEpochSecond()}"

        fun planItemKey(itemId: UUID): String = "plan-$itemId"
    }
}
