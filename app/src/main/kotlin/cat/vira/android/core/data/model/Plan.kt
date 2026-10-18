package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Element del pla del dia (`PlanItem` de l'iPhone, `DATA_MODEL.md` §2.2).
///
/// `day` és **l'inici del dia en hora local**, com un instant: és el que desa l'iPhone
/// (`Calendar.current.startOfDay`) i el que viatja a `plan_items.day`. Un `LocalDate` aquí faria
/// que el mateix dia es llegís diferent als dos telèfons.
///
/// `systemReminderID` és de l'iPhone (Recordatoris) i no viatja: a Android no hi ha importació.
@Entity(tableName = "PlanItem", indices = [Index("day")])
data class PlanItem(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val title: String = "",
    val notes: String? = null,
    val day: Instant = ViraTime.now(),
    val time: Instant? = null,
    val statusRaw: String = PlanItemStatus.PENDING.raw,
    val completedAt: Instant? = null,
    val reminderOffsetMinutes: Int? = null,
    val recurrenceRaw: String = PlanItemRecurrence.NONE.raw,
    val symbolName: String = "picto-flag",
    val sortIndex: Int = 0,
    val createdAt: Instant = ViraTime.now(),
    val systemReminderID: String? = null,
) : ViraSyncable<PlanItem> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

// Fora de la classe a posta: Room no ha de veure-les com a columnes.
val PlanItem.status: PlanItemStatus get() = PlanItemStatus.from(statusRaw)
val PlanItem.recurrence: PlanItemRecurrence get() = PlanItemRecurrence.from(recurrenceRaw)

enum class PlanItemStatus(val raw: String) {
    PENDING("pending"), DONE("done"), DISMISSED("dismissed");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: PENDING
    }
}

/// Els valors en cru són els de l'iPhone: viatgen tal qual a `plan_items.recurrence`.
enum class PlanItemRecurrence(val raw: String) {
    NONE("none"), WEEKLY("weekly"), MONTHLY("monthly"),
    DAILY_UNTIL_WEEK_END("dailyUntilWeekEnd"), DAILY_UNTIL_MONTH_END("dailyUntilMonthEnd");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

/// Metadades d'assistència d'una tasca (passos d'una tasca partida). El seu `id` és el de la
/// tasca: és una extensió seva, no un registre a part.
@Entity(tableName = "PlanAssistanceDetail")
data class PlanAssistanceDetail(
    @PrimaryKey override val id: UUID,
    val parentPlanItemID: UUID? = null,
    val stepOrder: Int? = null,
    val estimatedMinutes: Int? = null,
    /// Subconjunt de `phone`, `outside`, `home` (el servidor ho comprova).
    val contextIDs: List<String> = emptyList(),
    override val updatedAt: Instant = ViraTime.now(),
) : ViraSyncable<PlanAssistanceDetail> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// «Aquesta tasca va abans d'aquella». El seu `id` és el de la tasca.
@Entity(tableName = "PlanAssistanceLink")
data class PlanAssistanceLink(
    @PrimaryKey override val id: UUID,
    val beforePlanItemID: UUID? = null,
    override val updatedAt: Instant = ViraTime.now(),
) : ViraSyncable<PlanAssistanceLink> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Nota de represa d'una tasca. **Local**: no surt mai del telèfon (no és a cap taula del servidor).
@Entity(tableName = "AssistanceLocalNote")
data class AssistanceLocalNote(
    @PrimaryKey val id: UUID,
    val taskID: UUID? = null,
    val text: String = "",
    val calendarEventID: String? = null,
    val updatedAt: Instant = ViraTime.now(),
)
