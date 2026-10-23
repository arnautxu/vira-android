package cat.vira.android.features.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// La fitxa del que acabes de fer, a la pantalla del compte (`EntrySavedChip.swift`, Arnau,
/// 2026-10-23). El títol diu «Guarda el que has fet»; la fitxa diu **què**: la tasca pel seu títol,
/// i un check-in o un focus sense detall (l'emoció no es posa en pantalla fora d'on toca).
///
/// Va amb una marca, no només amb color, i TalkBack hi diu «Es guarda: …».
@Composable
fun EntrySavedChip(outcome: EntryFirstActionOutcome, modifier: Modifier = Modifier) {
    val area = when (outcome) {
        is EntryFirstActionOutcome.Task -> ViraArea.Plan
        is EntryFirstActionOutcome.CheckIn -> ViraArea.Emotions
        is EntryFirstActionOutcome.Focus -> ViraArea.Focus
    }
    val text = when (outcome) {
        is EntryFirstActionOutcome.Task -> outcome.title.trim().ifEmpty { stringResource(R.string.onboarding_done_task) }
        is EntryFirstActionOutcome.CheckIn -> stringResource(R.string.onboarding_account_saved_checkin)
        is EntryFirstActionOutcome.Focus -> stringResource(R.string.onboarding_account_saved_focus)
    }
    val label = stringResource(R.string.onboarding_account_saved_a11y, text)
    val ink = ViraColor.onSurface(area)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(ViraColor.surface(area), CircleShape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        EntryGlyphIcon(EntryGlyph.CHECK_CIRCLE, ink)
        Text(text, style = ViraFont.headline, color = ink)
    }
}
