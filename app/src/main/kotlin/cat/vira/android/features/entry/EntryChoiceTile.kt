package cat.vira.android.features.entry

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Fila de tria de la fulla, amb pictograma opcional ("Per on comencem?", edat) (`EntryChoiceTile.swift`).
///
/// Des del 2026-10-21, fitxes grans sobre la fulla blanca, com les maquetes aprovades:
///
/// - **Sense triar**: farciment `backgroundGrouped` i un cercle buit (`controlBorder`, ≥3:1) a la
///   dreta. Amb contrast alt, la fitxa porta a més un contorn `textPrimary`.
/// - **Triada**: farciment `surface(Today)`, contorn `accent(Today)` de 2 dp i el cercle ple amb la
///   marca. La selecció mai depèn només del color: el cercle canvia de forma.
@Composable
fun EntryChoiceTile(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    detail: String? = null,
    @DrawableRes pictogram: Int? = null,
) {
    val environment = LocalViraEnvironment.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(ViraRadius.m)
    val pictogramSide = 40.dp * LocalDensity.current.fontScale.coerceAtMost(2f)

    val surface = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = ViraTouch.comfortable)
        .clip(shape)
        .background(if (isSelected) ViraColor.surface(ViraArea.Today) else ViraColor.backgroundGrouped, shape)
        .then(
            when {
                isSelected -> Modifier.border(2.dp, ViraColor.accent(ViraArea.Today), shape)
                environment.increasedContrast -> Modifier.border(1.5.dp, ViraColor.textPrimary, shape)
                else -> Modifier
            },
        )
        .viraPressable {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
        .semantics(mergeDescendants = true) { selected = isSelected }
        .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)

    val picture: @Composable () -> Unit = {
        if (pictogram != null) {
            Image(painterResource(pictogram), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(pictogramSide))
        }
    }
    val labels: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
            if (detail != null) Text(detail, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
    }
    val check: @Composable () -> Unit = { EntryChoiceMark(isSelected) }

    // Amb lletra d'accessibilitat el pictograma puja a sobre del títol: al costat, es menjava
    // l'amplada i "Organitzar-me" es partia lletra a lletra.
    if (environment.isAccessibilitySize && pictogram != null) {
        Column(surface, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                picture()
                Spacer(Modifier.weight(1f))
                check()
            }
            labels(Modifier.fillMaxWidth())
        }
    } else {
        Row(surface, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            picture()
            labels(Modifier.weight(1f))
            check()
        }
    }
}

/// El cercle de la dreta: buit sense triar, ple amb la marca un cop triat.
@Composable
private fun EntryChoiceMark(isSelected: Boolean) {
    val side = 26.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    if (isSelected) {
        EntryGlyphIcon(EntryGlyph.CHECK_CIRCLE, ViraColor.accent(ViraArea.Today), side = 26.dp)
    } else {
        Box(Modifier.size(side).padding(2.dp).border(2.dp, ViraColor.controlBorder, CircleShape))
    }
}

/// Píndola de tria: l'alternativa compacta a una llista quan les opcions són poques i curtes (els
/// tres suggeriments de la primera tasca). La triada va plena, en negreta i amb ✓.
@Composable
fun EntryChip(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = if (isSelected) ViraColor.onAccent else ViraColor.textPrimary
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier.background(ViraColor.accent(ViraArea.Today), CircleShape)
                else Modifier.border(1.5.dp, ViraColor.controlBorder, CircleShape),
            )
            .viraPressable(onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .padding(horizontal = ViraSpacing.xs),
    ) {
        if (isSelected) EntryGlyphIcon(EntryGlyph.CHECK, ink, side = 14.dp)
        Text(
            title,
            style = ViraFont.callout.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
