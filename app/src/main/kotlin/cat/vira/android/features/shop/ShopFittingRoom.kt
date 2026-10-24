package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// El provador (`ShopFittingRoom`, Arnau, 2026-10-21): tocar una skin que no tens la posa a Viri a
/// l'escenari, gran, abans de comprar-la. Des del 2026-10-22 la Botiga només ven skins, i el provador
/// substitueix el full de detall pla.
///
/// L'acció de sota diu sempre en què quedes, en text:
/// - t'hi arribes → «Mantén per comprar» i el saldo que et quedarà;
/// - no t'hi arribes → quant et falta, el ritme («Al teu ritme, uns 9 dies», amb la teva mitjana de
///   monedes dels darrers catorze dies) i «La vull», que la fa el teu objectiu a la Botiga. Sense
///   vermell i sense cap pressa: és una fita teva.
///
/// «Com sona» no hi és: els sons de cada skin arriben amb les skins vestides (Google Play). El que
/// tampoc hi ha, igual que a l'iPhone (§2.5): disponibilitat, data de sortida ni cap compte enrere.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopFittingRoom(
    item: SkinItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    dailyPace: Double?,
    wishKey: String,
    onWish: (String) -> Unit,
    onBuy: () -> Unit,
    onWear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.background,
    ) {
        ShopFittingRoomContent(
            item, isOwned, isEquipped, balance, dailyPace, wishKey, onWish,
            onBuy = { onBuy(); onDismiss() }, onWear = { onWear(); onDismiss() }, onClose = onDismiss,
        )
    }
}

/// El contingut del provador, separat perquè es pugui capturar sense el full.
@Composable
fun ShopFittingRoomContent(
    item: SkinItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    dailyPace: Double?,
    wishKey: String,
    onWish: (String) -> Unit,
    onBuy: () -> Unit,
    onWear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ViraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ViraActionButton(stringResource(R.string.action_close), onClose, hierarchy = ViraActionHierarchy.Textual)
            ShopWallet(balance, Modifier.padding(end = ViraSpacing.xs))
        }
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            ShopStage(item, isTryingOn = !isOwned, height = shopStageHeight(large, inFittingRoom = true))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                Text(
                    stringResource(item.nameRes), style = ViraFont.largeTitle, color = ViraColor.textPrimary,
                    textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.shop_detail_cosmetic), style = ViraFont.footnote,
                    color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                )
            }
            FittingAction(item, isOwned, isEquipped, balance, dailyPace, wishKey, onWish, onBuy, onWear)
        }
    }
}

@Composable
private fun FittingAction(
    item: SkinItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    balance: Int,
    dailyPace: Double?,
    wishKey: String,
    onWish: (String) -> Unit,
    onBuy: () -> Unit,
    onWear: () -> Unit,
) {
    val missing = maxOf(0, item.price - balance)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        when {
            isEquipped -> Row(
                Modifier.defaultMinSize(minHeight = ViraTouch.minimum),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                ShopSymbolIcon(ShopSymbol.CheckCircle, ViraColor.accent(ViraArea.Today), size = 18.dp)
                Text(stringResource(R.string.shop_equipped), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Today))
            }
            isOwned -> ViraActionButton(stringResource(R.string.shop_wear), onWear, Modifier.fillMaxWidth(), textStyle = ViraFont.headline)
            missing == 0 -> {
                ShopHoldToBuyButton(item.price, onBuy)
                Text(
                    stringResource(R.string.shop_detail_balance_after, item.price.viraFormatted(), (balance - item.price).viraFormatted()),
                    style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                )
            }
            else -> {
                ShopProgressBar(
                    value = balance, total = item.price, area = item.tier.shopArea, ink = ViraColor.textPrimary,
                    label = stringResource(R.string.shop_featured_progress, balance.viraFormatted(), item.price.viraFormatted()),
                )
                Text(
                    stringResource(R.string.shop_missing, missing.viraFormatted()),
                    style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                )
                ShopCollection.daysToReach(missing, dailyPace)?.let { days ->
                    Text(
                        if (days == 1) stringResource(R.string.shop_pace_one) else stringResource(R.string.shop_pace_days, days.viraFormatted()),
                        style = ViraFont.headline.tabular(), color = ViraColor.textPrimary, textAlign = TextAlign.Center,
                    )
                }
                ShopWishButton(isWished = wishKey == item.key, onToggle = { onWish(if (wishKey == item.key) "" else item.key) })
            }
        }
    }
}

/// «La vull»: un sol objectiu alhora, i tornar-la a tocar el treu. Es desa al telèfon i prou: és una
/// preferència de pantalla, no una dada de l'usuari que hagi de viatjar.
@Composable
private fun ShopWishButton(isWished: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val label = stringResource(if (isWished) R.string.shop_wish_on else R.string.shop_wish_add)
    val shape = RoundedCornerShape(ViraRadius.s)
    val (fill, ink) = if (isWished) {
        ViraColor.accent(ViraArea.Today).copy(alpha = 0.15f) to ViraColor.onSurface(ViraArea.Today)
    } else {
        ViraColor.accent(ViraArea.Today) to ViraColor.onAccent
    }
    val toggle = {
        ViraHaptics.play(context, ViraHaptics.Feedback.Selection)
        onToggle()
    }
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable)
            .background(fill, shape)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isWished
                onClick { toggle(); true }
            }
            .viraPressable(onClick = toggle)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ShopSymbolIcon(if (isWished) ShopSymbol.Heart else ShopSymbol.HeartOutline, ink, size = 18.dp)
            Text(label, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center)
        }
    }
}
