package cat.vira.android.features.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cat.vira.android.R

/// Mòduls (`SettingsModulesSection`, D-37): els mateixos tres interruptors del flux d'entrada,
/// perquè la promesa que hi fa («Ho pots canviar quan vulguis a Ajustos») sigui certa. Apagar-ne un
/// amaga el seu bloc i la seva cara; no n'esborra cap dada.
///
/// Més el **calendari del telèfon**, l'única connexió d'aquesta llista a Android: els recordatoris de
/// l'iPhone no hi tenen equivalent (pla, «El que a Android no serà igual»). En encendre'l es demana
/// el permís; si es denega, l'interruptor **es queda encès** i surt com a connexió bloquejada amb el
/// camí als ajustos, perquè apagar-lo amagaria el problema.
@Composable
fun SettingsModulesSection() {
    var habits by rememberBooleanPreference(SettingsKeys.MODULE_HABITS, true)
    var medication by rememberBooleanPreference(SettingsKeys.MODULE_MEDICATION, true)
    var mindfulness by rememberBooleanPreference(SettingsKeys.MODULE_MINDFULNESS, true)
    var calendar by rememberBooleanPreference(SettingsKeys.CALENDAR, false)

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    // Es torna a mirar en tornar a primer pla: el permís es pot treure des dels ajustos del telèfon.
    var revision by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    val requestPermission = rememberPermissionRequest { revision++ }
    val isCalendarBlocked = remember(revision, calendar) { !isPreview && calendar && !calendarAllowed(context) }

    SettingsGroup(
        header = stringResource(R.string.settings_modules_section),
        footer = stringResource(R.string.settings_modules_footer),
    ) {
        row { SettingsToggleRow(stringResource(R.string.plan_section_habits), habits, { habits = it }) }
        row { SettingsToggleRow(stringResource(R.string.plan_section_medication), medication, { medication = it }) }
        row { SettingsToggleRow(stringResource(R.string.emotions_mindfulness_title), mindfulness, { mindfulness = it }) }
        row {
            SettingsToggleRow(stringResource(R.string.settings_calendar_title), calendar, {
                calendar = it
                if (it && !calendarAllowed(context)) requestPermission(Manifest.permission.READ_CALENDAR)
            })
        }
        if (isCalendarBlocked) {
            row { SettingsBlockedRow(stringResource(R.string.settings_calendar_denied)) { openAppSettings(context) } }
        }
    }
}

private fun calendarAllowed(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

/// La porta als modes de concentració, dins de «L'app».
@Composable
fun SettingsFocusModesSection(onOpen: () -> Unit) {
    val title = stringResource(R.string.focus_modes_title)
    SettingsGroup(header = stringResource(R.string.settings_focus_section)) {
        row { SettingsNavigationRow(onOpen) { SettingsLabel(title, pictogram = R.drawable.picto_focus_modes) } }
    }
}
