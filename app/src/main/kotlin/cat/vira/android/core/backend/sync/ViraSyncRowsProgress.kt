@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.data.model.ViraProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
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
    @SerialName("emotion_checkins_count") val emotionCheckinsCount: Int,
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

    override suspend fun apply(rows: List<ProgressCounterSyncRow>, db: ViraDatabase) {
        val row = rows.lastOrNull() ?: return
        val model = store(db).counter()
        if (!shouldOverwrite(row, model)) return
        db.progressCounters().upsertRaw(listOf(model.copy(
            totalXP = row.totalXp, coinBalance = row.coinBalance, coinsEarnedTotal = row.coinsEarnedTotal,
            emotionCheckinsCount = row.emotionCheckinsCount, focusSessionsCount = row.focusSessionsCount,
            habitsCompletedCount = row.habitsCompletedCount, daysActiveCount = row.daysActiveCount,
            updatedAt = row.updatedAt,
        )))
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
        if (!shouldOverwrite(row, model)) return
        db.streaks().upsertRaw(listOf(model.copy(
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
        )))
        afterApply(db)
    }
}
