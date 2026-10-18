package cat.vira.android.features.shop

import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/// `ViraCardsTests` de l'iPhone: cada carta s'obre exactament al seu llindar, mai abans.
class ViraCardsTest {

    private fun item(key: String) = requireNotNull(CardItem.item(key)) { "falta $key" }

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
    fun elCatalegTeLaMidaDeLaRecomanacioICapClauRepetida() {
        assertTrue(CardItem.catalog.size in 12..16)
        assertEquals(CardItem.catalog.size, CardItem.catalog.map { it.key }.toSet().size)
        for (card in CardItem.catalog) assertTrue(card.requirement.target > 0)
    }

    @Test
    fun dinsDUnaEscalaLaRaresaNoBaixa() {
        val ladders = listOf(
            listOf("first_checkin", "emotions_10", "emotions_50"),
            listOf("focus_5", "focus_10", "focus_30"),
            listOf("habits_10", "habits_50", "habits_150"),
            listOf("week_streak", "month_streak", "streak_100"),
            listOf("xp_100", "xp_1000", "xp_5000"),
        )
        for (ladder in ladders) {
            ladder.map(::item).zipWithNext { lower, higher ->
                assertTrue(lower.requirement.target < higher.requirement.target)
                assertTrue(lower.rarity <= higher.rarity)
            }
        }
    }

    @Test
    fun cadaCartaTeElSeuBadgeICapComprat() {
        assertEquals(CardItem.catalog.map { it.key }, BadgeItem.earnedCatalog.map { it.key })
        assertTrue(BadgeItem.boughtCatalog.none { it.isEarned })
    }
}
