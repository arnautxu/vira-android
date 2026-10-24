package cat.vira.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.UUID

/// Les cares de cada pestanya que es poden obrir des de fora, calcades de `PlanSection`,
/// `EmotionsSection` i `ProgressDestination` de l'iPhone. Viuen aquí, a l'arrel, i no a la carpeta
/// de cada pestanya, perquè les fan servir alhora l'arrel (que les tria) i altres pantalles (que hi
/// porten): Inici enllaça amb Hàbits, Mindfulness o la ratxa sense saber com està muntada Pla.
enum class PlanSection { Calendar, Habits, Medication }

enum class EmotionsSection { CheckIn, History, Mindfulness }

enum class ProgressDestination { Streak, Shop, Challenges, PersonalRewards, Cards, Friends }

/// Una cara concreta d'una pestanya, referenciable des de fora (`ViraDestination` de l'iPhone).
sealed interface ViraDestination {
    data class Plan(val section: PlanSection) : ViraDestination
    data class Emotions(val section: EmotionsSection) : ViraDestination
    data object Profile : ViraDestination
    data object Challenges : ViraDestination
    data object Friends : ViraDestination
    data object Streak : ViraDestination
    /// La Botiga, des del panell de les monedes de la capçalera.
    data object Shop : ViraDestination
}

/// La tasca a què es lliga una sessió de Concentra't oberta des d'una fila (`FocusStartRequest`
/// amb `linkedTitle` i `linkedItemID` a l'iPhone).
data class ViraFocusLink(val title: String, val itemId: UUID)

/// Les pantalles que s'obren per sobre de les pestanyes. A l'iPhone, Concentra't és a pantalla
/// completa i la resta són fulls; aquí totes són destins de la navegació de l'arrel, i el gest
/// d'enrere les tanca.
enum class ViraModal(val route: String) {
    Focus("focus"),
    Help("help"),
    /// La respiració directa, sense passar per Ajuda ara (des dels widgets i els accessos).
    Breathe("breathe"),
    Settings("settings"),
    /// L'editor d'un mode de Concentra't, des d'Ajustos.
    FocusProfileEditor("focus-profile"),
}

/// L'estat de la navegació de l'arrel: quina pestanya, quina cara de cada una i el camí dins de
/// Viri. És l'equivalent de l'estat de `RootView` i de `\.viraNavigate`, `\.startFocus`.
///
/// Les pantalles el demanen amb `LocalViraNavigator.current` en lloc de rebre'n lligams: Pla llegeix
/// i escriu `planSection`, Emocions `emotionsSection`, Viri `progressPath`, i qualsevol pantalla pot
/// cridar `navigate`, `startFocus` o `openHelp`.
@Stable
class ViraNavigator internal constructor(
    tab: ViraTab = ViraTab.Today,
    plan: PlanSection = PlanSection.Calendar,
    emotions: EmotionsSection = EmotionsSection.CheckIn,
    path: List<ProgressDestination> = emptyList(),
) {
    var selectedTab by mutableStateOf(tab)
        private set
    var planSection by mutableStateOf(plan)
    var emotionsSection by mutableStateOf(emotions)

    /// La pila dins de la pestanya Viri. Buida és el perfil.
    val progressPath = mutableStateListOf<ProgressDestination>().apply { addAll(path) }

    /// L'arrel hi connecta la seva navegació. Abans que hi sigui, obrir no fa res.
    internal var presentModal: (ViraModal) -> Unit = {}

    /// Tocar una pestanya. Tocar Viri torna sempre al perfil, com a l'iPhone.
    fun open(tab: ViraTab) {
        if (tab == ViraTab.Progress) progressPath.clear()
        selectedTab = tab
    }

    /// Salta a una cara concreta: tria la pestanya **i** la cara alhora, perquè arribar-hi sigui un
    /// sol toc des d'on siguis.
    fun navigate(destination: ViraDestination) {
        when (destination) {
            is ViraDestination.Plan -> {
                planSection = destination.section
                selectedTab = ViraTab.Plan
            }
            is ViraDestination.Emotions -> {
                emotionsSection = destination.section
                selectedTab = ViraTab.Emotions
            }
            ViraDestination.Profile -> showProgress()
            ViraDestination.Challenges -> showProgress(ProgressDestination.Challenges)
            ViraDestination.Friends -> showProgress(ProgressDestination.Friends)
            ViraDestination.Streak -> showProgress(ProgressDestination.Streak)
            ViraDestination.Shop -> showProgress(ProgressDestination.Shop)
        }
    }

    /// La tasca de la sessió que s'acaba d'obrir, si n'hi ha. La llegeix Concentra't.
    var focusLink by mutableStateOf<ViraFocusLink?>(null)

    /// Concentra't és «un estat en què entres», no lligat a cap pestanya.
    fun startFocus(link: ViraFocusLink? = null) {
        focusLink = link
        presentModal(ViraModal.Focus)
    }
    fun openHelp() = presentModal(ViraModal.Help)
    fun openBreathe() = presentModal(ViraModal.Breathe)
    fun openSettings() = presentModal(ViraModal.Settings)

    /// El mode que s'edita (`null` és un de nou). El llegeix l'editor de Concentra't.
    var editingFocusProfileId by mutableStateOf<UUID?>(null)
        private set

    fun editFocusProfile(profileId: UUID?) {
        editingFocusProfileId = profileId
        presentModal(ViraModal.FocusProfileEditor)
    }

    private fun showProgress(destination: ProgressDestination? = null) {
        progressPath.clear()
        destination?.let(progressPath::add)
        selectedTab = ViraTab.Progress
    }

    internal companion object {
        /// Sobreviu a un gir de pantalla i a un procés que Android tanca en segon pla, incloent el
        /// que porta la finestra oberta (la tasca de Concentra't, el mode que s'edita): la navegació
        /// de l'arrel restaura la ruta i la pantalla ha de trobar les mateixes dades.
        val saver: Saver<ViraNavigator, List<String>> = Saver(
            save = { navigator ->
                listOf(
                    navigator.selectedTab.name,
                    navigator.planSection.name,
                    navigator.emotionsSection.name,
                    navigator.focusLink?.title ?: NONE,
                    navigator.focusLink?.itemId?.toString() ?: NONE,
                    navigator.editingFocusProfileId?.toString() ?: NONE,
                ) + navigator.progressPath.map { it.name }
            },
            restore = { saved ->
                ViraNavigator(
                    tab = ViraTab.valueOf(saved[0]),
                    plan = PlanSection.valueOf(saved[1]),
                    emotions = EmotionsSection.valueOf(saved[2]),
                    path = saved.drop(6).map(ProgressDestination::valueOf),
                ).apply {
                    val linkedId = saved[4].takeUnless { it == NONE }?.let(UUID::fromString)
                    focusLink = linkedId?.let { ViraFocusLink(saved[3], it) }
                    editingFocusProfileId = saved[5].takeUnless { it == NONE }?.let(UUID::fromString)
                }
            },
        )

        private const val NONE = "\u0000"
    }
}

@Composable
internal fun rememberViraNavigator(): ViraNavigator = rememberSaveable(saver = ViraNavigator.saver) { ViraNavigator() }

/// Fora de `RootScreen` (les captures i les previsualitzacions d'una pantalla sola) n'hi ha un de
/// buit: canvia d'estat però no obre res.
val LocalViraNavigator = staticCompositionLocalOf { ViraNavigator() }
