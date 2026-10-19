package cat.vira.android.features.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.viraIdleBreath
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.plan.rememberPlanActions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/// Franja amb què s'obre la captura quan s'afegeix des d'un forat concret del calendari.
enum class QuickCapturePeriod(internal val rule: TodayPlanRules.Period) {
    Morning(TodayPlanRules.Period.Morning),
    Midday(TodayPlanRules.Period.Midday),
    Night(TodayPlanRules.Period.Night),
}

/// L'hora d'una captura: cap, una franja del dia o una hora exacta (`QuickCaptureTime`).
internal sealed interface QuickCaptureTime {
    data object None : QuickCaptureTime
    data class Period(val period: QuickCapturePeriod) : QuickCaptureTime
    data object Exact : QuickCaptureTime

    /// La franja es desa amb la seva hora per defecte, la mateixa que «Afegir aquí» del calendari.
    fun resolved(day: Instant, exactHour: Int, exactMinute: Int, zone: ZoneId): Instant? = when (this) {
        None -> null
        is Period -> day.atZone(zone).withHour(period.rule.defaultHour).withMinute(0).toInstant()
        Exact -> day.atZone(zone).withHour(exactHour).withMinute(exactMinute).toInstant()
    }
}

/// Captura ràpida: **un camp, i ja està** (`QuickCaptureView`). Si capturar una tasca costa més que
/// recordar-la, l'eina no s'utilitza: el títol és l'únic obligatori i tot té valor per defecte.
///
/// A pantalla completa. La fan servir Avui (el «+» i Viri) i el Pla (amb el dia i la franja del
/// forat on s'ha tocat).
@Composable
fun QuickCaptureScreen(
    onDismiss: () -> Unit,
    initialDay: Instant? = null,
    initialPeriod: QuickCapturePeriod? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // La mateixa escriptura que el Pla: les repeticions, la transacció i els avisos.
        val actions = rememberPlanActions()
        QuickCaptureContent(
            initialDay = initialDay,
            initialPeriod = initialPeriod,
            onCancel = onDismiss,
            onSave = { title, day, time, symbol, recurrence ->
                val trimmed = title.trim()
                if (trimmed.isNotEmpty()) {
                    actions.create(PlanItem(title = trimmed, day = day, time = time, symbolName = symbol), recurrence)
                    onDismiss()
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun QuickCaptureContent(
    initialDay: Instant?,
    initialPeriod: QuickCapturePeriod?,
    onCancel: () -> Unit,
    onSave: (title: String, day: Instant, time: Instant?, symbolName: String, recurrence: PlanItemRecurrence) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
) {
    val today = remember(now, zone) { TodayPlanRules.startOfDay(now, zone) }
    val tomorrow = remember(today) { TodayPlanRules.addDays(today, 1, zone) }
    var title by rememberSaveable { mutableStateOf("") }
    var symbolName by rememberSaveable { mutableStateOf(ViraPictogramName.planIcons.first()) }
    var selectedDay by remember { mutableStateOf(initialDay?.let { TodayPlanRules.startOfDay(it, zone) } ?: today) }
    var isPickingDay by remember { mutableStateOf(false) }
    var timeChoice by remember { mutableStateOf<QuickCaptureTime>(initialPeriod?.let { QuickCaptureTime.Period(it) } ?: QuickCaptureTime.None) }
    var recurrence by remember { mutableStateOf(PlanItemRecurrence.NONE) }
    val nowLocal = now.atZone(zone)
    val timeState = rememberTimePickerState(nowLocal.hour, nowLocal.minute)
    val canSave = title.isNotBlank()
    val isOtherDay = selectedDay != today && selectedDay != tomorrow

    fun save() {
        if (!canSave) return
        onSave(title, selectedDay, timeChoice.resolved(selectedDay, timeState.hour, timeState.minute, zone), symbolName, recurrence)
    }

    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today).statusBarsPadding().imePadding()) {
        // Barra: Cancel·la i el títol del full.
        Box(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs)) {
            Text(
                stringResource(R.string.action_cancel),
                style = ViraFont.body,
                color = ViraColor.accent(ViraArea.Today),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .defaultMinSize(minHeight = ViraTouch.minimum)
                    .viraPressable(onClick = onCancel)
                    .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.s),
            )
            Text(
                stringResource(R.string.capture_sheet_title),
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            ViriPrompt(Modifier.padding(horizontal = ViraSpacing.screenMargin))
            TitleField(title, { title = it }, symbolName, ::save, Modifier.padding(horizontal = ViraSpacing.screenMargin))
            IconRow(symbolName) { symbolName = it }

            Column(Modifier.padding(horizontal = ViraSpacing.screenMargin), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
                OptionGroup(stringResource(R.string.capture_when)) {
                    Chip(stringResource(R.string.capture_day_today), selectedDay == today && !isPickingDay) { selectedDay = today; isPickingDay = false }
                    Chip(stringResource(R.string.capture_day_tomorrow), selectedDay == tomorrow && !isPickingDay) { selectedDay = tomorrow; isPickingDay = false }
                    Chip(
                        if (isOtherDay) formatOtherDay(selectedDay, zone) else stringResource(R.string.capture_day_other),
                        isOtherDay || isPickingDay,
                        pictogram = R.drawable.picto_calendar,
                    ) { isPickingDay = !isPickingDay }
                }
                AnimatedVisibility(isPickingDay) {
                    val todayDate = today.atZone(zone).toLocalDate()
                    val pickerState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDay.atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(todayDate)
                        },
                    )
                    LaunchedEffect(pickerState.selectedDateMillis) {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date: LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            selectedDay = date.atStartOfDay(zone).toInstant()
                        }
                    }
                    DatePicker(
                        state = pickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        modifier = Modifier.background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l)),
                    )
                }

                OptionGroup(stringResource(R.string.capture_time)) {
                    Chip(stringResource(R.string.capture_time_none), timeChoice == QuickCaptureTime.None) { timeChoice = QuickCaptureTime.None }
                    QuickCapturePeriod.entries.forEach { period ->
                        val label = stringResource(
                            when (period) {
                                QuickCapturePeriod.Morning -> R.string.period_morning
                                QuickCapturePeriod.Midday -> R.string.period_midday
                                QuickCapturePeriod.Night -> R.string.period_night
                            },
                        )
                        Chip(label, timeChoice == QuickCaptureTime.Period(period)) { timeChoice = QuickCaptureTime.Period(period) }
                    }
                    Chip(stringResource(R.string.capture_time_exact), timeChoice == QuickCaptureTime.Exact, pictogram = R.drawable.picto_plan_alarm) {
                        timeChoice = QuickCaptureTime.Exact
                    }
                }
                AnimatedVisibility(timeChoice == QuickCaptureTime.Exact) {
                    Box(
                        Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l)).padding(ViraSpacing.m),
                        contentAlignment = Alignment.Center,
                    ) { TimeInput(timeState) }
                }

                RepeatRow(recurrence) { recurrence = it }
            }
        }

        // «Afegir» enganxat a baix, de manera que el camí ràpid cap sobre el teclat.
        Box(
            Modifier
                .fillMaxWidth()
                .background(ViraColor.surfaceShift(ViraArea.Today).copy(alpha = 0.94f))
                .navigationBarsPadding()
                .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
        ) {
            ViraActionButton(stringResource(R.string.action_add), onClick = ::save, enabled = canSave, modifier = Modifier.fillMaxWidth())
        }
    }
}

