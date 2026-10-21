package cat.vira.android.features.entry

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch

/// E7: entrar o crear el compte (`EntryAccountStep.swift`). Va **després** de la primera acció: es
/// presenta com a guardar el que s'acaba de fer, i no té "Més tard".
///
/// Una sola pantalla per als dos casos: el que canvia entre entrar i registrar-se és **el botó de
/// la capçalera** (`EntryNavigator.toggleSigningUp`), no la pantalla. El rol no es tria aquí: el
/// fixa l'enllaç "Sóc pare o mare" de la benvinguda.
///
/// Apple no hi és: a l'iPhone és el botó propi del sistema, que Android no té, i entrar amb Apple
/// per web encara no està configurat. Recuperar la contrasenya té pantalla pròpia.
@Composable
fun EntryAccountStep(
    services: EntryServices,
    role: ViraAccountRole,
    email: String,
    onEmailChange: (String) -> Unit,
    isSigningUp: Boolean,
    onForgotPassword: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val error by services.lastError.collectAsState()
    var password by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var showsConfirmNotice by remember { mutableStateOf(false) }
    val ink = ViraColor.onCard(ViraArea.Rewards)
    // Vuit caràcters és el mínim de Supabase per defecte: val més dir-ho abans d'enviar-ho.
    val isValid = email.contains("@") && email.length > 4 && password.length >= 8

    fun perform(work: suspend () -> Unit) {
        services.clearError()
        isWorking = true
        scope.launch {
            try { work() } finally { isWorking = false }
        }
    }

    fun submit() {
        if (!isValid || isWorking) return
        showsConfirmNotice = false
        if (!isSigningUp) return perform { services.signIn(email.trim(), password) }
        // L'alta no obre sessió tota sola: Supabase envia un correu de confirmació primer. Val més
        // dir-ho que deixar l'usuari mirant un botó que sembla que no ha fet res.
        perform {
            val opened = services.signUp(email.trim(), password, role)
            if (!opened && services.lastError.value == null) showsConfirmNotice = true
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryField(
            label = stringResource(R.string.account_email_field),
            value = email,
            onValueChange = onEmailChange,
            kind = EntryFieldKind.EMAIL,
            placeholder = stringResource(R.string.account_field_placeholder),
            labelBold = true,
            imeAction = ImeAction.Next,
        )
        EntryField(
            label = stringResource(R.string.account_password_field),
            value = password,
            onValueChange = { password = it },
            kind = if (isSigningUp) EntryFieldKind.NEW_PASSWORD else EntryFieldKind.PASSWORD,
            placeholder = stringResource(R.string.account_field_placeholder),
            labelBold = true,
            imeAction = ImeAction.Go,
            onSubmit = ::submit,
        )
        EntryPrimaryButton(
            stringResource(if (isSigningUp) R.string.account_email_submit_sign_up else R.string.account_email_submit_sign_in),
            ::submit,
            enabled = isValid && !isWorking,
            isLoading = isWorking,
        )
        if (!isSigningUp) {
            EntryLinkButton(stringResource(R.string.account_password_forgot), onForgotPassword)
        }
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            EntryMethodsDivider(stringResource(R.string.account_methods_divider))
            EntryProviderButton(stringResource(R.string.account_sign_in_google), {
                val activity = context.findActivity() ?: return@EntryProviderButton
                perform { services.signInWithGoogle(activity) }
            }, enabled = !isWorking)
        }
        if (showsConfirmNotice) {
            Text(stringResource(R.string.account_email_confirm_message), style = ViraFont.footnote, color = ink)
        }
        EntryErrorText(error)
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
