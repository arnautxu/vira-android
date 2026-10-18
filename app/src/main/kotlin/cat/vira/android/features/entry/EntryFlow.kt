package cat.vira.android.features.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.ViraApp
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Quin tros del flux demana qui l'obre (`ViraAuthGate` a l'iPhone). No hi ha una segona versió
/// d'aquestes pantalles enlloc: qui torna a entrar rep només el que li falta.
enum class EntryRequest(val steps: List<EntryStep>) {
    FULL(EntryStep.full),
    ACCOUNT(listOf(EntryStep.ACCOUNT)),
    CONSENT(listOf(EntryStep.CONSENT)),
    NEW_PASSWORD(listOf(EntryStep.NEW_PASSWORD)),
}

/// El punt d'entrada per a `app/`: el tros del flux que toca, amb els serveis de debò.
@Composable
fun Entry(request: EntryRequest, onFinish: () -> Unit) {
    EntryFlow(steps = request.steps, onFinish = onFinish)
}

/// Els serveis de debò, darrere de l'aplicació.
@Composable
fun rememberLiveEntryServices(): EntryServices {
    val context = LocalContext.current
    return remember(context) {
        val app = context.applicationContext as ViraApp
        EntryServices.live(context, app.backend)
    }
}

/// El flux d'entrada de Vira, **primer el valor i després el compte** (`EntryFlowView.swift`).
///
/// Capçalera (enrere · barra de progrés · acció; a la benvinguda, el logotip), **l'escenari amb
/// Viri** (un posat per pas, la bombolla a sobre del cap) i **la fulla groga al peu**, que fa
/// l'alçada del que porta fins a un sostre (el 70 % de la pantalla, el 86 % amb lletra gran); per
/// sobre, es desplaça. Dins de la fulla l'ordre és **títol → controls → acció principal**, i la
/// frase de Viri hi torna només a mides d'accessibilitat.
///
/// `initialStep` i `initialOutcome` són per a les captures: un pas amb tot l'escenari.
@Composable
fun EntryFlow(
    steps: List<EntryStep> = EntryStep.full,
    onFinish: () -> Unit,
    services: EntryServices = rememberLiveEntryServices(),
    initialStep: EntryStep? = null,
    initialOutcome: EntryFirstActionOutcome? = null,
) {
    val prefs = services.prefs
    val state by services.state.collectAsState()
    val isRecovering by services.isRecoveringPassword.collectAsState()
    val finish by rememberUpdatedState(onFinish)
    fun navigator() = EntryNavigator(
        steps = steps,
        prefs = prefs,
        requiresAccount = { services.requiresAccount },
        user = { services.user },
        onFinish = { finish() },
        initialStep = initialStep,
        initialOutcome = initialOutcome,
    )
    // Sobreviu a la recreació de l'activitat: vegeu `EntryNavigator.snapshot`.
    val nav = rememberSaveable(
        services, steps,
        saver = Saver<EntryNavigator, ArrayList<Any?>>(
            save = { it.snapshot() },
            restore = { saved -> navigator().also { it.restore(saved) } },
        ),
    ) { navigator() }
    val environment = LocalViraEnvironment.current
    val lineOnStage = !environment.isAccessibilitySize
    val band = if (environment.isAccessibilitySize) 0.86f else 0.7f

    // Entrar no torna cap valor al pas: el que canvia és la sessió, i el flux continua tot sol.
    LaunchedEffect(state, isRecovering, nav.parentRoleAttempt) {
        if (isRecovering) nav.startRecovery()
        else if (state is ViraSession.State.SignedIn) {
            if (nav.step == EntryStep.ACCOUNT && nav.role == ViraAccountRole.PARENT && !services.adoptParentRole()) {
                nav.parentRoleFailed = true
                return@LaunchedEffect
            }
            nav.onSignedIn(isRecoveringPassword = false)
        }
    }
    BackHandler(enabled = nav.canGoBack || nav.step == EntryStep.PASSWORD_RESET) {
        if (nav.step == EntryStep.PASSWORD_RESET) nav.backFromPasswordReset() else nav.goBack()
    }

    val line = nav.viriLine?.let { stringResource(it) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetMax = maxHeight * band
        EntryBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .semantics { isTraversalGroup = true },
        ) {
            // Per a TalkBack la capçalera va **al final**: frase de Viri → títol → controls → acció → saltar.
            EntryHeader(nav, Modifier.statusBarsPadding().semantics { traversalIndex = 1f })
            EntryStage(
                pose = nav.step.viriPose(nav.intent),
                line = if (lineOnStage) line else null,
                celebrates = nav.step == EntryStep.DONE && prefs.celebrationsEnabled.value,
                reaction = nav.reaction,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMax)
                    .then(if (nav.step != EntryStep.WELCOME) Modifier.entrySheetBackground() else Modifier),
            ) {
                AnimatedContent(
                    targetState = nav.step,
                    transitionSpec = {
                        if (environment.reduceMotion) {
                            fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                        } else {
                            (slideInVertically(ViraMotion.standard()) { it } + fadeIn(ViraMotion.standard())) togetherWith
                                fadeOut(ViraMotion.quick())
                        }
                    },
                    label = "entryStep",
                ) { shown ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = ViraSpacing.screenMargin)
                            .padding(top = if (shown == EntryStep.WELCOME) 0.dp else ViraSpacing.l, bottom = ViraSpacing.m),
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                    ) {
                        if (shown != EntryStep.WELCOME) {
                            val shownLine = nav.viriLineFor(shown)?.let { stringResource(it) }
                            if (!lineOnStage && shownLine != null) EntryViriLine(shownLine)
                            nav.titleFor(shown)?.let { title ->
                                EntryHeading(stringResource(title), shown.message?.let { stringResource(it) })
                            }
                        }
                        EntryStepContent(shown, nav, services)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryStepContent(step: EntryStep, nav: EntryNavigator, services: EntryServices) {
    val prefs = services.prefs
    when (step) {
        EntryStep.WELCOME -> EntryWelcomeStep(
            onStart = nav::startFresh,
            onSignIn = if (nav.canSignIn) nav::signInFromWelcome else null,
            onParent = if (nav.canSignIn) nav::startAsParent else null,
        )
        EntryStep.START -> EntryStartStep(prefs, nav::chose, nav::advance)
        EntryStep.AGE -> EntryAgeStep(prefs, services.requiresAccount, nav::chose, nav::advance)
        EntryStep.PRIVACY -> EntryPrivacyStep(prefs, services.canLockApp, nav::advance)
        EntryStep.FIRST_ACTION -> EntryFirstActionStep(nav.intent, services, nav::completeFirstAction)
        EntryStep.DONE -> EntryDoneStep(nav.outcome, prefs, nav::advance)
        EntryStep.ACCOUNT -> if (nav.parentRoleFailed) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                EntryErrorText(ViraBackendError.UNAVAILABLE)
                EntryPrimaryButton(stringResource(R.string.action_retry), nav::retryParentRole)
            }
        } else EntryAccountStep(
            services = services,
            role = nav.role,
            email = nav.email,
            onEmailChange = { nav.email = it },
            isSigningUp = nav.isSigningUp,
            onForgotPassword = { nav.go(EntryStep.PASSWORD_RESET) },
        )
        EntryStep.PASSWORD_RESET -> EntryPasswordResetStep(services, nav.email, { nav.email = it }, nav::backFromPasswordReset)
        EntryStep.NEW_PASSWORD -> EntryNewPasswordStep(services, nav::finishRecovery)
        EntryStep.CONSENT -> EntryConsentStep(services, nav::advance)
        EntryStep.NICKNAME -> EntryNicknameStep(services, nav::advance)
        EntryStep.NOTIFICATIONS -> EntryNotificationsStep(prefs, nav.outcome?.taskTitle, nav::advance, nav::advance)
        EntryStep.CONNECTIONS -> EntryConnectionsStep(prefs, nav::advance)
    }
}

