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
/// línia que explica què passa en encendre'l. Tot dins de la vora clara (`entryFieldBorder`).
///
/// Tota la fila és l'interruptor: és un sol element per a TalkBack i un sol toc.
@Composable
fun EntryToggleRow(title: String, isOn: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .entryFieldBorder()
            .toggleable(value = isOn, role = Role.Switch, onValueChange = onChange)
            .padding(ViraSpacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(title, style = ViraFont.body, color = ink)
            if (hint != null) Text(hint, style = ViraFont.footnote, color = ink.copy(alpha = 0.8f))
        }
        Switch(
            checked = isOn,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ViraColor.accent(ViraArea.Today),
                checkedThumbColor = ViraColor.onAccent,
                checkedBorderColor = ViraColor.accent(ViraArea.Today),
                uncheckedBorderColor = ViraColor.controlBorder(on = ink),
                uncheckedThumbColor = ViraColor.controlBorder(on = ink),
                uncheckedTrackColor = ViraColor.surfaceElevated,
            ),
        )
    }
}

/// El peu dels passos d'interruptors: res del que es tria aquí és definitiu.
@Composable
fun EntryTogglesFootnote() {
    Text(stringResource(R.string.onboarding_modules_footnote), style = ViraFont.footnote, color = ViraColor.onCard(ViraArea.Rewards))
}
