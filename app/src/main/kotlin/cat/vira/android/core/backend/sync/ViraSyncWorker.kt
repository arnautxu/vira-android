package cat.vira.android.core.backend.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cat.vira.android.ViraApp
import java.util.concurrent.TimeUnit

/// Sincronització amb l'app tancada (`BGAppRefreshTask` de l'iPhone). Android decideix quan; el
/// mínim que permet és cada quinze minuts, i només amb xarxa.
class ViraSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val backend = (applicationContext as ViraApp).backend
        backend.syncIfStale()
        // Sense compte no hi ha res a sincronitzar: un error d'una sessió ja tancada no es reintenta.
        if (!backend.session.isSignedIn) return Result.success()
        // Un error de xarxa o del servidor es torna a provar aviat; la resta, a la passada següent.
        val failure = (backend.engine.state.value as? ViraSyncEngine.State.Failed)?.error
        return if (failure?.isRetryable == true) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "cat.vira.android.sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ViraSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
