package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Una fila d'interruptor dins de la fulla (`EntryToggleRow.swift`): un títol i, de vegades, una
/// línia que explica què passa en encendre'l. Des del 2026-10-21 és una fila agrupada
/// (`entryGroupedRow`), com les llistes d'Ajustos: la fulla és blanca i l'interruptor ja diu on és el control.
///
/// Tota la fila és l'interruptor: és un sol element per a TalkBack i un sol toc.
@Composable
fun EntryToggleRow(title: String, isOn: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .entryGroupedRow()
            .toggleable(value = isOn, role = Role.Switch, onValueChange = onChange)
            .padding(ViraSpacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(title, style = ViraFont.body, color = ViraColor.textPrimary)
            if (hint != null) Text(hint, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        Switch(
            checked = isOn,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ViraColor.accent(ViraArea.Today),
                checkedThumbColor = ViraColor.onAccent,
                checkedBorderColor = ViraColor.accent(ViraArea.Today),
                uncheckedBorderColor = ViraColor.controlBorder,
                uncheckedThumbColor = ViraColor.controlBorder,
                uncheckedTrackColor = ViraColor.surfaceElevated,
            ),
        )
    }
}

/// El peu dels passos d'interruptors: res del que es tria aquí és definitiu.
@Composable
fun EntryTogglesFootnote() {
    Text(stringResource(R.string.onboarding_modules_footnote), style = ViraFont.footnote, color = ViraColor.textSecondary)
}
