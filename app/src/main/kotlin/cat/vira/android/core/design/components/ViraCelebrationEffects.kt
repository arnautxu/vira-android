package cat.vira.android.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/// Confeti curt i saltable (`ConfettiBurst`): setze peces que cauen i s'esvaeixen en menys d'un
/// segon. Amb Reduir moviment no es dibuixa res. Només tons de la paleta: els accents de les àrees,
/// menys el de l'àrea sobre la qual cau (un confeti daurat sobre el groc no es veuria).
@Composable
fun ViraConfettiBurst(modifier: Modifier = Modifier, over: ViraArea? = null) {
    if (LocalViraEnvironment.current.reduceMotion) return
    val areas = listOf(ViraArea.Rewards, ViraArea.Focus, ViraArea.Emotions, ViraArea.Plan).filter { it != over }
    val palette = areas.map { ViraColor.accent(it) } + if (over != ViraArea.Today) listOf(ViraColor.brandAccent) else emptyList()
    val pieces = remember { List(16) { ConfettiPiece.random(Random) } }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(820, easing = LinearOutSlowInEasing)) }

    Canvas(modifier.size(1.dp).clearAndSetSemantics { }) {
        pieces.forEachIndexed { index, piece ->
            val t = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            val w = piece.width.dp.toPx()
            val origin = Offset(piece.dx.dp.toPx() * t, -12.dp.toPx() + (piece.dy + 12f).dp.toPx() * t)
            rotate(piece.rotation * t, origin) {
                drawRoundRect(
                    color = palette[index % palette.size].copy(alpha = 1f - t),
                    topLeft = origin - Offset(w / 2f, w * 0.2f),
                    size = Size(w, w * 0.4f),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val width: Float, val dx: Float, val dy: Float, val rotation: Float, val delay: Float,
) {
    companion object {
        fun random(random: Random) = ConfettiPiece(
            width = random.nextFloat() * 4f + 6f,
            dx = random.nextFloat() * 220f - 110f,
            dy = random.nextFloat() * 80f + 140f,
            rotation = random.nextFloat() * 340f + 180f,
            delay = random.nextFloat() * 0.15f,
        )
    }
}

/// La ràfega de darrere l'heroi de les celebracions (`CelebrationBurst`): tres anelles de polígon
/// arrodonit, cadascuna girada una mica, en el to del text de la pantalla a opacitat baixa.
/// Decoració pura, amagada a TalkBack.
@Composable
fun ViraCelebrationBurst(modifier: Modifier = Modifier, area: ViraArea = ViraArea.Rewards, size: Dp = 320.dp, rings: Int = 3) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val ink = ViraColor.onCard(area)
    val growth = remember { List(rings) { Animatable(if (reduceMotion) 1f else 0f) } }
    LaunchedEffect(Unit) {
        growth.forEachIndexed { index, animatable ->
            launch {
                kotlinx.coroutines.delay(index * 50L)
                animatable.animateTo(1f, ViraMotion.emphasis())
            }
        }
    }
    Box(modifier.size(size).clearAndSetSemantics { }) {
        Canvas(Modifier.size(size)) {
            repeat(rings) { index ->
                val g = growth[index].value
                val side = this.size.minDimension * (1f - 0.19f * index)
                val factor = if (reduceMotion) 1f else 0.55f + 0.45f * g
                rotate(index * 9f) {
                    scale(factor) {
                        val path = roundedPolygonPath(7, 0.32f, Size(side, side))
                        path.translate(Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f))
                        drawPath(path, ink.copy(alpha = (0.05f + 0.04f * index) * g.coerceIn(0f, 1f)))
                    }
                }
            }
        }
    }
}

/// Polígon regular amb les cantonades arrodonides (`RoundedPolygon`), també com a `Shape`.
class ViraRoundedPolygonShape(private val sides: Int = 7, private val cornerFraction: Float = 0.3f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(roundedPolygonPath(sides, cornerFraction, size))
}

internal fun roundedPolygonPath(sides: Int, cornerFraction: Float, size: Size): Path {
    val path = Path()
    if (sides < 3) {
        path.addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
        return path
    }
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f
    val corner = radius * min(cornerFraction, sin(PI / sides).toFloat())
    val vertices = List(sides) { index ->
        // El primer vèrtex a dalt de tot.
        val angle = index.toDouble() / sides * 2 * PI - PI / 2
        Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }
    vertices.forEachIndexed { index, vertex ->
        val start = vertex.movedToward(vertices[(index + sides - 1) % sides], corner)
        val end = vertex.movedToward(vertices[(index + 1) % sides], corner)
        if (index == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        path.quadraticTo(vertex.x, vertex.y, end.x, end.y)
    }
    path.close()
    return path
}

private fun Offset.movedToward(other: Offset, distance: Float): Offset {
    val dx = other.x - x
    val dy = other.y - y
    val length = sqrt(dx * dx + dy * dy)
    if (length == 0f) return this
    return Offset(x + dx / length * distance, y + dy / length * distance)
}
