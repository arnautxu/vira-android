package cat.vira.android.features.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.core.data.ViraPreferences

/// Les claus d'Ajustos, amb **els mateixos noms que a l'iPhone** (`ViraPreferences.swift`). Les que
/// ja són a `core/data` (aparença, celebracions, sincronització…) es fan servir d'allà; aquí hi ha
/// les que només llegeixen Ajustos i els fils que en depenen (avisos, Avui, Viri).
///
/// Tots els valors per defecte són **els de l'iPhone**: si una pantalla en llegeix un altre, el
/// recompte de la portada mentiria el primer dia (`SettingsTally`).
object SettingsKeys {
    // Avisos (`USER_FLOWS.md` F24)
    const val REMINDERS_HABITS = "settings.remindersHabitsEnabled"
    const val REMINDERS_MEDICATION = "settings.remindersMedicationEnabled"
    const val REMINDERS_PLAN = "settings.remindersPlanEnabled"
    const val REMINDERS_EMOTION = "settings.remindersEmotionEnabled"
    const val EMOTION_REMINDER_HOUR = "settings.emotionReminderHour"
    const val EMOTION_REMINDER_MINUTE = "settings.emotionReminderMinute"
    /// El nom del fàrmac té interruptor propi i neix apagat (Arnau, 2026-08-13).
    const val NOTIFICATION_DETAIL_MEDICATION = "settings.notificationDetailMedicationEnabled"
    const val VIRI_MESSAGES = "settings.viriMessagesEnabled"
    const val VIRI_MESSAGE_HOUR = "settings.viriMessageHour"
    const val VIRI_MESSAGE_MINUTE = "settings.viriMessageMinute"

    // Mòduls (D-37) i connexions
    const val MODULE_HABITS = "settings.moduleHabitsEnabled"
    const val MODULE_MEDICATION = "settings.moduleMedicationEnabled"
    const val MODULE_MINDFULNESS = "settings.moduleMindfulnessEnabled"
    /// Apagat de fàbrica, com tot el que depèn d'un permís.
    const val CALENDAR = "settings.calendarEnabled"

    // Motivació (`VIRI_AND_GAMIFICATION.md` §3.5)
    const val SOUND_MODE = "settings.soundMode"
    const val HAPTICS = "settings.hapticsEnabled"
    const val STREAK_LIVE_ACTIVITY = "settings.streakLiveActivityEnabled"
    const val PROGRESS_VISIBLE = "settings.progressVisible"
    const val SHOP_VISIBLE = "settings.shopVisible"

    const val EMOTION_DEFAULT_HOUR = 19
    const val EMOTION_DEFAULT_MINUTE = 0
    const val VIRI_DEFAULT_HOUR = 20
    const val VIRI_DEFAULT_MINUTE = 30
}

/// Aparença de l'app (`ViraAppearance`). El valor desat és el de l'iPhone.
enum class ViraAppearance(val raw: String) {
    AUTO("auto"), LIGHT("light"), DARK("dark");

    /// `null` vol dir «el que digui el telèfon».
    val forcesDark: Boolean? get() = when (this) {
        AUTO -> null
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: AUTO

        /// Per al fil de l'esquelet: el tema fosc que toca segons Ajustos i el sistema.
        fun isDark(preferences: SharedPreferences, systemIsDark: Boolean): Boolean =
            from(preferences.getString(ViraPreferences.APPEARANCE, null)).forcesDark ?: systemIsDark
    }
}

/// Sons (Arnau, 2026-10-18): Tots · Només celebracions · Cap. El valor desat és el de l'iPhone.
enum class ViraSoundMode(val raw: String) {
    ALL("all"), CELEBRATIONS("celebrations"), OFF("off");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: ALL
    }
}

/// Les preferències que llegeixen les pantalles d'Ajustos. A l'app són les de `ViraBackend`; a les
/// captures, unes en memòria (`InMemoryPreferences`).
val LocalSettingsPreferences = staticCompositionLocalOf<SharedPreferences> {
    error("Ajustos sense preferències")
}

/// Una preferència com a estat de Compose (`@AppStorage`): es redibuixa quan canvia, també si la
/// canvia una altra pantalla.
@Composable
fun rememberBooleanPreference(key: String, default: Boolean): MutableState<Boolean> =
    rememberPreference(key, read = { getBoolean(key, default) }, write = { putBoolean(key, it) })

@Composable
fun rememberIntPreference(key: String, default: Int): MutableState<Int> =
    rememberPreference(key, read = { getInt(key, default) }, write = { putInt(key, it) })

@Composable
fun rememberStringPreference(key: String, default: String): MutableState<String> =
    rememberPreference(key, read = { getString(key, default) ?: default }, write = { putString(key, it) })

@Composable
private fun <T> rememberPreference(
    key: String,
    read: SharedPreferences.() -> T,
    write: SharedPreferences.Editor.(T) -> Unit,
): MutableState<T> {
    val preferences = LocalSettingsPreferences.current
    val state = remember(preferences, key) { PreferenceState(preferences, read, write) }
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            // `null` és «s'ha buidat tot» (Android 11+): també s'ha de tornar a llegir.
            if (changed == null || changed == key) state.reload()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        state.reload()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

private class PreferenceState<T>(
    private val preferences: SharedPreferences,
    private val read: SharedPreferences.() -> T,
    private val write: SharedPreferences.Editor.(T) -> Unit,
) : MutableState<T> {
    private val backing = mutableStateOf(preferences.read())

    fun reload() { backing.value = preferences.read() }

    override var value: T
        get() = backing.value
        set(newValue) {
            backing.value = newValue
            preferences.edit().apply { write(newValue) }.apply()
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

/// El recompte de les famílies, recalculat quan canvia qualsevol preferència.
@Composable
fun rememberSettingsTally(): SettingsTally {
    val preferences = LocalSettingsPreferences.current
    val tally = remember(preferences) { mutableStateOf(SettingsTally.from(preferences)) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tally.value = SettingsTally.from(preferences) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return tally.value
}
