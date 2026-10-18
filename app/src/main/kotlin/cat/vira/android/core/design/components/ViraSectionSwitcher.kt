package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec

/// Una cara del selector: el nom i el pictograma (`ViraSectionItem`).
data class ViraSectionItem(
    val key: String,
    val label: String,
    @DrawableRes val pictogram: Int,
)

/// Selector de seccions dins d'una pestanya (`ViraSectionSwitcher`, `DESIGN_RULES.md` §2): una
/// pista blanca tenyida de l'àrea i la cara triada, blanca i aixecada amb ombra.
///
/// Tres formes, i es tria la primera que hi cap sense tallar cap nom:
/// 1. Amb `showsAllLabels`: **icona i nom a cada cara**, cares d'ample idèntic a tot l'ample.
/// 2. Icones, i només l'activa porta nom; l'aire es reparteix entre les cares.
/// 3. La mateixa fila, desplaçable en horitzontal.
@Composable
fun ViraSectionSwitcher(
    area: ViraArea,
    items: List<ViraSectionItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    showsAllLabels: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val environment = LocalViraEnvironment.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxWidth
        val gap = ViraSpacing.xxs
        val track = gap * 2

        fun textWidth(text: String, style: androidx.compose.ui.text.TextStyle): Dp =
            with(density) { measurer.measure(text, style).size.width.toDp() }

        // 1. Cada cara és tan ampla com la més llarga: icona de 24 sobre el nom, 12 a cada costat.
        val labeledCell = items.maxOf { maxOf(24.dp, textWidth(it.label, ViraFont.footnoteEmphasis)) } + ViraSpacing.s * 2
        val labeledFits = showsAllLabels &&
            labeledCell * items.size + gap * (items.size - 1) + track <= available
        // 2. Icones de 28 i el nom només a l'activa.
        val compactWidth = items.sumOf { item ->
            val labelPart = if (item.key == selectedKey) {
                ViraSpacing.xs + textWidth(item.label, ViraFont.headline) + ViraSpacing.s * 2
            } else {
                ViraSpacing.xs * 2
            }
            (28.dp + labelPart).value.toDouble()
        }.dp + gap * (items.size - 1) + track
        val compactFits = compactWidth <= available

        val shape: Shape = if (labeledFits) RoundedCornerShape(ViraRadius.m + gap) else CircleShape
        val pistModifier = Modifier
            .background(ViraColor.surfaceElevated, shape)
            .background(ViraColor.accent(area).copy(alpha = 0.10f), shape)
            .then(if (environment.increasedContrast) Modifier.border(2.dp, ViraColor.separator, shape) else Modifier)
            .padding(gap)

        when {
            labeledFits -> Row(
                modifier = Modifier.fillMaxWidth().then(pistModifier),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                items.forEach { item ->
                    Box(Modifier.weight(1f)) {
                        Option(area, item, item.key == selectedKey, allLabels = true, fills = true) { onSelect(item.key) }
                    }
                }
            }
            compactFits -> Row(
                modifier = Modifier.fillMaxWidth().then(pistModifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    Option(area, item, item.key == selectedKey, allLabels = false) { onSelect(item.key) }
                }
            }
            else -> Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).then(pistModifier),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    Option(area, item, item.key == selectedKey, allLabels = false) { onSelect(item.key) }
                }
            }
        }
    }
}

@Composable
private fun Option(
    area: ViraArea,
    item: ViraSectionItem,
    isSelected: Boolean,
    allLabels: Boolean,
    fills: Boolean = false,
    onClick: () -> Unit,
) {
    val environment = LocalViraEnvironment.current
    // Amb icona sobre nom la cara és gairebé quadrada: rectangle arrodonit, concèntric amb la
    // pista. En fila, la càpsula.
    val shape: Shape = if (allLabels) RoundedCornerShape(ViraRadius.m) else CircleShape
    val fill by animateColorAsState(
        if (isSelected) ViraColor.surfaceElevated else Color.Transparent,
        viraSpec(ViraMotion.standard()),
        label = "sectionFill",
    )
    val elevation by animateDpAsState(
        if (isSelected) 4.dp else 0.dp,
        viraSpec(ViraMotion.standard()),
        label = "sectionLift",
    )
    val ink = if (isSelected) ViraColor.onSurface(area) else ViraColor.textPrimary
    val shadowTint = ViraColor.textPrimary.copy(alpha = 0.14f)

    val base = Modifier
        .then(if (fills) Modifier.fillMaxWidth() else Modifier)
        .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
        .shadow(elevation, shape, clip = false, ambientColor = shadowTint, spotColor = shadowTint)
        .background(fill, shape)
        .then(
            if (isSelected && environment.increasedContrast) Modifier.border(2.dp, ViraColor.accent(area), shape)
            else Modifier,
        )
        .viraPressable(role = Role.Tab) { if (!isSelected) onClick() }
        .semantics {
            selected = isSelected
            // Sense nom visible (forma compacta), TalkBack el diu igualment.
            if (!allLabels && !isSelected) contentDescription = item.label
        }

    if (allLabels) {
        Column(
            modifier = base.padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline, Alignment.CenterVertically),
        ) {
            ViraPictogram(item.pictogram, size = 24.dp)
            Text(item.label, style = ViraFont.footnoteEmphasis, color = ink, maxLines = 1, softWrap = false)
        }
    } else {
        Row(
            modifier = base.padding(horizontal = if (isSelected) ViraSpacing.s else ViraSpacing.xs, vertical = ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            ViraPictogram(item.pictogram, size = 28.dp)
            AnimatedVisibility(isSelected, enter = fadeIn(), exit = fadeOut()) {
                Text(item.label, style = ViraFont.headline, color = ink, maxLines = 1, softWrap = false)
            }
        }
    }
}
