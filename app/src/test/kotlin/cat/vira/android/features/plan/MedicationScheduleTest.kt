package cat.vira.android.features.plan

import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// `MedicationSchedule`, calcat de `MedicationScheduleTests` de l'iPhone. Un espai que no es
/// genera és un avís que no arriba, i un aparellament que falla és una presa duplicada.
class MedicationScheduleTest {

    private val zone = ZoneId.of("Europe/Madrid")

    /// Dilluns 3 d'agost del 2026.
    private val monday = LocalDate.of(2026, 8, 3)

    private fun medication(times: List<Int> = listOf(8 * 60), weekdays: List<Int> = emptyList()) = Medication(
        name = "Elvanse", dose = "30 mg", formRaw = MedicationForm.CAPSULE.raw, timesInMinutes = times, weekdays = weekdays,
    )

    private fun slots(medication: Medication, intakes: List<MedicationIntake> = emptyList(), day: LocalDate = monday) =
        MedicationSchedule.slots(medication, intakes, day, zone)

    private fun taken(medication: Medication, hour: Int) = MedicationIntake(
        scheduledAt = PlanDay.at(monday, hour, 0, zone), statusRaw = IntakeStatus.TAKEN.raw, medicationId = medication.id,
    )

    @Test fun `cada hora de la pauta genera un espai`() {
        assertEquals(3, slots(medication(listOf(8 * 60, 14 * 60, 21 * 60))).size)
    }

    @Test fun `els espais surten ordenats per hora encara que la pauta no ho estigui`() {
        val result = slots(medication(listOf(21 * 60, 8 * 60, 14 * 60))).map { it.scheduledAt }
        assertEquals(result.sorted(), result)
    }

    @Test fun `l'hora de l'espai es la de la pauta al minut`() {
        val time = slots(medication(listOf(8 * 60 + 30))).first().scheduledAt.atZone(zone)
        assertEquals(8, time.hour)
        assertEquals(30, time.minute)
    }

    @Test fun `una pauta de dies concrets no genera res els dies que no toca`() {
        // 2 = dilluns a la numeració de l'iPhone (1 = diumenge).
        assertEquals(1, slots(medication(weekdays = listOf(2))).size)
        assertTrue("dimarts no és dilluns", slots(medication(weekdays = listOf(3))).isEmpty())
    }

    @Test fun `una pauta sense dies concrets toca cada dia`() {
        repeat(7) { assertEquals(1, slots(medication(), day = monday.plusDays(it.toLong())).size) }
    }

    @Test fun `una pauta conclosa deixa de generar espais`() {
        assertTrue(slots(ViraMedication.end(medication(), Instant.EPOCH)).isEmpty())
    }

    @Test fun `concloure conserva el registre i marca la data`() {
        val ended = ViraMedication.end(medication(), Instant.EPOCH)
        assertFalse(ended.isActive)
        assertEquals(Instant.EPOCH, ended.endedAt)
    }

    @Test fun `un registre a la mateixa hora s'aparella amb el seu espai`() {
        val med = medication(listOf(8 * 60))
        assertEquals(IntakeStatus.TAKEN, slots(med, listOf(taken(med, 8))).first().status)
    }

    @Test fun `un registre d'una altra hora no s'aparella`() {
        val med = medication(listOf(8 * 60, 14 * 60))
        val result = slots(med, listOf(taken(med, 8)))
        assertEquals(IntakeStatus.TAKEN, result.first().status)
        assertEquals("la presa de la tarda encara no s'ha resolt", IntakeStatus.PENDING, result.last().status)
    }

    @Test fun `un registre d'una altra pauta no s'aparella`() {
        val med = medication(listOf(8 * 60))
        assertEquals(IntakeStatus.PENDING, slots(med, listOf(taken(medication(), 8))).first().status)
    }

    @Test fun `sense registre l'espai es pendent`() {
        assertEquals(IntakeStatus.PENDING, slots(medication()).first().status)
    }

    @Test fun `els espais de diverses pautes es barregen per hora`() {
        val morning = medication(listOf(8 * 60))
        val night = medication(listOf(21 * 60))
        val result = MedicationSchedule.slots(listOf(night, morning), emptyList(), monday, zone).map { it.scheduledAt }
        assertEquals(2, result.size)
        assertEquals("l'agenda del dia va per hora", result.sorted(), result)
    }

    @Test fun `tornar a triar el mateix estat el desmarca`() {
        val med = medication(listOf(8 * 60))
        val slot = slots(med, listOf(taken(med, 8))).first()
        assertTrue(ViraMedication.record(IntakeStatus.TAKEN, slot) is ViraMedication.Change.Delete)
    }

    @Test fun `canviar d'estat desa el mateix registre amb l'hora d'ara`() {
        val med = medication(listOf(8 * 60))
        val intake = taken(med, 8)
        val change = ViraMedication.record(IntakeStatus.SKIPPED, slots(med, listOf(intake)).first(), Instant.EPOCH)
        change as ViraMedication.Change.Save
        assertEquals(intake.id, change.intake.id)
        assertEquals(IntakeStatus.SKIPPED, change.intake.status)
        assertEquals(Instant.EPOCH, change.intake.loggedAt)
    }

    @Test fun `un espai sense registre en crea un, i pendent no en crea cap`() {
        val slot = slots(medication()).first()
        val change = ViraMedication.record(IntakeStatus.LATER, slot)
        change as ViraMedication.Change.Create
        assertEquals(slot.scheduledAt, change.intake.scheduledAt)
        assertEquals(ViraMedication.Change.Nothing, ViraMedication.record(IntakeStatus.PENDING, slot))
    }

    @Test fun `presa saltada i mes tard son estats diferents de pendent`() {
        val statuses = setOf(IntakeStatus.TAKEN, IntakeStatus.SKIPPED, IntakeStatus.LATER)
        assertEquals(3, statuses.size)
        assertFalse(IntakeStatus.PENDING in statuses)
    }
}
