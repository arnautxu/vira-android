package cat.vira.android.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.tokens.ViraColor

/// Porta d'entrada (`ViraOnboardingGate`): el flux d'entrada la primera vegada i, després, la porta
/// del compte. Cada porta mira una sola cosa i tapa la següent.
@Composable
fun ViraOnboardingGate(content: @Composable () -> Unit) {
    val backend = LocalViraBackend.current
    val completed by rememberPreference(backend.preferences, ViraPreferences.ONBOARDING_COMPLETED, false)
    when {
        // Sense flux d'entrada encara no hi hauria manera d'entrar a l'app de proves.
        !ViraFeatureSlots.ENTRY_READY -> content()
        // El flux marca la preferència ell mateix en acabar; l'escoltador fa la resta.
        !completed -> ViraFeatureSlots.Entry(ViraEntryRequest.Full, onFinish = {})
        else -> ViraAuthGate(content)
    }
}

/// Què hi ha entre el flux d'entrada i l'app: compte, permís del tutor, o res (`ViraAuthGate`).
///
/// No té pantalles pròpies: demana el **tros que falta del mateix flux** d'entrada.
/// - Sense backend configurat (`Unconfigured`), l'app funciona en local com sempre i deixa passar.
/// - Un compte de pare o mare no té pla, ni emocions, ni Viri: només el panell.
/// - Sense permís del tutor, el flux del permís: no és una pantalla informativa, és la condició de
///   `ViraSyncScope`. Amb el correu al tutor acabat d'enviar, el pas encara ha d'ensenyar a qui ha
///   anat (`ViraSession.guardianReceipt`).
@Composable
fun ViraAuthGate(content: @Composable () -> Unit) {
    val session = LocalViraBackend.current.session
    val recovering by session.isRecoveringPassword.collectAsStateWithLifecycle()
    val state by session.state.collectAsStateWithLifecycle()
    val receipt by session.guardianReceipt.collectAsStateWithLifecycle()

    if (recovering) {
        ViraFeatureSlots.Entry(ViraEntryRequest.NewPassword, onFinish = {})
        return
    }
    when (val current = state) {
        ViraSession.State.Loading -> AccountLoading()
        ViraSession.State.Unconfigured -> content()
        ViraSession.State.SignedOut -> ViraFeatureSlots.Entry(ViraEntryRequest.Account, onFinish = {})
        is ViraSession.State.SignedIn -> when {
            current.user.role == ViraAccountRole.PARENT -> ViraFeatureSlots.ParentRoot()
            current.user.consent == ViraConsentStatus.MISSING || receipt != null ->
                ViraFeatureSlots.Entry(ViraEntryRequest.Consent, onFinish = {})
            else -> content()
        }
    }
}

@Composable
private fun AccountLoading() {
    val label = stringResource(R.string.account_loading)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ViraColor.background)
            .semantics { contentDescription = label },
    ) {
        CircularProgressIndicator(color = ViraColor.brandAccent)
    }
}
