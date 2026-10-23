package cat.vira.android.features.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Capçalera plegable de les targetes d'Avui (`TodayCardDisclosureButton`): el títol, «Obre» o
/// «Plega» en text i la fletxa. TalkBack en diu l'estat.
@Composable
internal fun TodayCardDisclosure(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    ink: androidx.compose.ui.graphics.Color,
    label: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val state = stringResource(if (isExpanded) R.string.today_card_expanded else R.string.today_card_collapsed)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable { onExpandedChange(!isExpanded) }
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        label()
        Text(
            stringResource(if (isExpanded) R.string.today_card_collapse else R.string.today_card_expand),
            style = ViraFont.caption,
            color = ink,
        )
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = ink,
            )
        }
    }
}
