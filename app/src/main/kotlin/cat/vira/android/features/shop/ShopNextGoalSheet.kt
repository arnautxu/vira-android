package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
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

/// «I ara, la següent» (`ShopNextGoalSheet`, Arnau, 2026-10-22): en tancar la revelació d'una compra,
/// la col·lecció ja suma la nova i proposem tres objectius, un per raresa (`ShopCollection.nextGoals`).
///
/// Un toc en marca un com a objectiu («La vull», el mateix de sempre: un sol objectiu alhora);
/// «Fet» el desa i «Després» tanca sense tocar res. Cap pressa, cap rellotge i res a l'atzar: és una
/// tria teva, i la pots canviar quan vulguis des del provador.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopNextGoalSheet(
    bought: SkinItem,
    catalog: List<SkinItem>,
    ownedKeys: Set<String>,
    balance: Int,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.background,
    ) {
        ShopNextGoalContent(bought, catalog, ownedKeys, balance, onChoose = { onChoose(it); onDismiss() }, onLater = onDismiss)
    }
}

/// El contingut del full, separat perquè es pugui capturar.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopNextGoalContent(
    bought: SkinItem,
    catalog: List<SkinItem>,
    ownedKeys: Set<String>,
    balance: Int,
    onChoose: (String) -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
    initialChoice: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    var choice by rememberSaveable { mutableStateOf(initialChoice) }
    val options = ShopCollection.nextGoals(catalog, ownedKeys)
    val (owned, total) = ShopCollection.progress(catalog, ownedKeys)
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ViraSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs)) {
            ViraActionButton(stringResource(R.string.shop_next_later), onLater, hierarchy = ViraActionHierarchy.Textual)
        }
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                ShopCollectionMeter(owned, total)
                Text(
                    stringResource(R.string.shop_next_owned, stringResource(bought.nameRes)),
                    style = ViraFont.footnote, color = ViraColor.textSecondary,
                )
                // Quantes en tens de cada raresa, amb el símbol i el nom: mai només el color.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                ) {
                    SkinTier.entries.forEach { tier ->
                        val items = catalog.filter { it.tier == tier }
                        Row(
                            Modifier.semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                        ) {
                            ShopRibbon(stringResource(tier.nameRes), tier.symbol, tier.shopArea)
                            ShopCollectionCount(items.count { it.key in ownedKeys }, items.size)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.shop_next_title), style = ViraFont.title, color = ViraColor.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                options.forEach { item ->
                    GoalOption(item, balance, isChosen = choice == item.key) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        choice = if (choice == item.key) null else item.key
                    }
                }
            }
            ViraActionButton(
                stringResource(R.string.action_done),
                { choice?.let { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChoose(it) } ?: onLater() },
                Modifier.fillMaxWidth(), textStyle = ViraFont.headline,
            )
        }
    }
}

/// Una proposta. Amb la lletra gran, sense miniatura i el cor a sota: en fila el nom es partia.
@Composable
private fun GoalOption(item: SkinItem, balance: Int, isChosen: Boolean, onToggle: () -> Unit) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val missing = maxOf(0, item.price - balance)
    val name = stringResource(item.nameRes)
    val tier = stringResource(item.tier.nameRes)
    val status = if (missing == 0) {
        stringResource(R.string.shop_ready_tag)
    } else {
        stringResource(R.string.shop_featured_progress, balance.viraFormatted(), item.price.viraFormatted())
    }
    val hint = stringResource(R.string.shop_next_hint)
    val shape = RoundedCornerShape(ViraRadius.m)
    val content = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            ShopRibbon(tier, item.tier.symbol, item.tier.shopArea)
            Text(name, style = ViraFont.headline, color = ViraColor.textPrimary)
            ShopMiniBar(minOf(balance, item.price), item.price, ViraColor.accent(item.tier.shopArea))
            Text(status, style = ViraFont.caption.tabular(), color = ViraColor.textSecondary)
        }
    }
    val heart = @Composable {
        Box(
            Modifier.size(ViraTouch.minimum).clip(CircleShape)
                .background(if (isChosen) ViraColor.accent(ViraArea.Today) else ViraColor.surfaceElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ShopSymbolIcon(
                if (isChosen) ShopSymbol.Heart else ShopSymbol.HeartOutline,
                if (isChosen) ViraColor.onAccent else ViraColor.accent(ViraArea.Today), size = 20.dp,
            )
        }
    }
    val frame = Modifier.fillMaxWidth()
        .clearAndSetSemantics {
            contentDescription = "$name, $tier"
            stateDescription = status
            role = Role.Button
            selected = isChosen
            onClick(label = hint) { onToggle(); true }
        }
        .background(if (isChosen) ViraColor.surface(ViraArea.Today) else ViraColor.surfaceElevated, shape)
        .border(if (isChosen) 2.dp else 1.dp, if (isChosen) ViraColor.accent(ViraArea.Today) else ViraColor.separator, shape)
        .viraPressable(onClick = onToggle)
        .padding(ViraSpacing.xs)
    if (large) {
        Column(frame, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { content(); heart() }
    } else {
        Row(frame, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(ViraRadius.s)).clipToBounds()) {
                SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize())
            }
            Box(Modifier.weight(1f)) { content() }
            heart()
        }
    }
}
