package cat.vira.android.features.entry

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.viraSpec

/// La frase de Viri a cada pas (`EntryViriLine.swift`).
///
/// - **≤15 paraules**: el límit el vigila el catàleg, no la vista.
/// - **Mai text sobre el degradat ni sobre la il·lustració**: la bombolla és una superfície
///   (`surface(Rewards)`).
/// - No és la capçalera del pas —ho és el títol— però sí el primer que llegeix TalkBack.
/// - Apareix sense efecte de màquina d'escriure: el missatge és la frase, no l'animació.
///
/// `tailOnTop` dins de la fulla (Viri a sobre); a l'escenari la cua surt de sota i apunta al cap.
/// A mides d'accessibilitat la cua desapareix: la bombolla passa a ser un bloc a tota amplada.
@Composable
fun EntryViriLine(text: String, modifier: Modifier = Modifier, tailOnTop: Boolean = true) {
    val environment = LocalViraEnvironment.current
    val hasTail = !environment.isAccessibilitySize
    val fill = ViraColor.surface(ViraArea.Rewards)
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val shape = RoundedCornerShape(ViraRadius.m)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(if (shown) 1f else 0f, viraSpec(ViraMotion.entrance()), label = "viriLine")
    val scale = if (environment.reduceMotion) 1f else 0.85f + 0.15f * progress

    Box(
        contentAlignment = if (tailOnTop) Alignment.TopStart else Alignment.BottomCenter,
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true; traversalIndex = -2f },
    ) {
        Box(
            Modifier
                .padding(top = if (tailOnTop && hasTail) TAIL else 0.dp, bottom = if (!tailOnTop && hasTail) TAIL else 0.dp)
                .graphicsLayer {
                    alpha = progress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = if (tailOnTop) TransformOrigin(0f, 0f) else TransformOrigin(0.5f, 1f)
                }
                .align(if (tailOnTop) Alignment.TopStart else Alignment.BottomCenter),
        ) {
            Text(
                text,
                style = ViraFont.headline,
                color = ink,
                modifier = Modifier
                    .then(if (hasTail) Modifier else Modifier.fillMaxWidth())
                    .background(fill, shape)
                    .then(if (environment.increasedContrast) Modifier.border(1.dp, ink, shape) else Modifier)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
            )
            if (hasTail) {
                Box(
                    Modifier
                        .align(if (tailOnTop) Alignment.TopStart else Alignment.BottomCenter)
                        .offset(
                            x = if (tailOnTop) ViraSpacing.l else 0.dp,
                            y = if (tailOnTop) -TAIL + 0.5.dp else TAIL - 0.5.dp,
                        )
                        .size(TAIL * 2, TAIL)
                        .background(fill, if (tailOnTop) TAIL_UP else TAIL_DOWN),
                )
            }
        }
    }
}

private val TAIL = 10.dp

private val TAIL_UP = GenericShape { size, _ ->
    moveTo(0f, size.height); lineTo(size.width / 2f, 0f); lineTo(size.width, size.height); close()
}

private val TAIL_DOWN = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width / 2f, size.height); lineTo(size.width, 0f); close()
}
