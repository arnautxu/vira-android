package cat.vira.android.features.progress.logic

import androidx.room.withTransaction
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Streak
import cat.vira.android.features.progress.logic.StreakRules.DayRequirement
import cat.vira.android.features.progress.logic.StreakRules.ReconcileOutcome
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/// Punt d'entrada **únic** per registrar que l'usuari ha fet alguna cosa (`ViraProgress.swift` i
/// `ViraStreak.swift` de l'iPhone).
///
/// Cada acció fa dues coses alhora —donar monedes i XP, i mirar si el dia ja compta per a la
/// ratxa— i escampar les dues crides per cinc pantalles garanteix que tard o d'hora una se
/// n'oblidi. Des del 2026-08-10 el dia demana **un hàbit, un registre emocional i una tasca**: si
/// una acció no passa per aquí, la que tanqui el dia pot no arribar mai a comptar-lo.
///
/// - **Cap crida des de medicació.** Ni monedes, ni XP, ni ratxa: `CoinReason` ni tan sols en té
///   el cas.
/// - Es crida només en **completar**. Desmarcar no resta res enlloc.
///
/// `onActivity` és on Notificacions engega la reprogramació dels avisos (l'iPhone ho fa des
/// d'aquí mateix): fer una cosa canvia què cal recordar, i un avís d'una cosa ja feta és Viri sense
/// mirar.
class ViraProgress(
    private val store: ViraStore,
    private val clock: () -> Instant = ViraTime::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val onActivity: suspend (Instant) -> Unit = {},
    /// Fita de ratxa (7, 30, 100) a què s'acaba d'arribar: Viri la comenta al proper avís
    /// (`ViraStreakNotifications.record(.milestone)` de l'iPhone). Ho decideix Notificacions.
    private val onMilestone: suspend (Int) -> Unit = {},
    focusMinimumSeconds: Int = ViraRewards.FOCUS_MINIMUM_SECONDS,
    /// Primer dia de la setmana dels reptes setmanals: el mateix que fa servir la pantalla, o un
    /// repte que s'ensenya llest podria caure en un altre cicle en recollir-lo.
    private val firstDayOfWeek: () -> DayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
) {
    private val db get() = store.db
    private val rewards = ViraRewards(store, clock, zone, focusMinimumSeconds)
    private fun days() = ViraDays(zone(), firstDayOfWeek())

    /// Registra una acció completada. Retorna les **ViriCoins** concedides, que és el que la
    /// interfície celebra (0 si el sostre diari ja era esgotat).
    suspend fun record(reason: CoinReason, now: Instant = clock()): Int {
        var milestone: Int? = null
        val coins = db.withTransaction {
            milestone = registerActivity(now)
            rewards.award(reason, now)
        }
        milestone?.let { onMilestone(it) }
        onActivity(now)
        return coins
    }

    /// Una sessió de concentració acabada (`FocusSessionController.finish`): monedes si toca
    /// (15 min, 4 al dia) i, si és completada, descongela Viri **sense** comptar el dia.
    suspend fun recordFocus(session: FocusSession, now: Instant = clock()): Int {
        val coins = db.withTransaction {
            val coins = rewards.award(session, now)
            if (session.completed) saveStreak(StreakRules.touch(store.streak(), now, days()))
            coins
        }
        onActivity(now)
        return coins
    }

    /// Un hàbit fet compta per al comptador acumulat d'«El que has fet». Va a part de `record`
    /// perquè a l'iPhone es fa en tancar-se la finestra de Desfer, no en tocar.
    suspend fun countHabitCompleted() = db.withTransaction {
        val counter = store.counter()
        store.save(db.progressCounters(), counter.copy(habitsCompletedCount = counter.habitsCompletedCount + 1))
    }

    /// Un registre emocional desat, per al comptador acumulat (`ViraEmotionCheckIn`).
    suspend fun countEmotionCheckIn() = db.withTransaction {
        val counter = store.counter()
        store.save(db.progressCounters(), counter.copy(emotionCheckinsCount = counter.emotionCheckinsCount + 1))
    }

    /// Posa la ratxa al dia en obrir l'app: cobreix els dies perduts amb dies guardats, o la
    /// reinicia sense drama. Ha de passar encara que l'usuari no faci res.
    suspend fun reconcileOnLaunch(now: Instant = clock()): ReconcileOutcome = db.withTransaction {
        val (streak, outcome) = StreakRules.reconcile(store.streak(), now, days())
        saveStreak(streak)
        outcome
    }

    /// Què li queda al dia per comptar. Buit vol dir que la ratxa ja hi pot avançar.
    ///
    /// Es calcula **de les dades**: desfer una acció es reflecteix sola. La tasca compta pel dia en
    /// què es marca (`completedAt`), no pel que tenia al pla.
    suspend fun missingRequirements(now: Instant = clock()): Set<DayRequirement> {
        val days = days()
        val today = days.startOfDay(now)
        val tomorrow = days.addDays(today, 1)
        fun Instant.isToday() = this >= today && this < tomorrow
        val missing = mutableSetOf<DayRequirement>()
        if (db.habitLogs().all().none { days.startOfDay(it.day) == today }) missing += DayRequirement.HABIT
        if (db.emotionEntries().all().none { it.createdAt.isToday() }) missing += DayRequirement.EMOTION
        if (db.planItems().all().none { it.completedAt?.isToday() == true }) missing += DayRequirement.TASK
        return missing
    }

    /// Compra un dia guardat més (200 ViriCoins). `false` sense tocar res si no hi ha monedes o ja
    /// s'ha arribat al sostre.
    suspend fun buyFreeze(now: Instant = clock()): Boolean = db.withTransaction {
        // El mes nou primer: amb la pantalla oberta a mitjanit de canvi de mes, els dies gratuïts
        // s'han de sumar abans de decidir si cal comprar-ne.
        val streak = StreakRules.refreshMonthlyGrant(store.streak(), now, days())
        val bought = StreakRules.canBuyFreeze(streak) &&
            rewards.spend(Streak.FREEZE_PRICE, CoinReason.PURCHASE_FREEZE, now)
        saveStreak(if (bought) streak.copy(freezesAvailable = streak.freezesAvailable + 1) else streak)
        bought
    }

    /// Recull el premi d'un repte complet i encara no recollit aquest cicle. `false` sense tocar res
    /// si encara no és complet o ja s'havia recollit: la unicitat per cicle és el que evita
    /// «farmejar-lo».
    suspend fun claim(item: ChallengeItem, now: Instant = clock()): Boolean = db.withTransaction {
        val days = days()
        val ledger = db.coinTransactions().all()
        val claims = db.challengeClaims().all()
        if (!ViraChallenges.isCompleted(item, ledger, now, days) ||
            ViraChallenges.isClaimed(item, claims, now, days)
        ) return@withTransaction false

        val counter = store.counter()
        store.save(
            db.progressCounters(),
            counter.copy(
                totalXP = counter.totalXP + item.reward,
                coinBalance = counter.coinBalance + item.reward,
                coinsEarnedTotal = counter.coinsEarnedTotal + item.reward,
            ),
        )
        store.create(db.coinTransactions(), CoinTransaction(amount = item.reward, reasonRaw = CoinReason.CHALLENGE.raw, createdAt = now))
        store.create(db.challengeClaims(), ChallengeClaim(key = item.key, cycleStart = ViraChallenges.cycleStart(item.period, now, days), claimedAt = now))
        true
    }

    /// Si toca celebrar una fita de ratxa (7 o 30 dies), en marca el senyal i en torna la clau de
    /// carta. Una sola vegada per fita.
    suspend fun takeStreakMilestone(): String? = db.withTransaction {
        val (streak, card) = StreakMilestone.checkAndMark(store.streak())
        if (card != null) saveStreak(streak)
        card
    }

    // Ratxa

    /// Registra que avui s'ha fet una acció que compta. **Sempre** descongela Viri; només fa
    /// avançar la ratxa quan les tres condicions del dia ja hi són.
    private suspend fun registerActivity(now: Instant): Int? {
        val days = days()
        val touched = StreakRules.touch(store.streak(), now, days)
        if (missingRequirements(now).isNotEmpty()) {
            saveStreak(touched)
            return null
        }
        val counted = StreakRules.countDay(touched, now, days)
        saveStreak(counted.streak)
        if (counted.isNewDay) {
            val counter = store.counter()
            store.save(db.progressCounters(), counter.copy(daysActiveCount = counter.daysActiveCount + 1))
        }
        return counted.milestone
    }

    /// Compta el dia **sense** mirar les tres condicions: per a les proves i l'eina de depuració,
    /// com `ViraStreak.countDay` de l'iPhone.
    internal suspend fun countDay(now: Instant = clock()) = db.withTransaction {
        val counted = StreakRules.countDay(store.streak(), now, days())
        saveStreak(counted.streak)
        if (counted.isNewDay) {
            val counter = store.counter()
            store.save(db.progressCounters(), counter.copy(daysActiveCount = counter.daysActiveCount + 1))
        }
    }

    /// Només es desa si ha canviat: una ratxa intacta datada d'ara trepitjaria la del servidor.
    private suspend fun saveStreak(streak: Streak) {
        if (streak != store.streak()) store.save(db.streaks(), streak)
    }
}