/// Capçalera (`EntryFlowView+Header.swift`). A la benvinguda, el logotip i res més; a la resta,
/// enrere · barra · acció. **Amb lletra gran la barra baixa a una fila pròpia**: els botons creixen
/// amb el text i la barra quedaria aixafada.
@Composable
private fun EntryHeader(nav: EntryNavigator, modifier: Modifier) {
    val isAccessibilitySize = LocalViraEnvironment.current.isAccessibilitySize
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = ViraSpacing.s, bottom = ViraSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        when {
            nav.step == EntryStep.WELCOME -> EntryLogo()
            isAccessibilitySize -> Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                EntryHeaderControls(nav, withProgress = false)
                if (nav.showsProgress) EntryHeaderProgress(nav)
            }
            else -> EntryHeaderControls(nav, withProgress = true)
        }
    }
}

@Composable
private fun EntryHeaderControls(nav: EntryNavigator, withProgress: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nav.canGoBack) EntryBackButton(nav::goBack, stringResource(R.string.action_back))
        if (withProgress && nav.showsProgress) {
            Box(Modifier.weight(1f)) { EntryHeaderProgress(nav) }
        } else {
            Spacer(Modifier.weight(1f))
        }
        when {
            nav.step == EntryStep.ACCOUNT -> EntryCapsuleButton(
                stringResource(if (nav.isSigningUp) R.string.account_email_sign_in_title else R.string.account_sign_up_action),
                nav::toggleSigningUp,
                bold = true,
            )
            // "Ara no" i no "Saltar": passa per sobre de la celebració, que sense acció no té res a celebrar.
            nav.step == EntryStep.FIRST_ACTION -> EntryCapsuleButton(stringResource(R.string.action_not_now), nav::skipAction)
            nav.offersSkip -> EntryCapsuleButton(stringResource(R.string.action_skip), nav::skip)
        }
    }
}

@Composable
private fun EntryHeaderProgress(nav: EntryNavigator) {
    val position = nav.progressPosition
    EntryProgressBar(nav.progress, position.position, position.total)
}
