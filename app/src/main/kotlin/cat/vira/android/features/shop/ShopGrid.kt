package cat.vira.android.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraSpacing

/// Quantes columnes té una graella de la Botiga: tres, dues amb lletra gran i una a mides
/// d'accessibilitat, perquè el nom i el botó no es tallin.
@Composable
fun shopColumnCount(): Int = when {
    LocalViraEnvironment.current.isAccessibilitySize -> 1
    LocalDensity.current.fontScale >= 1.3f -> 2
    else -> 3
}

/// Graella amb les fitxes de cada fila igual d'altes (`LazyVGrid` amb alineació a dalt).
@Composable
fun <T> ShopGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = ViraSpacing.xs,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                row.forEach { cell(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
