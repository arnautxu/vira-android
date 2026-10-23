package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraSpacing

/// Pas 1: la benvinguda (`EntryWelcomeStep.swift`, Arnau, 2026-10-21).
///
/// Viri saluda des de l'escena («Hola! Sóc Viri.») i la fulla diu què és Vira en una línia: el
/// logotip, **«El teu dia, més fàcil»** i les tres coses que fa. Una acció, «Som-hi», i les dues
/// sortides discretes de qui no ve a començar de zero: «Ja tinc compte» i «Sóc pare o mare».
///
/// Fins al 2026-10-21 el botó era «Vull començar a…» amb el final que canviava sol: amb el
/// missatge escrit a sobre ja no cal que el botó l'expliqui.
///
/// `onSignIn` i `onParent` són `null` sense backend: sense comptes no volen dir res.
@Composable
fun EntryWelcomeStep(onStart: () -> Unit, onSignIn: (() -> Unit)?, onParent: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            EntryLogo()
            EntryHeading(
                stringResource(R.string.onboarding_welcome_headline),
                stringResource(R.string.onboarding_welcome_message),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            EntryPrimaryButton(stringResource(R.string.onboarding_welcome_start), onStart)
            if (onSignIn != null) EntryLinkButton(stringResource(R.string.account_have_account_short), onSignIn)
            if (onParent != null) EntryParentLink(onParent)
        }
    }
}

/// "Sóc pare o mare": un enllaç discret a la benvinguda, amb el mateix pes que "Ja tinc compte".
/// Qui és pare o mare el troba, i el jove no el respon mai.
@Composable
fun EntryParentLink(onClick: () -> Unit) {
    EntryLinkButton(stringResource(R.string.account_role_parent_link), onClick)
}
