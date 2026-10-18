package cat.vira.android.features.emotions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.data.model.emotion
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/// Una cara per dia del període (`EmotionHistoryDayGrid`): la setmana en una fila de set, el mes en
/// una graella amb el número del dia. Cada dia ensenya **l'últim** registre, no "el més important";
/// un dia buit és un cercle discontinu, no un forat.
///
/// L'iPhone hi afegeix un gràfic d'àudio per a VoiceOver (`AXChartDescriptor`); Android no en té
/// d'equivalent, i cada dia ja es pot recórrer amb TalkBack pel seu nom.
@Composable
internal fun EmotionHistoryDayGrid(history: EmotionHistory, range: EmotionHistoryRange, today: LocalDate, modifier: Modifier = Modifier) {
    val scale = LocalDensity.current.fontScale
    val scaled = (if (range == EmotionHistoryRange.WEEK) 40.dp else 30.dp) * scale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // La setmana, set columnes fixes perquè avui no salti de línia. Només amb lletra
        // d'accessibilitat es deixa que la fila salti; fins aleshores la cara no creix més que
        // la seva columna, o les veïnes es trepitjarien.
        val fixedWeek = range == EmotionHistoryRange.WEEK && !LocalViraEnvironment.current.isAccessibilitySize
        val face = if (fixedWeek) minOf(scaled, maxWidth / 7 - 6.dp) else scaled
        val columns = if (fixedWeek) 7
        else ((maxWidth + ViraSpacing.xxs) / (face + ViraSpacing.xs + ViraSpacing.xxs)).toInt().coerceAtLeast(1)
        val gap = if (columns == 7 && range == EmotionHistoryRange.WEEK) 0.dp else ViraSpacing.xxs
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            history.days.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { day -> Box(Modifier.weight(1f)) { DayCell(day, range, face, day.date == today) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: EmotionHistory.Day, range: EmotionHistoryRange, size: Dp, isToday: Boolean) {
    val locale = viraLocale()
    val dayLabel = day.date.format(DateTimeFormatter.ofPattern(if (range == EmotionHistoryRange.WEEK) "EEEEE" else "d", locale))
    val spoken = longDate(day.date, locale)
    // "Content, Trist" o "Sense registres": els noms, no el recompte, que és el que la cara diu.
    val value = if (day.isEmpty) stringResource(R.string.emotions_history_day_empty)
    else day.entries.map { stringResource(it.emotion.label) }.joinToString(", ")
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken; stateDescription = value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Box(
            Modifier
                .size(size + 6.dp)
                .then(if (isToday) Modifier.border(2.dp, ViraColor.accent(ViraArea.Emotions), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val latest = day.entries.firstOrNull()
            if (latest != null) {
                EmotionFace(latest.emotion, size)
                if (day.entries.size > 1) ExtraBadge(day.entries.size - 1, Modifier.align(Alignment.TopEnd))
            } else {
                val dash = ViraColor.separator
                Canvas(Modifier.size(size * 0.8f)) {
                    val stroke = 2.dp.toPx()
                    drawCircle(
                        dash, radius = this.size.minDimension / 2 - stroke / 2,
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                    )
                }
            }
        }
        Text(
            dayLabel,
            style = ViraFont.caption.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium),
            color = if (isToday) ViraColor.onSurface(ViraArea.Emotions) else ViraColor.textSecondary,
        )
    }
}

/// "+2": quants registres més té el dia a part del que ensenya la cara.
@Composable
private fun ExtraBadge(extra: Int, modifier: Modifier) {
    Text(
        "+$extra",
        style = ViraFont.caption.copy(fontWeight = FontWeight.Bold),
        color = ViraColor.onAccent,
        modifier = modifier
            .offset(x = ViraSpacing.xs, y = -ViraSpacing.xxs)
            .wrapContentSize(unbounded = true)
            .border(2.dp, ViraColor.surfaceElevated, CircleShape)
            .background(ViraGradient.accent(ViraArea.Emotions), CircleShape)
            .padding(horizontal = ViraSpacing.xxs + 1.dp),
    )
}

/// "dilluns, 3 d'agost": el dia com el llegeix una persona (`.weekday(.wide).day().month(.wide)`).
internal fun longDate(date: LocalDate, locale: Locale): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}
