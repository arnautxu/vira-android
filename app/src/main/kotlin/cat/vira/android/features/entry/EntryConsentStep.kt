package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.launch

/// E8 · El permís del tutor (`EntryConsentStep.swift`).
///
/// - **12–15**: correu del tutor → `guardian_consents` → pendent.
/// - **16+**: `not_required` al servidor directament, **sense pantalla**: el pas escriu en entrar i
///   avança. Mentre treballa es veu un indicador, i si falla, l'error i "Torna-hi". **No avança
///   mai sense haver escrit** (`EntryConsentWriter`).
/// - **Sense tram** (un telèfon nou, un canvi de compte): el tram es demana aquí mateix.
///
/// **No diu que s'hagi enviat cap correu**, perquè no se n'envia cap: només desa a qui s'ha de
/// demanar.
@Composable
fun EntryConsentStep(services: EntryServices, onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ageRange = services.prefs.ageRange
    val writer = remember(services) { EntryConsentWriter(services::declareConsent) }
    var guardianEmail by rememberSaveable { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    // El tram es demana dins del pas només si hi faltava en entrar-hi: un cop triat, la pantalla
    // no ha de desaparèixer sota el dit de qui l'acaba de triar.
    var asksAge by remember { mutableStateOf(EntryConsentWriter.plan(ageRange.value) == EntryConsentWriter.Plan.NEEDS_AGE) }
    val plan = EntryConsentWriter.plan(ageRange.value)
    val ink = ViraColor.onCard(ViraArea.Rewards)

    fun submit() {
        if (isWorking) return
        error = null
        isWorking = true
        scope.launch {
            error = writer.submit(ageRange.value, guardianEmail.trim(), onContinue)
            isWorking = false
        }
    }

    LaunchedEffect(Unit) { if (plan == EntryConsentWriter.Plan.GRANT) submit() }

    if (plan == EntryConsentWriter.Plan.GRANT && !asksAge) {
        // 16+: no hi ha res a preguntar. Només es veu si triga o si falla.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            val failure = error
            if (failure != null) {
                EntryErrorText(failure)
                EntryPrimaryButton(stringResource(R.string.action_retry), ::submit)
            } else {
                val loading = stringResource(R.string.account_loading)
                Box(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable).semantics { contentDescription = loading },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ink, modifier = Modifier.size(36.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryViriLine(stringResource(R.string.onboarding_viri_guardian))
        EntryHeading(stringResource(EntryStep.CONSENT.title), EntryStep.CONSENT.message?.let { stringResource(it) })
        if (asksAge) {
            Text(stringResource(R.string.onboarding_age_title), style = ViraFont.footnote, color = ink)
            EntryAgeChoice(ageRange, allowsClearing = false)
        }
        if (plan == EntryConsentWriter.Plan.ASK_GUARDIAN) {
            EntryField(
                label = stringResource(R.string.account_consent_email_label),
                value = guardianEmail,
                onValueChange = { guardianEmail = it },
                kind = EntryFieldKind.EMAIL,
                onSubmit = ::submit,
            )
        }
        EntryErrorText(error)
        val isValid = when (plan) {
            EntryConsentWriter.Plan.NEEDS_AGE -> false
            EntryConsentWriter.Plan.GRANT -> true
            EntryConsentWriter.Plan.ASK_GUARDIAN -> guardianEmail.contains("@")
        }
        EntryPrimaryButton(
            stringResource(if (plan == EntryConsentWriter.Plan.ASK_GUARDIAN) R.string.account_consent_submit else R.string.action_continue),
            ::submit,
            enabled = isValid && !isWorking,
            isLoading = isWorking,
        )
        Text(stringResource(R.string.account_consent_footnote), style = ViraFont.footnote, color = ink)
    }
}

/// E9 · Com vols que et digui? Un camp i prou, opcional. Escriu a `ViraProfile.displayName`, el
/// mateix camp del Perfil. **Mai nom real**: la pista del camp ho diu.
@Composable
fun EntryNicknameStep(services: EntryServices, onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf("") }
    var stored by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        stored = services.displayName()
        if (draft.isEmpty()) draft = stored
    }

    fun save() {
        val trimmed = draft.trim()
        scope.launch {
            if (trimmed != stored) services.setDisplayName(trimmed)
            onContinue()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryField(
            label = stringResource(R.string.profile_nickname_placeholder),
            value = draft,
            onValueChange = { draft = it },
            kind = EntryFieldKind.NAME,
            onSubmit = ::save,
        )
        EntryPrimaryButton(stringResource(R.string.action_continue), ::save)
    }
}
