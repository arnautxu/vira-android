package cat.vira.android.features.parent

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Les dues coses que un adult pot enviar (`ParentComposeView.Mode` de l'iPhone).
enum class ParentComposeMode { MESSAGE, REMINDER }

/// La cara **Envia** del detall (`ParentChildSendContent` de l'iPhone): Viri escrivint i les dues
/// coses que un adult pot enviar.
///
/// Cada targeta diu què passa després d'enviar-la, perquè és el que les fa diferents: el missatge
/// arriba tal qual; el recordatori espera que el fill o la filla el vulgui. No és un xat i les
/// respostes no tornen (`PRIVACY_AND_SAFETY.md` §4.1).
@Composable
internal fun ParentChildSendContent(onCompose: (ParentComposeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap)) {
        ParentViriCard(stringResource(R.string.parent_viri_send), R.drawable.viri_entry_writing)
        ParentSectionHeader(stringResource(R.string.parent_send_section), Modifier.padding(top = ViraSpacing.s))
        Option(R.drawable.picto_heart, R.string.parent_compose_message, R.string.parent_compose_message_footnote, R.string.parent_send_write) {
            onCompose(ParentComposeMode.MESSAGE)
        }
        Option(R.drawable.picto_plan_reminder, R.string.parent_compose_reminder, R.string.parent_compose_reminder_footnote, R.string.parent_send_suggest) {
            onCompose(ParentComposeMode.REMINDER)
        }
        ParentFootnote(stringResource(R.string.parent_compose_footnote))
    }
}

@Composable
private fun Option(
    @DrawableRes pictogram: Int,
    @StringRes title: Int,
    @StringRes detail: Int,
    @StringRes action: Int,
    perform: () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val titleText = stringResource(title)
    val detailText = stringResource(detail)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.05f), spotColor = ViraColor.shadow.copy(alpha = 0.05f))
            .background(ViraColor.surfaceElevated, shape)
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Row(
            Modifier.clearAndSetSemantics { contentDescription = "$titleText. $detailText" },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPictogram(pictogram, size = 44.dp, scalesWithText = true)
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(titleText, style = ViraFont.headline, color = ViraColor.textPrimary)
                Text(detailText, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        ViraActionButton(stringResource(action), perform, Modifier.fillMaxWidth())
    }
}
