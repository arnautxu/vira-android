package cat.vira.android.features.shop

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.model.ProgressCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// `CardCelebrationTests` de l'iPhone: el que baixa del compte no és una fita d'ara.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraCardCelebrationsTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("cards-test", Context.MODE_PRIVATE).also { it.edit().clear().commit() }
    private val celebrations = ViraCardCelebrations(prefs)

    @Test
    fun absorbirDonaPerVistElQueJaEstaGuanyatIResMes() {
        celebrations.absorb(ProgressCounter(emotionCheckinsCount = 1, totalXP = 100), null)
        val seen = celebrations.seenKeys
        assertTrue("first_checkin" in seen)
        assertTrue("xp_100" in seen)
        assertFalse("encara no és seva: absorbir no regala res", "xp_1000" in seen)
        assertTrue(celebrations.isSeeded)
    }

    @Test
    fun elQueBaixaDelCompteNoObreCapCelebracio() {
        val synced = ProgressCounter(emotionCheckinsCount = 50, focusSessionsCount = 30, totalXP = 1000)
        celebrations.absorb(synced, null)
        assertTrue(celebrations.pending(synced, null).isEmpty())
        // Una fita nova de debò, després, sí que surt.
        val later = synced.copy(habitsCompletedCount = 10)
        assertEquals(setOf("habits_1", "habits_5", "habits_10"), celebrations.pending(later, null).map { it.key }.toSet())
    }

    @Test
    fun marcarNeUnaNoNEsborraCapAltra() {
        celebrations.markAsSeen("first_checkin")
        celebrations.markAsSeen("xp_100")
        celebrations.markAsSeen("first_checkin")
        assertEquals(setOf("first_checkin", "xp_100"), celebrations.seenKeys)
    }
}
