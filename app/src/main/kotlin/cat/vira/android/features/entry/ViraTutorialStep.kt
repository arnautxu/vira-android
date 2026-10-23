package cat.vira.android.features.entry

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.ViraTab

/// Les parades del tomb per l'app que surt **un sol cop**, en una obertura posterior a la primera
/// acció (`ViraTutorialStep.swift`; redissenyat a l'iPhone el 2026-10-21).
///
/// El flux d'entrada pregunta; el tomb **ensenya on són les coses**: quan comença, l'app ja hi és
/// a sota, i el que s'assenyala són peces reals de la pantalla.
///
/// - Una pregunta abans de res, **«Et faig un tomb?»** (`INTRO`), amb el mapa de les quatre
///   pestanyes. «Ara no» el deixa per a Ajustos.
/// - Cada parada il·lumina **diverses peces** (`targets`) amb un número, i la targeta parla de cada
///   número (`lines`).
/// - La pestanya de la parada brilla i diu «Toca aquí»: tocar-la avança.
///
/// **Sense la parada dels widgets**: Android encara no en té. Les integracions només porten el
/// calendari (`EntryConnectionToggles`).
enum class ViraTutorialStep {
    INTRO, TODAY, PLAN, EMOTIONS, PROFILE, INTEGRATIONS;

    /// La pestanya que es veu a sota mentre es llegeix. Les integracions tornen a Avui.
    val screen: ViraTab
        get() = when (this) {
            INTRO, TODAY, INTEGRATIONS -> ViraTab.Today
            PLAN -> ViraTab.Plan
            EMOTIONS -> ViraTab.Emotions
            PROFILE -> ViraTab.Progress
        }

    /// Les peces que s'il·luminen, en l'ordre dels números. La pestanya, quan hi és, és sempre la
    /// primera: és la que es pot tocar per avançar.
    val targets: List<ViraTutorialTarget>
        get() = when (this) {
            INTRO, INTEGRATIONS -> emptyList()
            TODAY -> listOf(ViraTutorialTarget.TodayDay, ViraTutorialTarget.HelpNow)
            PLAN -> listOf(ViraTutorialTarget.Tab(ViraTab.Plan), ViraTutorialTarget.PlanSections, ViraTutorialTarget.PlanAdd)
            EMOTIONS -> listOf(ViraTutorialTarget.Tab(ViraTab.Emotions), ViraTutorialTarget.EmotionFaces)
            PROFILE -> listOf(ViraTutorialTarget.Tab(ViraTab.Progress), ViraTutorialTarget.Streak)
        }

    /// Una frase per peça, amb el mateix número. Només la d'Avui depèn de qui la llegeix.
    fun lines(context: ViraTutorialContext = ViraTutorialContext()): List<ViraTutorialLine> = when (this) {
        INTRO, INTEGRATIONS -> emptyList()
        TODAY -> listOf(context.todayLine, ViraTutorialLine(R.string.tutorial_today_help))
        PLAN -> listOf(
            ViraTutorialLine(R.string.tutorial_tab_here),
            ViraTutorialLine(R.string.tutorial_plan_sections),
            ViraTutorialLine(R.string.tutorial_plan_add),
        )
        EMOTIONS -> listOf(ViraTutorialLine(R.string.tutorial_tab_here), ViraTutorialLine(R.string.tutorial_emotions_faces))
        PROFILE -> listOf(ViraTutorialLine(R.string.tutorial_tab_here_viri), ViraTutorialLine(R.string.tutorial_profile_streak))
    }

    /// La pestanya que s'il·lumina, si n'hi ha.
    val tab: ViraTab? get() = targets.firstNotNullOfOrNull { (it as? ViraTutorialTarget.Tab)?.tab }

    @get:StringRes
    val title: Int
        get() = when (this) {
            INTRO -> R.string.tutorial_intro_title
            TODAY -> R.string.tutorial_today_title
            PLAN -> R.string.tab_plan
            EMOTIONS -> R.string.tutorial_emotions_title
            PROFILE -> R.string.tutorial_profile_title
            INTEGRATIONS -> R.string.tutorial_integrations_title
        }

    /// La línia de sota del títol, a les parades que no assenyalen peces.
    @get:StringRes
    val message: Int?
        get() = when (this) {
            INTRO -> R.string.tutorial_intro_message
            INTEGRATIONS -> R.string.tutorial_integrations_message
            else -> null
        }

    /// Una frase sense número al final de la llista: el que hi ha i no es veu.
    @get:StringRes
    val note: Int? get() = if (this == PROFILE) R.string.tutorial_profile_more else null

    /// El Viri que fa de guia a la cantonada de la targeta: assenyalant les peces, i amb el polze
    /// enlaire als interruptors. La pregunta porta una escena sencera.
    @get:DrawableRes
    val guide: Int?
        get() = when (this) {
            INTRO -> null
            TODAY, PLAN, EMOTIONS, PROFILE -> R.drawable.viri_guide_point
            INTEGRATIONS -> R.drawable.viri_guide_thumbs
        }
}

