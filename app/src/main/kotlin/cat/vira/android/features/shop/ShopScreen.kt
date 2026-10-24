package cat.vira.android.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.sound.ViraSound
import kotlinx.coroutines.launch
import java.time.Instant

/// La Botiga (`SkinShopView`, `USER_FLOWS.md` F18/F19): botiga de joc amb l'estètica de Vira.
///
/// Des del 2026-10-22 (Arnau: «li falta un push al procés, de la botiga treurem les cartes» i «treu
/// insígnies») **només ven skins**. Un sol scroll: l'escenari amb Viri i la skin que porta
/// (`ShopStage`), la destacada (el teu objectiu o la següent), la col·lecció que creix
/// (`ShopCollectionMeter`) i el catàleg per raresa amb quantes en tens de cada una.
///
/// Tocar una skin que ja és teva la posa a l'instant (reversible, gratuït i freqüent: no pot costar
/// tres tocs); tocar-ne una que no tens obre el provador (`ShopFittingRoom`), on es compra mantenint
/// el botó. La compra passa pel vestidor (`ShopDressingRoom`) i, en acabar, el següent objectiu
/// (`ShopNextGoalSheet`).
@Composable
fun ShopScreen(onBack: (() -> Unit)? = null) {
    val data = rememberShopData()
    val scope = rememberCoroutineScope()
    val celebrationsOn = rememberCelebrationsEnabled()

    val counter by data.counter.collectAsStateWithLifecycle(null)
    val skins by data.skins.collectAsStateWithLifecycle(emptyList())
    val transactions by data.transactions.collectAsStateWithLifecycle(emptyList())
    val wishKey by rememberPreference(data.preferences, ShopPreferences.WISH, "")
    val openWishRequested by rememberPreference(data.preferences, ShopPreferences.OPEN_WISH, false)

    var selection by remember { mutableStateOf<SkinItem?>(null) }
    var revealing by remember { mutableStateOf<SkinItem?>(null) }
    var justBought by remember { mutableStateOf<SkinItem?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val balance = counter?.coinBalance ?: 0
    val ownedKeys = skins.map { it.key }.toSet()
    val equippedKey = skins.firstOrNull { it.isEquipped }?.key
    val catalog = ShopCollection.catalog(ownedKeys)
    val pace = remember(transactions) { ShopCollection.dailyPace(transactions, Instant.now()) }

    fun setWish(key: String) = data.preferences.edit { putString(ShopPreferences.WISH, key) }

    fun wear(key: String?) {
        ViraHaptics.play(context, ViraHaptics.Feedback.Done)
        ViraSound.play(context, ViraSound.Cue.EQUIP)
        scope.launch { data.skinsLogic.equip(key) }
    }

    /// Desa tot o res. Un error de la base de dades es diu a la pantalla; quedar-se curt de
    /// monedes no és un error (el botó ja ho deia) i no canvia res.
    fun buy(item: SkinItem) {
        scope.launch {
            val result = runCatching { data.skinsLogic.buy(item) }
            saveFailed = result.isFailure
            if (!result.getOrDefault(false)) return@launch
            if (wishKey == item.key) setWish("")
            ViraHaptics.play(context, ViraHaptics.Feedback.Reward)
            // Amb les celebracions apagades no hi ha vestidor: la skin ja és posada i prou. Amb
            // revelació, el so el porta ella, sincronitzat amb el que es veu.
            if (!celebrationsOn) ViraSound.play(context, ViraSound.Cue.EQUIP)
            if (celebrationsOn) revealing = item else showNextGoals(item, catalog, ownedKeys + item.key) { justBought = it }
        }
    }

    // L'avís d'objectiu de fora de la Botiga demana obrir el provador del teu objectiu.
    LaunchedEffect(openWishRequested, catalog.size) {
        if (!openWishRequested) return@LaunchedEffect
        data.preferences.edit { putBoolean(ShopPreferences.OPEN_WISH, false) }
        catalog.firstOrNull { it.key == wishKey && it.key !in ownedKeys }?.let { selection = it }
    }

    ShopContent(
        balance = balance,
        ownedKeys = ownedKeys,
        equippedKey = equippedKey,
        wishKey = wishKey,
        saveFailed = saveFailed,
        onBack = onBack,
        onDefaultTap = { wear(null) },
        onSkinTap = { item -> if (item.key in ownedKeys) wear(item.key) else selection = item },
    )

    selection?.let { item ->
        ShopFittingRoom(
            item = item,
            isOwned = item.key in ownedKeys,
            isEquipped = item.key == equippedKey,
            balance = balance,
            dailyPace = pace,
            wishKey = wishKey,
            onWish = ::setWish,
            onBuy = { buy(item) },
            onWear = { wear(item.key) },
            onDismiss = { selection = null },
        )
    }
    revealing?.let { item ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false),
        ) {
            ShopDressingRoom(item, onContinue = {
                revealing = null
                showNextGoals(item, catalog, ownedKeys) { justBought = it }
            }, celebrationsEnabled = celebrationsOn)
        }
    }
    justBought?.let { item ->
        ShopNextGoalSheet(item, catalog, ownedKeys, balance, onChoose = ::setWish, onDismiss = { justBought = null })
    }
}

