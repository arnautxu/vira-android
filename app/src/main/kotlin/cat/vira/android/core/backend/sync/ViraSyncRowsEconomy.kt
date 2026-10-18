@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.AccessoryOwnership
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.SkinOwnership
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

/// Llibre major de ViriCoins. Només s'hi afegeix; res no es reescriu.
@Serializable
data class CoinTransactionSyncRow(
    val id: UUID,
    val amount: Int,
    val reason: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: CoinTransaction) : this(model.id, model.amount, model.reasonRaw, model.createdAt, model.updatedAt)
}

class CoinTransactionSyncTable(private val store: (ViraDatabase) -> ViraStore) :
    ViraSyncTable<CoinTransactionSyncRow>("coin_transactions", ViraSyncLayer.CORE, CoinTransactionSyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.coinTransactions().all().map(::CoinTransactionSyncRow)

    override suspend fun apply(rows: List<CoinTransactionSyncRow>, db: ViraDatabase) {
        val existing = db.coinTransactions().all().map { it.id }.toSet()
        db.coinTransactions().upsertRaw(rows.filter { it.id !in existing }.map {
            CoinTransaction(id = it.id, amount = it.amount, reasonRaw = it.reason, createdAt = it.createdAt, updatedAt = it.updatedAt)
        })
        reconcileBalanceWithLedger(db, store(db))
    }

    companion object {
        /// El saldo viu al comptador, una altra taula que durant un temps no es va pujar mai: un
        /// telèfon nou es baixava totes les transaccions i es quedava el número a zero
        /// (`ViraRewards.reconcileBalanceWithLedger`). **Només suma**: una reparació automàtica no pot
        /// treure monedes a ningú.
        suspend fun reconcileBalanceWithLedger(db: ViraDatabase, store: ViraStore) {
            val ledger = db.coinTransactions().all()
            if (ledger.isEmpty()) return
            val balance = ledger.sumOf { it.amount }
            val earned = ledger.sumOf { maxOf(0, it.amount) }
            val counter = store.counter()
            val repaired = counter.copy(
                coinBalance = maxOf(counter.coinBalance, balance),
                coinsEarnedTotal = maxOf(counter.coinsEarnedTotal, earned),
            )
            // Sense segellar, com a l'iPhone: passa dins de la baixada, amb el segellador en pausa.
            if (repaired != counter) db.progressCounters().upsertRaw(listOf(repaired))
        }
    }
}

@Serializable
data class PersonalRewardSyncRow(
    val id: UUID,
    val title: String,
    val cost: Int,
    @SerialName("symbol_name") val symbolName: String,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("redeemed_count") val redeemedCount: Int,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: PersonalReward) : this(
        model.id, model.title, model.cost, model.symbolName, model.isArchived, model.redeemedCount, model.createdAt,
        model.updatedAt,
    )
}

object PersonalRewardSyncTable :
    ViraSyncTable<PersonalRewardSyncRow>("personal_rewards", ViraSyncLayer.CORE, PersonalRewardSyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.personalRewards().all().map(::PersonalRewardSyncRow)

    override suspend fun apply(rows: List<PersonalRewardSyncRow>, db: ViraDatabase) {
        val existing = db.personalRewards().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: PersonalReward(id = row.id)).copy(
                title = row.title, cost = row.cost, symbolName = row.symbolName, isArchived = row.isArchived,
                redeemedCount = row.redeemedCount, createdAt = row.createdAt, updatedAt = row.updatedAt,
            )
        }
        db.personalRewards().upsertRaw(changed)
    }
}

/// Skins comprades. Clau natural `(compte, skin)`.
@Serializable
data class SkinOwnershipSyncRow(
    val key: String,
    @SerialName("purchased_at") val purchasedAt: Instant,
    @SerialName("is_equipped") val isEquipped: Boolean,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: SkinOwnership) : this(model.key, model.purchasedAt, model.isEquipped, model.updatedAt)
}

object SkinOwnershipSyncTable : ViraSyncTable<SkinOwnershipSyncRow>(
    "skin_ownerships", ViraSyncLayer.CORE, SkinOwnershipSyncRow.serializer(), conflictTarget = "user_id,key",
) {
    override suspend fun local(db: ViraDatabase) = db.skinOwnerships().all().map(::SkinOwnershipSyncRow)

    override suspend fun apply(rows: List<SkinOwnershipSyncRow>, db: ViraDatabase) {
        val existing = db.skinOwnerships().all().associateBy { it.key }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.key]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: SkinOwnership(key = row.key)).copy(
                purchasedAt = row.purchasedAt, isEquipped = row.isEquipped, updatedAt = row.updatedAt,
            )
        }
        db.skinOwnerships().upsertRaw(changed)
    }
}

/// Accessoris comprats. Clau natural `(compte, accessori)`, calcada de les skins.
@Serializable
data class AccessoryOwnershipSyncRow(
    val key: String,
    @SerialName("purchased_at") val purchasedAt: Instant,
    @SerialName("is_equipped") val isEquipped: Boolean,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: AccessoryOwnership) : this(model.key, model.purchasedAt, model.isEquipped, model.updatedAt)
}

object AccessoryOwnershipSyncTable : ViraSyncTable<AccessoryOwnershipSyncRow>(
    "accessory_ownerships", ViraSyncLayer.CORE, AccessoryOwnershipSyncRow.serializer(), conflictTarget = "user_id,key",
) {
    override suspend fun local(db: ViraDatabase) = db.accessoryOwnerships().all().map(::AccessoryOwnershipSyncRow)

    override suspend fun apply(rows: List<AccessoryOwnershipSyncRow>, db: ViraDatabase) {
        val existing = db.accessoryOwnerships().all().associateBy { it.key }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.key]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: AccessoryOwnership(key = row.key)).copy(
                purchasedAt = row.purchasedAt, isEquipped = row.isEquipped, updatedAt = row.updatedAt,
            )
        }
        db.accessoryOwnerships().upsertRaw(changed)
    }
}

/// Reptes ja cobrats. Clau natural `(compte, repte, cicle)`; un cop cobrat no canvia.
@Serializable
data class ChallengeClaimSyncRow(
    val key: String,
    @SerialName("cycle_start") val cycleStart: Instant,
    @SerialName("claimed_at") val claimedAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: ChallengeClaim) : this(model.key, model.cycleStart, model.claimedAt, model.updatedAt)
}

object ChallengeClaimSyncTable : ViraSyncTable<ChallengeClaimSyncRow>(
    "challenge_claims", ViraSyncLayer.CORE, ChallengeClaimSyncRow.serializer(), conflictTarget = "user_id,key,cycle_start",
) {
    override suspend fun local(db: ViraDatabase) = db.challengeClaims().all().map(::ChallengeClaimSyncRow)

    override suspend fun apply(rows: List<ChallengeClaimSyncRow>, db: ViraDatabase) {
        val existing = db.challengeClaims().all().map { it.key to it.cycleStart }.toSet()
        db.challengeClaims().upsertRaw(rows.filter { (it.key to it.cycleStart) !in existing }.map {
            ChallengeClaim(key = it.key, cycleStart = it.cycleStart, claimedAt = it.claimedAt, updatedAt = it.updatedAt)
        })
    }
}
