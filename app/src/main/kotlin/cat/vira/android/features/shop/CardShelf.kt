package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraShopBackdrop
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Un prestatge de l'àlbum (`CardShelf`, Arnau, 2026-10-22): la capçalera de la categoria i les
/// cartes que tens, de costat, amb el dors de les que falten al final.
///
/// La capçalera porta a la pàgina de la categoria (`CardCategoryScreen`) i cada carta s'obre a
/// pantalla completa. Les cartes són petites i quietes: la part animada és per a la que es mira de
/// prop (`CardLiveEffect`).
@Composable
fun CardShelf(
    category: CardItem.Category,
    owned: List<CardItem>,
    total: Int,
    onCategory: () -> Unit,
    onOpen: (CardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        CardCategoryHeader(
            category, owned.size, total,
            Modifier.viraPressable(onClickLabel = stringResource(R.string.card_category_open), onClick = onCategory),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            verticalAlignment = Alignment.Top,
        ) {
            owned.forEach { card -> CardShelfTile(card, onOpen = { onOpen(card) }) }
            if (total > owned.size) CardBackFace(total - owned.size, baseWidth = 84.dp)
        }
    }
}

/// Nom, símbol i «3 de 5» d'una categoria. El recompte va en text: el color i el símbol ajuden a
/// trobar el prestatge, però no diuen res que no digui el nom.
@Composable
fun CardCategoryHeader(category: CardItem.Category, owned: Int, total: Int, modifier: Modifier = Modifier) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    // La rajola del símbol creix amb la lletra: amb una mida fixa, a lletra gran el símbol en sortia.
    val chip = ViraTouch.minimum * 0.75f * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val title = stringResource(category.titleRes)
    val count = stringResource(R.string.card_collection_owned, owned, total)
    val tile = @Composable {
        Box(
            Modifier.size(chip).background(category.fill, RoundedCornerShape(chip * 0.25f)),
            contentAlignment = Alignment.Center,
        ) { ShopSymbolIcon(category.symbol, category.onFill, size = chip * 0.5f) }
    }
    val chevron = @Composable { ShopSymbolIcon(ShopSymbol.Chevron, ViraColor.textSecondary, size = 14.dp) }
    val names = @Composable { nameModifier: Modifier ->
        if (large) {
            Column(nameModifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
                Text(title, style = ViraFont.title, color = ViraColor.textPrimary)
                Text(count, style = ViraFont.callout.tabular(), color = ViraColor.textSecondary)
            }
        } else {
            Row(nameModifier, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                Text(title, style = ViraFont.title, color = ViraColor.textPrimary, modifier = Modifier.weight(1f).alignByBaseline())
                Text(count, style = ViraFont.callout.tabular(), color = ViraColor.textSecondary, modifier = Modifier.alignByBaseline())
            }
        }
    }
    val frame = modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
        .clearAndSetSemantics { contentDescription = "$title, $count"; heading() }
        .padding(horizontal = ViraSpacing.screenMargin)
    // A mides d'accessibilitat, la rajola i la fletxa a sobre i el nom a sota amb tota l'amplada: de
    // costat, «Concentración» es partia a mitja paraula.
    if (large) {
        Column(frame, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                tile()
                Spacer(Modifier.weight(1f))
                chevron()
            }
            names(Modifier.fillMaxWidth())
        }
    } else {
        Row(frame, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            tile()
            names(Modifier.weight(1f))
            chevron()
        }
    }
}

/// Una carta petita del prestatge, amb el nom a sota.
@Composable
private fun CardShelfTile(card: CardItem, onOpen: () -> Unit) {
    val width = 84.dp * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val rarity = stringResource(card.rarity.labelRes)
    Column(
        Modifier.width(width)
            .viraPressable(onClickLabel = stringResource(R.string.card_collection_open), onClick = onOpen)
            .semantics(mergeDescendants = true) { stateDescription = rarity },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        CardFace(card, baseWidth = 84.dp)
        Text(stringResource(card.titleRes), style = ViraFont.caption, color = ViraColor.textPrimary)
    }
}

/// El dors de les cartes que encara no tens (`CardBackFace`): **quantes en falten, mai quines ni què
/// cal fer** (Arnau, 2026-10-22, «Sí, amb dors»). Una carta que no és teva no es veu enlloc: el dors
/// no ensenya la il·lustració, el nom ni el requisit. Diu el mateix que el «3 de 5» de la capçalera.
@Composable
fun CardBackFace(remaining: Int, baseWidth: Dp, modifier: Modifier = Modifier) {
    val width = baseWidth * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = ViraColor.onCard(ViraArea.Today)
    val spoken = stringResource(R.string.card_back_accessibility, remaining)
    Column(
        modifier.width(width).defaultMinSize(minHeight = width * 1.5f)
            .clearAndSetSemantics { contentDescription = spoken }
            .background(ViraGradient.card(ViraArea.Today), shape)
            .border(1.dp, ink.copy(alpha = 0.35f), shape)
            .padding(ViraSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterVertically),
    ) {
        ShopSymbolIcon(ShopSymbol.Star, ink.copy(alpha = 0.55f), size = width * 0.34f)
        Text(
            stringResource(R.string.card_back_remaining, remaining), style = ViraFont.caption, color = ink,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/// El pla de color de la categoria: els cinc tons d'àrea per a les que en tenen una, i tres dels fons
/// de la Botiga per a les que no. Sempre amb el seu parell de text validat.
val CardItem.Category.fill: Color
    @Composable get() = when (this) {
        CardItem.Category.Emotions -> ViraColor.surface(ViraArea.Emotions)
        CardItem.Category.Focus -> ViraColor.surface(ViraArea.Focus)
        CardItem.Category.Habits -> ViraColor.surface(ViraArea.Today)
        CardItem.Category.Plan -> ViraColor.surface(ViraArea.Plan)
        CardItem.Category.Streak -> ViraColor.surface(ViraArea.Rewards)
        CardItem.Category.Days -> ViraColor.shopBackdrop(ViraShopBackdrop.Mint)
        CardItem.Category.Points -> ViraColor.shopBackdrop(ViraShopBackdrop.Indigo)
        CardItem.Category.Moments -> ViraColor.shopBackdrop(ViraShopBackdrop.Coral)
    }

val CardItem.Category.onFill: Color
    @Composable get() = when (this) {
        CardItem.Category.Emotions -> ViraColor.onSurface(ViraArea.Emotions)
        CardItem.Category.Focus -> ViraColor.onSurface(ViraArea.Focus)
        CardItem.Category.Habits -> ViraColor.onSurface(ViraArea.Today)
        CardItem.Category.Plan -> ViraColor.onSurface(ViraArea.Plan)
        CardItem.Category.Streak -> ViraColor.onSurface(ViraArea.Rewards)
        CardItem.Category.Days -> ViraColor.onShopBackdrop(ViraShopBackdrop.Mint)
        CardItem.Category.Points -> ViraColor.onShopBackdrop(ViraShopBackdrop.Indigo)
        CardItem.Category.Moments -> ViraColor.onShopBackdrop(ViraShopBackdrop.Coral)
    }
