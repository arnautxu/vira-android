package cat.vira.android.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// «La partim?» (`QuickCaptureForm+Split.swift`; Arnau, 2026-10-19: «si l'usuari ho ha de fer, no ho
/// farà»): si la tasca sembla gran, abans de Fet Viri proposa partir-la en passos. Qui decideix si és
/// gran és Jev i, si no respon, la regla del telèfon (`ViriTaskSize`). A Claude només hi va el títol,
/// i només després del «sí» i de l'avís. Les dues respostes desen la tasca.
@Composable
fun TaskSplitStep(title: String) {
    AddStepScene(
        AddScene.Breakdown,
        title = stringResource(R.string.add_task_split_title),
        subtitle = stringResource(R.string.add_task_split_subtitle),
    )
    AddChoiceCard(title = title, pill = stringResource(R.string.add_task_split_pill)) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            SplitPoint(ViraPictogramName.planChecklist, stringResource(R.string.add_task_split_point_steps))
            SplitPoint(ViraPictogramName.focus, stringResource(R.string.add_task_split_point_start))
        }
    }
}

/// Fila blanca que explica, sense botó: les respostes són a baix, com a la resta de passos.
@Composable
private fun SplitPoint(pictogram: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(pictogram, size = 32.dp, scalesWithText = true)
        Text(text, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
    }
}
