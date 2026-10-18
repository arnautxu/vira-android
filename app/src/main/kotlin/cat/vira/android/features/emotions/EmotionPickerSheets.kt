package cat.vira.android.features.emotions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Vocabulari secundari del check-in (`EmotionOtherPickerSheet`). Només surt quan l'usuari no
/// s'ha reconegut en les cares principals; cada elecció segueix el mateix flux que una d'aquelles.
@Composable
internal fun EmotionOtherPickerSheet(emotions: List<Emotion>, onDismiss: () -> Unit, onSelect: (Emotion) -> Unit) {
    EmotionSheet(onDismiss) { EmotionOtherPickerContent(emotions, onDismiss, onSelect) }
}

@Composable
internal fun ColumnScope.EmotionOtherPickerContent(emotions: List<Emotion>, onClose: () -> Unit, onSelect: (Emotion) -> Unit) {
    EmotionSheetBar(stringResource(R.string.emotions_other_title), stringResource(R.string.action_close) to onClose)
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.l),
    ) {
        EmotionFaceGrid(emotions.size) { index, size -> EmotionButton(emotions[index], size) { onSelect(emotions[index]) } }
    }
}

/// Quines cares surten primer (`EmotionPrimaryPickerSheet`). El catàleg sencer hi és sempre: una
/// cara que deixa de ser principal passa sola a «Una altra», i en cal almenys una.
@Composable
internal fun EmotionPrimaryPickerSheet(initialSelection: List<Emotion>, onDismiss: () -> Unit, onSave: (List<Emotion>) -> Unit) {
    var selection by remember { mutableStateOf(initialSelection) }
    EmotionSheet(onDismiss) {
        EmotionPrimaryPickerContent(selection, { selection = it }, onDismiss) { onSave(selection) }
    }
}

@Composable
internal fun ColumnScope.EmotionPrimaryPickerContent(
    selection: List<Emotion>,
    onChange: (List<Emotion>) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    EmotionSheetBar(
        stringResource(R.string.emotions_customize_title),
        leading = stringResource(R.string.action_cancel) to onCancel,
        trailing = stringResource(R.string.action_done) to onDone,
        trailingEnabled = selection.isNotEmpty(),
    )
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
    ) {
        Text(stringResource(R.string.emotions_customize_hint), style = ViraFont.body, color = ViraColor.textSecondary)
        Text(stringResource(R.string.emotions_customize_count, selection.size), style = ViraFont.footnote, color = ViraColor.textSecondary)
        // Dues columnes de 148 com a mínim; amb lletra d'accessibilitat, una.
        val columns = if (LocalViraEnvironment.current.isAccessibilitySize) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Emotion.entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    row.forEach { emotion ->
                        Box(Modifier.weight(1f)) {
                            PrimaryOption(emotion, selection) { toggled ->
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onChange(toggled)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PrimaryOption(emotion: Emotion, selection: List<Emotion>, onToggle: (List<Emotion>) -> Unit) {
    val isSelected = emotion in selection
    val isLast = isSelected && selection.size == 1
    val shape = RoundedCornerShape(ViraRadius.m)
    val state = stringResource(if (isSelected) R.string.emotions_customize_selected else R.string.emotions_customize_not_selected)
    val hint = stringResource(if (isLast) R.string.emotions_customize_minimum_hint else R.string.emotions_customize_toggle_hint)
    val accent = ViraColor.accent(ViraArea.Emotions)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .background(if (isSelected) ViraColor.surface(ViraArea.Emotions) else ViraColor.surfaceElevated, shape)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else ViraColor.controlBorder, shape)
            .viraPressable(onClickLabel = hint, enabled = !isLast) {
                onToggle(if (isSelected) selection - emotion else selection + emotion)
            }
            .semantics(mergeDescendants = true) { selected = isSelected; stateDescription = state }
            .padding(ViraSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            EmotionFace(emotion, 76.dp)
            val check = 20.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
            Box(Modifier.background(ViraColor.surfaceElevated, CircleShape)) {
                EmotionGlyphIcon(
                    if (isSelected) EmotionGlyph.Checked else EmotionGlyph.Unchecked,
                    if (isSelected) accent else ViraColor.textSecondary,
                    check,
                )
            }
        }
        Text(
            stringResource(emotion.label),
            style = if (isSelected) ViraFont.headline else ViraFont.body,
            color = if (isSelected) ViraColor.onSurface(ViraArea.Emotions) else ViraColor.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}
