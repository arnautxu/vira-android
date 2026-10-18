package cat.vira.android.features.entry

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.viraSpec
import kotlin.math.min

/// L'escenari del flux: **Viri, de cos sencer, dret a sobre de la fulla groga** (Arnau, 2026-10-10).
///
/// - **Un posat per pas** (`EntryStep.viriPose`).
/// - Viri **no queda tapat**: ocupa l'espai que la fulla deixa lliure i hi seu a sobre.
/// - La **bombolla surt d'ell**, a sobre del cap.
/// - **Moviment**: en canviar de pas Viri cau, rebota i s'aixafa una mica en tocar la fulla; quan
///   l'usuari tria una opció fa un saltet. Amb Reduir moviment, només un fos.
///
/// Cap text sobre el degradat ni sobre la il·lustració: la bombolla és una superfície. I segueix
/// sent **una sola aparició expressiva** per pantalla.
@Composable
fun EntryStage(
    @DrawableRes pose: Int,
    modifier: Modifier = Modifier,
    line: String? = null,
    celebrates: Boolean = false,
    reaction: Int = 0,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    Box(modifier) {
        EntryStageLight(celebrates, Modifier.matchParentSize())
        Column(
            Modifier.fillMaxSize().padding(top = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (line != null) {
                EntryViriLine(line, tailOnTop = false, modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin))
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clearAndSetSemantics { }) {
                val width = maxWidth
                val height = maxHeight
                // L'ombra als peus, on Viri toca la fulla.
                Canvas(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                        .size(width = min(width.value * 0.5f, height.value * 0.6f).dp, height = 18.dp)
                        .blur(8.dp),
                ) { drawOval(ViraColor.shadow.copy(alpha = 0.22f)) }

                AnimatedContent(
                    targetState = pose,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                        } else {
                            (slideInVertically(ViraMotion.settle()) { -it / 6 } + fadeIn(ViraMotion.settle())) togetherWith
                                (slideOutHorizontally(ViraMotion.settle()) { -it / 6 } + fadeOut(ViraMotion.settle()))
                        }
                    },
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    label = "entryPose",
                ) { shown ->
                    Image(
                        painter = painterResource(shown),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sizeIn(maxWidth = width * 0.82f, maxHeight = height)
                            .entryHop(key = shown to reaction, enabled = !reduceMotion),
                    )
                }
            }
        }
    }
}

/// El saltet de Viri: puja, s'aixafa en tocar la fulla i rebota. Ancorat als peus, perquè
/// l'aixafada sembli que toca terra i no que s'encongeix sencer.
@Composable
private fun Modifier.entryHop(key: Any, enabled: Boolean): Modifier {
    val lift = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    val density = LocalDensity.current
    LaunchedEffect(key, enabled) {
        if (!enabled) return@LaunchedEffect
        val peak = with(density) { 22.dp.toPx() }
        squash.animateTo(1.04f, tween(180, easing = FastOutSlowInEasing))
        lift.animateTo(peak, spring(dampingRatio = 0.8f, stiffness = 500f))
        lift.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
        squash.animateTo(0.9f, tween(140, easing = FastOutSlowInEasing))
        squash.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    return graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 1f)
        scaleX = 2f - squash.value
        scaleY = squash.value
        translationY = -lift.value
    }
}

/// Llum de focus darrere de Viri, uns quants estels i, a la celebració, anelles i confeti. És
/// decoració pura: res no porta informació, i TalkBack no la veu.
@Composable
private fun EntryStageLight(celebrates: Boolean, modifier: Modifier) {
    val isDark = LocalViraEnvironment.current.isDark
    val veil = ViraColor.lightArtworkVeil
    val veilAlpha = if (isDark) 0.12f else 0.38f
    val open by animateFloatAsState(
        targetValue = if (celebrates) 1f else 0f,
        animationSpec = viraSpec(ViraMotion.settle(), decorative = true),
        label = "entryCelebration",
    )
    val confettiColors = CONFETTI.map { ViraColor.cardFill(it.area) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val side = min(size.width, size.height) * 1.1f
        val center = Offset(size.width / 2f, size.height * 0.55f)
        drawCircle(
            Brush.radialGradient(listOf(veil.copy(alpha = veilAlpha), Color.Transparent), center, side / 2f),
            radius = side / 2f,
            center = center,
        )
        SPARKLES.forEach { (x, y, s) ->
            val px = s * density
            drawPath(sparkle(Offset(size.width * x, size.height * y), px), veil.copy(alpha = 0.75f))
        }
        if (open > 0f) {
            listOf(0.62f, 0.9f).forEach { ratio ->
                val alpha = if (ratio < 0.8f) 0.5f else 0.25f
                drawCircle(
                    veil.copy(alpha = alpha * open),
                    radius = side * ratio / 2f * (0.4f + 0.6f * open),
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            CONFETTI.forEachIndexed { index, piece ->
                val target = Offset(size.width * piece.x, size.height * piece.y)
                val at = center + (target - center) * open
                val pieceSize = Size(8.dp.toPx(), (if (piece.long) 18 else 10).dp.toPx())
                rotate(piece.angle * open, at) {
                    drawRoundRect(
                        confettiColors[index].copy(alpha = open),
                        topLeft = at - Offset(pieceSize.width / 2f, pieceSize.height / 2f),
                        size = pieceSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun sparkle(center: Offset, size: Float): Path = Path().apply {
    val r = size / 2f
    moveTo(center.x, center.y - r)
    quadraticTo(center.x, center.y, center.x + r, center.y)
    quadraticTo(center.x, center.y, center.x, center.y + r)
    quadraticTo(center.x, center.y, center.x - r, center.y)
    quadraticTo(center.x, center.y, center.x, center.y - r)
    close()
}

private val SPARKLES = listOf(
    Triple(0.12f, 0.3f, 18f), Triple(0.86f, 0.42f, 14f), Triple(0.8f, 0.86f, 20f), Triple(0.16f, 0.78f, 12f),
)

/// Rectangles petits amb els quatre colors de mòdul que ja té el sistema.
private data class ConfettiPiece(val x: Float, val y: Float, val angle: Float, val area: ViraArea, val long: Boolean)

private val CONFETTI = listOf(
    ConfettiPiece(0.1f, 0.22f, 20f, ViraArea.Rewards, true),
    ConfettiPiece(0.22f, 0.1f, -30f, ViraArea.Emotions, false),
    ConfettiPiece(0.78f, 0.14f, 40f, ViraArea.Plan, true),
    ConfettiPiece(0.9f, 0.3f, 10f, ViraArea.Focus, false),
    ConfettiPiece(0.14f, 0.5f, -50f, ViraArea.Plan, true),
    ConfettiPiece(0.86f, 0.56f, 60f, ViraArea.Rewards, true),
    ConfettiPiece(0.5f, 0.04f, 0f, ViraArea.Focus, false),
    ConfettiPiece(0.66f, 0.08f, -20f, ViraArea.Emotions, true),
    ConfettiPiece(0.32f, 0.36f, 70f, ViraArea.Rewards, false),
    ConfettiPiece(0.7f, 0.42f, 15f, ViraArea.Emotions, false),
    ConfettiPiece(0.04f, 0.4f, 0f, ViraArea.Focus, false),
    ConfettiPiece(0.95f, 0.12f, -35f, ViraArea.Plan, false),
)
