@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.PlanAssistanceDetail
import cat.vira.android.core.data.model.PlanAssistanceLink
import cat.vira.android.core.data.model.PlanItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

/// Pla del dia.
@Serializable
data class PlanItemSyncRow(
    val id: UUID,
    val title: String,
    val notes: String?,
    val day: Instant,
    val time: Instant?,
    val status: String,
    @SerialName("completed_at") val completedAt: Instant?,
    @SerialName("reminder_offset_minutes") val reminderOffsetMinutes: Int?,
    val recurrence: String,
    @SerialName("symbol_name") val symbolName: String,
    @SerialName("sort_index") val sortIndex: Int,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: PlanItem) : this(
        model.id, model.title, model.notes, model.day, model.time, model.statusRaw, model.completedAt,
        model.reminderOffsetMinutes, model.recurrenceRaw, model.symbolName, model.sortIndex, model.createdAt,
        model.updatedAt,
    )

    /// Els camps que no viatgen (`systemReminderID`) es queden com eren.
    fun writeTo(model: PlanItem) = model.copy(
        title = title, notes = notes, day = day, time = time, statusRaw = status, completedAt = completedAt,
        reminderOffsetMinutes = reminderOffsetMinutes, recurrenceRaw = recurrence, symbolName = symbolName,
        sortIndex = sortIndex, createdAt = createdAt, updatedAt = updatedAt,
    )
}

object PlanItemSyncTable : ViraSyncTable<PlanItemSyncRow>("plan_items", ViraSyncLayer.CORE, PlanItemSyncRow.serializer()) {
    override suspend fun local(db: ViraDatabase) = db.planItems().all().map(::PlanItemSyncRow)

    override suspend fun apply(rows: List<PlanItemSyncRow>, db: ViraDatabase) {
        val existing = db.planItems().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            when {
                model == null -> row.writeTo(PlanItem(id = row.id))
                shouldOverwrite(row, model) -> row.writeTo(model)
                else -> null
            }
        }
        db.planItems().upsertRaw(changed)
    }
}

/// Passos d'una tasca partida. El seu `id` és el de la tasca.
@Serializable
data class PlanAssistanceSyncRow(
    val id: UUID,
    @SerialName("parent_plan_item_id") val parentPlanItemId: UUID?,
    @SerialName("step_order") val stepOrder: Int?,
    @SerialName("estimated_minutes") val estimatedMinutes: Int?,
    @SerialName("context_ids") val contextIds: List<String>,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: PlanAssistanceDetail) : this(
        model.id, model.parentPlanItemID, model.stepOrder, model.estimatedMinutes, model.contextIDs, model.updatedAt,
    )
}

object PlanAssistanceSyncTable :
    ViraSyncTable<PlanAssistanceSyncRow>("plan_assistance_details", ViraSyncLayer.CORE, PlanAssistanceSyncRow.serializer()) {

    /// Només les que tenen tasca: el servidor les lliga amb una clau forana.
    override suspend fun local(db: ViraDatabase): List<PlanAssistanceSyncRow> {
        val items = db.planItems().all().map { it.id }.toSet()
        return db.assistanceDetails().all().filter { it.id in items }.map(::PlanAssistanceSyncRow)
    }

    /// Es conserven les referències a tasques que encara no han arribat: la paginació pot separar
    /// pares i fills. El que ja s'ha esborrat aquí no torna.
    override suspend fun apply(rows: List<PlanAssistanceSyncRow>, db: ViraDatabase) {
        val existing = db.assistanceDetails().all().associateBy { it.id }
        val deleted = db.tombstones().recordIDs(ViraStore.TABLE_PLAN_ITEMS).toSet()
        val changed = rows.filter { it.id !in deleted }.mapNotNull { row ->
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: PlanAssistanceDetail(id = row.id)).copy(
                parentPlanItemID = row.parentPlanItemId.takeIf { it != row.id },
                stepOrder = row.stepOrder,
                estimatedMinutes = row.estimatedMinutes,
                contextIDs = row.contextIds,
                updatedAt = row.updatedAt,
            )
        }
        db.assistanceDetails().upsertRaw(changed)
    }
}

