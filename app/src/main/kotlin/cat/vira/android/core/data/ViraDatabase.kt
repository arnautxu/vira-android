package cat.vira.android.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

/// Les dades locals (`ViraModelContainer` de l'iPhone): les mateixes 21 taules que l'esquema V3 de
/// SwiftData, amb els mateixos noms de taula i de camp.
///
/// ## Privacitat
/// - El fitxer viu al directori privat de l'app i **no entra a la còpia de seguretat de Google**
///   (`allowBackup="false"` al manifest). És l'equivalent de la protecció de fitxer de l'iPhone.
/// - Cap altre procés l'obre: els widgets, quan n'hi hagi, llegiran una fotografia sense emocions ni
///   medicació, igual que a l'iPhone (`ViraWidgetStore`).
///
/// ## Versions
/// `version = 1` és la primera d'Android i no té res a veure amb el V3 de l'iPhone. Cap canvi
/// d'esquema sense una `Migration`: esborrar les dades d'algú per una columna nova no és una opció.
@Database(
    version = 1,
    exportSchema = false,
    entities = [
        PlanItem::class, PlanAssistanceDetail::class, PlanAssistanceLink::class, AssistanceLocalNote::class,
        Habit::class, HabitLog::class, Medication::class, MedicationIntake::class, EmotionEntry::class,
        FocusProfile::class, FocusSession::class, ProgressCounter::class, Streak::class,
        CoinTransaction::class, PersonalReward::class, SkinOwnership::class, AccessoryOwnership::class,
        BadgeOwnership::class, ChallengeClaim::class, ViraProfile::class, SyncTombstone::class,
    ],
)
@TypeConverters(ViraConverters::class)
abstract class ViraDatabase : RoomDatabase() {
    abstract fun planItems(): PlanItemDao
    abstract fun assistanceDetails(): PlanAssistanceDetailDao
    abstract fun assistanceLinks(): PlanAssistanceLinkDao
    abstract fun assistanceNotes(): AssistanceLocalNoteDao
    abstract fun habits(): HabitDao
    abstract fun habitLogs(): HabitLogDao
    abstract fun medications(): MedicationDao
    abstract fun medicationIntakes(): MedicationIntakeDao
    abstract fun emotionEntries(): EmotionEntryDao
    abstract fun focusProfiles(): FocusProfileDao
    abstract fun focusSessions(): FocusSessionDao
    abstract fun progressCounters(): ProgressCounterDao
    abstract fun streaks(): StreakDao
    abstract fun coinTransactions(): CoinTransactionDao
    abstract fun personalRewards(): PersonalRewardDao
    abstract fun skinOwnerships(): SkinOwnershipDao
    abstract fun accessoryOwnerships(): AccessoryOwnershipDao
    abstract fun badgeOwnerships(): BadgeOwnershipDao
    abstract fun challengeClaims(): ChallengeClaimDao
    abstract fun profiles(): ViraProfileDao
    abstract fun tombstones(): SyncTombstoneDao

    companion object {
        const val FILE_NAME = "vira.db"

        /// La base de dades de l'app. Se'n crea una per procés, a `ViraApp`; no és un singleton
        /// global perquè les proves en puguin fer una en memòria sense arrossegar estat.
        fun open(context: Context): ViraDatabase =
            Room.databaseBuilder(context.applicationContext, ViraDatabase::class.java, FILE_NAME).build()

        /// En memòria, per a proves i previsualitzacions.
        fun inMemory(context: Context): ViraDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, ViraDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
