package cat.vira.android.features.plan

import cat.vira.android.core.sound.ViraSound
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.components.ViraIconPicker
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.notifications.ViraNotifications
import java.time.LocalTime
import java.time.ZoneId

/// Crear o editar un hàbit (`HabitEditor`): nom, icona, quins dies, notes i l'avís amb la seva
/// hora. El retorn del teclat desa, perquè el nom és d'una línia.
///
/// Encendre l'avís demana el permís d'avisos **en aquell moment**, mai abans.
@Composable
fun HabitEditor(habit: Habit?, actions: PlanActions, onDismiss: () -> Unit, zone: ZoneId = ZoneId.systemDefault()) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(habit?.title.orEmpty()) }
    var notes by remember { mutableStateOf(habit?.notes.orEmpty()) }
    var icon by remember { mutableStateOf(habit?.symbolName ?: ViraPictogramName.habits) }
    var weekdays by remember { mutableStateOf(habit?.weekdays?.toSet() ?: emptySet()) }
    var hasReminder by remember { mutableStateOf(habit?.reminderTime != null) }
    var reminder by remember { mutableStateOf(habit?.reminderTime?.atZone(zone)?.toLocalTime() ?: LocalTime.of(9, 0)) }
    var isDenied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> isDenied = !granted }
    // Un avís que ja hi era i ha perdut el permís ho diu al peu; el diàleg només surt en encendre'l.
    LaunchedEffect(Unit) { if (hasReminder && !ViraNotifications.canPost(context)) isDenied = true }
    val canSave = title.isNotBlank()

    fun save() {
        if (!canSave) return
        val reminderAt = if (hasReminder) PlanDay.at(PlanDay.today(), reminder.hour, reminder.minute, zone) else null
        val base = habit ?: Habit()
        actions.save(
            base.copy(
                title = title.trim(),
                notes = notes.ifEmpty { null },
                symbolName = icon,
                weekdays = WeekdaySelection.stored(weekdays),
                reminderTime = reminderAt,
            ),
            isNew = habit == null,
        )
        ViraSound.saved(context)
        onDismiss()
    }

    PlanSheet(
        title = stringResource(if (habit == null) R.string.habits_new else R.string.habits_edit),
        cancelLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(if (habit == null) R.string.action_add else R.string.action_done),
        confirmEnabled = canSave,
        onCancel = onDismiss,
        onConfirm = ::save,
    ) {
        FormSection {
            FormTextField(title, { title = it }, label = stringResource(R.string.habits_name_placeholder), onDone = ::save)
        }
        FormSection(header = stringResource(R.string.habits_icon)) {
            ViraIconPicker(ViraPictogramName.habitIcons, icon, { icon = it }, area = ViraArea.Plan)
        }
        FormSection(header = stringResource(R.string.habits_when)) {
            WeekdayFields(weekdays) { weekdays = it }
        }
        FormSection(header = stringResource(R.string.capture_optional)) {
            FormTextField(notes, { notes = it }, label = stringResource(R.string.habits_notes_placeholder), singleLine = false)
        }
        // L'avís va en un grup propi: activar-lo demana un permís i porta l'hora al darrere.
        // Un cop denegat, Android ja no torna a preguntar: callar deixaria un interruptor que no fa res.
        FormSection(
            header = stringResource(R.string.habits_reminder),
            footer = if (isDenied) {
                { ReminderDeniedFooter() }
            } else {
                null
            },
        ) {
            FormToggle(stringResource(R.string.habits_reminder_on), hasReminder, { isOn ->
                hasReminder = isOn
                isDenied = false
                if (isOn && !ViraNotifications.canPost(context)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(ViraNotifications.PERMISSION) else isDenied = true
                }
            })
            if (hasReminder) {
                FormDivider()
                FormTimeRow(stringResource(R.string.habits_reminder), reminder) { reminder = it }
            }
        }
    }
}
