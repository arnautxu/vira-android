package cat.vira.android.features.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraSpacing

/// Pas de connexions: què deixa entrar Vira del telèfon (`EntryConnectionsStep.swift`). No és al
/// camí curt (`EntryStep.full`); hi és per al tutorial i per a qui el demani.
@Composable
fun EntryConnectionsStep(prefs: EntryPreferences, onContinue: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
        EntryConnectionToggles(prefs)
        EntryTogglesFootnote()
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)
    }
}

/// Els interruptors del que entra del telèfon. A Android només hi ha el calendari: Recordatoris no
/// es porta, i Health Connect arriba més endavant. Compartits amb el tutorial: és la mateixa clau
/// des dels dos llocs, i el permís es demana **en encendre'l**; si es denega, es torna a apagar sol.
@Composable
fun EntryConnectionToggles(prefs: EntryPreferences) {
    val context = LocalContext.current
    val calendar = prefs.boolean(EntryPreferenceKeys.CALENDAR_ENABLED, false)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendar.value = granted
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        EntryToggleRow(stringResource(R.string.settings_calendar_title), calendar.value, { isOn ->
            calendar.value = isOn
            if (!isOn) return@EntryToggleRow
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.READ_CALENDAR)
        })
    }
}
