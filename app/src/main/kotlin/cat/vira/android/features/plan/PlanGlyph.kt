package cat.vira.android.features.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Els símbols de sistema que fa servir Pla a l'iPhone (`plus`, `checkmark`, `list.bullet`,
/// `chevron.left/right`, `ellipsis`), dibuixats: l'app no porta cap paquet d'icones. Sempre
/// decoratius; qui els posa en un botó li dona el nom.
enum class PlanGlyphShape { Plus, Check, List, ChevronLeft, ChevronRight, Ellipsis }

@Composable
fun PlanGlyph(shape: PlanGlyphShape, color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, weight: Float = 0.11f) {
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val w = this.size.width
        val stroke = Stroke(width = w * weight, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (shape) {
            PlanGlyphShape.Plus -> {
                drawLine(color, Offset(w / 2, w * 0.12f), Offset(w / 2, w * 0.88f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.12f, w / 2), Offset(w * 0.88f, w / 2), stroke.width, StrokeCap.Round)
            }
            PlanGlyphShape.Check -> drawPath(
                Path().apply { moveTo(w * 0.14f, w * 0.52f); lineTo(w * 0.4f, w * 0.78f); lineTo(w * 0.88f, w * 0.24f) },
                color,
                style = stroke,
            )
            PlanGlyphShape.List -> listOf(0.25f, 0.5f, 0.75f).forEach { y ->
                drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.14f, w * y))
                drawLine(color, Offset(w * 0.34f, w * y), Offset(w * 0.9f, w * y), stroke.width * 0.85f, StrokeCap.Round)
            }
            PlanGlyphShape.ChevronLeft -> drawPath(
                Path().apply { moveTo(w * 0.64f, w * 0.14f); lineTo(w * 0.3f, w * 0.5f); lineTo(w * 0.64f, w * 0.86f) },
                color,
                style = stroke,
            )
            PlanGlyphShape.ChevronRight -> drawPath(
                Path().apply { moveTo(w * 0.36f, w * 0.14f); lineTo(w * 0.7f, w * 0.5f); lineTo(w * 0.36f, w * 0.86f) },
                color,
                style = stroke,
            )
            PlanGlyphShape.Ellipsis -> listOf(0.2f, 0.5f, 0.8f).forEach { x ->
                drawCircle(color, radius = w * 0.08f, center = Offset(w * x, w / 2))
            }
        }
    }
}
