package cat.vira.android.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing

/// La peça destacada (`ShopFeaturedCard`): **la següent que pots tenir**, la més barata que encara no
/// és teva, amb la barra del que portes estalviat. No és una oferta: no canvia cada dia, no té
/// rellotge ni descompte, i es mou sola quan la compres (§2.5).
///
/// Si has marcat una skin amb «La vull» al provador, la targeta és aquella: l'objectiu el tries tu.
/// Quan ja t'hi arribes, ho diu en gran («Ja la pots tenir!»), brilla amb la vora daurada del
/// moneder i convida a emprovar-te-la. La barra ja no cal.
@Composable
fun ShopFeaturedCard(item: SkinItem, balance: Int, action: () -> Unit, modifier: Modifier = Modifier, isWish: Boolean = false) {
    val area = item.tier.shopArea
    val ink = ViraColor.onCard(area)
    val missing = maxOf(0, item.price - balance)
    val isReady = missing == 0
    val title = stringResource(
        when {
            isReady -> R.string.shop_featured_ready
            isWish -> R.string.shop_wish_add
            else -> R.string.shop_featured_title
        },
    )
    val titleSymbol = when {
        isReady -> ShopSymbol.Sparkles
        isWish -> ShopSymbol.Heart
        else -> ShopSymbol.Star
    }
    val name = stringResource(item.nameRes)
    val tier = stringResource(item.tier.nameRes)
    val status = if (missing > 0) {
        stringResource(R.string.shop_missing, missing.viraFormatted())
    } else {
        stringResource(R.string.shop_price, item.price.viraFormatted())
    }
    val hint = stringResource(R.string.shop_accessibility_details)
    val semantics = Modifier.clearAndSetSemantics {
        contentDescription = listOf(title, name, tier, status).joinToString(", ")
        role = Role.Button
        onClick(label = hint) { action(); true }
    }

    val info: @Composable (Modifier) -> Unit = { infoModifier ->
        Column(infoModifier.padding(ViraSpacing.s), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                ShopSymbolIcon(titleSymbol, ink, size = 16.dp)
                Text(title, style = ViraFont.headline, color = ink)
            }
            ShopRibbon(tier, item.tier.symbol, area)
            Text(name, style = ViraFont.title, color = ink)
            if (isReady) {
                val gold = ViraColor.onCard(ViraArea.Rewards)
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
                        .background(ViraGradient.card(ViraArea.Rewards), RoundedCornerShape(ViraRadius.s))
                        .padding(horizontal = ViraSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterHorizontally),
                ) {
                    ShopSymbolIcon(ShopSymbol.Sparkles, gold, size = 16.dp)
                    Text(stringResource(R.string.shop_featured_try), style = ViraFont.headline, color = gold)
                }
            } else {
                ShopProgressBar(
                    value = balance, total = item.price, area = area, ink = ink,
                    label = stringResource(R.string.shop_featured_progress, minOf(balance, item.price).viraFormatted(), item.price.viraFormatted()),
                )
                ShopPriceLabel(ShopPriceKind.Missing(missing))
            }
        }
    }
    val frame = modifier.fillMaxWidth().then(semantics)
        // El reflex de les vitrines èpiques, aquí per dir «ja és a l'abast».
        .shopTierShine(if (isReady) SkinTier.Epic else null)
        .shopFrame(area, ink)
        .then(if (isReady) Modifier.border(4.dp, ViraColor.cardFill(ViraArea.Rewards), RoundedCornerShape(ViraRadius.l)) else Modifier)
        .viraPressable(onClick = action)

    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(frame) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.4f).clipToBounds()) {
                SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize())
            }
            info(Modifier.fillMaxWidth())
        }
    } else {
        // L'escena va al costat i fa l'alçada que mana el text.
        Row(frame.height(IntrinsicSize.Min)) {
            Box(Modifier.width(ART_WIDTH).fillMaxHeight().clipToBounds()) {
                SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize())
            }
            info(Modifier.weight(1f))
        }
    }
}

private val ART_WIDTH = 132.dp
