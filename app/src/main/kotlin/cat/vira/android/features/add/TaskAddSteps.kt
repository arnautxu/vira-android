package cat.vira.android.features.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
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
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.plan.DayPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val area = ViraArea.Plan

// 1 · Què

/// L'escena amb la pregunta, el camp i «Les de sempre» (o tres idees, si encara no n'hi ha). El
/// teclat no s'obre sol (Arnau, 2026-10-19 i 2026-10-23). Al camp, el micròfon per dir-ho
/// (`TaskDictation`), només si el telèfon reconeix la veu sense enviar-la fora.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskWhatStep(
    state: TaskAddState,
    usual: List<CaptureUsual.Entry>?,
    titleFocus: FocusRequester,
    callbacks: TaskStepCallbacks,
    dictation: TaskDictation? = null,
    onMic: () -> Unit = {},
) {
    val voice = dictation?.isAvailable == true
    val listening = dictation?.state == TaskDictation.State.Listening
    AddStepScene(
        AddScene.Capture,
        stringResource(R.string.add_task_what_title),
        stringResource(if (voice) R.string.add_task_what_subtitle_voice else R.string.add_task_what_subtitle),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        AddTitleField(
            value = state.title,
            onValueChange = state::updateTitle,
            placeholder = stringResource(if (listening) R.string.add_task_dictate_listening else R.string.add_task_placeholder),
            iconName = state.symbolName,
            isPickingIcon = state.isPickingIcon,
            onToggleIcon = { state.isPickingIcon = !state.isPickingIcon },
            onNext = callbacks.onAdvance,
            focusRequester = titleFocus,
            showsPencil = true,
            trailing = if (voice) ({ MicButton(listening, onMic) }) else null,
        )
        if (dictation?.state == TaskDictation.State.Denied) {
            Text(stringResource(R.string.add_task_dictate_denied), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
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

/// Parlar en lloc d'escriure. Escoltant, el cercle porta un anell i el botó atura.
@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (listening) R.string.add_task_dictate_stop else R.string.add_task_dictate)
    Box(
        Modifier
            .size(ViraTouch.minimum + 8.dp)
            .then(if (listening) Modifier.border(6.dp, ViraColor.accent(area).copy(alpha = 0.35f), CircleShape) else Modifier)
            .padding(if (listening) 4.dp else 0.dp)
            .background(ViraColor.accent(area), CircleShape)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (listening) DictationIcons.stop else DictationIcons.mic, contentDescription = null, tint = ViraColor.onAccent, modifier = Modifier.size(24.dp))
    }
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
