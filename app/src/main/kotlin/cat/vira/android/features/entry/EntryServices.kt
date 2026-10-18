package cat.vira.android.features.entry

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.PlanItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Tot el que el flux d'entrada demana a la resta de l'app: el compte, el permís, la primera tasca
/// i el comptador. Darrere hi ha `ViraBackend` (`live`); a les captures, una versió que no toca ni
/// la xarxa ni la base de dades (`preview`).
///
/// No és una capa nova sobre el backend: són les mateixes crides de `ViraSession` i `ViraStore`,
/// reunides perquè les pantalles es puguin dibuixar soles.
interface EntryServices {
    val prefs: EntryPreferences
    val requiresAccount: Boolean
    val state: StateFlow<ViraSession.State>
    val isRecoveringPassword: StateFlow<Boolean>
    /// L'últim error d'entrar, registrar-se o demanar l'enllaç (`ViraSession.lastError`).
    val lastError: StateFlow<ViraBackendError?>
    /// El telèfon té bloqueig de pantalla: sense, el bloqueig de l'app deixaria l'usuari fora.
    val canLockApp: Boolean

    val user: ViraUser? get() = (state.value as? ViraSession.State.SignedIn)?.user

    fun clearError()
    suspend fun signIn(email: String, password: String)
    /// Torna `true` si la sessió s'ha obert; `false` si cal confirmar el correu o hi ha error.
    suspend fun signUp(email: String, password: String, role: ViraAccountRole): Boolean
    suspend fun signInWithGoogle(activity: Activity)
    /// Un compte nou de Google (o del navegador) no porta cap rol a les metadades i neix jove: qui
    /// ha triat "Sóc pare o mare" el posa aquí. Mai treu el rol de pare a ningú.
    suspend fun adoptParentRole()
    suspend fun sendPasswordReset(email: String)
    /// Llança `ViraBackendException` si no es pot.
    suspend fun updatePassword(password: String)
    /// Llança `ViraBackendException` si no es pot.
    suspend fun declareConsent(status: ViraConsentStatus, guardianEmail: String?)

    /// Crea la tasca pel camí de la captura ràpida. Torna el títol desat, o `null` si era buit.
    suspend fun addTask(title: String): String?
    /// L'estat del comptador ara. **No el crea** si no existeix: mirar no ha d'inserir res.
    suspend fun rewardSnapshot(): EntryReward
    /// Les sessions de focus començades des de `since`: si n'hi ha cap de completada.
    /// `null` si no n'hi ha cap (es va tancar abans d'arrencar).
    suspend fun focusCompletedSince(since: Instant): Boolean?
    suspend fun displayName(): String
    suspend fun setDisplayName(name: String)

    companion object {
        fun live(context: Context, backend: ViraBackend): EntryServices = LiveEntryServices(context, backend)
        fun preview(
            requiresAccount: Boolean = true,
            user: ViraUser? = null,
            prefs: EntryPreferences = EntryPreferences.inMemory(),
        ): EntryServices = PreviewEntryServices(requiresAccount, user, prefs)
    }
}

private class LiveEntryServices(context: Context, private val backend: ViraBackend) : EntryServices {
    private val session = backend.session
    private val store = ViraStore(backend.db)

    override val prefs = EntryPreferences.shared(backend.preferences)
    override val requiresAccount get() = session.requiresAccount
    override val state get() = session.state
    override val isRecoveringPassword get() = session.isRecoveringPassword
    override val lastError get() = session.lastError
    override val canLockApp: Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    override fun clearError() = session.clearError()
    override suspend fun signIn(email: String, password: String) { session.signIn(email, password) }
    override suspend fun signUp(email: String, password: String, role: ViraAccountRole) =
        session.signUp(email, password, role, ViraConsentStatus.MISSING)
    override suspend fun signInWithGoogle(activity: Activity) { session.signInWithGoogle(activity) }
    override suspend fun adoptParentRole() {
        if (session.user?.role == ViraAccountRole.PARENT) return
        try {
            session.updateRole(ViraAccountRole.PARENT)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Es queda jove; la porta del compte no se salta per això.
        }
    }
    override suspend fun sendPasswordReset(email: String) { session.sendPasswordReset(email) }
    override suspend fun updatePassword(password: String) { session.updatePassword(password) }
    override suspend fun declareConsent(status: ViraConsentStatus, guardianEmail: String?) {
        session.declareConsent(status, guardianEmail)
    }

    override suspend fun addTask(title: String): String? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        return store.create(backend.db.planItems(), PlanItem(title = trimmed, day = today)).title
    }

    override suspend fun rewardSnapshot(): EntryReward {
        val counter = backend.db.progressCounters().all().firstOrNull()
        return EntryReward(counter?.coinsEarnedTotal ?: 0, counter?.totalXP ?: 0)
    }

    override suspend fun focusCompletedSince(since: Instant): Boolean? {
        val sessions = backend.db.focusSessions().all().filter { !it.startedAt.isBefore(since) }
        return if (sessions.isEmpty()) null else sessions.any { it.completed }
    }

    override suspend fun displayName(): String = backend.db.profiles().all().firstOrNull()?.displayName.orEmpty()

    override suspend fun setDisplayName(name: String) {
        store.save(backend.db.profiles(), store.profile().copy(displayName = name))
    }
}

private class PreviewEntryServices(
    override val requiresAccount: Boolean,
    user: ViraUser?,
    override val prefs: EntryPreferences,
) : EntryServices {
    override val state = MutableStateFlow(
        when {
            !requiresAccount -> ViraSession.State.Unconfigured
            user != null -> ViraSession.State.SignedIn(user)
            else -> ViraSession.State.SignedOut
        },
    )
    override val isRecoveringPassword = MutableStateFlow(false)
    override val lastError = MutableStateFlow<ViraBackendError?>(null)
    override val canLockApp = true
    private var name = ""

    override fun clearError() { lastError.value = null }
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signUp(email: String, password: String, role: ViraAccountRole) = false
    override suspend fun signInWithGoogle(activity: Activity) = Unit
    override suspend fun adoptParentRole() = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun updatePassword(password: String) = Unit
    override suspend fun declareConsent(status: ViraConsentStatus, guardianEmail: String?) = Unit
    override suspend fun addTask(title: String) = title.trim().ifEmpty { null }
    override suspend fun rewardSnapshot() = EntryReward.NONE
    override suspend fun focusCompletedSince(since: Instant): Boolean? = null
    override suspend fun displayName() = name
    override suspend fun setDisplayName(name: String) { this.name = name }
}

/// L'hora d'ara, amb la mateixa precisió que desa la base de dades.
internal fun entryNow(): Instant = ViraTime.now()
