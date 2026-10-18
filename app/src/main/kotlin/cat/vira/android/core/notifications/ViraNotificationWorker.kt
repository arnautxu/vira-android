package cat.vira.android.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

/// La passada de manteniment dels avisos: la `BGAppRefreshTask` de l'iPhone.
///
/// Que els avisos no s'acabin quan l'app no s'obre en dies, i que «t'he guardat la ratxa» arribi
/// sense obrir-la. **Una hora**, com a l'iPhone: Android ho reparteix segons la bateria, i aquí
/// només s'obre la porta; la garantia la segueix donant obrir l'app.
class ViraNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = Instant.now()
        if (inputData.getBoolean(KEY_RECONCILE, true)) ViraNotifications.reconcileInBackground(applicationContext, now)
        ViraNotifications.reschedule(applicationContext, now)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "cat.vira.notifications.refresh"
        private const val ONCE = "cat.vira.notifications.reschedule"
        private const val KEY_RECONCILE = "reconcile"

        /// S'engega a l'arrencada de l'app. `KEEP`: no reinicia el comptador a cada obertura.
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ViraNotificationWorker>(1, TimeUnit.HOURS).build(),
            )
        }

        /// Refà els avisos sense esperar-se: el que criden les pantalles després de marcar o editar.
        /// Diverses crides seguides en fan una sola (`REPLACE`).
        fun request(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONCE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ViraNotificationWorker>()
                    .setInputData(androidx.work.workDataOf(KEY_RECONCILE to false))
                    .build(),
            )
        }
    }
}

/// Android esborra totes les alarmes en apagar el telèfon, i un canvi d'hora o de zona les deixa
/// a l'hora equivocada. Aquí es tornen a posar.
class ViraNotificationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ViraNotificationScheduler(context).rearm(Instant.now())
                ViraNotificationWorker.request(context)
            }
            // L'hora local d'un avís ha canviat d'instant: s'han de recalcular, no només tornar a
            // posar, i **ara**, abans que una alarma calculada amb la zona d'abans soni a deshora.
            // També quan s'acaba de donar l'accés a les alarmes exactes: les que s'havien posat
            // aproximades es tornen a posar a l'hora justa.
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, EXACT_ALARM_GRANTED -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        ViraNotifications.reschedule(context.applicationContext)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /// `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (Android 12+).
        const val EXACT_ALARM_GRANTED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
