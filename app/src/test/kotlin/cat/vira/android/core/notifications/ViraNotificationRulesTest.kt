package cat.vira.android.core.notifications

import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.features.settings.InMemoryPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/// Els avisos després de la recerca de l'iPhone (2026-10-23): l'absència, l'hora apresa, el
/// descans de 12–15, «Avui no», l'avís del matí i un sol interruptor.
class ViraNotificationRulesTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2027, 1, 15)
    private val now: Instant = today.atTime(7, 0).atZone(zone).toInstant()
    private val text = ViraNotificationText { id, args -> "$id|${args.joinToString()}" }
    private val time = ViraClock(20, 30)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) = day.atTime(hour, minute).atZone(zone).toInstant()

    // Absència

    @Test
    fun laRetiradaEsNormalEspaiadaComiatISilenci() {
        val opened = at(today, 10)
        fun on(days: Long) = ViraAbsence.allowance(today.plusDays(days), opened, zone)
        assertEquals(ViraAbsence.Allowance.NORMAL, on(0))
        assertEquals(ViraAbsence.Allowance.NORMAL, on(2))
        assertEquals(ViraAbsence.Allowance.SPARSE, on(3))
        assertEquals(ViraAbsence.Allowance.SILENT, on(4))
        assertEquals(ViraAbsence.Allowance.SPARSE, on(12))
        assertEquals(ViraAbsence.Allowance.FAREWELL, on(14))
        assertEquals(ViraAbsence.Allowance.SILENT, on(15))
    }

    @Test
    fun viriArribaFinsAlComiatIDespresCalla() {
        val planned = ViraStreakNotifications.requests(now, null, dayIsOpen = true, muted = null, time, zone, text)
        val days = planned.map { java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.ofInstant(it.fireAt, zone)) }
        assertEquals(listOf(0L, 1, 2, 3, 6, 9, 12, 14), days)
        assertEquals(text.get(R.string.notification_streak_farewell), planned.last().body)
        assertTrue(planned.size <= ViraStreakNotifications.BUDGET)
    }

    @Test
    fun elComiatManaSobreLaNoticia() {
        val since = at(today.minusDays(14), 10)
        val planned = ViraStreakNotifications.requests(
            now, ViraStreakNotifications.Event.Reset, dayIsOpen = true, muted = null, time, zone, text, since = since,
        )
        assertEquals(1, planned.size)
        assertEquals(text.get(R.string.notification_streak_farewell), planned.single().body)
    }

    @Test
    fun laMedicacioNoEsRetiraMai() {
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60))
        val settings = ViraNotificationSettings(since = at(today.minusDays(30), 10), emotion = false, morning = false)
        val planned = ViraNotificationPlanner.plan(ViraNotificationInputs(medications = listOf(medication)), settings, now, zone, text)
        assertEquals(ViraNotificationPlanner.WINDOW_DAYS, planned.size)
    }

    // Hora apresa

    @Test
    fun lHoraSAprenUnaHoraAbansDeLaMitjanaIEntre17i21() {
        assertNull(ViraEveningHour.learned(listOf(19 * 60, 20 * 60)))
        assertEquals(19 * 60, ViraEveningHour.learned(listOf(20 * 60, 20 * 60, 20 * 60)))
        assertEquals(17 * 60, ViraEveningHour.learned(listOf(12 * 60, 13 * 60, 14 * 60)))
        // Passada la mitjanit compta com a tard.
        assertEquals(21 * 60, ViraEveningHour.learned(listOf(30, 45, 23 * 60)))
        assertEquals(18 * 60 + 45, ViraEveningHour.learned(listOf(19 * 60 + 50, 19 * 60 + 40, 19 * 60 + 45)))
    }

    @Test
    fun lHoraApresaNoFaServirDiesDeFaMesDe14Dies() {
        val old = mapOf(today.minusDays(20) to 18 * 60, today.minusDays(19) to 18 * 60, today.minusDays(15) to 18 * 60)
        assertTrue("sense dies nous, els vells no compten", ViraEveningHour.recent(old, today).isEmpty())
        val mixed = old + (today.minusDays(2) to 20 * 60)
        assertEquals(listOf(20 * 60), ViraEveningHour.recent(mixed, today))
    }

    // Medicació amagada

    @Test
    fun ambElModulDeMedicacioAmagatNoSurtenElsSeusAvisos() {
        val on = InMemoryPreferences()
        assertTrue(ViraNotificationPreferences.medicationEnabled(on))
        val hidden = InMemoryPreferences(mapOf(ViraNotificationPreferences.MODULE_MEDICATION_ENABLED to false))
        assertFalse(ViraNotificationPreferences.medicationEnabled(hidden))
    }

    // Descans de 12–15

    @Test
    fun elTramJoveNoRepResQueNoHagiDemanatDesDeLes2130() {
        assertTrue(ViraRestHours.allows(at(today, 21, 29), young = true, zone))
        assertFalse(ViraRestHours.allows(at(today, 21, 30), young = true, zone))
        assertTrue(ViraRestHours.allows(at(today, 23), young = false, zone))
        val late = ViraNotificationSettings(emotionTime = ViraClock(22, 0), young = true, morning = false)
        assertTrue(ViraNotificationPlanner.emotion(ViraNotificationInputs(), late, now, zone, text).isEmpty())
    }

    // «Avui no»

    @Test
    fun avuiNoICallaUnsDies() {
        val today = ViraViriQuiet.quietEnd(1, now, zone)
        assertEquals(at(this.today.plusDays(1), 0), today)
        assertEquals(at(this.today.plusDays(3), 0), ViraViriQuiet.quietEnd(ViraViriQuiet.SEVERAL_DAYS, now, zone))
        val planned = ViraStreakNotifications.requests(now, null, true, null, time, zone, text, quietUntil = today)
        assertTrue(planned.none { LocalDate.ofInstant(it.fireAt, zone) == this.today })
        val emotion = ViraNotificationPlanner.emotion(ViraNotificationInputs(), ViraNotificationSettings(quietUntil = today), now, zone, text)
        assertTrue(emotion.none { LocalDate.ofInstant(it.fireAt, zone) == this.today })
    }

    // Un sol avís al vespre

    @Test
    fun quanViriParlaLEmocionalCalla() {
        val speaks = ViraNotificationSettings(viriSpeaks = true)
        assertTrue(ViraNotificationPlanner.emotion(ViraNotificationInputs(), speaks, now, zone, text).isEmpty())
        val silent = ViraNotificationSettings(viriSpeaks = false)
        assertEquals(3, ViraNotificationPlanner.emotion(ViraNotificationInputs(), silent, now, zone, text).size)
    }

    // Un sol interruptor

    @Test
    fun avisosApagatsNomesDeixaLaMedicacio() {
        val medication = Medication(name = "M", timesInMinutes = listOf(9 * 60))
        val habit = Habit(title = "H", reminderTime = at(today, 18))
        val off = ViraNotificationSettings(general = false)
        val planned = ViraNotificationPlanner.plan(
            ViraNotificationInputs(medications = listOf(medication), habits = listOf(habit)), off, now, zone, text,
        )
        assertTrue(planned.isNotEmpty())
        assertTrue(planned.all { it.channel == ViraNotificationChannel.MEDICATION })
    }

    // El teu dia

    @Test
    fun elTeuDiaDiuQuantesCosesILaPrimeraAmbHora() {
        val day = today.atStartOfDay(zone).toInstant()
        val items = listOf(
            PlanItem(title = "Mates", day = day, time = at(today, 9)),
            PlanItem(title = "Comprar", day = day),
        )
        val settings = ViraNotificationSettings(locale = Locale.ROOT)
        val planned = ViraMorningNotification.requests(ViraNotificationInputs(planItems = items), settings, now, zone, text, null)
        assertEquals(3, planned.size)
        val first = planned.first()
        assertEquals(at(today, 8), first.fireAt)
        assertTrue(first.body.startsWith(text.get(R.string.notification_morning_many, 2)))
        assertTrue(first.body.contains("Mates"))
        // Els dies següents són buits: conviden a apuntar.
        assertEquals(text.get(R.string.notification_morning_empty), planned[1].body)
    }

    @Test
    fun elTeuDiaTriaLaPrimeraPerLHoraDeLaTascaINoPerLaDeLAvis() {
        val day = today.atStartOfDay(zone).toInstant()
        val items = listOf(
            PlanItem(title = "Mates", day = day, time = at(today, 9), reminderOffsetMinutes = 60),
            PlanItem(title = "Angles", day = day, time = at(today, 8, 30)),
        )
        val said = ViraMorningNotification.message(ViraNotificationInputs(planItems = items), today, zone, true, Locale.ROOT)
        assertEquals("Angles", (said as ViraMorningNotification.Message.Pending).first?.first)
    }

    @Test
    fun elTeuDiaNoSurtSiJaEsTotFetNiAmbAbsencia() {
        val day = today.atStartOfDay(zone).toInstant()
        val done = PlanItem(title = "Fet", day = day, statusRaw = PlanItemStatus.DONE.raw)
        assertNull(ViraMorningNotification.message(ViraNotificationInputs(planItems = listOf(done)), today, zone, true, Locale.ROOT))
        val away = ViraNotificationSettings(since = at(today.minusDays(3), 10))
        assertTrue(ViraMorningNotification.requests(ViraNotificationInputs(), away, now, zone, text, null).isEmpty())
        val noDetail = ViraMorningNotification.message(
            ViraNotificationInputs(planItems = listOf(PlanItem(title = "Mates", day = day, time = at(today, 9)))),
            today, zone, detail = false, locale = Locale.ROOT,
        )
        assertEquals(ViraMorningNotification.Message.Pending(1, null), noDetail)
    }
}
