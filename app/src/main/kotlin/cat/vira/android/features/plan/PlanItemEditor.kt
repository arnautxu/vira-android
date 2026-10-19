package cat.vira.android.features.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.recurrence
import cat.vira.android.core.design.components.ViraIconPicker
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/// Editar un ítem del calendari (`PlanItemEditor`). **No reobre la captura ràpida**: editar és el
/// moment en què ja saps què vols canviar, i és aquí on tenen sentit el dia concret, les notes i
/// l'esborrat. Els camps són els mateixos que es poden triar en crear-lo, ni un més.
///
/// `isNew` crea en lloc de desar i amaga el botó d'esborrar. Pla crea amb la captura ràpida
/// d'Avui; queda per a qui necessiti l'editor sencer per a un ítem nou.
@Composable
fun PlanItemEditor(
    item: PlanItem,
    actions: PlanActions,
    onDismiss: () -> Unit,
    isNew: Boolean = false,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var notes by remember(item.id) { mutableStateOf(item.notes.orEmpty()) }
    var symbol by remember(item.id) { mutableStateOf(item.symbolName) }
    var day by remember(item.id) { mutableStateOf(PlanDay.date(item.day, zone)) }
    var time by remember(item.id) { mutableStateOf(item.time?.atZone(zone)?.toLocalTime()) }
    var recurrence by remember(item.id) { mutableStateOf(item.recurrence) }
    val canSave = title.isNotBlank()

    fun save() {
        if (!canSave) return
        val edited = item.copy(
            title = title.trim(),
            notes = notes.ifEmpty { null },
            symbolName = symbol,
            day = PlanDay.start(day, zone),
            time = time?.let { PlanDay.at(day, it.hour, it.minute, zone) },
        )
        if (isNew) actions.create(edited, recurrence) else actions.save(item, edited, recurrence)
        onDismiss()
    }

    PlanSheet(
        title = stringResource(if (isNew) R.string.capture_title else R.string.plan_edit),
        cancelLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(if (isNew) R.string.action_add else R.string.action_done),
        confirmEnabled = canSave,
        onCancel = onDismiss,
        onConfirm = ::save,
    ) {
        FormSection {
            FormTextField(title, { title = it }, label = null, placeholder = stringResource(R.string.capture_title_placeholder), onDone = ::save)
        }
        FormSection(header = stringResource(R.string.capture_icon)) {
            ViraIconPicker(ViraPictogramName.planIcons, symbol, { symbol = it }, area = ViraArea.Plan)
        }
        FormSection {
            // Aquí sí que hi ha calendari complet, i no el parell avui/demà de la captura: si
            // estàs editant, mous una cosa a un dia que ja tens al cap.
            FormDateRow(stringResource(R.string.capture_when), day) { day = it }
            FormDivider()
            FormToggle(stringResource(R.string.capture_has_time), checked = time != null, onCheckedChange = { isOn ->
                time = if (isOn) LocalTime.of(DayPeriod.Morning.defaultHour, 0) else null
            })
            time?.let { current ->
                FormDivider()
                FormTimeRow(stringResource(R.string.capture_time), current) { time = it }
            }
            FormDivider()
            FormMenuRow(
                stringResource(R.string.plan_repeat),
                PlanItemRecurrence.entries.map { it to stringResource(PlanItemSeries.title(it)) },
                recurrence,
            ) { recurrence = it }
            FormDivider()
            FormTextField(notes, { notes = it }, label = null, placeholder = stringResource(R.string.plan_notes_placeholder), singleLine = false)
        }
        // L'esborrat va al final i en secció pròpia: es troba quan es busca i no s'ensopega quan
        // no. Sense diàleg: l'ítem es pot tornar a escriure.
        if (!isNew) {
            FormSection {
                FormActionRow(stringResource(R.string.plan_delete), ViraColor.error) {
                    actions.delete(item)
                    onDismiss()
                }
            }
        }
    }
}
