package cat.vira.android.features.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItem

/// Què es pot fer des de la bombolla de Viri. Cap acció escriu res que no es pugui desfer.
internal class TodayViriActions(
    val toggle: (AgendaEntry) -> Unit,
    val startFocus: (PlanItem) -> Unit,
    val capture: (tomorrow: Boolean) -> Unit,
    val openCheckIn: () -> Unit,
    val beGentle: () -> Unit,
    val skip: () -> Unit,
    val moveToTomorrow: (PlanItem) -> Unit,
    val undoMove: () -> Unit,
)

/// «1 cosa» / «3 coses»: el plural el resol el catàleg de textos per idioma.
@Composable
internal fun thingsText(count: Int): String = pluralStringResource(R.plurals.today_viri_count_things, count, count)

@Composable
internal fun habitsText(count: Int): String = pluralStringResource(R.plurals.today_viri_count_habits, count, count)

/// Les frases i els botons de cada moment (`TodayView+Viri.swift`).
@Composable
internal fun todayViriMoment(plan: TodayViriPlan, data: TodayData, actions: TodayViriActions): TodayViriMomentUi =
    when (plan) {
        is TodayViriPlan.Night -> nightMoment(plan, actions)
        is TodayViriPlan.Empty -> TodayViriMomentUi(
            id = "empty",
            scene = "viri-scene-plan-empty",
            title = greetingTitle(plan.isMorning, data.displayName),
            message = stringResource(R.string.today_viri_empty_message),
            actions = listOf(
                TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_capture), Icons.Filled.Add) { actions.capture(false) },
            ),
        )
        is TodayViriPlan.AllDone -> {
            var message = stringResource(R.string.today_viri_done_message)
            if (plan.habitsLeft > 0) {
                message += " " + pluralStringResource(R.plurals.today_viri_done_habits, plan.habitsLeft, plan.habitsLeft)
            }
            TodayViriMomentUi(
                id = "done",
                scene = "viri-home-done",
                title = stringResource(R.string.today_viri_done_title),
                message = message,
            )
        }
        is TodayViriPlan.Next -> nextMoment(plan, data, actions)
    }

@Composable
private fun nextMoment(plan: TodayViriPlan.Next, data: TodayData, actions: TodayViriActions): TodayViriMomentUi {
    val entry = plan.entry
    // Viri és neutre amb la medicació: no en diu el nom. El nom només surt a «El teu dia».
    val message = when {
        entry.isMedication && plan.isGentle -> stringResource(R.string.today_viri_gentle_message_medication)
        entry.isMedication && plan.isFirstLook ->
            stringResource(R.string.today_viri_first_message_medication, thingsText(plan.total), habitsText(plan.habitsTotal))
        entry.isMedication -> stringResource(R.string.today_viri_next_message_medication, plan.doneCount, plan.total)
        plan.isGentle -> stringResource(R.string.today_viri_gentle_message, itemLabel(entry, data))
        plan.isFirstLook ->
            stringResource(R.string.today_viri_first_message, thingsText(plan.total), habitsText(plan.habitsTotal), itemLabel(entry, data))
        else -> stringResource(R.string.today_viri_next_message, plan.doneCount, plan.total, itemLabel(entry, data))
    }

    val buttons = mutableListOf(primaryAction(plan, actions))
    when {
        plan.isGentle -> buttons += TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_checkin), run = actions.openCheckIn)
        plan.isFirstLook -> buttons += TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_hard), run = actions.beGentle)
        plan.canSkip -> buttons += TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_other), run = actions.skip)
    }

    return TodayViriMomentUi(
        id = "next-${entry.id}-${plan.isGentle}-${plan.isFirstLook}",
        // La primera mirada del dia només passa abans de migdia: és el «bon dia».
        scene = when {
            plan.isFirstLook -> "viri-home-morning"
            plan.isFocusTask -> "viri-suggest-focus-1"
            else -> "viri-suggest-capture-1"
        },
        title = if (plan.isGentle) stringResource(R.string.today_viri_gentle_title) else greetingTitle(plan.isMorning, data.displayName),
        message = message,
        actions = buttons,
    )
}

