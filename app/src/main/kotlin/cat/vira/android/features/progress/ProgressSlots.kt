package cat.vira.android.features.progress

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.progress.ui.ProgressBackButton
import cat.vira.android.features.progress.ui.SlidersMark

/// Les peces de la pestanya Viri que són d'un altre fil: **Botiga i premis** (`features/shop`).
///
/// És l'única cosa d'aquesta carpeta que un altre fil toca: quan la seva peça arriba a `main`,
/// substitueix aquí la línia que li toca per la crida a la seva, i res més. Fins aleshores, cada
/// lloc ensenya el que hi havia abans de la skin (el Viri de sempre, ranures buides) o el
/// marcador de l'esquelet.
internal object ProgressSlots {

    /// La Botiga: `ShopScreen(onBack)`. S'hi arriba des de «Vesteix-lo» i des de la fitxa «Botiga».
    /// Les pantalles de Botiga porten la seva barra amb la fletxa; el marcador, la nostra.
    @Composable
    fun Shop(onBack: () -> Unit) = cat.vira.android.features.shop.ShopScreen(onBack)

    /// Els meus premis: `PersonalRewardsScreen(onBack)`.
    @Composable
    fun PersonalRewards(onBack: () -> Unit) = PendingPage(onBack)

    /// La col·lecció de cartes: `CardsScreen(onBack)`. Només amb `ViraReleaseGate.cards`.
    @Composable
    fun Cards(onBack: () -> Unit) = cat.vira.android.features.shop.CardsScreen(onBack)

    /// El Viri amb la skin posada, omplint el marc: `EquippedSkinPortrait(modifier)`, amb
    /// `EquippedAccessory` a dalt a la dreta. Sense skin, el Viri de sempre.
    @Composable
    fun Portrait(modifier: Modifier) {
        Image(
            painter = painterResource(R.drawable.viri_entry_waving),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }

    /// El prestatge de cartes penjades, un sol botó: `PinnedBadgesShelf(onCustomize, modifier, ink)`.
    @Composable
    fun Shelf(onCustomize: () -> Unit, ink: Color, modifier: Modifier) = EmptyShelf(onCustomize, ink, modifier)

    /// La fulla de personalitzar el perfil: `ProfileCustomizeSheet(onDismiss)`. Mentre no hi és,
    /// es tanca sola.
    @Composable
    fun Customize(onDismiss: () -> Unit) {
        LaunchedEffect(Unit) { onDismiss() }
    }
}

@Composable
private fun PendingPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        ProgressBackButton(onBack, Modifier.padding(horizontal = ViraSpacing.xs))
        Box(Modifier.weight(1f)) { PendingScreen() }
    }
}

/// El prestatge abans de tenir cartes (`ProfileHeaderView.shelf`): quatre ranures buides i el botó
/// de personalitzar, tot un sol botó perquè quatre objectius petits serien un trencaclosques per a
/// TalkBack i per al dit.
@Composable
internal fun EmptyShelf(onCustomize: () -> Unit, ink: Color, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.profile_customize)
    val value = stringResource(R.string.profile_badges_empty)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ViraTouch.minimum)
            .clip(RoundedCornerShape(ViraRadius.m))
            .background(ViraColor.surfaceElevated.copy(alpha = 0.45f))
            .viraPressable(onClick = onCustomize)
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = value
                role = Role.Button
                onClick { onCustomize(); true }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        repeat(BADGE_SLOTS) { BadgeSlot(40.dp) }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(40.dp).background(ViraColor.surfaceElevated.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SlidersMark(ink, 18.dp)
        }
    }
}

/// Quantes cartes caben al prestatge (`ViraBadges.slots`).
private const val BADGE_SLOTS = 4

/// Una ranura buida del prestatge (`BadgeSlot`): un «+» dins d'un cercle de traç discontinu, que
/// ensenya que es pot omplir.
@Composable
internal fun BadgeSlot(size: Dp, modifier: Modifier = Modifier) {
    val ink = ViraColor.onSurface(ViraArea.Rewards)
    val fill = ViraColor.surface(ViraArea.Rewards)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(fill, radius = r)
        val stroke = 1.5.dp.toPx()
        drawCircle(
            ink.copy(alpha = 0.4f),
            radius = r - stroke / 2,
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
        val arm = r * 0.3f
        val c = center
        drawLine(ink, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(ink, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}
