package cat.vira.android.features.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Capçalera del panell de pares (`ParentTopBar` de l'iPhone, `DESIGN_RULES.md` §1).
///
/// La mateixa pastilla blanca d'ample sencer que `ViraTopBar`, amb dues diferències que no són
/// estètiques:
/// - **El compte, no la skin.** Un pare o una mare no té Viri, ni monedes, ni ratxa: la pastilla
///   diu de qui és el panell i quants fills o filles hi ha vinculats, i obre el Compte.
/// - **«Vincula» a la dreta, no «Ajuda ara».** Ajuda ara és l'eina del jove en un mal moment;
///   aquí, l'acció que un adult busca és afegir un vincle.
@Composable
internal fun ParentTopBar(linkedCount: Int, onAccount: () -> Unit, onLink: () -> Unit, modifier: Modifier = Modifier) {
    val environment = LocalViraEnvironment.current
    val stacked = environment.isAccessibilitySize
    val shape: Shape = if (stacked) RoundedCornerShape(ViraRadius.l) else CircleShape
    val bar = Modifier
        .fillMaxWidth()
        .shadow(8.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.08f), spotColor = ViraColor.shadow.copy(alpha = 0.08f))
        .background(ViraColor.surfaceElevated, shape)
        .border(
            if (environment.increasedContrast) 2.dp else 1.dp,
            if (environment.increasedContrast) ViraColor.textPrimary else ViraColor.separator,
            shape,
        )
        .padding(start = ViraSpacing.xxs, top = ViraSpacing.xxs, bottom = ViraSpacing.xxs, end = ViraSpacing.xs)

    Box(
        modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceShift(ViraArea.Today))
            .statusBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
    ) {
        if (stacked) {
            Column(bar, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                AccountPill(linkedCount, onAccount)
                LinkButton(onLink)
            }
        } else {
            Row(bar, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f, fill = false).padding(end = ViraSpacing.s)) { AccountPill(linkedCount, onAccount) }
                LinkButton(onLink)
            }
        }
    }
}

@Composable
private fun AccountPill(linkedCount: Int, onAccount: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val name = stringResource(R.string.parent_topbar_name)
    val count = stringResource(R.string.parent_topbar_count, linkedCount)
    val hint = stringResource(R.string.account_settings_title)
    val avatar = 44.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            modifier = Modifier
                .clip(CircleShape)
                .viraPressable(onClickLabel = hint) { menuOpen = true }
                .clearAndSetSemantics {
                    contentDescription = "$name, $count"
                    role = Role.Button
                    onClick(hint) { menuOpen = true; true }
                }
                .padding(end = ViraSpacing.m),
        ) {
            ViraPictogram(R.drawable.picto_profile, size = avatar)
            Column {
                Text(name, style = ViraFont.headline, color = ViraColor.textPrimary, maxLines = if (LocalViraEnvironment.current.isAccessibilitySize) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                Text(count, style = ViraFont.footnote, color = ViraColor.textSecondary, maxLines = 1)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(hint, style = ViraFont.body) },
                onClick = { menuOpen = false; onAccount() },
            )
        }
    }
}

@Composable
private fun LinkButton(onLink: () -> Unit) {
    val label = stringResource(R.string.parent_link_add)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        modifier = Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.6f), CircleShape)
            .border(1.5.dp, ViraColor.brandAccent.copy(alpha = 0.45f), CircleShape)
            .viraPressable(onClick = onLink)
            .clearAndSetSemantics { contentDescription = label; role = Role.Button; onClick(label) { onLink(); true } }
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.s),
    ) {
        PlusGlyph(ViraColor.brandAccent)
        Text(stringResource(R.string.parent_redeem_submit), style = ViraFont.headline, color = ViraColor.brandAccent, maxLines = 1)
    }
}

/// El «+» de `systemImage: "plus"`, que escala amb la lletra.
@Composable
internal fun PlusGlyph(color: Color, modifier: Modifier = Modifier) {
    val side = 14.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(modifier.size(side)) {
        val stroke = size.width * 0.16f
        drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
    }
}
