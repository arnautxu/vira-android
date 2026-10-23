package cat.vira.android.features.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.plan.DayPeriod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// La cara **Tasca** del full «Afegir» (`QuickCaptureForm`): **què** → **quan** (un toc i passa) →
/// **hora**, opcional, amb «Es repeteix» → **fet**, una petita celebració amb «+ Una altra».
///
/// Si capturar una tasca costa més que recordar-la, l'eina no s'utilitza: el títol és l'únic que
/// cal. «Per avui» desa d'un toc des del primer pas.
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
) {
    val state = remember {
        TaskAddState(
            today = now.atZone(zone).toLocalDate(),
            initialDay = initialDay,
            initialPeriod = initialPeriod,
            now = now.atZone(zone).toLocalTime(),
            initialStep = initialStep,
            initialTitle = initialTitle,
        ).also { it.justAdded += initialAdded }
    }
    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var focusTitle by remember { mutableStateOf(false) }
    LaunchedEffect(state.step) { onFirstStep(state.step == TaskAddStep.What) }
    LaunchedEffect(focusTitle) {
        if (focusTitle) runCatching { titleFocus.requestFocus() }
        focusTitle = false
    }
    val usual by produceState<List<CaptureUsual.Entry>?>(null) {
        value = CaptureUsual.pick(actions.recentTasks().map { CaptureUsual.Entry(it.title, it.symbolName) })
    }

    fun go(next: TaskAddStep) {
        focusManager.clearFocus()
        state.step = next
    }

    fun finish() {
        if (state.save(actions.createTask, zone)) go(TaskAddStep.Done)
    }

    /// «Següent», el retorn del teclat, un dia triat i «Desar». Quan no queda res per preguntar, desa.
    fun advance() {
        if (!state.canSave) return
        val next = state.nextStep()
        if (next == TaskAddStep.Done) finish() else go(next)
    }

    val callbacks = TaskStepCallbacks(
        onAdvance = ::advance,
        onPickDay = { day ->
            // Un dia és una resposta sencera: es veu el check un moment i passa.
            state.choose(day)
            scope.launch {
                delay(220)
                if (state.step == TaskAddStep.When) advance()
            }
        },
        onPickUsual = { entry ->
            state.updateTitle(entry.title)
            state.symbolName = entry.symbolName
            advance()
        },
        onUndoLast = { state.undoLast(actions.deleteTasks) },
        onUndoFromDone = { state.undoFromDone(actions.deleteTasks) },
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
                canGoBack = state.step.previous(state.dayIsGiven) != null,
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
                onSaveWithoutTime = {
                    state.choose(AddTime.None)
                    finish()
                },
                onAnother = {
                    state.another()
                    focusTitle = true
                },
                onFinish = onDismiss,
            )
        },
    ) { step ->
        when (step) {
            TaskAddStep.What -> TaskWhatStep(state, usual, titleFocus, callbacks)
            TaskAddStep.When -> TaskWhenStep(state, callbacks, zone)
            TaskAddStep.Time -> TaskTimeStep(state)
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
    val onPickDay: (LocalDate) -> Unit,
    val onPickUsual: (CaptureUsual.Entry) -> Unit,
    val onUndoLast: () -> Unit,
    val onUndoFromDone: () -> Unit,
)

/// Els botons de baix de cada pas. A Quan no n'hi ha: un dia és la resposta.
@Composable
private fun TaskActions(
    state: TaskAddState,
    onAdvance: () -> Unit,
    onSaveForToday: () -> Unit,
    onSaveWithoutTime: () -> Unit,
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
        TaskAddStep.When -> Unit
        TaskAddStep.Time -> AddStepActions(area) {
            ViraActionButton(stringResource(R.string.add_save), onClick = onAdvance, modifier = Modifier.fillMaxWidth(), enabled = state.hasTime)
            ViraActionButton(
                stringResource(R.string.capture_time_none), onClick = onSaveWithoutTime, modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
        }
        TaskAddStep.Done -> AddStepActions(area) {
            ViraActionButton("+ " + stringResource(R.string.add_task_another), onClick = onAnother, modifier = Modifier.fillMaxWidth())
            ViraActionButton(
                stringResource(R.string.add_task_finish), onClick = onFinish, modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
        }
    }
}
