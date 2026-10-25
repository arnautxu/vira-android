package cat.vira.android.features.assist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Full a tota l'alçada per a les ajudes de Viri (`.sheet` amb `.presentationDetents([.large])`).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViriAssistSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.backgroundGrouped,
        modifier = Modifier.statusBarsPadding(),
        content = content,
    )
}

/// «Viri pensa amb Claude» (`ViriAssistConsentSheet`): la primera vegada que es toca una ajuda,
/// abans d'enviar res. Diu qui rep el text, què surt i què no. «Ara no» no envia res i no castiga.
/// Es pot desfer a Ajustos › Mòduls, que apagant les ajudes oblida aquest «D'acord».
@Composable
fun ViriAssistConsentSheet(onAccept: () -> Unit, onDismiss: () -> Unit) {
    ViriAssistSheet(onDismiss) { ViriAssistConsentContent(onAccept, onDismiss) }
}

@Composable
fun ViriAssistConsentContent(onAccept: () -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            Image(
                painterResource(R.drawable.viri_entry_bodyguard),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
            )
            Text(
                stringResource(R.string.viri_assist_consent_title),
                style = ViraFont.title,
                color = ViraColor.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.viri_assist_consent_body), style = ViraFont.body, color = ViraColor.textSecondary)
            Column(
                Modifier.fillMaxWidth()
                    .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                    .padding(ViraSpacing.m),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                ConsentLine(Icons.Filled.CheckCircle, stringResource(R.string.viri_assist_consent_only))
                ConsentLine(Icons.Filled.CheckCircle, stringResource(R.string.viri_assist_consent_training))
                ConsentLine(Icons.Filled.Clear, stringResource(R.string.viri_assist_consent_never))
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .background(ViraColor.surfaceShift(ViraArea.Today).copy(alpha = 0.94f))
                .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            ViraActionButton(stringResource(R.string.viri_assist_consent_accept), onAccept, Modifier.fillMaxWidth())
            ViraActionButton(
                stringResource(R.string.viri_assist_consent_later), onLater, Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual,
            )
        }
    }
}

/// La icona diu el mateix que el text: mai només el color.
@Composable
private fun ConsentLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Icon(icon, contentDescription = null, tint = ViraColor.brandAccent, modifier = Modifier.size(24.dp))
        Text(text, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
    }
}
