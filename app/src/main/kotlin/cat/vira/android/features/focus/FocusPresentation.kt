package cat.vira.android.features.focus

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.ViraApp
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusProfile
import java.util.UUID
import kotlinx.coroutines.launch

/// Petició per obrir una sessió de focus amb uns valors inicials concrets (`FocusStartRequest`).
///
/// `autostart` arrenca el rellotge sense passar per la preparació: només des del flux d'entrada, on
/// «Comencem» ja és la decisió de posar-s'hi. Des de qualsevol altre lloc, sempre hi ha la pantalla
/// enmig, perquè triar el mode i la durada és part de la decisió.
data class FocusStartRequest(
    val kind: FocusKind = FocusKind.FOCUS,
    val seconds: Int = 25 * 60,
    val linkedTitle: String? = null,
    val linkedItemID: UUID? = null,
    val profileID: UUID? = null,
    val autostart: Boolean = false,
)

/// L'editor de modes amb el desament inclòs, per obrir-lo des de fora de la sessió (Ajustos → Modes
/// de concentració). `profile` nul crea un mode nou. `onClose` rep el mode desat, o nul si es torna
/// enrere sense desar.
@Composable
fun FocusProfileEditorScreen(profile: FocusProfile?, onClose: (FocusProfile?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { (context.applicationContext as? ViraApp)?.let { ViraStore(it.backend.db) } }
    BackHandler { onClose(null) }
    FocusProfileEditor(
        profile = profile,
        onSave = { edited -> saveProfile(store, edited, isNew = profile == null, context, scope, onClose) },
        onBack = { onClose(null) },
    )
}

/// En desar, el mode nou queda triat: qui acaba de crear-lo el vol fer servir ara.
private fun saveProfile(
    store: ViraStore?,
    profile: FocusProfile,
    isNew: Boolean,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onSaved: (FocusProfile) -> Unit,
) {
    scope.launch {
        val dao = store?.db?.focusProfiles()
        val saved = when {
            store == null || dao == null -> profile
            isNew -> store.create(dao, profile)
            else -> store.save(dao, profile)
        }
        FocusSound.play(context, FocusSound.Cue.SAVED)
        onSaved(saved)
    }
}
