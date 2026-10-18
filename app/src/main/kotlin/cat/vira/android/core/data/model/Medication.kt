package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Tractament (`Medication` de l'iPhone). Capa `medication`: no surt del telèfon sense
/// l'interruptor **i** el permís del tutor (`ViraSyncScope`).
///
/// Cap camp d'adherència ni cap comptador: `DATA_MODEL.md` §2.7 ho prohibeix.
/// `weekdays` amb la numeració de l'iPhone (1 = diumenge), com `Habit.weekdays`.
@Entity(tableName = "Medication")
data class Medication(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val name: String = "",
    val dose: String? = null,
    val formRaw: String = MedicationForm.TABLET.raw,
    val colorIndex: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant = ViraTime.now(),
    val endedAt: Instant? = null,
    val hasReminder: Boolean = true,
    val timesInMinutes: List<Int> = emptyList(),
    val weekdays: List<Int> = emptyList(),
) : ViraSyncable<Medication> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

val Medication.form: MedicationForm get() = MedicationForm.from(formRaw)

enum class MedicationForm(val raw: String) {
    TABLET("tablet"), CAPSULE("capsule"), SYRUP("syrup"), DROPS("drops"), PATCH("patch"), OTHER("other");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: TABLET
    }
}

/// Presa registrada. Esborrar el tractament se n'emporta les preses.
@Entity(
    tableName = "MedicationIntake",
    foreignKeys = [ForeignKey(entity = Medication::class, parentColumns = ["id"], childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("medicationId"), Index("scheduledAt")],
)
data class MedicationIntake(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val scheduledAt: Instant = ViraTime.now(),
    val statusRaw: String = IntakeStatus.PENDING.raw,
    val loggedAt: Instant? = null,
    /// La relació `medication` de SwiftData.
    val medicationId: UUID? = null,
) : ViraSyncable<MedicationIntake> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

val MedicationIntake.status: IntakeStatus get() = IntakeStatus.from(statusRaw)

enum class IntakeStatus(val raw: String) {
    PENDING("pending"), TAKEN("taken"), SKIPPED("skipped"), LATER("later");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: PENDING
    }
}
