package cat.vira.android.core.notifications

import cat.vira.android.R
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Quins recordatoris toquen i què diuen (`ViraNotificationPlanner`). La regla de fons: **cap
/// avís d'una cosa ja feta**, i el nom del fàrmac només amb el seu interruptor propi.
class ViraNotificationPlannerTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2027, 1, 15)
    private val now: Instant = today.atTime(8, 0).atZone(zone).toInstant()
    private val text = ViraNotificationText { id, args -> "$id|${args.joinToString()}" }
    // El registre emocional s'encén només a les seves proves: sinó, sumaria tres avisos a cada una.
    private val settings = ViraNotificationSettings(emotion = false)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) = day.atTime(hour, minute).atZone(zone).toInstant()

    private fun plan(inputs: ViraNotificationInputs, settings: ViraNotificationSettings = this.settings) =
        ViraNotificationPlanner.plan(inputs, settings, now, zone, text)

    // Hàbits

    @Test
    fun unHabitAmbHoraTeUnAvisCadaDiaDeLaFinestra() {
        val habit = Habit(title = "Llegir", reminderTime = at(today, 18))
        val planned = plan(ViraNotificationInputs(habits = listOf(habit)))
        assertEquals(7, planned.size)
        assertTrue(planned.all { it.actions == ViraActionSet.HABIT && it.habitId == habit.id.toString() })
        assertEquals(text.get(R.string.notification_detail_body, "Llegir"), planned.first().body)
    }

    @Test
    fun unDiaJaMarcatNoEsRecorda() {
        val habit = Habit(title = "Llegir", reminderTime = at(today, 18))
        val log = HabitLog(habitId = habit.id, day = today.atStartOfDay(zone).toInstant())
        val planned = plan(ViraNotificationInputs(habits = listOf(habit), habitLogs = listOf(log)))
        assertTrue(planned.none { LocalDate.ofInstant(it.fireAt, zone) == today })
        assertEquals(6, planned.size)
    }

    @Test
    fun unHabitArxivatOSenseHoraNoEnTe() {
        val archived = Habit(title = "A", reminderTime = at(today, 18), isArchived = true)
        val noTime = Habit(title = "B")
        assertTrue(plan(ViraNotificationInputs(habits = listOf(archived, noTime))).isEmpty())
    }

    @Test
    fun nomesElsDiesDeLaSetmanaTriats() {
        // 1 = diumenge, com a l'iPhone. El 15 de gener de 2027 és divendres (6).
        val habit = Habit(title = "Futbol", reminderTime = at(today, 18), weekdays = listOf(6))
        val planned = plan(ViraNotificationInputs(habits = listOf(habit)))
        assertEquals(listOf(today), planned.map { LocalDate.ofInstant(it.fireAt, zone) })
    }

    @Test
    fun elsHabitsNoPassenDelSeuPressupost() {
        val habits = (0 until 5).map { Habit(title = "H$it", reminderTime = at(today, 9 + it)) }
        val planned = plan(ViraNotificationInputs(habits = habits))
        assertEquals(ViraNotificationPlanner.HABIT_BUDGET, planned.size)
        assertEquals("el que cau és el més llunyà", planned.sortedBy { it.fireAt }, planned)
    }

    @Test
    fun senseDetallElTextEsGeneric() {
        val habit = Habit(title = "Llegir", reminderTime = at(today, 18))
        val planned = plan(ViraNotificationInputs(habits = listOf(habit)), settings.copy(detail = false))
        assertTrue(planned.all { it.body == text.get(R.string.notification_generic_body) })
    }

    // Medicació

    @Test
    fun elNomDelFarmacNomesAmbElSeuInterruptor() {
        val medication = Medication(name = "Concerta", timesInMinutes = listOf(9 * 60))
        val inputs = ViraNotificationInputs(medications = listOf(medication))
        assertTrue(
            "neix apagat encara que el detall de l'hàbit sigui encès",
            plan(inputs).none { it.body.contains("Concerta") },
        )
        assertTrue(plan(inputs, settings.copy(medicationDetail = true)).all { it.body.contains("Concerta") })
    }

    @Test
    fun unaPresaJaResoltaNoEsRecorda() {
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60, 21 * 60))
        val resolved = IntakeStatus.entries.filter { it != IntakeStatus.PENDING }.map { status ->
            val inputs = ViraNotificationInputs(
                medications = listOf(medication),
                intakes = listOf(MedicationIntake(medicationId = medication.id, scheduledAt = at(today, 9), statusRaw = status.raw)),
            )
            plan(inputs).count { LocalDate.ofInstant(it.fireAt, zone) == today }
        }
        assertEquals("presa, saltada i més tard: només queda la de la nit", listOf(1, 1, 1), resolved)
    }

    @Test
    fun unaPautaConclosaOSenseAvisNoEnDona() {
        val ended = Medication(name = "A", timesInMinutes = listOf(600), isActive = false)
        val silent = Medication(name = "B", timesInMinutes = listOf(600), hasReminder = false)
        assertTrue(plan(ViraNotificationInputs(medications = listOf(ended, silent))).isEmpty())
    }

    @Test
    fun laMedicacioPortaLaHoraIElCanalAlt() {
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60))
        val first = plan(ViraNotificationInputs(medications = listOf(medication))).first()
        assertEquals(ViraNotificationChannel.MEDICATION, first.channel)
        assertEquals(ViraActionSet.MEDICATION, first.actions)
        assertEquals(at(today, 9).toEpochMilli(), first.scheduledAtMillis)
    }

    @Test
    fun elsPressupostosSumen51() {
        assertEquals(
            51,
            ViraNotificationPlanner.MEDICATION_BUDGET + ViraNotificationPlanner.HABIT_BUDGET +
                ViraNotificationPlanner.PLAN_BUDGET + ViraNotificationPlanner.EMOTION_BUDGET,
        )
        assertEquals(56, 51 + ViraStreakNotifications.BUDGET)
    }

    @Test
    fun elDiaQueCanviaLHoraLaPresaSegueixALaSevaHora() {
        // A Madrid, el 28 de març de 2027 es passa de les 2 a les 3: les 9 han de seguir sent les 9.
        val day = LocalDate.of(2027, 3, 28)
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60))
        assertEquals(listOf(at(day, 9)), ViraNotificationPlanner.medicationTimes(medication, day, zone))
    }

    @Test
    fun unAvisJaProgramatDeixaDeTocarQuanEsMarca() {
        val habit = Habit(title = "Llegir", reminderTime = at(today, 18))
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60))
        val item = PlanItem(title = "Dentista", day = today.atStartOfDay(zone).toInstant(), time = at(today, 17))
        val inputs = ViraNotificationInputs(habits = listOf(habit), medications = listOf(medication), planItems = listOf(item))
        val planned = plan(inputs)
        val h = planned.first { it.habitId == habit.id.toString() }
        val m = planned.first { it.medicationId == medication.id.toString() }
        val p = planned.first { it.planItemId == item.id.toString() }
        assertTrue(listOf(h, m, p).all { ViraNotificationPlanner.isStillPending(it, inputs, zone) })

        val resolved = inputs.copy(
            habitLogs = listOf(HabitLog(habitId = habit.id, day = today.atStartOfDay(zone).toInstant())),
            intakes = listOf(MedicationIntake(medicationId = medication.id, scheduledAt = at(today, 9), statusRaw = IntakeStatus.TAKEN.raw)),
            planItems = listOf(item.copy(statusRaw = PlanItemStatus.DONE.raw)),
        )
        assertTrue(listOf(h, m, p).none { ViraNotificationPlanner.isStillPending(it, resolved, zone) })
    }

    @Test
    fun unHabitAmbLHoraCanviadaJaNoGuardaElSeuAvis() {
        val habit = Habit(title = "Llegir", reminderTime = at(today, 18))
        val inputs = ViraNotificationInputs(habits = listOf(habit))
        val n = plan(inputs).first { it.habitId == habit.id.toString() }
        assertTrue(ViraNotificationPlanner.isStillPending(n, inputs, zone))
        val moved = inputs.copy(habits = listOf(habit.copy(reminderTime = at(today, 19))))
        assertTrue(!ViraNotificationPlanner.isStillPending(n, moved, zone))
    }

    @Test
    fun elRecordatoriEmocionalDAhirJaNoToca() {
        val n = plan(ViraNotificationInputs(), settings.copy(emotion = true)).first()
        val inputs = ViraNotificationInputs()
        assertTrue(ViraNotificationPlanner.isStillPending(n, inputs, zone, n.fireAt))
        assertTrue(!ViraNotificationPlanner.isStillPending(n, inputs, zone, n.fireAt.plusSeconds(86_400)))
    }

    @Test
    fun unaTascaMogudaOSenseHoraJaNoGuardaElSeuAvis() {
        val day = today.atStartOfDay(zone).toInstant()
        val item = PlanItem(title = "Dentista", day = day, time = at(today, 17))
        val inputs = ViraNotificationInputs(planItems = listOf(item))
        val n = plan(inputs).single()
        assertTrue(ViraNotificationPlanner.isStillPending(n, inputs, zone))
        assertTrue(!ViraNotificationPlanner.isStillPending(n, inputs.copy(planItems = listOf(item.copy(time = at(today, 18)))), zone))
        assertTrue(!ViraNotificationPlanner.isStillPending(n, inputs.copy(planItems = listOf(item.copy(time = null))), zone))
    }

    // Pla

    @Test
    fun nomesLesTasquesPendentsAmbHoraIAmbLAntelacio() {
        val day = today.atStartOfDay(zone).toInstant()
        val withTime = PlanItem(title = "Dentista", day = day, time = at(today, 17), reminderOffsetMinutes = 30)
        val noTime = PlanItem(title = "Comprar", day = day)
        val done = PlanItem(title = "Fet", day = day, time = at(today, 12), statusRaw = PlanItemStatus.DONE.raw)
        val planned = plan(ViraNotificationInputs(planItems = listOf(withTime, noTime, done)))
        assertEquals(1, planned.size)
        assertEquals(at(today, 16, 30), planned.first().fireAt)
        assertEquals(withTime.id.toString(), planned.first().planItemId)
    }

    @Test
    fun unaTascaForaDeLaFinestraNoEnTe() {
        val far = today.plusDays(7)
        val item = PlanItem(title = "Lluny", day = far.atStartOfDay(zone).toInstant(), time = at(far, 10))
        assertTrue(plan(ViraNotificationInputs(planItems = listOf(item))).isEmpty())
    }

    // Emocions

    @Test
    fun elRegistreEmocionalEsUnAlDiaITresDies() {
        val planned = plan(ViraNotificationInputs(), settings.copy(emotion = true))
        assertEquals(3, planned.size)
        assertTrue(planned.all { it.body == text.get(R.string.notification_emotion_body) && it.actions == ViraActionSet.VIRI })
        assertEquals(at(today, 19), planned.first().fireAt)
    }

    @Test
    fun unDiaQueJaTeRegistreNoRepElRecordatori() {
        val entry = EmotionEntry(createdAt = at(today, 7))
        val planned = plan(ViraNotificationInputs(emotionEntries = listOf(entry)), settings.copy(emotion = true))
        assertEquals(2, planned.size)
        assertTrue(planned.none { LocalDate.ofInstant(it.fireAt, zone) == today })
    }

    @Test
    fun lesCategoriesApagadesNoProgramenRes() {
        val off = settings.copy(habits = false, medication = false, plan = false, emotion = false)
        val habit = Habit(title = "H", reminderTime = at(today, 18))
        assertTrue(plan(ViraNotificationInputs(habits = listOf(habit)), off).isEmpty())
    }

    // Concentració

    @Test
    fun unaSessioQueSilenciaViraEsMenjaElsAvisosDeDins() {
        val session = FocusSession(startedAt = at(today, 17, 50), plannedSeconds = 25 * 60, mutesNotifications = true)
        val habit = Habit(title = "H", reminderTime = at(today, 18))
        val planned = plan(ViraNotificationInputs(habits = listOf(habit), focusSessions = listOf(session)))
        assertTrue(planned.none { LocalDate.ofInstant(it.fireAt, zone) == today && it.channel == ViraNotificationChannel.HABITS })
    }

    @Test
    fun unaSessioAcabadaOQueNoSilenciaNoCompta() {
        val ended = FocusSession(startedAt = at(today, 7), plannedSeconds = 3600 * 3, endedAt = at(today, 7, 30), mutesNotifications = true)
        val loud = FocusSession(startedAt = at(today, 7), plannedSeconds = 3600 * 3)
        val past = FocusSession(startedAt = at(today, 5), plannedSeconds = 60, mutesNotifications = true)
        assertEquals(null, ViraNotificationPlanner.muteWindow(listOf(ended, loud, past), now))
    }
}
