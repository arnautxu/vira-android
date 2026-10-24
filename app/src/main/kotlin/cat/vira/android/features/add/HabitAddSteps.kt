package cat.vira.android.features.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.plan.DayPeriod

/// Les sis idees del primer pas (`HabitIdeas`).
private val habitIdeas = listOf(
    R.string.add_habit_idea_water to ViraPictogramName.habitWater,
    R.string.add_habit_idea_read to ViraPictogramName.habitRead,
    R.string.add_habit_idea_tidy to ViraPictogramName.habitTidy,
    R.string.add_habit_idea_walk to ViraPictogramName.habitWalk,
    R.string.add_habit_idea_study to ViraPictogramName.habitStudy,
    R.string.add_habit_idea_disconnect to ViraPictogramName.habitDisconnect,
)

// 1 · Què

/// Les idees passen d'un toc; el camp és per al teu, amb «Següent» o el retorn.
@Composable
fun HabitWhatStep(state: HabitAddState, callbacks: HabitStepCallbacks) {
    AddStepScene(AddScene.Habit, stringResource(R.string.add_habit_what_title), stringResource(R.string.add_habit_what_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        AddTitleField(
            value = state.title,
            onValueChange = { state.title = it },
            placeholder = stringResource(R.string.add_habit_placeholder),
            iconName = state.iconName,
            isPickingIcon = state.isPickingIcon,
            onToggleIcon = { state.isPickingIcon = !state.isPickingIcon },
            onNext = callbacks.onAdvance,
        )
        AnimatedVisibility(state.isPickingIcon) {
            AddIconRow(ViraPictogramName.habitIcons, state.iconName, { state.iconName = it }, modifier = Modifier.bleed(ViraSpacing.screenMargin))
        }
    }
    AddChoiceCard(stringResource(R.string.add_task_ideas), stringResource(R.string.add_task_when_pill)) {
        val ideas = habitIdeas.map { (title, icon) -> stringResource(title) to icon }
        ViraTileGrid(ideas) { (title, icon), modifier ->
            AddChoiceTile(icon, title, isSelected = false, modifier = modifier) { callbacks.onPickIdea(title, icon) }
        }
    }
}

// 2 · Quins dies

/// Tres dreceres d'un toc i «Tria'ls», que obre els set cercles.
@Composable
fun HabitDaysStep(state: HabitAddState, callbacks: HabitStepCallbacks) {
    AddStepScene(AddScene.Board, stringResource(R.string.add_habit_days_title), state.trimmedTitle)
    AddChoiceCard(stringResource(R.string.add_habit_days_card), stringResource(R.string.add_task_when_pill)) {
        WeekdayPreset.entries.forEach { preset ->
            AddChoiceRow(
                preset.pictogram, stringResource(preset.title), stringResource(preset.hint),
                isSelected = !state.picksDays && state.weekdays == preset.days,
            ) { callbacks.onPickDays(preset.days) }
        }
        AddChoiceRow(
            ViraPictogramName.planStar, stringResource(R.string.add_habit_days_custom), stringResource(R.string.add_habit_days_custom_hint),
            isSelected = state.picksDays,
        ) { state.picksDays = !state.picksDays }
        AnimatedVisibility(state.picksDays) {
            AddWeekdayPicker(state.weekdays, { state.weekdays = it }, showsPresets = false)
        }
    }
}

private val WeekdayPreset.hint: Int
    get() = when (this) {
        WeekdayPreset.Everyday -> R.string.add_habit_days_everyday_hint
        WeekdayPreset.Weekdays -> R.string.add_habit_days_weekdays_hint
        WeekdayPreset.Weekend -> R.string.add_habit_days_weekend_hint
    }

private val WeekdayPreset.pictogram: String
    get() = when (this) {
        WeekdayPreset.Everyday -> ViraPictogramName.calendar
        WeekdayPreset.Weekdays -> ViraPictogramName.planChecklist
        WeekdayPreset.Weekend -> ViraPictogramName.calFreetime
    }

// 3 · T'ho recordo?

/// Les tres franges en fitxes i, si se'n tria una, l'hora amb − i +. «Sense avís» desa sense.
@Composable
fun HabitReminderStep(state: HabitAddState, callbacks: HabitStepCallbacks) {
    AddStepScene(
        AddScene.Board,
        stringResource(R.string.add_habit_reminder_title),
        stringResource(R.string.add_task_time_subtitle, weekdaysSummary(state.weekdays)),
    )
    AddChoiceCard(stringResource(R.string.add_habit_reminder_card), stringResource(R.string.add_task_time_pill)) {
        val tile = @Composable { period: DayPeriod, modifier: Modifier ->
            AddChoiceTile(
                AddTime.Period(period).pictogram, stringResource(period.title),
                isSelected = state.reminderPeriod == period, modifier = modifier,
            ) { callbacks.onSetReminder(period.defaultHour * 60) }
        }
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                DayPeriod.entries.forEach { tile(it, Modifier.fillMaxWidth()) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                DayPeriod.entries.forEach { tile(it, Modifier.weight(1f)) }
            }
        }
        state.reminderMinutes?.let { minutes ->
            AddStepperRow(
                pictogram = ViraPictogramName.planAlarm,
                label = stringResource(R.string.add_habit_reminder_at),
                value = clockText(minutes),
                onDecrement = { state.reminderMinutes = ClockMinutes.moved(minutes, -1) },
                onIncrement = { state.reminderMinutes = ClockMinutes.moved(minutes, 1) },
            )
        }
    }
    // Un cop denegat el permís, Android ja no torna a preguntar. Callar deixaria un avís triat que
    // no sonarà mai.
    if (state.isRemindersDenied && state.reminderMinutes != null) {
        val context = LocalContext.current
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.labelGap)) {
            Text(stringResource(R.string.habits_reminder_denied), style = ViraFont.footnote, color = ViraColor.textSecondary)
            ViraActionButton(
                stringResource(R.string.action_open_settings),
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Plan,
            )
        }
    }
}

// Fet

/// La mateixa celebració petita que la tasca. «Desfer» l'arxiva, com treure un hàbit.
@Composable
fun HabitDoneStep(state: HabitAddState, coins: Int?, callbacks: HabitStepCallbacks) {
    AddStepScene(
        AddScene.HabitDone, stringResource(R.string.add_habit_done_title), stringResource(R.string.add_habit_done_subtitle),
        heightRatio = 0.82f,
    )
    val created = state.created ?: return
    val days = weekdaysSummary(state.weekdays)
    AddDoneCard(
        newLabel = stringResource(R.string.add_habit_done_new),
        whenText = state.reminderMinutes?.let { days + " · " + clockText(it) } ?: days,
        symbolName = created.symbolName,
        title = created.title,
        rewardLead = stringResource(R.string.add_reward_habit),
        coins = coins,
        onUndo = callbacks.onUndo,
    )
}
