package cat.vira.android.core.notifications

import android.app.AlarmManager
import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.R
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.IntakeStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/// Una app de proves amb les accions connectades a una llibreta, per veure què demana cada botó.
class NotificationTestApp : Application(), ViraNotificationHost {
    val calls = mutableListOf<String>()
    override val notificationDatabase: ViraDatabase by lazy { ViraDatabase.inMemory(this) }
    override val notificationEffects = ViraNotificationEffects(
        completeHabit = { id, day -> calls += "habit $id ${day.toEpochMilli()}" },
        completePlanItem = { id -> calls += "plan $id" },
        recordIntake = { id, at, status -> calls += "intake $id ${at.toEpochMilli()} ${status.raw}" },
        logEmotion = { emotion -> calls += "emotion ${emotion.raw}" },
        capture = { text -> calls += "capture $text" },
        afterChange = { calls += "afterChange" },
    )
}

/// La meitat de sistema: les alarmes, el registre de programades, els botons i les respostes.
@RunWith(RobolectricTestRunner::class)
@Config(application = NotificationTestApp::class)
class ViraNotificationSystemTest {

    private val app: NotificationTestApp = ApplicationProvider.getApplicationContext()
    private val scheduler = ViraNotificationScheduler(app)
    private val now = Instant.parse("2027-01-15T08:00:00Z")

    private fun planned(id: String, minutes: Long = 60, actions: ViraActionSet = ViraActionSet.HABIT) = ViraPlannedNotification(
        id = id, fireAtMillis = now.plusSeconds(minutes * 60).toEpochMilli(), title = "Vira", body = "Hora de llegir",
        channel = ViraNotificationChannel.HABITS, actions = actions,
        habitId = UUID.randomUUID().toString(), dayMillis = now.toEpochMilli(),
        medicationId = UUID.randomUUID().toString(), scheduledAtMillis = now.toEpochMilli(),
    )

    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms

    @Test
    fun referLaFinestraTreuElsPropisIPosaElsNous() {
        scheduler.replace(listOf("habit-"), listOf(planned("habit-a"), planned("habit-b")))
        scheduler.replace(listOf("habit-"), listOf(planned("habit-c")))
        assertEquals(listOf("habit-c"), scheduler.armed().map { it.id })
        assertEquals(1, alarms.size)
    }

    @Test
    fun unaCategoriaNoTocaLesAltres() {
        scheduler.replace(listOf("habit-"), listOf(planned("habit-a")))
        scheduler.replace(listOf("med-"), listOf(planned("med-a")))
        assertEquals(setOf("habit-a", "med-a"), scheduler.armed().map { it.id }.toSet())
    }

    @Test
    fun unMesTardSobreviuALaPassadaQueSegueixALAccio() {
        val original = planned("habit-a")
        scheduler.add(ViraNotificationActions.snoozed(original, now))
        scheduler.replace(listOf("habit-"), emptyList())
        val kept = scheduler.armed().single()
        assertEquals("habit-a-snooze", kept.id)
        assertEquals(now.plusSeconds(600), kept.fireAt)
        assertEquals("el mateix text i els mateixos botons", original.body, kept.body)
        assertEquals(
            "ajornar-lo dues vegades no apila sufixos",
            "habit-a-snooze", ViraNotificationActions.snoozed(kept, now).id,
        )
    }

    @Test
    fun treureHoTotSEnduTambeElsMesTard() {
        scheduler.add(ViraNotificationActions.snoozed(planned("habit-a"), now))
        scheduler.cancelAll(listOf("habit-"))
        assertTrue("cap avís pot sonar amb el contingut d'abans", scheduler.armed().isEmpty())
    }

    @Test
    fun unaAlarmaEndarreridaNoEsTreuEnReferLaFinestra() {
        scheduler.replace(listOf("habit-"), listOf(planned("habit-late", minutes = -5), planned("habit-old", minutes = -90)), now)
        scheduler.replace(listOf("habit-"), emptyList(), now)
        assertEquals(
            "la de fa cinc minuts encara pot sonar tard; la de fa hora i mitja ja no",
            listOf("habit-late"), scheduler.armed().map { it.id },
        )
    }

    @Test
    fun unMesTardDUnaCosaJaFetaEsTreu() {
        scheduler.add(ViraNotificationActions.snoozed(planned("habit-a"), now))
        scheduler.replace(listOf("habit-"), emptyList(), now) { false }
        assertTrue(scheduler.armed().isEmpty())
    }

    @Test
    fun esborrarHoTotTreuTambeElsAvisosJaPublicats() = runBlocking {
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        ViraNotificationChannel.register(app)
        manager.notify("med-a", 0, ViraNotificationAlarmReceiver.build(app, planned("med-a", actions = ViraActionSet.MEDICATION)))
        manager.notify("focus", 7, ViraNotificationAlarmReceiver.build(app, planned("focus")))
        ViraNotifications.cancelEverything(app)
        assertEquals(listOf("focus"), manager.activeNotifications.map { it.tag })
    }

