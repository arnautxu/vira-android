package cat.vira.android.features.settings

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.tokens.ViraArea
import java.text.Normalizer
import java.util.Locale

/// Les famílies d'Ajustos (`SettingsCategory` de l'iPhone, Arnau, 2026-08-07 i 2026-10-10).
///
/// L'agrupació respon a «què vull canviar», no a com està organitzat el codi: la sincronització va
/// amb el compte perquè sense compte no existeix, i el bloqueig va amb les dades perquè tots dos
/// responen a la mateixa pregunta —qui pot veure això.
///
/// El panell de proves de l'iPhone (`.dev`) no hi és: només existeix en depuració i els seus textos
/// són literals, fora del catàleg.
enum class SettingsCategory(
    @StringRes val title: Int,
    @StringRes val detail: Int,
    /// El color de cada família és la mateixa codificació cromàtica que orienta a la resta de l'app.
    val area: ViraArea,
    /// Sobre Vira no en porta: a l'iPhone és la icona nativa d'informació.
    @DrawableRes val pictogram: Int?,
    @StringRes private val keywords: Int,
) {
    ACCOUNT(
        R.string.settings_category_account, R.string.settings_category_account_detail, ViraArea.Today,
        R.drawable.picto_set_account, R.string.settings_search_keywords_account,
    ),
    NOTIFICATIONS(
        R.string.settings_notifications_section, R.string.settings_category_notifications_detail, ViraArea.Plan,
        R.drawable.picto_set_notifications, R.string.settings_search_keywords_notifications,
    ),
    APP(
        R.string.settings_category_app, R.string.settings_category_app_detail, ViraArea.Focus,
        R.drawable.picto_set_general, R.string.settings_search_keywords_app,
    ),
    MOTIVATION(
        R.string.settings_motivation_section, R.string.settings_category_motivation_detail, ViraArea.Rewards,
        R.drawable.picto_set_progress, R.string.settings_search_keywords_motivation,
    ),
    PRIVACY(
        R.string.settings_category_privacy, R.string.settings_category_privacy_detail, ViraArea.Emotions,
        R.drawable.picto_set_privacy, R.string.settings_search_keywords_privacy,
    ),
    ABOUT(
        R.string.settings_about_section, R.string.settings_category_about_detail, ViraArea.Today,
        null, R.string.settings_search_keywords_about,
    );

    /// El títol de la targeta de resum: diu què s'està comptant.
    @get:StringRes val heroTitle: Int get() = when (this) {
        NOTIFICATIONS -> R.string.settings_hero_notifications
        APP -> R.string.settings_hero_app
        MOTIVATION -> R.string.settings_hero_motivation
        else -> title
    }

    @Composable
    fun searchKeywords(): String = stringResource(keywords)

    companion object {
        /// Les quatre famílies que surten en fitxes de color a la portada. El compte té la seva
        /// targeta a dalt i Sobre Vira va al peu: cap de les dues es configura com les altres.
        val families = listOf(NOTIFICATIONS, APP, MOTIVATION, PRIVACY)

        /// Files blanques del peu de la portada.
        val footer = listOf(ABOUT)

        /// Sense majúscules ni accents: qui escriu «notificacio» ha de trobar «Notificacions».
        fun matches(query: String, keywords: String): Boolean {
            val needle = fold(query.trim())
            if (needle.isEmpty()) return false
            return fold(keywords).contains(needle)
        }

        private fun fold(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)
    }
}

/// L'estat de cada família en una línia: el que surt al peu de la seva fitxa i a la targeta de
/// resum de dins. Els valors per defecte són **els mateixos** que a cada secció.
data class SettingsTally(
    val reminders: Boolean = true,
    val habitReminders: Boolean = true,
    val medicationReminders: Boolean = true,
    val planReminders: Boolean = true,
    val emotionReminders: Boolean = true,
    val viriMessages: Boolean = true,
    val habitsModule: Boolean = true,
    val medicationModule: Boolean = true,
    val mindfulnessModule: Boolean = true,
    val celebrations: Boolean = true,
    val progressVisible: Boolean = true,
    val shopVisible: Boolean = true,
    val challenges: Boolean = true,
    val lock: Boolean = false,
) {
    data class Count(val on: Int, val total: Int) {
        val fraction: Float get() = if (total == 0) 0f else on.toFloat() / total
    }

    /// Quants interruptors d'aquesta família estan encesos. `null` quan la família no es resumeix
    /// amb un recompte (Privacitat diu si hi ha bloqueig; Compte i Sobre Vira, res).
    fun count(category: SettingsCategory): Count? = when (category) {
        SettingsCategory.NOTIFICATIONS -> {
            // Amb els recordatoris apagats, les quatre categories no sonen encara que estiguin
            // marcades: comptar-les seria dir que hi ha avisos que no arribaran.
            val categories = listOf(habitReminders, medicationReminders, planReminders, emotionReminders)
            val on = (if (reminders) categories.count { it } else 0) + (if (viriMessages) 1 else 0)
            Count(on, categories.size + 1)
        }
        SettingsCategory.APP -> listOf(habitsModule, medicationModule, mindfulnessModule).let { Count(it.count { on -> on }, it.size) }
        SettingsCategory.MOTIVATION ->
            listOf(celebrations, progressVisible, shopVisible, challenges).let { Count(it.count { on -> on }, it.size) }
        else -> null
    }

    companion object {
        fun from(preferences: SharedPreferences) = SettingsTally(
            reminders = preferences.getBoolean(ViraPreferences.REMINDERS_ENABLED, true),
            habitReminders = preferences.getBoolean(SettingsKeys.REMINDERS_HABITS, true),
            medicationReminders = preferences.getBoolean(SettingsKeys.REMINDERS_MEDICATION, true),
            planReminders = preferences.getBoolean(SettingsKeys.REMINDERS_PLAN, true),
            emotionReminders = preferences.getBoolean(SettingsKeys.REMINDERS_EMOTION, true),
            viriMessages = preferences.getBoolean(SettingsKeys.VIRI_MESSAGES, true),
            habitsModule = preferences.getBoolean(SettingsKeys.MODULE_HABITS, true),
            medicationModule = preferences.getBoolean(SettingsKeys.MODULE_MEDICATION, true),
            mindfulnessModule = preferences.getBoolean(SettingsKeys.MODULE_MINDFULNESS, true),
            celebrations = preferences.getBoolean(ViraPreferences.CELEBRATIONS_ENABLED, true),
            progressVisible = preferences.getBoolean(SettingsKeys.PROGRESS_VISIBLE, true),
            shopVisible = preferences.getBoolean(SettingsKeys.SHOP_VISIBLE, true),
            challenges = preferences.getBoolean(ViraPreferences.CHALLENGES_ENABLED, true),
            lock = preferences.getBoolean(ViraPreferences.BIOMETRIC_LOCK_ENABLED, false),
        )
    }
}

/// El text de la píndola: sempre en paraules, mai només un color.
@Composable
fun SettingsTally.summary(category: SettingsCategory): String? = when (category) {
    SettingsCategory.APP -> count(category)?.let { stringResource(R.string.settings_tally_modules, it.on, it.total) }
    SettingsCategory.PRIVACY -> stringResource(if (lock) R.string.settings_tally_lock_on else R.string.settings_tally_lock_off)
    else -> count(category)?.let { stringResource(R.string.settings_tally_on, it.on, it.total) }
}
