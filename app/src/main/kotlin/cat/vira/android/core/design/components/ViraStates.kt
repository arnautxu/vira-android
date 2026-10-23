package cat.vira.android.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing

/// Estat buit amb acció (`ViraEmptyState`): diu què passa i ofereix què fer-hi. Mai una caixa de
/// color plena de majúscules sense sortida.
///
/// `icon` és la il·lustració o la icona de dalt (a l'iPhone, un símbol del sistema en gris).
@Composable
fun ViraEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    /// «Com funciona?»: una parada del tomb, per a qui el va saltar (`helpStop` a l'iPhone).
    helpTitle: String? = null,
    onHelp: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(ViraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (icon != null) Box(Modifier.padding(bottom = ViraSpacing.xxs)) { icon() }
        Text(
            title,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (message != null) {
            Text(message, style = ViraFont.body, color = ViraColor.textSecondary, textAlign = TextAlign.Center)
        }
        if (actionTitle != null && onAction != null) {
            ViraActionButton(actionTitle, onAction, modifier = Modifier.padding(top = ViraSpacing.xs))
        }
        if (helpTitle != null && onHelp != null) {
            ViraActionButton(helpTitle, onHelp, hierarchy = ViraActionHierarchy.Textual)
        }
    }
}

/// Missatge d'error en línia (`ViraInlineError`). La icona és qui diu que és un error, no el
/// vermell (`CLAUDE.md`: mai informació només per color), i TalkBack el precedeix d'«Error».
@Composable
fun ViraInlineError(message: String, modifier: Modifier = Modifier) {
    val spoken = stringResource(R.string.a11y_error, message)
    val color = ViraColor.error
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        val side = 16.dp * LocalDensity.current.fontScale
        Canvas(
            Modifier.padding(top = 1.dp).size(side)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            // Triangle d'avís ple, amb el signe buidat: `exclamationmark.triangle.fill`.
            val w = size.width
            val triangle = Path().apply {
                moveTo(w / 2f, 0f); lineTo(w, w * 0.92f); lineTo(0f, w * 0.92f); close()
            }
            drawPath(triangle, color, style = Stroke(width = w * 0.12f, join = StrokeJoin.Round))
            drawPath(triangle, color)
            drawLine(
                Color.Black, Offset(w / 2f, w * 0.34f), Offset(w / 2f, w * 0.6f),
                strokeWidth = w * 0.12f, cap = StrokeCap.Round, blendMode = BlendMode.Clear,
            )
            drawCircle(Color.Black, radius = w * 0.065f, center = Offset(w / 2f, w * 0.76f), blendMode = BlendMode.Clear)
        }
        Text(message, style = ViraFont.footnote, color = color)
    }
}

/// Títol gran d'una subpantalla empesa (`ViraPageTitle`, `DESIGN_RULES.md` §1): Ratxa, Botiga,
/// Ajustos… En el color de l'àrea i marcat com a capçalera.
@Composable
fun ViraPageTitle(text: String, area: ViraArea, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ViraFont.largeTitle,
        color = ViraColor.accent(area),
        modifier = modifier.fillMaxWidth().semantics { heading() },
    )
}

/// Títol d'una pestanya arrel (`ViraTabTitle`): a l'esquerra, a la fila de les accions (+).
@Composable
fun ViraTabTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ViraFont.title, color = ViraColor.textPrimary, modifier = modifier.semantics { heading() })
}

/// La casella de «fet» de tota l'app (`ViraCompletionCheck`): cercle buit o cercle ple amb la ✓.
///
/// Fer-la fa un bot petit; desfer-la, no. **La medicació no en porta** (`celebrates = false`):
/// marcar una presa confirma, no premia (D-27). Amb Reduir moviment, només el canvi de forma.
///
/// Sense tint, agafa `ink` (el color del text de la fila), que dins d'una targeta de color és el
/// que ja contrasta.
@Composable
fun ViraCompletionCheck(
    isDone: Boolean,
    modifier: Modifier = Modifier,
    celebrates: Boolean = true,
    ink: Color = ViraColor.textPrimary,
    doneTint: Color? = null,
    pendingTint: Color? = null,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val bounce = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(isDone) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (isDone && celebrates && !reduceMotion) {
            bounce.animateTo(1.18f, ViraMotion.quick())
            bounce.animateTo(1f, ViraMotion.emphasis())
        }
    }
    val done = doneTint ?: ink
    val pending = pendingTint ?: ink
    val side = 26.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Box(modifier.viraTouchTarget(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = isDone,
            transitionSpec = {
                if (reduceMotion) fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                else fadeIn(ViraMotion.quick()) togetherWith fadeOut(ViraMotion.quick())
            },
            label = "completion",
        ) { checked ->
            Canvas(
                Modifier.size(side).graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            ) {
                val stroke = size.width * 0.08f
                if (checked) {
                    drawCircle(done)
                    val check = Path().apply {
                        moveTo(size.width * 0.28f, size.height * 0.52f)
                        lineTo(size.width * 0.44f, size.height * 0.67f)
                        lineTo(size.width * 0.73f, size.height * 0.36f)
                    }
                    // La ✓ es buida del cercle, com el símbol ple d'iOS.
                    drawPath(
                        check,
                        Color.Black,
                        style = Stroke(width = stroke * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = BlendMode.Clear,
                    )
                } else {
                    drawCircle(pending, radius = size.width / 2f - stroke / 2f, style = Stroke(stroke))
                }
            }
        }
    }
}