    @Test
    fun unAvisJaPublicatDUnaCosaMarcadaALAppEsTreu() = runBlocking {
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        ViraNotificationChannel.register(app)
        ViraPreferences.open(app).edit().putBoolean(ViraNotificationPreferences.REMINDERS_ENABLED, true).commit()
        shadowOf(app).grantPermissions(ViraNotifications.PERMISSION)
        // Un hàbit que ja no existeix: el que recordava ja no està per fer.
        val stale = planned("habit-a")
        manager.notify(stale.id, 0, ViraNotificationAlarmReceiver.build(app, stale))
        ViraNotifications.reschedule(app, now)
        assertTrue(manager.activeNotifications.none { it.tag == "habit-a" })
    }

    @Test
    fun apagarElDetallTreuElNomDelFarmacDUnAvisJaProgramat() {
        val medication = planned("med-a", actions = ViraActionSet.MEDICATION).copy(
            channel = ViraNotificationChannel.MEDICATION, body = "Concerta",
        )
        assertEquals(app.getString(R.string.notification_generic_body), ViraNotifications.presented(app, medication).body)
        ViraPreferences.open(app).edit().putBoolean(ViraNotificationPreferences.NOTIFICATION_DETAIL_MEDICATION_ENABLED, true).commit()
        assertEquals("Concerta", ViraNotifications.presented(app, medication).body)
    }

    @Test
    fun unAvisDUnaCategoriaApagadaNoSurt() {
        ViraPreferences.open(app).edit().putBoolean(ViraNotificationPreferences.REMINDERS_HABITS_ENABLED, false).commit()
        assertFalse(ViraNotifications.isWanted(app, planned("habit-a")))
        assertTrue(ViraNotifications.isWanted(app, planned("plan-a").copy(channel = ViraNotificationChannel.PLAN)))
    }

    @Test
    fun enArrencarElTelefonNomesTornenLesQueHanDeSonar() {
        scheduler.replace(listOf("habit-"), listOf(planned("habit-past", minutes = -5), planned("habit-future")))
        scheduler.rearm(now)
        assertEquals(listOf("habit-future"), scheduler.armed().map { it.id })
    }

    @Test
    fun unaNoticiaDeViriCaducaSiLaSevaFrancaJaHaPassat() {
        val zone = ZoneId.of("UTC")
        val preferences = ViraPreferences.open(app)
        val time = ViraClock(20, 30)
        ViraStreakNotifications.record(ViraStreakNotifications.Event.Milestone(30), preferences, now)
        assertEquals(
            ViraStreakNotifications.Event.Milestone(30),
            ViraStreakNotifications.pendingEvent(preferences, time, now.plusSeconds(3600), zone),
        )
        assertNull(ViraStreakNotifications.pendingEvent(preferences, time, now.plusSeconds(86_400), zone))
        assertNull("caducada, s'esborra", ViraStreakNotifications.pendingEvent(preferences, time, now, zone))
    }

    @Test
    fun elsBotonsDiuenPresaIMaiElNomDelFarmac() {
        val medication = planned("med-a", actions = ViraActionSet.MEDICATION).copy(
            channel = ViraNotificationChannel.MEDICATION, body = "Tens un recordatori",
        )
        val built = ViraNotificationAlarmReceiver.build(app, medication)
        val titles = (0 until NotificationCompat.getActionCount(built)).map { NotificationCompat.getAction(built, it)!!.title.toString() }
        // Amb els textos de l'idioma de la prova (Robolectric surt en anglès), no escrits a mà.
        val expected = listOf(
            R.string.notification_action_taken, R.string.notification_action_skipped, R.string.notification_action_snooze,
        ).map(app::getString)
        assertEquals(expected, titles)
    }

    @Test
    fun lAvisDeViriPortaTresBotons() {
        val built = ViraNotificationAlarmReceiver.build(app, planned("emotion-a", actions = ViraActionSet.VIRI))
        assertEquals("Android n'ensenya tres com a molt", 3, NotificationCompat.getActionCount(built))
    }

    @Test
    fun cadaBotoCridaElCamiDeLaSevaPantalla() = runBlocking {
        val n = planned("med-a", actions = ViraActionSet.MEDICATION)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.HABIT_DONE, n), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.MEDICATION_SKIPPED, n), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request("emotion.anxious", n), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.CAPTURE, n, "  "), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.CAPTURE, n, " dentista "), now)
        val writes = app.calls.filter { it != "afterChange" }
        assertEquals(
            listOf(
                "habit ${n.habitId} ${n.dayMillis}",
                "intake ${n.medicationId} ${n.scheduledAtMillis} ${IntakeStatus.SKIPPED.raw}",
                "emotion ${Emotion.ANXIOUS.raw}",
                "capture dentista",
            ),
            writes,
        )
    }

    @Test
    fun unaAccioDesconegudaOSenseIdentificadorNoEscriuRes() = runBlocking {
        val n = planned("plan-a").copy(planItemId = null, habitId = "no és un id")
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.PLAN_DONE, n), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request(ViraNotificationActions.HABIT_DONE, n), now)
        ViraNotificationActions.handle(app, ViraNotificationActions.Request("emotion.inventada", n), now)
        assertFalse(app.calls.any { it.startsWith("plan") || it.startsWith("habit") || it.startsWith("emotion") })
    }

    @Test
    fun senseElPermisNoQuedaCapRecordatoriProgramat() = runBlocking {
        scheduler.replace(listOf("habit-"), listOf(planned("habit-a")))
        ViraNotifications.reschedule(app, now)
        assertTrue(scheduler.armed().isEmpty())
    }
}
