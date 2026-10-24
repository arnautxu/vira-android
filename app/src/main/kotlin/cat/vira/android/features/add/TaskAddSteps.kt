package cat.vira.android.features.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.plan.DayPeriod
import cat.vira.android.features.plan.FormTimeRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val area = ViraArea.Plan

// 1 · Què

/// L'escena amb la pregunta, el camp i «Les de sempre» (o tres idees, si encara no n'hi ha).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskWhatStep(state: TaskAddState, usual: List<CaptureUsual.Entry>?, titleFocus: FocusRequester, callbacks: TaskStepCallbacks) {
    AddStepScene(AddScene.Capture, stringResource(R.string.add_task_what_title), stringResource(R.string.add_task_what_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        AddTitleField(
            value = state.title,
            onValueChange = state::updateTitle,
            placeholder = stringResource(R.string.add_task_placeholder),
            iconName = state.symbolName,
            isPickingIcon = state.isPickingIcon,
            onToggleIcon = { state.isPickingIcon = !state.isPickingIcon },
            onNext = callbacks.onAdvance,
            focusRequester = titleFocus,
        )
        AnimatedVisibility(state.isPickingIcon) {
            AddIconRow(ViraPictogramName.planIcons, state.symbolName, { state.symbolName = it }, modifier = Modifier.bleed(ViraSpacing.screenMargin))
        }
        DetectedChips(state)
    }
    val ideas = listOf(
        CaptureUsual.Entry(stringResource(R.string.add_task_idea_homework), ViraPictogramName.planWrite),
        CaptureUsual.Entry(stringResource(R.string.add_task_idea_study), ViraPictogramName.habitStudy),
        CaptureUsual.Entry(stringResource(R.string.add_task_idea_call), ViraPictogramName.calCall),
    )
    if (usual != null) {
        val isHistory = usual.isNotEmpty()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            AddGroupTitle(stringResource(if (isHistory) R.string.add_task_usual else R.string.add_task_ideas))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                (if (isHistory) usual else ideas).forEach { entry ->
                    AddChip(entry.title, entry.symbolName) { callbacks.onPickUsual(entry) }
                }
            }
        }
    }
    if (state.justAdded.isNotEmpty()) AddedList(state.justAdded, state.today, callbacks.onUndoLast)
}

/// El que la frase ha llegit, en píndoles sota el camp: «Demà ×», «17:00 ×». La creu ho treu i la
/// frase es desa tal com s'ha escrit. No surt res si no s'ha llegit res.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectedChips(state: TaskAddState) {
    val day = state.detectedDay
    val time = state.detectedTime
    if (day == null && time == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.add_detected), style = ViraFont.footnote, color = ViraColor.textSecondary)
        if (day != null) RemovableChip(dayName(day, state.today, "EEEE d MMM"), state::clearDetectedDay)
        if (time != null) RemovableChip(clockText(time.hour * 60 + time.minute), state::clearDetectedTime)
    }
}

// 2 · Quan

/// Avui, Demà i els cinc dies següents. Un toc i passa al pas següent; «més» obre el calendari.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskWhenStep(state: TaskAddState, callbacks: TaskStepCallbacks, zone: ZoneId) {
    AddStepScene(AddScene.Board, stringResource(R.string.add_task_when_title), state.titleToSave)
    AddChoiceCard(stringResource(R.string.add_task_when_card), stringResource(R.string.add_task_when_pill)) {
        AddChoiceRow(
            ViraPictogramName.calToday, stringResource(R.string.capture_day_today), formatted(state.today, "EEEE d"),
            isSelected = state.dayWasChosen && state.day == state.today,
        ) { callbacks.onPickDay(state.today) }
        AddChoiceRow(
            ViraPictogramName.calendar, stringResource(R.string.capture_day_tomorrow), formatted(state.tomorrow, "EEEE d"),
            isSelected = state.dayWasChosen && state.day == state.tomorrow,
        ) { callbacks.onPickDay(state.tomorrow) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            state.laterDays.forEach { candidate ->
                DayCell(
                    top = formatted(candidate, "EEE"),
                    bottom = candidate.dayOfMonth.toString(),
                    label = formatted(candidate, "EEEE d MMMM"),
                    isSelected = state.dayWasChosen && state.day == candidate && !state.isPickingDay,
                ) { callbacks.onPickDay(candidate) }
            }
            val isOther = state.day != state.today && state.day != state.tomorrow && state.day !in state.laterDays
            DayCell(
                top = stringResource(R.string.add_task_when_more),
                bottom = "›",
                label = stringResource(R.string.capture_day_other),
                isSelected = state.isPickingDay || (state.dayWasChosen && isOther),
            ) { state.isPickingDay = !state.isPickingDay }
        }
        AnimatedVisibility(state.isPickingDay) { OtherDayPicker(state.today, zone, callbacks.onPickDay) }
    }
}

