package cat.vira.android.features.emotions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing

/// Full a tota l'alçada (`.sheet` amb `.presentationDetents([.large])` de l'iPhone). Material
/// només per sota: el que es veu és el contingut de Vira.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmotionSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.backgroundGrouped,
        modifier = Modifier.statusBarsPadding(),
        content = content,
    )
}

/// La barra de dalt d'un full (`navigationTitle` en línia amb les accions de la barra): cancel·lar
/// a l'esquerra, el títol al mig i, si n'hi ha, confirmar a la dreta.
@Composable
internal fun EmotionSheetBar(
    title: String,
    leading: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
    trailing: Pair<String, () -> Unit>? = null,
    trailingEnabled: Boolean = true,
) {
    Box(modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs)) {
        ViraActionButton(
            leading.first, leading.second,
            modifier = Modifier.align(Alignment.CenterStart),
            hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Emotions,
        )
        Text(
            title,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.5f).semantics { heading() },
        )
        if (trailing != null) {
            ViraActionButton(
                trailing.first, trailing.second,
                modifier = Modifier.align(Alignment.CenterEnd),
                hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Emotions, enabled = trailingEnabled,
            )
        }
    }
}

/// Contenidor d'un full per a les captures, que no poden fotografiar una finestra emergent.
@Composable
internal fun EmotionSheetPreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped).padding(top = ViraSpacing.m), content = content)
}
