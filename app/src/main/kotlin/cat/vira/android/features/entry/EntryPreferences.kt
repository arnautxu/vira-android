package cat.vira.android.features.entry

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import cat.vira.android.core.data.ViraPreferences

/// Les claus que fan servir el flux d'entrada i el tutorial, **amb els mateixos noms que l'iPhone**
/// (`ViraPreferences.swift`). Les comunes ja són a `core/data/ViraPreferences`; aquí hi ha les que
/// només llegeixen aquestes pantalles, i les d'avisos i mòduls que el pas d'avisos també toca. Si un
/// altre fil n'acaba sent l'amo (Ajustos, Notificacions), el nom és el mateix i la preferència també.
object EntryPreferenceKeys {
    const val ONBOARDING_COMPLETED = ViraPreferences.ONBOARDING_COMPLETED
    const val TUTORIAL_COMPLETED = ViraPreferences.TUTORIAL_COMPLETED
    /// El tomb es va saltar abans de l'última parada: les pistes de Pla i Emocions només surten a
    /// qui no les ha vistes. Com `tutorialCompleted`, és estat del telèfon.
    const val TUTORIAL_SKIPPED = "settings.tutorialSkipped"
    /// «Aquí és Viri» ja s'ha vist en aquest telèfon (`ViriTabTip`).
    const val VIRI_TAB_TIP_SEEN = "settings.viriTabTipSeen"
    const val CELEBRATIONS_ENABLED = ViraPreferences.CELEBRATIONS_ENABLED
    const val BIOMETRIC_LOCK_ENABLED = ViraPreferences.BIOMETRIC_LOCK_ENABLED
    const val REMINDERS_ENABLED = ViraPreferences.REMINDERS_ENABLED

    /// Rang orientatiu (`ViraAgeRange`). Buit vol dir "no triat". Mai una data de naixement.
    const val AGE_RANGE = "settings.ageRange"
    /// Per on vol començar qui entra (`EntryStartIntent`).
    const val ENTRY_START_INTENT = "settings.entryStartIntent"
    /// Què ha fet de debò qui ha passat per la primera acció (`EntryFirstActionKind`); buit si ha
    /// dit «Ara no». Només serveix perquè el tomb no repeteixi el que «Fet» ja ha dit.
    const val ENTRY_FIRST_ACTION = "settings.entryFirstAction"
    const val PROGRESS_VISIBLE = "settings.progressVisible"
    const val MODULE_HABITS_ENABLED = "settings.moduleHabitsEnabled"
    const val MODULE_MEDICATION_ENABLED = "settings.moduleMedicationEnabled"
    const val REMINDERS_HABITS_ENABLED = "settings.remindersHabitsEnabled"
    const val REMINDERS_MEDICATION_ENABLED = "settings.remindersMedicationEnabled"
    const val REMINDERS_PLAN_ENABLED = "settings.remindersPlanEnabled"
    const val REMINDERS_EMOTION_ENABLED = "settings.remindersEmotionEnabled"
    const val VIRI_MESSAGES_ENABLED = "settings.viriMessagesEnabled"
    const val STREAK_LIVE_ACTIVITY_ENABLED = "settings.streakLiveActivityEnabled"
    const val CALENDAR_ENABLED = "settings.calendarEnabled"
    /// Android no diu si el permís d'avisos s'ha demanat mai: aquesta marca ho recorda. És del
    /// telèfon, no de la persona.
    const val NOTIFICATIONS_ASKED = "entry.notificationsAsked"
}

/// Una preferència que les pantalles poden observar: llegir-la redibuixa, escriure-la la desa.
class EntryPreference<T>(initial: T, private val write: (T) -> Unit) {
    private val state = mutableStateOf(initial)
    var value: T
        get() = state.value
        set(newValue) {
            state.value = newValue
            write(newValue)
        }
}

/// Les preferències del flux. Una sola instància per flux, perquè dues pantalles que miren la
/// mateixa clau (els interruptors del pas de connexions i del tutorial) vegin el mateix valor.
class EntryPreferences(private val store: Store) {

    /// On es desen de debò. A l'app, `SharedPreferences`; a les captures i a les proves, un mapa.
    interface Store {
        fun getString(key: String, default: String): String
        fun putString(key: String, value: String)
        fun getBoolean(key: String, default: Boolean): Boolean
        fun putBoolean(key: String, value: Boolean)
    }

    private val strings = mutableMapOf<String, EntryPreference<String>>()
    private val booleans = mutableMapOf<String, EntryPreference<Boolean>>()

    fun string(key: String, default: String = ""): EntryPreference<String> = strings.getOrPut(key) {
        EntryPreference(store.getString(key, default)) { store.putString(key, it) }
    }

    fun boolean(key: String, default: Boolean): EntryPreference<Boolean> = booleans.getOrPut(key) {
        EntryPreference(store.getBoolean(key, default)) { store.putBoolean(key, it) }
    }

    val startIntent get() = string(EntryPreferenceKeys.ENTRY_START_INTENT)
    val firstAction get() = string(EntryPreferenceKeys.ENTRY_FIRST_ACTION)
    val ageRange get() = string(EntryPreferenceKeys.AGE_RANGE)
    val onboardingCompleted get() = boolean(EntryPreferenceKeys.ONBOARDING_COMPLETED, false)
    val celebrationsEnabled get() = boolean(EntryPreferenceKeys.CELEBRATIONS_ENABLED, true)
    val progressVisible get() = boolean(EntryPreferenceKeys.PROGRESS_VISIBLE, true)

    /// L'interruptor d'«Avisos» amb tot el que el segueix: els missatges de Viri i la Live Activity
    /// de la ratxa (`ViraSimpleSettings.setNotifications`). Ajustos i l'entrada no poden quedar
    /// desaparellats.
    fun setNotifications(isOn: Boolean) {
        boolean(EntryPreferenceKeys.REMINDERS_ENABLED, true).value = isOn
        boolean(EntryPreferenceKeys.VIRI_MESSAGES_ENABLED, true).value = isOn
        boolean(EntryPreferenceKeys.STREAK_LIVE_ACTIVITY_ENABLED, true).value = isOn
    }

    companion object {
        fun shared(preferences: SharedPreferences) = EntryPreferences(object : Store {
            override fun getString(key: String, default: String) = preferences.getString(key, default) ?: default
            override fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
            override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
            override fun putBoolean(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
        })

        /// En memòria: captures i proves.
        fun inMemory(initial: Map<String, Any> = emptyMap()) = EntryPreferences(object : Store {
            private val values = initial.toMutableMap()
            override fun getString(key: String, default: String) = values[key] as? String ?: default
            override fun putString(key: String, value: String) { values[key] = value }
            override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        })
    }
}
