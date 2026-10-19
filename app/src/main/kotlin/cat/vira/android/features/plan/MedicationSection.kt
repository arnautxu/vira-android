package cat.vira.android.features.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.viraLocale
import cat.vira.android.R
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.form
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate
import java.time.ZoneId

private val area = ViraArea.Plan

/// Medicació (`MedicationView`): les preses d'avui per franja i, a sota, les pautes.
///
/// **Cap gamificació** (D-27): marcar una presa no dona res, no fa cap bot ni cap so de premi, i
/// cap text no compara ni compta en contra. El disclaimer surt sempre, a mida llegible.
@Composable
fun MedicationSection(
    snapshot: PlanSnapshot,
    actions: PlanActions,
    chrome: PlanChrome,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medication?>(null) }
    val medications = snapshot.activeMedications
    val slots = MedicationSchedule.slots(medications, snapshot.intakes, today, zone)

    LaunchedEffect(chrome.adding) {
        if (chrome.adding == PlanSection.Medication) {
            chrome.adding = null
            creating = true
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = if (medications.isEmpty()) ViraSpacing.xxl else ViraSpacing.m, bottom = viraTabBarClearance() + ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(if (medications.isEmpty()) ViraSpacing.l else ViraSpacing.xl),
    ) {
        if (medications.isEmpty()) {
            ViraEmptyState(
                title = stringResource(R.string.medication_empty_title),
                message = stringResource(R.string.medication_empty_message),
                actionTitle = stringResource(R.string.medication_new),
                onAction = { creating = true },
                icon = { ViraPictogram(R.drawable.picto_medication, size = 56.dp) },
            )
        } else {
            TodaySection(slots, zone) { status, slot -> actions.record(status, slot) }
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                SectionHeader(stringResource(R.string.medication_schedule))
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    medications.forEach { medication -> MedicationScheduleRow(medication) { editing = medication } }
                }
            }
        }
        Text(
            stringResource(R.string.medication_disclaimer),
            style = ViraFont.footnote,
            color = ViraColor.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = ViraSpacing.s),
        )
    }

    if (creating) MedicationEditor(null, actions, onDismiss = { creating = false })
    editing?.let { MedicationEditor(it, actions, onDismiss = { editing = null }) }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.semantics { heading() })
}

@Composable
private fun TodaySection(slots: List<MedicationSchedule.Slot>, zone: ZoneId, onSelect: (IntakeStatus, MedicationSchedule.Slot) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        if (slots.isEmpty()) {
            SectionHeader(stringResource(R.string.medication_today))
            Text(
                stringResource(R.string.today_empty_message),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
                modifier = Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)).padding(ViraSpacing.m),
            )
        } else {
            MedicationTodayHero(slots, zone)
            DayPeriod.entries.forEach { period ->
                val inPeriod = slots.filter { DayPeriod.containing(it.scheduledAt, zone) == period }
                if (inPeriod.isNotEmpty()) {
                    Text(
                        stringResource(period.title).uppercase(),
                        style = ViraFont.footnoteEmphasis,
                        color = ViraColor.textSecondary,
                        modifier = Modifier.padding(top = ViraSpacing.xs).semantics { heading() },
                    )
                    inPeriod.forEach { slot -> MedicationSlotRow(slot, zone) { onSelect(it, slot) } }
                }
            }
        }
    }
}

