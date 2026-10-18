package cat.vira.android.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import cat.vira.android.R
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.IntakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/// El que es pot fer **des de l'avís**, sense obrir Vira (`ViraNotificationActions` de l'iPhone).
///
/// - **Hàbit** i **tasca**: Fet · Més tard (10 min).
/// - **Medicació**: Presa · Salta-la · Més tard. Cap recompensa, com a tot arreu.
/// - **Viri** (l'avís del vespre i el del registre emocional): emocions de resposta ràpida.
///
/// Escriu dades sensibles des de la pantalla bloquejada (Arnau, 2026-08-10). El que es conserva és
/// que el **contingut** no hi surt: els botons diuen «Presa», mai el nom del fàrmac.
object ViraNotificationActions {

    const val HABIT_DONE = "habit.done"
    const val PLAN_DONE = "plan.done"
    const val MEDICATION_TAKEN = "medication.taken"
    const val MEDICATION_SKIPPED = "medication.skipped"
    const val SNOOZE = "snooze"
    const val CAPTURE = "capture"
    /// `emotion.` + el valor en cru de l'`Emotion`.
    const val EMOTION_PREFIX = "emotion."

    const val EXTRA_TEXT = "cat.vira.notification.text"

    /// Quant s'ajorna un «Més tard»: prou perquè serveixi i prou poc perquè no es mengi el dia.
    const val SNOOZE_SECONDS = 10L * 60

    /// Un botó de l'avís de Viri.
    sealed interface ViriButton {
        data class Feeling(val emotion: Emotion, @StringRes val label: Int) : ViriButton
        data object Capture : ViriButton
    }

    /// Els de l'iPhone són Content · Ansiós · Trist · «Apunta una cosa», però Android n'ensenya
    /// tres com a molt. Mentre l'Arnau no triï, es queden les tres emocions: tots dos avisos
    /// pregunten com estàs, i apuntar una cosa segueix a un toc obrint l'app.
    val viriButtons: List<ViriButton> = listOf(
        ViriButton.Feeling(Emotion.CONTENT, R.string.emotion_content),
        ViriButton.Feeling(Emotion.ANXIOUS, R.string.emotion_anxious),
        ViriButton.Feeling(Emotion.SAD, R.string.emotion_sad),
    )

    /// La resposta de l'usuari, reduïda a valors.
    data class Request(val action: String, val notification: ViraPlannedNotification, val text: String? = null) {
        val habitId: UUID? get() = notification.habitId?.toUuid()
        val medicationId: UUID? get() = notification.medicationId?.toUuid()
        val planItemId: UUID? get() = notification.planItemId?.toUuid()
        val day: Instant? get() = notification.dayMillis?.let(Instant::ofEpochMilli)
        val scheduledAt: Instant? get() = notification.scheduledAtMillis?.let(Instant::ofEpochMilli)
    }

    /// Aplica la resposta i refà la finestra: marcar una cosa canvia què cal recordar.
    suspend fun handle(context: Context, request: Request, now: Instant = Instant.now()) {
        val host = context.notificationHost ?: return
        val effects = host.notificationEffects
        when {
            request.action == HABIT_DONE -> {
                val id = request.habitId ?: return
                effects.completeHabit(id, request.day ?: return)
            }
            request.action == PLAN_DONE -> effects.completePlanItem(request.planItemId ?: return)
            request.action == MEDICATION_TAKEN || request.action == MEDICATION_SKIPPED -> {
                val id = request.medicationId ?: return
                val status = if (request.action == MEDICATION_TAKEN) IntakeStatus.TAKEN else IntakeStatus.SKIPPED
                val at = request.scheduledAt ?: return
                // Ja respost: el botó no el canvia, i l'avís desapareix amb la passada.
                if (!ViraNotifications.intakeResolved(context, id, at)) effects.recordIntake(id, at, status)
            }
            request.action == SNOOZE -> snooze(context, request.notification, now)
            request.action == CAPTURE -> {
                val text = request.text?.trim().orEmpty()
                if (text.isNotEmpty()) effects.capture(text)
            }
            request.action.startsWith(EMOTION_PREFIX) -> {
                val raw = request.action.removePrefix(EMOTION_PREFIX)
                Emotion.entries.firstOrNull { it.raw == raw }?.let { effects.logEmotion(it) }
            }
            else -> return
        }
        ViraNotifications.reschedule(context, now)
        effects.afterChange()
    }

    /// Torna a posar **el mateix avís** d'aquí a deu minuts, amb el mateix text i botons.
    fun snoozed(notification: ViraPlannedNotification, now: Instant) = notification.copy(
        id = notification.id.removeSuffix(ViraNotificationScheduler.SNOOZE_SUFFIX) + ViraNotificationScheduler.SNOOZE_SUFFIX,
        fireAtMillis = now.plusSeconds(SNOOZE_SECONDS).toEpochMilli(),
    )

    private fun snooze(context: Context, notification: ViraPlannedNotification, now: Instant) {
        ViraNotificationScheduler(context).add(snoozed(notification, now))
    }

    private fun String.toUuid(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
}

/// Rep el toc a un botó de l'avís. Sense estat: tot el que necessita viatja a l'`Intent`.
class ViraNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val raw = intent.getStringExtra(ViraNotificationAlarmReceiver.EXTRA_PAYLOAD) ?: return
        val notification = runCatching {
            Json.decodeFromString(ViraPlannedNotification.serializer(), raw)
        }.getOrNull() ?: return
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(ViraNotificationActions.EXTRA_TEXT)?.toString()

        // Respost: l'avís desapareix, com a l'iPhone.
        NotificationManagerCompat.from(context).cancel(notification.id, 0)

        val pending = goAsync()
        scope.launch {
            try {
                ViraNotificationActions.handle(context.applicationContext, ViraNotificationActions.Request(action, notification, text))
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
