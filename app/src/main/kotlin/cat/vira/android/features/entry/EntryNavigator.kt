package cat.vira.android.features.entry

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraUser

/// Moviment del flux d'entrada: avançar, saltar, "Ara no", enrere i la recuperació de la
/// contrasenya (`EntryFlowView+Navigation.swift`). Les regles de **quin** pas toca viuen a
/// `EntryStep`; aquí s'apliquen a l'estat del flux.
///
/// És una classe i no estat dins de la pantalla perquè el moviment —el que es torna a preguntar,
/// on porta enrere— es pugui provar sense dibuixar res.
class EntryNavigator(
    val steps: List<EntryStep>,
    private val prefs: EntryPreferences,
    private val requiresAccount: () -> Boolean,
    private val user: () -> ViraUser?,
    private val onFinish: () -> Unit,
    initialStep: EntryStep? = null,
    initialOutcome: EntryFirstActionOutcome? = null,
) {
    var step: EntryStep by mutableStateOf(initialStep ?: steps.firstOrNull() ?: EntryStep.WELCOME)
        private set

    /// D'on s'ha vingut, per al botó enrere. És un historial i no "el pas d'abans de `steps`": qui
    /// ha entrat amb "Ja tinc compte" ha de tornar a la benvinguda, no a la primera acció.
    var history: List<EntryStep> by mutableStateOf(emptyList())
        private set

    /// El fixa l'enllaç de la benvinguda i el necessita el compte, que el crea.
    var role: ViraAccountRole by mutableStateOf(ViraAccountRole.TEEN)
        private set

    /// Entrar o registrar-se: el commuta el botó de la capçalera, no una altra pantalla.
    var isSigningUp: Boolean by mutableStateOf(false)
        private set

    /// S'ha entrat per "Ja tinc compte": després del compte només li pot quedar el permís.
    var cameFromSignIn: Boolean by mutableStateOf(false)
        private set

    /// El correu el comparteixen el pas de compte i el de recuperar la contrasenya.
    var email: String by mutableStateOf("")

    /// El que ha deixat la primera acció.
    var outcome: EntryFirstActionOutcome? by mutableStateOf(initialOutcome)
        private set

    /// La barra **no retrocedeix** quan un pas condicional apareix; enrere sí.
    var progress: Double by mutableStateOf(0.0)
        private set

    /// Cada tria fa fer un saltet a Viri.
    var reaction: Int by mutableStateOf(0)
        private set

    private var stepBeforeRecovery: EntryStep? = null

    init {
        progress = progressPosition.fraction
    }

    val pendingRole: ViraAccountRole? get() = if (role == ViraAccountRole.PARENT) ViraAccountRole.PARENT else null

    val intent: EntryStartIntent get() = EntryStartIntent.from(prefs.startIntent.value)

    fun applies(candidate: EntryStep): Boolean {
        if (cameFromSignIn && !isSigningUp && candidate !in RETURNING_STEPS) return false
        return candidate.applies(requiresAccount(), user(), pendingRole)
    }

    val offersSkip: Boolean get() = step.offersSkip(requiresAccount(), user(), pendingRole)

    /// Posició a la barra: els passos que es veuen, sense la benvinguda.
    val progressPosition: ProgressPosition
        get() {
            val visible = steps.filter { it != EntryStep.WELCOME && applies(it) }
            val index = visible.indexOf(step)
            if (index < 0) return ProgressPosition(0, visible.size, progress)
            return ProgressPosition(index + 1, visible.size, (index + 1).toDouble() / visible.size)
        }

    data class ProgressPosition(val position: Int, val total: Int, val fraction: Double)

    /// La frase de Viri del pas. El compte només en porta per al jove que es registra.
    @get:StringRes
    val viriLine: Int? get() = viriLineFor(step)

    @StringRes
    fun viriLineFor(step: EntryStep): Int? = when (step) {
        EntryStep.WELCOME -> R.string.onboarding_viri_welcome
        EntryStep.START -> R.string.onboarding_viri_start
        EntryStep.AGE -> R.string.onboarding_viri_age
        EntryStep.PRIVACY -> R.string.onboarding_viri_privacy
        EntryStep.FIRST_ACTION -> intent.viriLine
        EntryStep.DONE -> R.string.onboarding_viri_done
        EntryStep.ACCOUNT -> if (role == ViraAccountRole.TEEN && isSigningUp) R.string.onboarding_viri_account else null
        EntryStep.NICKNAME -> R.string.onboarding_viri_nickname
        EntryStep.NOTIFICATIONS -> R.string.onboarding_viri_reminders
        else -> null
    }

    /// El compte no porta títol (els camps ja diuen què són), el permís el posa ell mateix i la
    /// celebració no en té.
    @get:StringRes
    val title: Int? get() = titleFor(step)

    @StringRes
    fun titleFor(step: EntryStep): Int? = when (step) {
        EntryStep.ACCOUNT, EntryStep.CONSENT, EntryStep.DONE, EntryStep.WELCOME -> null
        EntryStep.FIRST_ACTION -> intent.actionTitle
        else -> step.title
    }

    val showsProgress: Boolean
        get() = step !in setOf(EntryStep.WELCOME, EntryStep.DONE, EntryStep.PASSWORD_RESET, EntryStep.NEW_PASSWORD) &&
            progressPosition.position > 0

    /// Sense backend no hi ha cap compte que ja es tingui, ni rols: els enllaços no hi són.
    val canSignIn: Boolean get() = applies(EntryStep.ACCOUNT)

    fun signInFromWelcome() {
        isSigningUp = false
        cameFromSignIn = true
        go(EntryStep.ACCOUNT)
    }

    /// "Sóc pare o mare": fixa el rol i continua (la privacitat, i després el compte).
    fun startAsParent() {
        role = ViraAccountRole.PARENT
        isSigningUp = true
        cameFromSignIn = false
        advance()
    }

    /// "Vull començar a…": el camí normal.
    fun startFresh() {
        role = ViraAccountRole.TEEN
        isSigningUp = true
        cameFromSignIn = false
        advance()
    }

    /// El botó de la capçalera del compte. Qui passa a "Entrar" ja té compte.
    fun toggleSigningUp() {
        isSigningUp = !isSigningUp
        cameFromSignIn = !isSigningUp
    }

    fun chose() { reaction += 1 }

    fun go(next: EntryStep) {
        history = history + step
        moveTo(next)
    }

    fun advance() {
        val following = steps.dropWhile { it != step }.drop(1)
        val next = following.firstOrNull(::applies) ?: return finish()
        go(next)
    }

    fun completeFirstAction(result: EntryFirstActionOutcome) {
        outcome = result
        advance()
    }

    /// "Saltar": fins al primer pas obligatori que quedi —o la privacitat, si encara no s'ha
    /// vist—; si no en queda cap, tanca el flux. Saltar "Per on comencem?" vol dir organitzar-se.
    fun skip() {
        if (step == EntryStep.START && prefs.startIntent.value.isEmpty()) {
            prefs.startIntent.value = EntryStartIntent.fallback.raw
        }
        if (step == EntryStep.WELCOME) isSigningUp = true
        val target = EntryStep.skipTarget(step, steps, requiresAccount(), user(), pendingRole) ?: return finish()
        go(target)
    }

    /// "Ara no" a la primera acció: directe al compte, o al següent que apliqui.
    fun skipAction() {
        outcome = null
        val target = EntryStep.afterSkippedAction(steps, requiresAccount(), user(), pendingRole) ?: return finish()
        go(target)
    }

    /// On tornaria "Enrere": mai la celebració —no es desfà— ni una primera acció ja feta.
    private val backTarget: EntryStep?
        get() = history.lastOrNull { candidate ->
            candidate != EntryStep.DONE && applies(candidate) &&
                !(candidate == EntryStep.FIRST_ACTION && outcome != null)
        }

    val canGoBack: Boolean
        get() {
            if (step in NO_BACK) return false
            if (step == EntryStep.ACCOUNT && user() != null) return false
            return backTarget != null
        }

    fun goBack() {
        val target = backTarget ?: return
        val index = history.lastIndexOf(target)
        history = history.take(index)
        if (target == EntryStep.WELCOME) {
            role = ViraAccountRole.TEEN
            cameFromSignIn = false
        }
        moveTo(target)
    }

    /// "Torna" de la recuperació: la recuperació no és un pas del camí, és una branca.
    fun backFromPasswordReset() {
        if (history.lastOrNull() == EntryStep.ACCOUNT) history = history.dropLast(1)
        moveTo(EntryStep.ACCOUNT)
    }

    /// La sessió s'ha obert: el flux continua tot sol, i ja no es torna enrere cap a abans del compte.
    /// La recuperació obre sessió abans de canviar la contrasenya: llavors no s'avança.
    fun onSignedIn(isRecoveringPassword: Boolean) {
        if (step != EntryStep.ACCOUNT || isRecoveringPassword) return
        advance()
        history = emptyList()
    }

    /// L'enllaç del correu ha obert l'app: la contrasenya nova tapa el pas que hi hagués.
    fun startRecovery() {
        if (step == EntryStep.NEW_PASSWORD) return
        stepBeforeRecovery = step
        moveTo(EntryStep.NEW_PASSWORD)
    }

    /// Contrasenya nova escrita: es torna al pas d'abans i s'hi avança com si s'acabés d'entrar.
    fun finishRecovery() {
        moveTo(stepBeforeRecovery ?: EntryStep.ACCOUNT)
        stepBeforeRecovery = null
        if (step !in steps) return finish()
        advance()
    }

    fun finish() {
        prefs.onboardingCompleted.value = true
        onFinish()
    }

    private fun moveTo(next: EntryStep) {
        val old = step
        step = next
        val target = progressPosition.fraction
        val movedForward = steps.indexOf(next) > steps.indexOf(old)
        progress = if (movedForward) maxOf(progress, target) else target
    }

    companion object {
        /// Els únics passos de qui entra amb "Ja tinc compte" i es queda en mode entrar.
        val RETURNING_STEPS = setOf(EntryStep.WELCOME, EntryStep.ACCOUNT, EntryStep.CONSENT)

        private val NO_BACK = setOf(
            EntryStep.WELCOME, EntryStep.START, EntryStep.DONE, EntryStep.PASSWORD_RESET, EntryStep.NEW_PASSWORD,
        )
    }
}
