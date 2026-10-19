package cat.vira.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import cat.vira.android.app.CelebrationQueue
import cat.vira.android.app.ViraIncomingLinks
import cat.vira.android.app.processCompletionQueue
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.backend.sync.ViraSyncWorker
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.notifications.ViraNotificationChannel
import cat.vira.android.core.notifications.ViraNotificationEffects
import cat.vira.android.core.notifications.ViraNotificationHost
import cat.vira.android.core.notifications.ViraNotificationWorker
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.emotions.ViraEmotionCheckIn
import cat.vira.android.features.plan.PlanNotificationEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/// Arrencada de l'app: les inicialitzacions globals (dades i sincronització, canals i finestra
/// d'avisos) a mesura que els fils les construeixin.
class ViraApp : Application(), ViraNotificationHost {

    /// Dades, compte i sincronització. Es crea el primer cop que algú la demana.
    val backend: ViraBackend by lazy { ViraBackend(this) }

    /// Els enllaços que han obert l'app i encara no ha fet servir ningú (`ViraIncomingLinks`).
    val incomingLinks = ViraIncomingLinks()
    /// On ha de portar l'avís que s'acaba de tocar (`ViraNotifications.EXTRA_DESTINATION`). Viu al
    /// procés i no a l'activitat: l'arrel el llegeix quan ja hi és i el buida.
    internal val notificationDestination = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /// Els avisos llegeixen la mateixa base de dades que les pantalles (`ViraNotificationHost`).
    override val notificationDatabase: ViraDatabase get() = backend.db

    /// Els botons dels avisos fan el que fa cada pantalla: les hi connecta cada fil quan la seva
    /// pantalla ja escriu. Fins aleshores, aquell botó no escriu res i Viri no programa cap avís.
    override var notificationEffects = ViraNotificationEffects(
        // El mateix registre que el check-in d'Emocions, sense intensitat ni nota.
        logEmotion = { ViraEmotionCheckIn.record(ViraStore(backend.db), it) },
        // Pla: el mateix camí que la pantalla, sense «Desfer». La medicació no dona res.
        completeHabit = { id, day -> PlanNotificationEffects(ViraStore(backend.db)).completeHabit(id, day) },
        completePlanItem = { PlanNotificationEffects(ViraStore(backend.db)).completePlanItem(it) },
        recordIntake = { id, at, status -> PlanNotificationEffects(ViraStore(backend.db)).recordIntake(id, at, status) },
    )

    /// La finestra de «Desfer» i la recompensa que ve després (`ViraCompletionQueue`). És del
    /// procés perquè un gir de pantalla no se l'endugui a mitges.
    internal val completions by lazy {
        processCompletionQueue(this, ProcessLifecycleOwner.get().lifecycleScope).also { queue ->
            queue.deferStreak = { deferred -> celebrations.defer(deferred) }
        }
    }

    /// Les celebracions de ratxa, de carta i de nivell (`ViraCelebrationQueue`), també del procés:
    /// una celebració encuada o retinguda no es perd en girar la pantalla.
    internal val celebrations by lazy { CelebrationQueue(backend, ProcessLifecycleOwner.get().lifecycleScope) }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(ForegroundSync())
        ViraSyncWorker.schedule(this)
        ViraNotificationChannel.register(this)
        ViraNotificationWorker.schedule(this)
        ViraNotifications.watch(this, backend.db)
    }

    /// En tornar a primer pla, una passada; mentre s'hi està, una cada cinc minuts.
    private inner class ForegroundSync : DefaultLifecycleObserver {
        private var loop: Job? = null

        override fun onStart(owner: LifecycleOwner) {
            loop = owner.lifecycleScope.launch(Dispatchers.Default) { backend.syncWhileVisible() }
            // Cada tornada a primer pla torna a fer la finestra d'avisos, com a l'iPhone.
            owner.lifecycleScope.launch(Dispatchers.Default) { ViraNotifications.reschedule(this@ViraApp) }
        }

        override fun onStop(owner: LifecycleOwner) {
            loop?.cancel()
            loop = null
        }
    }
}
