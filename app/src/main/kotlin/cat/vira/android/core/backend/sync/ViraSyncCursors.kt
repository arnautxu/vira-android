package cat.vira.android.core.backend.sync

import android.content.SharedPreferences
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraTime
import java.time.Duration
import java.time.Instant

/// Per on anava la sincronització, per taula i direcció (`sync.cursor.push.plan_items`…).
///
/// Van a les preferències i no a la base de dades perquè no són dades d'usuari: si es perden, la
/// pitjor conseqüència és una sincronització completa de més.
class ViraSyncCursors(private val preferences: SharedPreferences) {

    /// Data del cursor **menys el marge**: els rellotges de dos telèfons no coincideixen mai del
    /// tot, i repetir feina és barat; perdre un canvi no.
    fun get(key: String): Instant? =
        preferences.getString(ViraPreferences.SYNC_CURSOR_PREFIX + key, null)
            ?.let(ViraTime::parse)
            ?.minus(OVERLAP)

    fun set(key: String, instant: Instant) {
        preferences.edit().putString(ViraPreferences.SYNC_CURSOR_PREFIX + key, ViraTime.format(instant)).apply()
    }

    /// Oblida per on anava, perquè la propera passada es baixi **el compte sencer**. Es crida en
    /// obrir sessió (Arnau, 2026-08-12) i després de l'esborrat total.
    fun reset() {
        val keys = preferences.all.keys.filter { it.startsWith(ViraPreferences.SYNC_CURSOR_PREFIX) }
        preferences.edit().apply { keys.forEach(::remove) }.commit()
    }

    companion object {
        val OVERLAP: Duration = Duration.ofMinutes(5)
    }
}
