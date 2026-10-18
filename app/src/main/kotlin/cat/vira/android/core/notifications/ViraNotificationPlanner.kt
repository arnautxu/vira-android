package cat.vira.android.core.notifications

import cat.vira.android.R
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.iosWeekday
import cat.vira.android.core.data.model.status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/// Quins recordatoris toquen i què diuen: la meitat de càlcul de `ViraNotifications` de l'iPhone.
///
/// ## El sostre que mana
/// iOS només reté **64 avisos pendents per app** i descarta la resta en silenci. Android no té
/// aquest sostre, però el repartiment es conserva igual: és el que decideix **quins** avisos hi
/// ha, i dues apps que programessin coses diferents amb les mateixes dades serien dues apps. Per
/// això també es conserva la finestra lliscant de pocs dies, que es refà a cada acció, en tornar a
/// primer pla i des del treball de segon pla.
///
/// 26 medicació + 14 hàbits + 8 pla + 3 emocions = **51**, més els 5 de Viri
/// (`ViraStreakNotificationPlanner`) = **56 de 64**; els 8 lliures són per als «Més tard».
/// L'ordre és el del dany de quedar-se sense l'avís: medicació primer, Viri al final.
///
/// ## El que no fa mai
/// - **Recordar una cosa ja feta**: ni un hàbit ja marcat, ni una presa ja resolta, ni una tasca
///   feta o descartada, ni el registre emocional d'un dia que ja en té.
/// - **Avisos repetitius**: no es podrien saltar els dies ja fets.
/// - **Dir el nom del fàrmac** si no s'ha encès el seu interruptor propi.
object ViraNotificationPlanner {

    const val HABIT_PREFIX = "habit-"
    const val MEDICATION_PREFIX = "med-"
    const val PLAN_PREFIX = "plan-"
    const val EMOTION_PREFIX = "emotion-"

    const val MEDICATION_BUDGET = 26
    const val HABIT_BUDGET = 14
    const val PLAN_BUDGET = 8
    const val EMOTION_BUDGET = 3

    /// Finestra recomanada a `IOS_NATIVE_INTEGRATIONS.md` §2.1.
    const val WINDOW_DAYS = 7

    /// N'hi ha un al dia: tres dies per endavant cobreixen de sobres el temps que pot passar sense
    /// que s'obri l'app ni corri el treball de segon pla.
    const val EMOTION_WINDOW_DAYS = 3

    /// Totes les categories enceses, cadascuna dins del seu pressupost.
    fun plan(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
    ): List<ViraPlannedNotification> {
        val muted = muteWindow(inputs.focusSessions, now)
        return buildList {
            if (settings.medication) addAll(medication(inputs, settings, now, zone, text, muted))
            if (settings.habits) addAll(habits(inputs, settings, now, zone, text, muted))
            if (settings.plan) addAll(planItems(inputs, settings, now, zone, text, muted))
            if (settings.emotion) addAll(emotion(inputs, settings, now, zone, text, muted))
        }
    }

    /// Finestra en què no es programa res perquè hi corre una sessió de concentració d'un mode que
    /// silencia Vira. **Es deriva d'una consulta, no d'estat desat**: si l'app mor a mitja sessió,
    /// no queda res enganxat. Silenciar vol dir no programar-los; en acabar, la propera passada
    /// torna a posar els que encara no han passat.
    ///
    /// *Imperfecció acceptada (com a l'iPhone):* si la sessió es pausa, la finestra queda curta.
    fun muteWindow(sessions: List<FocusSession>, now: Instant): ClosedOpenRange? {
        val session = sessions
            .filter { it.endedAt == null && it.mutesNotifications }
            .maxByOrNull { it.startedAt } ?: return null
        val end = session.startedAt.plusSeconds(session.plannedSeconds.toLong())
        if (!end.isAfter(now)) return null
        return ClosedOpenRange(session.startedAt, end)
    }

