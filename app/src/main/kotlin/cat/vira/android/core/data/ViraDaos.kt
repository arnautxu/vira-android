package cat.vira.android.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import cat.vira.android.core.data.model.AccessoryOwnership
import cat.vira.android.core.data.model.AssistanceLocalNote
import cat.vira.android.core.data.model.BadgeOwnership
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.PlanAssistanceDetail
import cat.vira.android.core.data.model.PlanAssistanceLink
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.SkinOwnership
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.data.model.SyncTombstone
import cat.vira.android.core.data.model.ViraProfile
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/// Escriptura en cru, **sense segellar**.
///
/// Les pantalles no la fan servir: escriuen amb `ViraStore`, que posa `updatedAt`. Aquí només hi
/// passa el que arriba del servidor (que ja porta la seva data i no s'ha de tornar a datar, o es
/// reenviaria tot seguit) i l'esborrat total.
interface ViraRowDao<T> {
    @Upsert suspend fun upsertRaw(rows: List<T>)
    @Delete suspend fun deleteRaw(rows: List<T>)
}

// Cada taula té les mateixes quatre lectures de base. Les consultes que una pantalla necessiti de
// més (per dia, per estat…) es demanen al fil de Dades i s'afegeixen aquí.

@Dao interface PlanItemDao : ViraRowDao<PlanItem> {
    @Query("SELECT * FROM PlanItem") suspend fun all(): List<PlanItem>
    @Query("SELECT * FROM PlanItem ORDER BY day, sortIndex") fun observeAll(): Flow<List<PlanItem>>
    @Query("SELECT * FROM PlanItem WHERE id = :id") suspend fun byId(id: UUID): PlanItem?
    @Query("DELETE FROM PlanItem") suspend fun deleteAll()
}

@Dao interface PlanAssistanceDetailDao : ViraRowDao<PlanAssistanceDetail> {
    @Query("SELECT * FROM PlanAssistanceDetail") suspend fun all(): List<PlanAssistanceDetail>
    @Query("SELECT * FROM PlanAssistanceDetail") fun observeAll(): Flow<List<PlanAssistanceDetail>>
    @Query("SELECT * FROM PlanAssistanceDetail WHERE id = :id") suspend fun byId(id: UUID): PlanAssistanceDetail?
    @Query("DELETE FROM PlanAssistanceDetail") suspend fun deleteAll()
}

@Dao interface PlanAssistanceLinkDao : ViraRowDao<PlanAssistanceLink> {
    @Query("SELECT * FROM PlanAssistanceLink") suspend fun all(): List<PlanAssistanceLink>
    @Query("SELECT * FROM PlanAssistanceLink") fun observeAll(): Flow<List<PlanAssistanceLink>>
    @Query("SELECT * FROM PlanAssistanceLink WHERE id = :id") suspend fun byId(id: UUID): PlanAssistanceLink?
    @Query("DELETE FROM PlanAssistanceLink") suspend fun deleteAll()
}

@Dao interface AssistanceLocalNoteDao : ViraRowDao<AssistanceLocalNote> {
    @Query("SELECT * FROM AssistanceLocalNote") suspend fun all(): List<AssistanceLocalNote>
    @Query("SELECT * FROM AssistanceLocalNote") fun observeAll(): Flow<List<AssistanceLocalNote>>
    @Query("SELECT * FROM AssistanceLocalNote WHERE id = :id") suspend fun byId(id: UUID): AssistanceLocalNote?
    @Query("DELETE FROM AssistanceLocalNote") suspend fun deleteAll()
}

@Dao interface HabitDao : ViraRowDao<Habit> {
    @Query("SELECT * FROM Habit") suspend fun all(): List<Habit>
    @Query("SELECT * FROM Habit ORDER BY createdAt") fun observeAll(): Flow<List<Habit>>
    @Query("SELECT * FROM Habit WHERE id = :id") suspend fun byId(id: UUID): Habit?
    @Query("DELETE FROM Habit") suspend fun deleteAll()
}

@Dao interface HabitLogDao : ViraRowDao<HabitLog> {
    @Query("SELECT * FROM HabitLog") suspend fun all(): List<HabitLog>
    @Query("SELECT * FROM HabitLog") fun observeAll(): Flow<List<HabitLog>>
    @Query("SELECT * FROM HabitLog WHERE id = :id") suspend fun byId(id: UUID): HabitLog?
    @Query("DELETE FROM HabitLog") suspend fun deleteAll()
}

@Dao interface MedicationDao : ViraRowDao<Medication> {
    @Query("SELECT * FROM Medication") suspend fun all(): List<Medication>
    @Query("SELECT * FROM Medication ORDER BY createdAt") fun observeAll(): Flow<List<Medication>>
    @Query("SELECT * FROM Medication WHERE id = :id") suspend fun byId(id: UUID): Medication?
    @Query("DELETE FROM Medication") suspend fun deleteAll()
}

@Dao interface MedicationIntakeDao : ViraRowDao<MedicationIntake> {
    @Query("SELECT * FROM MedicationIntake") suspend fun all(): List<MedicationIntake>
    @Query("SELECT * FROM MedicationIntake ORDER BY scheduledAt") fun observeAll(): Flow<List<MedicationIntake>>
    @Query("SELECT * FROM MedicationIntake WHERE id = :id") suspend fun byId(id: UUID): MedicationIntake?
    @Query("DELETE FROM MedicationIntake") suspend fun deleteAll()
}

