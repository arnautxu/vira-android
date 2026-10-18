package cat.vira.android.features.shop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraCelebrationMoment
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.CelebrationPill
import cat.vira.android.features.progress.CelebrationProgressBar
import cat.vira.android.features.progress.CelebrationSummaryCard
import cat.vira.android.features.progress.CelebrationSummaryRow
import kotlinx.coroutines.delay

/// La carta nova, just després de la celebració de la fita (`CardUnlockedView`). Dues pantalles i
/// no una: la fita celebra el que has fet i la carta ensenya el que t'endus.
///
/// La raresa mana el color, com a la Botiga, sempre amb símbol i nom a la píndola.
///
/// @param owned quantes en té ara, aquesta inclosa; com a mínim 1.
@Composable
fun CardUnlockedScreen(card: CardItem, owned: Int, onContinue: () -> Unit, modifier: Modifier = Modifier, onShown: () -> Unit = {}) {
    val area = card.rarity.shopArea
    val title = stringResource(card.titleRes)
    val total = CardItem.catalog.size
    val count = owned.coerceAtLeast(1)
    val countText = stringResource(R.string.card_collection_owned, count, total)
    ViraCelebrationMoment(
        title = stringResource(R.string.celebration_card_title),
        message = stringResource(R.string.celebration_card_message, title),
        onContinue = onContinue,
        modifier = modifier,
        area = area,
        onShown = onShown,
        scene = {
            // Sense il·lustració de fons: la carta és la il·lustració, i Viri treu el cap pel costat.
            Box(Modifier.fillMaxSize().background(ViraGradient.card(area))) {
                TurningCard(card, Modifier.align(Alignment.Center).rotate(-6f))
                Image(
                    painterResource(R.drawable.viri_hero), null,
                    Modifier.align(Alignment.BottomEnd).width(150.dp).offset(x = ViraSpacing.xs, y = ViraSpacing.s).rotate(8f),
                    contentScale = ContentScale.Fit,
                )
            }
        },
        summary = {
            CelebrationSummaryCard(
                area,
                pills = listOf(
                    CelebrationPill(stringResource(card.rarity.labelRes), isFilled = true, icon = { ShopSymbolIcon(card.rarity.symbol, LocalContentColor.current, size = 12.dp) }),
                    CelebrationPill(stringResource(card.descriptionRes)),
                ),
            ) {
                CelebrationSummaryRow(R.drawable.picto_cards, stringResource(R.string.card_collection_title), area, detail = countText, isNumber = false)
                CelebrationProgressBar(count.toFloat() / total.coerceAtLeast(1), countText, area)
            }
        },
    )
}

/// La carta arriba de gairell (–65°) i es gira cap a l'usuari: a aquell angle encara es veu la
/// cara, així que no cal inventar cap revers. Sense rebot: és una peça que pesa. Amb Reduir
/// moviment, ja hi és.
@Composable
private fun TurningCard(card: CardItem, modifier: Modifier) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val angle = remember { Animatable(if (reduceMotion) 0f else -65f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            angle.snapTo(0f)
        } else {
            delay(120)
            angle.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
        }
    }
    CardFace(
        card,
        modifier.graphicsLayer {
            rotationY = angle.value
            cameraDistance = 18f * density
        },
        baseWidth = 168.dp,
    )
}
