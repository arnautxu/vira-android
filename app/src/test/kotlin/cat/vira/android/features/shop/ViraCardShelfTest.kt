package cat.vira.android.features.shop

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// `ViraCardShelfTests` de l'iPhone: la carta de dalt de l'àlbum i la seva «Nova».
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraCardShelfTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("shelf-test", Context.MODE_PRIVATE).also { it.edit().clear().commit() }
    private val shelf = ViraCardShelf(prefs)

    private fun cards(vararg keys: String) = keys.mapNotNull(CardItem::item)

    private fun celebrated(keys: String) = prefs.edit().putString(ViraCardCelebrations.SEEN_KEY, keys).commit()

    @Test
    fun aDaltHiVaLUltimaGuanyadaNoLUltimaDelCataleg() {
        celebrated("xp_100,first_checkin,focus_1")
        assertEquals("focus_1", shelf.latest(cards("first_checkin", "focus_1", "xp_100"))?.key)
    }

    @Test
    fun senseOrdreGuardatADaltHiVaLaMesRara() {
        assertEquals("week_streak", shelf.latest(cards("first_checkin", "week_streak", "xp_100"))?.key)
        assertNull(shelf.latest(emptyList()))
    }

    @Test
    fun elPrimerCopElQueJaEraTeuNoPortaNovaILaSeguentSiFinsQueLObres() {
        celebrated("first_checkin")
        shelf.seedIfNeeded()
        var owned = cards("first_checkin")
        assertFalse(shelf.isNew(owned[0], owned))

        celebrated("first_checkin,focus_1")
        owned = cards("first_checkin", "focus_1")
        val focus = owned[1]
        assertTrue(shelf.isNew(focus, owned))
        assertFalse("només la de dalt porta «Nova»", shelf.isNew(owned[0], owned))

        shelf.markOpened(focus.key)
        assertFalse(shelf.isNew(focus, owned))
    }

    @Test
    fun esGuardaAmbElPrefixDeLesCartes() {
        assertTrue(ViraCardShelf.OPENED_KEY.startsWith("cards."))
        assertTrue(cat.vira.android.core.data.ViraDataEraser.ownPrefixes.contains("cards."))
    }
}
