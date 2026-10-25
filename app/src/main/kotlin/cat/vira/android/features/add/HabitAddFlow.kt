package cat.vira.android.features.add

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.sound.ViraSound
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.plan.DayPeriod
import cat.vira.android.features.plan.PlanDay
import cat.vira.android.features.plan.WeekdaySelection
import cat.vira.android.features.plan.isScheduled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/// Les respostes d'un hàbit a mig crear (`HabitAddFlow`).
@Stable
class HabitAddState(initialStep: HabitAddStep, initialTitle: String) {
    var step by mutableStateOf(initialStep)
    var title by mutableStateOf(initialTitle)
    var iconName by mutableStateOf(ViraPictogramName.habits)
    var isPickingIcon by mutableStateOf(false)
    /// Dies (1 = diumenge, com l'iPhone). Buit = cada dia.
    var weekdays by mutableStateOf(emptySet<Int>())
    /// Si ja s'ha triat una drecera de dies. Buit vol dir «cada dia», i sense això «Cada dia» sortia
    /// ja marcat en arribar al pas, abans de tocar res (Arnau, 2026-10-23).
    var hasPickedDays by mutableStateOf(false)
    var picksDays by mutableStateOf(false)
    /// Minuts del dia de l'avís, o `null` si no n'hi ha.
    var reminderMinutes by mutableStateOf<Int?>(null)
    var isRemindersDenied by mutableStateOf(false)
    var created by mutableStateOf<Habit?>(null)

    val trimmedTitle: String get() = title.trim()
    val canSave: Boolean get() = trimmedTitle.isNotEmpty()

    /// La franja de l'avís, per marcar la fitxa. Les 9:15 segueixen sent Matí.
    val reminderPeriod: DayPeriod? get() = reminderMinutes?.let { DayPeriod.containing(it / 60) }
}

