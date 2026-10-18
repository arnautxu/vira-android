package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/// Pas de privacitat: el comportament real i l'avís clínic, **a mida de cos** (`EntryPrivacyStep.swift`).
/// Reutilitza `settings.about.medical`, el mateix text de Medicació i Mindfulness.
///
/// Va **abans** del compte: demanar un correu abans de dir què se sincronitza seria demanar-lo a
/// cegues. El **bloqueig de l'app** s'ofereix aquí, però només si el telèfon té bloqueig de
/// pantalla: sense, deixaria l'usuari fora de la seva pròpia app.
///
/// Els documents legals encara no tenen adreça: surten com a "Pendent de publicar", sense enllaç.
@Composable
fun EntryPrivacyStep(prefs: EntryPreferences, canLockApp: Boolean, onContinue: () -> Unit) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val lock = prefs.boolean(EntryPreferenceKeys.BIOMETRIC_LOCK_ENABLED, false)
    LaunchedEffect(canLockApp) { if (!canLockApp) lock.value = false }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
        Text(
            stringResource(R.string.settings_about_medical),
            style = ViraFont.footnote,
            color = ink,
            modifier = Modifier.fillMaxWidth().entryFieldBorder().padding(ViraSpacing.m),
        )
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
