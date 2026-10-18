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
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.progress.ui.SlidersMark
import cat.vira.android.features.shop.CardsScreen
import cat.vira.android.features.shop.EquippedAccessory
import cat.vira.android.features.shop.EquippedSkinPortrait
import cat.vira.android.features.shop.PersonalRewardsScreen
import cat.vira.android.features.shop.PinnedBadgesShelf
import cat.vira.android.features.shop.ProfileCustomizeSheet
import cat.vira.android.features.shop.ShopScreen

/// Les peces de la pestanya Viri que són d'un altre fil: **Botiga i premis** (`features/shop`).
internal object ProgressSlots {

    /// La Botiga. S'hi arriba des de «Vesteix-lo» i des de la fitxa «Botiga». Porta la seva barra
    /// amb la fletxa.
    @Composable
    fun Shop(onBack: () -> Unit) = ShopScreen(onBack)

    /// Els meus premis.
    @Composable
    fun PersonalRewards(onBack: () -> Unit) = PersonalRewardsScreen(onBack)

    /// La col·lecció de cartes. Només amb `ViraReleaseGate.cards`.
    @Composable
    fun Cards(onBack: () -> Unit) = CardsScreen(onBack)

    /// La fulla de personalitzar el perfil (malnom, skin i badges). És una fulla de baix pròpia.
    @Composable
    fun Customize(onDismiss: () -> Unit) = ProfileCustomizeSheet(onDismiss)
}

/// Les dues peces de la capçalera del perfil que llegeixen la base de dades de Botiga: el retrat
/// (skin i accessori posats) i el prestatge de badges. La capçalera no llegeix res ella mateixa;
/// les captures fan servir [Placeholder], que no necessita base de dades.
internal class ProfileArt(
    val portrait: @Composable (modifier: Modifier) -> Unit,
    val shelf: @Composable (onCustomize: () -> Unit, ink: Color, modifier: Modifier) -> Unit,
) {
    companion object {
        /// El que hi ha a la base de dades: la skin posada omplint el marc, l'accessori al racó de
        /// dalt a la dreta i els badges penjats. Sense skin, el Viri de sempre.
        val Equipped = ProfileArt(
            portrait = { modifier ->
                Box(modifier) {
                    EquippedSkinPortrait(Modifier.fillMaxSize())
                    EquippedAccessory(Modifier.align(Alignment.TopEnd))
                }
            },
            shelf = { onCustomize, ink, modifier -> PinnedBadgesShelf(onCustomize, modifier, ink) },
        )

        /// Sense skin ni badges: el Viri de sempre i quatre ranures buides.
        val Placeholder = ProfileArt(
            portrait = { modifier ->
                Image(
                    painter = painterResource(R.drawable.viri_entry_waving),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier,
                )
            },
            shelf = { onCustomize, ink, modifier -> EmptyShelf(onCustomize, ink, modifier) },
        )
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
