package cat.vira.android.features.focus

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Crear o editar un mode de concentració (`FocusProfileEditor`, `USER_FLOWS.md` F17): una sola
/// pantalla verda amb el nom, la icona, dues targetes de temps i el botó al peu.
///
/// **Un sol camp obligatori** (el nom); la resta, amb valors per defecte. No es desa res fins al
/// botó: tornar enrere no perd res i no cal confirmar-ho.
///
/// No és un full sobre la sessió: la substitueix a la mateixa pantalla, perquè la sessió ja és a
/// pantalla completa i no hi ha modal sobre modal (`INFORMATION_ARCHITECTURE.md`).
@Composable
fun FocusProfileEditor(
    profile: FocusProfile?,
    onSave: (FocusProfile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(profile?.name ?: "") }
    var iconName by rememberSaveable { mutableStateOf(profile?.iconName ?: FocusPictogram.FOCUS_MODES) }
    var focusMinutes by rememberSaveable { mutableIntStateOf(profile?.focusMinutes ?: 25) }
    var breakMinutes by rememberSaveable { mutableIntStateOf(profile?.breakMinutes ?: 5) }
    var mutes by rememberSaveable { mutableStateOf(profile?.muteViraNotifications ?: false) }
    var showsTask by rememberSaveable { mutableStateOf(profile?.showsLinkedTask ?: true) }
    val title = stringResource(if (profile == null) R.string.focus_modes_new else R.string.focus_modes_edit)
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val card = RoundedCornerShape(ViraRadius.l)

    fun save() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val base = profile ?: FocusProfile(name = trimmed)
        onSave(
            base.copy(
                name = trimmed, iconName = iconName, focusMinutes = focusMinutes, breakMinutes = breakMinutes,
                muteViraNotifications = mutes, showsLinkedTask = showsTask,
            ),
        )
    }

    Column(modifier.fillMaxSize().viraSectionBackground(ViraArea.Focus).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            FocusIconButton(FocusGlyphKind.Back, stringResource(R.string.action_back), ViraColor.accent(ViraArea.Focus), onBack)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ViraSpacing.screenMargin)
                .padding(bottom = ViraSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
        ) {
            ViraPageTitle(title, ViraArea.Focus)
            NameField(name, onChange = { name = it }, onDone = ::save, autoFocus = profile == null)

            Group(stringResource(R.string.habits_icon)) { IconPicker(iconName) { iconName = it } }

            // Dos números i prou: quant et concentres i quant descanses. El mínim de concentració és
            // 15: per sota, la sessió no arribaria mai al llindar de ViriCoins.
            Group(stringResource(R.string.focus_mode_durations)) {
                val focusCard: @Composable (Modifier) -> Unit = {
                    FocusDurationCard(stringResource(R.string.focus_mode_duration_focus), focusMinutes, 15..120, 5, it) { v -> focusMinutes = v }
                }
                val breakCard: @Composable (Modifier) -> Unit = {
                    FocusDurationCard(stringResource(R.string.focus_mode_duration_break), breakMinutes, 5..45, 5, it) { v -> breakMinutes = v }
                }
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { focusCard(Modifier.fillMaxWidth()); breakCard(Modifier.fillMaxWidth()) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { focusCard(Modifier.weight(1f)); breakCard(Modifier.weight(1f)) }
                }
            }

            Group(stringResource(R.string.focus_mode_session)) {
                Column(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, card)) {
                    ToggleRow(stringResource(R.string.focus_mode_mute), mutes) { mutes = it }
                    HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.separator)
                    ToggleRow(stringResource(R.string.focus_mode_show_task), showsTask) { showsTask = it }
                }
                // El límit real, dit sense tecnicismes: Vira només es pot silenciar a si mateixa.
                Text(
                    stringResource(R.string.focus_mode_mute_footer),
                    style = ViraFont.footnote,
                    color = ViraColor.textSecondary,
                    modifier = Modifier.padding(horizontal = ViraSpacing.xs),
                )
            }

            ViraActionButton(
                stringResource(if (profile == null) R.string.action_add else R.string.action_done),
                ::save,
                Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(title, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, onDone: () -> Unit, autoFocus: Boolean) {
    val label = stringResource(R.string.focus_mode_name_placeholder)
    val focus = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (autoFocus && !inspection) runCatching { focus.requestFocus() } }
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Text(label, style = ViraFont.footnote, color = ViraColor.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = ViraFont.headline.copy(color = ViraColor.textPrimary),
            cursorBrush = SolidColor(ViraColor.accent(ViraArea.Focus)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
        )
    }
}

@Composable
private fun IconPicker(selection: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = ViraSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        FocusPictogram.modeIcons.forEach { name ->
            val chosen = name == selection
            val shape = RoundedCornerShape(ViraRadius.m)
            val label = stringResource(FocusPictogram.label(name))
            Box(
                Modifier
                    .clip(shape)
                    .background(if (chosen) ViraColor.surface(ViraArea.Focus) else ViraColor.surfaceElevated, shape)
                    .then(if (chosen) Modifier.border(2.5.dp, ViraColor.accent(ViraArea.Focus), shape) else Modifier)
                    .viraPressable { onSelect(name) }
                    .semantics {
                        contentDescription = label
                        selected = chosen
                        role = Role.Button
                    }
                    .padding(ViraSpacing.xs),
            ) {
                ViraPictogram(FocusPictogram.drawable(name), size = 44.dp, isScene = name.startsWith("viri-"))
                // La selecció no pot dependre només del fons de color.
                if (chosen) {
                    FocusGlyph(
                        FocusGlyphKind.Check, ViraColor.accent(ViraArea.Focus),
                        Modifier.align(Alignment.BottomEnd), size = 16.dp, ink = ViraColor.onAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = ViraColor.accent(ViraArea.Focus), checkedThumbColor = ViraColor.onAccent),
        )
    }
}
