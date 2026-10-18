package cat.vira.android.features.focus

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusSession
import java.time.Instant

/// El que una sessió fa fora de si mateixa: l'avís fix, l'alarma de final, el so i les monedes.
///
/// Separat de `FocusSessionController` perquè les proves de la lògica no engeguin serveis ni
/// alarmes: allà es fa servir `None`.
interface FocusEffects {
    fun started(controller: FocusSessionController)
    fun paused(controller: FocusSessionController)
    fun resumed(controller: FocusSessionController)
    fun finished(controller: FocusSessionController, completed: Boolean, endsNow: Boolean, kind: FocusKind)

    /// Les monedes d'una sessió completada. Torna les que s'han concedit.
    suspend fun reward(session: FocusSession, now: Instant): Int

    object None : FocusEffects {
        override fun started(controller: FocusSessionController) = Unit
        override fun paused(controller: FocusSessionController) = Unit
        override fun resumed(controller: FocusSessionController) = Unit
        override fun finished(controller: FocusSessionController, completed: Boolean, endsNow: Boolean, kind: FocusKind) = Unit
        override suspend fun reward(session: FocusSession, now: Instant) = 0
    }
}

/// Els efectes de debò: el servei en primer pla (que substitueix la Live Activity), l'alarma de
/// final (que substitueix l'avís local programat de l'iPhone) i les recompenses.
class AndroidFocusEffects(context: Context, private val store: ViraStore) : FocusEffects {
    private val context = context.applicationContext

    override fun started(controller: FocusSessionController) {
        FocusRuntime.attach(controller)
        FocusSound.play(context, FocusSound.Cue.START)
        FocusService.show(context)
        controller.state.endDate?.let { FocusAlarm.schedule(context, it) }
    }

    override fun paused(controller: FocusSessionController) {
        FocusSound.play(context, FocusSound.Cue.PAUSE)
        FocusAlarm.cancel(context)
        FocusService.refresh(context)
    }

    override fun resumed(controller: FocusSessionController) {
        FocusSound.play(context, FocusSound.Cue.RESUME)
        // Amb l'hora de final nova: la pausa l'ha endarrerida.
        controller.state.endDate?.let { FocusAlarm.schedule(context, it) }
        FocusService.refresh(context)
    }

    override fun finished(controller: FocusSessionController, completed: Boolean, endsNow: Boolean, kind: FocusKind) {
        FocusRuntime.ended(controller)
        FocusAlarm.cancel(context)
        FocusService.dismiss(context)
        if (!completed) return
        // Amb l'app a la vista sonen les campanes; fora, l'avís de final, que és el que fa l'iPhone
        // quan l'app no hi és. Una sessió aturada a mitges no es celebra, tampoc d'orella.
        val visible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        when {
            !visible -> FocusNotifications.postEnded(context)
            endsNow -> FocusSound.play(context, if (kind == FocusKind.FOCUS) FocusSound.Cue.END else FocusSound.Cue.BREAK_END)
        }
    }

    override suspend fun reward(session: FocusSession, now: Instant): Int = FocusProgress.reward(store, session, now)
}
