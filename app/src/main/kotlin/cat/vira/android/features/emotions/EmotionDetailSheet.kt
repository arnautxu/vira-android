package cat.vira.android.features.emotions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// El full que s'obre en tocar una cara (`EmotionDetailSheet`). Només «Confirma» crea el
/// registre; cancel·lar o tancar-lo descarta el que s'hi hagi posat.
///
/// `onConfirm` rep el valor i una funció per dir si s'ha pogut desar: si no, el full es queda
/// obert amb l'error, perquè no es perdi el que l'usuari ha escrit.
@Composable
internal fun EmotionDetailSheet(
    initial: EmotionCheckInDraft,
    onDismiss: () -> Unit,
    onHelpNow: () -> Unit,
    onConfirm: (EmotionCheckInDraft, (Boolean) -> Unit) -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    EmotionSheet(onDismiss) {
        EmotionDetailContent(
            draft = draft,
            onChange = { draft = it },
            saveFailed = saveFailed,
            isSaving = isSaving,
            onCancel = onDismiss,
            onHelpNow = onHelpNow,
            onConfirm = {
                if (!isSaving) {
                    isSaving = true
                    onConfirm(draft) { saved ->
                        isSaving = false
                        saveFailed = !saved
                    }
                }
            },
        )
    }
}

@Composable
internal fun ColumnScope.EmotionDetailContent(
    draft: EmotionCheckInDraft,
    onChange: (EmotionCheckInDraft) -> Unit,
    saveFailed: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onHelpNow: () -> Unit,
    onConfirm: () -> Unit,
) {
    EmotionSheetBar(
        title = stringResource(R.string.tab_emotions),
        leading = stringResource(R.string.action_cancel) to onCancel,
    )
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(ViraSpacing.contentInset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Portrait(draft.emotion) { onChange(draft.copy(emotion = it, intensity = null)) }
        Text(
            stringResource(R.string.emotions_intensity_question),
            style = ViraFont.title,
            color = ViraColor.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        EmotionIntensityPicker(draft.intensity) { onChange(draft.copy(intensity = it)) }
        IntensityOptions(draft.intensity != null) { onChange(draft.copy(intensity = null)) }
        NoteField(draft.note) { onChange(draft.copy(note = it)) }
        if (draft.shouldOfferSupport) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                Text(stringResource(R.string.emotions_support_offer), style = ViraFont.body, color = ViraColor.textPrimary)
                ViraActionButton(
                    stringResource(R.string.action_help_now), onHelpNow,
                    hierarchy = ViraActionHierarchy.Secondary, area = ViraArea.Emotions,
                )
            }
        }
        if (saveFailed) ViraInlineError(stringResource(R.string.emotions_save_error), Modifier.fillMaxWidth())
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.backgroundGrouped)
            .padding(horizontal = ViraSpacing.contentInset, vertical = ViraSpacing.s)
            .imePadding(),
    ) {
        ViraActionButton(
            stringResource(R.string.emotions_checkin_confirm), onConfirm,
            modifier = Modifier.fillMaxWidth(), enabled = !isSaving,
        )
    }
}

/// La cara en moviment i, a sota, el menú per canviar d'emoció sense tancar el full.
@Composable
private fun Portrait(emotion: Emotion, onChange: (Emotion) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = stringResource(emotion.label)
    val change = stringResource(R.string.emotions_change)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        EmotionFaceAnimated(emotion, size = 112.dp)
        Box {
            Row(
                modifier = Modifier
                    .heightIn(min = ViraTouch.minimum)
                    .viraPressable(onClickLabel = change) { expanded = true }
                    .semantics(mergeDescendants = true) { contentDescription = change; stateDescription = name },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                Text(name, style = ViraFont.headline, color = ViraColor.textPrimary)
                EmotionGlyphIcon(
                    EmotionGlyph.Chevron, ViraColor.textSecondary, 14.dp * LocalDensity.current.fontScale,
                    Modifier.rotate(90f),
                )
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, containerColor = ViraColor.surfaceElevated) {
                Emotion.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label), style = ViraFont.body, color = ViraColor.textPrimary) },
                        onClick = { expanded = false; if (option != emotion) onChange(option) },
                    )
                }
            }
        }
    }
}

/// «Intensitat opcional» i, si n'hi ha una de triada, «Treu la selecció». Al costat si hi caben;
/// amb lletra gran, l'una sota l'altra (`ViewThatFits`).
@Composable
private fun IntensityOptions(hasSelection: Boolean, onClear: () -> Unit) {
    val label = @Composable {
        Text(stringResource(R.string.emotion_intensity_optional), style = ViraFont.footnote, color = ViraColor.textSecondary)
    }
    val clear = @Composable {
        if (hasSelection) {
            Box(
                Modifier.heightIn(min = ViraTouch.minimum).viraPressable(onClick = onClear),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(stringResource(R.string.emotion_intensity_clear), style = ViraFont.footnote, color = ViraColor.brandAccent)
            }
        }
    }
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(Modifier.fillMaxWidth().heightIn(min = ViraTouch.minimum), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            label(); clear()
        }
    } else {
        Row(
            Modifier.fillMaxWidth().heightIn(min = ViraTouch.minimum),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) { label(); clear() }
    }
}

@Composable
private fun NoteField(note: String, onChange: (String) -> Unit) {
    val label = stringResource(R.string.emotion_intensity_note_label)
    val placeholder = stringResource(R.string.emotions_note_placeholder)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Text(label, style = ViraFont.callout, color = ViraColor.textPrimary)
        BasicTextField(
            value = note,
            onValueChange = onChange,
            textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
            cursorBrush = SolidColor(ViraColor.accent(ViraArea.Emotions)),
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
                        .padding(ViraSpacing.m),
                ) {
                    if (note.isEmpty()) Text(placeholder, style = ViraFont.body, color = ViraColor.textSecondary)
                    field()
                }
            },
        )
    }
}
