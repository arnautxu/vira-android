package cat.vira.android.features.plan

import cat.vira.android.core.design.components.ViraSceneBanner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.viraLocale
import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraIdleBreath
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val area = ViraArea.Plan

/// Detall d'un hàbit (`HabitDetailView`): títol gran amb el pictograma, l'escena de Viri amb una
/// frase i tres xifres (setmana, mes, total), el botó de marcar i el mes en graella. Sense la carta
/// següent: a les builds de botiga les cartes són fora (`ViraReleaseGate.cards`).
@Composable
fun HabitDetail(
    habit: Habit,
    progress: HabitProgress,
    actions: PlanActions,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = ViraSpacing.xs, bottom = viraTabBarClearance() + ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClose)
            Spacer(Modifier.weight(1f))
            Box {
                val more = stringResource(R.string.a11y_more)
                Box(
                    Modifier.size(ViraTouch.minimum).viraPressable { menu = true }.semantics { contentDescription = more },
                    contentAlignment = Alignment.Center,
                ) { PlanGlyph(PlanGlyphShape.Ellipsis, ViraColor.accent(area), size = 22.dp) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.habits_edit), style = ViraFont.body) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.habits_archive), style = ViraFont.body) },
                        onClick = { menu = false; actions.archive(habit); onClose() },
                    )
                }
            }
        }
        Header(habit, zone)
        Hero(habit, progress) { actions.toggle(habit, progress.today, progress) }
        HabitMonthCard(habit, progress)
    }
}

/// Tornar a la llista. A l'iPhone és el botó d'enrere de la barra; aquí, el mateix gest d'enrere
/// i aquesta fletxa.
@Composable
private fun BackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.plan_section_habits)
    Row(
        Modifier.defaultMinSize(minHeight = ViraTouch.minimum).viraPressable(onClick = onClick).padding(end = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        PlanGlyph(PlanGlyphShape.ChevronLeft, ViraColor.accent(area), size = 18.dp, weight = 0.14f)
        Text(label, style = ViraFont.body, color = ViraColor.accent(area))
    }
}

@Composable
private fun Header(habit: Habit, zone: ZoneId) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val icon = 48.dp * LocalDensity.current.fontScale
    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(habit.title, style = ViraFont.largeTitle, color = ViraColor.accent(area), modifier = Modifier.semantics { heading() })
            habit.reminderTime?.let {
                Text(PlanFormat.time(it.atZone(zone).toLocalTime()), style = ViraFont.callout, color = ViraColor.textSecondary)
            }
        }
    }
    if (isLarge) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraPictogram(habit.symbolName, Modifier.clearAndSetSemantics { }, size = icon)
            content()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ViraPictogram(habit.symbolName, Modifier.clearAndSetSemantics { }, size = icon)
            content()
        }
    }
}

@Composable
private fun Hero(habit: Habit, progress: HabitProgress, onToggle: () -> Unit) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val isDone = progress.isDone(habit)
    val week = progress.week(habit).first
    val shape = RoundedCornerShape(ViraRadius.l)
    val line = when {
        isDone -> stringResource(R.string.habits_detail_viri_done)
        week == 1 -> stringResource(R.string.habits_detail_viri_week_one)
        week > 1 -> stringResource(R.string.habits_detail_viri_week, week)
        else -> stringResource(R.string.habits_detail_viri_start)
    }
    val spoken = stringResource(R.string.today_viri_a11y, line)
    Column(Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(area, ViraGradient.Axis.Vertical))) {
        // La bombolla en tapa 20 dp: els 32 que puja menys els 12 de marge.
        ViraSceneBanner(R.drawable.viri_suggest_habit_1, heightRatio = 0.62f, hiddenBottom = 20.dp)
        Column(Modifier.padding(ViraSpacing.s), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Text(
                line,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.pullUp(ViraSpacing.xxl).fillMaxWidth()
                    .shadow(10.dp, shape, ambientColor = ViraColor.textPrimary.copy(alpha = 0.12f), spotColor = ViraColor.textPrimary.copy(alpha = 0.12f))
                    .background(ViraColor.surfaceElevated, shape)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                    .clearAndSetSemantics { contentDescription = spoken },
            )
            val stats: List<Pair<Int, Int>> = listOf(
                week to R.string.habits_detail_week,
                progress.daysThisMonth(habit) to R.string.habits_detail_month,
                progress.totalDays(habit) to R.string.habits_detail_total,
            )
            if (isLarge) {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { stats.forEach { Stat(it.first, stringResource(it.second), Modifier.fillMaxWidth()) } }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { stats.forEach { Stat(it.first, stringResource(it.second), Modifier.weight(1f)) } }
            }
            ToggleButton(isDone, onToggle)
        }
    }
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier) {
    val ink = ViraColor.onCard(area)
    Column(
        modifier.background(ViraColor.lightArtworkVeil.copy(alpha = 0.4f), RoundedCornerShape(ViraRadius.m))
            .padding(vertical = ViraSpacing.xs)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
    ) {
        Text("$value", style = ViraFont.title, color = ink)
        Text(label, style = ViraFont.caption, color = ink, textAlign = TextAlign.Center)
    }
}