/// «Aquesta tasca va abans d'aquella».
@Serializable
data class PlanAssistanceLinkRow(
    val id: UUID,
    @SerialName("before_plan_item_id") val beforePlanItemId: UUID?,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: PlanAssistanceLink) : this(model.id, model.beforePlanItemID, model.updatedAt)
}

object PlanAssistanceLinkSyncTable :
    ViraSyncTable<PlanAssistanceLinkRow>("plan_assistance_links", ViraSyncLayer.CORE, PlanAssistanceLinkRow.serializer()) {

    override suspend fun local(db: ViraDatabase): List<PlanAssistanceLinkRow> {
        val items = db.planItems().all().map { it.id }.toSet()
        return db.assistanceLinks().all().filter { it.id in items }.map(::PlanAssistanceLinkRow)
    }

    override suspend fun apply(rows: List<PlanAssistanceLinkRow>, db: ViraDatabase) {
        val existing = db.assistanceLinks().all().associateBy { it.id }
        val deleted = db.tombstones().recordIDs(ViraStore.TABLE_PLAN_ITEMS).toSet()
        val changed = rows.filter { it.id !in deleted }.mapNotNull { row ->
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            val before = row.beforePlanItemId?.takeIf { it != row.id && it !in deleted }
            (model ?: PlanAssistanceLink(id = row.id)).copy(beforePlanItemID = before, updatedAt = row.updatedAt)
        }
        db.assistanceLinks().upsertRaw(changed)
    }
}

/// Hàbits. No s'esborren mai des de la interfície —s'arxiven—, així que no tenen làpida.
@Serializable
data class HabitSyncRow(
    val id: UUID,
    val title: String,
    val notes: String?,
    @SerialName("symbol_name") val symbolName: String,
    val weekdays: List<Int>,
    @SerialName("reminder_time") val reminderTime: Instant?,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: Habit) : this(
        model.id, model.title, model.notes, model.symbolName, model.weekdays, model.reminderTime, model.isArchived,
        model.createdAt, model.updatedAt,
    )

    fun writeTo(model: Habit) = model.copy(
        title = title, notes = notes, symbolName = symbolName, weekdays = weekdays, reminderTime = reminderTime,
        isArchived = isArchived, createdAt = createdAt, updatedAt = updatedAt,
    )
}

object HabitSyncTable : ViraSyncTable<HabitSyncRow>("habits", ViraSyncLayer.CORE, HabitSyncRow.serializer()) {
    override suspend fun local(db: ViraDatabase) = db.habits().all().map(::HabitSyncRow)

    override suspend fun apply(rows: List<HabitSyncRow>, db: ViraDatabase) {
        val existing = db.habits().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            when {
                model == null -> row.writeTo(Habit(id = row.id))
                shouldOverwrite(row, model) -> row.writeTo(model)
                else -> null
            }
        }
        db.habits().upsertRaw(changed)
    }
}

/// El registre d'un hàbit fet. Si l'hàbit no hi és, la fila es descarta: el motor sincronitza
/// `habits` abans, així que arribar aquí sense pare vol dir que s'ha esborrat.
@Serializable
data class HabitLogSyncRow(
    val id: UUID,
    @SerialName("habit_id") val habitId: UUID?,
    val day: Instant,
    @SerialName("completed_at") val completedAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: HabitLog) : this(model.id, model.habitId, model.day, model.completedAt, model.updatedAt)
}

object HabitLogSyncTable : ViraSyncTable<HabitLogSyncRow>("habit_logs", ViraSyncLayer.CORE, HabitLogSyncRow.serializer()) {
    override suspend fun local(db: ViraDatabase) = db.habitLogs().all().map(::HabitLogSyncRow)

    override suspend fun apply(rows: List<HabitLogSyncRow>, db: ViraDatabase) {
        val existing = db.habitLogs().all().associateBy { it.id }
        val habits = db.habits().all().map { it.id }.toSet()
        val changed = rows.mapNotNull { row ->
            val parent = row.habitId?.takeIf { it in habits } ?: return@mapNotNull null
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: HabitLog(id = row.id)).copy(
                habitId = parent, day = row.day, completedAt = row.completedAt, updatedAt = row.updatedAt,
            )
        }
        db.habitLogs().upsertRaw(changed)
    }
}
