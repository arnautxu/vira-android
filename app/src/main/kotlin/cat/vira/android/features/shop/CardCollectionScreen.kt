package cat.vira.android.features.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Les cartes guanyades a pantalla completa, una per pàgina (`CardCollectionView`), amb el text que
/// diu què volen dir. Sense confeti: celebrar cinc vegades seguides mentre passes pàgines devalua
/// el moment que sí que ho és. Només les guanyades: barrejar-hi siluetes convertiria una pantalla
/// de recompensa en un recompte del que et falta.
@Composable
fun CardCollectionScreen(
    cards: List<CardItem>,
    startingAt: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /// Obrir-la és el que li treu la «Nova» de dalt de l'àlbum (`ViraCardShelf.markOpened`).
    onOpened: (String) -> Unit = {},
) {
    val area = ViraArea.Rewards
    val ink = ViraColor.onCard(area)
    val start = cards.indexOfFirst { it.key == startingAt }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { cards.size }
    LaunchedEffect(pager.currentPage, cards.size) { cards.getOrNull(pager.currentPage)?.let { onOpened(it.key) } }
    Column(modifier.fillMaxSize().background(ViraGradient.card(area)).safeDrawingPadding()) {
        // Títol, posició i tancar. La posició va en text perquè els punts de sota són només color.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.card_collection_title), style = ViraFont.title, color = ink, modifier = Modifier.semantics { heading() })
                if (cards.size > 1) {
                    Text(
                        stringResource(R.string.card_collection_position, pager.currentPage + 1, cards.size),
                        style = ViraFont.footnote.tabular(), color = ink, modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
            CloseButton(ink, onClose)
        }
        if (cards.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = ViraSpacing.screenMargin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s, Alignment.CenterVertically),
            ) {
                ViraPictogram(R.drawable.viri_medal, size = 120.dp, isScene = true)
                Text(stringResource(R.string.card_collection_empty), style = ViraFont.body, color = ink, textAlign = TextAlign.Center)
            }
        } else {
            HorizontalPager(pager, Modifier.weight(1f)) { index -> CardPage(cards[index], isLive = index == pager.currentPage) }
            if (cards.size > 1) PageDots(cards.size, pager.currentPage)
        }
    }
}

/// Una carta: la targeta gran, el nom, la raresa en paraules i què vol dir. Amb desplaçament,
/// perquè a lletra gran la targeta creix i el que es perdria és el text que l'explica.
@Composable
private fun CardPage(card: CardItem, isLive: Boolean) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xl, bottom = ViraSpacing.xl)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
    ) {
        CardFace(card, baseWidth = 232.dp, isLive = isLive)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Text(stringResource(card.titleRes), style = ViraFont.title, color = ink, textAlign = TextAlign.Center)
            // La raresa en text: el gruix de la vora sol no és informació.
            Text(stringResource(card.rarity.labelRes), style = ViraFont.callout, color = ink, textAlign = TextAlign.Center)
            Text(
                stringResource(card.meaningRes), style = ViraFont.body, color = ink, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = ViraSpacing.xxs),
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    Row(
        Modifier.fillMaxWidth().padding(vertical = ViraSpacing.s).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            Box(Modifier.size(8.dp).background(ink.copy(alpha = if (i == current) 1f else 0.3f), CircleShape))
        }
    }
}

/// Contorn i no farciment: sobre el degradat, un fons translúcid menjaria contrast al glif.
@Composable
internal fun CloseButton(ink: androidx.compose.ui.graphics.Color, onClose: () -> Unit) {
    val label = stringResource(R.string.action_close)
    Box(
        Modifier.size(ViraTouch.minimum)
            .border(1.5.dp, ink, CircleShape)
            .viraPressable(onClick = onClose)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val w = size.width
            drawLine(ink, Offset(0f, 0f), Offset(w, w), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(w, 0f), Offset(0f, w), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
