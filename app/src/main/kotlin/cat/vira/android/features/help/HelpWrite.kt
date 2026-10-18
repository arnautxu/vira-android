package cat.vira.android.features.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Espai d'escriptura temporal (`HelpWriteView`): no es desa ni s'analitza, i es perd en sortir.
/// L'usuari decideix si vol anar al registre emocional; el text **no hi passa**.
///
/// Viri escrivint a dalt amb una sola frase, el full blanc gran a sota i dos botons: esborrar el que
/// hi ha (apagat si és buit) o anar a «Com estàs?».
@Composable
fun HelpWriteContent(onBack: () -> Unit, onCheckIn: () -> Unit, initialText: String = "") {
    var text by remember { mutableStateOf(initialText) }
    val isEmpty = text.isBlank()
    val label = stringResource(R.string.help_write_title)

    HelpPage(label, onBack = onBack) {
        HelpViriScene(stringResource(R.string.help_write_viri), R.drawable.viri_entry_writing, imageWidth = 150.dp)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
            cursorBrush = SolidColor(ViraColor.accent(ViraArea.Today)),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                        .padding(ViraSpacing.s),
                ) { field() }
            },
        )
        val clear = @Composable { modifier: Modifier ->
            ViraActionButton(
                stringResource(R.string.help_write_clear),
                onClick = { text = "" },
                modifier = modifier,
                hierarchy = ViraActionHierarchy.Secondary,
                enabled = !isEmpty,
            )
        }
        val checkIn = @Composable { modifier: Modifier ->
            ViraActionButton(stringResource(R.string.help_write_check_in), onCheckIn, modifier)
        }
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                clear(Modifier.fillMaxWidth())
                checkIn(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                clear(Modifier.weight(1f))
                checkIn(Modifier.weight(1f))
            }
        }
        HelpFootnote(stringResource(R.string.help_write_footer))
    }
}
