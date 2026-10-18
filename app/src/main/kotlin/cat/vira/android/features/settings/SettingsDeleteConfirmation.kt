package cat.vira.android.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch

/// La paraula escrita encaixa amb la demanada, sense distingir majúscules ni espais als extrems.
fun eraseWordMatches(typed: String, word: String): Boolean = typed.trim().equals(word, ignoreCase = true)

/// «Confirmació escrita» per a l'esborrat total (`USER_FLOWS.md` F25). No hi ha desfer després
/// d'això, i un diàleg de sí o no es toca sense llegir-lo: calia més fricció que una confirmació.
///
/// L'esborrat passa per la sincronització (`ViraSyncEngine.eraseEverything`): amb compte, la còpia
/// del servidor tornaria sola. Si el servidor no s'ha pogut buidar, el telèfon tampoc s'ha tocat:
/// el full es queda obert amb el motiu, en comptes de tancar-se dient que sí.
@Composable
fun SettingsDeleteConfirmation(onDismiss: () -> Unit, onErased: () -> Unit) {
    val backend = LocalSettingsBackend.current
    val scope = rememberCoroutineScope()
    val word = stringResource(R.string.settings_data_erase_word)
    var typed by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val focus = remember { FocusRequester() }
    val isPreview = LocalInspectionMode.current
    val canConfirm = eraseWordMatches(typed, word) && !isWorking
    val errorText = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { backendErrorMessage(it) }

    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(ViraColor.background).safeDrawingPadding().imePadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isWorking) {
                        Text(stringResource(R.string.action_cancel), style = ViraFont.body, color = ViraColor.textPrimary)
                    }
                }
                Column(
                    Modifier.padding(horizontal = ViraSpacing.m),
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                ) {
                    Text(
                        stringResource(R.string.settings_data_erase),
                        style = ViraFont.title,
                        color = ViraColor.textPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    SettingsGroup {
                        row { SettingsNoteRow(stringResource(R.string.settings_data_erase_warning), color = ViraColor.textPrimary) }
                    }
                    SettingsGroup(header = stringResource(R.string.settings_data_erase_prompt, word)) {
                        row {
                            OutlinedTextField(
                                value = typed,
                                onValueChange = { typed = it },
                                placeholder = { Text(word, style = ViraFont.body) },
                                singleLine = true,
                                enabled = !isWorking,
                                textStyle = ViraFont.body,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                                modifier = Modifier.fillMaxWidth().padding(ViraSpacing.s).focusRequester(focus),
                            )
                        }
                    }
                    SettingsGroup {
                        row {
                            SettingsActionRow(
                                stringResource(R.string.settings_data_erase),
                                destructive = true,
                                enabled = canConfirm,
                                onClick = {
                                    error = null
                                    isWorking = true
                                    scope.launch {
                                        error = attempt { backend.eraseEverything() }
                                        isWorking = false
                                        if (error == null) onErased()
                                    }
                                },
                            )
                        }
                    }
                    if (errorText != null) ViraInlineError(errorText)
                }
            }
            if (isWorking) CircularProgressIndicator(Modifier.align(Alignment.Center), color = ViraColor.error)
        }
    }
    LaunchedEffect(Unit) { if (!isPreview) runCatching { focus.requestFocus() } }
}
