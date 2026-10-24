package cat.vira.android.features.shop

import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.ViraCardTallies.Kind as Tally
import cat.vira.android.core.data.model.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/// `ViraCardsTests` de l'iPhone: cada carta s'obre exactament al seu llindar, mai abans.
class ViraCardsTest {

    private fun item(key: String) = requireNotNull(CardItem.item(key)) { "falta $key" }

    private fun tallies(vararg values: Pair<Tally, Int>) = Tallies(values.toMap())

    @Test
    fun senseComptadorsTotesSurtenBloquejades() {
        for (card in CardItem.catalog) assertFalse(card.key, card.isUnlocked(null, null))
    }

    @Test
    fun primerRegistreEmocional() {
        val card = item("first_checkin")
        assertFalse(card.isUnlocked(ProgressCounter(), null))
        assertTrue(card.isUnlocked(ProgressCounter(emotionCheckinsCount = 1), null))
    }

    @Test
    fun laSetmanaEsFixaAmbLaMillorRatxaNoLActual() {
        val card = item("week_streak")
        assertFalse(card.isUnlocked(null, Streak(currentDays = 100, bestDays = 6)))
        assertTrue(card.isUnlocked(null, Streak(currentDays = 100, bestDays = 7)))
    }

    @Test
    fun elMesPerSotaIJustAlLlindar() {
        val card = item("month_streak")
        assertFalse(card.isUnlocked(null, Streak(bestDays = 29)))
        assertTrue(card.isUnlocked(null, Streak(bestDays = 30)))
    }

    @Test
    fun deuSessionsDeFocus() {
        val card = item("focus_10")
        assertFalse(card.isUnlocked(ProgressCounter(focusSessionsCount = 9), null))
        assertTrue(card.isUnlocked(ProgressCounter(focusSessionsCount = 10), null))
    }

    @Test
    fun cinquantaHabits() {
        val card = item("habits_50")
        assertFalse(card.isUnlocked(ProgressCounter(habitsCompletedCount = 49), null))
        assertTrue(card.isUnlocked(ProgressCounter(habitsCompletedCount = 50), null))
    }

    @Test
    fun milXpILaCartaMaiEsTanca() {
        val card = item("xp_1000")
        assertFalse(card.isUnlocked(ProgressCounter(totalXP = 999), null))
        assertTrue(card.isUnlocked(ProgressCounter(totalXP = 1000), null))
        assertTrue(card.isUnlocked(ProgressCounter(totalXP = 5000), null))
    }

    @Test
    fun elsDiesActius() {
        val card = item("days_30")
        assertFalse(card.isUnlocked(ProgressCounter(daysActiveCount = 29), null))
        assertTrue(card.isUnlocked(ProgressCounter(daysActiveCount = 30), null))
    }

    @Test
    fun elCatalegTeLes67CartesDeLAlbumICapClauRepetida() {
        assertEquals(67, CardItem.catalog.size)
        assertTrue("cap de les setze primeres desapareix", CardItem.catalog.map { it.key }.containsAll(CardItem.firstCatalogKeys))
        assertEquals(CardItem.catalog.size, CardItem.catalog.map { it.key }.toSet().size)
        for (card in CardItem.catalog) assertTrue(card.requirement.target > 0)
    }

    @Test
    fun dinsDUnaEscalaLaRaresaNoBaixa() {
        val ladders = listOf(
            listOf("first_checkin", "emotions_5", "emotions_10", "emotions_25", "emotions_50", "emotions_100"),
            listOf("focus_1", "focus_5", "focus_10", "focus_20", "focus_30", "focus_50", "focus_100"),
            listOf("focus_minutes_60", "focus_minutes_300", "focus_minutes_1500", "focus_minutes_6000"),
            listOf("habits_1", "habits_5", "habits_10", "habits_25", "habits_50", "habits_100", "habits_150", "habits_300"),
            listOf("tasks_1", "tasks_5", "tasks_10", "tasks_25", "tasks_50", "tasks_100", "tasks_250", "tasks_500"),
            listOf("streak_3", "week_streak", "streak_14", "streak_21", "month_streak", "streak_45", "streak_60", "streak_100"),
            listOf("xp_100", "xp_250", "xp_500", "xp_1000", "xp_1500", "xp_2500", "xp_5000"),
            listOf("days_1", "days_7", "days_14", "days_30", "days_60", "days_100", "days_200", "days_365"),
            listOf("challenges_1", "challenges_5", "challenges_25"),
            listOf("rewards_1", "rewards_5", "rewards_20"),
            listOf("skins_1", "skins_3", "skins_10"),
            listOf("comeback", "comeback_3"),
        )
        for (ladder in ladders) {
            ladder.map(::item).zipWithNext { lower, higher ->
                assertTrue(lower.requirement.target < higher.requirement.target)
                assertTrue(lower.rarity <= higher.rarity)
            }
        }
    }

    @Test
    fun capLlindarPassaDels100DiesDeRatxaNiDels5000Xp() {
        for (card in CardItem.catalog) {
            when (val requirement = card.requirement) {
                is CardItem.Requirement.BestStreakDays -> assertTrue(requirement.target <= 100)
                is CardItem.Requirement.TotalXP -> assertTrue(requirement.target <= 5_000)
                else -> Unit
            }
        }
    }

    @Test
    fun lesEscalesNovesEsLlegeixenDelsRecomptesIElsMinutsSurtenDelsSegons() {
        val tasks = item("tasks_1")
        assertFalse(tasks.isUnlocked(null, null, Tallies.EMPTY))
        assertTrue(tasks.isUnlocked(null, null, tallies(Tally.TASKS_DONE to 1)))

        val hours = item("focus_minutes_300")
        assertFalse(hours.isUnlocked(null, null, tallies(Tally.FOCUS_SECONDS to 300 * 60 - 1)))
        assertTrue(hours.isUnlocked(null, null, tallies(Tally.FOCUS_SECONDS to 300 * 60)))
    }

    @Test
    fun cadaCartaTeUnSolPrestatgeICapPrestatgeQuedaBuit() {
        assertEquals(CardItem.Category.entries.toSet(), CardItem.catalog.map { it.category }.toSet())
        assertEquals(CardItem.catalog.size, CardItem.Category.entries.sumOf { CardItem.catalog(it).size })
        for (category in CardItem.Category.entries) assertTrue("$category és massa curt", CardItem.catalog(category).size >= 3)
    }

    @Test
    fun lesEscalesVanAlSeuPrestatge() {
        assertEquals(CardItem.Category.Focus, item("focus_minutes_300").category)
        assertEquals(CardItem.Category.Focus, item("focus_10").category)
        assertEquals(CardItem.Category.Plan, item("tasks_25").category)
        assertEquals(CardItem.Category.Days, item("days_7").category)
        assertEquals(CardItem.Category.Moments, item("challenges_1").category)
        assertEquals(CardItem.Category.Moments, item("skins_1").category)
        assertEquals(CardItem.Category.Moments, item("comeback").category)
    }

    @Test
    fun cadaCartaTeElSeuBadgeICapComprat() {
        assertEquals(CardItem.catalog.map { it.key }, BadgeItem.earnedCatalog.map { it.key })
        assertTrue(BadgeItem.boughtCatalog.none { it.isEarned })
    }
}