/// El calendari sencer. Triar-hi un dia és una resposta, com un toc a la tira: passa al pas següent.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherDayPicker(today: LocalDate, zone: ZoneId, onPick: (LocalDate) -> Unit) {
    // El selector treballa en mil·lisegons UTC: el dia s'hi posa i se'n treu en UTC.
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = null,
        initialDisplayedMonthMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        },
    )
    LaunchedEffect(picker.selectedDateMillis) {
        picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
    }
    DatePicker(
        state = picker,
        title = null,
        headline = null,
        showModeToggle = false,
        modifier = Modifier.background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)),
    )
}

// 3 · Hora

/// Les tres franges del Pla i «Hora exacta» en fitxes. Triar-ne una no passa sola: aquí hi ha també
/// la repetició, i «Desar» és qui tanca. «Sense hora» desa sense.
@Composable
fun TaskTimeStep(state: TaskAddState) {
    AddStepScene(
        AddScene.Board,
        stringResource(R.string.add_task_time_title),
        stringResource(R.string.add_task_time_subtitle, dayName(state.day, state.today, "EEEE d")),
    )
    AddChoiceCard(stringResource(R.string.add_task_time_card), stringResource(R.string.add_task_time_pill)) {
        ViraTileGrid(AddTime.choices) { option, modifier ->
            val (title, subtitle) = when (option) {
                is AddTime.Period -> stringResource(option.period.title) to
                    stringResource(R.string.add_task_time_around, clockText(option.period.defaultHour * 60))
                else -> stringResource(R.string.capture_time_exact) to
                    (state.time?.takeIf { state.isChosen(AddTime.Exact) }?.let { clockText(it.hour * 60 + it.minute) }
                        ?: stringResource(R.string.add_task_time_exact_hint))
            }
            AddChoiceTile(option.pictogram, title, subtitle, isSelected = state.isChosen(option), modifier = modifier) {
                if (option == AddTime.Exact) state.chooseExactTime() else state.choose(option)
            }
        }
        AnimatedVisibility(state.detectedTime == null && state.timeChoice == AddTime.Exact) {
            Box(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)).padding(horizontal = ViraSpacing.m)) {
                FormTimeRow(stringResource(R.string.capture_time_exact), state.exactTime) { state.exactTime = it }
            }
        }
    }
    AddRepeatRow(state.recurrence) { state.recurrence = it }
}

// Fet

/// Una celebració petita: l'escena, la bombolla i la targeta groga amb el que s'ha apuntat i el que
/// donarà fer-ho. El permís d'avisos, en context: només si la tasca té hora.
@Composable
fun TaskDoneStep(last: AddedTask?, coins: Int?, today: LocalDate, remindersOn: Boolean, callbacks: TaskStepCallbacks) {
    AddStepScene(
        AddScene.Done, stringResource(R.string.add_task_done_title), stringResource(R.string.add_task_done_subtitle),
        heightRatio = 0.82f,
    )
    if (last != null) {
        AddDoneCard(
            newLabel = stringResource(R.string.add_task_done_new),
            whenText = whenText(last.day, last.time, today),
            symbolName = last.symbolName,
            title = last.title,
            rewardLead = stringResource(R.string.add_reward_task),
            coins = coins,
            onUndo = callbacks.onUndoFromDone,
        )
        last.time?.let { AddNotificationPermissionCard(it, remindersOn) }
    }
}

// Peces

val AddTime.pictogram: String
    get() = when (this) {
        is AddTime.Period -> when (period) {
            DayPeriod.Morning -> ViraPictogramName.sun
            DayPeriod.Midday -> ViraPictogramName.calSun
            DayPeriod.Night -> ViraPictogramName.moon
        }
        else -> ViraPictogramName.planAlarm
    }

@Composable
private fun DayCell(top: String, bottom: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.s)
    Column(
        Modifier
            .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.comfortable)
            .background(if (isSelected) ViraColor.accent(area) else ViraColor.surfaceElevated, shape)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label; selected = isSelected }
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val ink = if (isSelected) ViraColor.onAccent else ViraColor.textPrimary
        Text(top, style = ViraFont.caption, color = ink)
        Text(bottom, style = ViraFont.headline, color = ink)
    }
}

@Composable
fun formatted(day: LocalDate, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, viraLocale()).format(day)

/// «Demà · 17:00», «Avui» o la data curta. Res de «Sense hora»: no cal dir el que no hi és.
@Composable
fun whenText(day: LocalDate, time: java.time.LocalTime?, today: LocalDate): String {
    val dayText = dayName(day, today, "EEE d MMM")
    return if (time == null) dayText else dayText + " · " + clockText(time.hour * 60 + time.minute)
}

/// «Avui», «Demà» o el dia amb el patró que toqui.
@Composable
fun dayName(day: LocalDate, today: LocalDate, pattern: String): String = when (day) {
    today -> stringResource(R.string.capture_day_today)
    today.plusDays(1) -> stringResource(R.string.capture_day_tomorrow)
    else -> formatted(day, pattern)
}
