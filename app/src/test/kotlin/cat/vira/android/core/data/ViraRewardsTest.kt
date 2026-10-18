package cat.vira.android.core.data

import android.app.Application
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusSession
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/// `FocusRewardTests` i la part de `ViraRewards` de `ViraEconomyTests` de l'iPhone.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraRewardsTest {

    private val t0 = Instant.ofEpochSecond(1_000_000)
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db, clock = { t0 }, zone = { ZoneOffset.UTC })
    private val rewards = ViraRewards(store, clock = { t0 }, zone = { ZoneOffset.UTC })
    private val threshold = ViraRewards.FOCUS_MINIMUM_SECONDS

    @After fun close() = db.close()

    private fun session(kind: FocusKind = FocusKind.FOCUS, seconds: Int, completed: Boolean) =
        FocusSession(plannedSeconds = seconds, actualSeconds = seconds, completed = completed, kindRaw = kind.raw)

    // Taxes

    @Test
    fun lesTaxesSonLesDocumentades() {
        assertEquals(ViraRewards.Rule(3, 5), ViraRewards.rule(CoinReason.PLAN_ITEM))
        assertEquals(ViraRewards.Rule(5, 5), ViraRewards.rule(CoinReason.HABIT))
        assertEquals(ViraRewards.Rule(10, 2), ViraRewards.rule(CoinReason.EMOTION))
        assertEquals(ViraRewards.Rule(15, 4), ViraRewards.rule(CoinReason.FOCUS))
    }

    @Test
    fun lesCompresNoGenerenRes() {
        assertNull(ViraRewards.rule(CoinReason.PURCHASE_SKIN))
        assertNull(ViraRewards.rule(CoinReason.PURCHASE_FREEZE))
        assertNull(ViraRewards.rule(CoinReason.PERSONAL_REWARD))
    }

    @Test
    fun capMotiuNoEsDeMedicacio() {
        // Si algú afegeix un motiu a `CoinReason`, aquesta prova ha de caure: comprova que no sigui
        // de medicació.
        assertEquals(10, CoinReason.entries.size)
        assertTrue(CoinReason.entries.none { it.raw.contains("medic", ignoreCase = true) })
    }

    // Sostre diari i XP

    @Test
    fun elLimitDiariAturaLesMonedesPeroNoLXp() = runBlocking {
        assertEquals(10, rewards.award(CoinReason.EMOTION, t0))
        assertEquals(10, rewards.award(CoinReason.EMOTION, t0))
        assertEquals(0, rewards.award(CoinReason.EMOTION, t0))
        val counter = store.counter()
        assertEquals(20, counter.coinBalance)
        assertEquals(30, counter.totalXP)
    }

    @Test
    fun elSostreDAhirNoComptaPerAlDAvui() = runBlocking {
        val yesterday = t0.minus(Duration.ofDays(1))
        assertEquals(10, rewards.award(CoinReason.EMOTION, yesterday))
        assertEquals(10, rewards.award(CoinReason.EMOTION, yesterday))
        assertEquals(10, rewards.award(CoinReason.EMOTION, t0))
    }

    @Test
    fun gastarBaixaElSaldoPeroNoLXpNiElTotalGuanyat() = runBlocking {
        repeat(4) { rewards.award(CoinReason.FOCUS, t0) }
        assertEquals(60, store.counter().coinBalance)
        assertEquals(60, store.counter().coinsEarnedTotal)

        assertFalse(rewards.spend(500, CoinReason.PURCHASE_SKIN, t0))
        assertEquals(60, store.counter().coinBalance)
        assertTrue(rewards.spend(50, CoinReason.PURCHASE_SKIN, t0))

        val counter = store.counter()
        assertEquals(10, counter.coinBalance)
        assertEquals(60, counter.coinsEarnedTotal)
        assertEquals(60, counter.totalXP)
        assertEquals(10, db.coinTransactions().all().sumOf { it.amount })
    }

    @Test
    fun gastarZeroONegatiuNoFaRes() = runBlocking {
        assertFalse(rewards.spend(0, CoinReason.PURCHASE_SKIN, t0))
        assertFalse(rewards.spend(-5, CoinReason.PURCHASE_SKIN, t0))
        assertTrue(db.coinTransactions().all().isEmpty())
    }

    @Test
    fun unaCompraQueFallaDinsDUnaTransaccioNoDeixaRes() = runBlocking {
        rewards.award(CoinReason.FOCUS, t0)
        runCatching {
            db.withTransaction {
                assertTrue(rewards.spend(10, CoinReason.PURCHASE_SKIN, t0))
                error("la propietat no s'ha pogut desar")
            }
        }
        assertEquals(15, store.counter().coinBalance)
        assertEquals(1, db.coinTransactions().all().size)
    }

    @Test
    fun guanyarDataElComptadorISenseSentinella() = runBlocking {
        assertEquals(ViraTime.distantPast, store.counter().updatedAt)
        rewards.award(CoinReason.HABIT, t0)
        assertEquals(t0, store.counter().updatedAt)
    }

    // Focus

    @Test
    fun justPerSotaDelLlindarNoDonaMonedes() = runBlocking {
        assertEquals(0, rewards.award(session(seconds = threshold - 1, completed = true), t0))
    }

    @Test
    fun exactamentAlLlindarDona15Monedes() = runBlocking {
        assertEquals(15, rewards.award(session(seconds = threshold, completed = true), t0))
        assertEquals(15, store.counter().coinBalance)
    }

    @Test
    fun unaSessioDeDescansNoDonaMonedes() = runBlocking {
        assertEquals(0, rewards.award(session(FocusKind.SHORT_BREAK, threshold, completed = true), t0))
    }

    @Test
    fun unaSessioInterrompudaNoDonaMonedesINoPenalitza() = runBlocking {
        store.save(db.progressCounters(), store.counter().copy(coinBalance = 50))
        assertEquals(0, rewards.award(session(seconds = 25 * 60, completed = false), t0))
        assertEquals(50, store.counter().coinBalance)
    }

    @Test
    fun elSostreDeFocusEsDe4ConcessionsAlDia() = runBlocking {
        repeat(4) { assertEquals(15, rewards.award(session(seconds = threshold, completed = true), t0)) }
        assertEquals(0, rewards.award(session(seconds = threshold, completed = true), t0))
        assertEquals(60, store.counter().coinBalance)
        assertEquals(4, rewards.grantsToday(CoinReason.FOCUS, t0))
    }

    @Test
    fun unaConcessioDAhirNoComptaPerAlSostreDAvui() = runBlocking {
        val yesterday = t0.minus(Duration.ofDays(1))
        repeat(4) { assertEquals(15, rewards.award(session(seconds = threshold, completed = true), yesterday)) }
        assertEquals(15, rewards.award(session(seconds = threshold, completed = true), t0))
    }

    @Test
    fun focusSessionsCountNomesPujaAmbSessionsDeFocusCompletades() = runBlocking {
        rewards.award(session(seconds = threshold, completed = true), t0)
        rewards.award(session(seconds = 900, completed = false), t0)
        rewards.award(session(FocusKind.SHORT_BREAK, threshold, completed = true), t0)
        assertEquals(1, store.counter().focusSessionsCount)
    }

    @Test
    fun elSaldoEsLaSumaDeLesTransaccions() = runBlocking {
        repeat(3) { rewards.award(session(seconds = threshold, completed = true), t0) }
        val total = db.coinTransactions().all().sumOf { it.amount }
        assertEquals(45, total)
        assertEquals(total, store.counter().coinBalance)
    }

    // Reparació del saldo

    @Test
    fun laReparacioNomesSuma() = runBlocking {
        db.coinTransactions().upsertRaw(listOf(
            CoinTransaction(amount = 100, reasonRaw = CoinReason.CHALLENGE.raw, createdAt = t0),
            CoinTransaction(amount = -30, reasonRaw = CoinReason.PURCHASE_SKIN.raw, createdAt = t0),
        ))
        rewards.reconcileBalanceWithLedger()
        assertEquals(70, store.counter().coinBalance)
        assertEquals(100, store.counter().coinsEarnedTotal)
        // Sense data: és una reparació de la baixada, no una acció.
        assertEquals(ViraTime.distantPast, store.counter().updatedAt)

        store.save(db.progressCounters(), store.counter().copy(coinBalance = 500))
        rewards.reconcileBalanceWithLedger()
        assertEquals(500, store.counter().coinBalance)
    }
}
