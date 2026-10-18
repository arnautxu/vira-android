package cat.vira.android.features.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Una durada de l'editor (`FocusDurationCard`): targeta verda amb el número gros i dos botons − i +
/// d'ample sencer. Per a TalkBack, un sol element ajustable.
@Composable
fun FocusDurationCard(
    title: String,
    minutes: Int,
    range: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    val ink = ViraColor.onCard(ViraArea.Focus)
    val value = stringResource(R.string.focus_minutes, minutes)
    Column(
        modifier
            .background(ViraGradient.card(ViraArea.Focus), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.s)
            .clearAndSetSemantics {
                contentDescription = title
                stateDescription = value
                progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), range.first.toFloat()..range.last.toFloat(), (range.last - range.first) / step - 1)
                setProgress { target ->
                    val next = minutes + if (target > minutes) step else -step
                    if (next in range) { onChange(next); true } else false
                }
            },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Text(title, style = ViraFont.footnoteEmphasis, color = ink)
        Text(value, style = ViraFont.largeTitle.copy(fontFeatureSettings = "tnum"), color = ink)
        Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            DurationButton(FocusGlyphKind.Minus, minutes - step in range, Modifier.weight(1f)) { onChange(minutes - step) }
            DurationButton(FocusGlyphKind.Plus, minutes + step in range, Modifier.weight(1f)) { onChange(minutes + step) }
        }
    }
}

@Composable
private fun DurationButton(glyph: FocusGlyphKind, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated, CircleShape)
            .viraPressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FocusGlyph(glyph, ViraColor.textPrimary)
    }
}
