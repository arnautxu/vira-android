package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.ViraCardTallies
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraShopBackdrop

private typealias Tally = ViraCardTallies.Kind
typealias Tallies = ViraCardTallies.Snapshot

/// Una carta de la col·lecció (`CardItem`, `VIRI_AND_GAMIFICATION.md` §2.4). **Contingut de l'app**:
/// no té taula de propietat, perquè l'estat es calcula en viu contra `ProgressCounter`, `Streak` i
/// els recomptes de les cartes (`ViraCardTallies`), que mai baixen. Un cop es compleix el llindar,
/// la carta és teva per sempre.
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
    /// La il·lustració gran (`CardArt`). Totes 67 en tenen.
    @DrawableRes val art: Int?,
    val rarity: Rarity,
    val requirement: Requirement,
) {
    enum class Rarity(@StringRes val labelRes: Int, @StringRes val pluralRes: Int, val symbol: ShopSymbol) {
        Common(R.string.card_rarity_common, R.string.card_rarity_common_plural, ShopSymbol.Circle),
        Rare(R.string.card_rarity_rare, R.string.card_rarity_rare_plural, ShopSymbol.Sparkles),
        Epic(R.string.card_rarity_epic, R.string.card_rarity_epic_plural, ShopSymbol.Diamond),
        Legendary(R.string.card_rarity_legendary, R.string.card_rarity_legendary_plural, ShopSymbol.Crown);

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
        // Les sis de sota no viuen a `ProgressCounter` sinó a `ViraCardTallies` (2026-10-22): una
        // columna nova al comptador demanava una migració de Supabase.
        class TasksDone(target: Int) : Requirement(target)
        class FocusMinutes(target: Int) : Requirement(target)
        class ChallengesDone(target: Int) : Requirement(target)
        class RewardsRedeemed(target: Int) : Requirement(target)
        class SkinsOwned(target: Int) : Requirement(target)
        class Comebacks(target: Int) : Requirement(target)

        /// Quant en portes, mai per sobre del llindar: interessa el camí cap a aquesta carta.
        fun current(counter: ProgressCounter?, streak: Streak?, tallies: Tallies = Tallies.EMPTY): Int {
            val value = when (this) {
                is EmotionCheckins -> counter?.emotionCheckinsCount ?: 0
                is FocusSessions -> counter?.focusSessionsCount ?: 0
                is HabitsCompleted -> counter?.habitsCompletedCount ?: 0
                is TotalXP -> counter?.totalXP ?: 0
                is BestStreakDays -> streak?.bestDays ?: 0
                is DaysActive -> counter?.daysActiveCount ?: 0
                is TasksDone -> tallies[Tally.TASKS_DONE]
                is FocusMinutes -> tallies[Tally.FOCUS_SECONDS] / 60
                is ChallengesDone -> tallies[Tally.CHALLENGES_DONE]
                is RewardsRedeemed -> tallies[Tally.REWARDS_REDEEMED]
                is SkinsOwned -> tallies[Tally.SKINS_OWNED]
                is Comebacks -> tallies[Tally.COMEBACKS]
            }
            return minOf(value, target)
        }
    }

    /// El prestatge de l'àlbum on va cada carta (Arnau, 2026-10-22: «agrupar per categories»).
    /// **Es dedueix del requisit**, no es guarda: cap carta pot quedar sense prestatge ni en dos.
    /// L'ordre dels casos és l'ordre dels prestatges.
    enum class Category(@StringRes val titleRes: Int, val symbol: ShopSymbol) {
        Emotions(R.string.card_category_emotions, ShopSymbol.Heart),
        Focus(R.string.card_category_focus, ShopSymbol.Scope),
        Habits(R.string.card_category_habits, ShopSymbol.CheckCircle),
        Plan(R.string.card_category_plan, ShopSymbol.List),
        Streak(R.string.card_category_streak, ShopSymbol.Flame),
        Days(R.string.card_category_days, ShopSymbol.Calendar),
        Points(R.string.card_category_points, ShopSymbol.Star),
        Moments(R.string.card_category_moments, ShopSymbol.Sparkles),
    }

    val category: Category
        get() = when (requirement) {
            is Requirement.EmotionCheckins -> Category.Emotions
            is Requirement.FocusSessions, is Requirement.FocusMinutes -> Category.Focus
            is Requirement.HabitsCompleted -> Category.Habits
            is Requirement.TasksDone -> Category.Plan
            is Requirement.BestStreakDays -> Category.Streak
            is Requirement.DaysActive -> Category.Days
            is Requirement.TotalXP -> Category.Points
            is Requirement.ChallengesDone, is Requirement.RewardsRedeemed,
            is Requirement.SkinsOwned, is Requirement.Comebacks -> Category.Moments
        }

    /// El tema de les partícules de la part animada (`CardLiveEffect`), per escala.
    enum class ParticleStyle { Bubbles, Stars, Gems }

    val particleStyle: ParticleStyle
        get() = when (requirement) {
            is Requirement.EmotionCheckins -> ParticleStyle.Bubbles
            is Requirement.FocusSessions, is Requirement.FocusMinutes -> ParticleStyle.Stars
            else -> ParticleStyle.Gems
        }

    fun isUnlocked(counter: ProgressCounter?, streak: Streak?, tallies: Tallies = Tallies.EMPTY): Boolean =
        requirement.current(counter, streak, tallies) >= requirement.target

    companion object {
        /// Les 67 cartes (`CardCatalog.swift`), fetes d'**escales** sobre recomptes que no baixen
        /// mai. Mateixes claus que l'iPhone i en el mateix ordre (de la més fàcil a la més cara).
        ///
        /// **Fora per norma**: medicació i Mindfulness (no donen premis), crear coses, una carta per
        /// a cada emoció (seria un rànquing), temporades (edicions limitades) i hores concretes.
        val catalog: List<CardItem> = listOf(
            // Emocions
            card("first_checkin", R.string.card_first_checkin_title, R.string.card_first_checkin_description, R.string.card_first_checkin_meaning, R.drawable.picto_heart, R.drawable.card_emotions_1, Rarity.Common, Requirement.EmotionCheckins(1)),
            card("emotions_5", R.string.card_emotions_5_title, R.string.card_emotions_5_description, R.string.card_emotions_5_meaning, R.drawable.picto_heart, R.drawable.card_emotions_5, Rarity.Common, Requirement.EmotionCheckins(5)),
            card("emotions_10", R.string.card_emotions_10_title, R.string.card_emotions_10_description, R.string.card_emotions_10_meaning, R.drawable.picto_heart, R.drawable.card_emotions_10, Rarity.Rare, Requirement.EmotionCheckins(10)),
            card("emotions_25", R.string.card_emotions_25_title, R.string.card_emotions_25_description, R.string.card_emotions_25_meaning, R.drawable.picto_heart, R.drawable.card_emotions_25, Rarity.Rare, Requirement.EmotionCheckins(25)),
            card("emotions_50", R.string.card_emotions_50_title, R.string.card_emotions_50_description, R.string.card_emotions_50_meaning, R.drawable.picto_heart, R.drawable.card_emotions_50, Rarity.Epic, Requirement.EmotionCheckins(50)),
            card("emotions_100", R.string.card_emotions_100_title, R.string.card_emotions_100_description, R.string.card_emotions_100_meaning, R.drawable.picto_heart, R.drawable.card_emotions_100, Rarity.Legendary, Requirement.EmotionCheckins(100)),

            // Concentració
            card("focus_1", R.string.card_focus_1_title, R.string.card_focus_1_description, R.string.card_focus_1_meaning, R.drawable.picto_game_target, R.drawable.card_focus_1, Rarity.Common, Requirement.FocusSessions(1)),
            card("focus_5", R.string.card_focus_5_title, R.string.card_focus_5_description, R.string.card_focus_5_meaning, R.drawable.picto_game_target, R.drawable.card_focus_5, Rarity.Common, Requirement.FocusSessions(5)),
            card("focus_10", R.string.card_focus_10_title, R.string.card_focus_10_description, R.string.card_focus_10_meaning, R.drawable.picto_game_target, R.drawable.card_focus_10, Rarity.Rare, Requirement.FocusSessions(10)),
            card("focus_20", R.string.card_focus_20_title, R.string.card_focus_20_description, R.string.card_focus_20_meaning, R.drawable.picto_game_target, R.drawable.card_focus_20, Rarity.Rare, Requirement.FocusSessions(20)),
            card("focus_30", R.string.card_focus_30_title, R.string.card_focus_30_description, R.string.card_focus_30_meaning, R.drawable.picto_game_target, R.drawable.card_focus_30, Rarity.Epic, Requirement.FocusSessions(30)),
            card("focus_50", R.string.card_focus_50_title, R.string.card_focus_50_description, R.string.card_focus_50_meaning, R.drawable.picto_game_target, R.drawable.card_focus_50, Rarity.Epic, Requirement.FocusSessions(50)),
            card("focus_100", R.string.card_focus_100_title, R.string.card_focus_100_description, R.string.card_focus_100_meaning, R.drawable.picto_game_target, R.drawable.card_focus_100, Rarity.Legendary, Requirement.FocusSessions(100)),
            card("focus_minutes_60", R.string.card_focus_minutes_60_title, R.string.card_focus_minutes_60_description, R.string.card_focus_minutes_60_meaning, R.drawable.picto_hourglass, R.drawable.card_focus_minutes_60, Rarity.Common, Requirement.FocusMinutes(60)),
            card("focus_minutes_300", R.string.card_focus_minutes_300_title, R.string.card_focus_minutes_300_description, R.string.card_focus_minutes_300_meaning, R.drawable.picto_hourglass, R.drawable.card_focus_minutes_300, Rarity.Rare, Requirement.FocusMinutes(300)),
            card("focus_minutes_1500", R.string.card_focus_minutes_1500_title, R.string.card_focus_minutes_1500_description, R.string.card_focus_minutes_1500_meaning, R.drawable.picto_hourglass, R.drawable.card_focus_minutes_1500, Rarity.Epic, Requirement.FocusMinutes(1500)),
            card("focus_minutes_6000", R.string.card_focus_minutes_6000_title, R.string.card_focus_minutes_6000_description, R.string.card_focus_minutes_6000_meaning, R.drawable.picto_hourglass, R.drawable.card_focus_minutes_6000, Rarity.Legendary, Requirement.FocusMinutes(6000)),

            // Hàbits
            card("habits_1", R.string.card_habits_1_title, R.string.card_habits_1_description, R.string.card_habits_1_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_1, Rarity.Common, Requirement.HabitsCompleted(1)),
            card("habits_5", R.string.card_habits_5_title, R.string.card_habits_5_description, R.string.card_habits_5_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_5, Rarity.Common, Requirement.HabitsCompleted(5)),
            card("habits_10", R.string.card_habits_10_title, R.string.card_habits_10_description, R.string.card_habits_10_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_10, Rarity.Common, Requirement.HabitsCompleted(10)),
            card("habits_25", R.string.card_habits_25_title, R.string.card_habits_25_description, R.string.card_habits_25_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_25, Rarity.Rare, Requirement.HabitsCompleted(25)),
            card("habits_50", R.string.card_habits_50_title, R.string.card_habits_50_description, R.string.card_habits_50_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_50, Rarity.Rare, Requirement.HabitsCompleted(50)),
            card("habits_100", R.string.card_habits_100_title, R.string.card_habits_100_description, R.string.card_habits_100_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_100, Rarity.Epic, Requirement.HabitsCompleted(100)),
            card("habits_150", R.string.card_habits_150_title, R.string.card_habits_150_description, R.string.card_habits_150_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_150, Rarity.Epic, Requirement.HabitsCompleted(150)),
            card("habits_300", R.string.card_habits_300_title, R.string.card_habits_300_description, R.string.card_habits_300_meaning, R.drawable.picto_game_badge, R.drawable.card_habits_300, Rarity.Legendary, Requirement.HabitsCompleted(300)),

            // Tasques del Pla
            card("tasks_1", R.string.card_tasks_1_title, R.string.card_tasks_1_description, R.string.card_tasks_1_meaning, R.drawable.picto_flag, R.drawable.card_tasks_1, Rarity.Common, Requirement.TasksDone(1)),
            card("tasks_5", R.string.card_tasks_5_title, R.string.card_tasks_5_description, R.string.card_tasks_5_meaning, R.drawable.picto_flag, R.drawable.card_tasks_5, Rarity.Common, Requirement.TasksDone(5)),
            card("tasks_10", R.string.card_tasks_10_title, R.string.card_tasks_10_description, R.string.card_tasks_10_meaning, R.drawable.picto_flag, R.drawable.card_tasks_10, Rarity.Common, Requirement.TasksDone(10)),
            card("tasks_25", R.string.card_tasks_25_title, R.string.card_tasks_25_description, R.string.card_tasks_25_meaning, R.drawable.picto_flag, R.drawable.card_tasks_25, Rarity.Rare, Requirement.TasksDone(25)),
            card("tasks_50", R.string.card_tasks_50_title, R.string.card_tasks_50_description, R.string.card_tasks_50_meaning, R.drawable.picto_flag, R.drawable.card_tasks_50, Rarity.Rare, Requirement.TasksDone(50)),
            card("tasks_100", R.string.card_tasks_100_title, R.string.card_tasks_100_description, R.string.card_tasks_100_meaning, R.drawable.picto_flag, R.drawable.card_tasks_100, Rarity.Epic, Requirement.TasksDone(100)),
            card("tasks_250", R.string.card_tasks_250_title, R.string.card_tasks_250_description, R.string.card_tasks_250_meaning, R.drawable.picto_flag, R.drawable.card_tasks_250, Rarity.Epic, Requirement.TasksDone(250)),
            card("tasks_500", R.string.card_tasks_500_title, R.string.card_tasks_500_description, R.string.card_tasks_500_meaning, R.drawable.picto_flag, R.drawable.card_tasks_500, Rarity.Legendary, Requirement.TasksDone(500)),

            // Ratxa
            card("streak_3", R.string.card_streak_3_title, R.string.card_streak_3_description, R.string.card_streak_3_meaning, R.drawable.picto_flame_clay, R.drawable.card_streak_3, Rarity.Common, Requirement.BestStreakDays(3)),
            card("week_streak", R.string.card_week_streak_title, R.string.card_week_streak_description, R.string.card_week_streak_meaning, R.drawable.picto_flame_clay, R.drawable.badge_week_streak, Rarity.Rare, Requirement.BestStreakDays(7)),
            card("streak_14", R.string.card_streak_14_title, R.string.card_streak_14_description, R.string.card_streak_14_meaning, R.drawable.picto_flame_clay, R.drawable.card_streak_14, Rarity.Rare, Requirement.BestStreakDays(14)),
            card("streak_21", R.string.card_streak_21_title, R.string.card_streak_21_description, R.string.card_streak_21_meaning, R.drawable.picto_flame_clay, R.drawable.card_streak_21, Rarity.Rare, Requirement.BestStreakDays(21)),
            card("month_streak", R.string.card_month_streak_title, R.string.card_month_streak_description, R.string.card_month_streak_meaning, R.drawable.picto_game_trophy, R.drawable.badge_month_streak, Rarity.Epic, Requirement.BestStreakDays(30)),
            card("streak_45", R.string.card_streak_45_title, R.string.card_streak_45_description, R.string.card_streak_45_meaning, R.drawable.picto_game_trophy, R.drawable.card_streak_45, Rarity.Epic, Requirement.BestStreakDays(45)),
            card("streak_60", R.string.card_streak_60_title, R.string.card_streak_60_description, R.string.card_streak_60_meaning, R.drawable.picto_game_trophy, R.drawable.card_streak_60, Rarity.Epic, Requirement.BestStreakDays(60)),
            card("streak_100", R.string.card_streak_100_title, R.string.card_streak_100_description, R.string.card_streak_100_meaning, R.drawable.picto_game_trophy, R.drawable.card_streak_100, Rarity.Legendary, Requirement.BestStreakDays(100)),

            // Punts
            card("xp_100", R.string.card_xp_100_title, R.string.card_xp_100_description, R.string.card_xp_100_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_100, Rarity.Common, Requirement.TotalXP(100)),
            card("xp_250", R.string.card_xp_250_title, R.string.card_xp_250_description, R.string.card_xp_250_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_250, Rarity.Common, Requirement.TotalXP(250)),
            card("xp_500", R.string.card_xp_500_title, R.string.card_xp_500_description, R.string.card_xp_500_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_500, Rarity.Rare, Requirement.TotalXP(500)),
            card("xp_1000", R.string.card_xp_1000_title, R.string.card_xp_1000_description, R.string.card_xp_1000_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_1000, Rarity.Rare, Requirement.TotalXP(1000)),
            card("xp_1500", R.string.card_xp_1500_title, R.string.card_xp_1500_description, R.string.card_xp_1500_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_1500, Rarity.Rare, Requirement.TotalXP(1500)),
            card("xp_2500", R.string.card_xp_2500_title, R.string.card_xp_2500_description, R.string.card_xp_2500_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_2500, Rarity.Epic, Requirement.TotalXP(2500)),
            card("xp_5000", R.string.card_xp_5000_title, R.string.card_xp_5000_description, R.string.card_xp_5000_meaning, R.drawable.picto_game_medal, R.drawable.card_xp_5000, Rarity.Legendary, Requirement.TotalXP(5000)),

            // Dies amb Vira: dies que el dia va comptar (`ViraProgress`), no cal que seguits
            card("days_1", R.string.card_days_1_title, R.string.card_days_1_description, R.string.card_days_1_meaning, R.drawable.viri_medal, R.drawable.card_days_1, Rarity.Common, Requirement.DaysActive(1)),
            card("days_7", R.string.card_days_7_title, R.string.card_days_7_description, R.string.card_days_7_meaning, R.drawable.viri_medal, R.drawable.card_days_7, Rarity.Common, Requirement.DaysActive(7)),
            card("days_14", R.string.card_days_14_title, R.string.card_days_14_description, R.string.card_days_14_meaning, R.drawable.viri_medal, R.drawable.card_days_14, Rarity.Common, Requirement.DaysActive(14)),
            card("days_30", R.string.card_days_30_title, R.string.card_days_30_description, R.string.card_days_30_meaning, R.drawable.viri_medal, R.drawable.card_days_30, Rarity.Rare, Requirement.DaysActive(30)),
            card("days_60", R.string.card_days_60_title, R.string.card_days_60_description, R.string.card_days_60_meaning, R.drawable.viri_medal, R.drawable.card_days_60, Rarity.Epic, Requirement.DaysActive(60)),
            card("days_100", R.string.card_days_100_title, R.string.card_days_100_description, R.string.card_days_100_meaning, R.drawable.viri_medal, R.drawable.card_days_100, Rarity.Legendary, Requirement.DaysActive(100)),
            card("days_200", R.string.card_days_200_title, R.string.card_days_200_description, R.string.card_days_200_meaning, R.drawable.viri_medal, R.drawable.card_days_200, Rarity.Legendary, Requirement.DaysActive(200)),
            card("days_365", R.string.card_days_365_title, R.string.card_days_365_description, R.string.card_days_365_meaning, R.drawable.viri_medal, R.drawable.card_days_365, Rarity.Legendary, Requirement.DaysActive(365)),

            // Moments: reptes, premis, skins i tornar
            card("challenges_1", R.string.card_challenges_1_title, R.string.card_challenges_1_description, R.string.card_challenges_1_meaning, R.drawable.picto_rocket, R.drawable.card_challenges_1, Rarity.Common, Requirement.ChallengesDone(1)),
            card("challenges_5", R.string.card_challenges_5_title, R.string.card_challenges_5_description, R.string.card_challenges_5_meaning, R.drawable.picto_rocket, R.drawable.card_challenges_5, Rarity.Rare, Requirement.ChallengesDone(5)),
            card("challenges_25", R.string.card_challenges_25_title, R.string.card_challenges_25_description, R.string.card_challenges_25_meaning, R.drawable.picto_rocket, R.drawable.card_challenges_25, Rarity.Epic, Requirement.ChallengesDone(25)),
            card("rewards_1", R.string.card_rewards_1_title, R.string.card_rewards_1_description, R.string.card_rewards_1_meaning, R.drawable.picto_gift, R.drawable.card_rewards_1, Rarity.Common, Requirement.RewardsRedeemed(1)),
            card("rewards_5", R.string.card_rewards_5_title, R.string.card_rewards_5_description, R.string.card_rewards_5_meaning, R.drawable.picto_gift, R.drawable.card_rewards_5, Rarity.Rare, Requirement.RewardsRedeemed(5)),
            card("rewards_20", R.string.card_rewards_20_title, R.string.card_rewards_20_description, R.string.card_rewards_20_meaning, R.drawable.picto_gift, R.drawable.card_rewards_20, Rarity.Epic, Requirement.RewardsRedeemed(20)),
            card("skins_1", R.string.card_skins_1_title, R.string.card_skins_1_description, R.string.card_skins_1_meaning, R.drawable.picto_shop, R.drawable.card_skins_1, Rarity.Common, Requirement.SkinsOwned(1)),
            card("skins_3", R.string.card_skins_3_title, R.string.card_skins_3_description, R.string.card_skins_3_meaning, R.drawable.picto_shop, R.drawable.card_skins_3, Rarity.Rare, Requirement.SkinsOwned(3)),
            card("skins_10", R.string.card_skins_10_title, R.string.card_skins_10_description, R.string.card_skins_10_meaning, R.drawable.picto_shop, R.drawable.card_skins_10, Rarity.Epic, Requirement.SkinsOwned(10)),
            card("comeback", R.string.card_comeback_title, R.string.card_comeback_description, R.string.card_comeback_meaning, R.drawable.picto_key, R.drawable.card_comeback, Rarity.Rare, Requirement.Comebacks(1)),
            card("comeback_3", R.string.card_comeback_3_title, R.string.card_comeback_3_description, R.string.card_comeback_3_meaning, R.drawable.picto_key, R.drawable.card_comeback_3, Rarity.Epic, Requirement.Comebacks(3)),
        )

        /// Les setze del catàleg d'abans del 2026-10-22: el primer cop que s'obre una versió amb més
        /// cartes, diuen quines són noves (`ViraCardCelebrations.prepare`).
        val firstCatalogKeys: Set<String> = setOf(
            "first_checkin", "emotions_10", "emotions_50", "focus_5", "focus_10", "focus_30",
            "habits_10", "habits_50", "habits_150", "week_streak", "month_streak", "streak_100",
            "xp_100", "xp_1000", "xp_5000", "days_30",
        )

        fun item(key: String): CardItem? = catalog.firstOrNull { it.key == key }

        /// Les d'un prestatge, en l'ordre del catàleg.
        fun catalog(category: Category): List<CardItem> = catalog.filter { it.category == category }

        /// Les guanyades ara mateix, en l'ordre del catàleg.
        fun unlocked(counter: ProgressCounter?, streak: Streak?, tallies: Tallies = Tallies.EMPTY): List<CardItem> =
            catalog.filter { it.isUnlocked(counter, streak, tallies) }

        private fun card(
            key: String, title: Int, description: Int, meaning: Int, pictogram: Int, art: Int,
            rarity: Rarity, requirement: Requirement,
        ) = CardItem(key, title, description, meaning, pictogram, art, rarity, requirement)
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
