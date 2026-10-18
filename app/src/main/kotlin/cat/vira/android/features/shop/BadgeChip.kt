package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing

/// Un badge dibuixat (`BadgeChip`): al prestatge del banner, al triador i a la Botiga. Guanyat i
/// comprat no es distingeixen pel color: el guanyat porta anella i punts de raresa (forma). Bloquejat
/// tampoc és només opacitat: porta cadenat. Decoratiu per a TalkBack: el nom el diu qui el posa.
@Composable
fun BadgeChip(item: BadgeItem, modifier: Modifier = Modifier, size: Dp = 52.dp, isAvailable: Boolean = true) {
    val rarity = if (item.isEarned) CardItem.item(item.key)?.rarity else null
    val earnedAndAvailable = item.isEarned && isAvailable
    val medal = maxOf(size * 0.78f, 32.dp)
    val ringWidth = if (rarity != null && isAvailable) rarity.borderWidth.dp else 1.dp
    val rewardsInk = ViraColor.onSurface(ViraArea.Rewards)
    Box(modifier.size(size).clearAndSetSemantics { }) {
        Box(Modifier.matchParentSize().alpha(if (isAvailable) 1f else 0.42f), contentAlignment = Alignment.BottomCenter) {
            // El pedestal fa que es llegeixi com una insígnia, no com una icona solta.
            val pedestal = RoundedCornerShape(maxOf(size * 0.08f, 3.dp))
            Box(
                Modifier.padding(bottom = maxOf(size * 0.02f, 1.dp))
                    .size(size * 0.48f, maxOf(size * 0.15f, 6.dp))
                    .background(ViraColor.surface(ViraArea.Rewards), pedestal)
                    .border(1.dp, rewardsInk.copy(alpha = 0.35f), pedestal),
            )
            Box(Modifier.align(Alignment.Center).size(medal), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.matchParentSize().then(
                        if (earnedAndAvailable) Modifier.background(ViraGradient.card(ViraArea.Rewards), CircleShape)
                        else Modifier.background(ViraColor.surfaceElevated, CircleShape),
                    ),
                )
                Box(
                    Modifier.matchParentSize().padding(ViraSpacing.xxs).border(
                        ringWidth,
                        if (earnedAndAvailable) ViraColor.onCard(ViraArea.Rewards).copy(alpha = 0.48f) else ViraColor.separator,
                        CircleShape,
                    ),
                )
                Box(
                    Modifier.matchParentSize().padding(ViraSpacing.xxs + 1.dp).border(
                        if (earnedAndAvailable) 1.5.dp else 1.dp,
                        if (earnedAndAvailable) ViraColor.accent(ViraArea.Rewards) else ViraColor.separator,
                        CircleShape,
                    ),
                )
                ViraPictogram(item.pictogram, size = maxOf(size * 0.48f, 20.dp))
            }
            if (rarity != null && isAvailable) {
                Row(
                    Modifier.offset(y = maxOf(size * 0.045f, 2.dp))
                        .background(ViraColor.surface(ViraArea.Rewards), CircleShape)
                        .border(0.75.dp, rewardsInk.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = maxOf(size * 0.045f, 2.dp), vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(maxOf(size * 0.025f, 1.dp)),
                ) {
                    repeat(rarity.ordinal + 1) {
                        Box(Modifier.size(maxOf(size * 0.055f, 2.dp), maxOf(size * 0.04f, 2.dp)).background(rewardsInk, CircleShape))
                    }
                }
            }
        }
        if (!isAvailable) {
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .background(ViraColor.surfaceElevated, CircleShape)
                    .border(1.dp, ViraColor.separator, CircleShape)
                    .padding(ViraSpacing.xxs),
            ) { ShopSymbolIcon(ShopSymbol.Lock, rewardsInk, size = 12.dp) }
        }
    }
}

/// Ranura buida del prestatge (`BadgeSlot`): convida, per això porta un «+» i vora de punts.
@Composable
fun BadgeSlot(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val ink = ViraColor.onSurface(ViraArea.Rewards)
    Box(
        modifier.size(size).background(ViraColor.surface(ViraArea.Rewards), CircleShape).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = 1.5.dp.toPx()
            drawCircle(
                ink.copy(alpha = 0.4f), radius = this.size.minDimension / 2f - w / 2f,
                style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
        ShopSymbolIcon(ShopSymbol.Plus, ink, size = 14.dp)
    }
}
