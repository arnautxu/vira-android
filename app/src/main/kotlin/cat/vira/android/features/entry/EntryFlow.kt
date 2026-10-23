package cat.vira.android.features.entry

import androidx.activity.compose.BackHandler
import cat.vira.android.core.design.tokens.ViraColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
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
/// **L'escena** (Arnau, 2026-10-21): una il·lustració sencera del món de Viri omple la pantalla, també
/// per sota de la capçalera, i s'atura una mica per sota de la vora de la fulla. A sobre, **la
/// capçalera** (enrere · «2 de 9» · acció, totes tres superfícies), **la bombolla de Viri** i **la
/// fulla blanca al peu**, que fa l'alçada del que porta fins a un sostre (el 70 % de la pantalla, el
/// 86 % amb lletra gran); per sobre, es desplaça. Dins de la fulla l'ordre és **títol → controls →
/// acció principal**; a mides d'accessibilitat hi tornen també el «2 de 9» i la frase de Viri.
///
/// `initialStep`, `initialOutcome` i `initialSigningUp` són per a les captures: un pas amb tot l'escenari.
@Composable
fun EntryFlow(
    steps: List<EntryStep> = EntryStep.full,
    onFinish: () -> Unit,
    services: EntryServices = rememberLiveEntryServices(),
    initialStep: EntryStep? = null,
    initialOutcome: EntryFirstActionOutcome? = null,
    initialSigningUp: Boolean = false,
) {
    val prefs = services.prefs
    val state by services.state.collectAsState()
    val isRecovering by services.isRecoveringPassword.collectAsState()
    // Es llegeix aquí perquè la frase de Viri del permís canviï quan surt el correu.
    services.guardianReceipt.collectAsState().value
    val finish by rememberUpdatedState(onFinish)
    fun navigator() = EntryNavigator(
        steps = steps,
        prefs = prefs,
        requiresAccount = { services.requiresAccount },
        user = { services.user },
        onFinish = { finish() },
        initialStep = initialStep,
        initialOutcome = initialOutcome,
        guardianReceipt = { services.guardianReceipt.value },
        initialSigningUp = initialSigningUp,
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
    val density = LocalDensity.current
    // La frase de Viri va a l'escena. A mides d'accessibilitat torna a dins de la fulla: a l'escena
    // no hi cabria i hi quedaria tallada.
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
    val onTop = nav.step == EntryStep.WELCOME
    var headerHeight by remember { mutableStateOf(0.dp) }
    var lineHeight by remember { mutableStateOf(0.dp) }
    var sheetHeight by remember { mutableStateOf(0.dp) }
    // El tros d'escena que es queda la bombolla, amb el seu marge.
    val lineRoom = if (lineHeight > 0.dp) lineHeight + (if (onTop) EntryStageTopInset else ViraSpacing.s) else 0.dp
    BoxWithConstraints(Modifier.fillMaxSize().background(ViraColor.surfaceElevated)) {
        val sheetMax = maxHeight * band
        EntryScene(
            scene = nav.step.scene(nav.intent),
            coveredTop = headerHeight + (if (onTop) lineRoom else 0.dp),
            coveredBottom = EntrySheetOverlap + (if (onTop) 0.dp else lineRoom),
            minimumRoom = if (lineOnStage) 0.dp else 140.dp,
            celebrates = nav.step == EntryStep.DONE && prefs.celebrationsEnabled.value,
            reaction = nav.reaction,
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - sheetHeight + EntrySheetOverlap).coerceAtLeast(0.dp)),
        )
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .semantics { isTraversalGroup = true },
        ) {
            // Per a TalkBack la capçalera va **al final**: frase de Viri → títol → controls → acció → saltar.
            EntryHeader(
                nav,
                Modifier
                    .statusBarsPadding()
                    .onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
                    .semantics { traversalIndex = 1f },
            )
            EntryStage(
                line = if (lineOnStage) line else null,
                onTop = onTop,
                onLineHeight = { lineHeight = with(density) { it.toDp() } },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMax)
                    .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } }
                    .entrySheetBackground(),
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
                            .padding(top = ViraSpacing.xl, bottom = ViraSpacing.m)
                            .semantics { isTraversalGroup = true },
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                    ) {
                        if (!lineOnStage && nav.showsProgress && shown == nav.step) {
                            val position = nav.progressPosition
                            // TalkBack ja el té a la capçalera, amb la frase sencera.
                            EntryProgressCount(position.position, position.total, Modifier.clearAndSetSemantics { })
                        }
                        val shownLine = nav.viriLineFor(shown)?.let { stringResource(it) }
                        if (!lineOnStage && shownLine != null) EntryViriLine(shownLine)
                        if (shown == nav.step) {
                            nav.title?.let { title ->
                                EntryHeading(stringResource(title), nav.message?.let { stringResource(it) })
                            }
                        } else {
                            nav.titleFor(shown)?.let { EntryHeading(stringResource(it), shown.message?.let { stringResource(it) }) }
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
        } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            // Què es guarda (2026-10-23): la fitxa del que s'acaba de fer, només a qui es registra.
            val outcome = nav.outcome
            if (nav.savesFirstAction && outcome != null) EntrySavedChip(outcome)
            EntryAccountStep(
                services = services,
                role = nav.role,
                email = nav.email,
                onEmailChange = { nav.email = it },
                isSigningUp = nav.isSigningUp,
                onForgotPassword = { nav.go(EntryStep.PASSWORD_RESET) },
            )
        }
        EntryStep.PASSWORD_RESET -> EntryPasswordResetStep(services, nav.email, { nav.email = it }, nav::backFromPasswordReset)
        EntryStep.NEW_PASSWORD -> EntryNewPasswordStep(services, nav::finishRecovery)
        EntryStep.CONSENT -> EntryConsentStep(services, nav::advance)
        EntryStep.NICKNAME -> EntryNicknameStep(services, nav::advance)
        EntryStep.NOTIFICATIONS -> EntryNotificationsStep(prefs, nav.outcome?.taskTitle, nav::advance, nav::advance)
        EntryStep.CONNECTIONS -> EntryConnectionsStep(prefs, nav::advance)
    }
}

