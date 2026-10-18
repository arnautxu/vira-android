package cat.vira.android.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing

/// La col·lecció de cartes (`CardsView`, `VIRI_AND_GAMIFICATION.md` §2.4): **només les que ja són
/// teves** (Arnau, 2026-08-10). El comptador «6 de 16» diu quantes n'hi ha sense ensenyar-les, i
/// sense ell una col·lecció de tres semblaria completa. Cap estat propi: una carta és teva quan el
/// comptador arriba al llindar, i els comptadors mai baixen.
///
/// Tancada en Release com a l'iPhone: qui la presenta mira `cardsEnabled` abans d'obrir-la.
@Composable
fun CardsScreen(onBack: (() -> Unit)? = null) {
    val data = rememberShopData()
    val counter by data.counter.collectAsStateWithLifecycle(null)
    val streak by data.streak.collectAsStateWithLifecycle(null)
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    val owned = CardItem.unlocked(counter, streak)

    CardsContent(owned, onBack = onBack, onOpen = { opened = it.key })

    opened?.let { key ->
        Dialog(
            onDismissRequest = { opened = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            CardCollectionScreen(owned, startingAt = key, onClose = { opened = null })
        }
    }
}

/// La col·lecció sense estat: les cartes guanyades, en l'ordre del catàleg.
@Composable
fun CardsContent(
    owned: List<CardItem>,
    onOpen: (CardItem) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        Column(Modifier.fillMaxSize()) {
            ShopNavigationBar(stringResource(R.string.profile_cards_title), onBack, area = ViraArea.Rewards)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            ) {
                Header(owned.size)
                if (owned.isEmpty()) {
                    // No diu què falta (les bloquejades no es veuen) sinó que n'hi haurà.
                    Column(
                        Modifier.fillMaxWidth().padding(top = ViraSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                    ) {
                        ViraPictogram(R.drawable.viri_medal, size = 120.dp, isScene = true)
                        Text(stringResource(R.string.card_collection_empty), style = ViraFont.body, color = ViraColor.textSecondary, textAlign = TextAlign.Center)
                    }
                } else {
                    // Dues columnes i no tres: una carta és alta, i a tres per fila semblen cromos.
                    ShopGrid(owned, columns = 2, spacing = ViraSpacing.m) { card, cellModifier ->
                        CardTile(card, { onOpen(card) }, cellModifier)
                    }
                }
                Spacer(Modifier.height(viraTabBarClearance()))
            }
        }
    }
}

/// «Les guanyes fent coses» i «6 de 16»: el mateix que una barra, sense dependre del color ni
/// convertir la col·lecció en un percentatge per completar.
@Composable
private fun Header(owned: Int) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Text(stringResource(R.string.card_collection_subtitle), style = ViraFont.body, color = ViraColor.textSecondary, modifier = Modifier.weight(1f).alignByBaseline())
        Text(
            stringResource(R.string.card_collection_owned, owned, CardItem.catalog.size),
            style = ViraFont.headline.tabular(), color = ViraColor.textPrimary, modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun CardTile(card: CardItem, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .viraPressable(onClickLabel = stringResource(R.string.card_collection_open), onClick = onClick)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        CardFace(card, baseWidth = 132.dp)
        Text(stringResource(card.titleRes), style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center)
        Text(stringResource(card.rarity.labelRes), style = ViraFont.caption, color = ViraColor.textSecondary)
    }
}
