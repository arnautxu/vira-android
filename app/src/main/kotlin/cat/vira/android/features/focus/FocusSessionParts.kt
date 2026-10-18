package cat.vira.android.features.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Capçalera de la sessió en marxa i del final (`FocusSessionBar`): la mateixa pastilla blanca que
/// `ViraTopBar`, amb el mode i la tasca en lloc del perfil. «Ajuda ara» no hi és: l'única sortida
/// és «Acaba», que es desa igual.
@Composable
fun FocusSessionBar(
    mode: FocusMode,
    title: String,
    taskTitle: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val environment = LocalViraEnvironment.current
    val stacked = environment.isAccessibilitySize
    val shape: Shape = if (stacked) RoundedCornerShape(ViraRadius.l) else CircleShape
    // Un integrat porta Viri, i Viri ja surt a l'anell: a la capçalera, el pictograma de focus.
    val pictogram = if (mode.isScene) FocusPictogram.FOCUS else mode.pictogram
    val identity: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Box(Modifier.background(ViraColor.surface(ViraArea.Focus), CircleShape).padding(ViraSpacing.xxs)) {
                ViraPictogram(FocusPictogram.drawable(pictogram), size = 40.dp, scalesWithText = true)
            }
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
                Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
                if (taskTitle != null) {
                    Text(
                        taskTitle, style = ViraFont.footnote, color = ViraColor.textSecondary,
                        maxLines = if (stacked) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    val border = if (environment.increasedContrast) ViraColor.textPrimary else ViraColor.separator
    val barModifier = modifier
        .padding(horizontal = ViraSpacing.screenMargin)
        .padding(top = ViraSpacing.xs)
        .fillMaxWidth()
        .shadow(8.dp, shape, ambientColor = ViraColor.shadow, spotColor = ViraColor.shadow)
        .background(ViraColor.surfaceElevated, shape)
        .border(if (environment.increasedContrast) 2.dp else 1.dp, border, shape)
        .padding(ViraSpacing.xxs)
        .padding(end = ViraSpacing.xxs)
    if (stacked) {
        Column(barModifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            identity()
            trailing()
        }
    } else {
        Row(barModifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { identity() }
            trailing()
        }
    }
}

/// Píndola de text: estat, progrés, minuts, monedes. Sempre amb text, mai només color.
@Composable
fun FocusPill(
    text: String,
    modifier: Modifier = Modifier,
    glyph: FocusGlyphKind? = null,
    pictogram: Int? = null,
    accent: Boolean = false,
) {
    val ink = if (accent) ViraColor.onAccent else ViraColor.textPrimary
    Row(
        modifier
            .background(if (accent) ViraColor.accent(ViraArea.Focus) else ViraColor.surfaceElevated, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (glyph != null) FocusGlyph(glyph, ink, size = 14.dp)
        if (pictogram != null) ViraPictogram(pictogram, size = 20.dp, scalesWithText = true)
        Text(text, style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum"), color = ink)
    }
}

/// La bombolla que surt de Viri. Blanca, fora de la il·lustració i del degradat (`DESIGN_RULES.md` §5).
@Composable
fun FocusViriBubble(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Text(
        text,
        style = ViraFont.headline,
        color = ViraColor.textPrimary,
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = ViraColor.accent(ViraArea.Focus), spotColor = ViraColor.accent(ViraArea.Focus))
            .background(ViraColor.surfaceElevated, shape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
    )
}

/// Controls de la sessió en marxa: botons d'1–3 paraules, ≥ 56 dp, sense cap confirmació per
/// acabar. A lletra gran, un per fila: de costat, les paraules es partien pel mig.
@Composable
fun FocusControls(
    run: FocusTimerState.Run,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondary: @Composable (Modifier) -> Unit = { slot ->
        when (run) {
            FocusTimerState.Run.RUNNING -> SecondaryControl(stringResource(R.string.focus_pause), FocusGlyphKind.Pause, onPause, slot)
            FocusTimerState.Run.PAUSED -> SecondaryControl(stringResource(R.string.focus_resume), FocusGlyphKind.Play, onResume, slot)
            else -> Unit
        }
    }
    val primary: @Composable (Modifier) -> Unit = { slot ->
        ViraActionButton(stringResource(R.string.focus_finish), onFinish, slot, area = ViraArea.Focus)
    }
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            secondary(Modifier.fillMaxWidth())
            primary(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            secondary(Modifier.weight(1f))
            primary(Modifier.weight(1f))
        }
    }
}

/// Superfície **opaca** i blanca amb vora verda: pausar és el que més es fa mentre la sessió corre,
/// i no es pot perdre. La jerarquia amb el primari la porta el pes de la superfície, no el color.
@Composable
private fun SecondaryControl(text: String, glyph: FocusGlyphKind, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.s)
    val ink = ViraColor.onSurface(ViraArea.Focus)
    Row(
        modifier
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .background(ViraColor.surfaceElevated, shape)
            .border(1.5.dp, ViraColor.accent(ViraArea.Focus).copy(alpha = 0.35f), shape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FocusGlyph(glyph, ink)
        Spacer(Modifier.width(ViraSpacing.xs))
        Text(text, style = ViraFont.headline, color = ink)
    }
}

/// Un botó rodó de només símbol (la creu, el +), sempre amb nom per a TalkBack.
@Composable
fun FocusIconButton(
    glyph: FocusGlyphKind,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: androidx.compose.ui.graphics.Color? = null,
) {
    Box(
        modifier
            .defaultMinSize(ViraTouch.minimum, ViraTouch.minimum)
            .clip(CircleShape)
            .then(if (background != null) Modifier.background(background, CircleShape) else Modifier)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        FocusGlyph(glyph, color, size = 20.dp)
    }
}
