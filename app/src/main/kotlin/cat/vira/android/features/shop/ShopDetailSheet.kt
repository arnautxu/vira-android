package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// El detall d'una peça amb l'acció a dins (`ShopDetailSheet`), en full que puja des de baix. La
/// peça es veu gran, el preu i el que et quedarà són explícits i el botó és un de sol. No és una
/// acció destructiva: cap vermell ni avís, i es tanca amb un gest.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailSheet(
    item: ShopDisplayItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    onBuy: () -> Unit,
    onWear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.background,
    ) {
        ShopDetailContent(item, isOwned, isEquipped, balance, onBuy = { onBuy(); onDismiss() }, onWear = { onWear(); onDismiss() }, onClose = onDismiss)
    }
}

/// El contingut del full, separat perquè es pugui capturar sense el full.
@Composable
fun ShopDetailContent(
    item: ShopDisplayItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    onBuy: () -> Unit,
    onWear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val price = item.price ?: 0
    val tint = ViraColor.onShopBackdrop(item.backdrop)
    val backdropColor = ViraColor.shopBackdrop(item.backdrop)
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ViraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs)) {
            ViraActionButton(
                stringResource(R.string.action_close), onClose,
                hierarchy = cat.vira.android.core.design.components.ViraActionHierarchy.Textual,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            if (item.isEmblem && item.art != null) {
                Box(
                    Modifier.fillMaxWidth().height(260.dp).background(backdropColor).clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) { ViraPictogram(item.art, size = 150.dp) }
            } else {
                Box(Modifier.fillMaxWidth().height(300.dp).clipToBounds()) {
                    SkinPortrait(item.art, null, Modifier.matchParentSize())
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp)
                            .background(Brush.verticalGradient(listOf(backdropColor.copy(alpha = 0f), backdropColor))),
                    )
                    if (item.cutout != null) {
                        SkinPortrait(null, item.cutout, Modifier.matchParentSize().offset(y = 24.dp))
                    }
                }
            }
            Text(
                stringResource(item.nameRes),
                style = ViraFont.title, color = tint, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(ViraGradient.shopBackdrop(item.backdrop))
                    .padding(start = ViraSpacing.screenMargin, end = ViraSpacing.screenMargin, bottom = ViraSpacing.l),
            )
        }
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            item.tier?.let { TierBadge(it) }
            Text(
                stringResource(R.string.shop_detail_cosmetic),
                style = ViraFont.footnote, color = ViraColor.textSecondary, textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            when {
                isEquipped -> Row(
                    Modifier.defaultMinSize(minHeight = ViraTouch.minimum),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                ) {
                    ShopSymbolIcon(ShopSymbol.Check, ViraColor.accent(ViraArea.Today), size = 18.dp)
                    Text(stringResource(R.string.shop_equipped), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Today))
                }
                isOwned || item.price == null ->
                    ViraActionButton(stringResource(R.string.shop_wear), onWear, Modifier.fillMaxWidth())
                balance >= price -> {
                    ViraActionButton(stringResource(R.string.shop_buy), onBuy, Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.shop_detail_balance_after, price.viraFormatted(), (balance - price).viraFormatted()),
                        style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    ViraActionButton(stringResource(R.string.shop_buy), {}, Modifier.fillMaxWidth(), enabled = false)
                    Text(
                        stringResource(R.string.shop_missing, (price - balance).viraFormatted()),
                        style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/// La píndola de la raresa al detall: nom, símbol i vora més gruixuda com més rara.
@Composable
private fun TierBadge(tier: SkinTier) {
    val area = tier.shopArea
    Row(
        Modifier
            .background(ViraColor.cardFill(area), CircleShape)
            .border(tier.borderWidth.dp, ViraColor.accent(area), CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ShopSymbolIcon(tier.symbol, ViraColor.onCard(area), size = 12.dp)
        Text(stringResource(tier.nameRes), style = ViraFont.caption, color = ViraColor.onCard(area))
    }
}
