package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Totes les teves d'una categoria, per raresa, i el dors de les que falten al final
/// (`CardCategoryView`, Arnau, 2026-10-22). S'hi arriba des de la capçalera de cada prestatge.
///
/// Per raresa i no per ordre de catàleg: és la mateixa escala de la més fàcil a la més cara, però
/// amb el graó escrit al damunt, que és el que la vora gruixuda ja diu sense paraules.
@Composable
fun CardCategoryContent(
    category: CardItem.Category,
    owned: List<CardItem>,
    onOpen: (CardItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val all = CardItem.catalog(category)
    val mine = owned.filter { it.category == category }
    Column(modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        ShopNavigationBar(stringResource(category.titleRes), onBack, area = ViraArea.Rewards)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
        ) {
            Summary(category, mine.size, all.size)
            CardItem.Rarity.entries.forEach { rarity ->
                val cards = mine.filter { it.rarity == rarity }
                if (cards.isNotEmpty()) {
                    Section(stringResource(rarity.pluralRes)) {
                        ShopGrid(cards, columns = 2, spacing = ViraSpacing.m) { card, cellModifier ->
                            Tile(card, { onOpen(card) }, cellModifier)
                        }
                    }
                }
            }
            if (all.size > mine.size) {
                Section(stringResource(R.string.card_back_section)) {
                    CardBackFace(all.size - mine.size, baseWidth = 132.dp)
                }
            }
            Spacer(Modifier.height(viraTabBarClearance()))
        }
    }
}

@Composable
private fun Summary(category: CardItem.Category, owned: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }
            .background(category.fill, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Box(
            Modifier.size(ViraTouch.comfortable).background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)),
            contentAlignment = Alignment.Center,
        ) { ShopSymbolIcon(category.symbol, category.onFill, size = 24.dp) }
        Text(
            stringResource(R.string.card_collection_owned, owned, total), style = ViraFont.largeTitle.tabular(),
            color = category.onFill, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Text(title, style = ViraFont.headline, color = ViraColor.textSecondary, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Tile(card: CardItem, onClick: () -> Unit, modifier: Modifier) {
    val rarity = stringResource(card.rarity.labelRes)
    Column(
        modifier
            .viraPressable(onClickLabel = stringResource(R.string.card_collection_open), onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = rarity },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        CardFace(card, baseWidth = 132.dp)
        Text(stringResource(card.titleRes), style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center)
    }
}
