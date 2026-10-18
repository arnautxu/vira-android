package cat.vira.android.features.progress.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.features.progress.logic.StreakChainSource
import cat.vira.android.features.progress.logic.StreakMonth
import cat.vira.android.features.progress.logic.StreakWeek
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/// L'estat d'un dia en text: el que diu TalkBack i la llegenda.
private enum class DayState(val text: Int) {
    DONE(R.string.streak_day_done),
    PROTECTED(R.string.streak_day_protected),
    PENDING(R.string.streak_day_pending),
    EMPTY(R.string.streak_day_empty),
}

private fun StreakChainSource.state(day: LocalDate, today: LocalDate): DayState = when {
    isActive(day) -> DayState.DONE
    isProtected(day) -> DayState.PROTECTED
    day == today -> DayState.PENDING
    else -> DayState.EMPTY
}

/// Les lletres dels dies en l'ordre de la setmana del telèfon, no sempre dilluns.
@Composable
private fun WeekdayHeader(first: DayOfWeek, celebration: Boolean = false) {
    val locale = viraLocale()
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        (0L until 7L).forEach { offset ->
            Text(
                first.plus(offset).getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                style = if (celebration) ViraFont.callout.copy(fontWeight = FontWeight.Bold) else ViraFont.caption,
                // Sense opacitat sobre el groc: la jerarquia la fa la mida, no la transparència.
                color = if (celebration) ViraColor.onCard(ViraArea.Rewards) else ViraColor.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/// La càpsula d'un tram de dies encadenats (`StreakChainCapsule`): el degradat daurat de les fites.
@Composable
private fun ChainCapsule(run: IntRange, cell: Dp) {
    val accent = ViraColor.accent(ViraArea.Rewards)
    Box(
        Modifier
            .offset(x = cell * run.first)
            .width(cell * (run.last - run.first + 1))
            .height(cell)
            .shadow(5.dp, CircleShape, ambientColor = accent.copy(alpha = 0.4f), spotColor = accent.copy(alpha = 0.4f))
            .background(ViraGradient.card(ViraArea.Rewards), CircleShape),
    )
}

/// Una casella de dia (`StreakDayCell`): número i marca. **La forma diu l'estat**, no el color: la
/// flama, l'escut o l'exclamació d'avui.
@Composable
private fun DayCell(day: LocalDate, state: DayState, isToday: Boolean, side: Dp) {
    val locale = viraLocale()
    val label = day.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    val stateText = stringResource(state.text)
    val pending = state == DayState.PENDING
    val number = when {
        pending -> ViraColor.onAccent
        state == DayState.DONE || state == DayState.PROTECTED -> ViraColor.onCard(ViraArea.Rewards)
        else -> ViraColor.textSecondary
    }
    val ring = ViraColor.accent(ViraArea.Rewards)
    Column(
        Modifier
            .size(side)
            .then(if (pending) Modifier.background(ViraColor.warning, CircleShape) else Modifier)
            .then(if (isToday && !pending) Modifier.border(1.5.dp, ring, CircleShape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = stateText
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(day.dayOfMonth.toString(), style = ViraFont.caption, color = number)
        when (state) {
            DayState.DONE -> ViraStreakFlame(13.dp)
            DayState.PROTECTED -> ViraPictogram(R.drawable.picto_shield, size = 13.dp)
            DayState.PENDING -> ExclamationMark(ViraColor.onAccent, 9.dp)
            DayState.EMPTY -> Unit
        }
    }
}

/// Una fila de set dies: primer les càpsules dels trams, després els números a sobre.
@Composable
private fun ChainRow(days: List<LocalDate?>, source: StreakChainSource, today: LocalDate, rowHeight: Dp) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(rowHeight)) {
        val cell = maxWidth / 7
        val side = minOf(cell, rowHeight)
        source.runs(days).forEach { ChainCapsule(it, cell) }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Box(Modifier.width(cell).height(rowHeight), contentAlignment = Alignment.Center) {
                    if (day != null) DayCell(day, source.state(day, today), day == today, side)
                }
            }
        }
    }
}

@Composable
private fun rowHeight(base: Dp): Dp = base * LocalDensity.current.fontScale.coerceAtMost(2f)

/// El mes de la ratxa (`StreakCalendarView`): els dies encadenats com una càpsula per tram. **Un
/// reinici no esborra el calendari**: els dies fets es queden pintats per sempre.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StreakCalendar(month: StreakMonth, today: LocalDate, firstDayOfWeek: DayOfWeek) {
    val height = rowHeight(40.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekdayHeader(firstDayOfWeek)
        month.weeks.forEach { ChainRow(it, month, today, height) }
        // Llegenda: sense, les marques serien endevinalles.
        FlowRow(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LegendItem(DayState.DONE) { ViraStreakFlame(11.dp, Modifier.size(11.dp * LocalDensity.current.fontScale)) }
            LegendItem(DayState.PROTECTED) { ViraPictogram(R.drawable.picto_shield, size = 11.dp, scalesWithText = true) }
            LegendItem(DayState.PENDING) { ExclamationMark(ViraColor.textSecondary, 11.dp * LocalDensity.current.fontScale) }
        }
    }
}

