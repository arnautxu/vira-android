package cat.vira.android.features.add

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// L'etiqueta d'un grup de píndoles («Les de sempre», «Idees»).
@Composable
fun AddGroupTitle(text: String) {
    Text(text, style = ViraFont.headline, color = ViraColor.textSecondary, modifier = Modifier.semantics { heading() })
}

/// Una píndola d'un toc (`QuickCaptureChip`): pictograma i títol sobre blanc.
@Composable
fun AddChip(title: String, pictogram: String, onClick: () -> Unit) {
    Row(
        Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated, CircleShape)
            .viraPressable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(pictogram, size = 22.dp, scalesWithText = true)
        Text(title, style = ViraFont.callout, color = ViraColor.textPrimary)
    }
}

/// Una píndola amb creu: el que ha llegit la frase i es pot treure.
@Composable
fun RemovableChip(text: String, onRemove: () -> Unit) {
    val label = stringResource(R.string.add_detected_remove, text)
    Row(
        Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surface(ViraArea.Plan), CircleShape)
            .viraPressable(role = Role.Button, onClick = onRemove)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Text(text, style = ViraFont.footnoteEmphasis, color = ViraColor.onSurface(ViraArea.Plan))
        Icon(Icons.Filled.Close, contentDescription = null, tint = ViraColor.onSurface(ViraArea.Plan), modifier = Modifier.size(14.dp))
    }
}

/// Les tasques ja desades amb «+ Una altra», sota el camp (`QuickCaptureAddedList`). L'última porta
/// «Desfer» en lloc d'una confirmació: equivocar-se costa un toc, no un diàleg.
@Composable
fun AddedList(items: List<AddedTask>, today: java.time.LocalDate, onUndo: () -> Unit) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(
            stringResource(R.string.add_chain_count, items.size.toString()),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.textSecondary,
            modifier = Modifier.semantics { heading() },
        )
        Column(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))) {
            items.forEachIndexed { index, item ->
                val isLast = index == items.lastIndex
                val summary = @Composable { modifier: Modifier ->
                    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ViraColor.accent(ViraArea.Plan), modifier = Modifier.size(20.dp))
                        ViraPictogram(item.symbolName, size = 28.dp, scalesWithText = true)
                        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
                            Text(item.title, style = ViraFont.headline, color = ViraColor.textPrimary)
                            Text(whenText(item.day, item.time, today), style = ViraFont.footnote, color = ViraColor.textSecondary)
                        }
                    }
                }
                val undo = @Composable {
                    if (isLast) {
                        ViraActionButton(stringResource(R.string.action_undo), onClick = onUndo, hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Plan)
                    }
                }
                val rowModifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs)
                if (stacked) {
                    Column(rowModifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { summary(Modifier); undo() }
                } else {
                    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) { summary(Modifier.weight(1f)); undo() }
                }
                if (!isLast) HorizontalDivider(color = ViraColor.separator)
            }
        }
    }
}

/// «Es repeteix», sota la targeta del pas Hora (Arnau, 2026-10-19: «s'ha de ficar això»).
@Composable
fun AddRepeatRow(recurrence: PlanItemRecurrence, onSelect: (PlanItemRecurrence) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ViraTouch.comfortable)
                .clip(RoundedCornerShape(ViraRadius.l))
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .viraPressable { open = true }
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPictogram(ViraPictogramName.planRepeat, size = 32.dp, scalesWithText = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
                Text(stringResource(R.string.add_task_repeat), style = ViraFont.headline, color = ViraColor.textPrimary)
                Text(stringResource(R.string.add_task_repeat_hint), style = ViraFont.footnote, color = ViraColor.textSecondary)
                // Amb lletra gran el valor baixa a sota: al costat, el títol es partiria lletra a lletra.
                if (stacked) Text(stringResource(recurrence.titleRes), style = ViraFont.body, color = ViraColor.textSecondary)
            }
            if (!stacked) Text(stringResource(recurrence.titleRes), style = ViraFont.body, color = ViraColor.textSecondary, textAlign = TextAlign.End)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ViraColor.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PlanItemRecurrence.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.titleRes)) },
                    leadingIcon = if (option == recurrence) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

private val PlanItemRecurrence.titleRes: Int
    get() = when (this) {
        PlanItemRecurrence.NONE -> R.string.plan_repeat_none
        PlanItemRecurrence.WEEKLY -> R.string.plan_repeat_weekly
        PlanItemRecurrence.MONTHLY -> R.string.plan_repeat_monthly
        PlanItemRecurrence.DAILY_UNTIL_WEEK_END -> R.string.plan_repeat_daily_until_week_end
        PlanItemRecurrence.DAILY_UNTIL_MONTH_END -> R.string.plan_repeat_daily_until_month_end
    }
