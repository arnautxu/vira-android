package cat.vira.android.features.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Els pocs símbols del sistema que fa servir Ajustos a l'iPhone (`chevron.right`, `plus`,
/// `exclamationmark.triangle.fill`, `info.circle.fill`, `square.and.arrow.up`, `doc.richtext`,
/// `arrow.triangle.2.circlepath`), dibuixats amb traç: l'app no porta cap biblioteca d'icones.
///
/// Escalen amb la lletra i sempre van amagats a TalkBack: acompanyen un text que ja ho diu.
enum class SettingsGlyphKind { Chevron, Back, Plus, Warning, Info, Share, Document, Sync, Link }

@Composable
fun SettingsGlyph(kind: SettingsGlyphKind, color: Color, modifier: Modifier = Modifier, extent: Dp = 18.dp) {
    val side = extent * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(modifier.size(side).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val w = size.width
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(vararg points: Pair<Float, Float>) = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) }
        }
        when (kind) {
            SettingsGlyphKind.Chevron -> drawPath(path(0.38f to 0.18f, 0.7f to 0.5f, 0.38f to 0.82f), color, style = stroke)
            SettingsGlyphKind.Back -> drawPath(path(0.62f to 0.18f, 0.3f to 0.5f, 0.62f to 0.82f), color, style = stroke)
            SettingsGlyphKind.Plus -> {
                drawLine(color, Offset(w / 2, w * 0.15f), Offset(w / 2, w * 0.85f), w * 0.11f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.15f, w / 2), Offset(w * 0.85f, w / 2), w * 0.11f, StrokeCap.Round)
            }
            SettingsGlyphKind.Warning -> {
                val triangle = path(0.5f to 0.06f, 0.96f to 0.88f, 0.04f to 0.88f).apply { close() }
                drawPath(triangle, color, style = stroke)
                drawPath(triangle, color)
                drawLine(Color.Black, Offset(w / 2, w * 0.36f), Offset(w / 2, w * 0.6f), w * 0.11f, StrokeCap.Round, blendMode = BlendMode.Clear)
                drawCircle(Color.Black, w * 0.06f, Offset(w / 2, w * 0.74f), blendMode = BlendMode.Clear)
            }
            SettingsGlyphKind.Info -> {
                drawCircle(color)
                drawLine(Color.Black, Offset(w / 2, w * 0.45f), Offset(w / 2, w * 0.74f), w * 0.11f, StrokeCap.Round, blendMode = BlendMode.Clear)
                drawCircle(Color.Black, w * 0.065f, Offset(w / 2, w * 0.28f), blendMode = BlendMode.Clear)
            }
            SettingsGlyphKind.Share -> {
                drawPath(path(0.32f to 0.42f, 0.18f to 0.42f, 0.18f to 0.92f, 0.82f to 0.92f, 0.82f to 0.42f, 0.68f to 0.42f), color, style = stroke)
                drawLine(color, Offset(w / 2, w * 0.08f), Offset(w / 2, w * 0.62f), w * 0.11f, StrokeCap.Round)
                drawPath(path(0.32f to 0.24f, 0.5f to 0.07f, 0.68f to 0.24f), color, style = stroke)
            }
            SettingsGlyphKind.Document -> {
                drawPath(path(0.22f to 0.06f, 0.6f to 0.06f, 0.8f to 0.26f, 0.8f to 0.94f, 0.22f to 0.94f).apply { close() }, color, style = stroke)
                drawLine(color, Offset(w * 0.36f, w * 0.5f), Offset(w * 0.66f, w * 0.5f), w * 0.09f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, w * 0.7f), Offset(w * 0.66f, w * 0.7f), w * 0.09f, StrokeCap.Round)
            }
            SettingsGlyphKind.Sync -> {
                drawArc(color, 200f, 230f, false, topLeft = Offset(w * 0.14f, w * 0.14f),
                    size = Size(w * 0.72f, w * 0.72f), style = stroke)
                drawPath(path(0.6f to 0.1f, 0.78f to 0.22f, 0.62f to 0.36f), color, style = stroke)
            }
            SettingsGlyphKind.Link -> {
                drawPath(path(0.42f to 0.2f, 0.2f to 0.2f, 0.2f to 0.8f, 0.8f to 0.8f, 0.8f to 0.58f), color, style = stroke)
                drawPath(path(0.55f to 0.2f, 0.8f to 0.2f, 0.8f to 0.45f), color, style = stroke)
                drawLine(color, Offset(w * 0.8f, w * 0.2f), Offset(w * 0.46f, w * 0.54f), w * 0.11f, StrokeCap.Round)
            }
        }
    }
}
