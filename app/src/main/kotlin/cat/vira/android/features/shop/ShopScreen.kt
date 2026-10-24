package cat.vira.android.features.shop

import cat.vira.android.core.sound.ViraSound
import cat.vira.android.core.sound.ViraHaptics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.components.ViraConfettiBurst
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/// Les dues famílies de la Botiga.
enum class ShopSection { Skins, Badges }

/// La Botiga (`SkinShopView`, `USER_FLOWS.md` F18/F19): botiga de joc sobre la referència de Clash
/// Royale amb l'estètica de Vira. La peça destacada, les pestanyes i el catàleg en graella per
/// raresa. Comprar una skin obre una revelació; comprar un badge fa caure confeti.
///
/// Tocar una skin que ja és teva la posa a l'instant (reversible, gratuït i freqüent: no pot costar
/// tres tocs); tocar-ne una per comprar obre el detall.
@Composable
fun ShopScreen(onBack: (() -> Unit)? = null) {
    val data = rememberShopData()
    val scope = rememberCoroutineScope()
    val cardsOn = rememberCardsEnabled()
    val celebrationsOn = rememberCelebrationsEnabled()

    val counter by data.counter.collectAsStateWithLifecycle(null)
    val streak by data.streak.collectAsStateWithLifecycle(null)
    val skins by data.skins.collectAsStateWithLifecycle(emptyList())
    val badges by data.badges.collectAsStateWithLifecycle(emptyList())
    val profile by data.profile.collectAsStateWithLifecycle(null)

    var section by rememberSaveable { mutableStateOf(ShopSection.Skins) }
    var selection by remember { mutableStateOf<SkinItem?>(null) }
    var badgeSelection by remember { mutableStateOf<BadgeItem?>(null) }
    var revealing by remember { mutableStateOf<SkinItem?>(null) }
    var celebrating by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val balance = counter?.coinBalance ?: 0
    val ownedSkins = skins.map { it.key }.toSet()
    val equippedKey = skins.firstOrNull { it.isEquipped }?.key
    val ownedBadges = badges.map { it.key }.toSet()
    val pinned = profile?.pinnedBadgeKeys.orEmpty()

    /// Desa tot o res. Un error de la base de dades es diu a la pantalla; quedar-se curt de
    /// monedes no és un error (el botó ja ho deia) i no canvia res.
    fun wear(key: String?) {
        ViraHaptics.play(context, ViraHaptics.Feedback.Done)
        ViraSound.play(context, ViraSound.Cue.EQUIP)
        scope.launch { data.skinsLogic.equip(key) }
    }

    fun purchase(block: suspend () -> Boolean, onDone: () -> Unit) {
        scope.launch {
            val result = runCatching { block() }
            saveFailed = result.isFailure
            if (result.getOrDefault(false)) onDone()
        }
    }

    ShopContent(
        balance = balance,
        ownedSkinKeys = ownedSkins,
        equippedKey = equippedKey,
        ownedBadgeKeys = ownedBadges,
        counter = counter,
        streak = streak,
        cardsEnabled = cardsOn,
        saveFailed = saveFailed,
        section = section,
        onSection = { section = it },
        onBack = onBack,
        onDefaultTap = { wear(null) },
        // Una skin ja comprada es posa a l'instant: reversible, gratuïta i freqüent.
        onSkinTap = { item -> if (item.key in ownedSkins) wear(item.key) else selection = item },
        onBadgeTap = { badgeSelection = it },
        celebrating = celebrationsOn && celebrating,
    )

    selection?.let { item ->
        ShopDetailSheet(
            item = item.display,
            isOwned = item.key in ownedSkins,
            isEquipped = item.key == equippedKey,
            balance = balance,
            onBuy = {
                purchase({ data.skinsLogic.buy(item) }) {
                    ViraHaptics.play(context, ViraHaptics.Feedback.Reward)
                    // Amb revelació, el so el porta ella, sincronitzat amb el que es veu.
                    if (celebrationsOn) revealing = item else ViraSound.play(context, ViraSound.Cue.EQUIP)
                }
            },
            onWear = { wear(item.key) },
            onDismiss = { selection = null },
        )
    }
    badgeSelection?.let { item ->
        // Un badge no s'«equipa» d'aquí: es penja al banner del Perfil. Comprar-lo el penja sol.
        ShopDetailSheet(
            item = item.display,
            isOwned = item.key in ownedBadges,
            isEquipped = item.key in pinned,
            balance = balance,
            onBuy = { purchase({ data.badgesLogic.buy(item) }) { celebrating = true } },
            onWear = { scope.launch { data.badgesLogic.pin(item.key) } },
            onDismiss = { badgeSelection = null },
        )
    }
    revealing?.let { item ->
        Dialog(
            onDismissRequest = { revealing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ShopRevealScreen(item, onContinue = { revealing = null }, celebrationsEnabled = celebrationsOn)
        }
    }
    LaunchedEffect(celebrating) {
        if (celebrating) {
            delay(1_400)
            celebrating = false
        }
    }
}

/// La Botiga sense estat: el que es veu donades les dades. Les captures la fan servir directament.
@Composable
fun ShopContent(
    balance: Int,
    ownedSkinKeys: Set<String>,
    equippedKey: String?,
    ownedBadgeKeys: Set<String>,
    counter: ProgressCounter?,
    streak: Streak?,
    cardsEnabled: Boolean,
    saveFailed: Boolean,
    section: ShopSection,
    onSection: (ShopSection) -> Unit,
    onBack: (() -> Unit)?,
    onDefaultTap: () -> Unit,
    onSkinTap: (SkinItem) -> Unit,
    onBadgeTap: (BadgeItem) -> Unit,
    modifier: Modifier = Modifier,
    celebrating: Boolean = false,
) {
    val catalog = SkinItem.catalog.filter { it.artIsAvailable }
    val sections = listOf(
        ViraSectionItem(ShopSection.Skins.name, stringResource(R.string.shop_section_skins), R.drawable.viri_entry_waving),
        ViraSectionItem(ShopSection.Badges.name, stringResource(R.string.shop_section_badges), R.drawable.picto_cards),
    )
    Box(modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
        Column(Modifier.fillMaxSize()) {
            ShopNavigationBar(stringResource(R.string.shop_title), onBack) { ShopWallet(balance) }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            ) {
                if (saveFailed) ShopSaveError()
                if (section == ShopSection.Skins) {
                    SkinItem.next(catalog, ownedSkinKeys)?.let { next ->
                        ShopFeaturedCard(next, balance, action = { onSkinTap(next) })
                    }
                }
                ViraSectionSwitcher(
                    area = ViraArea.Today, items = sections, selectedKey = section.name,
                    onSelect = { onSection(ShopSection.valueOf(it)) }, showsAllLabels = true,
                )
                when (section) {
                    ShopSection.Skins -> SkinShopCatalog(catalog, ownedSkinKeys, equippedKey, balance, onDefaultTap, onSkinTap)
                    ShopSection.Badges -> BadgeShopGrid(balance, ownedBadgeKeys, counter, streak, cardsEnabled, onBadgeTap)
                }
                Spacer(Modifier.height(viraTabBarClearance()))
            }
        }
        // El confeti és per als badges: les skins tenen la revelació.
        if (celebrating) ViraConfettiBurst(Modifier.fillMaxSize())
    }
}

/// El catàleg de skins per seccions de raresa, cadascuna amb el preu a la capçalera
/// (`SkinShopCatalog`). El Viri de sempre obre la primera: gratuït, perquè treure's una skin no
/// ha de costar res.
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
                    ShopSectionHeader(stringResource(tier.sectionRes), tier.symbol, tier.shopArea) { ShopCoinAmount(tier.price) }
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
