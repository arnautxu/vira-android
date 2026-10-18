package cat.vira.android.features.entry

import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.app.ViraTab

/// Les parades del tutorial breu que surt **un sol cop**, quan el flux d'entrada ja s'ha acabat
/// (`ViraTutorialStep.swift`). El flux pregunta; el tutorial **ensenya on són les coses**.
///
/// - Inici i Concentra't en queden fora: el primer és on s'acaba, el segon no és un destí (D-02).
/// - Integracions i widgets no tenen cap peça a la pantalla: són targetes centrades, i la
///   d'integracions és l'única que porta controls.
enum class ViraTutorialStep {
    PLAN, EMOTIONS, PROFILE, INTEGRATIONS, WIDGETS;

    /// La icona de la barra que s'il·lumina i la pestanya que es veu a sota. `null` a les dues
    /// últimes: no hi ha res a assenyalar i no es canvia de pestanya.
    val tab: ViraTab?
        get() = when (this) {
            PLAN -> ViraTab.Plan
            EMOTIONS -> ViraTab.Emotions
            PROFILE -> ViraTab.Progress
            INTEGRATIONS, WIDGETS -> null
        }

    @get:StringRes
    val title: Int
        get() = when (this) {
            PLAN -> R.string.tutorial_plan_title
            EMOTIONS -> R.string.tutorial_emotions_title
            PROFILE -> R.string.tutorial_profile_title
            INTEGRATIONS -> R.string.tutorial_integrations_title
            WIDGETS -> R.string.tutorial_widgets_title
        }

    @get:StringRes
    val message: Int
        get() = when (this) {
            PLAN -> R.string.tutorial_plan_message
            EMOTIONS -> R.string.tutorial_emotions_message
            PROFILE -> R.string.tutorial_profile_message
            INTEGRATIONS -> R.string.tutorial_integrations_message
            WIDGETS -> R.string.tutorial_widgets_message
        }

    /// L'última parada tanca el tutorial amb "Comencem"; les altres continuen.
    @get:StringRes
    val action: Int get() = if (next == null) R.string.tutorial_action_start else R.string.action_continue

    val next: ViraTutorialStep? get() = entries.getOrNull(ordinal + 1)

    companion object {
        val first = PLAN
    }
}