/// El pastiller d'avui (`MedicationTodayHero`): "1/2 preses", la següent i un compartiment per
/// hora. Cap compartiment vermell ni cap retret: saltada és una vora discontínua.
@Composable
private fun MedicationTodayHero(slots: List<MedicationSchedule.Slot>, zone: ZoneId) {
    val taken = slots.count { it.status == IntakeStatus.TAKEN }
    val next = slots.firstOrNull { it.status == IntakeStatus.PENDING || it.status == IntakeStatus.LATER }
    val ink = ViraColor.onCard(area)
    val columns = if (LocalViraEnvironment.current.isAccessibilitySize) 2 else slots.size.coerceIn(1, 4)
    val message = next?.let {
        stringResource(R.string.medication_hero_next_message, it.medication.name, PlanFormat.time(it.scheduledAt.atZone(zone).toLocalTime()))
    } ?: stringResource(R.string.medication_hero_done_message)

    Column(
        Modifier.fillMaxWidth().background(ViraGradient.card(area, ViraGradient.Axis.Vertical), RoundedCornerShape(ViraRadius.l)).padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            ProgressRing(taken, slots.size, stringResource(R.string.medication_hero_doses))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(
                    stringResource(if (next == null) R.string.medication_hero_done_title else R.string.medication_hero_title),
                    style = ViraFont.title,
                    color = ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(message, style = ViraFont.footnote, color = ink)
            }
        }
        Column(Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            slots.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    row.forEach { Compartment(it, zone, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Compartment(slot: MedicationSchedule.Slot, zone: ZoneId, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.s)
    val isTaken = slot.status == IntakeStatus.TAKEN
    val ink = if (isTaken) ViraColor.onAccent else ViraColor.onCard(area)
    Row(
        modifier.defaultMinSize(minHeight = ViraTouch.minimum)
            .then(
                when (slot.status) {
                    IntakeStatus.TAKEN -> Modifier.background(ViraColor.accent(area), shape)
                    IntakeStatus.SKIPPED -> Modifier.dashedBorder(ViraColor.onCard(area).copy(alpha = 0.3f), ViraRadius.s, 1.5.dp)
                    else -> Modifier.background(ViraColor.lightArtworkVeil.copy(alpha = 0.45f), shape)
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(PlanFormat.time(slot.scheduledAt.atZone(zone).toLocalTime()), style = ViraFont.footnoteEmphasis, color = ink)
        if (isTaken) PlanGlyph(PlanGlyphShape.Check, ink, size = 12.dp * LocalDensity.current.fontScale, weight = 0.17f)
    }
}

/// Una presa d'avui (`MedicationSlotRow`): forma, hora, nom, dosi i estat en text; el cercle marca
/// "Presa" i el ⋯ ofereix Saltada i Més tard. Tornar a triar el mateix estat el desmarca.
@Composable
private fun MedicationSlotRow(slot: MedicationSchedule.Slot, zone: ZoneId, onSelect: (IntakeStatus) -> Unit) {
    val status = slot.status
    val isTaken = status == IntakeStatus.TAKEN
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = if (isTaken) ViraColor.onCard(area) else ViraColor.textPrimary
    val secondary = if (isTaken) ViraColor.onSurface(area) else ViraColor.textSecondary
    val time = PlanFormat.time(slot.scheduledAt.atZone(zone).toLocalTime())
    val dose = slot.medication.dose?.trim().orEmpty()
    val state = when (status) {
        IntakeStatus.TAKEN -> slot.intake?.loggedAt?.let { stringResource(R.string.medication_taken_at, PlanFormat.time(it.atZone(zone).toLocalTime())) }
            ?: stringResource(R.string.medication_status_taken)
        IntakeStatus.SKIPPED -> stringResource(R.string.medication_status_skipped)
        IntakeStatus.LATER -> stringResource(R.string.medication_status_later)
        IntakeStatus.PENDING -> ""
    }
    val detail = listOf(dose, state).filter { it.isNotEmpty() }.joinToString(" · ")
    val options = listOf(IntakeStatus.TAKEN, IntakeStatus.SKIPPED, IntakeStatus.LATER)
    val optionLabels = options.map { stringResource(it.title) }
    val statusLabel = stringResource(status.title)
    val spoken = listOf(time, slot.medication.name, detail).filter { it.isNotEmpty() }.joinToString(", ")
    var menu by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth()
            .then(
                when (status) {
                    IntakeStatus.TAKEN -> Modifier.background(ViraGradient.card(area, ViraGradient.Axis.Vertical), shape)
                    IntakeStatus.SKIPPED -> Modifier.dashedBorder(ViraColor.textSecondary.copy(alpha = 0.4f), ViraRadius.l, 1.5.dp)
                    else -> Modifier.background(ViraColor.surfaceElevated, shape)
                },
            )
            .semantics {
                contentDescription = spoken
                stateDescription = statusLabel
                customActions = options.mapIndexed { index, option -> CustomAccessibilityAction(optionLabels[index]) { onSelect(option); true } }
            }
            .padding(start = ViraSpacing.m, end = ViraSpacing.xs, top = ViraSpacing.s, bottom = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ViraPictogram(slot.medication.form.pictogram, Modifier.alpha(if (status == IntakeStatus.SKIPPED) 0.5f else 1f).clearAndSetSemantics { }, size = 48.dp)
            // L'hora a sobre del nom i no en una columna pròpia: amb la columna, un nom llarg es
            // partia a mitja paraula.
            Column(Modifier.weight(1f).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(time, style = ViraFont.footnoteEmphasis, color = if (isTaken) ViraColor.onCard(area) else ViraColor.onSurface(area))
                Text(slot.medication.name, style = ViraFont.headline, color = ink, maxLines = 2)
                if (detail.isNotEmpty()) Text(detail, style = ViraFont.footnote, color = secondary)
            }
            Box {
                val more = stringResource(R.string.a11y_more)
                Box(
                    Modifier.size(ViraTouch.minimum).viraPressable { menu = true }.clearAndSetSemantics { contentDescription = more; role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) { PlanGlyph(PlanGlyphShape.Ellipsis, secondary, size = 18.dp * LocalDensity.current.fontScale) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(optionLabels[index], style = if (status == option) ViraFont.headline else ViraFont.body) },
                            leadingIcon = if (status == option) {
                                { PlanGlyph(PlanGlyphShape.Check, ViraColor.textPrimary, size = 14.dp, weight = 0.16f) }
                            } else {
                                null
                            },
                            onClick = { menu = false; onSelect(option) },
                        )
                    }
                }
            }
            IntakeCheck(status) { onSelect(IntakeStatus.TAKEN) }
        }
        if (status == IntakeStatus.SKIPPED) {
            Text(stringResource(R.string.medication_skipped_note), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
    }
}

/// El cercle de "Presa". Sense bot: confirmar no és celebrar.
@Composable
private fun IntakeCheck(status: IntakeStatus, onClick: () -> Unit) {
    val accent = ViraColor.accent(area)
    val isTaken = status == IntakeStatus.TAKEN
    val ring = if (status == IntakeStatus.SKIPPED) ViraColor.textSecondary.copy(alpha = 0.5f) else accent
    val label = stringResource(R.string.medication_status_taken)
    Box(
        Modifier.size(ViraTouch.minimum).viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = label; role = Role.Button; selected = isTaken },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).then(if (isTaken) Modifier.background(accent, CircleShape) else Modifier.border(2.5.dp, ring, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (isTaken) PlanGlyph(PlanGlyphShape.Check, ViraColor.onAccent, size = 16.dp, weight = 0.17f)
        }
    }
}

/// Una pauta (`MedicationScheduleRow`): forma, nom, dosi, quins dies i les hores. Tocar-la l'edita.
@Composable
private fun MedicationScheduleRow(medication: Medication, onClick: () -> Unit) {
    val locale = viraLocale()
    val times = medication.timesInMinutes.sorted().joinToString(" · ") { PlanFormat.minutesIn(it, locale) }
    Row(
        Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
            .viraPressable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(medication.form.pictogram, Modifier.clearAndSetSemantics { }, size = ViraTouch.minimum, scalesWithText = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.Bottom) {
                Text(medication.name, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.weight(1f, fill = false))
                medication.dose?.takeIf { it.isNotEmpty() }?.let { Text(it, style = ViraFont.footnote, color = ViraColor.textSecondary) }
            }
            Text(
                stringResource(if (medication.weekdays.isEmpty()) R.string.habits_when_everyday else R.string.habits_when_somedays),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
            )
            Text(times, style = ViraFont.footnote, color = ViraColor.onSurface(area))
        }
        PlanGlyph(PlanGlyphShape.ChevronRight, ViraColor.textSecondary, size = 14.dp * LocalDensity.current.fontScale, weight = 0.14f)
    }
}

