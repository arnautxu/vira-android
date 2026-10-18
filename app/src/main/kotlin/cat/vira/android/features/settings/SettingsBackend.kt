package cat.vira.android.features.settings

import android.content.Context
import android.content.SharedPreferences
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDataExport
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.features.settings.export.ViraDataExportPdf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    override suspend fun deleteAccount() {
        backend.session.deleteAccount()
        // Deixar les dades al telèfon després d'esborrar el compte seria una mitja veritat.
        ViraDataEraser.eraseEverything(db, preferences)
    }

    override suspend fun eraseEverything() = backend.engine.eraseEverything()

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
}

/// La de les captures: un estat fix i cap efecte.
class PreviewSettingsBackend(
    override val preferences: SharedPreferences = InMemoryPreferences(),
    sessionState: ViraSession.State = ViraSession.State.Unconfigured,
    sync: ViraSyncEngine.State = ViraSyncEngine.State.Idle,
    name: String = "",
    skin: String? = null,
    profiles: List<FocusProfile> = emptyList(),
) : SettingsBackend {
    override val session = MutableStateFlow(sessionState)
    override val syncState = MutableStateFlow(sync)
    override val profileName: Flow<String> = flowOf(name)
    override val equippedSkin: Flow<String?> = flowOf(skin)
    override val streakVisible: Flow<Boolean> = flowOf(true)
    override val focusProfiles: Flow<List<FocusProfile>> = flowOf(profiles)
    override val appVersion = "0.1 (1)"

    override suspend fun syncNow() = Unit
    override suspend fun signOut() = Unit
    override suspend fun deleteAccount() = Unit
    override suspend fun eraseEverything() = Unit
    override suspend fun setStreakVisible(visible: Boolean) = Unit
    override suspend fun archive(profile: FocusProfile) = Unit
    override suspend fun exportJson(): File = error("Sense fitxers a les captures")
    override suspend fun exportPdf(): File = error("Sense fitxers a les captures")
}
