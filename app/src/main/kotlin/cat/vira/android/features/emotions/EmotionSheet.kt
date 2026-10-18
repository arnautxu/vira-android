package cat.vira.android.features.emotions

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.background
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
    // Com la barra de navegació d'iOS: el títol es queda centrat i, quan els botons creixen amb
    // la lletra, s'escurça amb punts suspensius en lloc de passar-los per sobre.
    Layout(
        content = {
            ViraActionButton(
                leading.first, leading.second,
                hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Emotions,
            )
            Text(
                title,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (trailing != null) {
                ViraActionButton(
                    trailing.first, trailing.second,
                    hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Emotions, enabled = trailingEnabled,
                )
            }
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val buttons = Constraints(maxWidth = (width * 0.4f).toInt())
        val lead = measurables[0].measure(buttons)
        val trail = measurables.getOrNull(2)?.measure(buttons)
        val side = maxOf(lead.width, trail?.width ?: 0)
        val titleWidth = (width - 2 * side).coerceAtLeast(0)
        val label = measurables[1].measure(Constraints(minWidth = titleWidth, maxWidth = titleWidth))
        val height = maxOf(lead.height, trail?.height ?: 0, label.height)
        layout(width, height) {
            lead.placeRelative(0, (height - lead.height) / 2)
            label.placeRelative(side, (height - label.height) / 2)
            trail?.placeRelative(width - trail.width, (height - trail.height) / 2)
        }
    }
}

/// Contenidor d'un full per a les captures, que no poden fotografiar una finestra emergent.
@Composable
internal fun EmotionSheetPreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped).padding(top = ViraSpacing.m), content = content)
}
