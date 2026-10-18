package cat.vira.android.core.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/// L'alarma ha sonat: publica l'avís amb els seus botons.
///
/// És la meitat «presentació» que iOS fa sol a partir del `UNNotificationRequest`. Els botons
/// són els de `ViraNotificationActions.Category` de l'iPhone; la resposta la rep
/// `ViraNotificationActionReceiver`.
class ViraNotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val raw = intent.getStringExtra(EXTRA_PAYLOAD) ?: return
        val notification = runCatching {
            Json.decodeFromString(ViraPlannedNotification.serializer(), raw)
        }.getOrNull() ?: return
        ViraNotificationScheduler(context).forget(notification.id)
        val pending = goAsync()
        scope.launch {
            try {
                // Mai recorda una cosa que ja no està per fer, encara que la passada no hagi arribat
                // a treure l'alarma (o que la fila sigui d'un compte que ja no hi és).
                if (!ViraNotifications.isStillPending(context.applicationContext, notification)) return@launch
                // Una notícia de Viri ja dita no es torna a programar si després es canvia l'hora de Viri.
                // Només si encara és la seva: una de més nova apuntada mentrestant no s'esborra.
                if (post(context, notification) && notification.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX)) {
                    ViraStreakNotifications.clearPendingEvent(ViraPreferences.open(context), onlyIfStamp = notification.scheduledAtMillis)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        const val EXTRA_PAYLOAD = "cat.vira.notification.payload"

        /// Publica l'avís. Sense permís (Android 13+) no es publica res i no passa res més: el
        /// permís es demana en context, quan l'usuari encén un avís, mai aquí.
        /// Retorna si s'ha publicat.
        fun post(context: Context, notification: ViraPlannedNotification): Boolean {
            if (!ViraNotifications.canPost(context) || !ViraNotifications.isWanted(context, notification)) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return false
            val shown = ViraNotifications.presented(context, notification)
            NotificationManagerCompat.from(context).notify(shown.id, 0, build(context, shown))
            return true
        }

        fun build(context: Context, notification: ViraPlannedNotification): Notification {
            val builder = NotificationCompat.Builder(context, notification.channel.id)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(
                    if (notification.channel == ViraNotificationChannel.MEDICATION) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT,
                )
                .setWhen(notification.fireAtMillis)
                .setShowWhen(true)
                .setAutoCancel(true)
                // Si es torna a publicar per treure'n el detall, que no torni a sonar.
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp(context, notification))
                // Per poder retirar-lo de la pantalla si mentrestant es marca a l'app.
                .addExtras(Bundle().apply { putString(EXTRA_PAYLOAD, Json.encodeToString(ViraPlannedNotification.serializer(), notification)) })
            actions(context, notification).forEach { builder.addAction(it) }
            return builder.build()
        }

        /// Un toc a l'avís obre la pantalla que li correspon: el de Viri porta al check-in, perquè
        /// és el que ofereix; la resta, a Inici. Qui obre la pantalla és l'arrencada de l'app
        /// (`app/`), que llegeix `EXTRA_DESTINATION`.
        private fun openApp(context: Context, notification: ViraPlannedNotification): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
            launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            launch.putExtra(
                ViraNotifications.EXTRA_DESTINATION,
                if (notification.actions == ViraActionSet.VIRI) ViraNotifications.DESTINATION_CHECK_IN
                else ViraNotifications.DESTINATION_TODAY,
            )
            // Un identificador i no una adreça (`data`): l'arrencada repartiria l'adreça com un enllaç
            // entrant, i el panell de pares la podria llegir com un codi de vinculació.
            launch.identifier = notification.id
            return PendingIntent.getActivity(
                context, notification.id.hashCode(), launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun actions(context: Context, n: ViraPlannedNotification): List<NotificationCompat.Action> =
            when (n.actions) {
                ViraActionSet.HABIT -> listOf(
                    action(context, n, ViraNotificationActions.HABIT_DONE, R.string.notification_action_done),
                    action(context, n, ViraNotificationActions.SNOOZE, R.string.notification_action_snooze),
                )
                ViraActionSet.PLAN -> listOf(
                    action(context, n, ViraNotificationActions.PLAN_DONE, R.string.notification_action_done),
                    action(context, n, ViraNotificationActions.SNOOZE, R.string.notification_action_snooze),
                )
                // Els botons diuen «Presa», mai el nom del fàrmac.
                ViraActionSet.MEDICATION -> listOf(
                    action(context, n, ViraNotificationActions.MEDICATION_TAKEN, R.string.notification_action_taken),
                    action(context, n, ViraNotificationActions.MEDICATION_SKIPPED, R.string.notification_action_skipped),
                    action(context, n, ViraNotificationActions.SNOOZE, R.string.notification_action_snooze),
                )
                // Android n'ensenya tres com a molt; l'iPhone en porta quatre
                // (`ViraNotificationActions.viriButtons`).
                ViraActionSet.VIRI -> ViraNotificationActions.viriButtons.map { button ->
                    when (button) {
                        is ViraNotificationActions.ViriButton.Feeling ->
                            action(context, n, ViraNotificationActions.EMOTION_PREFIX + button.emotion.raw, button.label)
                        ViraNotificationActions.ViriButton.Capture ->
                            action(context, n, ViraNotificationActions.CAPTURE, R.string.notification_action_capture)
                                .let { base ->
                                    NotificationCompat.Action.Builder(base)
                                        .addRemoteInput(
                                            RemoteInput.Builder(ViraNotificationActions.EXTRA_TEXT)
                                                .setLabel(context.getString(R.string.intent_capture_ask))
                                                .build(),
                                        )
                                        .build()
                                }
                    }
                }
            }

        private fun action(context: Context, n: ViraPlannedNotification, action: String, label: Int): NotificationCompat.Action {
            val intent = Intent(context, ViraNotificationActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("vira-notification://action/${Uri.encode(n.id)}/$action"))
                .putExtra(EXTRA_PAYLOAD, Json.encodeToString(ViraPlannedNotification.serializer(), n))
            // El camp de text necessita un `PendingIntent` mutable perquè el sistema hi pugui posar
            // el que s'ha escrit; la resta, immutables.
            val mutability = if (action == ViraNotificationActions.CAPTURE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_IMMUTABLE
            }
            val pending = PendingIntent.getBroadcast(
                context, (n.id + action).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or mutability,
            )
            return NotificationCompat.Action.Builder(0, context.getString(label), pending).build()
        }
    }
}
