package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraShopBackdrop

/// Una carta de la col·lecció (`CardItem`, `VIRI_AND_GAMIFICATION.md` §2.4). **Contingut de l'app**:
/// no té taula de propietat, perquè l'estat es calcula en viu contra `ProgressCounter` i `Streak`,
/// que mai baixen. Un cop es compleix el llindar, la carta és teva per sempre.
///
/// La raresa diu **quant costa**, no com n'hi ha de poques: no hi ha tirades ni res aleatori.
data class CardItem(
    val key: String,
    @StringRes val titleRes: Int,
    /// El requisit en text. Es diu quan la carta ja és teva, no abans.
    @StringRes val descriptionRes: Int,
    /// Què vol dir haver-la guanyada. Només a la col·lecció.
    @StringRes val meaningRes: Int,
    /// Pictograma de reserva mentre no hi ha il·lustració pròpia (`CardArt`).
    @DrawableRes val pictogram: Int,
    val rarity: Rarity,
    val requirement: Requirement,
) {
    enum class Rarity(@StringRes val labelRes: Int, val symbol: ShopSymbol) {
        Common(R.string.card_rarity_common, ShopSymbol.Circle),
        Rare(R.string.card_rarity_rare, ShopSymbol.Sparkles),
        Epic(R.string.card_rarity_epic, ShopSymbol.Diamond),
        Legendary(R.string.card_rarity_legendary, ShopSymbol.Crown);

        /// Gruix de la vora: la part de la distinció que sobreviu al blanc i negre.
        val borderWidth: Float get() = (ordinal + 1).toFloat()

        val shopArea: ViraArea
            get() = when (this) {
                Common -> ViraArea.Plan
                Rare -> ViraArea.Focus
                Epic -> ViraArea.Today
                Legendary -> ViraArea.Rewards
            }
    }

    sealed class Requirement(val target: Int) {
        class EmotionCheckins(target: Int) : Requirement(target)
        class FocusSessions(target: Int) : Requirement(target)
        class HabitsCompleted(target: Int) : Requirement(target)
        class TotalXP(target: Int) : Requirement(target)
        class BestStreakDays(target: Int) : Requirement(target)
        class DaysActive(target: Int) : Requirement(target)

        /// Quant en portes, mai per sobre del llindar: interessa el camí cap a aquesta carta.
        fun current(counter: ProgressCounter?, streak: Streak?): Int {
            val value = when (this) {
                is EmotionCheckins -> counter?.emotionCheckinsCount ?: 0
                is FocusSessions -> counter?.focusSessionsCount ?: 0
                is HabitsCompleted -> counter?.habitsCompletedCount ?: 0
                is TotalXP -> counter?.totalXP ?: 0
                is BestStreakDays -> streak?.bestDays ?: 0
                is DaysActive -> counter?.daysActiveCount ?: 0
            }
            return minOf(value, target)
        }
    }

    fun isUnlocked(counter: ProgressCounter?, streak: Streak?): Boolean =
        requirement.current(counter, streak) >= requirement.target

    /// La il·lustració gran, si en té (`CardArt`). Totes setze en tenen.
    @get:DrawableRes val art: Int? get() = CARD_ART[key]

    companion object {
        /// Les 16 cartes, escales sobre comptadors que ja existeixen. Mateixes claus que l'iPhone.
        val catalog: List<CardItem> = listOf(
            card("first_checkin", R.string.card_first_checkin_title, R.string.card_first_checkin_description, R.string.card_first_checkin_meaning, R.drawable.picto_heart, Rarity.Common, Requirement.EmotionCheckins(1)),
            card("emotions_10", R.string.card_emotions_10_title, R.string.card_emotions_10_description, R.string.card_emotions_10_meaning, R.drawable.picto_heart, Rarity.Rare, Requirement.EmotionCheckins(10)),
            card("emotions_50", R.string.card_emotions_50_title, R.string.card_emotions_50_description, R.string.card_emotions_50_meaning, R.drawable.picto_heart, Rarity.Epic, Requirement.EmotionCheckins(50)),
            card("focus_5", R.string.card_focus_5_title, R.string.card_focus_5_description, R.string.card_focus_5_meaning, R.drawable.picto_game_target, Rarity.Common, Requirement.FocusSessions(5)),
            card("focus_10", R.string.card_focus_10_title, R.string.card_focus_10_description, R.string.card_focus_10_meaning, R.drawable.picto_game_target, Rarity.Rare, Requirement.FocusSessions(10)),
            card("focus_30", R.string.card_focus_30_title, R.string.card_focus_30_description, R.string.card_focus_30_meaning, R.drawable.picto_game_target, Rarity.Epic, Requirement.FocusSessions(30)),
            card("habits_10", R.string.card_habits_10_title, R.string.card_habits_10_description, R.string.card_habits_10_meaning, R.drawable.picto_game_badge, Rarity.Common, Requirement.HabitsCompleted(10)),
            card("habits_50", R.string.card_habits_50_title, R.string.card_habits_50_description, R.string.card_habits_50_meaning, R.drawable.picto_game_badge, Rarity.Rare, Requirement.HabitsCompleted(50)),
            card("habits_150", R.string.card_habits_150_title, R.string.card_habits_150_description, R.string.card_habits_150_meaning, R.drawable.picto_game_badge, Rarity.Epic, Requirement.HabitsCompleted(150)),
            card("week_streak", R.string.card_week_streak_title, R.string.card_week_streak_description, R.string.card_week_streak_meaning, R.drawable.picto_flame_clay, Rarity.Rare, Requirement.BestStreakDays(7)),
            card("month_streak", R.string.card_month_streak_title, R.string.card_month_streak_description, R.string.card_month_streak_meaning, R.drawable.picto_game_trophy, Rarity.Epic, Requirement.BestStreakDays(30)),
            card("streak_100", R.string.card_streak_100_title, R.string.card_streak_100_description, R.string.card_streak_100_meaning, R.drawable.picto_game_trophy, Rarity.Legendary, Requirement.BestStreakDays(100)),
            card("xp_100", R.string.card_xp_100_title, R.string.card_xp_100_description, R.string.card_xp_100_meaning, R.drawable.picto_game_medal, Rarity.Common, Requirement.TotalXP(100)),
            card("xp_1000", R.string.card_xp_1000_title, R.string.card_xp_1000_description, R.string.card_xp_1000_meaning, R.drawable.picto_game_medal, Rarity.Rare, Requirement.TotalXP(1000)),
            card("xp_5000", R.string.card_xp_5000_title, R.string.card_xp_5000_description, R.string.card_xp_5000_meaning, R.drawable.picto_game_medal, Rarity.Legendary, Requirement.TotalXP(5000)),
            card("days_30", R.string.card_days_30_title, R.string.card_days_30_description, R.string.card_days_30_meaning, R.drawable.viri_medal, Rarity.Rare, Requirement.DaysActive(30)),
        )

        fun item(key: String): CardItem? = catalog.firstOrNull { it.key == key }

        /// Les guanyades ara mateix, en l'ordre del catàleg.
        fun unlocked(counter: ProgressCounter?, streak: Streak?): List<CardItem> =
            catalog.filter { it.isUnlocked(counter, streak) }

        private fun card(
            key: String, title: Int, description: Int, meaning: Int, pictogram: Int,
            rarity: Rarity, requirement: Requirement,
        ) = CardItem(key, title, description, meaning, pictogram, rarity, requirement)

        /// `CardArt` de l'iPhone: la il·lustració gran de cada carta.
        private val CARD_ART: Map<String, Int> = mapOf(
            "first_checkin" to R.drawable.card_emotions_1,
            "emotions_10" to R.drawable.card_emotions_10,
            "emotions_50" to R.drawable.card_emotions_50,
            "focus_5" to R.drawable.card_focus_5,
            "focus_10" to R.drawable.card_focus_10,
            "focus_30" to R.drawable.card_focus_30,
            "habits_10" to R.drawable.card_habits_10,
            "habits_50" to R.drawable.card_habits_50,
            "habits_150" to R.drawable.card_habits_150,
            "xp_100" to R.drawable.card_xp_100,
            "xp_1000" to R.drawable.card_xp_1000,
            "xp_5000" to R.drawable.card_xp_5000,
            "week_streak" to R.drawable.badge_week_streak,
            "month_streak" to R.drawable.badge_month_streak,
            "streak_100" to R.drawable.card_streak_100,
            "days_30" to R.drawable.card_days_30,
        )
    }
}

