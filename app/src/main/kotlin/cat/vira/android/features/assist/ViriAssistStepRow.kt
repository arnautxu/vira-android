package cat.vira.android.features.assist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

private val area = ViraArea.Plan

/// Un pas proposat (`ViriAssistStepRow`): marcar, canviar el text i veure els minuts en una
/// píndola. La marca és un check, a més del color. Treure'l és una acció de TalkBack; a la vista,
/// un pas desmarcat ja no es desa.
@Composable
fun ViriAssistStepRow(
    step: AssistanceStep,
    isLocked: Boolean,
    onToggle: () -> Unit,
    onEdit: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val removeLabel = stringResource(R.string.assistance_remove_step)
    val selectLabel = step.text.ifEmpty { stringResource(R.string.assistance_select_step) }
    val state = stringResource(if (step.isSelected) R.string.viri_assist_selected else R.string.viri_assist_unselected)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (step.isSelected) ViraColor.surface(area).copy(alpha = 0.55f) else Color.Transparent,
                RoundedCornerShape(ViraRadius.m),
            )
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs)
            .semantics {
                if (!isLocked) customActions = listOf(CustomAccessibilityAction(removeLabel) { onRemove(); true })
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Box(
            Modifier
                .size(ViraTouch.minimum)
                .viraPressable(role = Role.Checkbox, enabled = !isLocked, onClick = onToggle)
                .semantics { contentDescription = selectLabel; stateDescription = state },
            contentAlignment = Alignment.Center,
        ) {
            val mark = Modifier.size(28.dp)
            if (step.isSelected) {
                Box(mark.background(ViraColor.accent(area), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = ViraColor.onAccent, modifier = Modifier.size(18.dp))
                }
            } else {
                Box(mark.border(2.dp, ViraColor.controlBorder, CircleShape))
            }
        }
        Column(Modifier.weight(1f).padding(vertical = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            val style = if (step.isSelected) ViraFont.headline else ViraFont.body
            val placeholder = stringResource(R.string.viri_assist_steps_placeholder)
            val fieldLabel = stringResource(R.string.assistance_edit_step)
            BasicTextField(
                value = step.text,
                onValueChange = onEdit,
                enabled = !isLocked,
                textStyle = style.copy(color = ViraColor.textPrimary),
                cursorBrush = SolidColor(ViraColor.accent(area)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
                    .semantics { contentDescription = fieldLabel },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (step.text.isEmpty()) Text(placeholder, style = style, color = ViraColor.textSecondary)
                        inner()
                    }
                },
            )
            step.minutes?.let { minutes ->
                Row(
                    Modifier.background(ViraColor.surface(area), CircleShape)
                        .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                ) {
                    ViraPictogram(ViraPictogramName.focus, size = 18.dp, scalesWithText = true)
                    Text(stringResource(R.string.viri_assist_steps_minutes, minutes), style = ViraFont.footnoteEmphasis, color = ViraColor.textPrimary)
                }
            }
        }
    }
}
