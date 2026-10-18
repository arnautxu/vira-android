package cat.vira.android.core.data

import androidx.room.withTransaction
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.kind
import java.time.Instant
import java.time.ZoneId

/// Guany i despesa de ViriCoins i XP (`ViraRewards.swift` de l'iPhone).
///
/// Les taxes i els límits diaris són els de `VIRI_AND_GAMIFICATION.md` §2.2.1, literalment:
///
/// | Acció | ViriCoins | Límit diari |
/// |---|---|---|
/// | Tasca o esdeveniment completat | 3 | 5 |
/// | Hàbit completat | 5 | 5 |
/// | Registre emocional | 10 | 2 |
/// | Sessió de focus completada (≥15 min) | 15 | 4 |
/// | **Medicació** | **0 — no hi ha regla, i no n'hi pot haver** | — |
///
/// `CoinReason` no té cap cas de medicació: gamificar l'adherència és una decisió clínica.
///
/// **XP**: cap document en dona taxes. Puja sempre el que donaria l'acció, encara que el sostre de
/// monedes ja estigui esgotat: l'esforç sempre compta, el que es limita és la moneda. Mateixa
/// assumpció que l'iPhone, pendent de confirmar.
///
/// Tot passa dins de `withTransaction`, i Room les nia: es pot cridar des d'una transacció de qui
/// crida (comprar = `spend` + la propietat + equipar, tot o res).
class ViraRewards(
    private val store: ViraStore,
    private val clock: () -> Instant = ViraTime::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    /// 15 minuts. L'iPhone el baixa a 10 s només en builds de desenvolupament, per al botó de prova
    /// de la preparació del focus; aquí ho decideix qui construeix l'objecte.
    private val focusMinimumSeconds: Int = FOCUS_MINIMUM_SECONDS,
) {

    data class Rule(val coins: Int, val dailyCap: Int)

    private val db get() = store.db

    /// Quantes concessions d'aquest motiu ja s'han fet avui.
    suspend fun grantsToday(reason: CoinReason, now: Instant = clock()): Int {
        val startOfDay = now.atZone(zone()).toLocalDate().atStartOfDay(zone()).toInstant()
        return db.coinTransactions().grantsSince(reason.raw, startOfDay)
    }

    /// Concedeix el que toqui per una acció. L'XP puja sempre; les monedes, només si queda sostre
    /// diari. Retorna les **monedes** concedides, que és el que la interfície celebra.
    suspend fun award(reason: CoinReason, now: Instant = clock()): Int = db.withTransaction {
        val rule = rule(reason) ?: return@withTransaction 0
        val counter = store.counter()
        val underCap = grantsToday(reason, now) < rule.dailyCap
        if (underCap) {
            store.create(db.coinTransactions(), CoinTransaction(amount = rule.coins, reasonRaw = reason.raw, createdAt = now))
        }
        val coins = if (underCap) rule.coins else 0
        store.save(
            db.progressCounters(),
            counter.copy(
                totalXP = counter.totalXP + rule.coins,
                coinBalance = counter.coinBalance + coins,
                coinsEarnedTotal = counter.coinsEarnedTotal + coins,
            ),
        )
        coins
    }

    /// Gasta monedes. Retorna `false` i no toca res si no n'hi ha prou. El saldo és **l'únic**
    /// valor que pot baixar: `totalXP` i `coinsEarnedTotal` no es toquen mai aquí.
    suspend fun spend(amount: Int, reason: CoinReason, now: Instant = clock()): Boolean = db.withTransaction {
        if (amount <= 0) return@withTransaction false
        val counter = store.counter()
        if (counter.coinBalance < amount) return@withTransaction false
        store.create(db.coinTransactions(), CoinTransaction(amount = -amount, reasonRaw = reason.raw, createdAt = now))
        store.save(db.progressCounters(), counter.copy(coinBalance = counter.coinBalance - amount))
        true
    }

    /// Avalua una sessió acabada i, si toca, concedeix les monedes. `focusSessionsCount` puja amb
    /// qualsevol sessió de focus completada, sigui quin sigui el sostre: és un rècord de constància.
    suspend fun award(session: FocusSession, now: Instant = clock()): Int = db.withTransaction {
        if (session.kind != FocusKind.FOCUS || !session.completed) return@withTransaction 0
        val counter = store.counter()
        store.save(db.progressCounters(), counter.copy(focusSessionsCount = counter.focusSessionsCount + 1))
        // Una sessió massa curta no compta com a sessió, encara que estigui completada.
        if (session.actualSeconds < focusMinimumSeconds) return@withTransaction 0
        award(CoinReason.FOCUS, now)
    }

    /// Torna a quadrar el saldo amb el llibre major (`CoinTransaction`). Es crida en baixar
    /// transaccions: un telèfon acabat d'estrenar té el llibre sencer i el comptador buit.
    ///
    /// **Només cap amunt**: una reparació automàtica no pot treure monedes a ningú. `totalXP` no
    /// es toca perquè no té llibre major. No es data, com a l'iPhone: passa dins de la baixada.
    suspend fun reconcileBalanceWithLedger() = db.withTransaction {
        val ledger = db.coinTransactions().all()
        if (ledger.isEmpty()) return@withTransaction
        val counter = store.counter()
        val repaired = counter.copy(
            coinBalance = maxOf(counter.coinBalance, ledger.sumOf { it.amount }),
            coinsEarnedTotal = maxOf(counter.coinsEarnedTotal, ledger.sumOf { maxOf(0, it.amount) }),
        )
        if (repaired != counter) store.saveDeviceOnly(db.progressCounters(), repaired)
    }

    companion object {
        const val FOCUS_REWARD = 15
        const val FOCUS_DAILY_CAP = 4
        const val FOCUS_MINIMUM_SECONDS = 15 * 60

        /// `null` vol dir que aquell motiu no genera res: les compres.
        fun rule(reason: CoinReason): Rule? = when (reason) {
            CoinReason.PLAN_ITEM -> Rule(coins = 3, dailyCap = 5)
            CoinReason.HABIT -> Rule(coins = 5, dailyCap = 5)
            CoinReason.EMOTION -> Rule(coins = 10, dailyCap = 2)
            CoinReason.FOCUS -> Rule(coins = FOCUS_REWARD, dailyCap = FOCUS_DAILY_CAP)
            CoinReason.CHALLENGE -> Rule(coins = 50, dailyCap = 2)
            CoinReason.PURCHASE_SKIN, CoinReason.PURCHASE_FREEZE, CoinReason.PERSONAL_REWARD,
            CoinReason.PURCHASE_ACCESSORY, CoinReason.PURCHASE_BADGE -> null
        }
    }
}
