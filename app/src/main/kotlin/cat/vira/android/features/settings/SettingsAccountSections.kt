package cat.vira.android.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.settings.export.ViraResourceNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/// El text d'un error del servidor, en el vocabulari de Vira (`backend.error.*`).
@Composable
fun backendErrorMessage(error: ViraBackendError): String =
    ViraResourceNames.string(LocalContext.current, error.messageKey)

/// Què es copia al compte: el que surt sempre, els dos interruptors sensibles i el botó de
/// sincronitzar (`SettingsSyncSection`). Tot el que hi diu ha de dir la veritat sense endolcir-la:
/// qui els activa ha de saber què està activant (`PRIVACY_AND_SAFETY.md` §4.5b).
///
/// Sense permís del tutor, els interruptors ni tan sols es poden tocar: que estiguessin actius i no
/// fessin res seria pitjor que no tenir-los. El que en decideix la sortida és `ViraSyncScope`, que
/// llegeix el permís del servidor; aquí només es reflecteix.
@Composable
fun SettingsSyncSection() {
    val backend = LocalSettingsBackend.current
    val user = rememberSettingsUser() ?: return
    val sync by backend.syncState.collectAsState()
    var syncEmotions by rememberBooleanPreference(ViraPreferences.SYNC_EMOTIONS_ENABLED, ViraPreferences.SYNC_EMOTIONS_DEFAULT)
    var syncMedication by rememberBooleanPreference(ViraPreferences.SYNC_MEDICATION_ENABLED, ViraPreferences.SYNC_MEDICATION_DEFAULT)
    val scope = rememberCoroutineScope()
    val hasConsent = user.consent == ViraConsentStatus.GRANTED
    val needsConsent = if (hasConsent) null else stringResource(R.string.settings_sync_needs_consent)
    val failure = (sync as? ViraSyncEngine.State.Failed)?.let { backendErrorMessage(it.error) }

    val alwaysTitle = stringResource(R.string.settings_sync_always_title)
    val alwaysValue = stringResource(R.string.settings_sync_always_value)
    val emotions = stringResource(R.string.settings_sync_emotions_short)
    val emotionsSpoken = stringResource(R.string.settings_sync_emotions)
    val medication = stringResource(R.string.settings_sync_medication_short)
    val medicationSpoken = stringResource(R.string.settings_sync_medication)
    val syncNow = stringResource(R.string.settings_sync_now)

    SettingsGroup(
        header = stringResource(R.string.settings_sync_copied_section),
        footer = stringResource(if (hasConsent) R.string.settings_sync_sensitive_footnote else R.string.settings_sync_consent_blocked),
    ) {
        // El que viatja sempre, dit en una fila: sense ella, els dos interruptors feien pensar
        // que la resta tampoc no sortia del telèfon.
        row { SettingsValueRow(alwaysValue) { SettingsLabel(alwaysTitle, pictogram = R.drawable.picto_organization) } }
        row {
            SettingsToggleRow(syncEmotions, { syncEmotions = it }, enabled = hasConsent, spokenLabel = emotionsSpoken) {
                SettingsLabel(emotions, pictogram = R.drawable.picto_journal, hint = needsConsent)
            }
        }
        row {
            SettingsToggleRow(syncMedication, { syncMedication = it }, enabled = hasConsent, spokenLabel = medicationSpoken) {
                SettingsLabel(medication, pictogram = R.drawable.picto_medication, hint = needsConsent)
            }
        }
        // El motiu, quan n'hi ha: «No s'ha pogut» tot sol no diu si cal cobertura, tornar a entrar
        // o simplement esperar.
        if (failure != null) row { SettingsNoteRow(failure, color = ViraColor.error) }
        row {
            SettingsActionRow(
                syncNow,
                onClick = { scope.launch { runCatching { backend.syncNow() } } },
                enabled = sync !is ViraSyncEngine.State.Syncing,
                glyph = SettingsGlyphKind.Sync,
            )
        }
    }
}

/// Entrada al panell de pares (`PRIVACY_AND_SAFETY.md` §4.5b, D-38). La vinculació real queda
/// fora de les builds públiques fins que es resolgui D-29: la fila hi és per explicar-ne l'estat.
@Composable
fun SettingsParentPanelSection(isAvailable: Boolean, onOpen: () -> Unit) {
    val title = stringResource(R.string.settings_parent_panel_title)
    val hint = if (isAvailable) null else stringResource(R.string.settings_parent_panel_pending_short)
    SettingsGroup(header = stringResource(R.string.settings_parent_panel_section)) {
        row { SettingsNavigationRow(onOpen) { SettingsLabel(title, pictogram = R.drawable.picto_habit_friends, hint = hint) } }
    }
}

/// Tancar la sessió i esborrar el compte, al final del compte (`SettingsSessionSection`).
///
/// L'esborrat del compte i el de les dades locals van junts a propòsit: deixar les dades al
/// telèfon després d'esborrar el compte del servidor seria una mitja veritat.
@Composable
fun SettingsSessionSection(onFinished: () -> Unit) {
    val backend = LocalSettingsBackend.current
    rememberSettingsUser() ?: return
    val scope = rememberCoroutineScope()
    var isConfirmingDelete by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val signOut = stringResource(R.string.account_settings_sign_out)
    val delete = stringResource(R.string.account_settings_delete)
    val errorText = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { backendErrorMessage(it) }

    SettingsGroup(
        header = stringResource(R.string.settings_session_section),
        footer = stringResource(R.string.account_settings_delete_footnote),
    ) {
        row {
            SettingsActionRow(signOut, enabled = !isWorking, onClick = {
                isWorking = true
                scope.launch {
                    runCatching { backend.signOut() }
                    isWorking = false
                    onFinished()
                }
            })
        }
        row { SettingsActionRow(delete, destructive = true, enabled = !isWorking, onClick = { isConfirmingDelete = true }) }
        if (isWorking) {
            row {
                Box(Modifier.fillMaxWidth().padding(ViraSpacing.s), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViraColor.accent(LocalSettingsArea.current))
                }
            }
        }
        if (errorText != null) row { ViraInlineError(errorText, Modifier.padding(ViraSpacing.m)) }
    }

    if (isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            title = { Text(stringResource(R.string.account_settings_delete_confirm_title), style = ViraFont.headline) },
            text = { Text(stringResource(R.string.account_settings_delete_confirm_message), style = ViraFont.body) },
            confirmButton = {
                TextButton(onClick = {
                    isConfirmingDelete = false
                    error = null
                    isWorking = true
                    scope.launch {
                        error = attempt { backend.deleteAccount() }
                        isWorking = false
                        if (error == null) onFinished()
                    }
                }) { Text(delete, color = ViraColor.error, style = ViraFont.headline) }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingDelete = false }) { Text(stringResource(R.string.action_cancel), style = ViraFont.body) }
            },
        )
    }
}

/// Fa la feina i en torna l'error en el vocabulari de Vira, o `null` si ha anat bé.
suspend fun attempt(work: suspend () -> Unit): ViraBackendError? = try {
    work()
    null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: ViraBackendException) {
    failure.error
} catch (_: Exception) {
    ViraBackendError.UNAVAILABLE
}
