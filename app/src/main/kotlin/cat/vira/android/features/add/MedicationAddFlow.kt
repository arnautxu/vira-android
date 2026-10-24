package cat.vira.android.features.add

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.features.plan.WeekdaySelection
import cat.vira.android.features.plan.pictogram
import cat.vira.android.features.plan.title

/// Les respostes d'una pauta a mig crear (`MedicationAddFlow`).
@Stable
class MedicationAddState(initialStep: MedicationAddStep, initialName: String, initialTimes: List<Int>) {
    var step by mutableStateOf(initialStep)
    var name by mutableStateOf(initialName)
    var dose by mutableStateOf("")
    var form by mutableStateOf(MedicationForm.TABLET)
    var weekdays by mutableStateOf(emptySet<Int>())
    var picksDays by mutableStateOf(false)
    var times by mutableStateOf(initialTimes)
    var hasReminder by mutableStateOf(true)

    val trimmedName: String get() = name.trim()

    /// El nom és l'únic obligatori; sense cap hora (no passa: la llista no es buida) no hi ha pauta.
    val canAdvance: Boolean get() = trimmedName.isNotEmpty() && times.isNotEmpty()

    /// La següent hora arrenca dues hores després de l'última, que és el que sol tocar en una pauta
    /// de dues o tres preses.
    fun addTime() {
        times = times + ((times.maxOrNull() ?: (9 * 60)) + 120) % ClockMinutes.DAY_LENGTH
    }

    fun move(index: Int, steps: Int) {
        if (index !in times.indices) return
        times = times.toMutableList().also { it[index] = ClockMinutes.moved(it[index], steps) }
    }

    fun remove(index: Int) {
        if (index !in times.indices || times.size <= 1) return
        times = times.toMutableList().also { it.removeAt(index) }
    }

    /// Tancar «Dies concrets» torna a cada dia: amagar els dies triats i desar-los igualment faria
    /// una pauta que no és la que es veu.
    fun togglePicksDays() {
        picksDays = !picksDays
        if (!picksDays) weekdays = emptySet()
    }
}

/// Crear una pauta al full «Afegir», **tres preguntes i prou** (`MedicationAddFlow`): **què prens**
/// (nom, dosi opcional i forma) → **a quina hora** (una fila per presa) → **t'avisem?** «Desar»
/// tanca el full.
///
/// Neutre a posta: **sense escena, sense Viri, sense Fet, sense premis i sense so**. La pregunta va en
/// gran i a sobre hi ha el nom que s'ha escrit. L'editor d'una pàgina es queda per editar i concloure.
@Composable
fun MedicationAddFlow(
    actions: AddSheetActions,
    onDismiss: () -> Unit,
    switcher: @Composable () -> Unit,
    onFirstStep: (Boolean) -> Unit,
    initialStep: MedicationAddStep = MedicationAddStep.What,
    initialName: String = "",
    initialTimes: List<Int> = listOf(9 * 60),
) {
    val state = remember { MedicationAddState(initialStep, initialName, initialTimes) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val inPreview = LocalInspectionMode.current
    // El permís es demana en desar, com si l'haguessis encès tu, i no a la primera presa que no sonarà.
    val permission = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rescheduleAfterGrant(context)
        onDismiss()
    }
    LaunchedEffect(state.step) { onFirstStep(state.step == MedicationAddStep.What) }

    fun save() {
        if (!state.canAdvance) return
        actions.saveMedication(
            Medication(
                name = state.trimmedName,
                dose = state.dose.trim().ifEmpty { null },
                formRaw = state.form.raw,
                weekdays = WeekdaySelection.stored(state.weekdays),
                timesInMinutes = state.times.toSet().sorted(),
                hasReminder = state.hasReminder,
            ),
        )
        // Sense so: la medicació no en té. Només la vibració de desar.
        ViraHaptics.play(context, ViraHaptics.Feedback.Saved)
        // Amb els avisos de medicació apagats a Ajustos, l'avís no sortiria igualment: no es pregunta.
        val asks = state.hasReminder && actions.medicationRemindersOn && !inPreview && !ViraNotifications.canPost(context)
        if (asks && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permission != null) {
            permission.launch(ViraNotifications.PERMISSION)
        } else {
            onDismiss()
        }
    }

    fun advance() {
        if (!state.canAdvance) return
        focusManager.clearFocus()
        state.step.next?.let { state.step = it } ?: save()
    }

    AddStepScaffold(
        step = state.step,
        topBar = {
            AddTopBar(
                number = state.step.number,
                canGoBack = state.step.previous != null,
                closeTitle = stringResource(R.string.action_cancel),
                onBack = { state.step.previous?.let { state.step = it } },
                onClose = onDismiss,
            )
        },
        switcher = switcher,
        actions = {
            AddStepActions {
                ViraActionButton(
                    stringResource(if (state.step == MedicationAddStep.Reminder) R.string.add_save else R.string.add_next),
                    onClick = ::advance, modifier = Modifier.fillMaxWidth(), enabled = state.canAdvance,
                )
            }
        },
    ) { step ->
        when (step) {
            MedicationAddStep.What -> MedicationWhatStep(state, ::advance)
            MedicationAddStep.Time -> MedicationTimeStep(state)
            MedicationAddStep.Reminder -> MedicationReminderStep(state, actions.medicationNameInReminder)
        }
    }
}

