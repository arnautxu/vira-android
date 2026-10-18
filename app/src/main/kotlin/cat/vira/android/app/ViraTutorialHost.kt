package cat.vira.android.app

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.features.entry.EntryPreferences
import cat.vira.android.features.entry.ViraTutorialStep

/// El tutorial breu de la primera obertura (`RootView` de l'iPhone, «Tutorial de la primera
/// obertura»). Viu a l'arrel perquè **canvia de pestanya a cada parada**: el que hi ha darrere de
/// la cortina és la secció que s'explica, i qui posseeix la pestanya és el navegador.
class ViraTutorialState internal constructor(
    private val navigator: ViraNavigator,
    private val preferences: SharedPreferences,
    /// El nom de la parada, perquè sobrevisqui a un gir de pantalla.
    private val stepName: MutableState<String?>,
    val prefs: EntryPreferences,
) {
    val step: ViraTutorialStep? get() = stepName.value?.let(ViraTutorialStep::valueOf)

    /// L'última parada que s'ha ensenyat, per dibuixar-la mentre la cortina es fon.
    var lastStep: ViraTutorialStep? = null
        private set

    fun advance() {
        val next = step?.next ?: return finish()
        show(next)
    }

    /// Tanca el tutorial i torna a Avui: acabar-lo no és haver navegat enlloc. Marca la clau
    /// **abans** de res: si l'app mor amb la cortina a la pantalla, no torna a sortir. Saltar compta
    /// com haver-lo vist; un tutorial que insisteix és soroll.
    fun finish() {
        if (step == null) return
        preferences.edit().putBoolean(ViraPreferences.TUTORIAL_COMPLETED, true).apply()
        stepName.value = null
        navigator.open(ViraTab.Today)
    }

    /// La parada i la pestanya de sota es mouen alhora.
    internal fun show(step: ViraTutorialStep) {
        lastStep = step
        stepName.value = step.name
        step.tab?.let(navigator::open)
    }
}

/// Arrenca en una obertura posterior a una primera acció (pla, focus, hàbit o check-in): la
/// primera sessió queda per fer servir l'app. Si algú torna a posar la clau a `false` (esborrar
/// les dades), torna a mirar-ho.
@Composable
fun rememberViraTutorial(navigator: ViraNavigator): ViraTutorialState {
    val backend = LocalViraBackend.current
    val stepName = rememberSaveable { mutableStateOf<String?>(null) }
    val state = remember(navigator, backend) {
        ViraTutorialState(navigator, backend.preferences, stepName, EntryPreferences.shared(backend.preferences))
    }
    val completed by rememberPreference(backend.preferences, ViraPreferences.TUTORIAL_COMPLETED, false)
    LaunchedEffect(completed) {
        if (completed || state.step != null) return@LaunchedEffect
        if (hasUsedApp(backend.db)) state.show(ViraTutorialStep.first)
    }
    return state
}

private suspend fun hasUsedApp(db: ViraDatabase): Boolean =
    db.emotionEntries().all().isNotEmpty() || db.planItems().all().isNotEmpty() ||
        (db.progressCounters().all().firstOrNull()?.totalXP ?: 0) > 0
