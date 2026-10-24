package cat.vira.android.features.plan

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import cat.vira.android.R
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.data.model.form
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.add.ClockMinutes
import java.time.LocalTime

/// Crear o editar una pauta (`MedicationEditor`): què (nom, dosi, forma), quins dies, a quines
/// hores i l'avís. **Concloure** el tractament demana confirmació: deixa d'avisar i conserva
/// l'historial, i no és un "esborrar" que es pugui desfer amb un toc.
@Composable
fun MedicationEditor(medication: Medication?, actions: PlanActions, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(medication?.name.orEmpty()) }
    var dose by remember { mutableStateOf(medication?.dose.orEmpty()) }
    var form by remember { mutableStateOf(medication?.form ?: MedicationForm.TABLET) }
    var weekdays by remember { mutableStateOf(medication?.weekdays?.toSet() ?: emptySet()) }
    var times by remember { mutableStateOf(medication?.timesInMinutes?.sorted()?.ifEmpty { null } ?: listOf(9 * 60)) }
    var hasReminder by remember { mutableStateOf(medication?.hasReminder ?: true) }
    var confirmingEnd by remember { mutableStateOf(false) }
    var isDenied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> isDenied = !granted }
    // Una pauta nova neix amb l'avís encès: sense permís, es demana ara i no en tocar l'interruptor,
    // o es desaria un avís que no sonaria mai. Una que ja el tenia i ha perdut el permís ho diu
    // al peu, sense obrir cap diàleg només per entrar-hi.
    LaunchedEffect(Unit) {
        if (hasReminder && !ViraNotifications.canPost(context)) {
            if (medication == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(ViraNotifications.PERMISSION) else isDenied = true
        }
    }
    val canSave = name.isNotBlank() && times.isNotEmpty()

    fun save() {
        if (!canSave) return
        val base = medication ?: Medication()
        actions.save(
            base.copy(
                name = name.trim(),
                dose = dose.trim().ifEmpty { null },
                formRaw = form.raw,
                weekdays = WeekdaySelection.stored(weekdays),
                timesInMinutes = times.toSet().sorted(),
                hasReminder = hasReminder,
            ),
            isNew = medication == null,
        )
        onDismiss()
    }

    PlanSheet(
        title = stringResource(if (medication == null) R.string.medication_new else R.string.medication_edit),
        cancelLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(if (medication == null) R.string.action_add else R.string.action_done),
        confirmEnabled = canSave,
        onCancel = onDismiss,
        onConfirm = ::save,
    ) {
        FormSection(header = stringResource(R.string.medication_section_what)) {
            FormTextField(
                name, { name = it },
                label = stringResource(R.string.medication_name_label),
                placeholder = stringResource(R.string.medication_name_placeholder),
                capitalization = KeyboardCapitalization.Words,
            )
            FormDivider()
            FormTextField(
                dose, { dose = it },
                label = stringResource(R.string.medication_dose_label),
                placeholder = stringResource(R.string.medication_dose_placeholder),
            )
            FormDivider()
            // El text és l'únic que cal per distingir la forma, i és més estable a lletra gran.
            FormMenuRow(
                stringResource(R.string.medication_form),
                MedicationForm.entries.map { it to stringResource(it.title) },
                form,
            ) { form = it }
        }
        FormSection(header = stringResource(R.string.habits_when)) {
            WeekdayFields(weekdays) { weekdays = it }
        }
        FormSection(header = stringResource(R.string.medication_times)) {
            times.forEachIndexed { index, minutes ->
                if (index > 0) FormDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FormTimeRow(stringResource(DayPeriod.containing(minutes / 60).title), LocalTime.of(minutes / 60 % 24, minutes % 60)) { time ->
                            times = times.toMutableList().also { it[index] = time.hour * 60 + time.minute }
                        }
                    }
                    // A l'iPhone es treu lliscant; aquí, amb un botó a la mateixa fila.
                    if (times.size > 1) {
                        TextButton(onClick = { times = times.toMutableList().also { it.removeAt(index) } }) {
                            Text(stringResource(R.string.medication_times_remove), style = ViraFont.footnote, color = ViraColor.error)
                        }
                    }
                }
            }
            FormDivider()
            FormActionRow(stringResource(R.string.medication_times_add), ViraColor.accent(ViraArea.Plan)) {
                // La següent arrenca dues hores després de l'última, que és el que sol tocar.
                times = times + ClockMinutes.nextDose(times)
            }
        }
        FormSection(
            header = stringResource(R.string.habits_reminder),
            footer = {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    if (isDenied) ReminderDeniedFooter()
                    FormFooter(stringResource(R.string.medication_disclaimer))
                }
            },
        ) {
            FormToggle(stringResource(R.string.habits_reminder_on), hasReminder, { isOn ->
                hasReminder = isOn
                isDenied = false
                if (isOn && !ViraNotifications.canPost(context)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(ViraNotifications.PERMISSION) else isDenied = true
                }
            })
        }
        if (medication != null) {
            FormSection {
                FormActionRow(stringResource(R.string.medication_end), ViraColor.error) { confirmingEnd = true }
            }
        }
    }

    if (confirmingEnd && medication != null) {
        AlertDialog(
            onDismissRequest = { confirmingEnd = false },
            title = { Text(stringResource(R.string.medication_end_confirm), style = ViraFont.headline) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    Text(stringResource(R.string.medication_end_message), style = ViraFont.body)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingEnd = false
                    actions.end(medication)
                    onDismiss()
                }) { Text(stringResource(R.string.medication_end), style = ViraFont.headline, color = ViraColor.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingEnd = false }) {
                    Text(stringResource(R.string.action_cancel), style = ViraFont.body, color = ViraColor.accent(ViraArea.Plan))
                }
            },
        )
    }
}