/// Pendent: "Marcar com a fet", el botó primari. Fet: la píndola "Fet avui" i "Desfer" al costat.
@Composable
private fun ToggleButton(isDone: Boolean, onToggle: () -> Unit) {
    if (isDone) {
        val undoLabel = stringResource(R.string.habits_detail_undo)
        Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).defaultMinSize(minHeight = ViraTouch.minimum).background(ViraColor.surfaceElevated, CircleShape)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).background(ViraColor.textPrimary, CircleShape), contentAlignment = Alignment.Center) {
                    PlanGlyph(PlanGlyphShape.Check, ViraColor.surfaceElevated, size = 11.dp, weight = 0.2f)
                }
                Text(stringResource(R.string.habits_detail_done_pill), style = ViraFont.headline, color = ViraColor.textPrimary)
            }
            Box(
                Modifier.defaultMinSize(minHeight = ViraTouch.minimum)
                    .border(2.dp, ViraColor.onCard(area), CircleShape)
                    .clip(CircleShape)
                    .viraPressable(onClick = onToggle)
                    .clearAndSetSemantics { contentDescription = undoLabel }
                    .padding(horizontal = ViraSpacing.m),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.action_undo), style = ViraFont.headline, color = ViraColor.onCard(area))
            }
        }
    } else {
        ViraActionButton(stringResource(R.string.habits_detail_mark), onToggle, Modifier.fillMaxWidth(), area = area)
    }
}

/// El mes de l'hàbit en graella (`HabitMonthCard`): el dia fet en degradat amb ✓, el que ja ha
/// passat en blau clar, el futur més tènue i avui amb vora. Cap dia en vermell: només sumem.
@Composable
fun HabitMonthCard(habit: Habit, progress: HabitProgress) {
    val locale = viraLocale()
    val shape = RoundedCornerShape(ViraRadius.l)
    val cellHeight = 40.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Column(
        Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, shape).padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                progress.today.format(DateTimeFormatter.ofPattern("LLLL", locale)).replaceFirstChar { it.titlecase(locale) },
                style = ViraFont.title,
                color = ViraColor.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                stringResource(R.string.habits_detail_month_days, progress.daysThisMonth(habit)),
                style = ViraFont.footnoteEmphasis,
                color = ViraColor.onSurface(area),
                modifier = Modifier.background(ViraColor.surface(area), CircleShape)
                    .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
            )
        }
        Column(Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                progress.currentWeek.forEach { day ->
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = ViraFont.caption,
                        color = ViraColor.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            progress.monthGrid.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f)) { if (day != null) MonthCell(habit, progress, day, cellHeight) else Spacer(Modifier.height(cellHeight)) }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text(stringResource(R.string.habits_detail_no_penalty), style = ViraFont.footnote, color = ViraColor.textSecondary)
    }
}

@Composable
private fun MonthCell(habit: Habit, progress: HabitProgress, day: LocalDate, height: androidx.compose.ui.unit.Dp) {
    val done = progress.isDone(habit, day)
    val future = progress.isFuture(day)
    val shape = RoundedCornerShape(ViraRadius.s)
    val ink = if (done) ViraColor.onCard(area) else ViraColor.textSecondary
    Box(
        Modifier.fillMaxWidth().height(height)
            .then(
                when {
                    done -> Modifier.background(ViraGradient.card(area), shape)
                    !future -> Modifier.background(ViraColor.surface(area), shape)
                    else -> Modifier
                },
            )
            .then(if (day == progress.today) Modifier.border(2.5.dp, ViraColor.brandAccent, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.dayOfMonth}",
            style = if (done) ViraFont.footnoteEmphasis else ViraFont.caption,
            color = ink.copy(alpha = if (future) 0.55f else 1f),
        )
        if (done) {
            PlanGlyph(PlanGlyphShape.Check, ink, Modifier.align(Alignment.TopEnd).padding(ViraSpacing.xxs), size = 9.dp, weight = 0.2f)
        }
    }
}
