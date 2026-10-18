package cat.vira.android.features.shop

import android.app.Application
import cat.vira.android.core.data.model.ProgressCounter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// `ViraBadgesTests` de l'iPhone: què es pot comprar, què es guanya i com es penja.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraBadgesTest {

    private val f = ShopFixture()

    @After fun close() = f.close()

    @Test
    fun unaMedallaGuanyadaNoEsPotComprar() = runBlocking {
        f.give(100_000)
        val earned = BadgeItem.earnedCatalog[0]
        assertNull(earned.price)
        assertFalse(f.badges.buy(earned))
        assertFalse(f.badges.isOwned(earned.key))
    }

    @Test
    fun comprarUnBadgeElPenjaTotSol() = runBlocking {
        val item = BadgeItem.boughtCatalog[0]
        f.give(item.price ?: 0)
        assertTrue(f.badges.buy(item))
        assertTrue(f.badges.isOwned(item.key))
        assertTrue(f.badges.isPinned(item.key))
    }

    @Test
    fun senseMonedesNoEsCompraINoEsPenja() = runBlocking {
        val item = BadgeItem.boughtCatalog[3]
        f.give((item.price ?: 0) - 1)
        assertFalse(f.badges.buy(item))
        assertFalse(f.badges.isPinned(item.key))
    }

    @Test
    fun elMateixBadgeNoEsCompraDuesVegades() = runBlocking {
        val item = BadgeItem.boughtCatalog[0]
        val price = item.price ?: 0
        f.give(price * 2)
        assertTrue(f.badges.buy(item))
        assertFalse(f.badges.buy(item))
        assertEquals(price, f.balance())
    }

    @Test
    fun elPrestatgeTeSostreICauElMesAntic() = runBlocking {
        val keys = BadgeItem.catalog.take(ViraBadges.SLOTS + 1).map { it.key }
        keys.forEach { f.badges.pin(it) }
        val pinned = f.badges.pinnedKeys()
        assertEquals(ViraBadges.SLOTS, pinned.size)
        assertFalse(keys.first() in pinned)
        assertTrue(keys.last() in pinned)
    }

    @Test
    fun penjarDuesVegadesNoElPosaDosCops() = runBlocking {
        val key = BadgeItem.catalog[0].key
        f.badges.pin(key)
        f.badges.pin(key)
        assertEquals(listOf(key), f.badges.pinnedKeys())
    }

    @Test
    fun despenjarElTreu() = runBlocking {
        val key = BadgeItem.catalog[0].key
        f.badges.toggle(key)
        assertTrue(f.badges.isPinned(key))
        f.badges.toggle(key)
        assertFalse(f.badges.isPinned(key))
    }

    @Test
    fun unBadgeGuanyatNomesEsDisponibleAmbLaMedalla() = runBlocking {
        val item = requireNotNull(BadgeItem.item("first_checkin"))
        assertFalse(f.badges.isAvailable(item, ProgressCounter(), null))
        assertTrue(f.badges.isAvailable(item, ProgressCounter(emotionCheckinsCount = 1), null))
    }

    @Test
    fun capBadgeComprableCostaTantComUnaSkin() {
        for (item in BadgeItem.boughtCatalog) assertTrue((item.price ?: 0) < SkinTier.Common.price)
    }
}
