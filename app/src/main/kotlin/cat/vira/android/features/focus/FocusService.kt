package cat.vira.android.features.focus

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import cat.vira.android.R
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.FocusKind
import kotlinx.coroutines.launch

/// L'avís fix de la sessió en marxa, amb el compte enrere i els botons Pausa/Segueix i Acaba: el
/// que a l'iPhone és la Live Activity (`FocusLiveActivity.swift`).
///
/// És un servei en primer pla perquè Android no mati la sessió mentre l'app és fora de la vista.
/// No porta res sensible: la fase (o el nom del mode) i el temps, com la Live Activity.
class FocusService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = FocusRuntime.current.value
        when (intent?.action) {
            ACTION_PAUSE -> controller?.pause()
            ACTION_RESUME -> controller?.resume()
            ACTION_FINISH -> controller?.let { FocusRuntime.scope.launch { it.stop() } }
        }
        val live = FocusRuntime.current.value
        if (live == null || live.phase != FocusSessionController.Phase.RUNNING) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = FocusNotifications.session(this, live)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, FocusNotifications.SESSION_ID, notification, type)
        // Si la sessió és fora del procés (o ja s'ha acabat), no es torna a engegar sola.
        return START_NOT_STICKY
    }

    companion object {
        private const val ACTION_SHOW = "cat.vira.android.focus.SHOW"
        internal const val ACTION_PAUSE = "cat.vira.android.focus.PAUSE"
        internal const val ACTION_RESUME = "cat.vira.android.focus.RESUME"
        internal const val ACTION_FINISH = "cat.vira.android.focus.FINISH"

        /// Es crida en prémer «Comença», amb l'app a la vista: és l'únic moment en què Android
        /// deixa engegar un servei en primer pla.
        fun show(context: Context) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_SHOW))
        }

        /// Torna a dibuixar l'avís (pausa, represa). El servei ja és en marxa.
        fun refresh(context: Context) {
            runCatching { context.startService(intent(context, ACTION_SHOW)) }
        }

        fun dismiss(context: Context) {
            context.stopService(Intent(context, FocusService::class.java))
        }

        internal fun intent(context: Context, action: String) =
            Intent(context, FocusService::class.java).setAction(action)
    }
}

/// Els dos avisos de Concentra't: el fix de la sessió i el de final quan l'app no és a la vista.
object FocusNotifications {
    const val SESSION_ID = 4200
    private const val ENDED_ID = 4202
    private const val SESSION_CHANNEL = "focus.session"
    private const val ENDED_CHANNEL = "focus.ended"

    /// El fix: sense so ni vibració, que és una sessió per concentrar-se.
    fun session(context: Context, controller: FocusSessionController): Notification {
        ensureChannels(context)
        val state = controller.state
        val paused = state.run == FocusTimerState.Run.PAUSED
        val title = when {
            paused -> context.getString(R.string.focus_state_paused)
            state.kind == FocusKind.FOCUS -> controller.mode.label(context)
            state.kind == FocusKind.SHORT_BREAK -> context.getString(R.string.focus_kind_short_break)
            else -> context.getString(R.string.focus_kind_long_break)
        }
        val now = ViraTime.now()
        val planned = state.plannedSeconds / 60
        val done = minOf(planned, (state.elapsed(now) / 60).toInt())
        val builder = NotificationCompat.Builder(context, SESSION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.focus_progress, done, planned))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(context))
        val end = state.endDate
        if (!paused && end != null) {
            // El compte enrere el porta el sistema, sense redibuixar res cada segon.
            builder.setUsesChronometer(true).setChronometerCountDown(true).setWhen(end.toEpochMilli()).setShowWhen(true)
        }
        if (paused) {
            builder.addAction(0, context.getString(R.string.focus_resume), action(context, FocusService.ACTION_RESUME, 1))
        } else {
            builder.addAction(0, context.getString(R.string.focus_pause), action(context, FocusService.ACTION_PAUSE, 2))
        }
        builder.addAction(0, context.getString(R.string.focus_finish), action(context, FocusService.ACTION_FINISH, 3))
        return builder.build()
    }

    /// «Sessió acabada», genèric com a l'iPhone: no diu de quina tasca era.
    fun postEnded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, ENDED_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_generic_title))
            .setContentText(context.getString(R.string.focus_notification_body))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(ENDED_ID, notification)
    }

    /// Es pot avisar? Sense el permís, el compte enrere segueix, però la pantalla ho diu.
    fun areEnabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(SESSION_CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(SESSION_CHANNEL, context.getString(R.string.focus_title), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
        }
        if (manager.getNotificationChannel(ENDED_CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(ENDED_CHANNEL, context.getString(R.string.focus_notification_body), NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    private fun action(context: Context, name: String, request: Int): PendingIntent = PendingIntent.getService(
        context, request, FocusService.intent(context, name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openApp(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