    fun habits(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
        muted: ClosedOpenRange? = muteWindow(inputs.focusSessions, now),
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        val logsByHabit = inputs.habitLogs.groupBy { it.habitId }
        val slots = mutableListOf<Pair<Instant, Habit>>()

        for (offset in 0 until WINDOW_DAYS) {
            val day = today.plusDays(offset.toLong())
            for (habit in inputs.habits) {
                if (habit.isArchived) continue
                val time = habit.reminderTime ?: continue
                if (!habit.isScheduled(day)) continue
                val fire = at(day, time, zone)
                if (!fire.isAfter(now)) continue
                // Un dia que ja té registre no es recorda.
                val done = logsByHabit[habit.id].orEmpty().any { LocalDate.ofInstant(it.day, zone) == day }
                if (done) continue
                if (muted?.contains(fire) == true) continue
                slots += fire to habit
            }
        }

        // Cronològic abans de truncar: el que es perd són els dies més llunyans, que la propera
        // passada tornarà a programar.
        return slots.sortedBy { it.first }.take(HABIT_BUDGET).map { (fire, habit) ->
            val day = LocalDate.ofInstant(fire, zone)
            ViraPlannedNotification(
                id = "$HABIT_PREFIX${habit.id}-${day.dayOfMonth}-${day.monthValue}",
                fireAtMillis = fire.toEpochMilli(),
                title = text.get(R.string.notification_generic_title),
                body = if (settings.detail) text.get(R.string.notification_detail_body, habit.title)
                else text.get(R.string.notification_generic_body),
                channel = ViraNotificationChannel.HABITS,
                actions = ViraActionSet.HABIT,
                habitId = habit.id.toString(),
                dayMillis = day.atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        }
    }

    /// Una hora **ja resolta** —presa, saltada o deixada per a més tard— no es recorda.
    fun medication(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
        muted: ClosedOpenRange? = muteWindow(inputs.focusSessions, now),
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        val intakesByMedication = inputs.intakes.groupBy { it.medicationId }
        val slots = mutableListOf<Pair<Instant, Medication>>()

        for (offset in 0 until WINDOW_DAYS) {
            val day = today.plusDays(offset.toLong())
            for (medication in inputs.medications) {
                if (!medication.hasReminder) continue
                for (scheduledAt in medicationTimes(medication, day, zone)) {
                    if (!scheduledAt.isAfter(now)) continue
                    // `pending` vol dir que l'usuari no hi ha dit res. Si la sincronització en deixa
                    // dues per a la mateixa hora, n'hi ha prou que una estigui resolta.
                    val resolved = intakesByMedication[medication.id].orEmpty()
                        .any { sameMinute(it.scheduledAt, scheduledAt) && it.status != IntakeStatus.PENDING }
                    if (resolved) continue
                    if (muted?.contains(scheduledAt) == true) continue
                    slots += scheduledAt to medication
                }
            }
        }

        return slots.sortedBy { it.first }.take(MEDICATION_BUDGET).map { (scheduledAt, medication) ->
            ViraPlannedNotification(
                id = "$MEDICATION_PREFIX${medication.id}-${scheduledAt.epochSecond}",
                fireAtMillis = scheduledAt.toEpochMilli(),
                title = text.get(R.string.notification_generic_title),
                // Interruptor **propi** i apagat de fàbrica: el nom del fàrmac no hi pot anar de
                // rebot amb el de l'hàbit.
                body = if (settings.medicationDetail) text.get(R.string.notification_detail_body, medication.name)
                else text.get(R.string.notification_generic_body),
                channel = ViraNotificationChannel.MEDICATION,
                actions = ViraActionSet.MEDICATION,
                medicationId = medication.id.toString(),
                scheduledAtMillis = scheduledAt.toEpochMilli(),
            )
        }
    }

    /// Només les tasques **amb hora**: una sense hora no en té cap de bona. `reminderOffsetMinutes`
    /// és l'antelació; `null` vol dir **a l'hora**.
    fun planItems(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
        muted: ClosedOpenRange? = muteWindow(inputs.focusSessions, now),
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        val end = today.plusDays(WINDOW_DAYS.toLong())
        val slots = inputs.planItems.mapNotNull { item ->
            if (item.status != PlanItemStatus.PENDING) return@mapNotNull null
            val day = LocalDate.ofInstant(item.day, zone)
            if (day < today || day >= end) return@mapNotNull null
            val time = item.time ?: return@mapNotNull null
            // L'hora viu a part del dia: es recompon sobre el dia de l'ítem.
            val fire = at(day, time, zone).minusSeconds((item.reminderOffsetMinutes ?: 0) * 60L)
            if (!fire.isAfter(now) || muted?.contains(fire) == true) return@mapNotNull null
            fire to item
        }

        return slots.sortedBy { it.first }.take(PLAN_BUDGET).map { (fire, item) ->
            ViraPlannedNotification(
                id = "$PLAN_PREFIX${item.id}",
                fireAtMillis = fire.toEpochMilli(),
                title = text.get(R.string.notification_generic_title),
                body = if (settings.detail) text.get(R.string.notification_detail_body, item.title)
                else text.get(R.string.notification_generic_body),
                channel = ViraNotificationChannel.PLAN,
                actions = ViraActionSet.PLAN,
                planItemId = item.id.toString(),
            )
        }
    }

    /// Un al dia, a l'hora d'Ajustos, i **cap si el dia ja té registre**. El text no té variant
    /// amb detall: el detall seria dir a qui miri la pantalla que aquesta persona porta un
    /// registre emocional (`PRIVACY_AND_SAFETY.md` §4.6).
    fun emotion(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
        muted: ClosedOpenRange? = muteWindow(inputs.focusSessions, now),
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        return (0 until EMOTION_WINDOW_DAYS).mapNotNull { offset ->
            val day = today.plusDays(offset.toLong())
            val fire = day.atTime(settings.emotionTime.hour, settings.emotionTime.minute).atZone(zone).toInstant()
            if (!fire.isAfter(now) || muted?.contains(fire) == true) return@mapNotNull null
            // Només d'avui es pot saber si ja té registre; els següents els retira la propera
            // passada o registrar-ne un.
            if (offset == 0 && inputs.emotionEntries.any { LocalDate.ofInstant(it.createdAt, zone) == day }) {
                return@mapNotNull null
            }
            ViraPlannedNotification(
                id = "$EMOTION_PREFIX${day.year}-${day.monthValue}-${day.dayOfMonth}",
                fireAtMillis = fire.toEpochMilli(),
                title = text.get(R.string.notification_generic_title),
                body = text.get(R.string.notification_emotion_body),
                channel = ViraNotificationChannel.EMOTION,
                actions = ViraActionSet.VIRI,
            )
        }.take(EMOTION_BUDGET)
    }

    /// Les hores d'una pauta en un dia (`MedicationSchedule.slots` de l'iPhone). Una pauta
    /// conclosa o que no toca aquell dia no en dona cap.
    fun medicationTimes(medication: Medication, day: LocalDate, zone: ZoneId): List<Instant> {
        if (!medication.isActive) return emptyList()
        if (medication.weekdays.isNotEmpty() && day.dayOfWeek.iosWeekday() !in medication.weekdays) return emptyList()
        // Hora de rellotge sobre el dia, no minuts des de mitjanit: el dia que canvia l'hora,
        // «les 9» han de seguir sent les 9. (L'iPhone suma minuts i es desplaça una hora aquells
        // dos dies.)
        return medication.timesInMinutes.sorted().map { day.atTime(it / 60, it % 60).atZone(zone).toInstant() }
    }

    /// Si allò que recorda un avís ja programat encara està per fer. Un «Més tard» o una alarma
    /// endarrerida sobreviuen a la passada, però no si mentrestant s'ha marcat a l'app: seria
    /// recordar una cosa ja feta, i els botons de medicació actuarien sobre una presa resolta.
    fun isStillPending(notification: ViraPlannedNotification, inputs: ViraNotificationInputs, zone: ZoneId): Boolean =
        when (notification.actions) {
            ViraActionSet.HABIT -> {
                val habitId = notification.habitId?.toUuidOrNull()
                val day = notification.dayMillis?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), zone) }
                habitId != null && day != null &&
                    inputs.habits.any { it.id == habitId && !it.isArchived && it.reminderTime != null && it.isScheduled(day) } &&
                    inputs.habitLogs.none { it.habitId == habitId && LocalDate.ofInstant(it.day, zone) == day }
            }
            ViraActionSet.MEDICATION -> {
                val medicationId = notification.medicationId?.toUuidOrNull()
                val at = notification.scheduledAtMillis?.let(Instant::ofEpochMilli)
                medicationId != null && at != null &&
                    inputs.medications.any { m ->
                        m.id == medicationId && m.hasReminder &&
                            medicationTimes(m, LocalDate.ofInstant(at, zone), zone).any { sameMinute(it, at) }
                    } &&
                    inputs.intakes.none {
                        it.medicationId == medicationId && sameMinute(it.scheduledAt, at) && it.status != IntakeStatus.PENDING
                    }
            }
            ViraActionSet.PLAN -> {
                val itemId = notification.planItemId?.toUuidOrNull()
                inputs.planItems.any { it.id == itemId && it.status == PlanItemStatus.PENDING }
            }
            ViraActionSet.VIRI -> {
                val day = LocalDate.ofInstant(notification.fireAt, zone)
                inputs.emotionEntries.none { LocalDate.ofInstant(it.createdAt, zone) == day }
            }
        }

    private fun String.toUuidOrNull() = runCatching { java.util.UUID.fromString(this) }.getOrNull()

    /// L'aparellament d'una presa amb la seva hora és **al minut**, com a l'iPhone.
    fun sameMinute(a: Instant, b: Instant) =
        a.truncatedTo(ChronoUnit.MINUTES) == b.truncatedTo(ChronoUnit.MINUTES)

    private fun Habit.isScheduled(day: LocalDate) =
        weekdays.isEmpty() || day.dayOfWeek.iosWeekday() in weekdays

    /// L'hora i el minut de `time`, en hora local, sobre el dia `day`.
    private fun at(day: LocalDate, time: Instant, zone: ZoneId): Instant {
        val clock = ZonedDateTime.ofInstant(time, zone)
        return day.atTime(clock.hour, clock.minute).atZone(zone).toInstant()
    }
}
