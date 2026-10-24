package cat.vira.android.app

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.features.entry.EntryFirstActionKind
import cat.vira.android.features.entry.EntryPreferenceKeys
import cat.vira.android.features.entry.EntryPreferences
import cat.vira.android.features.entry.ViraTutorialContext
import cat.vira.android.features.entry.ViraTutorialRoute
import cat.vira.android.features.entry.ViraTutorialStep
import cat.vira.android.features.entry.ViraTutorialTargets

/// El tomb de la primera obertura (`RootView` de l'iPhone, «Tutorial de la primera obertura»). Viu a
/// l'arrel perquè **canvia de pestanya a cada parada**: el que hi ha darrere de la cortina és la
/// cara que s'explica, i qui posseeix la pestanya és el navegador.
///
/// - El primer tomb, que surt sol, és curt (`ViraTutorialRoute.FIRST`); «Torna a veure el tomb»
///   d'Ajustos posa `tutorialCompleted` a `false` i fa el sencer (`REPLAY`).
/// - «Com funciona?» d'un estat buit obre **una sola parada** i torna on era (`showStop`).
/// - Saltar abans de l'última parada marca `tutorialSkipped`: les pistes de Pla i Emocions surten
///   per ocupar el lloc del que no s'ha vist.
class ViraTutorialState internal constructor(
    private val navigator: ViraNavigator,
    private val preferences: SharedPreferences,
    /// La parada i el recorregut, perquè sobrevisquin a un gir de pantalla.
    private val saved: MutableState<List<String>?>,
    val prefs: EntryPreferences,
) {
    /// On és cada peça. El llegeixen les peces mateixes (`LocalViraTutorialTargets`).
    val targets = ViraTutorialTargets()

    /// Encara es mira si toca: mentrestant, les celebracions esperen, o una sortiria just abans que
    /// el tomb i el taparia.
    internal var checking by mutableStateOf(!preferences.getBoolean(ViraPreferences.TUTORIAL_COMPLETED, false))

    /// El tomb és a la pantalla o hi pot ser d'aquí a un moment.
    val holdsCelebrations: Boolean get() = checking || step != null

    val step: ViraTutorialStep? get() = saved.value?.firstOrNull()?.let(ViraTutorialStep::valueOf)

    val route: ViraTutorialRoute
        get() = saved.value?.getOrNull(1)?.split(",")?.map(ViraTutorialStep::valueOf)?.let(::ViraTutorialRoute)
            ?: ViraTutorialRoute.FIRST

    /// La pestanya d'on s'ha obert una parada sola, i la cara de Pla i d'Emocions que hi havia,
    /// per tornar exactament on era (l'estat buit d'Hàbits o de l'historial).
    private val origin: ViraTab? get() = saved.value?.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let(ViraTab::valueOf)
    private val originPlan: PlanSection? get() = saved.value?.getOrNull(3)?.let(PlanSection::valueOf)
    private val originEmotions: EmotionsSection? get() = saved.value?.getOrNull(4)?.let(EmotionsSection::valueOf)

    /// L'última parada que s'ha ensenyat, per dibuixar-la mentre la cortina es fon.
    var lastStep: ViraTutorialStep? = null
        private set

    /// Què ha fet qui el veu: la primera acció i la tasca que es pot provar a Avui.
    val context: ViraTutorialContext
        get() = ViraTutorialContext(
            firstAction = EntryFirstActionKind.from(prefs.firstAction.value),
            tryTitle = targets.tryIt?.title,
        )

    /// Comença per la pregunta «Et faig un tomb?», a Avui.
    fun start(route: ViraTutorialRoute) {
        if (step != null) return
        targets.startTracking()
        show(ViraTutorialStep.INTRO, route, origin = null)
    }

    /// Una sola parada, demanada des d'un estat buit. Torna on era en acabar.
    fun showStop(stop: ViraTutorialStep) {
        if (step != null) return
        targets.startTracking()
        val plan = navigator.planSection
        val emotions = navigator.emotionsSection
        show(stop, ViraTutorialRoute(listOf(stop)), origin = navigator.selectedTab)
        saved.value = saved.value.orEmpty() + listOf(plan.name, emotions.name)
    }

    /// Marca la tasca que es pot provar i torna com desfer-ho: la mateixa acció sobre la mateixa
    /// fila, encara que el «Prova-ho» ja apunti a la següent.
    fun tryIt(): (() -> Unit)? {
        val perform = targets.tryIt?.perform ?: return null
        perform()
        return perform
    }

    fun advance() {
        val current = step ?: return
        val next = route.next(current) ?: return finish()
        show(next, route, origin)
    }

    /// «Salta el tomb» abans de l'última parada.
    fun skip() {
        val current = step ?: return
        if (route.stops.size > 1 && route.next(current) != null) {
            preferences.edit().putBoolean(EntryPreferenceKeys.TUTORIAL_SKIPPED, true).apply()
        }
        finish()
    }

    /// Tanca el tomb i torna a Avui (o, d'una parada sola, on era): acabar-lo no és haver navegat
    /// enlloc. Marca la clau **abans** de res: si l'app mor amb la cortina a la pantalla, no torna a
    /// sortir. Un tomb que insisteix és soroll.
    fun finish() {
        if (step == null) return
        val single = route.stops.size == 1
        val back = origin
        val plan = originPlan
        val emotions = originEmotions
        preferences.edit().putBoolean(ViraPreferences.TUTORIAL_COMPLETED, true).apply()
        saved.value = null
        targets.stopTracking()
        if (single) {
            plan?.let { navigator.planSection = it }
            emotions?.let { navigator.emotionsSection = it }
        }
        navigator.open(if (single) back ?: ViraTab.Today else ViraTab.Today)
    }

    /// La parada i la pestanya de sota es mouen alhora. Cada pestanya s'obre per la cara que el tomb
    /// explica: Organització pel calendari (on és el «+»), Emocions per les cares i Viri per l'arrel.
    private fun show(step: ViraTutorialStep, route: ViraTutorialRoute, origin: ViraTab?) {
        lastStep = step
        saved.value = listOf(step.name, route.stops.joinToString(",") { it.name }, origin?.name.orEmpty())
        when (step.screen) {
            ViraTab.Plan -> navigator.navigate(ViraDestination.Plan(PlanSection.Calendar))
            ViraTab.Emotions -> navigator.navigate(ViraDestination.Emotions(EmotionsSection.CheckIn))
            else -> navigator.open(step.screen)
        }
    }
}

