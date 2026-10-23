package cat.vira.android.features.add

import android.icu.text.ListFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.plan.DayPeriod

// 2 · A quina hora

/// Una fila per presa, amb − i +; «Una altra hora» n'afegeix una dues hores més tard. «Dies
/// concrets» obre els set cercles.
@Composable
fun MedicationTimeStep(state: MedicationAddState) {
    AddStepHeading(stringResource(R.string.add_medication_time_title), eyebrow = state.trimmedName)
    AddChoiceCard(stringResource(if (state.picksDays) R.string.habits_when_somedays else R.string.habits_when_everyday)) {
        state.times.forEachIndexed { index, minutes ->
            val period = DayPeriod.containing(minutes / 60)
            val removable = state.times.size > 1
            AddStepperRow(
                pictogram = AddTime.Period(period).pictogram,
                label = stringResource(period.title),
                value = clockText(minutes),
                onDecrement = { state.move(index, -1) },
                onIncrement = { state.move(index, 1) },
                removeLabel = if (removable) stringResource(R.string.medication_times_remove) else null,
                onRemove = if (removable) ({ state.remove(index) }) else null,
            )
        }
        AddChoiceRow(ViraPictogramName.planAlarm, stringResource(R.string.add_medication_another_time), isSelected = false, onClick = state::addTime)
        AddChoiceRow(
            ViraPictogramName.calendar, stringResource(R.string.habits_when_somedays), stringResource(R.string.add_habit_days_custom_hint),
            isSelected = state.picksDays, onClick = state::togglePicksDays,
        )
        AnimatedVisibility(state.picksDays) {
            AddWeekdayPicker(state.weekdays, { state.weekdays = it })
        }
    }
}

// 3 · T'avisem?

/// Sí o no, i a sota la nota de privacitat (si el nom no surt a l'avís) i l'avís legal de sempre.
@Composable
fun MedicationReminderStep(state: MedicationAddState, showsNameInReminder: Boolean) {
    AddStepHeading(stringResource(R.string.add_medication_reminder_title), eyebrow = state.trimmedName)
    AddChoiceCard(stringResource(R.string.add_habit_reminder_card)) {
        AddChoiceRow(
            ViraPictogramName.planReminder, stringResource(R.string.add_medication_reminder_yes), timesSummary(state.times),
            isSelected = state.hasReminder,
        ) { state.hasReminder = true }
        AddChoiceRow(
            ViraPictogramName.planChecklist, stringResource(R.string.add_medication_reminder_no), stringResource(R.string.add_medication_reminder_no_hint),
            isSelected = !state.hasReminder,
        ) { state.hasReminder = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        if (state.hasReminder && !showsNameInReminder) {
            Text(stringResource(R.string.add_medication_reminder_private), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        Text(stringResource(R.string.medication_disclaimer), style = ViraFont.footnote, color = ViraColor.textSecondary)
    }
}

/// «8:00 i 21:00» per a la resposta «Sí» de l'avís.
@Composable
private fun timesSummary(times: List<Int>): String =
    ListFormatter.getInstance(viraLocale()).format(times.toSet().sorted().map(::clockText))
