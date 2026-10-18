@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.data.model.ViraProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonObject
import java.time.Instant

// Els tres singletons. Al servidor la clau és `user_id`, no un `id` aleatori: dos telèfons del
// mateix compte han de convergir en una fila, no en dues. Per això no envien cap `id`.

@Serializable
data class ProfileSyncRow(
    @SerialName("display_name") val displayName: String,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: ViraProfile) : this(model.displayName, model.updatedAt)
}

class ProfileSyncTable(private val store: (ViraDatabase) -> ViraStore) :
    ViraSyncTable<ProfileSyncRow>("profiles", ViraSyncLayer.CORE, ProfileSyncRow.serializer(), conflictTarget = "user_id") {

    override suspend fun local(db: ViraDatabase) = db.profiles().all().map(::ProfileSyncRow)

    override suspend fun apply(rows: List<ProfileSyncRow>, db: ViraDatabase) {
        val row = rows.lastOrNull() ?: return
        val model = store(db).profile()
        if (!shouldOverwrite(row, model)) return
        db.profiles().upsertRaw(listOf(model.copy(displayName = row.displayName, updatedAt = row.updatedAt)))
    }
}

@Serializable
data class ProgressCounterSyncRow(
    @SerialName("total_xp") val totalXp: Int,
    @SerialName("coin_balance") val coinBalance: Int,
    @SerialName("coins_earned_total") val coinsEarnedTotal: Int,
    /// `null` quan la capa d'emocions no pot sortir del telèfon: no s'envia i, si arriba així, no toca
    /// el recompte local (`ViraSyncRowsEmotionCounts.kt`).
    @SerialName("emotion_checkins_count") val emotionCheckinsCount: Int?,
    @SerialName("focus_sessions_count") val focusSessionsCount: Int,
    @SerialName("habits_completed_count") val habitsCompletedCount: Int,
    @SerialName("days_active_count") val daysActiveCount: Int,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: ProgressCounter) : this(
        model.totalXP, model.coinBalance, model.coinsEarnedTotal, model.emotionCheckinsCount,
        model.focusSessionsCount, model.habitsCompletedCount, model.daysActiveCount, model.updatedAt,
    )
}

/// `afterApply`: el que arriba del compte ja es va guanyar en un altre telèfon i no s'ha de
/// celebrar aquí (`ViraCardCelebrations.absorb`). Ho connecta el fil de la Botiga.
class ProgressCounterSyncTable(
    private val store: (ViraDatabase) -> ViraStore,
    private val afterApply: suspend (ViraDatabase) -> Unit,
) : ViraSyncTable<ProgressCounterSyncRow>(
    "progress_counters", ViraSyncLayer.CORE, ProgressCounterSyncRow.serializer(), conflictTarget = "user_id",
) {
    override suspend fun local(db: ViraDatabase) = db.progressCounters().all().map(::ProgressCounterSyncRow)

    override fun withholds(scope: ViraSyncScope) = ViraEmotionTrace.withholds(scope)

    override fun preparedForUpload(rows: List<ProgressCounterSyncRow>, scope: ViraSyncScope) =
        if (!withholds(scope)) rows
        else rows.map { it.copy(emotionCheckinsCount = null, updatedAt = it.updatedAt.minus(ViraEmotionTrace.heldUploadOffset)) }

    override fun preparedAfterDownload(rows: List<ProgressCounterSyncRow>, scope: ViraSyncScope) =
        if (withholds(scope)) rows.map { it.copy(emotionCheckinsCount = null) } else rows

    /// Sense recompte, el camp **no hi és**: un `null` buidaria el del servidor.
    override fun encode(row: ProgressCounterSyncRow) = super.encode(row).let { json ->
        if (row.emotionCheckinsCount != null) json else JsonObject(json - "emotion_checkins_count")
    }

    override suspend fun apply(rows: List<ProgressCounterSyncRow>, db: ViraDatabase) {
        val row = rows.lastOrNull() ?: return
        val model = store(db).counter()
        if (!shouldOverwrite(row, model)) return
        db.progressCounters().upsertRaw(listOf(model.copy(
            totalXP = row.totalXp, coinBalance = row.coinBalance, coinsEarnedTotal = row.coinsEarnedTotal,
            emotionCheckinsCount = row.emotionCheckinsCount ?: model.emotionCheckinsCount,
            focusSessionsCount = row.focusSessionsCount,
            habitsCompletedCount = row.habitsCompletedCount, daysActiveCount = row.daysActiveCount,
            // Sense recompte, el model es queda el local, que el servidor no té: ha de quedar per sobre
            // de la fila baixada fins i tot després de restar-li el mil·lisegon de la pujada retinguda,
            // o en obrir la porta la pujada sencera hi empataria i `keep_last_write` la descartaria.
            updatedAt = if (row.emotionCheckinsCount != null) row.updatedAt
                else maxOf(ViraTime.now(), row.updatedAt.plus(ViraEmotionTrace.heldUploadOffset.multipliedBy(2))),
        )))
        // El comptador del servidor pot ser més vell que el llibre major que ja hi ha al telèfon (una
        // passada anterior el va reparar i ara la baixada del llibre no porta res de nou).
        CoinTransactionSyncTable.reconcileBalanceWithLedger(db, store(db))
        afterApply(db)
    }
}

