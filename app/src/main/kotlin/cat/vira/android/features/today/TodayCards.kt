package cat.vira.android.features.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPop
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle

// MARK: Com estàs?

/// La targeta gran del check-in a tota amplada, amb Viri a sang (`TodayCheckInRow`, que a
/// l'iPhone és un `ViraModuleCard` en fila, cantonada exagerada a baix a la dreta i el pictograma
/// sagnant per baix).
@Composable
internal fun TodayCheckInRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val area = ViraArea.Emotions
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val hero = if (stacked) 132.dp else 176.dp
    val shape = RoundedCornerShape(ViraRadius.l, ViraRadius.l, ViraRadius.dramatic, ViraRadius.l)
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (stacked) 124.dp else 136.dp)
            .clip(shape)
            .background(ViraGradient.card(area), shape)
            .viraPressable(onClick = onClick),
    ) {
        Text(
            stringResource(R.string.today_how_are_you),
            style = ViraFont.title,
            color = ViraColor.onCard(area),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = ViraSpacing.l, end = hero * 0.78f, top = ViraSpacing.s, bottom = ViraSpacing.s),
        )
        ViraPictogram(
            R.drawable.viri_phone,
            size = hero,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = -ViraSpacing.s).padding(end = ViraSpacing.s),
        )
    }
}

// MARK: Hàbits d'avui

