package cat.vira.android.features.emotions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs

/// Intensitat opcional de cinc nivells (`EmotionIntensityPicker`). Tocar no confirma ni desa
/// res; tornar a tocar el nivell triat el treu.
///
/// Les cinc càpsules en fila; la triada creix al mig amb el nom a dins i les altres s'aparten.
/// És geometria, i amb Reduir moviment no es fon: salta al seu lloc. Amb lletra gran, una llista.
@Composable
internal fun EmotionIntensityPicker(selection: EmotionIntensity?, onSelect: (EmotionIntensity?) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val select = { level: EmotionIntensity ->
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onSelect(if (selection == level) null else level)
    }
    // `typeSize >= .xxxLarge` de l'iPhone: l'últim esglaó abans de les mides d'accessibilitat.
    if (LocalDensity.current.fontScale >= 1.3f) ListPicker(selection, select) else GeometryPicker(selection, select)
}

@Composable
private fun ListPicker(selection: EmotionIntensity?, select: (EmotionIntensity) -> Unit) {
    val hint = stringResource(R.string.emotion_intensity_selection_hint)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        EmotionIntensity.entries.forEach { level ->
            val isSelected = selection == level
            val name = stringResource(level.label)
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
                Text(name, style = ViraFont.callout, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
                EmotionGlyphIcon(
                    if (isSelected) EmotionGlyph.Checked else EmotionGlyph.Unchecked,
                    ViraColor.textPrimary, 22.dp * LocalDensity.current.fontScale.coerceAtMost(2f),
                )
            }
        }
    }
}

@Composable
private fun GeometryPicker(selection: EmotionIntensity?, select: (EmotionIntensity) -> Unit) {
    val environment = LocalViraEnvironment.current
    val hint = stringResource(R.string.emotion_intensity_selection_hint)
    val accent = ViraColor.brandAccent
    val glow by animateFloatAsState(if (selection == null) 0.08f else 0.19f, viraSpec(ViraMotion.standard()), label = "glow")
    val labelsAlpha by animateFloatAsState(if (selection == null) 1f else 0f, viraSpec(ViraMotion.quick()), label = "labels")

    BoxWithConstraints(Modifier.fillMaxWidth().height(CANVAS_HEIGHT).clipToBounds()) {
        val width = maxWidth
        val scale = minOf(width.value / 345f, 1f)
        val canvas = if (selection == null) width else maxOf(width, 530.dp)
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        // Centrat en obrir-se i cada cop que canvia d'ample (`defaultScrollAnchor(.center)`).
        LaunchedEffect(canvas, width) { scroll.scrollTo(with(density) { ((canvas - width) / 2).roundToPx() }) }

        Box(
            Modifier
                .horizontalScroll(scroll)
                .width(canvas)
                .height(CANVAS_HEIGHT)
                .drawBehind {
                    if (!environment.reduceMotion && !environment.increasedContrast) {
                        drawCircle(
                            Brush.radialGradient(listOf(accent.copy(alpha = glow), Color.Transparent), Offset(size.width / 2, size.height / 2), 150.dp.toPx()),
                            radius = 150.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2),
                        )
                    }
                },
        ) {
            EmotionIntensity.entries.forEach { level ->
                Level(level, selection, canvas, scale, hint, select)
                val x by animateDpAsState(center(level, null, canvas, scale), viraSpec(ViraMotion.settle(), decorative = true), label = "labelX")
                Text(
                    stringResource(level.label),
                    style = ViraFont.footnote,
                    color = ViraColor.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(x = x - 31.dp * scale, y = 170.dp)
                        .width(62.dp * scale)
                        .alpha(labelsAlpha)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

@Composable
private fun Level(
    level: EmotionIntensity,
    selection: EmotionIntensity?,
    canvas: Dp,
    scale: Float,
    hint: String,
    select: (EmotionIntensity) -> Unit,
) {
    val active = selection == level
    val dimmed = selection != null && !active
    val spec = viraSpec(ViraMotion.settle<Dp>(), decorative = true)
    val width by animateDpAsState(if (active) 134.dp else if (dimmed) 38.dp else 52.dp * scale, spec, label = "w")
    val height by animateDpAsState(if (active) 192.dp else if (dimmed) 64.dp else 88.dp, spec, label = "h")
    val x by animateDpAsState(center(level, selection, canvas, scale), spec, label = "x")
    val ink = if (dimmed) ViraColor.textSecondary else ViraColor.onIntensity(level.raw)
    val fill = if (dimmed) Brush.verticalGradient(listOf(ViraColor.surfaceElevated, ViraColor.surfaceElevated))
    else Brush.verticalGradient(listOf(ViraColor.intensityTop(level.raw), ViraColor.intensityBottom(level.raw)))
    val stroke = if (LocalViraEnvironment.current.increasedContrast) 2.dp else 1.dp
    val name = stringResource(level.label)
    // La zona tàctil mai baixa de 48 dp, encara que la càpsula apartada en faci 38.
    val touchWidth = maxOf(width, ViraTouch.minimum)
    Box(
        modifier = Modifier
            .offset(x = x - touchWidth / 2, y = CENTER_Y - height / 2)
            .size(touchWidth, height)
            .viraPressable(onClickLabel = hint, role = Role.RadioButton) { select(level) }
            .semantics { contentDescription = name; selected = active },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .size(width, height)
                .background(fill, CircleShape)
                .border(stroke, ink, CircleShape)
                .padding(horizontal = if (active) 12.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically),
        ) {
            Meter(level, ink)
            if (active) Text(name, style = ViraFont.callout, color = ink, textAlign = TextAlign.Center)
        }
    }
}

/// Cinc barres que pugen: quantes en són plenes diu el nivell sense llegir.
@Composable
private fun Meter(level: EmotionIntensity, ink: Color) {
    Row(
        Modifier.height(25.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        (1..5).forEach { index ->
            Box(
                Modifier
                    .size(3.5.dp, (8 + index * 3).dp)
                    .background(ink.copy(alpha = if (index <= level.raw) 1f else 0.2f), CircleShape),
            )
        }
    }
}

/// On cau el centre de cada càpsula. Sense res triat, en fila a 62 dp; amb un nivell triat, aquest
/// al mig i els altres apartats a 95 dp del centre i 48 més per cada pas.
private fun center(level: EmotionIntensity, selection: EmotionIntensity?, width: Dp, scale: Float): Dp {
    if (selection == null) return width / 2 + 62.dp * scale * (level.raw - 3)
    val delta = level.raw - selection.raw
    if (delta == 0) return width / 2
    val distance = 95.dp + 48.dp * (abs(delta) - 1)
    return width / 2 + if (delta < 0) -distance else distance
}

private val CANVAS_HEIGHT = 226.dp
private val CENTER_Y = 112.dp