/// Una frase de la targeta: el text i, a «Prova-ho», el títol de la tasca.
data class ViraTutorialLine(@StringRes val text: Int, val argument: String? = null)

/// Una peça de la pantalla que el tomb pot il·luminar.
sealed interface ViraTutorialTarget {
    /// La icona d'una pestanya a `ViraTabBar`.
    data class Tab(val tab: ViraTab) : ViraTutorialTarget
    /// «El teu dia», a Avui.
    data object TodayDay : ViraTutorialTarget
    /// «Ajuda ara», a la capçalera.
    data object HelpNow : ViraTutorialTarget
    /// El selector Calendari · Hàbits · Medicació.
    data object PlanSections : ViraTutorialTarget
    /// El «+» del calendari.
    data object PlanAdd : ViraTutorialTarget
    /// La primera fila de cares del check-in.
    data object EmotionFaces : ViraTutorialTarget
    /// La fitxa de la ratxa, a Viri.
    data object Streak : ViraTutorialTarget
    /// El cercle de la primera tasca per marcar d'Avui. No porta número: és on es toca per
    /// provar-ho, dins del forat d'«El teu dia».
    data object TodayCircle : ViraTutorialTarget

    /// Fins on pot créixer el forat. «El teu dia» pot fer tota la pantalla amb un dia ple: el
    /// forat se'n queda la part de dalt.
    val maxHeight: Dp? get() = if (this == TodayDay) 240.dp else null

    val isTab: Boolean get() = this is Tab
}

/// Quines parades fa un tomb i en quin ordre (`ViraTutorialRoute.swift`). El primer, que surt sol,
/// és curt; el que es demana des d'Ajustos («Torna a veure el tomb») és el sencer.
data class ViraTutorialRoute(val stops: List<ViraTutorialStep>) {

    /// Les parades que compten per a «2 de 3»: totes menys la pregunta del principi.
    val stopCount: Int get() = stops.count { it != ViraTutorialStep.INTRO }

    /// La parada que es veu a «N de 3». `null` a la pregunta del principi.
    fun position(step: ViraTutorialStep): Int? {
        if (step == ViraTutorialStep.INTRO) return null
        val index = stops.filter { it != ViraTutorialStep.INTRO }.indexOf(step)
        return if (index < 0) null else index + 1
    }

    fun next(step: ViraTutorialStep): ViraTutorialStep? {
        val index = stops.indexOf(step)
        return if (index < 0) null else stops.getOrNull(index + 1)
    }

    fun isLast(step: ViraTutorialStep): Boolean = next(step) == null

    /// La pregunta diu «Som-hi», l'última parada tanca amb «Comencem» i les altres continuen. Una
    /// parada sola, demanada des d'un estat buit, no «comença» res: s'ha entès.
    @StringRes
    fun action(step: ViraTutorialStep): Int = when {
        stops.size == 1 -> R.string.viri_tip_ok
        step == ViraTutorialStep.INTRO -> R.string.onboarding_welcome_start
        isLast(step) -> R.string.tutorial_action_start
        else -> R.string.tutorial_action_next
    }

    companion object {
        val FIRST = ViraTutorialRoute(listOf(ViraTutorialStep.INTRO, ViraTutorialStep.TODAY, ViraTutorialStep.PLAN, ViraTutorialStep.EMOTIONS))
        val REPLAY = ViraTutorialRoute(ViraTutorialStep.entries)
    }
}

/// El que el tomb sap de qui el veu, per no dir-li el que ja sap (`ViraTutorialContext.swift`).
///
/// - **Frase d'Avui segons la primera acció**: «Fet» ja havia dit «Quan ho acabis, toca el cercle».
/// - **«Prova-ho»**: si Avui té una tasca per marcar dins del forat, la frase hi porta el títol i el
///   cercle es pot tocar de debò (`tryTitle`). Marcar-lo fa el que fa fora del tomb.
data class ViraTutorialContext(
    val firstAction: EntryFirstActionKind? = null,
    /// El títol de la tasca que es pot provar; `null` si no n'hi ha cap a l'abast del forat.
    val tryTitle: String? = null,
    /// Ja s'ha tocat el cercle durant aquesta parada.
    val didTry: Boolean = false,
) {
    /// La primera frase de la parada d'Avui (la que va amb el número 1).
    val todayLine: ViraTutorialLine
        get() = when {
            didTry -> ViraTutorialLine(R.string.tutorial_today_tried)
            tryTitle != null -> ViraTutorialLine(R.string.tutorial_today_try, tryTitle)
            else -> ViraTutorialLine(
                when (firstAction) {
                    EntryFirstActionKind.TASK -> R.string.tutorial_today_day_task
                    EntryFirstActionKind.CHECK_IN -> R.string.tutorial_today_day_checkin
                    EntryFirstActionKind.FOCUS -> R.string.tutorial_today_day_focus
                    null -> R.string.tutorial_today_day
                },
            )
        }
}