/// Els hàbits d'avui, una bombolla per hàbit, marcables d'un toc (`TodayHabitsStrip`). Fora de
/// l'agenda perquè no tenen hora i el que la constància necessita és **ser visible cada dia**.
/// El «Desfer» no cap dins d'una bombolla: surt en una línia a sota, amb el nom de l'hàbit.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodayHabitsStrip(
    habits: List<Habit>,
    doneIds: Set<java.util.UUID>,
    completion: TodayCompletion,
    habitCompletionId: (Habit) -> String,
    onToggle: (Habit) -> Unit,
    onOpenHabits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val area = ViraArea.Plan
    val doneCount = habits.count { it.id in doneIds }
    Column(modifier.fillMaxWidth().padding(vertical = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .defaultMinSize(minHeight = ViraTouch.minimum)
                    .viraPressable(onClick = onOpenHabits)
                    .padding(horizontal = ViraSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                Text(stringResource(R.string.today_habits_title), style = ViraFont.headline, color = ViraColor.onSurface(area))
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ViraColor.onSurface(area), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            if (habits.isNotEmpty()) {
                Text(
                    stringResource(R.string.today_progress, doneCount, habits.size),
                    style = ViraFont.footnote.copy(fontFeatureSettings = "tnum"),
                    color = ViraColor.onSurface(area),
                    modifier = Modifier.padding(horizontal = ViraSpacing.xxs),
                )
            }
        }
        if (habits.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = ViraSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.today_habits_empty), style = ViraFont.footnote, color = ViraColor.textSecondary, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.today_habits_add),
                    style = ViraFont.callout,
                    color = ViraColor.accent(area),
                    modifier = Modifier.defaultMinSize(minHeight = ViraTouch.minimum).viraPressable(onClick = onOpenHabits).wrapContentCenter(),
                )
            }
        } else {
            val active = habits.firstNotNullOfOrNull { habit ->
                completion.phaseFor(habitCompletionId(habit))?.let { habit to it }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                    .padding(ViraSpacing.s),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                BoxWithConstraints {
                    // Prou ample per a la bombolla i un nom de dues línies; amb lletra gran, més.
                    val minColumn = if (LocalViraEnvironment.current.isAccessibilitySize) 140.dp else 72.dp
                    val spacing = ViraSpacing.xs
                    val columns = ((maxWidth + spacing) / (minColumn + spacing)).toInt().coerceAtLeast(1)
                    val cell = (maxWidth - spacing * (columns - 1)) / columns
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                        maxItemsInEachRow = columns,
                    ) {
                        habits.forEach { habit ->
                            HabitBubble(habit, habit.id in doneIds, Modifier.width(cell)) { onToggle(habit) }
                        }
                    }
                }
                if (active != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                        Text(
                            active.first.title,
                            style = ViraFont.footnoteEmphasis,
                            color = ViraColor.onSurface(area),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        TodayInlineCompletion(active.second, ViraColor.onSurface(area), onUndo = { completion.undo(habitCompletionId(active.first)) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/// Bombolla d'un hàbit. L'estat **mai depèn només del color**: fet porta una marca a la vora.
@Composable
private fun HabitBubble(habit: Habit, isDone: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val area = ViraArea.Plan
    val size = 62.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    val stateText = stringResource(if (isDone) R.string.habits_legend_done else R.string.habits_legend_pending)
    val actionText = stringResource(if (isDone) R.string.a11y_mark_pending else R.string.a11y_mark_done)
    Column(
        modifier
            .viraPressable(role = Role.Button, onClick = onToggle)
            .clearAndSetSemantics {
                contentDescription = habit.title
                stateDescription = stateText
                role = Role.Button
                customActions = listOf(CustomAccessibilityAction(actionText) { onToggle(); true })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Box(Modifier.size(size).viraPop(isDone)) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .then(
                        if (isDone) {
                            Modifier.background(ViraGradient.card(area, ViraGradient.Axis.Vertical), CircleShape)
                                .border(3.dp, ViraColor.accent(area), CircleShape)
                        } else {
                            Modifier.background(ViraColor.surface(area), CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ViraPictogram(todayDrawable(habit.symbolName, R.drawable.picto_habits), size = size * 0.64f)
            }
            if (isDone) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(3.dp, 3.dp)
                        .size(22.dp)
                        .background(ViraColor.accent(area), CircleShape)
                        .border(2.dp, ViraColor.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = ViraColor.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            habit.title,
            style = ViraFont.caption,
            color = ViraColor.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// MARK: Demà

/// «Demà» al peu d'Avui, a partir de la tarda o quan el dia ja és fet (`TodayTomorrowCard`).
/// Només per llegir; tocar-la obre el calendari.
@Composable
internal fun TodayTomorrowCard(
    day: Instant,
    zone: ZoneId,
    entries: List<AgendaEntry>,
    habitCount: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val area = ViraArea.Plan
    val locale = LocalConfiguration.current.locales[0]
    val weekday = day.atZone(zone).dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    Column(
        modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(horizontal = ViraSpacing.m),
    ) {
        TodayCardDisclosure(isExpanded, onExpandedChange, ink = ViraColor.accent(area)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(stringResource(R.string.today_tomorrow_title, weekday), style = ViraFont.headline, color = ViraColor.onSurface(area))
                if (isExpanded) {
                    Text(
                        stringResource(R.string.today_tomorrow_summary, thingsText(entries.size), habitsText(habitCount)),
                        style = ViraFont.footnote,
                        color = ViraColor.textSecondary,
                    )
                }
            }
        }
        if (isExpanded) {
            val hint = stringResource(R.string.home_open_calendar)
            Column(Modifier.fillMaxWidth().viraPressable(onClickLabel = hint, onClick = onOpen).semantics(mergeDescendants = true) {}) {
                entries.take(3).forEach { entry ->
                    HorizontalDivider(color = ViraColor.separator)
                    Row(
                        Modifier.padding(vertical = ViraSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                    ) {
                        TimeLabel(entry.time, zone, ViraColor.accent(area), Modifier.defaultMinSize(minWidth = 48.dp))
                        ViraPictogram(todayDrawable(entry.pictogramName), size = 28.dp)
                        Text(entry.title, style = ViraFont.callout, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = ViraColor.accent(area), modifier = Modifier.size(20.dp))
                    Text(hint, style = ViraFont.callout, color = ViraColor.accent(area))
                }
            }
        }
    }
}

private fun Modifier.wrapContentCenter(): Modifier = this.then(Modifier.padding(horizontal = ViraSpacing.xxs))
