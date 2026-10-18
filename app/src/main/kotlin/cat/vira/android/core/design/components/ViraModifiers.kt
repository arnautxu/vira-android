package cat.vira.android.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec

/// Fons tenyit amb el color de la secció (`ViraSectionBackground`): la secció es reconeix abans de
/// llegir res. Cada pantalla d'una àrea l'ha de fer servir (`DESIGN_RULES.md` §7).
@Composable
fun Modifier.viraSectionBackground(area: ViraArea): Modifier = background(ViraGradient.section(area))

/// Àrea tàctil mínima (48 dp) sense canviar la mida visual de l'element.
fun Modifier.viraTouchTarget(size: Dp = ViraTouch.minimum): Modifier =
    defaultMinSize(minWidth = size, minHeight = size)

/// Toc amb enfonsament lleuger (`ViraPressableButtonStyle`): l'estil de qualsevol targeta o fila
/// tocable. Escala, no opacitat, perquè la targeta ja porta color propi. Amb Reduir moviment, cap
/// canvi d'escala: un enfosquiment breu, que respon sense desplaçar res.
fun Modifier.viraPressable(
    onClickLabel: String? = null,
    role: Role? = Role.Button,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.96f else 1f,
        animationSpec = viraSpec(ViraMotion.quick(), decorative = true),
        label = "viraPressable",
    )
    val shade = ViraColor.shadow
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawWithContent {
            drawContent()
            if (pressed && reduceMotion) drawRect(shade.copy(alpha = 0.08f))
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

/// Un bot curt quan `isOn` passa a cert (la pestanya triada, l'hàbit marcat). Només en encendre's,
/// i amb Reduir moviment, res: el canvi ja el diu el color o el número.
fun Modifier.viraPop(isOn: Boolean, scale: Float = 1.15f): Modifier = composed {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val value = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(isOn) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (isOn && !reduceMotion) bounce(value, scale)
    }
    graphicsLayer { scaleX = value.value; scaleY = value.value }
}

/// El mateix bot, cada cop que `trigger` canvia (la flama quan la ratxa creix).
fun Modifier.viraPop(trigger: Int, scale: Float = 1.15f): Modifier = composed {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val value = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (!reduceMotion) bounce(value, scale)
    }
    graphicsLayer { scaleX = value.value; scaleY = value.value }
}

private suspend fun bounce(value: Animatable<Float, AnimationVector1D>, peak: Float) {
    value.animateTo(peak, tween(120))
    value.animateTo(1f, spring(dampingRatio = 0.6f))
}

/// Viri «respira» quan no fa res: un vaivé del 2 % ancorat als peus. Decoratiu: amb Reduir
/// moviment, quiet del tot.
fun Modifier.viraIdleBreath(): Modifier = composed {
    if (LocalViraEnvironment.current.reduceMotion) return@composed this
    val transition = rememberInfiniteTransition(label = "viraIdleBreath")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            tween(ViraMotion.BREATH_MILLIS, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breath",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}
