package cat.vira.android.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import cat.vira.android.R
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing

/// Quantes columnes té una graella de la Botiga: tres, dues amb lletra gran i una a mides
/// d'accessibilitat, perquè el nom i el botó no es tallin.
@Composable
fun shopColumnCount(): Int = when {
    LocalViraEnvironment.current.isAccessibilitySize -> 1
    LocalDensity.current.fontScale >= 1.3f -> 2
    else -> 3
}

/// Graella amb les fitxes de cada fila igual d'altes (`LazyVGrid` amb alineació a dalt).
@Composable
fun <T> ShopGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = ViraSpacing.xs,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                row.forEach { cell(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/// La pestanya de Cartes de la Botiga (`BadgeShopGrid`). Dues seccions:
/// - **Les guanyes fent**: les cartes amb la barra del que hi portes. No es venen i no tenen botó.
///   Només surten amb les cartes obertes (`cardsEnabled`, apagades en Release com a l'iPhone).
/// - **Per comprar**: els badges de monedes, que es pengen al perfil.
@Composable
fun BadgeShopGrid(
    balance: Int,
    ownedKeys: Set<String>,
    counter: ProgressCounter?,
    streak: Streak?,
    cardsEnabled: Boolean,
    onSelect: (BadgeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = shopColumnCount()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        if (cardsEnabled) {
            // Primer les que ja tens, després les que tens més a prop.
            val cards = CardItem.catalog.sortedWith(
                compareByDescending<CardItem> { fraction(it, counter, streak) }.thenBy { it.rarity },
            )
            val earned = CardItem.catalog.count { it.isUnlocked(counter, streak) }
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                ShopSectionHeader(stringResource(R.string.shop_cards_earned_title), ShopSymbol.Star, ViraArea.Focus) {
                    Text(
                        stringResource(R.string.shop_cards_earned_count, earned.viraFormatted(), CardItem.catalog.size.viraFormatted()),
                        style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary,
                    )
                }
                ShopGrid(cards, columns) { card, cellModifier -> EarnedTile(card, counter, streak, cellModifier) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ShopSectionHeader(stringResource(R.string.shop_cards_bought_title), ShopSymbol.Diamond, ViraArea.Today) {
                Text(stringResource(R.string.shop_cards_bought_subtitle), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
            ShopGrid(BadgeItem.boughtCatalog, columns) { item, cellModifier ->
                BoughtTile(item, item.key in ownedKeys, balance, { onSelect(item) }, cellModifier)
            }
        }
    }
}

private fun fraction(card: CardItem, counter: ProgressCounter?, streak: Streak?): Double {
    val target = card.requirement.target
    if (target <= 0) return 1.0
    return card.requirement.current(counter, streak).toDouble() / target
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun EarnedTile(card: CardItem, counter: ProgressCounter?, streak: Streak?, modifier: Modifier) {
    val current = card.requirement.current(counter, streak)
    val target = card.requirement.target
    val done = current >= target
    val area = card.rarity.shopArea
    val ink = ViraColor.onCard(area)
    val progress = if (done) {
        stringResource(R.string.shop_cards_earned_done)
    } else {
        stringResource(R.string.shop_cards_progress, current.viraFormatted(), target.viraFormatted())
    }
    val title = stringResource(card.titleRes)
    val rarity = stringResource(card.rarity.labelRes)
    val side = 56.dp * LocalDensity.current.fontScale
    Box(modifier.clearAndSetSemantics { contentDescription = title; stateDescription = "$rarity, $progress" }) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight().shopFrame(area, ink).padding(ViraSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            Image(
                painterResource(card.pictogram), null,
                Modifier.size(side).alpha(if (done) 1f else 0.45f),
                colorFilter = if (done) null else grayscale,
            )
            Text(title, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center, maxLines = 3, modifier = Modifier.fillMaxWidth())
            // La raresa també en text: el color de la fitxa sol no la pot dir.
            Text(rarity, style = ViraFont.caption, color = ink)
            Spacer(Modifier.weight(1f))
            ShopProgressBar(current, target, area, progress, ink)
        }
        if (done) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp)
                    .background(ViraColor.accent(ViraArea.Today), CircleShape).padding(3.dp),
            ) { ShopSymbolIcon(ShopSymbol.Check, ViraColor.onAccent, size = 14.dp) }
        }
    }
}

@Composable
private fun BoughtTile(item: BadgeItem, owned: Boolean, balance: Int, action: () -> Unit, modifier: Modifier) {
    val kind = if (owned) ShopPriceKind.Owned else shopPriceKind(item.price, balance, isOwned = false, isEquipped = false)
    val ink = ViraColor.onCard(item.shopArea)
    val name = stringResource(item.nameRes)
    val status = shopStatusDescription(kind)
    val hint = stringResource(R.string.shop_accessibility_details)
    Column(
        modifier
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = status
                role = Role.Button
                onClick(label = hint) { action(); true }
            }
            .shopFrame(item.shopArea, ink)
            .viraPressable(onClick = action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        BadgeChip(item, Modifier.padding(top = ViraSpacing.xs), size = 64.dp)
        Text(name, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        ShopPriceLabel(kind, Modifier.padding(start = 6.dp, end = 6.dp, bottom = ViraSpacing.xs))
    }
}
