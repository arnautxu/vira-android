package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraCaptures
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.tokens.ViraColor
import com.android.tools.screenshot.PreviewTest

/// Les pantalles de la Botiga, els premis i les cartes, per posar al costat de l'iPhone. Mateixes
/// dades de mostra que les `#Preview` de l'iPhone sempre que n'hi ha.

private val counter = ProgressCounter(coinBalance = 620, totalXP = 1_200, emotionCheckinsCount = 12, focusSessionsCount = 6, habitsCompletedCount = 14)
private val streak = Streak(currentDays = 9, bestDays = 9)

@Composable
private fun Screen(content: @Composable () -> Unit) = ViraCapture {
    ViraTheme(reduceMotion = true) {
        Box(Modifier.background(ViraColor.background)) { content() }
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaSkins() = Screen {
    ShopContent(
        balance = 620, ownedKeys = setOf("chef"), equippedKey = "chef", wishKey = "",
        saveFailed = false, onBack = {}, onDefaultTap = {}, onSkinTap = {},
    )
}

/// La teva fita marcada amb «La vull» i ja a l'abast: Viri ho diu a l'escenari.
@PreviewTest @ViraScreenCaptures @Composable
fun BotigaObjectiu() = Screen {
    ShopContent(
        balance = 1_600, ownedKeys = setOf("chef"), equippedKey = null, wishKey = "shark",
        saveFailed = false, onBack = {}, onDefaultTap = {}, onSkinTap = {},
    )
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaProvadorComprar() = Screen {
    ShopFittingRoomContent(
        requireNotNull(SkinItem.item("astronaut")), isOwned = false, isEquipped = false, balance = 2_000,
        dailyPace = null, wishKey = "", onWish = {}, onBuy = {}, onWear = {}, onClose = {},
    )
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaProvadorFalten() = Screen {
    ShopFittingRoomContent(
        requireNotNull(SkinItem.item("dragon")), isOwned = false, isEquipped = false, balance = 620,
        dailyPace = 60.0, wishKey = "dragon", onWish = {}, onBuy = {}, onWear = {}, onClose = {},
    )
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaVestidor() = Screen {
    ShopDressingRoom(requireNotNull(SkinItem.item("pirate")), onContinue = {}, holdsClosed = true)
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaSeguentObjectiu() = Screen {
    ShopNextGoalContent(
        bought = requireNotNull(SkinItem.item("chef")), catalog = ShopCollection.catalog(setOf("chef")),
        ownedKeys = setOf("chef"), balance = 620, onChoose = {}, onLater = {}, initialChoice = "shark",
    )
}

@PreviewTest @ViraCaptures @Composable
fun BotigaAvisObjectiu() = Screen {
    ShopGoalBannerCard(requireNotNull(SkinItem.item("dragon")), onOpen = {}, onClose = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun BotigaRevelacio() = Screen {
    ShopRevealScreen(requireNotNull(SkinItem.item("chef")), onContinue = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun PremisBuit() = Screen {
    PersonalRewardsContent(emptyList(), balance = 150, onCreate = {}, onRedeem = {}, onArchive = {}, onBack = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun PremisAmbPremis() = Screen {
    PersonalRewardsContent(
        listOf(
            PersonalReward(title = "Jugar 20 minuts", cost = 100, redeemedCount = 2),
            PersonalReward(title = "Menjar un gelat", cost = 300),
        ),
        balance = 150, onCreate = {}, onRedeem = {}, onArchive = {}, onBack = {},
    )
}

@PreviewTest @ViraCaptures @Composable
fun PremiNou() = Screen {
    PersonalRewardEditorContent(onSave = { _, _ -> }, onCancel = {}, initialTitle = "Jugar 20 minuts", focusesTitle = false)
}

@PreviewTest @ViraScreenCaptures @Composable
fun PremiCelebracio() = Screen {
    RewardCelebrationScreen("Jugar 20 minuts", onContinue = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun CartesBuit() = Screen {
    CardsContent(emptyList(), latest = null, latestIsNew = false, onOpen = {}, onCategory = {}, onBack = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun CartesAmbCartes() = Screen {
    val owned = CardItem.unlocked(counter, streak)
    CardsContent(owned, latest = CardItem.item("week_streak"), latestIsNew = true, onOpen = {}, onCategory = {}, onBack = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun CartesCategoria() = Screen {
    CardCategoryContent(CardItem.Category.Emotions, CardItem.unlocked(counter, streak), onOpen = {}, onBack = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun CartaOberta() = Screen {
    CardCollectionScreen(CardItem.unlocked(counter, streak), startingAt = "week_streak", onClose = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun CartaNova() = Screen {
    CardUnlockedScreen(requireNotNull(CardItem.item("week_streak")), owned = 6, onContinue = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun PersonalitzarPerfil() = Screen {
    ProfileCustomizeContent(
        nickname = "Arnau", ownedSkins = listOfNotNull(SkinItem.item("chef"), SkinItem.item("hacker")), equippedKey = "chef",
        pinnedKeys = listOf("first_checkin", "bolt"), ownedBadgeKeys = setOf("bolt"), counter = counter, streak = streak,
        onDone = {}, onSaveNickname = {}, onEquip = {}, onToggleBadge = {},
    )
}

@PreviewTest @ViraCaptures @Composable
fun PrestatgeDeBadges() = Screen {
    PinnedBadgesShelfContent(listOf("first_checkin", "bolt"), onCustomize = {})
}
