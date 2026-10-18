@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

// Les tres taules sensibles. Cap d'elles surt del telèfon sense l'interruptor **i** el permís del
// tutor (`ViraSyncScope`): el motor les salta abans de llegir-les.

/// Registres emocionals. Capa `EMOTIONS`.
@Serializable
data class EmotionEntrySyncRow(
    val id: UUID,
    val emotion: String,
    val intensity: Int?,
    val note: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: EmotionEntry) : this(
        model.id, model.emotionRaw, model.intensityRaw, model.note, model.createdAt, model.updatedAt,
    )
}

object EmotionEntrySyncTable :
    ViraSyncTable<EmotionEntrySyncRow>("emotion_entries", ViraSyncLayer.EMOTIONS, EmotionEntrySyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.emotionEntries().all().map(::EmotionEntrySyncRow)

    override suspend fun apply(rows: List<EmotionEntrySyncRow>, db: ViraDatabase) {
        val existing = db.emotionEntries().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: EmotionEntry(id = row.id)).copy(
                emotionRaw = row.emotion, intensityRaw = row.intensity, note = row.note,
                createdAt = row.createdAt, updatedAt = row.updatedAt,
            )
        }
        db.emotionEntries().upsertRaw(changed)
    }
}

/// Tractaments. Capa `MEDICATION`. Cap camp d'adherència ni cap comptador (`DATA_MODEL.md` §2.7).
@Serializable
data class MedicationSyncRow(
    val id: UUID,
    val name: String,
    val dose: String?,
    val form: String,
    @SerialName("color_index") val colorIndex: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("has_reminder") val hasReminder: Boolean,
    @SerialName("times_in_minutes") val timesInMinutes: List<Int>,
    val weekdays: List<Int>,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("ended_at") val endedAt: Instant?,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: Medication) : this(
        model.id, model.name, model.dose, model.formRaw, model.colorIndex, model.isActive, model.hasReminder,
        model.timesInMinutes, model.weekdays, model.createdAt, model.endedAt, model.updatedAt,
    )

    fun writeTo(model: Medication) = model.copy(
        name = name, dose = dose, formRaw = form, colorIndex = colorIndex, isActive = isActive,
        hasReminder = hasReminder, timesInMinutes = timesInMinutes, weekdays = weekdays, createdAt = createdAt,
        endedAt = endedAt, updatedAt = updatedAt,
    )
}

object MedicationSyncTable :
    ViraSyncTable<MedicationSyncRow>("medications", ViraSyncLayer.MEDICATION, MedicationSyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.medications().all().map(::MedicationSyncRow)

    override suspend fun apply(rows: List<MedicationSyncRow>, db: ViraDatabase) {
        val existing = db.medications().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            when {
                model == null -> row.writeTo(Medication(id = row.id))
                shouldOverwrite(row, model) -> row.writeTo(model)
                else -> null
            }
        }
        db.medications().upsertRaw(changed)
    }
}

/// Preses. Com els registres d'hàbit, sense tractament pare es descarten.
@Serializable
data class MedicationIntakeSyncRow(
    val id: UUID,
    @SerialName("medication_id") val medicationId: UUID?,
    @SerialName("scheduled_at") val scheduledAt: Instant,
    val status: String,
    @SerialName("logged_at") val loggedAt: Instant?,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: MedicationIntake) : this(
        model.id, model.medicationId, model.scheduledAt, model.statusRaw, model.loggedAt, model.updatedAt,
    )
}

object MedicationIntakeSyncTable : ViraSyncTable<MedicationIntakeSyncRow>(
    "medication_intakes", ViraSyncLayer.MEDICATION, MedicationIntakeSyncRow.serializer(),
) {
    override suspend fun local(db: ViraDatabase) = db.medicationIntakes().all().map(::MedicationIntakeSyncRow)

    override suspend fun apply(rows: List<MedicationIntakeSyncRow>, db: ViraDatabase) {
        val existing = db.medicationIntakes().all().associateBy { it.id }
        val medications = db.medications().all().map { it.id }.toSet()
        val changed = rows.mapNotNull { row ->
            val parent = row.medicationId?.takeIf { it in medications } ?: return@mapNotNull null
            val model = existing[row.id]
            if (model != null && !shouldOverwrite(row, model)) return@mapNotNull null
            (model ?: MedicationIntake(id = row.id)).copy(
                medicationId = parent, scheduledAt = row.scheduledAt, statusRaw = row.status,
                loggedAt = row.loggedAt, updatedAt = row.updatedAt,
            )
        }
        db.medicationIntakes().upsertRaw(changed)
    }
}
