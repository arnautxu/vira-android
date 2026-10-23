package cat.vira.android.features.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay

/// El vestidor (`ShopDressingRoom`, Arnau, 2026-10-21): en comprar una skin, la cortina es tanca, es
/// veu l'ombra de Viri canviant-se, la cortina es sacseja i s'obre sobre la revelació
/// (`ShopRevealScreen`).
///
/// A l'iPhone la cortina espera el paquet de la skin vestida. Android encara no en té (arriben amb
/// Google Play), així que dura només les sacsejades de la raresa: una les comunes, cinc les
/// llegendàries. Cap barra ni percentatge: Viri s'està canviant.
///
/// Amb Reduir moviment la cortina no tremola ni llisca: s'esvaeix.
@Composable
fun ShopDressingRoom(
    item: SkinItem,
    onContinue: () -> Unit,
    celebrationsEnabled: Boolean = true,
    /// Per a les captures: la cortina tancada, sense seqüència.
    holdsClosed: Boolean = false,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val title = stringResource(R.string.shop_dressing_title)
    var isOpen by rememberSaveable(item.key) { mutableStateOf(false) }
    val angle = remember { Animatable(0f) }

    LaunchedEffect(item.key, holdsClosed) {
        if (isOpen || holdsClosed) return@LaunchedEffect
        view.announceForAccessibility(title)
        repeat(item.tier.curtainShakes) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (!reduceMotion) {
                angle.animateTo(0f, keyframes {
                    durationMillis = 440
                    1.6f at 80
                    -1.4f at 200
                    0.8f at 300
                    0f at 440
                })
            }
            delay(if (reduceMotion) 520L else 80L)
        }
        isOpen = true
    }

    val opening by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 120f),
        label = "curtain",
    )

    Box(Modifier.fillMaxSize().background(ViraColor.background)) {
        if (isOpen) ShopRevealScreen(item, onContinue, celebrationsEnabled)
        if (opening < 1f) {
            Curtain(item, angle.value, opening, reduceMotion)
        }
        AnimatedVisibility(
            visible = !isOpen, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().background(ViraColor.background).padding(ViraSpacing.l)
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                ShopRibbon(stringResource(item.tier.nameRes), item.tier.symbol, item.tier.shopArea, isStrong = true)
                Text(title, style = ViraFont.title, color = ViraColor.textPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

/// Les dues meitats de vellut i l'ombra de Viri, sempre la del Viri de sempre: la skin encara no
/// s'ha de veure.
@Composable
private fun Curtain(item: SkinItem, angle: Float, opening: Float, reduceMotion: Boolean) {
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer {
                // Una mica més gran que la pantalla: en sacsejar-se no se n'han de veure les vores.
                scaleX = 1.06f
                scaleY = 1.06f
                rotationZ = angle
                transformOrigin = TransformOrigin(0.5f, 0f)
                alpha = if (reduceMotion) 1f - opening else 1f
            },
    ) {
        val half = maxWidth / 2
        val slide = if (reduceMotion) 0f else opening
        Row(Modifier.fillMaxSize()) {
            ShopVelvet(item.tier.shopArea, Modifier.weight(1f).fillMaxSize().graphicsLayer { translationX = -half.toPx() * slide })
            ShopVelvet(item.tier.shopArea, Modifier.weight(1f).fillMaxSize().graphicsLayer { translationX = half.toPx() * slide })
        }
        Image(
            painterResource(R.drawable.viri_hero), null,
            Modifier.align(Alignment.Center).widthIn(max = 300.dp)
                .graphicsLayer { rotationZ = -6f }
                .blur(3.dp)
                .alpha(0.4f * (1f - opening)),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color.Black),
        )
    }
}

/// Quantes vegades es sacseja la cortina: com més rara, més espectacle.
val SkinTier.curtainShakes: Int
    get() = when (this) {
        SkinTier.Common -> 1
        SkinTier.Special -> 2
        SkinTier.Epic -> 3
        SkinTier.Legendary -> 5
    }