/// Viri a dalt del full, amb una bombolla que surt d'ell. Cap text sobre la il·lustració; a lletra
/// gran la bombolla baixa a sota.
@Composable
private fun ViriPrompt(modifier: Modifier) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val spoken = stringResource(R.string.today_viri_a11y, stringResource(R.string.capture_viri))
    val art = @Composable { artModifier: Modifier ->
        Image(
            painterResource(R.drawable.viri_suggest_capture_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = artModifier.clip(RoundedCornerShape(ViraRadius.l)).viraIdleBreath(),
        )
    }
    val bubble = @Composable { bubbleModifier: Modifier ->
        Text(
            stringResource(R.string.capture_viri),
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = bubbleModifier
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                .clearAndSetSemantics { contentDescription = spoken },
        )
    }
    if (stacked) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            art(Modifier.fillMaxWidth().height(92.dp))
            bubble(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            art(Modifier.size(92.dp))
            bubble(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TitleField(value: String, onValueChange: (String) -> Unit, symbolName: String, onDone: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    val inPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!inPreview) runCatching { focus.requestFocus() } }
    val shape = RoundedCornerShape(ViraRadius.l)
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val placeholder = stringResource(R.string.capture_title_placeholder)
    val field = @Composable { fieldModifier: Modifier ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = ViraFont.title.copy(color = ViraColor.textPrimary),
            cursorBrush = SolidColor(ViraColor.accent(ViraArea.Today)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = fieldModifier.focusRequester(focus).semantics { contentDescription = placeholder },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = ViraFont.title, color = ViraColor.textSecondary)
                    inner()
                }
            },
        )
    }
    val icon = @Composable {
        Box(
            Modifier.size(52.dp).background(ViraColor.surface(ViraArea.Today), RoundedCornerShape(ViraRadius.m)),
            contentAlignment = Alignment.Center,
        ) { ViraPictogram(symbolName, size = 38.dp) }
    }
    val container = modifier
        .fillMaxWidth()
        .background(ViraColor.surfaceElevated, shape)
        .border(2.dp, ViraColor.accent(ViraArea.Today), shape)
        .padding(ViraSpacing.s)
    if (stacked) {
        Column(container, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { icon(); field(Modifier.fillMaxWidth()) }
    } else {
        Row(container, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            icon(); field(Modifier.weight(1f))
        }
    }
}

