package cat.vira.android.features.entry

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.features.settings.SettingsConnectCard
import cat.vira.android.features.settings.calendarAllowed
import cat.vira.android.features.settings.rememberCalendarConnection
import cat.vira.android.features.settings.rememberPermissionRequest
import cat.vira.android.core.design.tokens.ViraSpacing

/// Pas de connexions: «Connecta Vira amb el teu mòbil» (`EntryConnectionsStep.swift`). No és al
/// camí curt (`EntryStep.full`); hi és per al tutorial i per a qui el demani.
@Composable
fun EntryConnectionsStep(prefs: EntryPreferences, onContinue: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
        EntryConnectionToggles(prefs)
        EntryTogglesFootnote()
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)
    }
}

/// «Connecta» amb el calendari (`SettingsConnectCard`, Arnau, 2026-11-24): la mateixa targeta que a
/// dalt d'Ajustos, sense la capçalera perquè el pas ja porta el títol. Compartida amb el tutorial:
/// és la mateixa clau des dels tres llocs. El botó encén la clau i demana el permís en el mateix
/// toc; si es denega, es queda encesa i diu on es torna a donar, com a Ajustos.
@Composable
fun EntryConnectionToggles(prefs: EntryPreferences) {
    val context = LocalContext.current
    val calendar = prefs.boolean(EntryPreferenceKeys.CALENDAR_ENABLED, false)
    val (state, refresh) = rememberCalendarConnection(calendar.value)
    val requestPermission = rememberPermissionRequest { refresh() }
    SettingsConnectCard(
        calendar = state,
        onConnect = {
            calendar.value = true
            if (!calendarAllowed(context)) requestPermission(Manifest.permission.READ_CALENDAR)
        },
        onDisconnect = { calendar.value = false },
        showsHeader = false,
    )
}
