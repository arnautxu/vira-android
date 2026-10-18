package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec

/// Un destí de la barra: el nom curt (sempre visible) i la icona.
data class ViraTabBarItem(
    val key: String,
    val label: String,
    @DrawableRes val icon: Int,
)

/// Mides de la barra, calcades de `ViraTabBarMetrics`.
object ViraTabBarMetrics {
    val icon: Dp = 26.dp
    val iconCeiling: Dp = 40.dp
    val verticalPadding: Dp = 16.dp
    val clearanceGap: Dp = 24.dp
}

/// Barra inferior de Vira (`ViraTabBar`): quatre destins i Concentra't com a acció central, en
/// una càpsula **sempre opaca** que sura dins dels marges (Arnau, 2026-10-09).
///
/// Totes les icones porten el nom visible. La pestanya triada porta la pastilla lila suau al
/// darrere, el nom en lila fosc i fa un bot; les altres van al 78 % (al 100 % amb contrast alt).
/// Si un nom llarg o la lletra gran no hi caben, la barra es desplaça en horitzontal en lloc de
/// tallar cap nom.
///
/// Sense estat: `selectedKey` és la pestanya activa, `items` són les quatre (els dos primers van a
/// l'esquerra de Concentra't i els dos últims a la dreta).
@Composable
fun ViraTabBar(
    items: List<ViraTabBarItem>,
    selectedKey: String,
    focus: ViraTabBarItem,
    onTabTap: (String) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = LocalViraEnvironment.current
    val scrolls = environment.isAccessibilitySize
    val inactiveAlpha = if (environment.increasedContrast) 1f else 0.78f
    val iconSize = (ViraTabBarMetrics.icon * LocalDensity.current.fontScale).coerceAtMost(ViraTabBarMetrics.iconCeiling)
    val half = items.size / 2

    val content: @Composable () -> Unit = {
        items.take(half).forEach { item ->
            TabItem(item, item.key == selectedKey, iconSize, inactiveAlpha) { onTabTap(item.key) }
        }
        TabItem(focus, isSelected = false, iconSize, inactiveAlpha, onClick = onFocus)
        items.drop(half).forEach { item ->
            TabItem(item, item.key == selectedKey, iconSize, inactiveAlpha) { onTabTap(item.key) }
        }
    }

    val capsule = Modifier
        .shadow(12.dp, CircleShape, ambientColor = ViraColor.shadow.copy(alpha = 0.10f), spotColor = ViraColor.shadow.copy(alpha = 0.10f))
        .background(ViraColor.surfaceElevated, CircleShape)
        .border(
            if (environment.increasedContrast) 2.dp else 1.dp,
            if (environment.increasedContrast) ViraColor.textPrimary else ViraColor.separator,
            CircleShape,
        )
        .padding(ViraSpacing.xs)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
    ) {
        if (scrolls) {
            // A lletra gran els noms conserven l'ample sencer i la barra llisca: cinquens iguals
            // tallarien «Organització».
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).then(capsule),
                verticalAlignment = Alignment.CenterVertically,
            ) { content() }
        } else {
            TabRow(Modifier.fillMaxWidth().then(capsule), content)
        }
    }
}

@Composable
private fun TabItem(
    item: ViraTabBarItem,
    isSelected: Boolean,
    iconSize: Dp,
    inactiveAlpha: Float,
    onClick: () -> Unit,
) {
    val pill by animateColorAsState(
        targetValue = if (isSelected) ViraColor.brandSurface else Color.Transparent,
        animationSpec = viraSpec(ViraMotion.standard()),
        label = "tabPill",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        modifier = Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(pill, RoundedCornerShape(ViraRadius.m))
            .viraPressable(role = Role.Tab, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = ViraSpacing.xxs, vertical = ViraSpacing.xs),
    ) {
        ViraPictogram(
            item.icon,
            size = iconSize,
            modifier = Modifier.alpha(if (isSelected) 1f else inactiveAlpha).viraPop(isOn = isSelected),
        )
        Text(
            text = item.label,
            style = ViraFont.caption,
            color = if (isSelected) ViraColor.brandOnSurface else ViraColor.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/// Reparteix l'ample com l'`HStack` de l'iPhone amb `.frame(maxWidth: .infinity)` i noms
/// `fixedSize`: cap nom es talla, i l'espai que sobra es reparteix a parts iguals entre els que en
/// necessiten menys.
@Composable
private fun TabRow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val total = constraints.maxWidth
        val widths = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }.toIntArray()
        // Els que ja ocupen més que una part igual es queden amb el seu ample; la resta es reparteix.
        val fixed = BooleanArray(widths.size)
        while (true) {
            val free = fixed.count { !it }
            if (free == 0) break
            val share = (total - widths.indices.filter { fixed[it] }.sumOf { widths[it] }) / free
            val grown = widths.indices.filter { !fixed[it] && widths[it] > share }
            if (grown.isEmpty()) {
                widths.indices.filter { !fixed[it] }.forEach { widths[it] = share }
                break
            }
            grown.forEach { fixed[it] = true }
        }
        val placeables = measurables.mapIndexed { i, m -> m.measure(Constraints.fixedWidth(widths[i]).copy(maxHeight = constraints.maxHeight)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val width = placeables.sumOf { it.width }.coerceAtLeast(constraints.minWidth)
        layout(width, height) {
            var x = 0
            placeables.forEach { it.place(x, (height - it.height) / 2); x += it.width }
        }
    }
}

/// Marge inferior perquè l'últim element d'una pantalla quedi per sobre de la barra
/// (`viraTabBarClearance`): icona, nom, farciment i l'aire de sota.
@Composable
fun viraTabBarClearance(): Dp {
    val scale = LocalDensity.current.fontScale
    val icon = (ViraTabBarMetrics.icon * scale).coerceAtMost(ViraTabBarMetrics.iconCeiling)
    return icon + 14.dp * scale + ViraSpacing.xxs + ViraTabBarMetrics.verticalPadding + ViraTabBarMetrics.clearanceGap
}
