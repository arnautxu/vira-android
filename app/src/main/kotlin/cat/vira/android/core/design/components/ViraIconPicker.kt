package cat.vira.android.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Selector de pictograma per a un ítem que l'usuari crea (`ViraIconPicker` de l'iPhone): la
/// captura ràpida i l'editor d'hàbits el comparteixen perquè la selecció es vegi igual als dos.
///
/// **Graella, no fila**: cada icona dins d'una targeta pròpia sempre visible. Se n'ensenyen sis i
/// el «+» final desplega la resta; la triada no s'amaga mai. **La selecció mai depèn només del
/// color**: la triada porta també una marca de verificació. Només s'hi ofereixen noms del
/// catàleg (`ViraPictogramName.planIcons`, `habitIcons`, `focusModeIcons`).
@Composable
fun ViraIconPicker(
    icons: List<String>,
    selection: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Plan,
) {
    val scale = LocalDensity.current.fontScale
    val iconSize = 36.dp * scale
    // Mida de la targeta, no de la icona: marge perquè el pictograma no toqui la vora, i prou
    // perquè el tàctil no baixi mai del mínim.
    val cellSize = 60.dp * scale
    var isShowingAll by rememberSaveable { mutableStateOf(false) }
    val visible = if (isShowingAll) icons else icons.take(6).let {
        if (selection !in it && selection in icons) it + selection else it
    }
    val showsMore = !isShowingAll && icons.size > visible.size
    val cells: List<String?> = visible + if (showsMore) listOf(null) else emptyList()

    // Com el `LazyVGrid(.adaptive(minimum:))` de l'iPhone: tantes columnes com hi càpiguen, i
    // cada targeta centrada a la seva.
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = ViraSpacing.xxs)) {
        val spacing = ViraSpacing.s
        val columns = maxOf(1, ((maxWidth + spacing) / (cellSize + spacing)).toInt())
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            cells.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(columns) { index ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            when {
                                index >= row.size -> Unit
                                row[index] == null -> MoreCell(cellSize, area) { isShowingAll = true }
                                else -> {
                                    val name = row[index]!!
                                    IconCell(
                                        name = name,
                                        isSelected = name == selection,
                                        iconSize = if (name.startsWith("picto-cal-")) iconSize * 1.2f else iconSize,
                                        cellSize = cellSize,
                                        area = area,
                                    ) { onSelect(name) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconCell(
    name: String,
    isSelected: Boolean,
    iconSize: Dp,
    cellSize: Dp,
    area: ViraArea,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.m)
    val accent = ViraColor.accent(area)
    val label = ViraPictogramName.accessibilityLabel(name)?.let { stringResource(it) } ?: ""
    val elevated = ViraColor.surfaceElevated
    Box(
        Modifier
            .size(cellSize)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSelected
                onClick { onClick(); true }
            }
            .background(if (isSelected) ViraColor.surface(area) else elevated, shape)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else ViraColor.separator, shape),
        contentAlignment = Alignment.Center,
    ) {
        ViraPictogram(name, size = iconSize)
        if (isSelected) {
            SelectedMark(
                accent,
                elevated,
                Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(18.dp * LocalDensity.current.fontScale),
            )
        }
    }
}

/// `checkmark.circle.fill` sobre un cercle de la superfície, perquè es llegeixi sobre la vora.
@Composable
private fun SelectedMark(fill: Color, backing: Color, modifier: Modifier) {
    Canvas(modifier) {
        drawCircle(backing)
        drawCircle(fill, radius = size.minDimension / 2 * 0.86f)
        val w = size.width
        val check = Path().apply {
            moveTo(w * 0.30f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.66f)
            lineTo(w * 0.70f, w * 0.38f)
        }
        drawPath(check, backing, style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun MoreCell(cellSize: Dp, area: ViraArea, onClick: () -> Unit) {
    val label = stringResource(R.string.action_show_more)
    val ink = ViraColor.onSurface(area)
    Box(
        Modifier
            .size(cellSize)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick { onClick(); true }
            }
            .background(ViraColor.surface(area), RoundedCornerShape(ViraRadius.m)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp * LocalDensity.current.fontScale)) {
            val stroke = size.width * 0.16f
            drawLine(ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke, StrokeCap.Round)
            drawLine(ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
        }
    }
}
