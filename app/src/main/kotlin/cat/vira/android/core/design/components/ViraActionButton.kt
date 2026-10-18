package cat.vira.android.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import androidx.compose.ui.text.TextStyle

/// Jerarquia d'una acció (`ViraActionHierarchy`).
enum class ViraActionHierarchy { Primary, Secondary, Textual }

/// El botó de contingut de Vira (`viraAction` de l'iPhone, `DESIGN_RULES.md` §4): el primari és
/// **lila sòlid** amb l'etiqueta `onAccent`, el secundari és l'accent tènue amb tinta de
/// superfície, i el textual és només text en el color de l'àrea. Mai `Button` de Material a pèl.
///
/// Alçada mínima de 56 dp (48 el textual), que la lletra gran pot fer créixer. Si cal amplada
/// sencera, es passa `Modifier.fillMaxWidth()`.
@Composable
fun ViraActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hierarchy: ViraActionHierarchy = ViraActionHierarchy.Primary,
    area: ViraArea = ViraArea.Today,
    enabled: Boolean = true,
    /// L'etiqueta va en `callout`; l'iPhone en passa una altra en pocs llocs (p. ex. `headline` al
    /// bloqueig de l'app, `ViraAppLockContainer`).
    textStyle: TextStyle = ViraFont.callout,
) {
    val shape = RoundedCornerShape(ViraRadius.s)
    val (fill, ink) = when {
        !enabled && hierarchy != ViraActionHierarchy.Textual ->
            ViraColor.separator to ViraColor.textSecondary
        !enabled -> Color.Transparent to ViraColor.textSecondary
        hierarchy == ViraActionHierarchy.Primary -> ViraColor.accent(area) to ViraColor.onAccent
        // `.bordered` amb el tint de l'àrea: l'accent al 15 %, com el farciment natiu d'iOS.
        hierarchy == ViraActionHierarchy.Secondary ->
            ViraColor.accent(area).copy(alpha = 0.15f) to ViraColor.onSurface(area)
        else -> Color.Transparent to ViraColor.accent(area)
    }
    val minHeight = if (hierarchy == ViraActionHierarchy.Textual) ViraTouch.minimum else ViraTouch.comfortable
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = minHeight)
            .clip(shape)
            .background(fill, shape)
            .viraPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
    ) {
        Text(text = text, style = textStyle, color = ink, textAlign = TextAlign.Center)
    }
}
