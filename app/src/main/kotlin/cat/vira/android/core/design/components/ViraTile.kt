package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Els dos aspectes d'una fitxa.
enum class ViraTileStyle {
    /// Degradat d'àrea amb text `onCard`: una porta a una pantalla.
    Card,
    /// Fons blanc: una dada que es mira i no ha de fer competència al mòdul protagonista.
    Plain,
}

/// Fitxa de graella (`ViraTile`, `DESIGN_RULES.md` §4): il·lustració a dalt, nom, una línia de
/// suport i, al peu, l'acció, el preu o l'estat. Omple l'alçada que li dona la fila, perquè totes
/// les d'una fila facin la mateixa alçada (`ViraTileGrid`).
@Composable
fun ViraTile(
    area: ViraArea,
    @DrawableRes pictogram: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: ViraTileStyle = ViraTileStyle.Card,
    foot: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = if (style == ViraTileStyle.Card) ViraColor.onCard(area) else ViraColor.textPrimary
    val supportInk = if (style == ViraTileStyle.Card) ViraColor.onCard(area) else ViraColor.textSecondary
    val background = when (style) {
        ViraTileStyle.Card -> Modifier.background(ViraGradient.card(area), shape)
        ViraTileStyle.Plain -> Modifier
            .shadow(3.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.05f), spotColor = ViraColor.shadow.copy(alpha = 0.05f))
            .background(ViraColor.surfaceElevated, shape)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(background)
            .then(
                if (LocalViraEnvironment.current.increasedContrast) Modifier.border(2.dp, ink, shape) else Modifier,
            )
            .padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
    ) {
        ViraPictogram(pictogram, size = 48.dp, scalesWithText = true, modifier = Modifier.padding(bottom = ViraSpacing.xxs))
        Text(title, style = ViraFont.headline, color = ink)
        if (subtitle != null) Text(subtitle, style = ViraFont.footnote, color = supportInk)
        if (foot != null) {
            Spacer(Modifier.height(ViraSpacing.xs))
            Spacer(Modifier.weight(1f))
            foot()
        }
    }
}

/// Píndola d'etiqueta (`ViraTag`, `DESIGN_RULES.md` §4): sempre amb text, mai només un color.
@Composable
fun ViraTag(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes pictogram: Int? = null,
) {
    Row(
        modifier = modifier
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (pictogram != null) ViraPictogram(pictogram, size = 18.dp, scalesWithText = true)
        Text(text, style = ViraFont.footnoteEmphasis, color = ViraColor.textPrimary)
    }
}

/// Graella de dues columnes amb les fitxes de cada fila igual d'altes. A lletra d'accessibilitat
/// passa a una sola columna: dues fitxes amb text gran no hi caben.
@Composable
fun <T> ViraTileGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            items.forEach { cell(it, Modifier.fillMaxWidth()) }
        } else {
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                ) {
                    row.forEach { cell(it, Modifier.weight(1f).fillMaxHeight()) }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}