/// Ratxa, amb tot l'estat dels dies guardats.
@Serializable
data class StreakSyncRow(
    @SerialName("current_days") val currentDays: Int,
    @SerialName("best_days") val bestDays: Int,
    @SerialName("last_active_day") val lastActiveDay: Instant?,
    @SerialName("freezes_available") val freezesAvailable: Int,
    @SerialName("freezes_used_this_month") val freezesUsedThisMonth: Int,
    @SerialName("last_grant_month") val lastGrantMonth: Int,
    @SerialName("is_frozen") val isFrozen: Boolean,
    @SerialName("protected_days") val protectedDays: List<Instant>,
    /// Els dies amb activitat: el que pinta el calendari de la ratxa.
    @SerialName("active_days") val activeDays: List<Instant>,
    @SerialName("is_visible") val isVisible: Boolean,
    @SerialName("has_celebrated_week_streak") val hasCelebratedWeekStreak: Boolean,
    @SerialName("has_celebrated_month_streak") val hasCelebratedMonthStreak: Boolean,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: Streak) : this(
        model.currentDays, model.bestDays, model.lastActiveDay, model.freezesAvailable, model.freezesUsedThisMonth,
        model.lastGrantMonth, model.isFrozen, model.protectedDays, model.activeDays, model.isVisible,
        model.hasCelebratedWeekStreak, model.hasCelebratedMonthStreak, model.updatedAt,
    )
}

class StreakSyncTable(
    private val store: (ViraDatabase) -> ViraStore,
    private val afterApply: suspend (ViraDatabase) -> Unit,
) : ViraSyncTable<StreakSyncRow>("streaks", ViraSyncLayer.CORE, StreakSyncRow.serializer(), conflictTarget = "user_id") {

    override suspend fun local(db: ViraDatabase) = db.streaks().all().map(::StreakSyncRow)

    override suspend fun apply(rows: List<StreakSyncRow>, db: ViraDatabase) {
        val row = rows.lastOrNull() ?: return
        val model = store(db).streak()
        if (!shouldOverwrite(row, model)) {
            // El d'aquí és més nou i es queda, però el que només creix (dies, rècord, celebracions) es
            // suma igualment: si no, la propera pujada esborraria la història que només sabia el servidor.
            val union = model.copy(
                bestDays = maxOf(model.bestDays, row.bestDays),
                activeDays = (model.activeDays + row.activeDays).toSortedSet().toList(),
                hasCelebratedWeekStreak = model.hasCelebratedWeekStreak || row.hasCelebratedWeekStreak,
                hasCelebratedMonthStreak = model.hasCelebratedMonthStreak || row.hasCelebratedMonthStreak,
            )
            if (union != model) {
                db.streaks().upsertRaw(listOf(union.copy(updatedAt = maxOf(ViraTime.now(), model.updatedAt))))
                afterApply(db)
            }
            return
        }
        val merged = model.copy(
            currentDays = row.currentDays,
            // El rècord no baixa mai, ni tan sols per una fila més nova d'un telèfon que en sabia menys.
            bestDays = maxOf(model.bestDays, row.bestDays),
            lastActiveDay = row.lastActiveDay,
            freezesAvailable = row.freezesAvailable,
            freezesUsedThisMonth = row.freezesUsedThisMonth,
            lastGrantMonth = row.lastGrantMonth,
            isFrozen = row.isFrozen,
            protectedDays = row.protectedDays,
            // Unió, no substitució: cada telèfon pot conèixer dies que l'altre no ha vist mai.
            activeDays = (model.activeDays + row.activeDays).toSortedSet().toList(),
            isVisible = row.isVisible,
            // Les celebracions ja mostrades no es des-mostren.
            hasCelebratedWeekStreak = model.hasCelebratedWeekStreak || row.hasCelebratedWeekStreak,
            hasCelebratedMonthStreak = model.hasCelebratedMonthStreak || row.hasCelebratedMonthStreak,
            updatedAt = row.updatedAt,
        )
        // Si la unió hi ha afegit alguna cosa que el servidor no té, és un canvi d'aquest telèfon i es
        // data d'ara: amb la data del servidor no tornaria a pujar mai i els altres no el veurien.
        val learnedSomething = merged.bestDays != row.bestDays || merged.activeDays != row.activeDays.toSortedSet().toList() ||
            merged.hasCelebratedWeekStreak != row.hasCelebratedWeekStreak ||
            merged.hasCelebratedMonthStreak != row.hasCelebratedMonthStreak
        // Mai abans de la fila del servidor: si el rellotge de l'altre telèfon va avançat, el servidor
        // rebutjaria la pujada.
        val stamp = maxOf(ViraTime.now(), row.updatedAt.plusNanos(1_000))
        db.streaks().upsertRaw(listOf(if (learnedSomething) merged.copy(updatedAt = stamp) else merged))
        afterApply(db)
    }
}
