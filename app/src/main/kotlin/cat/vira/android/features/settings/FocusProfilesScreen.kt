package cat.vira.android.features.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.ViraTileStyle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.resources.viraPluralString
import cat.vira.android.features.settings.export.ViraResourceNames
import kotlinx.coroutines.launch

/// Els modes de concentració de l'usuari: llistar, crear, editar i arxivar (`FocusProfilesView`).
///
/// Viri a dalt amb el recompte, els modes en **fitxes** amb els números en píndoles, una fitxa
/// translúcida per crear-ne un de nou i, a sota, els tres de sempre per saber amb què conviuen.
///
/// **Arxivar, no esborrar**: l'historial de sessions conserva el nom del mode, i esborrar-lo el
/// deixaria orfe. El gest és mantenir premut, i TalkBack el té com a acció de la fitxa.
///
/// L'editor és del fil de Concentra't: `onEdit(null)` en crea un de nou.
@Composable
fun FocusProfilesContent(onEdit: (FocusProfile?) -> Unit) {
    val backend = LocalSettingsBackend.current
    val profiles by backend.focusProfiles.collectAsStateOr(emptyList())
    val scope = rememberCoroutineScope()
    val entries: List<FocusProfile?> = profiles + null

    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
        FocusProfilesHero(profiles.size)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            SectionTitle(stringResource(R.string.focus_modes_mine))
            ViraTileGrid(entries) { profile, modifier ->
                if (profile == null) {
                    AddModeTile(modifier) { onEdit(null) }
                } else {
                    ModeTile(profile, modifier, onOpen = { onEdit(profile) }, onArchive = { scope.launch { backend.archive(profile) } })
                }
            }
            // L'estat buit: Viri ja ho diu a dalt, aquí només què és un mode.
            if (profiles.isEmpty()) {
                Text(stringResource(R.string.focus_modes_empty_message), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        FocusBuiltInModesList()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.semantics { heading() })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModeTile(profile: FocusProfile, modifier: Modifier, onOpen: () -> Unit, onArchive: () -> Unit) {
    val context = LocalContext.current
    val archive = stringResource(R.string.focus_modes_archive)
    val pictogram = remember(profile.iconName) {
        ViraResourceNames.drawable(context, profile.iconName).takeIf { it != 0 } ?: R.drawable.picto_focus_modes
    }
    var isMenuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        ViraTile(
            area = ViraArea.Focus,
            pictogram = pictogram,
            title = profile.name,
            style = ViraTileStyle.Plain,
            modifier = Modifier
                .clip(RoundedCornerShape(ViraRadius.l))
                .combinedClickable(role = Role.Button, onClick = onOpen, onLongClick = { isMenuOpen = true })
                .semantics(mergeDescendants = true) {
                    // TalkBack no descobreix el gest de mantenir premut.
                    customActions = listOf(CustomAccessibilityAction(archive) { onArchive(); true })
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                FocusModePill(stringResource(R.string.focus_minutes, profile.focusMinutes))
                FocusModePill(stringResource(R.string.focus_mode_pill_break, profile.breakMinutes))
                if (profile.muteViraNotifications) FocusModePill(stringResource(R.string.focus_mode_pill_mute))
            }
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            DropdownMenuItem(
                text = { Text(archive, style = ViraFont.body, color = ViraColor.textPrimary) },
                onClick = {
                    isMenuOpen = false
                    onArchive()
                },
            )
        }
    }
}

/// Píndola verda d'una fitxa de mode: sobre fons blanc, la de `ViraTag` no es veuria.
@Composable
private fun FocusModePill(text: String) {
    Text(
        text,
        style = ViraFont.footnoteEmphasis,
        color = ViraColor.onSurface(ViraArea.Focus),
        modifier = Modifier
            .background(ViraColor.surface(ViraArea.Focus), CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
    )
}

/// La franja buida de `DESIGN_RULES.md` §4: translúcida i amb el «+», no un botó gros.
@Composable
private fun AddModeTile(modifier: Modifier, onClick: () -> Unit) {
    val accent = ViraColor.accent(ViraArea.Focus)
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier
            .defaultMinSize(minHeight = ViraTouch.comfortable * 2)
            .clip(shape)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.45f))
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = 0.5f),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(ViraRadius.l.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .viraPressable(onClick = onClick)
            .padding(ViraSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterVertically),
    ) {
        SettingsGlyph(SettingsGlyphKind.Plus, accent, extent = 26.dp)
        Text(stringResource(R.string.focus_modes_new), style = ViraFont.headline, color = accent)
    }
}

