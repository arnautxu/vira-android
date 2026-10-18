package cat.vira.android.features.settings

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import kotlinx.coroutines.launch

/// «Com et vull motivar» (`SettingsMotivationSection`, `VIRI_AND_GAMIFICATION.md` §3.5): tota la
/// gamificació es pot apagar, i apagar-la no esborra res —l'XP mai baixa i el que s'ha comprat es
/// conserva—, només deixa de dibuixar-se.
@Composable
fun SettingsMotivationSection() {
    val backend = LocalSettingsBackend.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var celebrations by rememberBooleanPreference(ViraPreferences.CELEBRATIONS_ENABLED, true)
    var soundMode by rememberStringPreference(SettingsKeys.SOUND_MODE, ViraSoundMode.ALL.raw)
    var hapticsOn by rememberBooleanPreference(SettingsKeys.HAPTICS, true)
    var streakLive by rememberBooleanPreference(SettingsKeys.STREAK_LIVE_ACTIVITY, true)
    var challenges by rememberBooleanPreference(ViraPreferences.CHALLENGES_ENABLED, true)
    var progress by rememberBooleanPreference(SettingsKeys.PROGRESS_VISIBLE, true)
    var shop by rememberBooleanPreference(SettingsKeys.SHOP_VISIBLE, true)
    // La ratxa és un singleton creat sota demanda: l'ajust s'ha de poder apagar abans del primer dia.
    val streakVisible by backend.streakVisible.collectAsStateOr(true)

    val soundsTitle = stringResource(R.string.settings_sounds)
    val soundsHint = stringResource(R.string.settings_sounds_hint)

    SettingsGroup(header = stringResource(R.string.settings_motivation_section)) {
        row { SettingsToggleRow(stringResource(R.string.settings_motivation_celebrations), celebrations, { celebrations = it }) }
        // A part de les celebracions: hi ha qui vol veure-les a classe però no que sonin.
        row {
            SettingsChoiceRow(
                options = ViraSoundMode.entries,
                selected = ViraSoundMode.from(soundMode),
                optionLabel = { mode ->
                    stringResource(
                        when (mode) {
                            ViraSoundMode.ALL -> R.string.settings_sounds_all
                            ViraSoundMode.CELEBRATIONS -> R.string.settings_sounds_celebrations
                            ViraSoundMode.OFF -> R.string.settings_sounds_off
                        },
                    )
                },
                onSelect = { soundMode = it.raw },
            ) { SettingsLabel(soundsTitle, hint = soundsHint) }
        }
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_haptics), hapticsOn, {
                    hapticsOn = it
                    // Un interruptor de vibració que no vibra en tocar-lo no es pot comprovar.
                    if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                hint = stringResource(R.string.settings_motivation_haptics_hint),
            )
        }
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_streak), streakVisible,
                { visible -> scope.launch { runCatching { backend.setStreakVisible(visible) } } },
                hint = stringResource(R.string.settings_motivation_streak_hint),
            )
        }
        // L'avís de ratxa que aquí es programa en local (sense push): el fil de Notificacions el
        // retira quan aquest interruptor s'apaga.
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_streak_live), streakLive, { streakLive = it },
                hint = stringResource(R.string.settings_motivation_streak_live_hint),
            )
        }
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_challenges), challenges, { challenges = it },
                hint = stringResource(R.string.settings_motivation_challenges_hint),
            )
        }
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_progress), progress, { progress = it },
                hint = stringResource(R.string.settings_motivation_progress_hint),
            )
        }
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_motivation_shop), shop, { shop = it },
                hint = stringResource(R.string.settings_motivation_shop_hint),
            )
        }
    }
}