// 1 · Què prens

/// Sense focus automàtic: obrir el teclat de cop amb el nom d'un fàrmac a la vista no és el que vol
/// ningú en una sala d'espera.
@Composable
private fun MedicationWhatStep(state: MedicationAddState, onNext: () -> Unit) {
    AddStepHeading(stringResource(R.string.add_medication_what_title))
    LabeledField(stringResource(R.string.medication_name_label), stringResource(R.string.add_medication_name_placeholder), state.name, { state.name = it }, isMain = true, onNext = onNext)
    LabeledField(stringResource(R.string.medication_dose_label), stringResource(R.string.medication_dose_placeholder), state.dose, { state.dose = it }, isMain = false, onNext = onNext)
    AddChoiceCard(stringResource(R.string.medication_form)) {
        val columns = if (LocalViraEnvironment.current.isAccessibilitySize) 1 else 3
        MedicationForm.entries.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                row.forEach { option ->
                    FormTile(option, state.form == option, Modifier.weight(1f)) { state.form = option }
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, placeholder: String, value: String, onChange: (String) -> Unit, isMain: Boolean, onNext: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .background(ViraColor.surfaceElevated, shape)
            .then(if (isMain) Modifier.border(2.dp, ViraColor.accent(ViraArea.Plan), shape) else Modifier)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Text(label, style = ViraFont.footnote, color = ViraColor.textSecondary)
        AddTextField(
            value, onChange, placeholder, if (isMain) ViraFont.title else ViraFont.headline, onNext,
            Modifier.fillMaxWidth(),
            capitalization = if (isMain) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            label = label,
        )
    }
}

/// Fitxa d'una forma (`MedicationFormTile`): pictograma i nom; la triada, tenyida i amb check.
@Composable
private fun FormTile(form: MedicationForm, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.m)
    Box(
        modifier
            .defaultMinSize(minHeight = 76.dp)
            .background(if (isSelected) ViraColor.surface(ViraArea.Plan) else ViraColor.surfaceElevated, shape)
            .then(if (isSelected) Modifier.border(2.5.dp, ViraColor.accent(ViraArea.Plan), shape) else Modifier)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .padding(ViraSpacing.xs),
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            ViraPictogram(form.pictogram, size = 34.dp, scalesWithText = true)
            Text(
                stringResource(form.title),
                style = if (isSelected) ViraFont.footnoteEmphasis else ViraFont.footnote,
                color = ViraColor.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ViraColor.accent(ViraArea.Plan), modifier = Modifier.align(Alignment.TopEnd).size(16.dp))
        }
    }
}
