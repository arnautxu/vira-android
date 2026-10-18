package cat.vira.android.features.shop

import android.app.Application
import cat.vira.android.R
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

/// `ViraSkinShopPresentationTests` i la part de la Botiga de `ViraEconomyTests` de l'iPhone.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraSkinsTest {

    private val f = ShopFixture()

    @After fun close() = f.close()

    private fun item(key: String) = requireNotNull(SkinItem.item(key)) { "falta $key" }

    @Test
    fun conservaLesClausExistentsSenseRepetir() {
        val expected = setOf("chef", "diver", "surfer", "pharaoh", "explorer", "astronaut", "wizard", "zombie", "mummy", "hacker", "gladiator")
        val keys = SkinItem.catalog.map { it.key }
        assertTrue(keys.toSet().containsAll(expected))
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun elsQuatreGraonsTenenPreusDinsDelSostre() {
        assertEquals(500, SkinTier.Common.price)
        assertEquals(1_500, SkinTier.Special.price)
        assertEquals(2_200, SkinTier.Epic.price)
        assertEquals(3_000, SkinTier.Legendary.price)
        assertEquals(3_000, SkinTier.entries.maxOf { it.price })
        assertEquals(3_000, item("pharaoh").price)
        assertEquals(3_000, item("wizard").price)
    }

    @Test
    fun lesPecesDe02ViriTenenArtINomPropi() {
        for (key in listOf("zombie", "mummy", "hacker", "gladiator", "tennis", "hula", "firefighter", "disco", "mime", "philosopher", "saint-george", "crystal")) {
            val skin = item(key)
            assertTrue("falta l'art de $key", skin.artIsAvailable)
            assertTrue(skin.price <= 3_000)
        }
    }

    @Test
    fun unaSkinSenseArtNoGastaMonedesNiCreaPropietat() = runBlocking {
        f.give(5_000)
        val missing = SkinItem("missing-art", null, R.string.skin_chef, SkinTier.Common)
        assertFalse(missing.artIsAvailable)
        assertFalse(f.skins.buy(missing))
        assertEquals(5_000, f.balance())
        assertFalse(f.skins.isOwned(missing.key))
    }

    @Test
    fun comprarUnaSkinLaDeixaEquipadaINomesUnaAlhora() = runBlocking {
        f.give(5_000)
        assertTrue(f.skins.buy(item("chef")))
        assertEquals("chef", f.skins.equipped()?.key)
        assertTrue(f.skins.buy(item("hacker")))
        assertEquals("hacker", f.skins.equipped()?.key)
        assertEquals(1, f.skins.owned().count { it.isEquipped })

        // El Viri de sempre sempre és una opció.
        f.skins.equip(null)
        assertNull(f.skins.equipped())
        f.skins.equip("chef")
        assertEquals("chef", f.skins.equipped()?.key)
    }

    @Test
    fun noEsPotComprarDuesVegadesLaMateixaSkin() = runBlocking {
        f.give(5_000)
        assertTrue(f.skins.buy(item("chef")))
        assertFalse(f.skins.buy(item("chef")))
        assertEquals(4_500, f.balance())
    }

    @Test
    fun senseMonedesNoEsCompraRes() = runBlocking {
        f.give(499)
        assertFalse(f.skins.buy(item("chef")))
        assertEquals(499, f.balance())
        assertTrue(f.skins.owned().isEmpty())
    }

    @Test
    fun elCatalegNoPortaCapIpDeTercers() {
        val banned = listOf("minecraft", "barca", "barça", "fcb", "lego", "pokemon", "nintendo")
        for (skin in SkinItem.catalog) for (word in banned) assertFalse(skin.key.lowercase().contains(word))
    }

    @Test
    fun laSeguentEsLaMesBarataQueNoTens() {
        val catalog = SkinItem.catalog.filter { it.artIsAvailable }
        val first = SkinItem.next(catalog, emptySet())
        assertEquals(SkinTier.Common, first?.tier)
        assertNull(SkinItem.next(catalog, catalog.map { it.key }.toSet()))
    }
}
