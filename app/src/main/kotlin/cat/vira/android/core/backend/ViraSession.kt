package cat.vira.android.core.backend

import android.app.Activity
import android.content.SharedPreferences
import android.net.Uri
import cat.vira.android.core.backend.sync.ViraSyncCursors
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/// Sessió viva de l'app: qui hi ha connectat i com renovar-ho (`ViraSession` de l'iPhone).
///
/// No és un singleton: es crea a l'arrencada i baixa a les pantalles. Les proves en fan una de pròpia
/// amb un `ViraAuthBackend` fals.
class ViraSession(
    private val auth: ViraAuthBackend?,
    private val db: ViraDatabase,
    private val preferences: SharedPreferences,
    private val rest: ViraRestClient = ViraRestClient(),
    private val clock: () -> Long = System::currentTimeMillis,
    /// El que els altres fils hagin de netejar quan entra un altre compte (avisos, widgets).
    private val alsoEraseOnAccountSwitch: suspend () -> Unit = {},
) {

    sealed interface State {
        /// Encara no s'ha mirat el magatzem: la primera pantalla no s'ha de decidir a cegues.
        data object Loading : State
        data object SignedOut : State
        data class SignedIn(val user: ViraUser) : State
        /// Sense backend: l'app funciona en local, com abans dels comptes.
        data object Unconfigured : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    /// Últim error mostrable. El posa qui inicia sessió; la pantalla el neteja en reintentar.
    private val _lastError = MutableStateFlow<ViraBackendError?>(null)
    val lastError: StateFlow<ViraBackendError?> = _lastError.asStateFlow()

    /// L'enllaç del correu de recuperació ha obert sessió i encara no s'ha escrit la contrasenya
    /// nova. Mentre sigui cert, el flux d'entrada tapa la resta amb el pas de contrasenya nova.
    private val _isRecoveringPassword = MutableStateFlow(false)
    val isRecoveringPassword: StateFlow<Boolean> = _isRecoveringPassword.asStateFlow()

    /// Invalida la feina pendent (una sincronització a mig fer) en canviar o tancar sessió, fins i
    /// tot si es torna a entrar al mateix compte.
    @Volatile var dataGeneration: UUID = UUID.randomUUID()
        private set

    private var stored: ViraStoredSession? = null

    val user: ViraUser? get() = (state.value as? State.SignedIn)?.user
    val isSignedIn: Boolean get() = user != null
    val requiresAccount: Boolean get() = auth != null

    fun clearError() { _lastError.value = null }

    // Cicle de vida

    /// En arrencar. Si el testimoni ha caducat amb l'app tancada, el renova en silenci.
    suspend fun restore() {
        val auth = auth ?: run { _state.value = State.Unconfigured; return }
        val saved = auth.storedSession() ?: run { _state.value = State.SignedOut; return }
        stored = saved
        claimLocalData(saved.user, isNewSignIn = false)
        _state.value = State.SignedIn(saved.user)
        if (saved.expiresAtEpochSeconds - clock() / 1000 < 60) {
            runCatching { accessToken() }
        }
    }

    suspend fun signIn(email: String, password: String) = attempt { adopt(requireAuth().signIn(email, password)) }

    /// Alta amb correu. Torna `false` quan cal confirmar el correu abans d'entrar.
    suspend fun signUp(email: String, password: String, role: ViraAccountRole, consent: ViraConsentStatus): Boolean =
        attempt {
            val session = requireAuth().signUp(email, password, ViraUserMetadata(role, consent))
            if (session != null) adopt(session)
            session != null
        } ?: false

    suspend fun startOAuth(provider: ViraOAuthProvider) = attempt { requireAuth().startOAuth(provider) }

    /// Google: primer el selector de comptes del telèfon i, si no es pot, el navegador.
    suspend fun signInWithGoogle(activity: Activity) {
        val clientId = ViraBackendConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId == null) {
            startOAuth(ViraOAuthProvider.GOOGLE)
            return
        }
        when (val result = ViraGoogleSignIn(clientId).request(activity)) {
            is ViraGoogleSignIn.Result.Token ->
                attempt { adopt(requireAuth().signInWithGoogleIdToken(result.idToken, result.rawNonce)) }
            ViraGoogleSignIn.Result.Cancelled -> Unit
            ViraGoogleSignIn.Result.Unavailable -> startOAuth(ViraOAuthProvider.GOOGLE)
        }
    }

    suspend fun sendPasswordReset(email: String) = attempt {
        requireAuth().sendPasswordReset(email)
        expectPasswordRecovery()
    }

    /// Tornada d'un enllaç que obre l'app (`vira://auth-callback?code=…`): Google, Apple, la
    /// confirmació del correu o la recuperació de la contrasenya.
    ///
    /// Amb PKCE, la tornada d'una recuperació i la d'una alta confirmada són el mateix URL. Qui les
    /// distingeix és la marca que es deixa en demanar el correu, vàlida una hora.
    suspend fun handleCallback(uri: Uri) {
        if (uri.scheme != ViraBackendConfig.CALLBACK_SCHEME || uri.host != ViraBackendConfig.CALLBACK_HOST) return
        val isRecovery = uri.getQueryParameter("type") == "recovery" || hasPendingRecoveryRequest()
        val adopted = attempt {
            adopt(requireAuth().session(fromCallback = uri))
            if (isRecovery) _isRecoveringPassword.value = true
        }
        // Només si ha anat bé: un reintent sense xarxa sovint torna sense `type=recovery`.
        if (adopted != null) preferences.edit().remove(PASSWORD_RECOVERY_REQUESTED_AT).apply()
    }

    suspend fun updatePassword(password: String) {
        requireAuth().updatePassword(password)
        _isRecoveringPassword.value = false
    }

    fun cancelPasswordRecovery() { _isRecoveringPassword.value = false }

    suspend fun signOut() {
        runCatching { auth?.signOut() }
        runCatching { auth?.forgetSession() }
        clearLocalSession()
    }

    /// Esborra el compte al servidor (`delete_current_user()`: GoTrue no ho ofereix des del client).
    /// L'esborrat local el fa qui ho demana, amb `ViraDataEraser`.
    suspend fun deleteAccount() {
        val token = accessToken()
        rest.rpc("delete_current_user", JsonObject(emptyMap()), token)
        // El compte ja no existeix: la sessió desada tampoc, o tornaria en reobrir l'app.
        runCatching { auth?.forgetSession() }
        clearLocalSession()
    }

    /// Testimoni vàlid per a qualsevol crida. Si el refresc falla de debò, la sessió ja no existeix
    /// al servidor i la interfície ho ha de saber de seguida.
    suspend fun accessToken(): String {
        val auth = auth ?: throw ViraBackendException(ViraBackendError.NOT_CONFIGURED)
        if (stored == null) throw ViraBackendException(ViraBackendError.SESSION_EXPIRED)
        val generation = dataGeneration
        try {
            val renewed = auth.session()
            if (generation != dataGeneration) throw CancellationException("Ha canviat el compte")
            stored = renewed
            // El refresc porta les metadades d'ara: un permís del tutor concedit o retirat en un
            // altre lloc ha d'arribar a `ViraSyncScope` sense tornar a entrar.
            if (user != renewed.user) _state.value = State.SignedIn(renewed.user)
            return renewed.accessToken
        } catch (error: CancellationException) {
            throw error
        } catch (error: ViraBackendException) {
            if (generation != dataGeneration) throw CancellationException("Ha canviat el compte")
            // Sense xarxa o amb el servidor caigut un moment, la sessió segueix sent bona: només es
            // tanca quan el servidor diu que no.
            if (error.error.isRetryable) throw error
            invalidate()
            throw ViraBackendException(ViraBackendError.SESSION_EXPIRED, error)
        }
    }

    suspend fun updateRole(role: ViraAccountRole) = updateMetadata(ViraUserMetadata(role, user?.consent))

    suspend fun updateConsent(consent: ViraConsentStatus) = updateMetadata(ViraUserMetadata(user?.role, consent))

    /// Sessió invalidada des de fora (testimoni revocat, compte esborrat en un altre telèfon).
    fun invalidate() {
        clearLocalSession()
        _lastError.value = ViraBackendError.SESSION_EXPIRED
    }

    // Privat

    private suspend fun updateMetadata(metadata: ViraUserMetadata) {
        val updated = requireAuth().updateMetadata(metadata)
        stored = stored?.copy(user = updated)
        _state.value = State.SignedIn(updated)
    }

    private suspend fun adopt(session: ViraStoredSession) {
        stored = session
        _lastError.value = null
        // Primer es decideix de qui són les dades del telèfon, i només després es diu a la interfície
        // que hi ha sessió. Al revés, hi hauria un instant amb les dades de qui hi havia abans.
        claimLocalData(session.user, isNewSignIn = true)
        _state.value = State.SignedIn(session.user)
    }

    /// Decideix de qui són les dades que hi ha ara al telèfon (`claimLocalData` de l'iPhone).
    ///
    /// 1. **Si el compte és un altre**, s'esborra tot el que hi havia. Només llavors.
    /// 2. **Sempre que s'obre sessió**, s'obliden els cursors perquè la passada següent es baixi el
    ///    compte sencer, i no només el que hagi canviat des de l'última vegada que *aquest telèfon*
    ///    va sincronitzar, que pot ser d'algú altre.
    private suspend fun claimLocalData(user: ViraUser, isNewSignIn: Boolean) {
        dataGeneration = UUID.randomUUID()
        val previous = preferences.getString(ViraPreferences.DATA_OWNER_ACCOUNT_ID, null)
        if (ViraDataEraser.isAccountSwitch(previous, user.id)) {
            ViraDataEraser.eraseForAccountSwitch(db, preferences, alsoEraseOnAccountSwitch)
        }
        // Després de l'esborrat, que se n'emporta aquesta clau com totes les altres.
        preferences.edit().putString(ViraPreferences.DATA_OWNER_ACCOUNT_ID, user.id.toString()).commit()
        if (isNewSignIn) ViraSyncCursors(preferences).reset()
    }

    private fun clearLocalSession() {
        dataGeneration = UUID.randomUUID()
        stored = null
        _isRecoveringPassword.value = false
        _state.value = if (auth != null) State.SignedOut else State.Unconfigured
    }

    private fun requireAuth() = auth ?: throw ViraBackendException(ViraBackendError.NOT_CONFIGURED)

    /// Les crides que inicia una pantalla: l'error es queda a `lastError` en el vocabulari de Vira.
    private suspend fun <T> attempt(work: suspend () -> T): T? = try {
        work()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ViraBackendException) {
        _lastError.value = error.error
        null
    }

    private fun expectPasswordRecovery() {
        preferences.edit().putLong(PASSWORD_RECOVERY_REQUESTED_AT, clock()).apply()
    }

    /// Una hora és el que dura un enllaç de GoTrue.
    private fun hasPendingRecoveryRequest(): Boolean {
        val requested = preferences.getLong(PASSWORD_RECOVERY_REQUESTED_AT, 0)
        return requested > 0 && clock() - requested < 3_600_000
    }

    companion object {
        const val PASSWORD_RECOVERY_REQUESTED_AT = "account.passwordRecoveryRequestedAt"
    }
}
