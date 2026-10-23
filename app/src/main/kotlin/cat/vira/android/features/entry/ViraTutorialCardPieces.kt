package cat.vira.android.features.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.app.ViraTab
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Les quatre pestanyes en miniatura, a la pregunta del principi: on és cada cosa, abans d'anar-hi
/// (`ViraTutorialTabMap`). Amb lletra gran, una llista: en fitxes, no hi cabien i es tallaven.
@Composable
fun ViraTutorialTabMap() {
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraTab.entries.forEach { tab ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                ) {
                    ViraTutorialTabIcon(tab)
                    Text(stringResource(tab.title), style = ViraFont.body, color = ViraColor.textPrimary)
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraTab.entries.forEach { tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterVertically),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ViraTouch.minimum)
                        .background(ViraColor.backgroundGrouped, RoundedCornerShape(ViraRadius.s))
                        .padding(vertical = ViraSpacing.xs)
                        .semantics(mergeDescendants = true) { },
                ) {
                    ViraTutorialTabIcon(tab)
                    Text(
                        stringResource(tab.title),
                        style = ViraFont.caption,
                        color = ViraColor.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = ViraSpacing.xxs),
                    )
                }
            }
        }
    }
}

@Composable
private fun ViraTutorialTabIcon(tab: ViraTab) {
    Image(
        painterResource(tab.icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(26.dp).clearAndSetSemantics { },
    )
}

/// Treu la peça del marge de la targeta, de vora a vora: l'escena de la pregunta va a sang.
fun Modifier.layoutBleed(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth
    val placeable = measurable.measure(Constraints.fixedWidth(width).copy(minHeight = constraints.minHeight, maxHeight = constraints.maxHeight))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed.roundToPx(), 0) }
}
