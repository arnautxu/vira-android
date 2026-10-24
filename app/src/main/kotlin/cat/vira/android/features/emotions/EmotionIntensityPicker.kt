package cat.vira.android.features.emotions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec
import cat.vira.android.core.sound.ViraHaptics

/// Intensitat opcional de cinc nivells (`EmotionIntensityPicker`). Tocar no confirma ni desa
/// res; tornar a tocar el nivell triat el treu.
///
/// Cinc cercles que creixen de «Gens» a «Moltíssim», amb la paraula sempre a sota. Només s'omple
/// el triat, amb el seu lila, una marca i un anell (Arnau, 2026-10-22, proposta B). Res canvia de
/// mida ni de lloc, i el registre emocional es queda calmat. Si la fila no hi cap (lletra gran,
/// castellà llarg), la llista; mai paraules tallades.
@Composable
internal fun EmotionIntensityPicker(selection: EmotionIntensity?, onSelect: (EmotionIntensity?) -> Unit) {
    val context = LocalContext.current
    val select = { level: EmotionIntensity ->
        // Cada punt vibra una mica més fort: la intensitat es nota sense mirar.
        ViraHaptics.play(context, ViraHaptics.Feedback.Intensity(level.raw))
        onSelect(if (selection == level) null else level)
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labels = EmotionIntensity.entries.map { stringResource(it.label) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // `ViewThatFits`: la fila hi cap si la suma de cada columna (la paraula en negreta, que és
        // com queda triada, o els 48 dp del toc) i els espais no passa de l'ample.
        val ideal = labels.map { label ->
            val width = with(density) { measurer.measure(label, ViraFont.footnoteEmphasis).size.width.toDp() }
            maxOf(width, ViraTouch.minimum)
        }
        val spacing = ViraSpacing.xxs * (ideal.size - 1)
        val total = ideal.fold(spacing) { sum, width -> sum + width }
        if (total <= maxWidth) {
            CircleRow(selection, labels, columnWidths(ideal, maxWidth - spacing), select)
        } else {
            ListPicker(selection, labels, select)
        }
    }
}

/// Totes iguals si cada paraula hi cap; si no, cada una la seva més un tros igual del que sobra.
private fun columnWidths(ideal: List<Dp>, available: Dp): List<Dp> {
    val share = available / ideal.size
    if (ideal.all { it <= share }) return ideal.map { share }
    val extra = (available - ideal.fold(0.dp) { sum, width -> sum + width }) / ideal.size
    return ideal.map { it + extra }
}

@Composable
private fun CircleRow(
    selection: EmotionIntensity?,
    labels: List<String>,
    widths: List<Dp>,
    select: (EmotionIntensity) -> Unit,
) {
    val hint = stringResource(R.string.emotion_intensity_selection_hint)
    Row(
        Modifier.fillMaxWidth().padding(vertical = ViraSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        verticalAlignment = Alignment.Top,
    ) {
        EmotionIntensity.entries.forEachIndexed { index, level ->
            val active = selection == level
            val name = labels[index]
            Column(
                Modifier
                    .width(widths[index])
                    .viraPressable(onClickLabel = hint, role = Role.RadioButton) { select(level) }
                    .clearAndSetSemantics { contentDescription = name; selected = active },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                Box(Modifier.heightIn(min = CIRCLE_AREA).width(ViraTouch.minimum), contentAlignment = Alignment.Center) {
                    IntensityCircle(level, active)
                }
                // La negreta, invisible, reserva sempre l'amplada: triar no pot moure la fila.
                Box(contentAlignment = Alignment.Center) {
                    Text(name, style = ViraFont.footnoteEmphasis, maxLines = 1, modifier = Modifier.alpha(0f))
                    Text(
                        name,
                        style = if (active) ViraFont.footnoteEmphasis else ViraFont.footnote,
                        color = if (active) ViraColor.textPrimary else ViraColor.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}

/// El cercle d'un nivell. Només canvien color i opacitat: un fos, també amb Reduir moviment.
@Composable
private fun IntensityCircle(level: EmotionIntensity, active: Boolean) {
    val diameter = diameter(level)
    val shown by animateFloatAsState(if (active) 1f else 0f, viraSpec(ViraMotion.quick()), label = "intensity")
    val increased = LocalViraEnvironment.current.increasedContrast
    val border = ViraColor.controlBorder
    val ring = ViraColor.textPrimary
    val ringWidth = if (increased) 3.dp else 2.5.dp
    Box(
        Modifier
            .size(diameter)
            // L'anell queda fora del cercle i no ocupa lloc: marca la tria sense color.
            .drawBehind {
                val stroke = ringWidth.toPx()
                drawCircle(
                    ring.copy(alpha = shown),
                    radius = size.minDimension / 2 + 5.dp.toPx() - stroke / 2,
                    style = Stroke(stroke),
                )
            }
            .background(ViraColor.surfaceElevated, CircleShape)
            .border(if (increased) 2.dp else 1.5.dp, border.copy(alpha = 1f - shown), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(diameter)
                .alpha(shown)
                .background(
                    Brush.verticalGradient(listOf(ViraColor.intensityTop(level.raw), ViraColor.intensityBottom(level.raw))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            EmotionGlyphIcon(EmotionGlyph.Checkmark, ViraColor.onIntensity(level.raw), diameter * 0.5f)
        }
    }
}

@Composable
private fun ListPicker(selection: EmotionIntensity?, labels: List<String>, select: (EmotionIntensity) -> Unit) {
    val hint = stringResource(R.string.emotion_intensity_selection_hint)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        EmotionIntensity.entries.forEachIndexed { index, level ->
            val isSelected = selection == level
            val name = labels[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ViraTouch.minimum)
                    .background(
                        if (isSelected) ViraColor.brandSurface else ViraColor.surfaceElevated,
                        RoundedCornerShape(ViraRadius.m),
                    )
                    .viraPressable(onClickLabel = hint, role = Role.RadioButton) { select(level) }
                    .clearAndSetSemantics { contentDescription = name; selected = isSelected }
                    .padding(ViraSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    style = if (isSelected) ViraFont.headline else ViraFont.body,
                    color = ViraColor.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                EmotionGlyphIcon(
                    if (isSelected) EmotionGlyph.Checked else EmotionGlyph.Unchecked,
                    ViraColor.textPrimary, 22.dp * LocalDensity.current.fontScale.coerceAtMost(2f),
                )
            }
        }
    }
}

/// Diàmetre dibuixat de cada cercle, de «Gens» a «Moltíssim». És un gràfic de mida fixa; el toc
/// sempre fa ≥ 48 dp, encara que el cercle sigui més petit.
private fun diameter(level: EmotionIntensity): Dp = listOf(26.dp, 34.dp, 42.dp, 50.dp, 58.dp)[level.raw - 1]

/// Alçada de la zona dels cercles: hi cap el més gran amb el seu anell.
private val CIRCLE_AREA = 64.dp
