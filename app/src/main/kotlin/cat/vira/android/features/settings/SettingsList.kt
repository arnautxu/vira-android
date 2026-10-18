package cat.vira.android.features.settings

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/// L'àrea de la família oberta: els interruptors i les accions agafen el seu color, com el `.tint`
/// de la llista de l'iPhone.
val LocalSettingsArea = compositionLocalOf { ViraArea.Today }

/// Les files d'una secció. No són composables: es decideix quines hi ha i el grup posa els
/// separadors entre elles, com la `Section` d'una llista agrupada de l'iPhone.
class SettingsGroupScope {
    internal val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { rows += content }
}

/// Una secció de la llista agrupada (`List(.insetGrouped)` de l'iPhone): capçalera en negreta,
/// targeta blanca amb les files i, a sota, el peu en text secundari.
@Composable
fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    build: SettingsGroupScope.() -> Unit,
) {
    val rows = SettingsGroupScope().apply(build).rows
    if (rows.isEmpty() && header == null) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        if (header != null) {
            Text(
                header,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.padding(horizontal = ViraSpacing.m).semantics { heading() },
            )
        }
        if (rows.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.s))) {
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.separator)
                    row()
                }
            }
        }
        if (footer != null) {
            Text(footer, style = ViraFont.footnote, color = ViraColor.textSecondary, modifier = Modifier.padding(horizontal = ViraSpacing.m))
        }
    }
}

/// Etiqueta d'una fila amb el seu pictograma (`SettingsRowLabel`): el dibuix diu de què parla la
/// fila abans de llegir-la. El text de suport explica el que no és evident en una línia.
@Composable
fun SettingsLabel(title: String, modifier: Modifier = Modifier, @DrawableRes pictogram: Int? = null, hint: String? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        if (pictogram != null) ViraPictogram(pictogram, size = 32.dp, scalesWithText = true)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(title, style = ViraFont.body, color = ViraColor.textPrimary)
            if (hint != null) Text(hint, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
    }
}

private val rowModifier = Modifier
    .fillMaxWidth()
    .defaultMinSize(minHeight = ViraTouch.minimum)

/// Interruptor: tota la fila és el control, i TalkBack el llegeix com un de sol.
@Composable
fun SettingsToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    spokenLabel: String? = null,
    label: @Composable () -> Unit,
) {
    val area = LocalSettingsArea.current
    Row(
        rowModifier
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .then(if (spokenLabel != null) Modifier.semantics { contentDescription = spokenLabel } else Modifier)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { label() }
        Spacer(Modifier.width(ViraSpacing.s))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ViraColor.accent(area),
                checkedThumbColor = ViraColor.onAccent,
                checkedBorderColor = ViraColor.accent(area),
            ),
        )
    }
}

@Composable
fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, hint: String? = null, @DrawableRes pictogram: Int? = null, enabled: Boolean = true) =
    SettingsToggleRow(checked, onCheckedChange, enabled) { SettingsLabel(title, pictogram = pictogram, hint = hint) }

/// Fila que porta a una altra pantalla.
@Composable
fun SettingsNavigationRow(onClick: () -> Unit, label: @Composable () -> Unit) {
    Row(
        rowModifier.viraPressable(onClick = onClick).padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { label() }
        SettingsGlyph(SettingsGlyphKind.Chevron, ViraColor.textSecondary, extent = 14.dp)
    }
}

/// Etiqueta i valor, sense acció (`LabeledContent`).
@Composable
fun SettingsValueRow(value: String, label: @Composable () -> Unit) {
    val modifier = rowModifier.semantics(mergeDescendants = true) {}.padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs)
    // A lletra gran el valor baixa a sota: al costat, l'etiqueta quedava en una lletra per línia.
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            label()
            Text(value, style = ViraFont.body, color = ViraColor.textSecondary)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { label() }
            Spacer(Modifier.width(ViraSpacing.s))
            Text(value, style = ViraFont.body, color = ViraColor.textSecondary)
        }
    }
}

/// Botó dins de la llista: en el color de la família, o en vermell si destrueix. El vermell no va
/// mai sol: el text ja diu què fa.
@Composable
fun SettingsActionRow(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true,
    glyph: SettingsGlyphKind? = null,
) {
    val color = when {
        !enabled -> ViraColor.textSecondary
        destructive -> ViraColor.error
        else -> ViraColor.accent(LocalSettingsArea.current)
    }
    Row(
        rowModifier.viraPressable(enabled = enabled, onClick = onClick).padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (glyph != null) SettingsGlyph(glyph, color)
        Text(text, style = ViraFont.headline, color = color)
    }
}

/// Text de suport dins de la llista.
@Composable
fun SettingsNoteRow(text: String, color: Color = ViraColor.textSecondary) {
    Text(
        text,
        style = ViraFont.footnote,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
    )
}

/// Avís de connexió trencada: la icona i el text diuen el mateix (mai només el color), i l'enllaç
/// porta als ajustos del telèfon.
@Composable
fun SettingsBlockedRow(message: String, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            SettingsGlyph(SettingsGlyphKind.Warning, ViraColor.textSecondary, Modifier.padding(top = 1.dp), extent = 15.dp)
            Text(message, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        Text(
            stringResource(R.string.settings_connection_open),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.accent(LocalSettingsArea.current),
            modifier = Modifier.defaultMinSize(minHeight = ViraTouch.minimum).viraPressable(role = Role.Button, onClick = onOpenSettings)
                .padding(vertical = ViraSpacing.s),
        )
    }
}

/// Hora triada: es desa com a hora i minut, no com un instant (el que tria l'usuari és «les 20:30»).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTimeRow(title: String, hour: Int, minute: Int, onChange: (hour: Int, minute: Int) -> Unit) {
    var isPicking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    val text = time.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a"))
    Row(
        rowModifier.viraPressable(onClick = { isPicking = true }).padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
        Text(
            text,
            style = ViraFont.body,
            color = ViraColor.textPrimary,
            modifier = Modifier
                .background(ViraColor.backgroundGrouped, RoundedCornerShape(ViraRadius.s))
                .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
        )
    }
    if (isPicking) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = is24)
        AlertDialog(
            onDismissRequest = { isPicking = false },
            confirmButton = {
                TextButton(onClick = { onChange(state.hour, state.minute); isPicking = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = { TextButton(onClick = { isPicking = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

/// Una opció d'entre poques (`Picker` en menú): la triada a la dreta i, en tocar-la, la llista.
@Composable
fun <T> SettingsChoiceRow(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    label: @Composable () -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    Row(
        rowModifier.viraPressable(onClick = { isOpen = true }).padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { label() }
        Spacer(Modifier.width(ViraSpacing.s))
        Box {
            Text(optionLabel(selected), style = ViraFont.body, color = ViraColor.accent(LocalSettingsArea.current))
            DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), style = ViraFont.body) },
                        onClick = { isOpen = false; if (option != selected) onSelect(option) },
                    )
                }
            }
        }
    }
}