/// Arrenca en una obertura posterior a una primera acció (pla, focus, hàbit o check-in): la primera
/// sessió queda per fer servir l'app. Si algú torna a posar la clau a `false` («Torna a veure el
/// tomb»), fa el recorregut sencer.
@Composable
fun rememberViraTutorial(navigator: ViraNavigator): ViraTutorialState {
    val backend = LocalViraBackend.current
    val saved = rememberSaveable { mutableStateOf<List<String>?>(null) }
    val state = remember(navigator, backend) {
        ViraTutorialState(navigator, backend.preferences, saved, EntryPreferences.shared(backend.preferences))
    }
    val completed by rememberPreference(backend.preferences, ViraPreferences.TUTORIAL_COMPLETED, false)
    // La primera lectura decideix si és el tomb curt; un canvi a `false` després (Ajustos, que tapa
    // les pestanyes un moment), el sencer. Per això sobreviu mentre Ajustos és a sobre.
    val firstLook = rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(completed) {
        val isFirst = firstLook.value
        firstLook.value = false
        if (completed || state.step != null) {
            // Un tomb restaurat després que Android recreï l'activitat: el registre és nou i cal
            // tornar a seguir les peces, o la cortina quedaria sense forats ni «Prova-ho».
            if (state.step != null) state.targets.startTracking()
            state.checking = false
            return@LaunchedEffect
        }
        if (!isFirst) state.start(ViraTutorialRoute.REPLAY)
        else if (hasUsedApp(backend.db)) state.start(ViraTutorialRoute.FIRST)
        state.checking = false
    }
    return state
}

private suspend fun hasUsedApp(db: ViraDatabase): Boolean =
    db.emotionEntries().all().isNotEmpty() || db.planItems().all().isNotEmpty() ||
        (db.progressCounters().all().firstOrNull()?.totalXP ?: 0) > 0
