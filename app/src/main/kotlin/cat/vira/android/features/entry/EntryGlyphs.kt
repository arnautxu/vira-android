package cat.vira.android.features.entry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Els símbols de sistema que l'iPhone fa servir al flux (`chevron.left`, `checkmark`,
/// `checkmark.circle.fill`, `sparkle`, `envelope`, `hand.raised`, `doc.text`,
/// `arrow.triangle.2.circlepath`, `lock`, `info.circle`, `hand.tap`), dibuixats: Android
/// no els té, i el sistema de disseny tampoc fa servir icones de Material. Sempre amagats a
/// TalkBack: acompanyen un text, no en diuen res de nou.
enum class EntryGlyph { BACK, CHECK, CHECK_CIRCLE, SPARKLE, ENVELOPE, PRIVACY, DOCUMENT, SYNC, LOCK, INFO, TAP }

/// `side` escala amb la lletra si `scalesWithText`, com un símbol de sistema al costat d'un text.
@Composable
fun EntryGlyphIcon(glyph: EntryGlyph, color: Color, side: Dp = 18.dp, scalesWithText: Boolean = true) {
    val size = if (scalesWithText) side * LocalDensity.current.fontScale.coerceAtMost(2f) else side
    Canvas(Modifier.size(size).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        draw(glyph, color)
    }
}

private fun DrawScope.draw(glyph: EntryGlyph, color: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        EntryGlyph.BACK -> drawPath(
            Path().apply { moveTo(w * 0.64f, w * 0.18f); lineTo(w * 0.32f, w * 0.5f); lineTo(w * 0.64f, w * 0.82f) },
            color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        EntryGlyph.CHECK -> drawPath(checkPath(w), color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        EntryGlyph.CHECK_CIRCLE -> {
            drawCircle(color)
            // La ✓ es buida del cercle, com el símbol ple d'iOS.
            drawPath(checkPath(w, inset = 0.2f), color, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
        }
        EntryGlyph.SPARKLE -> drawPath(
            Path().apply {
                val c = w / 2f
                moveTo(c, 0f)
                quadraticTo(c, c, w, c)
                quadraticTo(c, c, c, w)
                quadraticTo(c, c, 0f, c)
                quadraticTo(c, c, c, 0f)
                close()
            },
            color,
        )
        EntryGlyph.ENVELOPE -> {
            drawRoundRect(color, Offset(w * 0.06f, w * 0.2f), Size(w * 0.88f, w * 0.6f), CornerRadius(w * 0.08f), style = stroke)
            drawPath(Path().apply { moveTo(w * 0.1f, w * 0.26f); lineTo(w * 0.5f, w * 0.55f); lineTo(w * 0.9f, w * 0.26f) }, color, style = stroke)
        }
        EntryGlyph.PRIVACY -> drawPath(
            Path().apply {
                moveTo(w * 0.5f, w * 0.06f)
                lineTo(w * 0.86f, w * 0.2f)
                lineTo(w * 0.86f, w * 0.46f)
                quadraticTo(w * 0.86f, w * 0.8f, w * 0.5f, w * 0.94f)
                quadraticTo(w * 0.14f, w * 0.8f, w * 0.14f, w * 0.46f)
                lineTo(w * 0.14f, w * 0.2f)
                close()
            },
            color, style = stroke,
        )
        EntryGlyph.DOCUMENT -> {
            drawRoundRect(color, Offset(w * 0.18f, w * 0.06f), Size(w * 0.64f, w * 0.88f), CornerRadius(w * 0.08f), style = stroke)
            listOf(0.34f, 0.5f, 0.66f).forEach { y ->
                drawLine(color, Offset(w * 0.32f, w * y), Offset(w * 0.68f, w * y), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
            }
        }
        EntryGlyph.SYNC -> {
            val inset = w * 0.16f
            val arc = Size(w - inset * 2, w - inset * 2)
            drawArc(color, 200f, 130f, false, Offset(inset, inset), arc, style = stroke)
            drawArc(color, 20f, 130f, false, Offset(inset, inset), arc, style = stroke)
            // Les dues puntes de fletxa, al final de cada arc.
            drawPath(Path().apply { moveTo(w * 0.72f, w * 0.1f); lineTo(w * 0.8f, w * 0.27f); lineTo(w * 0.62f, w * 0.3f) }, color, style = stroke)
            drawPath(Path().apply { moveTo(w * 0.28f, w * 0.9f); lineTo(w * 0.2f, w * 0.73f); lineTo(w * 0.38f, w * 0.7f) }, color, style = stroke)
        }
        EntryGlyph.LOCK -> {
            drawRoundRect(color, Offset(w * 0.18f, w * 0.44f), Size(w * 0.64f, w * 0.48f), CornerRadius(w * 0.08f), style = stroke)
            drawArc(color, 180f, 180f, false, Offset(w * 0.3f, w * 0.1f), Size(w * 0.4f, w * 0.4f), style = stroke)
            drawLine(color, Offset(w * 0.3f, w * 0.3f), Offset(w * 0.3f, w * 0.44f), strokeWidth = w * 0.11f)
            drawLine(color, Offset(w * 0.7f, w * 0.3f), Offset(w * 0.7f, w * 0.44f), strokeWidth = w * 0.11f)
        }
        // Un toc: el punt on cau el dit i dues ones que se n'allunyen.
        EntryGlyph.TAP -> {
            drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.5f, w * 0.62f))
            drawArc(color, 200f, 140f, false, Offset(w * 0.24f, w * 0.36f), Size(w * 0.52f, w * 0.52f), style = stroke)
            drawArc(color, 205f, 130f, false, Offset(w * 0.06f, w * 0.18f), Size(w * 0.88f, w * 0.88f), style = stroke)
        }
        EntryGlyph.INFO -> {
            drawCircle(color, radius = w * 0.44f, style = stroke)
            drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.5f, w * 0.3f))
            drawLine(color, Offset(w * 0.5f, w * 0.45f), Offset(w * 0.5f, w * 0.72f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        }
    }
}

private fun checkPath(w: Float, inset: Float = 0.12f) = Path().apply {
    val span = 1f - inset * 2
    moveTo(w * (inset + span * 0.08f), w * (inset + span * 0.55f))
    lineTo(w * (inset + span * 0.38f), w * (inset + span * 0.82f))
    lineTo(w * (inset + span * 0.92f), w * (inset + span * 0.2f))
}
