package cat.vira.android.features.add

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.sound.ViraSound
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.plan.DayPeriod
import cat.vira.android.features.plan.PlanDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// La cara **Tasca** del full «Afegir» (`QuickCaptureForm`): **què** → **quan**, el dia i l'hora en
/// una sola pantalla amb tres moments d'un toc a dalt (Arnau, 2026-10-24, opció «A + C») → **fet**,
/// una petita celebració amb «+ Una altra».
///
/// Si capturar una tasca costa més que recordar-la, l'eina no s'utilitza: el títol és l'únic que
/// cal. «Per avui» desa d'un toc des del primer pas. Al camp hi ha el micròfon, i el teclat no
/// s'obre sol enlloc, tampoc a «+ Una altra».
@Composable
fun TaskAddFlow(
    actions: AddSheetActions,
    onDismiss: () -> Unit,
    switcher: @Composable () -> Unit,
    onFirstStep: (Boolean) -> Unit,
    initialDay: LocalDate? = null,
    initialPeriod: DayPeriod? = null,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
    initialStep: TaskAddStep = TaskAddStep.What,
    initialTitle: String = "",
    /// Les captures de Fet hi posen una tasca ja desada.
    initialAdded: List<AddedTask> = emptyList(),
    /// Les captures de «Quan» hi posen els punts del calendari.
    initialDayCounts: Map<LocalDate, Int> = emptyMap(),
) {
    val inPreview = LocalInspectionMode.current
    val state = remember {
        TaskAddState(
            today = now.atZone(zone).toLocalDate(),
            initialDay = initialDay,
            initialPeriod = initialPeriod,
            now = now.atZone(zone).toLocalTime(),
            initialStep = initialStep,
            initialTitle = initialTitle,
            clock = if (inPreview) ({ now.atZone(zone).toLocalTime() }) else ({ java.time.LocalTime.now(zone) }),
        ).also {
            it.justAdded += initialAdded
            // Les captures no esperen la base de dades: «Quan» ja surt amb els moments i els punts.
            if (inPreview && initialStep == TaskAddStep.When) it.loadWhen(initialDayCounts, emptyList(), now.atZone(zone).toLocalDateTime())
        }
    }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val titleFocus = remember { FocusRequester() }
    // El micròfon: res a les captures, que no tenen reconeixedor.
    val dictation = remember { if (inPreview) null else TaskDictation(context) }
    LaunchedEffect(dictation) { dictation?.checkAvailability() }
    DisposableEffect(dictation) { onDispose { dictation?.stop() } }
    val micPermission = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) dictation?.start() else dictation?.denied()
    }
    LaunchedEffect(state.step) {
        onFirstStep(state.step == TaskAddStep.What)
        dictation?.stop()
        if (state.step == TaskAddStep.When && !inPreview) {
            val history = actions.recentTasks().take(200).mapNotNull {
                CaptureMoment.of(
                    day = PlanDay.date(it.day, zone),
                    time = it.time?.atZone(zone)?.toLocalDateTime(),
                    createdOn = PlanDay.date(it.createdAt, zone),
                )
            }
            // Els punts del calendari: el que queda d'aquest mes i els dos següents.
            val counts = actions.dayCounts(state.today, state.today.plusMonths(3))
            val current = if (inPreview) now.atZone(zone).toLocalDateTime() else java.time.LocalDateTime.now(zone)
            state.loadWhen(counts, history, current)
        }
    }
    val usual by produceState<List<CaptureUsual.Entry>?>(null) {
        value = CaptureUsual.pick(actions.recentTasks().map { CaptureUsual.Entry(it.title, it.symbolName) })
    }

    fun go(next: TaskAddStep) {
        focusManager.clearFocus()
        state.step = next
    }

    fun toggleMic() {
        val mic = dictation ?: return
        ViraHaptics.play(context, ViraHaptics.Feedback.Selection)
        if (mic.state == TaskDictation.State.Listening) return mic.stop()
        focusManager.clearFocus()
        // Si ja hi havia text, el que es diu s'hi afegeix al darrere.
        val typed = state.title.trim()
        mic.onText = { heard -> state.updateTitle(if (typed.isEmpty()) heard else "$typed $heard") }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            mic.start()
        } else {
            micPermission?.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun finish() {
        if (state.save(actions.createTask, zone)) {
            ViraSound.saved(context)
            go(TaskAddStep.Done)
        }
    }

    fun undone() {
        ViraHaptics.play(context, ViraHaptics.Feedback.Selection)
        ViraSound.play(context, ViraSound.Cue.UNDO)
    }

    /// «Següent», el retorn del teclat, un dia triat i «Desar». Quan no queda res per preguntar, desa.
    fun advance() {
        if (!state.canSave) return
        val next = state.nextStep()
        if (next == TaskAddStep.Done) finish() else go(next)
    }

    val callbacks = TaskStepCallbacks(
        onAdvance = ::advance,
        onPickUsual = { entry ->
            state.updateTitle(entry.title)
            state.symbolName = entry.symbolName
            advance()
        },
        onUndoLast = { state.undoLast(actions.deleteTasks); undone() },
        onUndoFromDone = { state.undoFromDone(actions.deleteTasks); undone() },
    )

    AddStepScaffold(
        step = state.step,
        topBar = {
            val closeTitle = when {
                state.step == TaskAddStep.Done -> null
                state.justAdded.isEmpty() -> stringResource(R.string.action_cancel)
                else -> stringResource(R.string.action_done)
            }
            AddTopBar(
                number = state.step.number,
                total = TaskAddStep.QUESTION_COUNT,
                canGoBack = state.step.previous(state.dayIsGiven, state.timeIsGiven) != null,
                closeTitle = closeTitle,
                onBack = { focusManager.clearFocus(); state.goBack() },
                onClose = onDismiss,
            )
        },
        switcher = switcher,
        actions = {
            TaskActions(
                state = state,
                onAdvance = ::advance,
                onSaveForToday = {
                    if (state.canSave) {
                        state.choose(state.today)
                        state.choose(AddTime.None)
                        finish()
                    }
                },
                onAnother = state::another,
                onFinish = onDismiss,
            )
        },
    ) { step ->
        when (step) {
            TaskAddStep.What -> TaskWhatStep(state, usual, titleFocus, callbacks, dictation, ::toggleMic)
            TaskAddStep.When -> TaskWhenStep(state) { moment ->
                // Un moment és la resposta sencera: dia i franja, i desa.
                state.pick(moment)
                advance()
            }
            TaskAddStep.Split -> Unit
            TaskAddStep.Done -> {
                val last = state.justAdded.lastOrNull()
                val coins by produceState<Int?>(null, last) {
                    value = last?.let { actions.coins(CoinReason.PLAN_ITEM, it.day) }
                }
                TaskDoneStep(last, coins, state.today, actions.planRemindersOn, callbacks)
            }
        }
    }
}

