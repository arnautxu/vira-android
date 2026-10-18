package cat.vira.android.core.backend

import android.content.Context
import android.net.Uri
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/// Les peces de dades d'una execució de l'app: la base de dades, la sessió i el motor. En viu una
/// per procés, dins de `ViraApp`; les pantalles i el treball en segon pla la demanen allà.
class ViraBackend(context: Context) {

    val db: ViraDatabase = ViraDatabase.open(context)
    val preferences = ViraPreferences.open(context)
    val session = ViraSession(
        auth = ViraBackendConfig.current?.let { ViraSupabaseAuth(context, it) },
        db = db,
        preferences = preferences,
    )
    val engine = ViraSyncEngine(session, db, preferences)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /// La sessió desada es recupera un sol cop, en arrencar el procés. Tot el que sincronitza
    /// s'hi espera: sense, no se sabria de qui són les dades.
    private val restored: Deferred<Unit> = scope.async { session.restore() }

    init {
        // Cada cop que s'obre una sessió (o canvia de compte), una passada sencera.
        scope.launch {
            restored.await()
            session.state.distinctUntilChangedBy { (it as? ViraSession.State.SignedIn)?.user?.id }.collect {
                if (it is ViraSession.State.SignedIn) engine.sync()
            }
        }
    }

    /// Tornada de `vira://auth-callback`. Va al seu propi àmbit: si la pantalla es tanca a mig
    /// intercanvi, la sessió s'ha d'acabar d'obrir igualment.
    fun handleCallback(uri: Uri, onFailure: () -> Unit = {}) {
        scope.launch {
            restored.await()
            if (!session.handleCallback(uri)) onFailure()
        }
    }

    /// Passada si fa més de cinc minuts de l'última (en tornar a primer pla i des del segon pla).
    suspend fun syncIfStale() {
        restored.await()
        engine.syncIfStale()
    }

    /// Mentre l'app és a la vista, una passada cada cinc minuts, com l'iPhone.
    suspend fun syncWhileVisible() {
        while (true) {
            syncIfStale()
            delay(ViraSyncEngine.AUTOMATIC_INTERVAL.toMillis())
        }
    }
}
