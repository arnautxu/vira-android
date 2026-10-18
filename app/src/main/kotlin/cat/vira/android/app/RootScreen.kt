package cat.vira.android.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cat.vira.android.R
import cat.vira.android.ViraApp
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.components.ViraProfileSummary
import cat.vira.android.core.design.components.ViraTabBar
import cat.vira.android.core.design.components.ViraTabBarItem
import cat.vira.android.core.design.components.ViraTopBar
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.emotions.EmotionsScreen
import cat.vira.android.features.entry.ViraTutorialOverlay
import cat.vira.android.features.focus.FocusResumeEffect
import cat.vira.android.features.plan.PlanScreen
import cat.vira.android.features.progress.ProgressScreen
import cat.vira.android.features.today.TodayScreen

/// L'arrel: quatre destins permanents i Concentra't com a acció (`RootView` de l'iPhone), més les
/// pantalles que s'obren per sobre (Concentra't, Ajuda ara, la respiració i Ajustos).
@Composable
fun RootScreen() {
    val navController = rememberNavController()
    val navigator = rememberViraNavigator()
    // Un `DisposableEffect` i no un `SideEffect`: aquests s'executen després de tots els efectes, i
    // la represa del focus d'aquí sota ja pot navegar a la primera composició, si l'activitat neix represa.
    DisposableEffect(navController) {
        navigator.presentModal = { modal -> navController.navigate(modal.route) { launchSingleTop = true } }
        onDispose { }
    }
    val close: () -> Unit = { navController.popBackStack() }
    val app = LocalContext.current.applicationContext as ViraApp
    val celebrations = app.celebrations
    // L'avís tocat porta a Avui o al check-in, com `openFromIntent` de l'iPhone: una pantalla oberta,
    // mai una dada escrita.
    val notificationDestination by app.notificationDestination.collectAsStateWithLifecycle()
    LaunchedEffect(notificationDestination) {
        when (notificationDestination ?: return@LaunchedEffect) {
            ViraNotifications.DESTINATION_CHECK_IN -> navigator.navigate(ViraDestination.Emotions(EmotionsSection.CheckIn))
            else -> navigator.open(ViraTab.Today)
        }
        app.notificationDestination.value = null
    }
    // Una sessió en marxa torna a la vista quan l'app torna a primer pla (des de l'avís fix).
    FocusResumeEffect { navigator.startFocus(navigator.focusLink) }

    CompositionLocalProvider(LocalViraNavigator provides navigator) {
        ViraCompletionQueue {
            NavHost(
                navController = navController,
                startDestination = MAIN_ROUTE,
                // Els fulls de l'iPhone pugen des de baix; aquí també, i en tancar baixen.
                enterTransition = { slideInVertically { it } + fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { slideOutVertically { it } + fadeOut() },
            ) {
                composable(MAIN_ROUTE) { MainTabs(navigator) }
                composable(ViraModal.Focus.route) {
                    ViraFeatureSlots.Focus(link = navigator.focusLink, onClose = close)
                }
                composable(ViraModal.Help.route) {
                    ViraFeatureSlots.Help(
                        onClose = close,
                        onOpenCheckIn = {
                            close()
                            navigator.navigate(ViraDestination.Emotions(EmotionsSection.CheckIn))
                        },
                    )
                }
                composable(ViraModal.Breathe.route) { ViraFeatureSlots.Breathe(onClose = close) }
                composable(ViraModal.Settings.route) { ViraFeatureSlots.Settings(onClose = close) }
                composable(ViraModal.FocusProfileEditor.route) {
                    ViraFeatureSlots.FocusProfileEditor(profileId = navigator.editingFocusProfileId, onClose = close)
                }
            }
            ViraCelebrations(celebrations)
        }
    }
}

/// Les quatre pestanyes amb la capçalera i la barra. Cada pestanya conserva el seu estat (i el
/// desplaçament) en canviar d'una a l'altra, com el `TabView` de l'iPhone.
@Composable
private fun MainTabs(navigator: ViraNavigator) {
    val stateHolder = rememberSaveableStateHolder()
    val tab = navigator.selectedTab
    val tutorial = rememberViraTutorial(navigator)
    // Mentre hi ha la cortina, cap celebració agafa la pantalla; es queden pendents.
    val app = LocalContext.current.applicationContext as ViraApp
    val celebrations = app.celebrations
    // L'avís tocat porta a Avui o al check-in, com `openFromIntent` de l'iPhone: una pantalla oberta,
    // mai una dada escrita.
    val notificationDestination by app.notificationDestination.collectAsStateWithLifecycle()
    LaunchedEffect(notificationDestination) {
        when (notificationDestination ?: return@LaunchedEffect) {
            ViraNotifications.DESTINATION_CHECK_IN -> navigator.navigate(ViraDestination.Emotions(EmotionsSection.CheckIn))
            else -> navigator.open(ViraTab.Today)
        }
        app.notificationDestination.value = null
    }
    SideEffect { celebrations.tutorialVisible = tutorial.step != null }
    DisposableEffect(celebrations) { onDispose { celebrations.tutorialVisible = false } }
    val tabBounds = remember { mutableStateMapOf<String, Rect>() }

    // Enrere des d'una altra pestanya torna a Avui, que és on s'obre l'app; des d'Avui, surt.
    BackHandler(enabled = tab != ViraTab.Today) { navigator.open(ViraTab.Today) }
    // Amb el tutorial a la pantalla, Enrere és «Saltar»: compta com a vist, igual que el botó.
    BackHandler(enabled = tutorial.step != null) { tutorial.finish() }

    Box(Modifier.fillMaxSize()) {
        // La cortina és modal també per a TalkBack: l'arbre de sota no es pot recórrer.
        Box(if (tutorial.step != null) Modifier.clearAndSetSemantics {} else Modifier) {
            ViraRootLayout(
                selectedTab = tab,
                profile = rememberProfileSummary(),
                onTabTap = navigator::open,
                onFocus = { navigator.startFocus() },
                onProfile = { navigator.navigate(ViraDestination.Profile) },
                onSettings = navigator::openSettings,
                onHelp = navigator::openHelp,
                onTabBounds = { key, bounds -> tabBounds[key] = bounds },
            ) {
                stateHolder.SaveableStateProvider(tab.route) {
                    when (tab) {
                        ViraTab.Today -> TodayScreen()
                        ViraTab.Plan -> PlanScreen()
                        ViraTab.Emotions -> EmotionsScreen()
                        ViraTab.Progress -> ProgressScreen()
                    }
                }
            }
        }
        val step = tutorial.step
        AnimatedVisibility(visible = step != null, enter = fadeIn(), exit = fadeOut()) {
            // En fondre's, `step` ja és nul: es dibuixa l'última parada fins que desapareix.
            val shown = step ?: tutorial.lastStep ?: return@AnimatedVisibility
            ViraTutorialOverlay(
                step = shown,
                spotlight = shown.tab?.let { tabBounds[it.route] },
                prefs = tutorial.prefs,
                onNext = tutorial::advance,
                onSkip = tutorial::finish,
            )
        }
    }
}

/// La disposició de l'arrel, sense estat, perquè les captures la puguin dibuixar sola.
///
/// La capçalera és fixa a dalt (`ViraTopBar`, amb el to de la pestanya) i la barra sura a sobre del
/// contingut, a baix: cada pantalla deixa a sota l'espai de `viraTabBarClearance()`, com fa
/// `safeAreaInset` a l'iPhone.
@Composable
fun ViraRootLayout(
    selectedTab: ViraTab,
    profile: ViraProfileSummary,
    onTabTap: (ViraTab) -> Unit,
    onFocus: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onTabBounds: (key: String, bounds: Rect) -> Unit = { _, _ -> },
    content: @Composable () -> Unit,
) {
    val items = ViraTab.entries.map { ViraTabBarItem(it.route, stringResource(it.title), it.icon) }
    val focus = ViraTabBarItem("focus", stringResource(ViraTab.focusTitle), ViraTab.focusIcon)

    Column(Modifier.fillMaxSize().background(ViraColor.background)) {
        ViraTopBar(
            area = selectedTab.area,
            profile = profile,
            onProfile = onProfile,
            onSettings = onSettings,
            onHelp = onHelp,
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content()
            ViraTabBar(
                items = items,
                selectedKey = selectedTab.route,
                focus = focus,
                onTabTap = { key -> ViraTab.fromRoute(key)?.let(onTabTap) },
                onFocus = onFocus,
                modifier = Modifier.align(Alignment.BottomCenter),
                onItemBounds = onTabBounds,
            )
        }
    }
}

/// El que ensenya la pastilla de perfil, llegit de les dades locals (`ViraProfilePill`).
///
/// Amb «Mostrar progrés» apagat no hi surten números, i amb la ratxa amagada només hi ha les
/// monedes. Sense malnom, el nom és «Perfil». L'avatar de la skin el posarà el fil de la Botiga.
@Composable
private fun rememberProfileSummary(): ViraProfileSummary {
    val backend = LocalViraBackend.current
    val profile by backend.db.profiles().observe().collectAsStateWithLifecycle(null)
    val counter by backend.db.progressCounters().observe().collectAsStateWithLifecycle(null)
    val streak by backend.db.streaks().observe().collectAsStateWithLifecycle(null)
    val progressVisible by rememberPreference(backend.preferences, PROGRESS_VISIBLE, true)

    val nickname = profile?.displayName?.trim().orEmpty()
    return ViraProfileSummary(
        name = nickname.ifEmpty { stringResource(R.string.tab_profile) },
        coins = if (progressVisible) counter?.coinBalance ?: 0 else null,
        streakDays = if (progressVisible) streak?.takeIf { it.isVisible }?.currentDays else null,
    )
}

private const val MAIN_ROUTE = "main"

/// «Mostrar progrés» (Ajustos), amb el nom de l'iPhone. La clau és del fil d'Ajustos; aquí només
/// es llegeix.
private const val PROGRESS_VISIBLE = "settings.progressVisible"
