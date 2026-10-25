package cat.vira.android.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraSceneTile
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate

private val area = ViraArea.Plan

/// El pas **Quan** de la cara Tasca: el dia i l'hora a la mateixa pantalla (`QuickCaptureForm+When`,
/// Arnau, 2026-10-24).
///
/// A dalt, tres moments d'un toc que desen sense res més (`CaptureMoment`; opció A + C). A sota, «O
/// tria tu»: el calendari del mes amb un punt per cada cosa que ja hi ha, «Sense hora» o «A una
/// hora» amb el rellotge de roda, i «Es repeteix». «Desar» tanca.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskWhenStep(state: TaskAddState, onPickMoment: (CaptureMoment) -> Unit) {
    WhenPrompt(stringResource(R.string.add_task_when_title), state.titleToSave)
    if (state.moments.isNotEmpty()) {
        AddChoiceCard(stringResource(R.string.add_task_when_quick)) {
            MomentRow(state.moments, state.today, onPickMoment)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(if (state.moments.isEmpty()) R.string.add_task_when_day else R.string.add_task_when_own),
                style = ViraFont.headline,
                color = ViraColor.textSecondary,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.add_task_when_dots), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        AddCalendar(selected = state.day, today = state.today, counts = state.dayCounts, onPick = state::choose)
        AddClock(
            hasTime = state.time != null,
            minutes = state.timeMinutes,
            onNoTime = state::pickNoTime,
            onTimed = state::pickTime,
            onChange = state::setTime,
        )
    }
    AddRepeatRow(state.recurrence) { state.recurrence = it }
}

/// Viri petit i la bombolla que en surt (`QuickCaptureWhenPrompt`): «Quan et va bé?» i el títol. Més
/// baix que l'escena dels altres passos perquè a sota hi ha el dia i l'hora alhora. A lletra gran
/// la bombolla baixa a sota de Viri.
@Composable
private fun WhenPrompt(title: String, subtitle: String) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val label = stringResource(R.string.today_viri_a11y, "$title $subtitle")
    val bubble = @Composable { modifier: Modifier ->
        Column(
            modifier
                .shadow(10.dp, RoundedCornerShape(ViraRadius.l), ambientColor = ViraColor.accent(area), spotColor = ViraColor.accent(area).copy(alpha = 0.16f))
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                .clearAndSetSemantics { contentDescription = label; heading() },
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            Text(title, style = ViraFont.title, color = ViraColor.accent(area))
            Text(subtitle, style = ViraFont.body, color = ViraColor.textPrimary, maxLines = if (stacked) Int.MAX_VALUE else 2)
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraSceneTile(R.drawable.viri_suggest_habit_3, 96.dp)
            bubble(Modifier.fillMaxWidth())
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ViraSceneTile(R.drawable.viri_suggest_habit_3, 76.dp)
            bubble(Modifier.weight(1f))
        }
    }
}

/// Els tres moments d'un toc, d'ample igual (`QuickCaptureMomentRow`): el dia en gran i, a sota, la
/// franja i l'hora en dues línies, perquè les tres fitxes quedin iguals. A lletra gran, un sota
/// l'altre.
@Composable
private fun MomentRow(moments: List<CaptureMoment>, today: LocalDate, onPick: (CaptureMoment) -> Unit) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val hint = stringResource(R.string.add_task_when_quick_hint)
    val tile = @Composable { moment: CaptureMoment, modifier: Modifier ->
        val resolved = moment.resolved(today)
        Column(
            modifier
                .defaultMinSize(minHeight = ViraTouch.comfortable)
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
                .viraPressable(onClickLabel = hint, role = Role.Button) { onPick(moment) }
                .semantics(mergeDescendants = true) {}
                .padding(vertical = ViraSpacing.xs, horizontal = ViraSpacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline),
        ) {
            ViraPictogram(AddTime.Period(moment.period).pictogram, size = 30.dp, scalesWithText = true)
            Text(dayName(resolved.toLocalDate(), today, "EEEE"), style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center)
            Text(stringResource(moment.period.title), style = ViraFont.footnote, color = ViraColor.textSecondary, textAlign = TextAlign.Center)
            Text(clockText(resolved.hour * 60 + resolved.minute), style = ViraFont.footnoteEmphasis, color = ViraColor.textSecondary)
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            moments.forEach { tile(it, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            moments.forEach { tile(it, Modifier.weight(1f)) }
        }
    }
}
