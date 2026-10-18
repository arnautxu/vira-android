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
        (applicationContext as ViraApp).backend.syncIfStale()
        return Result.success()
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