@Dao interface EmotionEntryDao : ViraRowDao<EmotionEntry> {
    @Query("SELECT * FROM EmotionEntry") suspend fun all(): List<EmotionEntry>
    @Query("SELECT * FROM EmotionEntry ORDER BY createdAt DESC") fun observeAll(): Flow<List<EmotionEntry>>
    @Query("SELECT * FROM EmotionEntry WHERE id = :id") suspend fun byId(id: UUID): EmotionEntry?
    @Query("DELETE FROM EmotionEntry") suspend fun deleteAll()
}

@Dao interface FocusProfileDao : ViraRowDao<FocusProfile> {
    @Query("SELECT * FROM FocusProfile") suspend fun all(): List<FocusProfile>
    @Query("SELECT * FROM FocusProfile ORDER BY createdAt") fun observeAll(): Flow<List<FocusProfile>>
    @Query("SELECT * FROM FocusProfile WHERE id = :id") suspend fun byId(id: UUID): FocusProfile?
    @Query("DELETE FROM FocusProfile") suspend fun deleteAll()
}

@Dao interface FocusSessionDao : ViraRowDao<FocusSession> {
    @Query("SELECT * FROM FocusSession") suspend fun all(): List<FocusSession>
    @Query("SELECT * FROM FocusSession ORDER BY startedAt DESC") fun observeAll(): Flow<List<FocusSession>>
    @Query("SELECT * FROM FocusSession WHERE id = :id") suspend fun byId(id: UUID): FocusSession?
    @Query("DELETE FROM FocusSession") suspend fun deleteAll()
}

@Dao interface ProgressCounterDao : ViraRowDao<ProgressCounter> {
    @Query("SELECT * FROM ProgressCounter") suspend fun all(): List<ProgressCounter>
    @Query("SELECT * FROM ProgressCounter LIMIT 1") fun observe(): Flow<ProgressCounter?>
    @Query("DELETE FROM ProgressCounter") suspend fun deleteAll()
}

@Dao interface StreakDao : ViraRowDao<Streak> {
    @Query("SELECT * FROM Streak") suspend fun all(): List<Streak>
    @Query("SELECT * FROM Streak LIMIT 1") fun observe(): Flow<Streak?>
    @Query("DELETE FROM Streak") suspend fun deleteAll()
}

@Dao interface CoinTransactionDao : ViraRowDao<CoinTransaction> {
    @Query("SELECT * FROM CoinTransaction") suspend fun all(): List<CoinTransaction>
    @Query("SELECT * FROM CoinTransaction ORDER BY createdAt DESC") fun observeAll(): Flow<List<CoinTransaction>>
    @Query("DELETE FROM CoinTransaction") suspend fun deleteAll()
}

@Dao interface PersonalRewardDao : ViraRowDao<PersonalReward> {
    @Query("SELECT * FROM PersonalReward") suspend fun all(): List<PersonalReward>
    @Query("SELECT * FROM PersonalReward ORDER BY createdAt") fun observeAll(): Flow<List<PersonalReward>>
    @Query("SELECT * FROM PersonalReward WHERE id = :id") suspend fun byId(id: UUID): PersonalReward?
    @Query("DELETE FROM PersonalReward") suspend fun deleteAll()
}

@Dao interface SkinOwnershipDao : ViraRowDao<SkinOwnership> {
    @Query("SELECT * FROM SkinOwnership") suspend fun all(): List<SkinOwnership>
    @Query("SELECT * FROM SkinOwnership") fun observeAll(): Flow<List<SkinOwnership>>
    @Query("DELETE FROM SkinOwnership") suspend fun deleteAll()
}

@Dao interface AccessoryOwnershipDao : ViraRowDao<AccessoryOwnership> {
    @Query("SELECT * FROM AccessoryOwnership") suspend fun all(): List<AccessoryOwnership>
    @Query("SELECT * FROM AccessoryOwnership") fun observeAll(): Flow<List<AccessoryOwnership>>
    @Query("DELETE FROM AccessoryOwnership") suspend fun deleteAll()
}

@Dao interface BadgeOwnershipDao : ViraRowDao<BadgeOwnership> {
    @Query("SELECT * FROM BadgeOwnership") suspend fun all(): List<BadgeOwnership>
    @Query("SELECT * FROM BadgeOwnership") fun observeAll(): Flow<List<BadgeOwnership>>
    @Query("DELETE FROM BadgeOwnership") suspend fun deleteAll()
}

@Dao interface ChallengeClaimDao : ViraRowDao<ChallengeClaim> {
    @Query("SELECT * FROM ChallengeClaim") suspend fun all(): List<ChallengeClaim>
    @Query("SELECT * FROM ChallengeClaim") fun observeAll(): Flow<List<ChallengeClaim>>
    @Query("DELETE FROM ChallengeClaim") suspend fun deleteAll()
}

@Dao interface ViraProfileDao : ViraRowDao<ViraProfile> {
    @Query("SELECT * FROM ViraProfile") suspend fun all(): List<ViraProfile>
    @Query("SELECT * FROM ViraProfile LIMIT 1") fun observe(): Flow<ViraProfile?>
    @Query("DELETE FROM ViraProfile") suspend fun deleteAll()
}

@Dao interface SyncTombstoneDao : ViraRowDao<SyncTombstone> {
    @Query("SELECT * FROM SyncTombstone") suspend fun all(): List<SyncTombstone>
    @Query("SELECT recordID FROM SyncTombstone WHERE `table` = :table") suspend fun recordIDs(table: String): List<UUID>
    @Query("DELETE FROM SyncTombstone") suspend fun deleteAll()
}
