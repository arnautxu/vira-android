package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Una píndola de la targeta de resum: plena per al context, de contorn per al detall.
data class CelebrationPill(val text: String, val symbol: ShopSymbol? = null, val isFilled: Boolean = false)

/// La targeta de resum sota la bombolla de `ViraCelebrationMoment` (`CelebrationSummaryCard`):
/// píndoles, una fila amb el pictograma i el que s'ha guanyat i, si cal, el progrés amb text.
/// Degradat d'àrea amb `onCard`, cap text blanc.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CelebrationSummaryCard(
    area: ViraArea,
    modifier: Modifier = Modifier,
    pills: List<CelebrationPill> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = ViraColor.onCard(area)
    Column(
        modifier.fillMaxWidth()
            .background(ViraGradient.card(area), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (pills.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                pills.forEach { pill ->
                    // Plena: la parella validada `onCard`/`cardFill`, invertida.
                    val fg = if (pill.isFilled) ViraColor.cardFill(area) else ink
                    Row(
                        Modifier.then(
                            if (pill.isFilled) Modifier.background(ink, CircleShape) else Modifier.border(1.5.dp, ink, CircleShape),
                        ).padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                    ) {
                        pill.symbol?.let { ShopSymbolIcon(it, fg, size = 12.dp) }
                        Text(pill.text, style = ViraFont.footnoteEmphasis, color = fg)
                    }
                }
            }
        }
        content()
    }
}

/// Pictograma a l'esquerra, el que s'ha guanyat en gran i una línia que ho explica
/// (`CelebrationSummaryRow`). A mides d'accessibilitat el pictograma puja a sobre.
@Composable
fun CelebrationSummaryRow(
    @DrawableRes pictogram: Int,
    headline: String,
    area: ViraArea,
    detail: String? = null,
    isNumber: Boolean = true,
) {
    val ink = ViraColor.onCard(area)
    val texts: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
            Text(headline, style = if (isNumber) ViraFont.largeTitle.tabular() else ViraFont.title, color = ink)
            if (detail != null) Text(detail, style = ViraFont.callout, color = ink)
        }
    }
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraPictogram(pictogram, size = 56.dp)
            texts()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ViraPictogram(pictogram, size = 56.dp)
            texts()
        }
    }
}

/// Barra de progrés amb el text a sota (`CelebrationProgressBar`): «3 de 16».
@Composable
fun CelebrationProgressBar(fraction: Float, leading: String, area: ViraArea, trailing: String? = null) {
    val ink = ViraColor.onCard(area)
    val track = 8.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Column(Modifier.padding(top = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        Box(Modifier.fillMaxWidth().height(track).clip(CircleShape).background(ink.copy(alpha = 0.18f)).clearAndSetSemantics { }) {
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) Box(Modifier.fillMaxWidth(f).height(track).clip(CircleShape).background(ink))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(leading, style = ViraFont.footnoteEmphasis.tabular(), color = ink, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = ViraFont.footnoteEmphasis, color = ink)
        }
    }
}