/// Els tocs de les pantalles de la cara Tasca, que viuen a `TaskAddSteps.kt`.
class TaskStepCallbacks(
    val onAdvance: () -> Unit,
    val onPickUsual: (CaptureUsual.Entry) -> Unit,
    val onUndoLast: () -> Unit,
    val onUndoFromDone: () -> Unit,
)

/// Els botons de baix de cada pas.
@Composable
private fun TaskActions(
    state: TaskAddState,
    onAdvance: () -> Unit,
    onSaveForToday: () -> Unit,
    onAnother: () -> Unit,
    onFinish: () -> Unit,
) {
    val area = ViraArea.Plan
    when (state.step) {
        TaskAddStep.What -> AddStepActions(area) {
            // «Per avui» (2026-10-22): escriure i un toc. No surt si el full s'ha obert des d'un altre dia.
            val forToday = @Composable { modifier: Modifier ->
                ViraActionButton(
                    stringResource(R.string.add_task_for_today), onClick = onSaveForToday, modifier = modifier,
                    hierarchy = ViraActionHierarchy.Secondary, area = area, enabled = state.canSave,
                )
            }
            val next = @Composable { modifier: Modifier ->
                ViraActionButton(stringResource(R.string.add_next), onClick = onAdvance, modifier = modifier, enabled = state.canSave)
            }
            if (LocalViraEnvironment.current.isAccessibilitySize) {
                if (state.offersForToday) forToday(Modifier.fillMaxWidth())
                next(Modifier.fillMaxWidth())
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    if (state.offersForToday) forToday(Modifier.weight(1f))
                    next(Modifier.weight(1f))
                }
            }
        }
        // Sense res triat, desa avui i sense hora: l'hora no és obligatòria.
        TaskAddStep.When -> AddStepActions(area) {
            ViraActionButton(stringResource(R.string.add_save), onClick = onAdvance, modifier = Modifier.fillMaxWidth())
        }
        TaskAddStep.Split -> Unit
        TaskAddStep.Done -> AddStepActions(area) {
            ViraActionButton("+ " + stringResource(R.string.add_task_another), onClick = onAnother, modifier = Modifier.fillMaxWidth())
            ViraActionButton(
                stringResource(R.string.add_task_finish), onClick = onFinish, modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
        }
    }
}
