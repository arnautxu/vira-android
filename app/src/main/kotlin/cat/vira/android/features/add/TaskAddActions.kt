package cat.vira.android.features.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing

/// Els botons de baix de cada pas.
@Composable
internal fun TaskActions(
    state: TaskAddState,
    onAdvance: () -> Unit,
    onSaveForToday: () -> Unit,
    onAnother: () -> Unit,
    onFinish: () -> Unit,
    onSplit: (wantsSteps: Boolean) -> Unit,
) {
    val area = ViraArea.Plan
    when (state.step) {
        TaskAddStep.What -> AddStepActions(area) {
            // «Per avui» (2026-10-22): escriure i un toc. No surt si el full s'ha obert des d'un altre dia.
            val forToday = @Composable { modifier: Modifier ->
                ViraActionButton(
                    stringResource(R.string.add_task_for_today), onClick = onSaveForToday, modifier = modifier,
                    hierarchy = ViraActionHierarchy.Secondary, area = area, enabled = state.canSave,
                )
            }
            val next = @Composable { modifier: Modifier ->
                ViraActionButton(stringResource(R.string.add_next), onClick = onAdvance, modifier = modifier, enabled = state.canSave)
            }
            if (LocalViraEnvironment.current.isAccessibilitySize) {
                if (state.offersForToday) forToday(Modifier.fillMaxWidth())
                next(Modifier.fillMaxWidth())
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    if (state.offersForToday) forToday(Modifier.weight(1f))
                    next(Modifier.weight(1f))
                }
            }
        }
        // Sense res triat, desa avui i sense hora: l'hora no és obligatòria.
        TaskAddStep.When -> AddStepActions(area) {
            ViraActionButton(stringResource(R.string.add_save), onClick = onAdvance, modifier = Modifier.fillMaxWidth())
        }
        TaskAddStep.Split -> AddStepActions(area) {
            ViraActionButton(stringResource(R.string.add_task_split_yes), onClick = { onSplit(true) }, modifier = Modifier.fillMaxWidth())
            ViraActionButton(
                stringResource(R.string.add_task_split_no), onClick = { onSplit(false) }, modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
        }
        TaskAddStep.Done -> AddStepActions(area) {
            ViraActionButton("+ " + stringResource(R.string.add_task_another), onClick = onAnother, modifier = Modifier.fillMaxWidth())
            ViraActionButton(
                stringResource(R.string.add_task_finish), onClick = onFinish, modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
        }
    }
}
