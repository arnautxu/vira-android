package cat.vira.android.features.emotions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.emotion
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.resources.viraPluralString
import java.time.LocalDate
import java.util.Date

/// Les emocions del període en fitxes grans (`EmotionHistoryTiles`). L'ordre és per freqüència,
/// mai per si l'emoció és agradable, i el número va sempre en text: cap barra que sembli un
/// objectiu a omplir.
@Composable
internal fun EmotionHistoryTiles(counts: List<EmotionHistory.Count>) {
    val face = 60.dp * LocalDensity.current.fontScale
    Row(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        counts.forEach { count ->
            val name = stringResource(count.emotion.label)
            val times = viraPluralString(R.plurals.emotions_history_times, count.count, null, count.count)
            Column(
                Modifier.widthIn(min = face + ViraSpacing.m).clearAndSetSemantics { contentDescription = "$name, $times" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                EmotionFace(count.emotion, face)
                Text(name, style = ViraFont.footnote.copy(fontWeight = FontWeight.Medium), color = ViraColor.textPrimary, maxLines = 1)
                Text(times, style = ViraFont.caption.copy(fontWeight = FontWeight.Bold), color = ViraColor.onSurface(ViraArea.Emotions))
            }
        }
    }
}

/// Un dia de l'historial com a mòdul d'Emocions (`EmotionHistoryDayModule`): el degradat de
/// targeta, "Avui", "Ahir" o la data, i els registres amb l'hora a l'esquerra.
@Composable
internal fun EmotionHistoryDayModule(day: EmotionHistory.Day, onDelete: (EmotionEntry) -> Unit, today: LocalDate) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Emotions), RoundedCornerShape(ViraRadius.l))
            .padding(bottom = ViraSpacing.xs),
    ) {
        DayHeader(day, today)
        day.entries.forEachIndexed { index, entry ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.14f)))
            EntryRow(entry, onDelete)
        }
    }
}

@Composable
private fun DayHeader(day: EmotionHistory.Day, today: LocalDate) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    val title = when (day.date) {
        today -> stringResource(R.string.a11y_today)
        today.minusDays(1) -> stringResource(R.string.emotions_history_yesterday)
        else -> longDate(day.date, LocalConfiguration.current.locales[0])
    }
    Row(
        Modifier.fillMaxWidth().padding(start = ViraSpacing.m, end = ViraSpacing.m, top = ViraSpacing.m, bottom = ViraSpacing.xxs),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Text(title, style = ViraFont.title, color = ink, modifier = Modifier.weight(1f).semantics { heading() })
        Text(
            viraPluralString(R.plurals.emotions_history_count, day.entries.size, null, day.entries.size),
            style = ViraFont.footnote, color = ink,
        )
    }
}

/// Un registre dins del mòdul del dia. Mantenir-lo premut obre «Esborra»; TalkBack té la mateixa
/// acció pel seu nom. Cap dels dos demana confirmació (`CLAUDE.md`: desfer, no modals).
@Composable
private fun EntryRow(entry: EmotionEntry, onDelete: (EmotionEntry) -> Unit) {
    val context = LocalContext.current
    val ink = ViraColor.onCard(ViraArea.Emotions)
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val time = android.text.format.DateFormat.getTimeFormat(context).format(Date.from(entry.createdAt))
    val deleteLabel = stringResource(R.string.emotions_history_delete)
    var menu by remember { mutableStateOf(false) }
    val face = 44.dp * LocalDensity.current.fontScale
    Box(
        Modifier
            .fillMaxWidth()
            .pointerInput(entry.id) { detectTapGestures(onLongPress = { menu = true }) }
            .semantics(mergeDescendants = true) {
                customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(entry); true })
            }
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
    ) {
        val timeText = @Composable {
            Text(
                time,
                style = ViraFont.footnote.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                color = ink,
                modifier = if (large) Modifier else Modifier.widthIn(min = 44.dp).padding(top = ViraSpacing.s),
            )
        }
        val body = @Composable {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s), verticalAlignment = Alignment.Top) {
                EmotionFace(entry.emotion, face, background = SolidColor(ViraColor.surface(ViraArea.Emotions)))
                EntryDetails(entry)
            }
        }
        if (large) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { timeText(); body() }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s), verticalAlignment = Alignment.Top) { timeText(); body() }
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = ViraColor.surfaceElevated) {
            DropdownMenuItem(
                text = { Text(deleteLabel, style = ViraFont.body, color = ViraColor.error) },
                onClick = { menu = false; onDelete(entry) },
            )
        }
    }
}

@Composable
private fun EntryDetails(entry: EmotionEntry) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        Text(stringResource(entry.emotion.label), style = ViraFont.headline, color = ink)
        entry.intensity?.let { IntensityDots(it) }
        entry.note?.takeIf { it.isNotEmpty() }?.let { note ->
            Text(
                note,
                style = ViraFont.footnote,
                color = ViraColor.onSurface(ViraArea.Emotions),
                modifier = Modifier
                    .background(ViraColor.surface(ViraArea.Emotions), RoundedCornerShape(ViraRadius.s))
                    .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
            )
        }
    }
}

/// Cinc punts i l'etiqueta verbal. L'etiqueta hi és sempre i és el que llegeix TalkBack; els punts
/// només repeteixen el nivell per a qui mira.
@Composable
private fun IntensityDots(intensity: EmotionIntensity) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    val dot = 8.dp * LocalDensity.current.fontScale
    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            EmotionIntensity.entries.forEach { level ->
                Box(
                    Modifier
                        .size(dot)
                        .then(if (level.raw <= intensity.raw) Modifier.background(ink, CircleShape) else Modifier)
                        .border(1.5.dp, ink, CircleShape),
                )
            }
        }
        Text(stringResource(intensity.label), style = ViraFont.footnote, color = ink)
    }
}
