package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Comptadors de progrés. Singleton: n'hi ha un per telèfon (`ViraSingletons.counter`).
///
/// `lastCelebratedLevel` és del telèfon i no viatja: és «aquest nivell ja s'ha celebrat aquí».
@Entity(tableName = "ProgressCounter")
data class ProgressCounter(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val totalXP: Int = 0,
    val coinBalance: Int = 0,
    val coinsEarnedTotal: Int = 0,
    val emotionCheckinsCount: Int = 0,
    val focusSessionsCount: Int = 0,
    val habitsCompletedCount: Int = 0,
    val daysActiveCount: Int = 0,
    val lastCelebratedLevel: Int = 0,
) : ViraSyncable<ProgressCounter> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Ratxa. Singleton (`ViraSingletons.streak`), amb tot l'estat dels dies guardats: enviar-ne
/// només una part faria que canviar de telèfon regalés o robés dies.
///
/// `lastGrantMonth` és `any * 12 + mes`, com `ViraStreak.monthKey` de l'iPhone.
@Entity(tableName = "Streak")
data class Streak(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val currentDays: Int = 0,
    val bestDays: Int = 0,
    val lastActiveDay: Instant? = null,
    val freezesAvailable: Int = MONTHLY_FREE_GRANT,
    val freezesUsedThisMonth: Int = 0,
    val lastGrantMonth: Int = 0,
    val isFrozen: Boolean = false,
    val protectedDays: List<Instant> = emptyList(),
    val activeDays: List<Instant> = emptyList(),
    val isVisible: Boolean = true,
    val hasCelebratedWeekStreak: Boolean = false,
    val hasCelebratedMonthStreak: Boolean = false,
) : ViraSyncable<Streak> {
    override fun stamped(at: Instant) = copy(updatedAt = at)

    companion object {
        const val MONTHLY_FREE_GRANT = 2
        const val MAX_FREEZES = 4
        const val FREEZE_PRICE = 200
    }
}

/// Llibre major de ViriCoins. Només s'hi afegeix; res no es reescriu.
@Entity(tableName = "CoinTransaction", indices = [Index("createdAt")])
data class CoinTransaction(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val amount: Int = 0,
    val reasonRaw: String = CoinReason.HABIT.raw,
    val createdAt: Instant = ViraTime.now(),
) : ViraSyncable<CoinTransaction> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

val CoinTransaction.reason: CoinReason get() = CoinReason.from(reasonRaw)

/// Mai cap motiu lligat a medicació (`VIRI_AND_GAMIFICATION.md` §2.2.1).
enum class CoinReason(val raw: String) {
    PLAN_ITEM("planItem"), HABIT("habit"), EMOTION("emotion"), FOCUS("focus"), CHALLENGE("challenge"),
    PURCHASE_SKIN("purchaseSkin"), PURCHASE_FREEZE("purchaseFreeze"), PERSONAL_REWARD("personalReward"),
    PURCHASE_ACCESSORY("purchaseAccessory"), PURCHASE_BADGE("purchaseBadge");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: HABIT
    }
}

/// Premi personal que l'usuari es posa a si mateix.
@Entity(tableName = "PersonalReward")
data class PersonalReward(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val title: String = "",
    val cost: Int = 100,
    val symbolName: String = "gift",
    val isArchived: Boolean = false,
    val createdAt: Instant = ViraTime.now(),
    val redeemedCount: Int = 0,
) : ViraSyncable<PersonalReward> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}
