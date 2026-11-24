package cat.vira.android.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.assist.ViriAssist

/// Què surt a Vira (`SettingsModulesSection`, D-37): els mateixos interruptors del flux d'entrada,
/// perquè la promesa que hi fa («Ho pots canviar quan vulguis a Ajustos») sigui certa. Apagar-ne un
/// amaga el seu bloc i la seva cara; no n'esborra cap dada. A sota, els modes de Concentra't.
///
/// El calendari ja no hi és: ara té botó propi a «Connecta Vira amb el teu mòbil»
/// (`SettingsConnectSection`, Arnau, 2026-11-24).
@Composable
fun SettingsModulesSection(onOpenFocusModes: (() -> Unit)? = null) {
    var habits by rememberBooleanPreference(SettingsKeys.MODULE_HABITS, true)
    var medication by rememberBooleanPreference(SettingsKeys.MODULE_MEDICATION, true)
    var mindfulness by rememberBooleanPreference(SettingsKeys.MODULE_MINDFULNESS, true)
    var viriAssist by rememberBooleanPreference(ViriAssist.ENABLED, true)
    var viriAssistAccepted by rememberBooleanPreference(ViriAssist.ACCEPTED, false)
    val focusModes = stringResource(R.string.focus_modes_title)
    val focusModesHint = stringResource(R.string.settings_focus_modes_hint)

    SettingsGroup(
        header = stringResource(R.string.settings_what_shows_section),
        footer = stringResource(R.string.settings_modules_footer),
    ) {
        row { SettingsToggleRow(stringResource(R.string.plan_section_habits), habits, { habits = it }, pictogram = R.drawable.picto_habits) }
        row { SettingsToggleRow(stringResource(R.string.plan_section_medication), medication, { medication = it }, pictogram = R.drawable.picto_medication) }
        row { SettingsToggleRow(stringResource(R.string.emotions_mindfulness_title), mindfulness, { mindfulness = it }, pictogram = R.drawable.picto_moon) }
        // Les ajudes de Viri amb Claude (`ViriAssist`). Apagar-les també oblida el «D'acord»: si es
        // tornen a encendre, es torna a explicar què surt del telèfon.
        row {
            SettingsToggleRow(
                stringResource(R.string.viri_assist_settings_title), viriAssist,
                {
                    viriAssist = it
                    if (!it) viriAssistAccepted = false
                },
                hint = if (viriAssist) stringResource(R.string.viri_assist_settings_footer) else null,
                pictogram = R.drawable.picto_potion,
            )
        }
        if (onOpenFocusModes != null) {
            row {
                SettingsNavigationRow(onOpenFocusModes) {
                    SettingsLabel(focusModes, pictogram = R.drawable.picto_focus_modes, hint = focusModesHint)
                }
            }
        }
    }
}

/// La porta als modes de concentració, dins de «L'app».
@Composable
fun SettingsFocusModesSection(onOpen: () -> Unit) {
    val title = stringResource(R.string.focus_modes_title)
    SettingsGroup(header = stringResource(R.string.settings_focus_section)) {
        row { SettingsNavigationRow(onOpen) { SettingsLabel(title, pictogram = R.drawable.picto_focus_modes) } }
    }
}

/// Com es veu i com sona (Arnau, 2026-11-24): l'aparença i, a sota, el so i la vibració.
@Composable
fun SettingsLookSection() {
    SettingsGroup(header = stringResource(R.string.settings_look_section)) {
        row { SettingsAppearanceSwitcher(Modifier.padding(ViraSpacing.m)) }
        row { SettingsSoundsToggle() }
    }
}
