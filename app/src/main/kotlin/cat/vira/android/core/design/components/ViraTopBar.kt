package cat.vira.android.core.design.components

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import java.util.Locale

/// El que la capçalera ensenya. Les preferències manen: amb «Mostrar progrés» apagat no hi surten
/// números (`coins == null`), i amb la ratxa amagada només hi ha les monedes (`streakDays == null`).
/// `streakCountsToday`: avui ja suma, i la flama s'encén.
data class ViraProfileSummary(
    val name: String,
    val coins: Int? = null,
    val streakDays: Int? = null,
    val streakCountsToday: Boolean = false,
)

/// Capçalera fixa de totes les pestanyes (`ViraTopBar`, `DESIGN_RULES.md` §1): avatar, ratxa,
/// monedes i «Ajuda ara» (Arnau, 2026-10-22).
///
/// Ja no és una pastilla blanca amb el nom: és com la de Duolingo, sense caixa, cada número és un
/// botó que obre el seu panell just a sota, i la ratxa diu si avui ja suma.
///
/// - Una fila; només a mida de lletra d'accessibilitat, dues: avatar i ajuda a dalt, comptadors a
///   sota.
/// - El fons és `surfaceShift(area)`, el to d'entrada de la pantalla de sota, i una línia fina les
///   separa.
/// - L'avatar obre un menú amb Perfil i Ajustos: no hi ha engranatge. El malnom ja no s'hi veu,
///   però TalkBack el diu.
/// - «Ajuda ara» és fixa i sempre visible: a un toc des de qualsevol punt (D-39).
///
/// `helpModifier` deixa que l'arrel digui al tomb on és «Ajuda ara».
///
/// Sense estat de dades: qui la posa li dona el resum, les accions i el contingut dels dos panells.
@Composable
fun ViraTopBar(
    area: ViraArea,
    profile: ViraProfileSummary,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    helpModifier: Modifier = Modifier,
    avatar: @Composable () -> Unit = {},
    streakPanel: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
    coinsPanel: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    val environment = LocalViraEnvironment.current
    val lineColor = if (environment.increasedContrast) ViraColor.textPrimary else ViraColor.separator
    val lineHeight = if (environment.increasedContrast) 2.dp else 1.dp

    Column(modifier.fillMaxWidth().background(ViraColor.surfaceShift(area))) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xxs),
        ) {
            val counters = @Composable { ViraTopBarCounters(area, profile, streakPanel, coinsPanel) }
            if (environment.isAccessibilitySize) {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ViraProfileButton(profile.name, onProfile, onSettings, avatar)
                        Spacer(Modifier.weight(1f))
                        ViraHelpButton(onHelp, helpModifier)
                    }
                    if (profile.coins != null) counters()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                ) {
                    ViraProfileButton(profile.name, onProfile, onSettings, avatar, Modifier.padding(end = ViraSpacing.xxs))
                    if (profile.coins != null) counters()
                    Spacer(Modifier.weight(1f))
                    ViraHelpButton(onHelp, helpModifier)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(lineHeight).background(lineColor))
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

/// L'avatar de la skin posada. Obre el menú Perfil / Ajustos. 40 dp de dibuix, 48 de toc.
@Composable
fun ViraProfileButton(
    name: String,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    avatar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val side = 40.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    val ring = if (LocalViraEnvironment.current.increasedContrast) ViraColor.textPrimary else ViraColor.brand
    val hint = stringResource(R.string.profile_pill_hint)

    Box(modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(ViraTouch.minimum, ViraTouch.minimum)
                .clip(CircleShape)
                .viraPressable(onClickLabel = hint) { menuOpen = true }
                .clearAndSetSemantics {
                    contentDescription = name
                    role = Role.Button
                    onClick(hint) { menuOpen = true; true }
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(side)
                    .clip(CircleShape)
                    .background(ViraColor.brandSurface)
                    .border(2.dp, ring, CircleShape),
            ) { avatar() }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = ViraColor.surfaceElevated,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_profile), style = ViraFont.body, color = ViraColor.textPrimary) },
                onClick = { menuOpen = false; onProfile() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_settings), style = ViraFont.body, color = ViraColor.textPrimary) },
                onClick = { menuOpen = false; onSettings() },
            )
        }
    }
}

