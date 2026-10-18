package cat.vira.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.backend.sync.ViraSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/// Arrencada de l'app: les inicialitzacions globals (dades i sincronització; després, canals de
/// notificació) a mesura que els fils les construeixin.
class ViraApp : Application() {

    /// Dades, compte i sincronització. Es crea el primer cop que algú la demana.
    val backend: ViraBackend by lazy { ViraBackend(this) }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(ForegroundSync())
        ViraSyncWorker.schedule(this)
    }

    /// En tornar a primer pla, una passada; mentre s'hi està, una cada cinc minuts.
    private inner class ForegroundSync : DefaultLifecycleObserver {
        private var loop: Job? = null

        override fun onStart(owner: LifecycleOwner) {
            loop = owner.lifecycleScope.launch(Dispatchers.Default) { backend.syncWhileVisible() }
        }

        override fun onStop(owner: LifecycleOwner) {
            loop?.cancel()
            loop = null
        }
    }
}
