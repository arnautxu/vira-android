package cat.vira.android.features.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cat.vira.android.app.EmotionsSection
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraNavigator
import cat.vira.android.app.PlanSection
import cat.vira.android.app.ViraDestination
import cat.vira.android.app.ViraFocusLink
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.components.LocalActiveCompletion
import cat.vira.android.core.design.components.LocalPresentCompletion
import cat.vira.android.core.design.components.LocalUndoCompletion
import cat.vira.android.core.design.components.PendingCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/// Les preferències que Avui llegeix, amb les mateixes claus que `ViraPreferences` de l'iPhone.
internal object TodayKeys {
    const val HABITS_MODULE = "settings.moduleHabitsEnabled"
    const val MEDICATION_MODULE = "settings.moduleMedicationEnabled"
    const val VIRI_EXPANDED = "settings.todayViriExpanded"
    const val TOMORROW_EXPANDED = "settings.todayTomorrowExpanded"
}

/// La pestanya Avui (`TodayView`): llegeix les dades, la navegació i la cua de «Desfer» de l'arrel
/// i ho passa a `TodayContent`, que és qui dibuixa.
@Composable
fun TodayScreen() {
    val backend = LocalViraBackend.current
    val navigator = LocalViraNavigator.current
    val present = LocalPresentCompletion.current
    val undoCompletion = LocalUndoCompletion.current
    val active = LocalActiveCompletion.current
    val context = LocalContext.current
    val db = backend.db
    val prefs = backend.preferences
    val zone = remember { ZoneId.systemDefault() }
    val actions = rememberTodayActions()
    // Escriure no ha de morir amb la pantalla: un «Desfer» que arriba quan ja s'ha canviat de
    // pestanya encara ha de desfer.
    val scope = ProcessLifecycleOwner.get().lifecycleScope

    val planItems by remember(db) { db.planItems().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val habits by remember(db) { db.habits().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val habitLogs by remember(db) { db.habitLogs().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val medications by remember(db) { db.medications().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val intakes by remember(db) { db.medicationIntakes().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val profile by remember(db) { db.profiles().observe() }.collectAsStateWithLifecycle(null)

    val habitsEnabled by rememberPreference(prefs, TodayKeys.HABITS_MODULE, true)
    val medicationEnabled by rememberPreference(prefs, TodayKeys.MEDICATION_MODULE, true)
    val viriExpanded by rememberPreference(prefs, TodayKeys.VIRI_EXPANDED, false)
    val tomorrowExpanded by rememberPreference(prefs, TodayKeys.TOMORROW_EXPANDED, false)
    val calendarEnabled by rememberPreference(prefs, TodayCalendar.ENABLED_KEY, false)

    // L'hora amb què Viri decideix què dir: es refà cada minut, que és quan pot canviar la franja.
    val now by produceState(Instant.now()) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            value = Instant.now()
        }
    }
    val today = TodayPlanRules.startOfDay(now, zone)

    // El calendari del sistema no són dades de Vira: es torna a llegir cada cop que la pantalla
    // torna a primer pla, que és l'única manera de veure'n un d'afegit des d'una altra app.
    var calendarEvents by remember { mutableStateOf(emptyList<TodayCalendarEvent>()) }
    var calendarReads by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        calendarReads += 1
        onPauseOrDispose { }
    }
    LaunchedEffect(today, calendarEnabled, calendarReads) {
        calendarEvents = if (calendarEnabled) {
            withContext(Dispatchers.IO) { TodayCalendar.events(context, prefs, today, zone) }
        } else {
            emptyList()
        }
    }

    var moved by remember { mutableStateOf<PlanItem?>(null) }
    var captureTomorrow by remember { mutableStateOf<Boolean?>(null) }

    val data = TodayData(
        now = now,
        zone = zone,
        planItems = planItems,
        habits = habits,
        habitLogs = habitLogs,
        medications = medications,
        intakes = intakes,
        calendarEvents = calendarEvents,
        displayName = profile?.displayName.orEmpty(),
        habitsEnabled = habitsEnabled,
        medicationEnabled = medicationEnabled,
        moved = moved?.let { original -> planItems.firstOrNull { it.id == original.id } },
    )

    val callbacks = TodayCallbacks(
        onToggle = { entry ->
            when (val kind = entry.kind) {
                is AgendaEntry.Kind.Plan -> scope.launch {
                    val id = entry.id
                    if (actions.toggle(kind.item)) {
                        present(PendingCompletion(id, CoinReason.PLAN_ITEM, onUndo = { scope.launch { actions.reopen(kind.item) } }))
                    } else {
                        undoCompletion(id)
                    }
                }
                // Un toc: la presa. Sense «Desfer» ni recompensa: la medicació no en dona cap, i
                // tornar-la a tocar ja la desmarca a l'instant.
                is AgendaEntry.Kind.Medication -> entry.time?.let { at -> scope.launch { actions.recordTaken(kind.medication, at) } }
                is AgendaEntry.Kind.Calendar -> {
                    TodayCalendar.toggleLocalCompletion(prefs, kind.event, today, zone)
                    calendarReads += 1
                }
            }
        },
        onToggleHabit = { habit ->
            scope.launch {
                val id = data.habitCompletionId(habit)
                val log = actions.toggle(habit, today)
                if (log != null) {
                    present(
                        PendingCompletion(
                            id = id,
                            reason = CoinReason.HABIT,
                            onUndo = { scope.launch { actions.undo(log) } },
                            onCommit = { actions.countHabitCompleted() },
                            title = habit.title,
                        ),
                    )
                } else {
                    undoCompletion(id)
                }
            }
        },
        // L'editor d'una tasca és del Pla; mentre no hi sigui, s'obre el calendari, on és.
        onEdit = { navigator.navigate(ViraDestination.Plan(PlanSection.Calendar)) },
        // Sense confirmació: esborrar ja demana premuda llarga i triar l'opció d'un menú.
        onDelete = { item -> scope.launch { actions.delete(item) } },
        onCapture = { tomorrow -> captureTomorrow = tomorrow },
        onMoveToTomorrow = { item -> scope.launch { moved = actions.moveToTomorrow(item) } },
        onUndoMove = {
            val original = moved
            moved = null
            if (original != null) scope.launch { actions.undoMove(original) }
        },
        onStartFocus = { item -> navigator.startFocus(ViraFocusLink(item.title, item.id)) },
        onOpenCalendar = { navigator.navigate(ViraDestination.Plan(PlanSection.Calendar)) },
        onOpenHabits = { navigator.navigate(ViraDestination.Plan(PlanSection.Habits)) },
        onOpenCheckIn = { navigator.navigate(ViraDestination.Emotions(EmotionsSection.CheckIn)) },
        onViriExpandedChange = { prefs.edit { putBoolean(TodayKeys.VIRI_EXPANDED, it) } },
        onTomorrowExpandedChange = { prefs.edit { putBoolean(TodayKeys.TOMORROW_EXPANDED, it) } },
    )

    TodayContent(
        data = data,
        completion = TodayCompletion(active, undoCompletion),
        callbacks = callbacks,
        isViriExpanded = viriExpanded,
        isTomorrowExpanded = tomorrowExpanded,
    )

    captureTomorrow?.let { forTomorrow ->
        QuickCaptureScreen(
            onDismiss = { captureTomorrow = null },
            initialDay = if (forTomorrow) data.tomorrow else null,
        )
    }
}

/// Les escriptures d'Avui i de la captura ràpida, sobre les dades de l'execució.
@Composable
internal fun rememberTodayActions(): TodayActions {
    val backend = LocalViraBackend.current
    return remember(backend) { TodayActions(ViraStore(backend.db)) }
}