/// La ratxa i les ViriCoins: icona + número, cadascun un botó que obre el seu panell
/// (`ViraTopBarCounters` de l'iPhone).
///
/// Mentre el dia no ha sumat, la flama surt apagada i el número en gris. Quan suma, s'encén, el
/// número passa a lila i hi surt una ✓: l'estat no depèn només del color. El número no canvia ni
/// es perd res; cap avís ni retret.
@Composable
private fun ViraTopBarCounters(
    area: ViraArea,
    profile: ViraProfileSummary,
    streakPanel: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    coinsPanel: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    val coins = profile.coins ?: return
    val scale = LocalDensity.current.fontScale.coerceAtMost(2f)
    val icon = 26.dp * scale
    var streakOpen by remember { mutableStateOf(false) }
    var coinsOpen by remember { mutableStateOf(false) }
    // La ratxa acaba de créixer: la flama fa un bot. Quan baixa no fa res: la ratxa mai es castiga.
    var bumps by remember { mutableIntStateOf(0) }
    var lastDays by remember { mutableStateOf(profile.streakDays) }
    LaunchedEffect(profile.streakDays) {
        if ((profile.streakDays ?: 0) > (lastDays ?: 0)) bumps++
        lastDays = profile.streakDays
    }

    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        profile.streakDays?.let { days ->
            val lit = profile.streakCountsToday
            val state = stringResource(if (lit) R.string.topbar_streak_today_done else R.string.topbar_streak_today_pending)
            val spoken = "$days ${stringResource(R.string.a11y_metric_streak)}. $state"
            Box {
                CounterLabel(
                    isActive = streakOpen,
                    activeFill = ViraColor.brandSurface,
                    description = spoken,
                    hint = stringResource(R.string.topbar_streak_hint),
                    onClick = { streakOpen = true },
                ) {
                    Box(Modifier.viraPop(bumps, scale = 1.4f)) {
                        ViraTopBarFlame(icon, isLit = lit)
                        if (lit) TodayCheck(area, 15.dp * scale, Modifier.align(Alignment.BottomEnd))
                    }
                    Text(
                        "$days",
                        style = ViraFont.headline,
                        color = if (lit) ViraColor.brandAccent else ViraColor.textSecondary,
                        maxLines = 1,
                    )
                }
                ViraTopBarPanel(streakOpen, onDismiss = { streakOpen = false }, content = streakPanel)
            }
        }
        Box {
            CounterLabel(
                isActive = coinsOpen,
                activeFill = ViraColor.surface(ViraArea.Rewards),
                description = "$coins ${stringResource(R.string.a11y_metric_coins)}",
                hint = stringResource(R.string.topbar_coins_hint),
                onClick = { coinsOpen = true },
            ) {
                ViraPictogram(R.drawable.picto_shop, size = icon)
                Text(
                    compactCount(coins, viraLocale()),
                    style = ViraFont.headline,
                    color = ViraColor.onSurface(ViraArea.Rewards),
                    maxLines = 1,
                )
            }
            ViraTopBarPanel(coinsOpen, onDismiss = { coinsOpen = false }, content = coinsPanel)
        }
    }
}

/// A partir de 10.000, en format curt («10,8 k»): amb sis xifres la fila no hi cabia al costat
/// d'«Ajuda ara».
fun compactCount(value: Int, locale: Locale): String {
    if (value < 10_000) return java.text.NumberFormat.getIntegerInstance(locale).format(value)
    val format = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
    format.maximumFractionDigits = 1
    return format.format(value)
}

/// La flama de marca; apagada (en gris) fins que avui suma.
@Composable
fun ViraTopBarFlame(size: Dp, isLit: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.picto_flame),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = if (isLit) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier.size(size).alpha(if (isLit) 1f else 0.45f),
    )
}

/// La ✓ d'«avui ja suma»: la forma diu l'estat, no el color.
@Composable
private fun TodayCheck(area: ViraArea, side: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = side * 0.3f, y = side * 0.2f)
            .size(side)
            .background(ViraColor.brandAccent, CircleShape)
            .border(1.5.dp, ViraColor.surfaceShift(area), CircleShape),
    ) {
        Text("✓", style = ViraFont.caption.copy(fontWeight = FontWeight.Black), color = ViraColor.onAccent, maxLines = 1)
    }
}

/// Icona + número a mida de títol de fila, amb 48 dp de toc. Mentre el seu panell és obert, el
/// comptador queda marcat.
@Composable
private fun CounterLabel(
    isActive: Boolean,
    activeFill: androidx.compose.ui.graphics.Color,
    description: String,
    hint: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.s)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        modifier = Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(shape)
            .background(if (isActive) activeFill else androidx.compose.ui.graphics.Color.Transparent, shape)
            .viraPressable(onClickLabel = hint, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(hint) { onClick(); true }
            }
            .padding(horizontal = ViraSpacing.xs),
    ) { content() }
}

/// El panell que baixa d'un comptador: un globus just a sota, que es tanca tocant fora; a mida
/// de lletra d'accessibilitat, un full, perquè el text gran hi càpiga sencer.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViraTopBarPanel(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        if (!expanded) return
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ViraColor.surfaceElevated,
            modifier = Modifier.statusBarsPadding(),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(ViraSpacing.l),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            ) { content(onDismiss) }
        }
    } else {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = ViraColor.surfaceElevated,
            shape = RoundedCornerShape(ViraRadius.l),
        ) {
            Column(
                Modifier.width(340.dp).padding(ViraSpacing.l),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            ) { content(onDismiss) }
        }
    }
}
