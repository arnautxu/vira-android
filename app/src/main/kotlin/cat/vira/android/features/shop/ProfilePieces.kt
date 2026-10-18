package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

// Les peces de la Botiga que la capçalera del Perfil (fil «Viri i ratxa») fa servir sense haver de
// llegir la base de dades ella mateixa: retrat, accessori, prestatge i la fulla de personalitzar.

/// El retrat de la skin posada, llegit de la base de dades. Sense skin, el Viri de sempre.
@Composable
fun EquippedSkinPortrait(modifier: Modifier = Modifier) {
    val skins by rememberShopData().skins.collectAsStateWithLifecycle(emptyList())
    SkinPortrait(skins.firstOrNull { it.isEquipped }?.let { SkinItem.item(it.key) }, modifier)
}

/// L'avatar rodó de la skin posada, per a `ViraTopBar`.
@Composable
fun EquippedSkinAvatar(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val skins by rememberShopData().skins.collectAsStateWithLifecycle(emptyList())
    SkinAvatar(skins.firstOrNull { it.isEquipped }?.let { SkinItem.item(it.key) }, modifier, size)
}

/// L'accessori posat, en un cercle al racó del retrat (`ProfileHeaderView`). No dibuixa res si no
/// n'hi ha cap. Decoratiu: no s'anuncia.
@Composable
fun EquippedAccessory(modifier: Modifier = Modifier) {
    val owned by rememberShopData().accessories.collectAsStateWithLifecycle(emptyList())
    val item = owned.firstOrNull { it.isEquipped }?.let { AccessoryItem.item(it.key) } ?: return
    Box(modifier.padding(ViraSpacing.s).background(ViraColor.surfaceElevated, CircleShape).padding(ViraSpacing.xs)) {
        ViraPictogram(item.art, size = 56.dp)
    }
}

/// El prestatge de badges penjats (`ProfileHeaderView.shelf`): **un sol botó** que obre
/// [ProfileCustomizeSheet]. Quatre objectius petits amb accions diferents serien un trencaclosques
/// per a TalkBack i per al dit. Les ranures buides ensenyen que es pot omplir.
@Composable
fun PinnedBadgesShelf(onCustomize: () -> Unit, modifier: Modifier = Modifier, ink: Color = ViraColor.onSurface(ViraArea.Rewards)) {
    val profile by rememberShopData().profile.collectAsStateWithLifecycle(null)
    PinnedBadgesShelfContent(profile?.pinnedBadgeKeys.orEmpty(), onCustomize, modifier, ink)
}

/// El prestatge sense estat, per a les captures.
@Composable
fun PinnedBadgesShelfContent(pinnedKeys: List<String>, onCustomize: () -> Unit, modifier: Modifier = Modifier, ink: Color = ViraColor.onSurface(ViraArea.Rewards)) {
    val pinned = pinnedKeys.mapNotNull { BadgeItem.item(it) }
    val label = stringResource(R.string.profile_customize)
    val names = pinned.map { stringResource(it.nameRes) }
    val state = if (names.isEmpty()) stringResource(R.string.profile_badges_empty) else names.joinToString(", ")
    Row(
        modifier.fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.45f), RoundedCornerShape(ViraRadius.m))
            .viraPressable(onClick = onCustomize)
            .clearAndSetSemantics { contentDescription = label; stateDescription = state }
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        pinned.forEach { BadgeChip(it, size = 40.dp) }
        repeat((ViraBadges.SLOTS - pinned.size).coerceAtLeast(0)) { BadgeSlot(size = 40.dp) }
        Spacer(Modifier.weight(1f).width(ViraSpacing.xs))
        Box(Modifier.size(40.dp).background(ViraColor.surfaceElevated.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            SlidersGlyph(ink)
        }
    }
}

/// El glif de «personalitzar» (`slider.horizontal.3`): tres guies amb un pom cadascuna.
@Composable
private fun SlidersGlyph(ink: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val stroke = 1.6.dp.toPx()
        listOf(0.2f to 0.65f, 0.5f to 0.3f, 0.8f to 0.55f).forEach { (y, knob) ->
            drawLine(ink, androidx.compose.ui.geometry.Offset(0f, w * y), androidx.compose.ui.geometry.Offset(w, w * y), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(ink, radius = w * 0.11f, center = androidx.compose.ui.geometry.Offset(w * knob, w * y))
        }
    }
}