/// Mòdul protagonista dels modes (`FocusProfilesHero`): Viri amb el calendari, una frase dins de la
/// seva placa blanca —mai sobre el degradat ni sobre Viri— i el recompte en píndola. A mides
/// d'accessibilitat Viri passa a sobre: al costat, la bombolla quedava en dues paraules per línia.
@Composable
private fun FocusProfilesHero(count: Int) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val message = stringResource(R.string.focus_modes_viri)
    val spoken = stringResource(R.string.today_viri_a11y, message)
    val countText = viraPluralString(R.plurals.focus_modes_count, count, R.string.focus_modes_count_zero, count)
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val text: @Composable (Modifier) -> Unit = { mod ->
        Column(mod.padding(ViraSpacing.m), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Text(
                message,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier
                    .background(ViraColor.surfaceElevated, shape)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                    .semantics { contentDescription = spoken },
            )
            ViraTag(countText, pictogram = R.drawable.picto_focus_modes)
        }
    }
    val viri: @Composable (Modifier) -> Unit = { mod ->
        Image(
            painterResource(R.drawable.viri_entry_calendar),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = mod.widthIn(max = 150.dp).padding(top = ViraSpacing.s),
        )
    }
    val container = Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Focus))
    if (isLarge) {
        Column(container, horizontalAlignment = Alignment.End) {
            viri(Modifier)
            text(Modifier.fillMaxWidth())
        }
    } else {
        Row(container, verticalAlignment = Alignment.Bottom) {
            text(Modifier.weight(1f))
            viri(Modifier)
        }
    }
}

/// Un dels tres modes de sempre (`FocusMode.builtIns()`).
private data class BuiltInMode(val label: Int, @DrawableRes val pictogram: Int, val isScene: Boolean, val minutes: Int)

private val builtInModes = listOf(
    BuiltInMode(R.string.focus_pomodoro_title, R.drawable.viri_focused, true, 25),
    BuiltInMode(R.string.focus_kind_short_break, R.drawable.viri_chill, true, 5),
    BuiltInMode(R.string.focus_kind_long_break, R.drawable.viri_meditating, true, 15),
)

/// Els tres modes de sempre, per saber amb què conviuen els teus al carrusel de Concentra't. Només
/// es miren: no són perfils i no es poden editar ni arxivar.
@Composable
private fun FocusBuiltInModesList() {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        val title = stringResource(R.string.focus_modes_builtin)
        val hint = stringResource(R.string.focus_modes_builtin_hint)
        if (isLarge) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                SectionTitle(title)
                Text(hint, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                SectionTitle(title)
                Spacer(Modifier.weight(1f).widthIn(min = ViraSpacing.xs))
                Text(hint, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        Column(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))) {
            builtInModes.forEachIndexed { index, mode ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.separator)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ViraTouch.minimum)
                        .semantics(mergeDescendants = true) {}
                        .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
                ) {
                    ViraPictogram(mode.pictogram, size = 32.dp, isScene = mode.isScene, scalesWithText = true)
                    val label = stringResource(mode.label)
                    val minutes = stringResource(R.string.focus_minutes, mode.minutes)
                    if (isLarge) {
                        // A lletra gran, en fila el nom es partia a mitja paraula («Concentrac-ió»).
                        Column(Modifier.weight(1f)) {
                            Text(label, style = ViraFont.body, color = ViraColor.textPrimary)
                            Text(minutes, style = ViraFont.body, color = ViraColor.textSecondary)
                        }
                    } else {
                        Text(label, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
                        Text(minutes, style = ViraFont.body, color = ViraColor.textSecondary)
                    }
                }
            }
        }
    }
}
