package cat.vira.android.features.shop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/// «Mantén per comprar» (`ShopHoldToBuyButton`, Arnau, 2026-10-21): el botó es va omplint mentre el
/// mantens i les monedes hi van caient, un toc de vibració cada una. Deixar-lo anar abans d'hora no
/// compra res.
///
/// No és una confirmació —cap diàleg ni cap «segur?»—: és el gest mateix el que fa que no es pugui
/// gastar per error amb un toc distret, i alhora converteix el pagament en un moment de joc.
///
/// **TalkBack i Accés amb interruptors** no poden mantenir res: per a ells és un botó normal, i
/// l'acció per defecte compra a l'instant.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopHoldToBuyButton(price: Int, onBuy: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val shape = RoundedCornerShape(ViraRadius.s)
    val label = stringResource(R.string.shop_buy)
    val value = stringResource(R.string.shop_price, price.viraFormatted())
    val hint = stringResource(R.string.shop_hold_hint)

    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .scale(if (reduceMotion) 1f else 1f - progress.value * 0.03f)
            .clip(shape)
            .background(ViraColor.accent(ViraArea.Today), shape)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = "$value. $hint"
                role = Role.Button
                onClick(label = label) { onBuy(); true }
            }
            .pointerInput(price) {
                awaitEachGesture {
                    awaitFirstDown()
                    val fill = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                    }
                    val ticks = scope.launch {
                        val step = HOLD_MILLIS / COIN_TICKS
                        repeat(COIN_TICKS) { index ->
                            delay(if (index == 0) 50L else step.toLong())
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    // `null` vol dir que ha passat el temps; un gest cancel·lat (scroll, dit fora) torna un
                    // `Unit` com deixar-lo anar, i no compra.
                    val ended = withTimeoutOrNull(HOLD_MILLIS.toLong()) { waitForUpOrCancellation(); Unit }
                    ticks.cancel()
                    if (ended == null) {
                        // Ha aguantat fins al final: es compra, i el que quedi del gest no fa res més.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBuy()
                        waitForUpOrCancellation()
                    } else {
                        // Deixar-lo anar abans d'hora: el botó es buida i no ha passat res.
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, tween(180)) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize()) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value.coerceIn(0f, 1f)).background(ViraColor.onAccent.copy(alpha = 0.24f)))
        }
        // En fila mentre hi cap; amb la lletra gran, el preu baixa sencer a sota i mai es parteix.
        FlowRow(
            Modifier.padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            Text(
                stringResource(R.string.shop_hold_buy), style = ViraFont.headline, color = ViraColor.onAccent,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.CenterVertically),
            )
            Row(
                Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                ViraPictogram(R.drawable.picto_shop, size = 22.dp, scalesWithText = true)
                Text(price.viraFormatted(), style = ViraFont.headline.tabular(), color = ViraColor.onAccent, maxLines = 1, softWrap = false)
            }
        }
    }
}

/// Prou llarg per no ser un toc, prou curt per no fer-se pesat.
private const val HOLD_MILLIS = 1_200
/// Les monedes que cauen mentre el mantens.
private const val COIN_TICKS = 6