/// En tancar el vestidor, el següent objectiu: la col·lecció ja suma la que acabes d'estrenar. Si ja
/// no en queda cap per proposar, no surt res.
private fun showNextGoals(item: SkinItem, catalog: List<SkinItem>, owned: Set<String>, show: (SkinItem) -> Unit) {
    if (ShopCollection.nextGoals(catalog, owned).isNotEmpty()) show(item)
}

/// La Botiga sense estat: el que es veu donades les dades. Les captures la fan servir directament.
@Composable
fun ShopContent(
    balance: Int,
    ownedKeys: Set<String>,
    equippedKey: String?,
    wishKey: String,
    saveFailed: Boolean,
    onBack: (() -> Unit)?,
    onDefaultTap: () -> Unit,
    onSkinTap: (SkinItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val catalog = ShopCollection.catalog(ownedKeys)
    val featured = ShopCollection.featured(catalog, ownedKeys, wishKey)
    val equipped = equippedKey?.let { SkinItem.item(it) }
    // Viri només parla quan la destacada ja és a l'abast: és l'única aparició expressiva.
    val stageLine = featured?.takeIf { balance >= it.first.price }?.let { stringResource(R.string.shop_stage_ready) }
    val (owned, total) = ShopCollection.progress(catalog, ownedKeys)
    Column(modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
        ShopNavigationBar(stringResource(R.string.shop_title), onBack) { ShopWallet(balance) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            if (saveFailed) ShopSaveError()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                ShopStage(equipped, height = shopStageHeight(large, inFittingRoom = false), line = stageLine)
                Text(
                    stringResource(R.string.shop_stage_hint), style = ViraFont.footnote,
                    color = ViraColor.textSecondary, textAlign = TextAlign.Center,
                )
            }
            featured?.let { (item, isWish) ->
                ShopFeaturedCard(item, balance, action = { onSkinTap(item) }, isWish = isWish)
            }
            ShopCollectionMeter(owned, total)
            SkinShopCatalog(catalog, ownedKeys, equippedKey, balance, onDefaultTap, onSkinTap)
            Spacer(Modifier.height(viraTabBarClearance()))
        }
    }
}

/// El catàleg de skins per seccions de raresa (`SkinShopCatalog`). Cada capçalera diu quantes en
/// tens de la raresa, i no el preu (2026-10-22): el preu ja és a cada fitxa, i el recompte és el que
/// fa venir ganes de completar-la. El Viri de sempre obre la primera: gratuït, perquè treure's una
/// skin no ha de costar res.
@Composable
fun SkinShopCatalog(
    items: List<SkinItem>,
    ownedKeys: Set<String>,
    equippedKey: String?,
    balance: Int,
    onDefaultTap: () -> Unit,
    onSkinTap: (SkinItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = shopColumnCount()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        SkinTier.entries.forEach { tier ->
            val tierItems = items.filter { it.tier == tier }
            if (tierItems.isNotEmpty() || tier == SkinTier.Common) {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    ShopSectionHeader(stringResource(tier.sectionRes), tier.symbol, tier.shopArea) {
                        ShopCollectionCount(tierItems.count { it.key in ownedKeys }, tierItems.size)
                    }
                    val cells: List<SkinItem?> = (if (tier == SkinTier.Common) listOf<SkinItem?>(null) else emptyList()) + tierItems
                    ShopGrid(cells, columns) { item, cellModifier ->
                        if (item == null) {
                            ShopItemCard(ShopDisplayItem.defaultViri, isOwned = true, isEquipped = equippedKey == null, balance = balance, action = onDefaultTap, modifier = cellModifier)
                        } else {
                            ShopItemCard(item.display, item.key in ownedKeys, equippedKey == item.key, balance, action = { onSkinTap(item) }, modifier = cellModifier)
                        }
                    }
                }
            }
        }
    }
}