/// Medicació: «Presa». Tasca sense hora: Concentra't amb aquella tasca. La resta: «Fet».
@Composable
private fun primaryAction(plan: TodayViriPlan.Next, actions: TodayViriActions): TodayViriMomentUi.Action {
    val entry = plan.entry
    val item = entry.planItem
    return when {
        entry.isMedication ->
            TodayViriMomentUi.Action(stringResource(R.string.medication_status_taken), Icons.Filled.Check) { actions.toggle(entry) }
        item != null && item.time == null ->
            TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_focus), Icons.Filled.PlayArrow) { actions.startFocus(item) }
        else -> TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_done), Icons.Filled.Check) { actions.toggle(entry) }
    }
}

@Composable
private fun nightMoment(plan: TodayViriPlan.Night, actions: TodayViriActions): TodayViriMomentUi {
    val title = stringResource(R.string.today_viri_night_title)
    val captureTomorrow = TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_note_tomorrow), Icons.Filled.Add) {
        actions.capture(true)
    }
    val moved = plan.moved
    if (moved != null) {
        return TodayViriMomentUi(
            id = "night-moved-${moved.id}",
            scene = "viri-suggest-windDown-2",
            title = title,
            message = stringResource(R.string.today_viri_night_moved, moved.title),
            actions = listOf(TodayViriMomentUi.Action(stringResource(R.string.action_undo), run = actions.undoMove), captureTomorrow),
            proposes = true,
        )
    }

    var message = if (plan.doneCount + plan.habitsDone == 0) {
        stringResource(R.string.today_viri_night_rest)
    } else {
        stringResource(R.string.today_viri_night_summary, thingsText(plan.doneCount), habitsText(plan.habitsDone))
    }
    val buttons = mutableListOf(captureTomorrow)
    plan.moveCandidate?.let { item ->
        message += " " + stringResource(R.string.today_viri_night_move_question, item.title)
        buttons.add(
            0,
            TodayViriMomentUi.Action(stringResource(R.string.today_viri_action_tomorrow), Icons.AutoMirrored.Filled.ArrowForward) {
                actions.moveToTomorrow(item)
            },
        )
    }
    return TodayViriMomentUi(
        id = "night-${message.hashCode()}",
        scene = "viri-suggest-windDown-2",
        title = title,
        message = message,
        actions = buttons,
        proposes = plan.moveCandidate != null,
    )
}

/// La línia de la píndola plegada: què toca ara, sense dir mai el nom d'un fàrmac (Viri és neutre
/// amb la medicació).
@Composable
internal fun todayViriStatus(data: TodayData): String {
    if (data.hour >= TodayPlanRules.Period.Night.startHour) return stringResource(R.string.today_viri_night_title)
    val pending = data.pending
    if (!data.agenda.isEmpty && pending.isEmpty()) return stringResource(R.string.today_viri_status_done)
    val next = pending.firstOrNull { !it.isMedication } ?: return stringResource(R.string.today_viri_status_idle)
    return stringResource(R.string.today_viri_status_next, itemLabel(next, data))
}

@Composable
private fun greetingTitle(isMorning: Boolean, displayName: String): String {
    val name = displayName.trim()
    return when {
        name.isEmpty() && isMorning -> stringResource(R.string.today_viri_morning_generic)
        name.isEmpty() -> stringResource(R.string.today_viri_afternoon_generic)
        isMorning -> stringResource(R.string.today_viri_morning, name)
        else -> stringResource(R.string.today_viri_afternoon, name)
    }
}

@Composable
private fun itemLabel(entry: AgendaEntry, data: TodayData): String {
    val time = entry.time ?: return entry.title
    return stringResource(R.string.today_viri_item_at, entry.title, formatTime(time, data.zone))
}
