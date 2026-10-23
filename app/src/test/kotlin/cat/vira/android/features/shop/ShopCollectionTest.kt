package cat.vira.android.features.shop

import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/// `ViraShopPushTests` i `ViraShopGameTests` de l'iPhone: col·lecció, següents objectius, ritme,
/// avís d'objectiu i el vestidor.
class ShopCollectionTest {

    private val catalog = ShopCollection.catalog(emptySet())
    private val now = Instant.parse("2026-10-23T18:00:00Z")
    private val utc = ZoneOffset.UTC

    private fun item(key: String) = requireNotNull(SkinItem.item(key)) { "falta $key" }

    @Test
    fun laColleccioComptaLesQueTensIUnaRetiradaNomesSiEsTeva() {
        val retired = SkinItem.catalog.first { it.isRetired && it.artIsAvailable }
        val withRetired = ShopCollection.catalog(setOf(retired.key))
        assertEquals(catalog.size + 1, withRetired.size)
        assertEquals(2 to withRetired.size, ShopCollection.progress(withRetired, setOf(retired.key, "pirate")))
    }

    @Test
    fun despresDeComprarTresObjectiusComAMoltCadascunDUnaRaresa() {
        val goals = ShopCollection.nextGoals(catalog, setOf("pirate"))
        assertEquals(3, goals.size)
        assertEquals(goals.size, goals.map { it.tier }.toSet().size)
        assertFalse(goals.any { it.key == "pirate" })
        assertTrue(goals.none { it.isRetired })
    }

    @Test
    fun ambQuatreRaresesObertesHiHaLesDuesMesAPropILaMesAlta() {
        assertEquals(listOf(SkinTier.Common, SkinTier.Special, SkinTier.Legendary), ShopCollection.nextGoals(catalog, emptySet()).map { it.tier })
    }

    @Test
    fun unaRaresaCompletaNoTeProposta() {
        val commons = catalog.filter { it.tier == SkinTier.Common }.map { it.key }.toSet()
        assertTrue(ShopCollection.nextGoals(catalog, commons).none { it.tier == SkinTier.Common })
        assertTrue(ShopCollection.nextGoals(catalog, catalog.map { it.key }.toSet()).isEmpty())
    }

    private fun tx(amount: Int, reason: CoinReason, daysAgo: Long) =
        CoinTransaction(amount = amount, reasonRaw = reason.raw, createdAt = now.minus(daysAgo, ChronoUnit.DAYS))

    @Test
    fun elRitmeEsLaMitjanaDeCatorzeDiesComptantElsDiesSenseMonedes() {
        val earned = (0L until 7L).map { tx(40, CoinReason.HABIT, it) }
        assertEquals(280.0 / 14.0, requireNotNull(ShopCollection.dailyPace(earned, now, utc)), 1e-9)
    }

    @Test
    fun senseProuDiesAmbMonedesNoHiHaRitme() {
        val earned = listOf(tx(500, CoinReason.CHALLENGE, 0), tx(500, CoinReason.CHALLENGE, 1))
        assertNull(ShopCollection.dailyPace(earned, now, utc))
    }

    @Test
    fun lesCompresILesMonedesDeFaMesDeCatorzeDiesNoCompten() {
        val all = listOf(
            tx(30, CoinReason.HABIT, 0), tx(30, CoinReason.FOCUS, 1), tx(30, CoinReason.EMOTION, 2),
            tx(-500, CoinReason.PURCHASE_SKIN, 1), tx(900, CoinReason.HABIT, 20),
        )
        assertEquals(90.0 / 14.0, requireNotNull(ShopCollection.dailyPace(all, now, utc)), 1e-9)
    }

    @Test
    fun elsDiesSArrodoneixenAmuntINoEsDiuResSiJaTHiArribesOSeriaMassaLluny() {
        assertEquals(4, ShopCollection.daysToReach(100, 30.0))
        assertEquals(1, ShopCollection.daysToReach(10, 30.0))
        assertNull(ShopCollection.daysToReach(0, 30.0))
        assertNull(ShopCollection.daysToReach(100, null))
        assertNull(ShopCollection.daysToReach(3_000, 10.0))
    }

    @Test
    fun lAvisSurtQuanLesMonedesCreuenElPreuDelTeuObjectiu() {
        val dragon = item("dragon")
        assertEquals("dragon", ShopCollection.goalReached("dragon", dragon.price - 10, dragon.price + 5, emptySet(), "")?.key)
    }

    @Test
    fun lAvisNoSurtSenseObjectiuSenseCreuarRepetitOJaComprat() {
        val price = item("dragon").price
        val below = price - 10
        val above = price + 5
        fun reached(wish: String = "dragon", from: Int = below, to: Int = above, owned: Set<String> = emptySet(), announced: String = "") =
            ShopCollection.goalReached(wish, from, to, owned, announced) != null
        assertFalse(reached(wish = ""))
        assertFalse(reached(from = above, to = above + 10))
        assertFalse(reached(from = below, to = below + 5))
        assertFalse(reached(announced = "dragon"))
        assertFalse(reached(owned = setOf("dragon")))
    }

    @Test
    fun laDestacadaEsLaQueHasMarcatAmbLaVull() {
        val featured = requireNotNull(ShopCollection.featured(catalog, emptySet(), "dragon"))
        assertEquals("dragon", featured.first.key)
        assertTrue(featured.second)
    }

    @Test
    fun senseObjectiuOAmbLObjectiuJaCompratTornaALaSeguent() {
        val next = requireNotNull(SkinItem.next(catalog, emptySet()))
        val none = requireNotNull(ShopCollection.featured(catalog, emptySet(), ""))
        assertEquals(next.key, none.first.key)
        assertFalse(none.second)
        val bought = requireNotNull(ShopCollection.featured(catalog, setOf("dragon"), "dragon"))
        assertNotEquals("dragon", bought.first.key)
    }

    @Test
    fun unaSkinRetiradaNoPotSerLObjectiu() {
        val retired = SkinItem.catalog.first { it.isRetired }
        assertNotEquals(true, ShopCollection.featured(SkinItem.catalog, emptySet(), retired.key)?.second)
    }

    @Test
    fun comMesRaraMesSacsejadesDeCortina() {
        val shakes = SkinTier.entries.map { it.curtainShakes }
        assertEquals(shakes.sorted(), shakes)
        assertEquals(shakes.size, shakes.toSet().size)
        assertTrue(shakes.all { it > 0 })
    }

    @Test
    fun cadaRaresaTeLaSevaFraseDeViri() {
        assertEquals(SkinTier.entries.size, SkinTier.entries.map { it.revealLineRes }.toSet().size)
        assertNotNull(SkinTier.Legendary.revealLineRes)
    }
}
