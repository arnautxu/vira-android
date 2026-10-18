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
    /// tot, i repetir feina és barat; perdre un canvi no. Mai abans del sentinella `0001-01-01`: l'any
    /// zero el servidor no l'accepta.
    fun get(key: String): Instant? =
        preferences.getString(ViraPreferences.SYNC_CURSOR_PREFIX + key, null)
            ?.let(ViraTime::parse)
            ?.minus(OVERLAP)
            ?.let { maxOf(it, ViraTime.distantPast) }

    fun set(key: String, instant: Instant) = synchronized(LOCK) {
        preferences.edit().putString(ViraPreferences.SYNC_CURSOR_PREFIX + key, ViraTime.format(instant)).commit()
    }

    /// Desa el cursor només si la passada encara és del compte que hi ha ara. La comprovació i
    /// l'escriptura van juntes, sota el mateix pany que `reset`: si no, un canvi de compte entremig
    /// buidaria els cursors i aquesta passada vella hi tornaria a posar el seu.
    fun setIfCurrent(key: String, instant: Instant, isCurrent: () -> Boolean): Boolean = synchronized(LOCK) {
        if (!isCurrent()) return false
        set(key, instant)
        true
    }

    /// La taula s'ha guardat files al telèfon (`ViraSyncTable.withholds`) i, quan ja no calgui,
    /// s'ha de tornar a pujar sencera. Va amb els cursors i se'n va amb ells a `reset`.
    fun isWithheld(table: String) = preferences.getBoolean(ViraPreferences.SYNC_CURSOR_PREFIX + "withheld.$table", false)

    fun setWithheld(table: String, withheld: Boolean) = synchronized(LOCK) {
        val key = ViraPreferences.SYNC_CURSOR_PREFIX + "withheld.$table"
        preferences.edit().apply { if (withheld) putBoolean(key, true) else remove(key) }.commit()
    }

    /// Oblida per on anava, perquè la propera passada es baixi **el compte sencer**. Es crida en
    /// obrir sessió (Arnau, 2026-08-12) i després de l'esborrat total.
    fun reset() = synchronized(LOCK) {
        val keys = preferences.all.keys.filter { it.startsWith(ViraPreferences.SYNC_CURSOR_PREFIX) }
        preferences.edit().apply { keys.forEach(::remove) }.commit()
    }

    companion object {
        val OVERLAP: Duration = Duration.ofMinutes(5)

        /// Un per procés, no per instància: la sessió i el motor en tenen cadascun la seva.
        private val LOCK = Any()
    }
}