@Composable
private fun LegendItem(state: DayState, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon()
        Text(stringResource(state.text), style = ViraFont.caption, color = ViraColor.textSecondary)
    }
}

/// La setmana d'avui a la targeta de la ratxa (`StreakWeekBar(.celebration)`): cercles grans i
/// plens, sense número, sobre el degradat groc. Entren un darrere l'altre i la flama s'encén quan el
/// cercle s'ha assentat; amb Reduir moviment, hi són de cop.
@Composable
internal fun StreakWeekBar(week: StreakWeek, today: LocalDate, firstDayOfWeek: DayOfWeek, revealDelayMillis: Long = 0) {
    val height = rowHeight(52.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekdayHeader(firstDayOfWeek, celebration = true)
        BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
            val cell = maxWidth / 7
            val side = minOf(cell, height)
            Row(Modifier.fillMaxWidth()) {
                week.days.forEachIndexed { index, day ->
                    Box(Modifier.width(cell).height(height), contentAlignment = Alignment.Center) {
                        DayBadge(day, week.state(day, today), side, revealDelayMillis + index * 80L)
                    }
                }
            }
        }
    }
}

/// Un dia de la franja: cercle ple amb la flama, escut sobre el blau, exclamació o cercle buit.
@Composable
private fun DayBadge(day: LocalDate, state: DayState, side: Dp, delayMillis: Long) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val flame = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(delayMillis)
        reveal.animateTo(1f, ViraMotion.emphasis())
        if (state == DayState.DONE) {
            delay(120)
            flame.animateTo(1f, ViraMotion.emphasis())
        }
    }
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val locale = viraLocale()
    val label = day.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale)
    val stateText = stringResource(state.text)
    val diameter = side * 0.82f
    Box(
        Modifier
            .size(diameter)
            .graphicsLayer {
                val scale = 0.4f + 0.6f * reveal.value
                scaleX = scale; scaleY = scale; alpha = reveal.value
            }
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = stateText
            },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            DayState.DONE -> {
                Box(Modifier.size(diameter).background(ink, CircleShape))
                ViraStreakFlame(
                    side * 0.52f,
                    Modifier.graphicsLayer {
                        val scale = 0.15f + 0.85f * flame.value
                        scaleX = scale; scaleY = scale; alpha = flame.value
                    },
                )
            }
            DayState.PROTECTED -> {
                Box(Modifier.size(diameter).background(ViraColor.accent(ViraArea.Plan), CircleShape))
                ViraPictogram(R.drawable.picto_shield, size = side * 0.5f)
            }
            DayState.PENDING -> {
                Box(Modifier.size(diameter).border(2.5.dp, ink, CircleShape))
                ExclamationMark(ink, side * 0.32f)
            }
            // 0,8: el cercle buit és l'únic senyal que aquell dia existeix, i ha de passar de 3:1.
            DayState.EMPTY -> Box(Modifier.size(diameter).border(2.dp, ink.copy(alpha = 0.8f), CircleShape))
        }
    }
}
