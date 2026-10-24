package cat.vira.android.features.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing

/// El que Avui pot demanar a fora: escriure, obrir una altra pantalla o una captura.
internal class TodayCallbacks(
    val onToggle: (AgendaEntry) -> Unit = {},
    val onToggleHabit: (Habit) -> Unit = {},
    val onEdit: (PlanItem) -> Unit = {},
    val onDelete: (PlanItem) -> Unit = {},
    /// Obre la captura ràpida; `true` és per a demà («Apuntar per a demà»).
    val onCapture: (tomorrow: Boolean) -> Unit = {},
    val onMoveToTomorrow: (PlanItem) -> Unit = {},
    val onUndoMove: () -> Unit = {},
    val onStartFocus: (PlanItem) -> Unit = {},
    val onOpenCalendar: () -> Unit = {},
    val onOpenHabits: () -> Unit = {},
    val onOpenCheckIn: () -> Unit = {},
    /// Crea un hàbit d'una idea de la fila buida (`TodayHabitSuggestions`); el títol ja és traduït.
    val onSuggestHabit: (TodayHabitSuggestion, String) -> Unit = { _, _ -> },
    val onUndoSuggestedHabit: () -> Unit = {},
    val onTomorrowExpandedChange: (Boolean) -> Unit = {},
)

/// Inici sense estat (`TodayView`): Viri a sobre del dia, que diu què toca; després el dia, els hàbits i el
/// check-in; i al vespre, què hi ha demà. La capçalera fixa (`ViraTopBar`) és de l'arrel.
@Composable
internal fun TodayContent(
    data: TodayData,
    completion: TodayCompletion,
    callbacks: TodayCallbacks,
    isTomorrowExpanded: Boolean,
    modifier: Modifier = Modifier,
    /// L'hàbit creat des d'una idea, mentre s'ofereix «Desfer».
    suggestedHabit: Habit? = null,
) {
    // Quantes vegades s'ha tocat «Una altra cosa», i «Avui em costa»: Viri baixa el ritme.
    var skips by rememberSaveable { mutableIntStateOf(0) }
    var isGentle by rememberSaveable { mutableStateOf(false) }

    val moment = todayViriMoment(
        plan = data.viriPlan(skips, isGentle),
        data = data,
        actions = TodayViriActions(
            toggle = callbacks.onToggle,
            startFocus = callbacks.onStartFocus,
            capture = callbacks.onCapture,
            openCheckIn = callbacks.onOpenCheckIn,
            beGentle = { isGentle = true },
            skip = { skips += 1 },
            moveToTomorrow = callbacks.onMoveToTomorrow,
            undoMove = callbacks.onUndoMove,
        ),
    )

    Column(
        modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Today)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        // Viri a sobre del dia, com a assistent (Arnau, 2026-10-22).
        Column(Modifier.padding(top = ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            TodayViriPill(moment = moment, status = todayViriStatus(data))
            AgendaModule(
                // Mentre dura el «Desfer», la fila marcada es queda al seu lloc: el missatge s'ha de
                // poder llegir quiet (`Agenda.ordered(holding:)`).
                entries = data.agenda.ordered(holding = completion.activeId, now = data.now),
                completion = completion,
                zone = data.zone,
                onToggle = callbacks.onToggle,
                onEdit = callbacks.onEdit,
                onDelete = callbacks.onDelete,
                onAdd = { callbacks.onCapture(false) },
                onOpenCalendar = callbacks.onOpenCalendar,
            )
        }
        // Els hàbits no tenen hora i no han de competir amb una agenda.
        if (data.habitsEnabled) {
            TodayHabitsStrip(
                habits = data.todayHabits,
                doneIds = data.doneHabitIds,
                completion = completion,
                habitCompletionId = data::habitCompletionId,
                onToggle = callbacks.onToggleHabit,
                onOpenHabits = callbacks.onOpenHabits,
                // Les idees només surten quan no hi ha cap hàbit enlloc.
                onSuggest = callbacks.onSuggestHabit.takeIf { data.habits.none { !it.isArchived } },
            )
            suggestedHabit?.let { TodayAddedHabitRow(it, callbacks.onUndoSuggestedHabit) }
        }
        // Només «Com estàs?» (Arnau, 2026-10-09): la resta de dreceres ja són a un toc.
        TodayCheckInRow(onClick = callbacks.onOpenCheckIn)
        if (data.showsTomorrow) {
            TodayTomorrowCard(
                day = data.tomorrow,
                zone = data.zone,
                entries = data.tomorrowAgenda.entries,
                habitCount = data.tomorrowHabits.size,
                isExpanded = isTomorrowExpanded,
                onExpandedChange = callbacks.onTomorrowExpandedChange,
                onOpen = callbacks.onOpenCalendar,
            )
        }
        // La barra de pestanyes flota per sobre: l'últim bloc ha de poder pujar per sobre seu.
        Spacer(Modifier.height(viraTabBarClearance()))
    }
}
