package cat.vira.android.features.emotions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp

/// Els símbols del sistema que fan servir les pantalles d'Emocions a l'iPhone (`ellipsis`,
/// `slider.horizontal.3`, `chevron.right`, `hourglass`, `wind`, `checkmark.circle.fill`,
/// `circle`), dibuixats amb traç perquè l'app no porta cap joc d'icones. Sempre decoratius: el
/// text del costat és qui diu què fa el botó.
enum class EmotionGlyph { Ellipsis, Sliders, Chevron, Hourglass, Wind, Checked, Unchecked }

@Composable
fun EmotionGlyphIcon(glyph: EmotionGlyph, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clearAndSetSemantics { },
    ) {
        val w = this.size.width
        val line = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            EmotionGlyph.Ellipsis -> listOf(0.2f, 0.5f, 0.8f).forEach {
                drawCircle(color, radius = w * 0.09f, center = Offset(w * it, w / 2))
            }
            EmotionGlyph.Sliders -> listOf(0.22f to 0.66f, 0.5f to 0.32f, 0.78f to 0.58f).forEach { (y, knob) ->
                drawLine(color, Offset(w * 0.1f, w * y), Offset(w * 0.9f, w * y), line.width, StrokeCap.Round)
                drawCircle(color, radius = w * 0.1f, center = Offset(w * knob, w * y))
            }
            EmotionGlyph.Chevron -> drawPath(
                Path().apply { moveTo(w * 0.36f, w * 0.18f); lineTo(w * 0.66f, w * 0.5f); lineTo(w * 0.36f, w * 0.82f) },
                color, style = line,
            )
            EmotionGlyph.Hourglass -> {
                drawLine(color, Offset(w * 0.22f, w * 0.1f), Offset(w * 0.78f, w * 0.1f), line.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.22f, w * 0.9f), Offset(w * 0.78f, w * 0.9f), line.width, StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(w * 0.3f, w * 0.12f); lineTo(w * 0.7f, w * 0.12f); lineTo(w * 0.5f, w * 0.5f)
                        lineTo(w * 0.7f, w * 0.88f); lineTo(w * 0.3f, w * 0.88f); lineTo(w * 0.5f, w * 0.5f); close()
                    },
                    color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round),
                )
            }
            EmotionGlyph.Wind -> listOf(0.28f to 0.72f, 0.5f to 0.9f, 0.72f to 0.6f).forEach { (y, end) ->
                drawPath(
                    Path().apply {
                        moveTo(w * 0.08f, w * y)
                        lineTo(w * (end - 0.12f), w * y)
                        quadraticTo(w * end, w * y, w * end, w * (y - 0.1f))
                    },
                    color, style = line,
                )
            }
            EmotionGlyph.Checked -> {
                drawCircle(color)
                drawPath(
                    Path().apply { moveTo(w * 0.28f, w * 0.52f); lineTo(w * 0.44f, w * 0.67f); lineTo(w * 0.73f, w * 0.36f) },
                    Color.Black,
                    style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = BlendMode.Clear,
                )
            }
            EmotionGlyph.Unchecked -> drawCircle(
                color, radius = w / 2 - w * 0.04f, style = Stroke(w * 0.08f),
            )
        }
    }
}