/// Capçalera (`EntryFlowView+Header.swift`, Arnau, 2026-10-21): **enrere · progrés «2 de 9» ·
/// acció**, sempre al mateix lloc i surant sobre l'escena. Les tres peces són superfícies, així que
/// cap text va directament sobre la il·lustració. A la benvinguda, res: el logotip és a la fulla.
/// **Amb lletra gran la càpsula del progrés desapareix** i «2 de 9» baixa a dalt de la fulla: entre
/// dos botons que creixen no hi cabria.
@Composable
private fun EntryHeader(nav: EntryNavigator, modifier: Modifier) {
    val isAccessibilitySize = LocalViraEnvironment.current.isAccessibilitySize
    Box(modifier.fillMaxWidth()) {
        if (nav.step == EntryStep.WELCOME) return@Box
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ViraTouch.minimum)
                .padding(horizontal = ViraSpacing.screenMargin)
                .padding(vertical = ViraSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nav.canGoBack) EntryBackButton(nav::goBack, stringResource(R.string.action_back))
            if (nav.showsProgress && !isAccessibilitySize) {
                val position = nav.progressPosition
                EntryProgressBar(nav.progress, position.position, position.total, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            when {
                nav.step == EntryStep.ACCOUNT -> EntryCapsuleButton(
                    stringResource(if (nav.isSigningUp) R.string.account_email_sign_in_title else R.string.account_sign_up_action),
                    nav::toggleSigningUp,
                )
                // "Ara no" i no "Saltar": passa per sobre de la celebració, que sense acció no té res a celebrar.
                nav.step == EntryStep.FIRST_ACTION -> EntryCapsuleButton(stringResource(R.string.action_not_now), nav::skipAction)
                nav.offersSkip -> EntryCapsuleButton(stringResource(R.string.action_skip), nav::skip)
            }
        }
    }
}
