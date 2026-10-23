package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/// Recuperar la contrasenya, primera meitat: demanar l'enllaç (`EntryPasswordResetStep.swift`).
///
/// El correu es veu, es pot corregir i el botó diu què fa. **No diu mai si el correu existeix**:
/// dir "aquest correu no està registrat" seria explicar a qualsevol qui té compte a una app de
/// salut d'adolescents. Enviat, el formulari desapareix: el que toca és anar a la safata.
@Composable
fun EntryPasswordResetStep(services: EntryServices, email: String, onEmailChange: (String) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sessionError by services.lastError.collectAsState()
    var localError by remember { mutableStateOf<ViraBackendError?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }
    val ink = ViraColor.textPrimary
    val isValid = email.contains("@") && email.length > 4

    fun send() {
        if (isWorking) return
        if (!isValid) {
            localError = ViraBackendError.INVALID_EMAIL
            return
        }
        localError = null
        services.clearError()
        isWorking = true
        scope.launch {
            try {
                services.sendPasswordReset(email.trim())
                isSent = services.lastError.value == null
            } finally {
                isWorking = false
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        if (isSent) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    EntryGlyphIcon(EntryGlyph.ENVELOPE, ink)
                    Text(stringResource(R.string.account_password_reset_sent), style = ViraFont.body, color = ink)
                }
                Text(email, style = ViraFont.headline, color = ink)
            }
        } else {
            EntryField(
                label = stringResource(R.string.account_email_field),
                value = email,
                onValueChange = onEmailChange,
                kind = EntryFieldKind.EMAIL,
                placeholder = stringResource(R.string.account_field_placeholder),
                labelBold = true,
                imeAction = ImeAction.Go,
                onSubmit = ::send,
            )
            EntryPrimaryButton(
                stringResource(R.string.account_password_reset_submit),
                ::send,
                enabled = isValid && !isWorking,
                isLoading = isWorking,
            )
        }
        EntryErrorText(localError ?: sessionError)
        EntryLinkButton(stringResource(R.string.account_password_reset_back), onBack)
    }
}

/// Recuperar la contrasenya, segona meitat: escriure'n una de nova (`EntryNewPasswordStep.swift`).
///
/// La sessió ja és oberta: el correu ha fet de prova, i per això no hi ha "contrasenya actual".
/// **No es pot saltar**: sortir-ne deixaria l'usuari dins amb la contrasenya que ja no recorda.
@Composable
fun EntryNewPasswordStep(services: EntryServices, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val ink = ViraColor.textPrimary
    // Vuit caràcters és el mínim de Supabase per defecte, igual que a l'alta.
    val isValid = password.length >= 8 && password == confirmation

    fun submit() {
        if (!isValid || isWorking) return
        error = null
        isWorking = true
        scope.launch {
            try {
                services.updatePassword(password)
                onDone()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: ViraBackendException) {
                error = failure.error
            } catch (_: Exception) {
                error = ViraBackendError.UNAVAILABLE
            } finally {
                isWorking = false
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryField(
            label = stringResource(R.string.account_password_new_field),
            value = password,
            onValueChange = { password = it },
            kind = EntryFieldKind.NEW_PASSWORD,
            placeholder = stringResource(R.string.account_field_placeholder),
            labelBold = true,
            imeAction = ImeAction.Next,
        )
        EntryField(
            label = stringResource(R.string.account_password_new_repeat),
            value = confirmation,
            onValueChange = { confirmation = it },
            kind = EntryFieldKind.NEW_PASSWORD,
            placeholder = stringResource(R.string.account_field_placeholder),
            labelBold = true,
            imeAction = ImeAction.Go,
            onSubmit = ::submit,
        )
        // El desajust es diu **abans** d'enviar res i sense tenyir el camp: el color no és l'únic
        // senyal, i mentre s'escriu la segona és normal que encara no coincideixin.
        if (confirmation.isNotEmpty() && password != confirmation) {
            Text(
                stringResource(R.string.account_password_new_mismatch),
                style = ViraFont.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = ViraColor.error,
            )
        }
        EntryPrimaryButton(stringResource(R.string.account_password_new_submit), ::submit, enabled = isValid && !isWorking, isLoading = isWorking)
        Text(stringResource(R.string.account_password_new_footnote), style = ViraFont.footnote, color = ink)
        EntryErrorText(error)
    }
}
