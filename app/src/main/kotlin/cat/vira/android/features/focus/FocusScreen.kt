package cat.vira.android.features.focus

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.Manifest
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cat.vira.android.R
import cat.vira.android.ViraApp
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.launch

/// Arrel de Concentra't (`FocusSessionView`): preparació → en marxa/pausa → acabat, dins d'una sola
/// pantalla completa, sense modals encadenats. L'editor de modes la substitueix sencera, capçalera
/// inclosa: dues sortides apilades no serien clares.
///
/// Si ja hi ha una sessió en marxa (s'hi torna des de l'avís fix), es reprèn la mateixa.
@Composable
fun FocusScreen(request: FocusStartRequest = FocusStartRequest(), onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val store = remember { (context.applicationContext as? ViraApp)?.let { ViraStore(it.backend.db) } }
    val controller = remember {
        FocusRuntime.current.value ?: FocusSessionController(
            store = store,
            scope = FocusRuntime.scope,
            effects = store?.let { AndroidFocusEffects(context.applicationContext, it) } ?: FocusEffects.None,
            kind = request.kind,
            plannedSeconds = request.seconds,
            linkedTitle = request.linkedTitle,
            linkedItemID = request.linkedItemID,
        )
    }
    val profiles by remember { store?.db?.focusProfiles()?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val items by remember { store?.db?.planItems()?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf(false) }
    var notificationsDenied by remember { mutableStateOf(notificationsDenied(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsDenied = notificationsDenied(context)
    }

    val modes = FocusMode.all(profiles.filter { !it.isArchived })
    val pending = items.filter { it.statusRaw == PlanItemStatus.PENDING.raw }
    val today = LocalDate.now()
    val tasks = pending.filter { it.day.atZone(ZoneId.systemDefault()).toLocalDate() == today || it.id == controller.linkedItemID }

    LaunchedEffect(Unit) {
        controller.applyRequestedProfile(request.profileID)
        if (request.autostart && controller.phase == FocusSessionController.Phase.SETUP) controller.start()
    }
    // Arribant amb el títol i prou, la tasca es lliga si n'hi ha una de sola amb aquell nom.
    LaunchedEffect(pending.size) {
        val title = controller.linkedTitle
        if (controller.linkedItemID == null && title != null) {
            pending.singleOrNull { it.title == title }?.let { controller.linkedItemID = it.id }
        }
    }
    // En tornar a primer pla, si el temps ja s'havia esgotat, la sessió es completa ara.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsDenied = notificationsDenied(context)
        scope.launch { controller.reconcile() }
    }
    // TalkBack anuncia el final: la pantalla canvia sola i, sense veure-la, no se sabria.
    LaunchedEffect(controller.completion) {
        val completion = controller.completion ?: return@LaunchedEffect
        view.announceForAccessibility(
            context.getString(if (completion.wasInterrupted) R.string.focus_interrupted_title else R.string.focus_done_title),
        )
    }

    fun close() {
        FocusRuntime.release(controller)
        onClose()
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= 33 && !FocusNotifications.areEnabled(context) && !asked(context)) {
            ViraPreferences.open(context).edit().putBoolean(ASKED_KEY, true).apply()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch { controller.start() }
    }

    // Mentre corre, «Acaba» és l'única sortida: el gest d'enrere no tanca una sessió a mitges.
    BackHandler {
        when {
            editing -> editing = false
            controller.phase == FocusSessionController.Phase.RUNNING -> Unit
            else -> close()
        }
    }

    if (editing) {
        FocusProfileEditor(
            profile = null,
            onSave = { profile -> saveProfile(store, profile, isNew = true, context, scope) { controller.select(FocusMode.of(it)) }; editing = false },
            onBack = { editing = false },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        if (controller.phase == FocusSessionController.Phase.SETUP) {
            FocusAmbience()
        } else {
            Box(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Focus))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            FocusHeader(controller, onClose = ::close, onNewMode = { editing = true })
            // Un fos que aterra (`entrance`); amb Reduir moviment, el fos curt de sempre.
            val reduce = LocalViraEnvironment.current.reduceMotion
            AnimatedContent(
                targetState = controller.phase,
                transitionSpec = {
                    fadeIn(if (reduce) ViraMotion.reduced() else ViraMotion.entrance()) togetherWith fadeOut(ViraMotion.quick())
                },
                modifier = Modifier.weight(1f),
                label = "focus-phase",
            ) { phase ->
                when (phase) {
                    FocusSessionController.Phase.SETUP -> FocusSetupContent(
                        modes = modes,
                        mode = controller.mode,
                        minutes = controller.state.plannedSeconds / 60,
                        tasks = tasks,
                        linkedItemID = controller.linkedItemID,
                        notificationsDenied = notificationsDenied,
                        onSelectMode = { controller.select(it) },
                        onSelectMinutes = { controller.select(it * 60) },
                        onSelectTask = { item ->
                            controller.linkedItemID = item?.id
                            controller.linkedTitle = item?.title
                        },
                        onStart = ::start,
                    )
                    FocusSessionController.Phase.RUNNING -> FocusRunningContent(
                        state = controller.state,
                        now = rememberFocusClock(controller.state.run == FocusTimerState.Run.RUNNING),
                        modeName = if (controller.mode.profileID != null) controller.mode.label() else null,
                        onPause = controller::pause,
                        onResume = controller::resume,
                        onFinish = { scope.launch { controller.stop() } },
                    )
                    FocusSessionController.Phase.COMPLETED -> controller.completion?.let { completion ->
                        FocusCompletionContent(
                            completion = completion,
                            onStartBreak = { scope.launch { controller.startSuggestedBreak() } },
                            onClose = ::close,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FocusHeader(controller: FocusSessionController, onClose: () -> Unit, onNewMode: () -> Unit) {
    when (controller.phase) {
        FocusSessionController.Phase.SETUP -> Row(Modifier.padding(horizontal = ViraSpacing.xs).padding(top = ViraSpacing.xs)) {
            FocusIconButton(FocusGlyphKind.Close, stringResource(R.string.focus_close), ViraColor.textSecondary, onClose)
            Spacer(Modifier.weight(1f))
            // Crear un mode viu aquí i no al carrusel: un medalló de «+» s'hi llegiria com un mode més.
            FocusIconButton(FocusGlyphKind.Plus, stringResource(R.string.focus_modes_new), ViraColor.accent(ViraArea.Focus), onNewMode)
        }
        FocusSessionController.Phase.RUNNING -> SessionBar(controller) {
            // L'hora de sortida, no només el compte enrere: «a les 17:25» costa menys que «18 minuts».
            val end = controller.state.endDate
            if (controller.state.run == FocusTimerState.Run.RUNNING && end != null) {
                val clock = DateFormat.getTimeFormat(LocalContext.current).format(Date.from(end))
                FocusPill(stringResource(R.string.focus_until, clock), glyph = FocusGlyphKind.Clock)
            }
        }
        FocusSessionController.Phase.COMPLETED -> SessionBar(controller) {
            FocusIconButton(
                FocusGlyphKind.Close, stringResource(R.string.focus_close), ViraColor.onSurface(ViraArea.Focus), onClose,
                background = ViraColor.surface(ViraArea.Focus),
            )
        }
    }
}

/// La pastilla de la sessió: mode i tasca. El descans proposat diu «Descans», no el nom del mode.
@Composable
private fun SessionBar(controller: FocusSessionController, trailing: @Composable () -> Unit) {
    val title = when (controller.state.kind) {
        FocusKind.FOCUS -> controller.mode.label()
        FocusKind.SHORT_BREAK -> stringResource(R.string.focus_kind_short_break)
        FocusKind.LONG_BREAK -> stringResource(R.string.focus_kind_long_break)
    }
    FocusSessionBar(
        mode = controller.mode,
        title = title,
        taskTitle = if (controller.mode.showsLinkedTask) controller.linkedTitle else null,
        trailing = trailing,
    )
}

private const val ASKED_KEY = "focus.notificationPermissionAsked"

private fun asked(context: Context) = ViraPreferences.open(context).getBoolean(ASKED_KEY, false)

/// Comprovació de l'estat, no una sol·licitud: el permís es demana quan la sessió comença de veritat.
private fun notificationsDenied(context: Context) = asked(context) && !FocusNotifications.areEnabled(context)
