package cat.vira.android.features.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.collectAsState
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDataExport
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.settings.export.ViraDataExportPdf
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/// El que les pantalles d'Ajustos llegeixen i fan fora d'elles mateixes: la sessió, la
/// sincronització, les dades del telèfon i les preferències.
///
/// Dues implementacions: la de l'app (`ViraSettingsBackend`, sobre `ViraBackend`) i la de les
/// captures (`PreviewSettingsBackend`), que no toca ni disc ni xarxa.
interface SettingsBackend {
    val preferences: SharedPreferences
    val session: StateFlow<ViraSession.State>
    val syncState: StateFlow<ViraSyncEngine.State>
    /// El pseudònim, buit si encara no n'hi ha.
    val profileName: Flow<String>
    /// La clau de la skin posada, o `null` per al Viri de sempre.
    val equippedSkin: Flow<String?>
    val streakVisible: Flow<Boolean>
    val focusProfiles: Flow<List<FocusProfile>>
    /// «0.1 (1)», com «versió (build)» a l'iPhone.
    val appVersion: String

    suspend fun syncNow()
    suspend fun signOut()
    /// Esborra el compte al servidor i, si ha anat bé, tot el que hi ha al telèfon.
    suspend fun deleteAccount()
    /// «Esborrar-ho tot»: amb compte, primer el servidor (`ViraSyncEngine.eraseEverything`).
    suspend fun eraseEverything()
    suspend fun setStreakVisible(visible: Boolean)
    /// Arxivar, no esborrar: l'historial de sessions conserva el nom del mode.
    suspend fun archive(profile: FocusProfile)
    suspend fun exportJson(): File
    suspend fun exportPdf(): File
    /// El resum que es veu abans de preguntar-lo: cap a l'app, el de mostra a les captures, que només
    /// dibuixen el primer fotograma.
    val initialGuardianSummary: ViraGuardianRequest? get() = null
    /// On és el permís del tutor (`ViraSession.guardianConsentSummary`).
    suspend fun guardianConsentSummary(): ViraGuardianRequest
    /// Amb `email`, demana el permís a aquesta adreça; sense, torna a enviar a qui ja hi era.
    suspend fun requestGuardianConsent(email: String?): ViraGuardianRequest
}

/// La de l'app, sobre les peces de dades d'aquesta execució.
class ViraSettingsBackend(private val context: Context, private val backend: ViraBackend) : SettingsBackend {
    private val db get() = backend.db
    private val store by lazy { ViraStore(db) }

    override val preferences: SharedPreferences get() = backend.preferences
    override val session: StateFlow<ViraSession.State> get() = backend.session.state
    override val syncState: StateFlow<ViraSyncEngine.State> get() = backend.engine.state
    override val profileName: Flow<String> = db.profiles().observe().map { it?.displayName.orEmpty() }
    override val equippedSkin: Flow<String?> = db.skinOwnerships().observeAll().map { all -> all.firstOrNull { it.isEquipped }?.key }
    override val streakVisible: Flow<Boolean> = db.streaks().observe().map { it?.isVisible ?: true }
    override val focusProfiles: Flow<List<FocusProfile>> =
        db.focusProfiles().observeAll().map { all -> all.filter { !it.isArchived } }

    override val appVersion: String by lazy {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName ?: "0.0"} (${info.longVersionCode})"
    }

    override suspend fun syncNow() = backend.engine.sync()

    override suspend fun signOut() = backend.session.signOut()

    // Les dues esborrades no es poden cancel·lar a mig fer: en tancar la sessió, la porta del compte
    // treu la pantalla que les ha cridat, i amb ella el seu àmbit de corrutines.
    override suspend fun deleteAccount() = withContext(NonCancellable) {
        backend.session.deleteAccount()
        // Deixar les dades al telèfon després d'esborrar el compte seria una mitja veritat.
        ViraDataEraser.eraseEverything(db, preferences, alsoErase = { ViraNotifications.cancelEverything(context) })
        eraseOwnLeftovers()
    }

    override suspend fun eraseEverything() = withContext(NonCancellable) {
        backend.engine.eraseEverything(alsoErase = { ViraNotifications.cancelEverything(context) })
        eraseOwnLeftovers()
    }

    /// El que Ajustos desa fora de les claus que coneix l'esborrat comú: les seves preferències i
    /// qualsevol exportació que hagi quedat a la memòria cau.
    private fun eraseOwnLeftovers() {
        preferences.edit().apply { SettingsKeys.erasable.forEach(::remove) }.apply()
        exportDirectory().deleteRecursively()
    }

    override suspend fun setStreakVisible(visible: Boolean) {
        // La ratxa es crea sota demanda: l'ajust s'ha de poder apagar abans del primer dia.
        store.save(db.streaks(), store.streak().copy(isVisible = visible))
    }

    override suspend fun archive(profile: FocusProfile) {
        store.save(db.focusProfiles(), profile.copy(isArchived = true))
    }

    override suspend fun exportJson(): File = ViraDataExport.writeSnapshotFile(db, exportDirectory(), appVersion)

    override suspend fun exportPdf(): File = ViraDataExportPdf.writeSnapshotFile(context, db, exportDirectory(), appVersion)

    private fun exportDirectory() = File(context.cacheDir, "exports")

    override suspend fun guardianConsentSummary() = backend.session.guardianConsentSummary()

    override suspend fun requestGuardianConsent(email: String?) = backend.session.requestGuardianConsent(email)
}

/// La de les captures: un estat fix i cap efecte.
class PreviewSettingsBackend(
    override val preferences: SharedPreferences = InMemoryPreferences(),
    sessionState: ViraSession.State = ViraSession.State.Unconfigured,
    sync: ViraSyncEngine.State = ViraSyncEngine.State.Idle,
    name: String = "",
    skin: String? = null,
    profiles: List<FocusProfile> = emptyList(),
    private val guardian: ViraGuardianRequest = ViraGuardianRequest(ViraConsentStatus.MISSING),
) : SettingsBackend {
    override val initialGuardianSummary = guardian
    override val session = MutableStateFlow(sessionState)
    override val syncState = MutableStateFlow(sync)
    override val profileName: Flow<String> = MutableStateFlow(name)
    override val equippedSkin: Flow<String?> = MutableStateFlow(skin)
    override val streakVisible: Flow<Boolean> = MutableStateFlow(true)
    override val focusProfiles: Flow<List<FocusProfile>> = MutableStateFlow(profiles)
    override val appVersion = "0.1 (1)"

    override suspend fun syncNow() = Unit
    override suspend fun signOut() = Unit
    override suspend fun deleteAccount() = Unit
    override suspend fun eraseEverything() = Unit
    override suspend fun setStreakVisible(visible: Boolean) = Unit
    override suspend fun archive(profile: FocusProfile) = Unit
    override suspend fun exportJson(): File = error("Sense fitxers a les captures")
    override suspend fun exportPdf(): File = error("Sense fitxers a les captures")
    override suspend fun guardianConsentSummary() = guardian
    override suspend fun requestGuardianConsent(email: String?) = guardian
}

/// El valor d'un flux com a estat. Si el flux ja en té un (`StateFlow`), es comença per aquest: les
/// captures només dibuixen el primer fotograma, i amb un valor inicial buit hi sortiria Ajustos buit.
@androidx.compose.runtime.Composable
fun <T> Flow<T>.collectAsStateOr(initial: T): androidx.compose.runtime.State<T> =
    collectAsState(initial = (this as? StateFlow<T>)?.value ?: initial)
