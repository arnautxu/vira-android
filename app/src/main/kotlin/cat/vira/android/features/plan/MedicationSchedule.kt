package cat.vira.android.features.plan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/// Una pauta toca aquell dia? Conclosa, mai (`Medication.isScheduled` de l'iPhone).
fun Medication.isScheduled(day: LocalDate): Boolean =
    isActive && (weekdays.isEmpty() || PlanDay.iosWeekday(day) in weekdays)

/// Les preses d'un dia a partir de la pauta (`MedicationSchedule` de l'iPhone).
///
/// ## L'espai és la pauta, el registre és opcional
/// Una hora de la pauta **sempre** genera un espai per al dia, tingui registre o no. El
/// `MedicationIntake` només existeix quan l'usuari ha dit alguna cosa (presa, saltada, més tard):
/// un pendent no és una dada de l'usuari, és l'absència d'una.
///
/// ## Cap càlcul d'adherència
/// Aquí no hi ha, ni hi pot haver, cap percentatge de compliment ni cap ratxa
/// (`PRIVACY_AND_SAFETY.md` §7).
object MedicationSchedule {

    /// Una hora concreta d'un dia, amb el registre que hi correspongui si n'hi ha cap.
    data class Slot(val medication: Medication, val scheduledAt: Instant, val intake: MedicationIntake?) {
        val id: String get() = "${medication.id}-${scheduledAt.epochSecond}"
        val status: IntakeStatus get() = intake?.status ?: IntakeStatus.PENDING
    }

    /// Els espais d'una pauta per a un dia, en ordre d'hora. Buit si la pauta no toca aquell dia o
    /// ja està conclosa, **tampoc per als dies passats**: el que ja s'havia registrat no es perd.
    fun slots(
        medication: Medication,
        intakes: List<MedicationIntake>,
        day: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Slot> {
        if (!medication.isScheduled(day)) return emptyList()
        val own = intakes.filter { it.medicationId == medication.id }
        return medication.timesInMinutes.sorted().map { minutes ->
            // L'hora de rellotge, no "inici del dia + minuts": el dia que canvia l'hora en té 23 o 25.
            val time = PlanDay.at(day, minutes / 60 % 24, minutes % 60, zone)
            Slot(medication, time, intake(own, time))
        }
    }

    /// Els espais de totes les pautes d'un dia, barrejats i ordenats per hora.
    fun slots(
        medications: List<Medication>,
        intakes: List<MedicationIntake>,
        day: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Slot> = medications.flatMap { slots(it, intakes, day, zone) }.sortedBy { it.scheduledAt }

    /// El registre d'una hora concreta, si existeix. L'aparellament és **al minut**: l'hora exacta
    /// faria que dos càlculs del mateix espai no s'aparellessin per una fracció de segon.
    fun intake(intakes: List<MedicationIntake>, scheduledAt: Instant): MedicationIntake? {
        val minute = scheduledAt.truncatedTo(ChronoUnit.MINUTES)
        return intakes.firstOrNull { it.scheduledAt.truncatedTo(ChronoUnit.MINUTES) == minute }
    }
}

/// Registrar una presa (`ViraMedication` de l'iPhone).
///
/// **Cap crida a `ViraProgress`.** Ni monedes, ni XP, ni ratxa: gamificar l'adherència
/// farmacològica és una decisió clínica, no de disseny.
object ViraMedication {

    /// El que cal fer a la base de dades per deixar un espai amb un estat.
    sealed interface Change {
        data class Create(val intake: MedicationIntake) : Change
        data class Save(val intake: MedicationIntake) : Change
        data class Delete(val intake: MedicationIntake) : Change
        data object Nothing : Change
    }

    /// Tornar a triar el mateix estat el desmarca: una presa registrada per error no pot
    /// necessitar un menú per desfer-se.
    fun record(status: IntakeStatus, slot: MedicationSchedule.Slot, now: Instant = ViraTime.now()): Change {
        val existing = slot.intake
        if (existing != null) {
            return if (existing.status == status) Change.Delete(existing)
            else Change.Save(existing.copy(statusRaw = status.raw, loggedAt = now))
        }
        if (status == IntakeStatus.PENDING) return Change.Nothing
        return Change.Create(
            MedicationIntake(
                scheduledAt = slot.scheduledAt,
                statusRaw = status.raw,
                loggedAt = now,
                medicationId = slot.medication.id,
            ),
        )
    }

    /// Concloure un tractament: deixa de generar espais i avisos, i **conserva l'historial**.
    fun end(medication: Medication, now: Instant = ViraTime.now()): Medication =
        medication.copy(isActive = false, endedAt = now)
}

@get:StringRes
val IntakeStatus.title: Int
    get() = when (this) {
        IntakeStatus.PENDING -> R.string.medication_status_pending
        IntakeStatus.TAKEN -> R.string.medication_status_taken
        IntakeStatus.SKIPPED -> R.string.medication_status_skipped
        IntakeStatus.LATER -> R.string.medication_status_later
    }

@get:StringRes
val MedicationForm.title: Int
    get() = when (this) {
        MedicationForm.TABLET -> R.string.medication_form_tablet
        MedicationForm.CAPSULE -> R.string.medication_form_capsule
        MedicationForm.SYRUP -> R.string.medication_form_syrup
        MedicationForm.DROPS -> R.string.medication_form_drops
        MedicationForm.PATCH -> R.string.medication_form_patch
        MedicationForm.OTHER -> R.string.medication_form_other
    }

/// Pictograma de la forma (`ViraPictogram.Name.medicationForm`).
@get:DrawableRes
val MedicationForm.pictogram: Int
    get() = when (this) {
        MedicationForm.TABLET -> R.drawable.picto_med_tablet
        MedicationForm.CAPSULE -> R.drawable.picto_med_capsule
        MedicationForm.SYRUP -> R.drawable.picto_med_syrup
        MedicationForm.DROPS -> R.drawable.picto_med_drops
        MedicationForm.PATCH -> R.drawable.picto_med_patch
        MedicationForm.OTHER -> R.drawable.picto_med_other
    }
