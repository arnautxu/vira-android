package cat.vira.android.features.entry

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Peces de formulari de la fulla: camps, errors i els botons dels proveïdors (`EntryFormChrome.swift`).

/// Què escriu un camp: decideix el teclat i si s'amaga.
enum class EntryFieldKind { TEXT, NAME, EMAIL, PASSWORD, NEW_PASSWORD }

/// Camp de text del flux, amb **etiqueta persistent** a sobre: un marcador sol desapareix en
/// començar a escriure i obliga a recordar què demanava el camp.
@Composable
fun EntryField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    kind: EntryFieldKind = EntryFieldKind.TEXT,
    placeholder: String? = null,
    labelBold: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: () -> Unit = {},
) {
    val ink = ViraColor.textPrimary
    val isSecret = kind == EntryFieldKind.PASSWORD || kind == EntryFieldKind.NEW_PASSWORD
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        Text(label, style = if (labelBold) ViraFont.headline else ViraFont.footnote, color = ink)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = ViraFont.body.copy(color = ink),
            cursorBrush = SolidColor(ink),
            visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = if (kind == EntryFieldKind.NAME || kind == EntryFieldKind.TEXT) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                autoCorrectEnabled = kind == EntryFieldKind.TEXT || kind == EntryFieldKind.NAME,
                keyboardType = when (kind) {
                    EntryFieldKind.EMAIL -> KeyboardType.Email
                    EntryFieldKind.PASSWORD, EntryFieldKind.NEW_PASSWORD -> KeyboardType.Password
                    else -> KeyboardType.Text
                },
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ViraTouch.comfortable)
                        .entryFieldBorder()
                        .padding(ViraSpacing.m),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = ViraFont.body, color = ViraColor.textSecondary)
                    }
                    inner()
                }
            },
        )
    }
}

/// Separador entre el formulari de correu i els proveïdors externs: sense ell, Google semblava un
/// tercer pas del mateix formulari. El text es llegeix a TalkBack: és la mateixa informació.
@Composable
fun EntryMethodsDivider(text: String) {
    // A mides d'accessibilitat, sense línies: el text necessita tota l'amplada.
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Row(
        Modifier.fillMaxWidth().padding(top = ViraSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!stacked) Box(Modifier.weight(1f).height(1.dp).background(ViraColor.separator))
        Text(
            text,
            style = ViraFont.footnote,
            color = ViraColor.textSecondary,
            textAlign = TextAlign.Center,
            modifier = if (stacked) Modifier.weight(1f) else Modifier,
        )
        if (!stacked) Box(Modifier.weight(1f).height(1.dp).background(ViraColor.separator))
    }
}

/// Botó d'inici de sessió d'un proveïdor extern, ample, amb la frase sencera ("Continua amb Google").
///
/// El logotip de Google **no** es dibuixa a mà ni se substitueix per res que s'hi assembli: o hi és
/// el de debò, o no hi ha logotip. Encara no és als recursos d'Android.
@Composable
fun EntryProviderButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(ViraRadius.m)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .background(ViraColor.providerSurface, shape)
            .viraPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
    ) {
        Text(text, style = ViraFont.headline, color = ViraColor.onProviderSurface, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/// Missatge d'error del pas, a la fulla.
@Composable
fun EntryErrorText(error: ViraBackendError?) {
    if (error == null) return
    ViraInlineError(stringResource(error.entryMessage), Modifier.fillMaxWidth())
}

/// Error escrit en text, sense icona: "No s'ha pogut desar", "No coincideixen".
@Composable
fun EntryInlineWarning(text: String) {
    Text(
        text,
        style = ViraFont.callout.copy(fontWeight = FontWeight.Bold),
        color = ViraColor.error,
        modifier = Modifier.fillMaxWidth(),
    )
}

/// El text de cada error, del catàleg (`backend.error.*`).
@get:StringRes
val ViraBackendError.entryMessage: Int
    get() = when (this) {
        ViraBackendError.NOT_CONFIGURED -> R.string.backend_error_not_configured
        ViraBackendError.OFFLINE -> R.string.backend_error_offline
        ViraBackendError.INVALID_CREDENTIALS -> R.string.backend_error_credentials
        ViraBackendError.EMAIL_ALREADY_REGISTERED -> R.string.backend_error_email_taken
        ViraBackendError.WEAK_PASSWORD -> R.string.backend_error_weak_password
        ViraBackendError.INVALID_EMAIL -> R.string.backend_error_invalid_email
        ViraBackendError.EMAIL_NOT_CONFIRMED -> R.string.backend_error_email_not_confirmed
        ViraBackendError.TOO_MANY_ATTEMPTS -> R.string.backend_error_too_many_attempts
        ViraBackendError.SESSION_EXPIRED -> R.string.backend_error_session_expired
        ViraBackendError.NOT_ALLOWED -> R.string.backend_error_not_allowed
        ViraBackendError.CANCELLED -> R.string.backend_error_cancelled
        ViraBackendError.REJECTED -> R.string.backend_error_rejected
        ViraBackendError.GUARDIAN_IS_YOU -> R.string.backend_error_guardian_is_you
        ViraBackendError.UNAVAILABLE -> R.string.backend_error_unavailable
    }
