package cat.vira.android.features.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Els editors de Pla (tasca, hàbit, pauta) són fulls a pantalla completa amb **Cancel·la** a
/// l'esquerra i l'acció de desar a la dreta, com els `Form` dins d'un `NavigationStack` de
/// l'iPhone. El cos és una pila de seccions blanques sobre el fons agrupat.
@Composable
fun PlanSheet(
    title: String,
    cancelLabel: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onCancel)
        Column(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped).statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarButton(cancelLabel, enabled = true, emphasized = false, onClick = onCancel)
                Text(
                    title,
                    style = ViraFont.headline,
                    color = ViraColor.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                BarButton(confirmLabel, enabled = confirmEnabled, emphasized = true, onClick = onConfirm)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.s),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
                content = content,
            )
        }
    }
}

@Composable
private fun BarButton(label: String, enabled: Boolean, emphasized: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
            .viraPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ViraSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (emphasized) ViraFont.headline else ViraFont.body,
            color = if (enabled) ViraColor.accent(ViraArea.Plan) else ViraColor.textSecondary,
        )
    }
}

/// Secció d'un formulari: capçalera petita, files en una targeta blanca i un peu opcional.
@Composable
fun FormSection(
    header: String? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
                modifier = Modifier.padding(horizontal = ViraSpacing.m).semantics { heading() },
            )
        }
        Column(
            Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs),
            content = content,
        )
        if (footer != null) {
            Box(Modifier.padding(horizontal = ViraSpacing.m)) { footer() }
        }
    }
}

/// Text del peu d'una secció.
@Composable
fun FormFooter(text: String) {
    Text(text, style = ViraFont.footnote, color = ViraColor.textSecondary)
}

@Composable
fun FormDivider() = HorizontalDivider(color = ViraColor.separator)

/// Fila tocable amb etiqueta a l'esquerra i valor a la dreta (un selector, una data).
@Composable
fun FormValueRow(label: String, value: String, labelColor: Color = ViraColor.textPrimary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum).viraPressable(onClick = onClick)
            .padding(vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Text(label, style = ViraFont.body, color = labelColor, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = ViraFont.body, color = ViraColor.accent(ViraArea.Plan))
    }
}

/// Fila d'acció (afegir una hora, concloure): només text, en el color que toca.
@Composable
fun FormActionRow(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(enabled = enabled, onClick = onClick).padding(vertical = ViraSpacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = ViraFont.body, color = if (enabled) color else ViraColor.textSecondary)
    }
}

/// Interruptor amb etiqueta: tota la fila el commuta, i TalkBack el llegeix com un de sol.
@Composable
fun FormToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(role = androidx.compose.ui.semantics.Role.Switch, enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Text(
            label,
            style = ViraFont.body,
            color = if (enabled) ViraColor.textPrimary else ViraColor.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ViraColor.accent(ViraArea.Plan),
                checkedThumbColor = ViraColor.onAccent,
            ),
        )
    }
}

/// Camp de text sense vora, amb l'etiqueta **a sobre** i persistent (no un placeholder que
/// desapareix en escriure): és el que fa l'iPhone als editors d'hàbits i medicació.
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String = "",
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        if (label != null) Text(label, style = ViraFont.callout, color = ViraColor.textPrimary)
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, style = ViraFont.body, color = ViraColor.textSecondary) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = ViraColor.accent(ViraArea.Plan),
                unfocusedIndicatorColor = ViraColor.separator,
                cursorColor = ViraColor.accent(ViraArea.Plan),
            ),
            modifier = modifier.fillMaxWidth(),
        )
    }
}
