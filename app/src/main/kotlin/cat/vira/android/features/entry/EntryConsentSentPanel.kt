package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
import cat.vira.android.core.backend.ViraGuardianRequest.Delivery
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/// E8, després d'«Envia-li»: a qui ha anat el correu i què passa ara (`EntryConsentSentPanel.swift`,
/// Arnau, 2026-10-20).
///
/// El correu de la persona adulta surt **retallat** (`ma•••@gmail.com`): és el que el jove ja sap, i
/// la pantalla no l'ha d'ensenyar sencer a qui miri per sobre l'espatlla.
///
/// Tres maneres d'acabar aquí, i cap diu «enviat» sense que ho sigui:
/// - **`SENT`** — ha sortit.
/// - **`NOT_SENT`** — el permís s'ha desat però la funció del correu no ha respost
///   (`EntryConsentWriter`). Es pot tornar a provar ara o des d'Ajustos.
/// - **`LIMIT_REACHED`** — ja n'han sortit tres en 24 hores (s'hi arriba canviant el correu).
@Composable
fun EntryConsentSentPanel(
    services: EntryServices,
    receipt: ViraGuardianRequest,
    onChangeEmail: () -> Unit,
    onContinue: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var isResending by remember { mutableStateOf(false) }
    var resent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val guardian = receipt.guardian.orEmpty()
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val again = stringResource(R.string.account_consent_sent_again)

    /// Torna a enviar a la mateixa adreça. El rebut nou substitueix l'antic, i la fulla canvia sola
    /// si ara sí que ha sortit (o si s'ha arribat al límit).
    fun resend() {
        if (isResending) return
        isResending = true
        error = null
        scope.launch {
            try {
                val next = services.requestGuardianConsent(null)
                // La persona adulta ja ha respost mentre la fulla era oberta: no hi ha res a reenviar,
                // i «no ha sortit» seria mentida. Es continua amb la resposta desada.
                if (next.status != ViraConsentStatus.PENDING) {
                    isResending = false
                    onContinue()
                    return@launch
                }
                services.guardianReceipt.value = next
                resent = next.delivery == Delivery.SENT
                if (resent) view.announceForAccessibility(again)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: ViraBackendException) {
                error = failure.error
            } catch (_: Exception) {
                error = ViraBackendError.UNAVAILABLE
            }
            isResending = false
        }
    }

    val title = when (receipt.delivery) {
        Delivery.SENT -> stringResource(R.string.account_consent_sent_title)
        Delivery.NOT_SENT -> stringResource(R.string.account_consent_unsent_title)
        Delivery.LIMIT_REACHED -> stringResource(R.string.account_consent_limit_title)
    }
    val message = when (receipt.delivery) {
        Delivery.SENT -> stringResource(R.string.account_consent_sent_message, guardian)
        Delivery.NOT_SENT -> stringResource(R.string.account_consent_unsent_message)
        Delivery.LIMIT_REACHED -> stringResource(R.string.account_consent_limit_message, guardian)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryHeading(title, message)
        if (resent) {
            Text(again, style = ViraFont.footnote.copy(fontWeight = FontWeight.SemiBold), color = ink)
        }
        EntryErrorText(error)
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)

        val change = stringResource(R.string.account_consent_sent_change)
        val retry = stringResource(
            if (receipt.delivery == Delivery.SENT) R.string.account_consent_sent_resend else R.string.action_retry,
        )
        // Els dos enllaços de costat, com a la maqueta; amb lletra gran, un sota l'altre.
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(Modifier.fillMaxWidth()) {
                EntryLinkButton(change, onChangeEmail)
                if (receipt.canResend) EntryLinkButton(retry, ::resend)
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                EntryLinkButton(change, onChangeEmail, Modifier.weight(1f))
                if (receipt.canResend) EntryLinkButton(retry, ::resend, Modifier.weight(1f))
            }
        }
    }
}
