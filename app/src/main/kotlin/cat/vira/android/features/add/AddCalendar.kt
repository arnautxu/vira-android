package cat.vira.android.features.add

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private val area = ViraArea.Plan

/// El calendari del mes del pas «Quan» (`QuickCaptureCalendar`, Arnau, 2026-10-24: «i si en
/// comptes de ficar files fiquem un calendari i un rellotge?»).
///
/// No és el `DatePicker` de Material perquè aquell no pot ensenyar els punts: un per cada cosa que
/// ja hi ha aquell dia, fins a tres. Els dies passats no hi surten (Arnau: «no té sentit que surtin
/// els dies d'abans»): les setmanes ja acabades es treuen i la d'avui comença en blanc fins a avui.
/// Els números no passen de la lletra una mica gran: set columnes no hi caben més grans; TalkBack
/// llegeix el dia sencer i el que ja hi ha.
@Composable
fun AddCalendar(selected: LocalDate, today: LocalDate, counts: Map<LocalDate, Int>, onPick: (LocalDate) -> Unit) {
    var shownMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = shownMonth?.let(YearMonth::parse) ?: YearMonth.from(selected)
    val locale = viraLocale()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val density = LocalDensity.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Text(
                monthTitle(shown, locale),
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Al mes d'avui no es pot tornar enrere: la fletxa es veu apagada, no només ho és.
            val isFirstMonth = !shown.isAfter(YearMonth.from(today))
            MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.add_task_when_month_previous), enabled = !isFirstMonth) {
                shownMonth = shown.minusMonths(1).toString()
            }
            MonthButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.add_task_when_month_next), enabled = true) {
                shownMonth = shown.plusMonths(1).toString()
            }
        }
        CompositionLocalProvider(LocalDensity provides Density(density.density, minOf(density.fontScale, MAX_FONT_SCALE))) {
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { index ->
                    Text(
                        firstDay.plus(index.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = ViraFont.caption,
                        color = ViraColor.textSecondary,
                        modifier = Modifier.weight(1f).clearAndSetSemantics { },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            CalendarMonth.weeks(shown, today, firstDay).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day != null) DayCell(day, isSelected = day == selected, isToday = day == today, count = counts[day] ?: 0, onPick)
                        }
                    }
                }
            }
        }
    }
}

/// Les setmanes del mes, amb `null` als forats d'abans de l'1, de després de l'últim i als dies que
/// ja han passat. Una setmana tota passada no hi és. Separat de la vista perquè es pugui provar.
object CalendarMonth {
    fun weeks(month: YearMonth, today: LocalDate, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val start = month.atDay(1)
        val lead = (start.dayOfWeek.value - firstDay.value + 7) % 7
        val days = MutableList<LocalDate?>(lead) { null }
        (1..month.lengthOfMonth()).forEach { days += month.atDay(it).takeUnless { day -> day.isBefore(today) } }
        while (days.size % 7 != 0) days += null
        return days.chunked(7).filter { week -> week.any { it != null } }
    }
}

private const val MAX_FONT_SCALE = 1.3f

@Composable
private fun MonthButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(ViraTouch.minimum)
            .alpha(if (enabled) 1f else 0.35f)
            .background(ViraColor.surface(area), CircleShape)
            .viraPressable(role = Role.Button, enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ViraColor.accent(area))
    }
}

@Composable
private fun DayCell(day: LocalDate, isSelected: Boolean, isToday: Boolean, count: Int, onPick: (LocalDate) -> Unit) {
    val locale = viraLocale()
    val label = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(day)
    val value = if (count > 0) stringResource(R.string.add_task_when_day_count, count.toString()) else ""
    val ink = when {
        isSelected -> ViraColor.onAccent
        isToday -> ViraColor.accent(area)
        else -> ViraColor.textPrimary
    }
    Column(
        Modifier
            .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
            .background(if (isSelected) ViraColor.accent(area) else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .viraPressable(role = Role.Button) { onPick(day) }
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = value
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(day.dayOfMonth.toString(), style = ViraFont.headline, color = ink)
        val dot = if (isSelected) ViraColor.onAccent else ViraColor.accent(area)
        Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(minOf(count, 3)) { Canvas(Modifier.size(5.dp)) { drawCircle(dot) } }
        }
        Spacer(Modifier.height(1.dp))
    }
}

private fun monthTitle(month: YearMonth, locale: java.util.Locale): String =
    java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }
