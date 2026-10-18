package cat.vira.android.features.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/// La sessió en marxa del procés, si n'hi ha.
///
/// A l'iPhone la sessió viu dins del `fullScreenCover` i la Live Activity només la mira. Aquí l'avís
/// fix té botons de debò (Pausa, Acaba) que han d'arribar a la mateixa sessió que la pantalla, i la
/// pantalla es pot tornar a crear mentre el rellotge corre: per això el controlador viu a nivell de
/// procés mentre hi ha una sessió oberta, i ningú més en té cap còpia.
object FocusRuntime {

    /// L'àmbit de les sessions: sobreviu a la pantalla, que es pot tornar a crear amb el rellotge en marxa.
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _current = MutableStateFlow<FocusSessionController?>(null)
    val current: StateFlow<FocusSessionController?> = _current.asStateFlow()

    private val _isActive = MutableStateFlow(false)

    /// Hi ha una sessió en marxa o en pausa. L'arrel la torna a obrir si l'app s'obre des de l'avís.
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun attach(controller: FocusSessionController) {
        _current.value = controller
        _isActive.value = true
    }

    /// La sessió s'ha acabat (completada o no): ja no hi ha res a tornar a obrir.
    fun ended(controller: FocusSessionController) {
        if (_current.value === controller) _isActive.value = false
    }

    /// La pantalla s'ha tancat. Una sessió en marxa no se suelta mai: només «Acaba» la tanca.
    fun release(controller: FocusSessionController) {
        if (_current.value !== controller) return
        if (controller.phase == FocusSessionController.Phase.RUNNING) return
        _current.value = null
        _isActive.value = false
    }
}

/// L'alarma de final de sessió: el que a l'iPhone és l'avís local programat.
///
/// Amb el telèfon adormit, un `delay` no avança; l'alarma sí. Exacta si el sistema ho deixa, i si
/// no, la més propera que permet el mode de repòs.
object FocusAlarm {
    private const val REQUEST = 4201

    fun schedule(context: Context, at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, FocusAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/// Quan sona l'alarma: si la sessió encara és viva, es completa; si el procés ja no hi era, només
/// queda dir-ho amb l'avís de final.
class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = FocusRuntime.current.value
        if (controller == null || controller.phase != FocusSessionController.Phase.RUNNING) {
            FocusNotifications.postEnded(context)
            return
        }
        val pending = goAsync()
        FocusRuntime.scope.launch {
            try {
                controller.reconcile()
            } finally {
                pending.finish()
            }
        }
    }
}

/// La represa des de l'avís fix: si l'app torna a primer pla amb una sessió en marxa, Concentra't
/// torna a ser a la vista. La crida l'arrel, amb `onResume` obrint la sessió.
@androidx.compose.runtime.Composable
fun FocusResumeEffect(onResume: () -> Unit) {
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val controller = FocusRuntime.current.value
        if (controller != null && controller.phase != FocusSessionController.Phase.SETUP) onResume()
    }
}
