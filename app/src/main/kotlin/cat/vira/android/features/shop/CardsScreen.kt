package cat.vira.android.features.shop

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraCardTallies
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// La col·lecció de cartes (`CardsView`, `VIRI_AND_GAMIFICATION.md` §2.4): un àlbum amb un
/// prestatge per categoria. Cap estat propi a persistir: una carta és teva quan el comptador arriba
/// al llindar, i els comptadors mai baixen.
///
/// ## Un àlbum per categories (Arnau, 2026-10-22)
/// Amb 67 cartes, una graella seguida de dues columnes era un mur. Ara:
/// - **A dalt, l'última que has guanyat**, gran i amb «Nova» fins que l'obres (`ViraCardShelf`).
/// - **Vuit prestatges** (`CardItem.Category`), cadascun amb el seu nom, símbol i «3 de 5».
/// - **Dors al final de cada prestatge** amb quantes en falten. Les bloquejades no es veuen: el dors
///   no diu quina carta és ni què cal fer.
///
/// Tancada en Release com a l'iPhone: qui la presenta mira `cardsEnabled` abans d'obrir-la.
@Composable
fun CardsScreen(onBack: (() -> Unit)? = null) {
    val data = rememberShopData()
    val counter by data.counter.collectAsStateWithLifecycle(null)
    val streak by data.streak.collectAsStateWithLifecycle(null)
    val tallies = rememberCardTallies(data.preferences)
    val shelf = remember(data) { ViraCardShelf(data.preferences) }
    // L'etiqueta «Nova» depèn del que s'ha celebrat i obert: es rellegeix quan canvia.
    val openedKeys by rememberPreference(data.preferences, ViraCardShelf.OPENED_KEY, "")
    val seenKeys by rememberPreference(data.preferences, ViraCardCelebrations.SEEN_KEY, "")
    var category by rememberSaveable { mutableStateOf<CardItem.Category?>(null) }
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    val owned = CardItem.unlocked(counter, streak, tallies)

    LaunchedEffect(Unit) {
        shelf.seedIfNeeded()
        // Els recomptes es reparen amb el que hi ha a la base de dades, sense festa per a les que ja
        // eren teves.
        runCatching { data.cardCelebrations.prepare(data.db) }
    }
    BackHandler(enabled = category != null) { category = null }

    val current = category
    if (current == null) {
        val latest = remember(owned, seenKeys) { shelf.latest(owned) }
        val latestIsNew = remember(latest, owned, seenKeys, openedKeys) { latest != null && shelf.isNew(latest, owned) }
        CardsContent(
            owned = owned,
            latest = latest,
            latestIsNew = latestIsNew,
            onOpen = { opened = it.key },
            onCategory = { category = it },
            onBack = onBack,
        )
    } else {
        CardCategoryContent(current, owned, onOpen = { opened = it.key }, onBack = { category = null })
    }

    opened?.let { key ->
        // Es passa per la categoria sencera: si obres una d'Emocions, el que hi ha a banda i banda
        // també són d'Emocions, no la següent del catàleg.
        val card = CardItem.catalog.firstOrNull { it.key == key }
        val cards = owned.filter { it.category == card?.category }
        Dialog(
            onDismissRequest = { opened = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            CardCollectionScreen(cards, startingAt = key, onClose = { opened = null }, onOpened = shelf::markOpened)
        }
    }
}

/// Els recomptes de les cartes (`ViraCardTallies`), llegits com a estat: es redibuixa quan una altra
/// pantalla en suma un.
@Composable
fun rememberCardTallies(preferences: SharedPreferences): Tallies {
    val tallies = remember(preferences) { ViraCardTallies(preferences) }
    var snapshot by remember(preferences) { mutableStateOf(tallies.snapshot()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith("cards.tally.")) snapshot = tallies.snapshot()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        snapshot = tallies.snapshot()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return snapshot
}

/// L'àlbum sense estat: la carta de dalt i els vuit prestatges.
@Composable
fun CardsContent(
    owned: List<CardItem>,
    latest: CardItem?,
    latestIsNew: Boolean,
    onOpen: (CardItem) -> Unit,
    onCategory: (CardItem.Category) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        ShopNavigationBar(stringResource(R.string.profile_cards_title), onBack, area = ViraArea.Rewards)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
        ) {
            Header(owned.size, Modifier.padding(horizontal = ViraSpacing.screenMargin))
            // Quan encara no n'hi ha cap, no hi és: els prestatges amb els dors ja diuen que n'hi haurà.
            latest?.let {
                CardLatestPanel(it, latestIsNew, onOpen = { onOpen(it) }, modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin))
            }
            CardItem.Category.entries.forEach { category ->
                CardShelf(
                    category, owned.filter { it.category == category }, CardItem.catalog(category).size,
                    onCategory = { onCategory(category) }, onOpen = onOpen,
                )
            }
            Spacer(Modifier.height(viraTabBarClearance()))
        }
    }
}

/// «Les guanyes fent coses» i «21 de 67», i no una barra: diu el mateix, no depèn del color i no
/// converteix la col·lecció en un percentatge per completar. A mides d'accessibilitat, un sota l'altre.
@Composable
private fun Header(owned: Int, modifier: Modifier = Modifier) {
    val subtitle = @Composable { m: Modifier ->
        Text(stringResource(R.string.card_collection_subtitle), style = ViraFont.body, color = ViraColor.textSecondary, modifier = m)
    }
    val count = @Composable { m: Modifier ->
        Text(
            stringResource(R.string.card_collection_owned, owned, CardItem.catalog.size),
            style = ViraFont.headline.tabular(), color = ViraColor.textPrimary, modifier = m,
        )
    }
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            subtitle(Modifier)
            count(Modifier)
        }
    } else {
        Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            subtitle(Modifier.weight(1f).alignByBaseline())
            count(Modifier.alignByBaseline())
        }
    }
}

/// L'última carta guanyada, a dalt de tot (`CardLatestPanel`). Carta i text de costat; a mides
/// d'accessibilitat, la carta a sobre i el text a sota.
@Composable
fun CardLatestPanel(card: CardItem, isNew: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val texts = @Composable { m: Modifier ->
        Column(m, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            val tag = stringResource(if (isNew) R.string.card_new else R.string.card_latest)
            if (isNew) {
                Text(
                    tag, style = ViraFont.footnoteEmphasis, color = ViraColor.onFeedback,
                    modifier = Modifier.background(ViraColor.feedbackFill, CircleShape)
                        .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.hairline),
                )
            } else {
                Text(tag, style = ViraFont.footnoteEmphasis, color = ViraColor.textSecondary)
            }
            Text(stringResource(card.titleRes), style = ViraFont.title, color = ViraColor.textPrimary)
            // Espai que no parteix abans del punt: si la línia salta, el punt es queda amb la raresa.
            Text(
                "${stringResource(card.rarity.labelRes)}\u00A0· ${stringResource(card.category.titleRes)}",
                style = ViraFont.callout, color = ViraColor.textSecondary,
            )
        }
    }
    val frame = modifier.fillMaxWidth()
        .viraPressable(onClickLabel = stringResource(R.string.card_collection_open), onClick = onOpen)
        .semantics(mergeDescendants = true) { }
        .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
        .padding(ViraSpacing.m)
    if (large) {
        Column(frame, verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
            CardFace(card, baseWidth = 100.dp)
            texts(Modifier.fillMaxWidth())
        }
    } else {
        Row(frame, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
            CardFace(card, baseWidth = 100.dp)
            texts(Modifier.weight(1f))
        }
    }
}
