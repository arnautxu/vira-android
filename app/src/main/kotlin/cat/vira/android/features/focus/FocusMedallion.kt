package cat.vira.android.features.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.viraSpec
import kotlin.math.roundToInt

/// Mides del medalló (`FocusMedallionMetrics`). Tot és proporcional al diàmetre: un traç fix es veu
/// prim en un medalló gran i gruixut en un de petit.
object FocusMedallionMetrics {
    fun lineWidth(side: Dp): Dp = maxOf(8.dp, side * 0.052f)

    /// El disc va per dins de l'anell: així el traç del progrés es veu sencer.
    fun discSide(side: Dp): Dp = side - lineWidth(side)

    /// Fins on arriba la claror que respira.
    const val HALO_SCALE = 2f
}

/// El plat del medalló: la llum ve del vèrtex superior esquerre, i l'ombra és del verd de focus.
/// En clar puja cap al blanc; en fosc baixa cap al negre. Mai es queda al to del fons.
@Composable
fun FocusDisc(side: Dp, modifier: Modifier = Modifier) {
    val surface = ViraColor.surface(ViraArea.Focus)
    val background = ViraColor.background
    val light = lerp(surface, background, 0.62f)
    val base = lerp(surface, background, 0.22f)
    Canvas(
        modifier
            .size(side)
            .shadow(side * 0.07f, CircleShape, ambientColor = ViraColor.accent(ViraArea.Focus), spotColor = ViraColor.accent(ViraArea.Focus)),
    ) {
        drawCircle(
            Brush.radialGradient(
                listOf(light, base),
                center = Offset(size.width * 0.34f, size.height * 0.28f),
                radius = size.width * 0.72f,
            ),
        )
    }
}

/// Esfera de durada: dotze blocs de 5 minuts, encesos fins al temps triat.
///
/// Blocs i no un traç continu: el traç continu vol dir **temps fet** un cop la sessió corre, i el
/// mateix dibuix per a dues coses obligaria l'anell a saltar enrere en prémer «Comença».
@Composable
fun FocusDial(side: Dp, minutes: Int, modifier: Modifier = Modifier) {
    val lit = (minutes / 5.0).roundToInt().coerceIn(1, SEGMENTS)
    val accent = ViraColor.accent(ViraArea.Focus)
    val animatedLit by animateFloatAsState(lit.toFloat(), viraSpec(ViraMotion.standard(), decorative = true), label = "dial")
    Canvas(modifier.size(side)) {
        val line = FocusMedallionMetrics.lineWidth(side).toPx()
        val span = 360f / SEGMENTS
        // Un 28 % de cada bloc és solc, amb els extrems rectes: si no, la corona torna a semblar
        // un arc continu amb osques.
        val gap = span * 0.28f
        val inset = line / 2
        val arcSize = Size(size.width - line, size.height - line)
        for (index in 0 until SEGMENTS) {
            val on = index < animatedLit.roundToInt()
            drawArc(
                color = if (on) accent else accent.copy(alpha = 0.22f),
                startAngle = -90f + index * span + gap / 2,
                sweepAngle = span - gap,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = line, cap = StrokeCap.Butt),
            )
        }
    }
}

private const val SEGMENTS = 12

/// Solc + traç de progrés. En marxa avança de manera contínua (lineal d'un segon, que és cada quan
/// arriba un valor nou); amb Reduir moviment, salta al valor i prou: la posició és informació.
@Composable
fun FocusRing(side: Dp, progress: Double, modifier: Modifier = Modifier, isContinuous: Boolean = false) {
    val accent = ViraColor.accent(ViraArea.Focus)
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val spec = when {
        reduceMotion -> snap()
        isContinuous -> tween(1000, easing = LinearEasing)
        else -> tween<Float>(450, easing = FastOutSlowInEasing)
    }
    val shown by animateFloatAsState(progress.toFloat().coerceIn(0.001f, 1f), spec, label = "ring")
    Canvas(modifier.size(side)) {
        val line = FocusMedallionMetrics.lineWidth(side).toPx()
        val inset = line / 2
        val arcSize = Size(size.width - line, size.height - line)
        drawArc(accent.copy(alpha = 0.16f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(line))
        drawArc(accent, -90f, 360f * shown, false, Offset(inset, inset), arcSize, style = Stroke(line, cap = StrokeCap.Round))
    }
}

/// Claror que respira al voltant del medalló, en un cicle de 5,5 s (≈11 respiracions per minut): la
/// pantalla proposa un ritme més lent que el de qui hi arriba. No porta informació: amb Reduir
/// moviment, i en pausa, es queda quieta.
@Composable
fun FocusBreathingHalo(side: Dp, modifier: Modifier = Modifier, isBreathing: Boolean = true) {
    val accent = ViraColor.accent(ViraArea.Focus)
    val breathes = isBreathing && !LocalViraEnvironment.current.reduceMotion
    val scale = if (breathes) {
        val transition = rememberInfiniteTransition(label = "halo")
        transition.animateFloat(1f, 1.06f, infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "inflate").value
    } else {
        1f
    }
    Box(
        modifier
            .size(side)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .blur(side * 0.02f),
    ) {
        Canvas(Modifier.size(side)) {
            drawCircle(
                Brush.radialGradient(
                    0.30f to accent.copy(alpha = 0f),
                    0.40f to accent.copy(alpha = 0.16f),
                    0.68f to accent.copy(alpha = 0.06f),
                    1f to accent.copy(alpha = 0f),
                    radius = size.minDimension / 2,
                ),
            )
        }
    }
}