/// Crear un hàbit al full «Afegir», **una pregunta cada vegada** (`HabitAddFlow`): **què** (idees
/// d'un toc) → **quins dies** (un toc) → **t'ho recordo?** (opcional) → **fet**, amb «Ja l'he fet
/// avui» si avui toca. L'editor d'una pàgina del Pla es queda per editar; les notes només hi són allà.
@Composable
fun HabitAddFlow(
    actions: AddSheetActions,
    onDismiss: () -> Unit,
    switcher: @Composable () -> Unit,
    onFirstStep: (Boolean) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
    initialStep: HabitAddStep = HabitAddStep.What,
    initialTitle: String = "",
    /// Les captures de Fet hi posen un hàbit ja desat.
    initialCreated: Habit? = null,
) {
    val state = remember { HabitAddState(initialStep, initialTitle).also { it.created = initialCreated } }
    val today = remember(now, zone) { now.atZone(zone).toLocalDate() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val inPreview = LocalInspectionMode.current
    val permission = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.isRemindersDenied = !granted
        if (granted) rescheduleAfterGrant(context)
    }
    LaunchedEffect(state.step) { onFirstStep(state.step == HabitAddStep.What) }

    fun go(next: HabitAddStep) {
        focusManager.clearFocus()
        state.step = next
    }

    fun save() {
        if (!state.canSave) return
        val habit = Habit(
            title = state.trimmedTitle,
            symbolName = state.iconName,
            weekdays = WeekdaySelection.stored(state.weekdays),
            reminderTime = state.reminderMinutes?.let { PlanDay.at(today, it / 60, it % 60, zone) },
        )
        actions.saveHabit(habit)
        ViraSound.saved(context)
        state.created = habit
        go(HabitAddStep.Done)
    }

    fun advance() {
        if (!state.canSave) return
        if (state.step == HabitAddStep.Reminder) save() else go(state.step.next)
    }

    /// Triar una hora és el moment de demanar el permís: **en context**, no a l'arrencada.
    fun setReminder(minutes: Int?) {
        val wasOff = state.reminderMinutes == null
        state.reminderMinutes = minutes
        if (minutes == null) {
            state.isRemindersDenied = false
        } else if (wasOff && !inPreview && !ViraNotifications.canPost(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission?.launch(ViraNotifications.PERMISSION) else state.isRemindersDenied = true
        }
    }

    val callbacks = HabitStepCallbacks(
        onAdvance = ::advance,
        onPickIdea = { title, icon ->
            // Una idea és una resposta sencera: nom, icona i al pas següent.
            state.title = title
            state.iconName = icon
            advance()
        },
        onPickDays = { days ->
            // Una drecera de dies passa sola: es veu el check un moment i avança.
            state.weekdays = days
            state.hasPickedDays = true
            state.picksDays = false
            scope.launch {
                delay(220)
                if (state.step == HabitAddStep.Days) advance()
            }
        },
        onSetReminder = ::setReminder,
        onUndo = {
            // «Desfer» a Fet: l'arxiva, i torna al camp amb el nom per si només calia arreglar-lo.
            state.created?.let(actions.archiveHabit)
            ViraHaptics.play(context, ViraHaptics.Feedback.Selection)
            ViraSound.play(context, ViraSound.Cue.UNDO)
            state.created = null
            go(HabitAddStep.What)
        },
    )

    AddStepScaffold(
        step = state.step,
        topBar = {
            AddTopBar(
                number = state.step.number,
                canGoBack = state.step.previous != null,
                closeTitle = if (state.step == HabitAddStep.Done) null else stringResource(R.string.action_cancel),
                onBack = { state.step.previous?.let(::go) },
                onClose = onDismiss,
            )
        },
        switcher = switcher,
        actions = {
            val area = ViraArea.Plan
            when (state.step) {
                HabitAddStep.What -> AddStepActions(area) {
                    ViraActionButton(stringResource(R.string.add_next), onClick = ::advance, modifier = Modifier.fillMaxWidth(), enabled = state.canSave)
                }
                // Les dreceres passen soles; només els dies triats a mà necessiten un botó.
                HabitAddStep.Days -> if (state.picksDays) AddStepActions(area) {
                    ViraActionButton(stringResource(R.string.add_next), onClick = ::advance, modifier = Modifier.fillMaxWidth())
                }
                HabitAddStep.Reminder -> AddStepActions(area) {
                    ViraActionButton(stringResource(R.string.add_save), onClick = ::save, modifier = Modifier.fillMaxWidth(), enabled = state.reminderMinutes != null)
                    ViraActionButton(
                        stringResource(R.string.add_habit_reminder_none),
                        onClick = { setReminder(null); save() },
                        modifier = Modifier.fillMaxWidth(), hierarchy = ViraActionHierarchy.Textual, area = area,
                    )
                }
                HabitAddStep.Done -> AddStepActions(area) {
                    val created = state.created
                    if (created != null && created.isScheduled(today)) {
                        ViraActionButton(
                            "✓ " + stringResource(R.string.add_habit_first_done),
                            onClick = { actions.markHabitDoneToday(created); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ViraActionButton(
                            stringResource(R.string.add_task_finish), onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                            hierarchy = ViraActionHierarchy.Textual, area = area,
                        )
                    } else {
                        ViraActionButton(stringResource(R.string.add_task_finish), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { step ->
        when (step) {
            HabitAddStep.What -> HabitWhatStep(state, callbacks)
            HabitAddStep.Days -> HabitDaysStep(state, callbacks)
            HabitAddStep.Reminder -> HabitReminderStep(state, callbacks)
            HabitAddStep.Done -> {
                val coins by produceState<Int?>(null, state.created) { value = actions.coins(CoinReason.HABIT, today) }
                HabitDoneStep(state, coins, callbacks)
            }
        }
    }
}

/// Els tocs de les pantalles de crear un hàbit, que viuen a `HabitAddSteps.kt`.
class HabitStepCallbacks(
    val onAdvance: () -> Unit,
    val onPickIdea: (title: String, icon: String) -> Unit,
    val onPickDays: (Set<Int>) -> Unit,
    val onSetReminder: (Int?) -> Unit,
    val onUndo: () -> Unit,
)
