package cat.vira.android.features.settings

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.tokens.ViraArea

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
) {
    ACCOUNT(R.string.settings_category_account, R.string.settings_category_account_detail, ViraArea.Today, R.drawable.picto_set_account),
    NOTIFICATIONS(
        R.string.settings_notifications_section, R.string.settings_category_notifications_detail, ViraArea.Plan,
        R.drawable.picto_set_notifications,
    ),
    APP(R.string.settings_category_app, R.string.settings_category_app_detail, ViraArea.Focus, R.drawable.picto_set_general),
    PRIVACY(R.string.settings_category_privacy, R.string.settings_category_privacy_detail, ViraArea.Emotions, R.drawable.picto_set_privacy),
    ABOUT(R.string.settings_about_section, R.string.settings_category_about_detail, ViraArea.Today, null);

    /// El títol de la targeta de resum: diu què s'està comptant.
    @get:StringRes val heroTitle: Int get() = when (this) {
        APP -> R.string.settings_hero_app
        else -> title
    }

    companion object {
        /// Les famílies que surten en fitxes de color a la portada. El compte té la seva targeta a
        /// dalt i Sobre Vira va al peu: cap de les dues es configura com les altres.
        ///
        /// **Tres, no quatre** (Arnau, 2026-10-23): «Com et vull motivar» ha sortit. La ratxa, les
        /// monedes, els reptes, la botiga i les celebracions es veuen sempre, i el so i la vibració
        /// han anat a L'app (`ViraSimpleSettings`). La cerca també ha sortit: amb una vintena de
        /// controls ja no feia falta.
        val families = listOf(NOTIFICATIONS, APP, PRIVACY)

        /// Files blanques del peu de la portada.
        val footer = listOf(ABOUT)
    }
}

/// L'estat de cada família en una línia: el que surt al peu de la seva fitxa i a la targeta de
/// resum de dins. Els valors per defecte són **els mateixos** que a cada secció.
data class SettingsTally(
    val notifications: Boolean = true,
    val habitsModule: Boolean = true,
    val medicationModule: Boolean = true,
    val mindfulnessModule: Boolean = true,
    val lock: Boolean = false,
) {
    data class Count(val on: Int, val total: Int) {
        val fraction: Float get() = if (total == 0) 0f else on.toFloat() / total
    }

    /// Quants interruptors d'aquesta família estan encesos. `null` quan la família no es resumeix
    /// amb un recompte: Avisos és un sol interruptor, Privacitat diu si hi ha bloqueig, i Compte i
    /// Sobre Vira, res.
    fun count(category: SettingsCategory): Count? = when (category) {
        SettingsCategory.APP -> listOf(habitsModule, medicationModule, mindfulnessModule).let { Count(it.count { on -> on }, it.size) }
        else -> null
    }

    companion object {
        fun from(preferences: SharedPreferences) = SettingsTally(
            notifications = preferences.getBoolean(ViraPreferences.REMINDERS_ENABLED, true),
            habitsModule = preferences.getBoolean(SettingsKeys.MODULE_HABITS, true),
            medicationModule = preferences.getBoolean(SettingsKeys.MODULE_MEDICATION, true),
            mindfulnessModule = preferences.getBoolean(SettingsKeys.MODULE_MINDFULNESS, true),
            lock = preferences.getBoolean(ViraPreferences.BIOMETRIC_LOCK_ENABLED, false),
        )
    }
}

/// El text de la píndola: sempre en paraules, mai només un color.
@Composable
fun SettingsTally.summary(category: SettingsCategory): String? = when (category) {
    SettingsCategory.APP -> count(category)?.let { stringResource(R.string.settings_tally_modules, it.on, it.total) }
    // Un sol interruptor (Arnau, 2026-10-23): la píndola diu si és encès, no quants.
    SettingsCategory.NOTIFICATIONS ->
        stringResource(if (notifications) R.string.settings_tally_notifications_on else R.string.settings_tally_notifications_off)
    SettingsCategory.PRIVACY -> stringResource(if (lock) R.string.settings_tally_lock_on else R.string.settings_tally_lock_off)
    else -> null
}
