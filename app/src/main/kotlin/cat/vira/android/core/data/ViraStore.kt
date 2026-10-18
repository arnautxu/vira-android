package cat.vira.android.core.data

import androidx.room.withTransaction
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.data.model.SyncTombstone
import cat.vira.android.core.data.model.ViraProfile
import cat.vira.android.core.data.model.ViraSyncable
import java.time.Instant
import java.time.ZoneId

/// Per on escriuen les pantalles. Data el que es desa i deixa làpida del que s'esborra.
///
/// És la meitat Android de `ViraSyncStamp` i `ViraSync.delete` de l'iPhone. Allà el segell es posa
/// sol en desar el context; aquí Room no té aquest moment, i per això **cap pantalla escriu amb el
/// DAO directament**: un canvi desat sense datar no peta, simplement no viatja mai.
///
/// La regla del segell és la de l'iPhone (2026-08-13):
/// - **`create`** (un objecte nou) es data amb l'hora d'ara, **excepte** si porta el sentinella
///   `ViraTime.distantPast`: és el creador dient «això és un marcador de posició, no una acció».
/// - **`save`** (un canvi) es data sempre, i això aixeca el sentinella.
class ViraStore(
    val db: ViraDatabase,
    private val clock: () -> Instant = ViraTime::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {

    suspend fun <T : ViraSyncable<T>> create(dao: ViraRowDao<T>, model: T): T {
        val stamped = ViraSyncStamp.inserted(model, clock())
        dao.upsertRaw(listOf(stamped))
        return stamped
    }

    suspend fun <T : ViraSyncable<T>> save(dao: ViraRowDao<T>, model: T): T {
        val stamped = ViraSyncStamp.changed(model, clock())
        dao.upsertRaw(listOf(stamped))
        return stamped
    }

    // Singletons

    /// El perfil. Si no n'hi ha, en crea un amb el sentinella: no ha de trepitjar el del compte.
    suspend fun profile(): ViraProfile = db.withTransaction {
        db.profiles().all().firstOrNull()
            ?: ViraProfile(updatedAt = ViraTime.distantPast).also { db.profiles().upsertRaw(listOf(it)) }
    }

    /// Els comptadors (`ViraRewards.counter`). Mateixa regla del sentinella.
    suspend fun counter(): ProgressCounter = db.withTransaction {
        db.progressCounters().all().firstOrNull()
            ?: ProgressCounter(updatedAt = ViraTime.distantPast).also { db.progressCounters().upsertRaw(listOf(it)) }
    }

    /// La ratxa (`ViraStreak.current`). Neix amb el mes d'ara com a mes de les concessions.
    suspend fun streak(): Streak = db.withTransaction {
        db.streaks().all().firstOrNull()
            ?: Streak(updatedAt = ViraTime.distantPast, lastGrantMonth = monthKey(clock(), zone()))
                .also { db.streaks().upsertRaw(listOf(it)) }
    }

    // Esborrats que viatgen

    /// Esborra una tasca i les seves extensions, i en deixa làpida (`ViraSync.delete` de l'iPhone).
    ///
    /// Els passos que en penjaven sobreviuen: només se'ls treu la relació. Les dependències que hi
    /// apuntaven, igual. La nota de represa queda òrfena, com a l'iPhone.
    suspend fun delete(item: PlanItem) = db.withTransaction {
        val now = clock()
        db.assistanceLinks().all().forEach { link ->
            when {
                link.id == item.id -> db.assistanceLinks().deleteRaw(listOf(link))
                link.beforePlanItemID == item.id ->
                    db.assistanceLinks().upsertRaw(listOf(link.copy(beforePlanItemID = null, updatedAt = now)))
            }
        }
        db.assistanceNotes().all().filter { it.taskID == item.id }.forEach { note ->
            db.assistanceNotes().upsertRaw(listOf(note.copy(taskID = null, calendarEventID = null, updatedAt = now)))
        }
        db.assistanceDetails().all().forEach { detail ->
            when {
                // El servidor l'esborra sol amb la tasca (clau forana en cascada).
                detail.id == item.id -> db.assistanceDetails().deleteRaw(listOf(detail))
                detail.parentPlanItemID == item.id -> db.assistanceDetails().upsertRaw(
                    listOf(detail.copy(parentPlanItemID = null, stepOrder = null, updatedAt = now)),
                )
            }
        }
        bury(TABLE_PLAN_ITEMS, item.id)
        db.planItems().deleteRaw(listOf(item))
    }

    suspend fun delete(log: HabitLog) = db.withTransaction {
        bury("habit_logs", log.id)
        db.habitLogs().deleteRaw(listOf(log))
    }

    suspend fun delete(entry: EmotionEntry) = db.withTransaction {
        bury("emotion_entries", entry.id)
        db.emotionEntries().deleteRaw(listOf(entry))
    }

    suspend fun delete(intake: MedicationIntake) = db.withTransaction {
        bury("medication_intakes", intake.id)
        db.medicationIntakes().deleteRaw(listOf(intake))
    }

    suspend fun delete(reward: PersonalReward) = db.withTransaction {
        bury("personal_rewards", reward.id)
        db.personalRewards().deleteRaw(listOf(reward))
    }

    private suspend fun bury(table: String, id: java.util.UUID) {
        db.tombstones().upsertRaw(listOf(SyncTombstone(table = table, recordID = id, deletedAt = clock())))
    }

    companion object {
        const val TABLE_PLAN_ITEMS = "plan_items"

        /// `any * 12 + mes`, com `ViraStreak.monthKey` de l'iPhone.
        fun monthKey(instant: Instant, zone: ZoneId): Int {
            val date = instant.atZone(zone)
            return date.year * 12 + date.monthValue
        }
    }
}

/// Les dues regles del segell, separades de Room perquè es puguin provar soles.
object ViraSyncStamp {

    fun <T : ViraSyncable<T>> inserted(model: T, now: Instant): T =
        if (model.updatedAt == ViraTime.distantPast) model else model.stamped(now)

    fun <T : ViraSyncable<T>> changed(model: T, now: Instant): T = model.stamped(now)
}