/// Una peça del banner del Perfil (`BadgeItem`). Dues fonts: **guanyada** (és una carta, no es
/// compra) i **comprada** (ViriCoins, purament estètica). Mai es distingeixen només pel color.
data class BadgeItem(
    val key: String,
    @DrawableRes val pictogram: Int,
    @StringRes val nameRes: Int,
    /// `null` als guanyats.
    val price: Int?,
    val requirement: CardItem.Requirement? = null,
) {
    val isEarned: Boolean get() = price == null

    /// Color del marc d'un badge comprable. Segueix l'ordre de preu, i el preu sempre és escrit.
    val shopArea: ViraArea
        get() = when (key) {
            "bolt" -> ViraArea.Plan
            "rocket" -> ViraArea.Focus
            "gem" -> ViraArea.Today
            else -> ViraArea.Rewards
        }

    /// Fons del detall (`BadgeItem.backdrop`).
    val shopBackdrop: ViraShopBackdrop
        get() = when (key) {
            "bolt" -> ViraShopBackdrop.Amber
            "rocket" -> ViraShopBackdrop.Indigo
            "gem" -> ViraShopBackdrop.Mint
            "crown" -> ViraShopBackdrop.Violet
            else -> ViraShopBackdrop.Ocean
        }

    companion object {
        /// Les cartes **són** badges: derivat, no duplicat.
        val earnedCatalog: List<BadgeItem> = CardItem.catalog.map {
            BadgeItem(it.key, it.pictogram, it.titleRes, price = null, requirement = it.requirement)
        }

        /// Per sota del terra de les skins (500): un badge és el primer que et pots permetre.
        val boughtCatalog: List<BadgeItem> = listOf(
            BadgeItem("bolt", R.drawable.picto_bolt, R.string.badge_bolt, 150),
            BadgeItem("rocket", R.drawable.picto_game_rocket, R.string.badge_rocket, 250),
            BadgeItem("gem", R.drawable.picto_gem, R.string.badge_gem, 350),
            BadgeItem("crown", R.drawable.picto_game_crown, R.string.badge_crown, 450),
        )

        val catalog: List<BadgeItem> = earnedCatalog + boughtCatalog

        fun item(key: String): BadgeItem? = catalog.firstOrNull { it.key == key }
    }
}
