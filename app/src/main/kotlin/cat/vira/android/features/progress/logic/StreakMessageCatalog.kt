package cat.vira.android.features.progress.logic

import android.content.SharedPreferences
import androidx.annotation.StringRes
import cat.vira.android.R

/// Frases de ratxa de la targeta i de la celebració (`StreakMessageCatalog.swift`): totes dues
/// parlen del mateix estat i han de dir el mateix.
///
/// - **«En marxa»** en té **31**; els altres tres estats, **14** (13 «encara cap ratxa»: la 5 es
///   va retirar el 2026-08-13 perquè no s'entenia).
/// - **No totes es poden dir sempre**: «un altre dia a la llista» el primer dia és mentida. Cada
///   frase porta el dia mínim a partir del qual és certa, i dues demanen un rècord al davant. Els
///   graons són els de l'iPhone; si es toca una frase al catàleg, es repensa el graó.
/// - **Canvia a cada aparició** (en tornar a l'app i a cada celebració) i mai repeteix l'anterior.
///   `message` no rota: la poden llegir tots els dibuixats que calgui.
///
/// `advance` escriu a les preferències: **mai des d'una composició**, només des d'un esdeveniment
/// (a l'iPhone, cridar-la des d'un `body` va penjar l'app).
class StreakMessageCatalog(
    private val storage: Storage,
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    enum class State(val raw: String) { RUNNING("running"), FROZEN("frozen"), RESET("reset"), NEVER("never") }

    /// On es desa la frase actual de cada estat: **la clau**, no un índex, perquè el conjunt de
    /// frases possibles canvia amb els dies.
    interface Storage {
        fun get(key: String): String?
        fun set(key: String, value: String)
    }

    /// Les preferències de l'app, amb les mateixes claus que `UserDefaults` a l'iPhone.
    class Preferences(private val prefs: SharedPreferences) : Storage {
        override fun get(key: String) = prefs.getString(key, null)
        override fun set(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    }

    /// Una frase i les condicions per poder-la dir.
    data class Line(
        val key: String,
        @StringRes val text: Int,
        /// Dia de ratxa a partir del qual té sentit. `1` = sempre.
        val from: Int = 1,
        /// Demana un rècord anterior més alt que la ratxa actual.
        val record: Boolean = false,
    )

    /// La frase actual, sense rotar.
    @StringRes
    fun message(state: State): Int {
        val all = lines(state)
        val stored = storage.get(storageKey(state))
        return (all.firstOrNull { it.key == stored } ?: all.first()).text
    }

    /// Passa a una frase nova i la torna. Mai la que hi havia, i mai una que encara no sigui certa.
    ///
    /// `days`: dies de ratxa **després** de comptar el d'avui. `hasRecordAhead`: si el rècord és
    /// més alt que la ratxa actual.
    @StringRes
    fun advance(state: State, days: Int = 1, hasRecordAhead: Boolean = false): Int {
        val candidates = pool(state, days, hasRecordAhead)
        val current = storage.get(storageKey(state))
        val choices = if (candidates.size > 1) candidates.filter { it.key != current } else candidates
        val line = choices.random(random)
        storage.set(storageKey(state), line.key)
        return line.text
    }

    /// Rota els quatre estats alhora, en tornar a primer pla.
    fun advanceAll(days: Int, hasRecordAhead: Boolean) {
        State.entries.forEach { advance(it, days, hasRecordAhead) }
    }

    private fun pool(state: State, days: Int, hasRecordAhead: Boolean): List<Line> {
        val all = lines(state)
        if (state != State.RUNNING) return all
        val eligible = all.filter { it.from <= days && (!it.record || hasRecordAhead) }
        // Una frase imprecisa és millor que una targeta sense text.
        return eligible.ifEmpty { all }
    }

    private fun storageKey(state: State) = "streak.message.key.${state.raw}"

    companion object {
        fun lines(state: State): List<Line> = when (state) {
            State.RUNNING -> running
            State.FROZEN -> frozen
            State.RESET -> reset
            State.NEVER -> never
        }

        private val running = listOf(
            Line("streak.active.message", R.string.streak_active_message),
            Line("streak.active.message.2", R.string.streak_active_message_2),
            Line("streak.active.message.3", R.string.streak_active_message_3, from = 2),
            Line("streak.active.message.4", R.string.streak_active_message_4),
            Line("streak.active.message.5", R.string.streak_active_message_5, from = 2),
            Line("streak.active.message.6", R.string.streak_active_message_6),
            Line("streak.active.message.7", R.string.streak_active_message_7),
            Line("streak.active.message.8", R.string.streak_active_message_8, from = 3),
            Line("streak.active.message.9", R.string.streak_active_message_9),
            Line("streak.active.message.10", R.string.streak_active_message_10),
            Line("streak.active.message.11", R.string.streak_active_message_11, from = 2),
            Line("streak.active.message.12", R.string.streak_active_message_12),
            Line("streak.active.message.13", R.string.streak_active_message_13),
            Line("streak.active.message.14", R.string.streak_active_message_14, from = 2),
            Line("streak.active.message.15", R.string.streak_active_message_15),
            Line("streak.active.message.16", R.string.streak_active_message_16),
            Line("streak.active.message.17", R.string.streak_active_message_17, from = 2),
            Line("streak.active.message.18", R.string.streak_active_message_18),
            Line("streak.active.message.19", R.string.streak_active_message_19, from = 2, record = true),
            Line("streak.active.message.20", R.string.streak_active_message_20, from = 2),
            Line("streak.active.message.21", R.string.streak_active_message_21),
            Line("streak.active.message.22", R.string.streak_active_message_22),
            Line("streak.active.message.23", R.string.streak_active_message_23),
            Line("streak.active.message.24", R.string.streak_active_message_24, from = 3, record = true),
            Line("streak.active.message.25", R.string.streak_active_message_25),
            Line("streak.active.message.26", R.string.streak_active_message_26),
            Line("streak.active.message.27", R.string.streak_active_message_27, from = 2),
            Line("streak.active.message.28", R.string.streak_active_message_28),
            Line("streak.active.message.29", R.string.streak_active_message_29),
            Line("streak.active.message.30", R.string.streak_active_message_30, from = 2),
            Line("streak.active.message.31", R.string.streak_active_message_31),
        )

        private val frozen = listOf(
            Line("streak.frozen.message", R.string.streak_frozen_message),
            Line("streak.frozen.message.2", R.string.streak_frozen_message_2),
            Line("streak.frozen.message.3", R.string.streak_frozen_message_3),
            Line("streak.frozen.message.4", R.string.streak_frozen_message_4),
            Line("streak.frozen.message.5", R.string.streak_frozen_message_5),
            Line("streak.frozen.message.6", R.string.streak_frozen_message_6),
            Line("streak.frozen.message.7", R.string.streak_frozen_message_7),
            Line("streak.frozen.message.8", R.string.streak_frozen_message_8),
            Line("streak.frozen.message.9", R.string.streak_frozen_message_9),
            Line("streak.frozen.message.10", R.string.streak_frozen_message_10),
            Line("streak.frozen.message.11", R.string.streak_frozen_message_11),
            Line("streak.frozen.message.12", R.string.streak_frozen_message_12),
            Line("streak.frozen.message.13", R.string.streak_frozen_message_13),
            Line("streak.frozen.message.14", R.string.streak_frozen_message_14),
        )

        private val reset = listOf(
            Line("streak.reset.message", R.string.streak_reset_message),
            Line("streak.reset.message.2", R.string.streak_reset_message_2),
            Line("streak.reset.message.3", R.string.streak_reset_message_3),
            Line("streak.reset.message.4", R.string.streak_reset_message_4),
            Line("streak.reset.message.5", R.string.streak_reset_message_5),
            Line("streak.reset.message.6", R.string.streak_reset_message_6),
            Line("streak.reset.message.7", R.string.streak_reset_message_7),
            Line("streak.reset.message.8", R.string.streak_reset_message_8),
            Line("streak.reset.message.9", R.string.streak_reset_message_9),
            Line("streak.reset.message.10", R.string.streak_reset_message_10),
            Line("streak.reset.message.11", R.string.streak_reset_message_11),
            Line("streak.reset.message.12", R.string.streak_reset_message_12),
            Line("streak.reset.message.13", R.string.streak_reset_message_13),
            Line("streak.reset.message.14", R.string.streak_reset_message_14),
        )

        /// Sense la 5 («El primer dia és el més barat de tots»): no s'entenia.
        private val never = listOf(
            Line("streak.none.message", R.string.streak_none_message),
            Line("streak.none.message.2", R.string.streak_none_message_2),
            Line("streak.none.message.3", R.string.streak_none_message_3),
            Line("streak.none.message.4", R.string.streak_none_message_4),
            Line("streak.none.message.6", R.string.streak_none_message_6),
            Line("streak.none.message.7", R.string.streak_none_message_7),
            Line("streak.none.message.8", R.string.streak_none_message_8),
            Line("streak.none.message.9", R.string.streak_none_message_9),
            Line("streak.none.message.10", R.string.streak_none_message_10),
            Line("streak.none.message.11", R.string.streak_none_message_11),
            Line("streak.none.message.12", R.string.streak_none_message_12),
            Line("streak.none.message.13", R.string.streak_none_message_13),
            Line("streak.none.message.14", R.string.streak_none_message_14),
        )
    }
}
