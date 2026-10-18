package cat.vira.android.features.focus

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.util.UUID

/// Vincular una tasca del dia a la sessió, o cap: la fitxa «Tasca» del panell (`FocusTaskTile`).
@Composable
fun FocusTaskTile(items: List<PlanItem>, selection: UUID?, onSelect: (PlanItem?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val none = stringResource(R.string.focus_task_none)
    val selected = items.firstOrNull { it.id == selection }?.title ?: none
    val pick = stringResource(R.string.focus_task_pick)
    Box {
        FocusLaunchTile(
            title = stringResource(R.string.focus_panel_task),
            value = selected,
            modifier = Modifier
                .viraPressable { open = true }
                .clearAndSetSemantics {
                    contentDescription = pick
                    stateDescription = selected
                    role = Role.DropdownList
                },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { MenuText(none, selection == null) }, onClick = { open = false; onSelect(null) })
            items.forEach { item ->
                DropdownMenuItem(text = { MenuText(item.title, item.id == selection) }, onClick = { open = false; onSelect(item) })
            }
        }
    }
}

@Composable
private fun MenuText(text: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        // La marca és forma, no només color: la tria es veu en grisos.
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (checked) FocusGlyph(FocusGlyphKind.Check, ViraColor.accent(ViraArea.Focus), size = 18.dp, ink = ViraColor.onAccent)
        }
        Text(text, style = ViraFont.body, color = ViraColor.textPrimary)
    }
}

/// Una fitxa blanca del panell: icona, què és i què hi ha triat (`FocusLaunchTile`). Les fletxes
/// diuen que hi ha tria. A lletra gran passa de fitxa a fila.
@Composable
fun FocusLaunchTile(title: String, value: String, modifier: Modifier = Modifier) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.m)
    val icon: @Composable () -> Unit = { ViraPictogram(R.drawable.picto_plan_check, size = 28.dp, scalesWithText = true) }
    val chevrons: @Composable () -> Unit = { FocusGlyph(FocusGlyphKind.Chevrons, ViraColor.textSecondary, size = 14.dp) }
    val texts: @Composable (Modifier) -> Unit = { slot ->
        Column(slot, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ViraFont.footnote, color = ViraColor.textSecondary)
            // Pot ser el títol d'una tasca, que és text de l'usuari.
            Text(value, style = ViraFont.headline, color = ViraColor.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    val container = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = ViraTouch.comfortable)
        .clip(shape)
        .background(ViraColor.surfaceElevated, shape)
        .padding(ViraSpacing.s)
    if (stacked) {
        Row(container, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            icon(); texts(Modifier.weight(1f)); chevrons()
        }
    } else {
        Column(container, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Row(Modifier.fillMaxWidth()) { icon(); Spacer(Modifier.weight(1f)); chevrons() }
            texts(Modifier)
        }
    }
}
