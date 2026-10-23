package cat.vira.android.core.backend

import android.content.Context
import android.net.Uri
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.core.resources.ViraLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/// Les peces de dades d'una execució de l'app: la base de dades, la sessió i el motor. En viu una
/// per procés, dins de `ViraApp`; les pantalles i el treball en segon pla la demanen allà.
class ViraBackend(
    context: Context,
    /// Després d'aplicar el comptador o la ratxa que baixen del compte (`ViraSyncEngine`): l'arrel
    /// hi dona per vistes les cartes que ja es van guanyar en un altre telèfon.
    afterProgressApplied: suspend (ViraDatabase) -> Unit = {},
) {

    private val appContext: Context = context.applicationContext

    val db: ViraDatabase = ViraDatabase.open(context)
    val preferences = ViraPreferences.open(context)
    val session = ViraSession(
        auth = ViraBackendConfig.current?.let { ViraSupabaseAuth(context, it) },
        db = db,
        preferences = preferences,
        // Cap avís del compte d'abans: s'han programat amb les seves dades.
        alsoEraseOnAccountSwitch = { ViraNotifications.cancelEverything(appContext) },
        language = { ViraLanguage.current(appContext) },
    )
    val engine = ViraSyncEngine(session, db, preferences, afterProgressApplied = afterProgressApplied)

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
    ///
    /// Cada enllaç es fa servir un sol cop per procés, encara que l'activitat es recreï a mig
    /// intercanvi (un gir de pantalla); si falla (sense xarxa), es pot tornar a provar.
    fun handleCallback(uri: Uri) {
        val key = uri.toString()
        if (callbacks.putIfAbsent(key, false) != null) return
        scope.launch {
            restored.await()
            if (session.handleCallback(uri)) {
                callbacks[key] = true
                lastAdoptedCallback = key
            } else {
                callbacks.remove(key)
            }
        }
    }

    /// L'últim enllaç que ha obert sessió. L'activitat el desa perquè, si Android tanca el procés,
    /// en tornar no el reenviï; un que estigués a mig fer no hi és i es torna a provar.
    @Volatile var lastAdoptedCallback: String? = null
        private set

    fun rememberAdoptedCallback(key: String?) {
        key ?: return
        callbacks[key] = true
        lastAdoptedCallback = key
    }

    /// Enllaç → si ja ha obert sessió (`true`) o encara és en marxa (`false`).
    private val callbacks = ConcurrentHashMap<String, Boolean>()

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
