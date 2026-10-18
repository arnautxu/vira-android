package cat.vira.android.core.design.components

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// El que la pastilla de perfil ensenya. Les preferències manen: amb «Mostrar progrés» apagat no
/// hi surten números (`coins == null`), i amb la ratxa amagada només hi ha les monedes
/// (`streakDays == null`).
data class ViraProfileSummary(
    val name: String,
    val coins: Int? = null,
    val streakDays: Int? = null,
)

/// Capçalera fixa de totes les pestanyes (`ViraTopBar`, `DESIGN_RULES.md` §1): la pastilla de
/// perfil a l'esquerra i «Ajuda ara» a la dreta, tot dins d'una pastilla blanca d'ample sencer.
///
/// - Una sola fila; només a mida de lletra d'accessibilitat es parteix en dues, i la pastilla
///   passa a rectangle arrodonit.
/// - El fons és `surfaceShift(area)`, el to d'entrada del fons de la pantalla de sota, perquè
///   capçalera i pantalla es llegeixin com una sola superfície.
/// - «Ajuda ara» és fixa i sempre visible: a un toc des de qualsevol punt (D-39).
/// - La pastilla obre un menú amb Perfil i Ajustos: no hi ha engranatge.
///
/// Sense estat propi: qui la posa li dona el resum i les tres accions. `avatar` és el retrat de la
/// skin posada, que dibuixa la Botiga; sense ell, el cercle lila buit.
@Composable
fun ViraTopBar(
    area: ViraArea,
    profile: ViraProfileSummary,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit = {},
) {
    val environment = LocalViraEnvironment.current
    val stacked = environment.isAccessibilitySize
    val shape: Shape = if (stacked) RoundedCornerShape(ViraRadius.l) else CircleShape
    val borderColor = if (environment.increasedContrast) ViraColor.textPrimary else ViraColor.separator
    val borderWidth = if (environment.increasedContrast) 2.dp else 1.dp

    val bar = Modifier
        .fillMaxWidth()
        .shadow(8.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.08f), spotColor = ViraColor.shadow.copy(alpha = 0.08f))
        .background(ViraColor.surfaceElevated, shape)
        .border(borderWidth, borderColor, shape)
        .padding(start = ViraSpacing.xxs, top = ViraSpacing.xxs, bottom = ViraSpacing.xxs, end = ViraSpacing.xs)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceShift(area))
            .statusBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
    ) {
        if (stacked) {
            Column(modifier = bar, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                ViraProfilePill(profile, onProfile, onSettings, avatar)
                ViraHelpButton(onHelp)
            }
        } else {
            Row(
                modifier = bar,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.weight(1f, fill = false).padding(end = ViraSpacing.s)) {
                    ViraProfilePill(profile, onProfile, onSettings, avatar)
                }
                ViraHelpButton(onHelp)
            }
        }
    }
}

/// «Ajuda ara»: contorn lila i text lila, sempre a la dreta de la capçalera.
@Composable
fun ViraHelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.action_help_now),
        style = ViraFont.headline,
        color = ViraColor.brandAccent,
        maxLines = 1,
        modifier = modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.6f), CircleShape)
            .border(1.5.dp, ViraColor.brandAccent.copy(alpha = 0.45f), CircleShape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.s),
    )
}

/// Avatar, malnom i, a sota, ViriCoins · ratxa. Tocar-la obre el menú Perfil / Ajustos.
@Composable
fun ViraProfilePill(
    profile: ViraProfileSummary,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    avatar: @Composable () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    val avatarSize = 44.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    val description = buildList {
        add(profile.name)
        profile.coins?.let { add("$it ${stringResource(R.string.a11y_metric_coins)}") }
        profile.streakDays?.let { add("$it ${stringResource(R.string.a11y_metric_streak)}") }
    }.joinToString(", ")
    val hint = stringResource(R.string.profile_pill_hint)

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            modifier = Modifier
                .clip(CircleShape)
                .viraPressable(onClickLabel = hint) { menuOpen = true }
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                    onClick(hint) { menuOpen = true; true }
                }
                .padding(end = ViraSpacing.m),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(ViraColor.brandSurface)
                    .border(2.dp, ViraColor.brand, CircleShape),
            ) { avatar() }

            Column {
                Text(
                    text = profile.name,
                    style = ViraFont.headline,
                    color = ViraColor.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.coins != null) ProfileMetrics(profile.coins, profile.streakDays)
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_profile), style = ViraFont.body) },
                onClick = { menuOpen = false; onProfile() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_settings), style = ViraFont.body) },
                onClick = { menuOpen = false; onSettings() },
            )
        }
    }
}

/// Icona + número: la icona sola seria un jeroglífic.
@Composable
private fun ProfileMetrics(coins: Int, streakDays: Int?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(R.drawable.picto_shop, size = 16.dp, scalesWithText = true)
        Text("$coins", style = ViraFont.footnote, color = ViraColor.textSecondary, maxLines = 1)
        if (streakDays != null) {
            Text("·", style = ViraFont.footnote, color = ViraColor.textSecondary)
            ViraPictogram(R.drawable.picto_flame, size = 16.dp, scalesWithText = true)
            Text(
                text = pluralStringResource(R.plurals.profile_pill_streak_days, streakDays, streakDays),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
                maxLines = 1,
            )
        }
    }
}
