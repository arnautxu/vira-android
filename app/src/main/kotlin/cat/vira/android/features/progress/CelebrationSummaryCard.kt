package cat.vira.android.features.progress

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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

/// Una píndola de la targeta de resum. Plena per al context («Nivell nou»), de contorn per al detall.
data class CelebrationPill(val text: String, val isFilled: Boolean = false)

/// La targeta de resum sota la bombolla de `ViraCelebrationMoment` (`CelebrationSummaryCard`):
/// píndoles, una fila amb el pictograma i el que s'ha guanyat i, si cal, el progrés amb text.
/// Degradat d'àrea amb `onCard`, cap text blanc, progrés mai només amb la barra.
///
/// La fan servir el nivell i els reptes d'aquí, i el premi i la carta de Botiga i premis.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CelebrationSummaryCard(
    area: ViraArea,
    modifier: Modifier = Modifier,
    pills: List<CelebrationPill> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = ViraColor.onCard(area)
    val fill = ViraColor.cardFill(area)
    Column(
        modifier
            .fillMaxWidth()
            .background(ViraGradient.card(area), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (pills.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                pills.forEach { pill ->
                    Text(
                        pill.text,
                        style = ViraFont.footnoteEmphasis,
                        // Plena: la parella validada `onCard`/`cardFill`, invertida.
                        color = if (pill.isFilled) fill else ink,
                        modifier = Modifier
                            .then(if (pill.isFilled) Modifier.background(ink, CircleShape) else Modifier.border(1.5.dp, ink, CircleShape))
                            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                    )
                }
            }
        }
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(color = ink)) { content() }
    }
}

/// Pictograma a l'esquerra, el que s'ha guanyat en gran i una línia que ho explica. Amb lletra
/// d'accessibilitat el pictograma puja a sobre perquè el text tingui tot l'ample.
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
            Text(headline, style = if (isNumber) ViraFont.largeTitle else ViraFont.title, color = ink)
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

/// Barra de progrés amb el text a sota: «40 / 800 XP · fins al 8», «3 de 3».
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CelebrationProgressBar(fraction: Float, leading: String, area: ViraArea, trailing: String? = null) {
    val ink = ViraColor.onCard(area)
    val track = 8.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Column(Modifier.padding(top = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        Box(Modifier.fillMaxWidth().height(track).background(ink.copy(alpha = 0.18f), CircleShape)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(ink, CircleShape))
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leading, style = ViraFont.footnoteEmphasis, color = ink)
            if (trailing != null) Text(trailing, style = ViraFont.footnoteEmphasis, color = ink)
        }
    }
}
