package cat.vira.android.features.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.viraLocale
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/// Control segmentat de dues o tres opcions (`.pickerStyle(.segmented)` de l'iPhone): Dia i
/// Setmana, Cada dia i Dies concrets.
@Composable
fun <T> PlanSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.s)
    Row(
        modifier.fillMaxWidth().background(ViraColor.textPrimary.copy(alpha = 0.08f), shape).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                Modifier.weight(1f).defaultMinSize(minHeight = ViraTouch.minimum)
                    .background(if (isSelected) ViraColor.surfaceElevated else Color.Transparent, RoundedCornerShape(ViraRadius.s - 2.dp))
                    .viraPressable(role = Role.Tab) { onSelect(value) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = if (isSelected) ViraFont.headline else ViraFont.body,
                    color = ViraColor.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/// **Cada dia** o **dies concrets** (`WeekdayPicker`). Files i no set cercles en línia: set
/// controls de 48 dp no caben a mida de lletra gran.
@Composable
fun WeekdayFields(weekdays: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val locale = viraLocale()
    val ordered = WeekdaySelection.ordered(PlanDay.firstDayOfWeek(locale))
    PlanSegmented(
        options = listOf(true to stringResource(R.string.habits_when_everyday), false to stringResource(R.string.habits_when_somedays)),
        selected = weekdays.isEmpty(),
        onSelect = { everyday -> onChange(if (everyday) emptySet() else ordered.toSet()) },
        modifier = Modifier.padding(vertical = ViraSpacing.xs),
    )
    if (weekdays.isNotEmpty()) {
        ordered.forEach { day ->
            val name = WeekdaySelection.dayOfWeek(day).getDisplayName(TextStyle.FULL_STANDALONE, locale)
                .replaceFirstChar { it.titlecase(locale) }
            FormToggle(
                label = name,
                checked = day in weekdays,
                enabled = weekdays != setOf(day),
                onCheckedChange = { onChange(WeekdaySelection.toggling(day, it, weekdays)) },
            )
        }
        Text(
            stringResource(R.string.habits_when_at_least_one),
            style = ViraFont.footnote,
            color = ViraColor.textSecondary,
            modifier = Modifier.padding(vertical = ViraSpacing.xs),
        )
    }
}

/// Hores i dates amb el format de l'idioma de la pantalla.
object PlanFormat {
    @Composable
    @ReadOnlyComposable
    fun time(time: LocalTime, locale: Locale = viraLocale()): String =
        time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

    @Composable
    @ReadOnlyComposable
    fun date(date: LocalDate, locale: Locale = viraLocale()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    @Composable
    @ReadOnlyComposable
    fun minutes(minutes: Int, locale: Locale = viraLocale()): String = minutesIn(minutes, locale)

    /// La mateixa hora fora d'un `@Composable` (dins d'un `joinToString`, per exemple).
    fun minutesIn(minutes: Int, locale: Locale): String =
        LocalTime.of(minutes / 60 % 24, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}

/// Fila amb una hora que obre el rellotge del sistema.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTimeRow(label: String, time: LocalTime, onChange: (LocalTime) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    FormValueRow(label, PlanFormat.time(time)) { isOpen = true }
    if (isOpen) {
        val context = LocalContext.current
        val state = rememberTimePickerState(time.hour, time.minute, android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { isOpen = false },
            confirmButton = {
                TextButton(onClick = { onChange(LocalTime.of(state.hour, state.minute)); isOpen = false }) {
                    Text(stringResource(R.string.action_done), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Plan))
                }
            },
            dismissButton = {
                TextButton(onClick = { isOpen = false }) {
                    Text(stringResource(R.string.action_cancel), style = ViraFont.body, color = ViraColor.accent(ViraArea.Plan))
                }
            },
            text = { TimePicker(state = state) },
        )
    }
}

/// Fila amb un dia que obre el calendari del sistema.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDateRow(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    FormValueRow(label, PlanFormat.date(date)) { isOpen = true }
    if (isOpen) {
        // El selector treballa en mil·lisegons UTC: el dia s'hi posa i se'n treu en UTC.
        val state = rememberDatePickerState(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { isOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    isOpen = false
                }) { Text(stringResource(R.string.action_done), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Plan)) }
            },
            dismissButton = {
                TextButton(onClick = { isOpen = false }) {
                    Text(stringResource(R.string.action_cancel), style = ViraFont.body, color = ViraColor.accent(ViraArea.Plan))
                }
            },
        ) { DatePicker(state = state) }
    }
}

/// Selector d'una opció amb menú desplegable (`Picker` en estil menú de l'iPhone).
@Composable
fun <T> FormMenuRow(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        FormValueRow(label, options.firstOrNull { it.first == selected }?.second.orEmpty()) { isOpen = true }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title, style = if (value == selected) ViraFont.headline else ViraFont.body) },
                    onClick = { onSelect(value); isOpen = false },
                )
            }
        }
    }
}
