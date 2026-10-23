package cat.vira.android.features.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.tokens.ViraRadius
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Pas de privacitat (`EntryPrivacyStep.swift`). Des del 2026-10-21, **tres fets curts amb icona**
/// en lloc d'un paràgraf: què se sincronitza, què no surt del telèfon sense permís i què **no** és
/// Vira (`settings.about.medical`, el mateix text de Medicació i Mindfulness). Tots tres a mida de cos.
///
/// Va **abans** del compte: demanar un correu abans de dir què se sincronitza seria demanar-lo a
/// cegues. El **bloqueig de l'app** s'ofereix aquí, però només si el telèfon té bloqueig de
/// pantalla: sense, deixaria l'usuari fora de la seva pròpia app.
///
/// Els documents legals encara no tenen adreça: surten com a "Pendent de publicar", sense enllaç.
@Composable
fun EntryPrivacyStep(prefs: EntryPreferences, canLockApp: Boolean, onContinue: () -> Unit) {
    val ink = ViraColor.textSecondary
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val lock = prefs.boolean(EntryPreferenceKeys.BIOMETRIC_LOCK_ENABLED, false)
    LaunchedEffect(canLockApp) { if (!canLockApp) lock.value = false }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            EntryPrivacyFact(EntryGlyph.SYNC, stringResource(R.string.onboarding_privacy_fact_sync))
            EntryPrivacyFact(EntryGlyph.LOCK, stringResource(R.string.onboarding_privacy_fact_sensitive))
            EntryPrivacyFact(EntryGlyph.INFO, stringResource(R.string.settings_about_medical))
        }
        if (canLockApp) {
            EntryToggleRow(stringResource(R.string.settings_privacy_lock), lock.value, { lock.value = it })
        }
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            listOf(
                EntryGlyph.PRIVACY to R.string.legal_privacy_title,
                EntryGlyph.DOCUMENT to R.string.legal_terms_title,
            ).forEach { (glyph, title) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ViraTouch.minimum)
                        .semantics(mergeDescendants = true) { },
                ) {
                    EntryGlyphIcon(glyph, ink)
                    // A mides d'accessibilitat l'estat baixa sota el títol: de costat, el títol
                    // es partia lletra a lletra.
                    if (stacked) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(title), style = ViraFont.footnote, color = ink)
                            Text(stringResource(R.string.legal_not_published), style = ViraFont.footnote, color = ink)
                        }
                    } else {
                        Text(stringResource(title), style = ViraFont.footnote, color = ink, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.legal_not_published), style = ViraFont.footnote, color = ink)
                    }
                }
            }
        }
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)
    }
}

/// Un fet de privacitat: icona lila en un quadrat lila clar i una frase a mida de cos.
@Composable
private fun EntryPrivacyFact(glyph: EntryGlyph, text: String) {
    val side = 32.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s), modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(side).background(ViraColor.surface(ViraArea.Today), RoundedCornerShape(ViraRadius.s)),
            contentAlignment = Alignment.Center,
        ) {
            EntryGlyphIcon(glyph, ViraColor.accent(ViraArea.Today), side = 16.dp)
        }
        Text(text, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
    }
}
