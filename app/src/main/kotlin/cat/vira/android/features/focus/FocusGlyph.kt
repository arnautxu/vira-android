package cat.vira.android.features.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Els símbols petits de la sessió, dibuixats: els SF Symbols de l'iPhone (`xmark`, `plus`,
/// `minus`, `pause.fill`, `play.fill`, `clock`, `chevron.up.chevron.down`, `checkmark.circle.fill`).
/// Sempre al costat d'un text o amb nom per a TalkBack: mai diuen res sols.
enum class FocusGlyphKind { Close, Plus, Minus, Pause, Play, Clock, Chevrons, Check, Back }

@Composable
fun FocusGlyph(
    kind: FocusGlyphKind,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 17.dp,
    /// La tinta de la marca dins del cercle ple (`Check`).
    ink: Color = Color.Unspecified,
) {
    // Creix amb la lletra, com un símbol en línia amb el text.
    val side = size * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(modifier.size(side)) {
        val w = this.size.minDimension
        val line = w * 0.13f
        val stroke = Stroke(width = line, cap = StrokeCap.Round)
        when (kind) {
            FocusGlyphKind.Close -> {
                drawLine(color, Offset(w * 0.2f, w * 0.2f), Offset(w * 0.8f, w * 0.8f), line, StrokeCap.Round)
                drawLine(color, Offset(w * 0.8f, w * 0.2f), Offset(w * 0.2f, w * 0.8f), line, StrokeCap.Round)
            }
            FocusGlyphKind.Plus -> {
                drawLine(color, Offset(w * 0.5f, w * 0.15f), Offset(w * 0.5f, w * 0.85f), line, StrokeCap.Round)
                drawLine(color, Offset(w * 0.15f, w * 0.5f), Offset(w * 0.85f, w * 0.5f), line, StrokeCap.Round)
            }
            FocusGlyphKind.Minus ->
                drawLine(color, Offset(w * 0.15f, w * 0.5f), Offset(w * 0.85f, w * 0.5f), line, StrokeCap.Round)
            FocusGlyphKind.Pause -> {
                val bar = Size(w * 0.22f, w * 0.7f)
                drawRoundRect(color, Offset(w * 0.2f, w * 0.15f), bar, cornerRadius(w * 0.05f))
                drawRoundRect(color, Offset(w * 0.58f, w * 0.15f), bar, cornerRadius(w * 0.05f))
            }
            FocusGlyphKind.Play -> drawPath(
                Path().apply {
                    moveTo(w * 0.25f, w * 0.12f); lineTo(w * 0.88f, w * 0.5f); lineTo(w * 0.25f, w * 0.88f); close()
                },
                color,
            )
            FocusGlyphKind.Clock -> {
                drawCircle(color, w * 0.42f, style = Stroke(width = line * 0.8f))
                drawLine(color, center, Offset(w * 0.5f, w * 0.24f), line * 0.8f, StrokeCap.Round)
                drawLine(color, center, Offset(w * 0.7f, w * 0.58f), line * 0.8f, StrokeCap.Round)
            }
            FocusGlyphKind.Chevrons -> {
                chevron(color, stroke, w, top = true)
                chevron(color, stroke, w, top = false)
            }
            FocusGlyphKind.Back -> drawPath(
                Path().apply { moveTo(w * 0.62f, w * 0.15f); lineTo(w * 0.3f, w * 0.5f); lineTo(w * 0.62f, w * 0.85f) },
                color,
                style = stroke,
            )
            FocusGlyphKind.Check -> {
                drawCircle(color, w * 0.5f)
                drawPath(
                    Path().apply { moveTo(w * 0.28f, w * 0.52f); lineTo(w * 0.44f, w * 0.68f); lineTo(w * 0.74f, w * 0.36f) },
                    ink,
                    style = Stroke(width = line, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun cornerRadius(r: Float) = androidx.compose.ui.geometry.CornerRadius(r, r)

private fun DrawScope.chevron(color: Color, stroke: Stroke, w: Float, top: Boolean) {
    val y0 = if (top) w * 0.4f else w * 0.6f
    val y1 = if (top) w * 0.18f else w * 0.82f
    drawPath(
        Path().apply { moveTo(w * 0.28f, y0); lineTo(w * 0.5f, y1); lineTo(w * 0.72f, y0) },
        color,
        style = stroke,
    )
}
