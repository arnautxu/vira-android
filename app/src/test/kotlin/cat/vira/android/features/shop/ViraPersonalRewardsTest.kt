package cat.vira.android.features.shop

import android.app.Application
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.reason
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

/// «Els meus premis» (`PersonalRewardsView`, `PersonalRewardEditor` de l'iPhone).
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraPersonalRewardsTest {

    private val f = ShopFixture()

    @After fun close() = f.close()

    @Test
    fun unTitolBuitNoEsDesa() = runBlocking {
        assertNull(f.personal.create("   ", 100))
        assertTrue(f.db.personalRewards().all().isEmpty())
    }

    @Test
    fun elCostEsQuedaDinsDelSostre() = runBlocking {
        assertEquals(3_000, f.personal.create("Gelat", 99_999)?.cost)
        assertEquals(10, f.personal.create("Sèrie", 0)?.cost)
        assertEquals("Jugar 20 minuts", f.personal.create("  Jugar 20 minuts ", 100)?.title)
    }

    @Test
    fun bescanviarGastaICompta() = runBlocking {
        val reward = requireNotNull(f.personal.create("Gelat", 300))
        f.give(500)
        assertTrue(f.personal.redeem(reward))
        assertEquals(200, f.balance())
        assertEquals(1, f.db.personalRewards().byId(reward.id)?.redeemedCount)
        val reasons = f.db.coinTransactions().all().map { it.reason }
        assertTrue(CoinReason.PERSONAL_REWARD in reasons)
    }

    @Test
    fun senseMonedesNoEsBescanviaINoCanviaRes() = runBlocking {
        val reward = requireNotNull(f.personal.create("Gelat", 300))
        f.give(299)
        assertFalse(f.personal.redeem(reward))
        assertEquals(299, f.balance())
        assertEquals(0, f.db.personalRewards().byId(reward.id)?.redeemedCount)
    }

    @Test
    fun arxivarNoEsborra() = runBlocking {
        val reward = requireNotNull(f.personal.create("Gelat", 300))
        f.personal.archive(reward)
        val stored = f.db.personalRewards().byId(reward.id)
        assertTrue(stored?.isArchived == true)
    }
}