/// Fila d'icones d'un toc, sota el camp. Llisquen, i la triada porta una marca a més del contorn.
@Composable
private fun IconRow(selection: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        ViraPictogramName.planIcons.forEach { name ->
            val isSelected = name == selection
            val label = ViraPictogramName.accessibilityLabel(name)?.let { stringResource(it) } ?: name
            val shape = RoundedCornerShape(ViraRadius.m)
            Box(
                Modifier
                    .size(52.dp)
                    .viraPressable(role = Role.Button) { onSelect(name) }
                    .semantics { contentDescription = label; selected = isSelected },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ViraColor.surfaceElevated, shape)
                        .then(if (isSelected) Modifier.border(2.5.dp, ViraColor.accent(ViraArea.Today), shape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { ViraPictogram(name, size = 34.dp) }
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ViraColor.accent(ViraArea.Today),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(ViraSpacing.xxs, -ViraSpacing.xxs)
                            .size(18.dp)
                            .background(ViraColor.surfaceElevated, CircleShape),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(title, style = ViraFont.headline, color = ViraColor.textSecondary, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            content()
        }
    }
}

/// Una píndola triable. La triada porta una marca, a més del color.
@Composable
private fun Chip(title: String, isSelected: Boolean, pictogram: Int? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(if (isSelected) ViraColor.accent(ViraArea.Today) else ViraColor.surfaceElevated, CircleShape)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = ViraColor.onAccent, modifier = Modifier.size(16.dp))
        } else if (pictogram != null) {
            ViraPictogram(pictogram, size = 22.dp, scalesWithText = true)
        }
        Text(
            title,
            style = if (isSelected) ViraFont.headline else ViraFont.callout,
            color = if (isSelected) ViraColor.onAccent else ViraColor.textPrimary,
        )
    }
}

@Composable
private fun RepeatRow(recurrence: PlanItemRecurrence, onSelect: (PlanItemRecurrence) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ViraTouch.comfortable)
                .viraPressable { open = true }
                .padding(horizontal = ViraSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPictogram(R.drawable.picto_plan_repeat, size = 30.dp, scalesWithText = true)
            Text(stringResource(R.string.plan_repeat), style = ViraFont.callout, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
            Text(stringResource(recurrence.titleRes), style = ViraFont.body, color = ViraColor.textSecondary, textAlign = TextAlign.End)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ViraColor.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PlanItemRecurrence.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.titleRes)) },
                    leadingIcon = if (option == recurrence) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

private val PlanItemRecurrence.titleRes: Int
    get() = when (this) {
        PlanItemRecurrence.NONE -> R.string.plan_repeat_none
        PlanItemRecurrence.WEEKLY -> R.string.plan_repeat_weekly
        PlanItemRecurrence.MONTHLY -> R.string.plan_repeat_monthly
        PlanItemRecurrence.DAILY_UNTIL_WEEK_END -> R.string.plan_repeat_daily_until_week_end
        PlanItemRecurrence.DAILY_UNTIL_MONTH_END -> R.string.plan_repeat_daily_until_month_end
    }

@Composable
private fun formatOtherDay(day: Instant, zone: ZoneId): String {
    val locale = viraLocale()
    return remember(day, locale) {
        DateTimeFormatter.ofPattern("EEE d MMM", locale).format(day.atZone(zone))
    }
}

