package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraShopBackdrop
import cat.vira.android.core.design.tokens.ViraSpacing

/// El que la Botiga ensenya d'una peça (`ShopDisplayItem`): una skin, un badge o el Viri de sempre.
data class ShopDisplayItem(
    val id: String,
    @DrawableRes val art: Int?,
    @DrawableRes val cutout: Int?,
    @StringRes val nameRes: Int,
    /// `null` quan no es compra: el Viri de sempre, sempre gratuït.
    val price: Int?,
    val tier: SkinTier?,
    val backdrop: ViraShopBackdrop,
    /// Una skin és una escena que omple el marc; un badge és un emblema centrat sobre el color.
    val isEmblem: Boolean = false,
) {
    companion object {
        /// Treure's una skin no costa res ni ha de semblar una pèrdua.
        val defaultViri = ShopDisplayItem(
            id = "default", art = R.drawable.viri_entry_waving, cutout = null, nameRes = R.string.shop_default,
            price = null, tier = null, backdrop = ViraShopBackdrop.Neutral,
        )
    }
}

val SkinItem.display: ShopDisplayItem
    get() = ShopDisplayItem(key, backdropArt, cutoutArt, nameRes, price, tier, shopBackdrop)

val BadgeItem.display: ShopDisplayItem
    get() = ShopDisplayItem(key, pictogram, null, nameRes, price, null, shopBackdrop, isEmblem = true)

/// Què diu el botó de preu d'una peça.
fun shopPriceKind(price: Int?, balance: Int, isOwned: Boolean, isEquipped: Boolean): ShopPriceKind = when {
    isEquipped -> ShopPriceKind.Equipped
    isOwned || price == null -> ShopPriceKind.Owned
    price > balance -> ShopPriceKind.Missing(price - balance)
    else -> ShopPriceKind.Price(price)
}

/// El que TalkBack diu de l'estat d'una peça, després del nom.
@Composable
fun shopStatusDescription(kind: ShopPriceKind, isDefault: Boolean = false): String = when (kind) {
    ShopPriceKind.Equipped -> stringResource(R.string.shop_equipped)
    ShopPriceKind.Owned -> stringResource(if (isDefault) R.string.shop_default_detail else R.string.shop_owned)
    is ShopPriceKind.Missing -> stringResource(R.string.shop_missing, kind.amount.viraFormatted())
    is ShopPriceKind.Price -> stringResource(R.string.shop_price, kind.amount.viraFormatted())
}

/// Una fitxa de la graella (`ShopItemCard`): targeta del color de la raresa, l'escena a dalt, el nom
/// i el botó de preu a sota. El preu no apaga mai la peça: si no t'hi arribes, el botó diu quant falta.
@Composable
fun ShopItemCard(
    item: ShopDisplayItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    action: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val area = item.tier?.shopArea ?: ViraArea.Today
    val ink = ViraColor.onCard(area)
    val kind = shopPriceKind(item.price, balance, isOwned, isEquipped)
    val name = stringResource(item.nameRes)
    val tierName = item.tier?.let { stringResource(it.nameRes) }
    val status = shopStatusDescription(kind, isDefault = item.price == null)
    val hint = stringResource(
        when {
            isEquipped -> R.string.shop_accessibility_equipped
            isOwned || item.price == null -> R.string.shop_accessibility_wear
            else -> R.string.shop_accessibility_details
        },
    )
    Column(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = listOfNotNull(tierName, status).joinToString(", ")
                role = Role.Button
                selected = isEquipped
                onClick(label = hint) { action(); true }
            }
            .shopFrame(area, ink)
            .viraPressable(onClick = action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds()) {
            SkinPortrait(item.art, item.cutout, Modifier.matchParentSize())
        }
        Text(
            name, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.padding(horizontal = ViraSpacing.xxs),
        )
        Spacer(Modifier.weight(1f))
        ShopPriceLabel(kind, Modifier.padding(start = ViraSpacing.xs, end = ViraSpacing.xs, bottom = ViraSpacing.xs))
    }
}
