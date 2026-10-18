package cat.vira.android.features.progress

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor

/// Les peces de la pestanya Viri que són d'un altre fil: **Botiga i premis** (`features/shop`).
///
/// És l'única cosa d'aquesta carpeta que un altre fil toca: quan la seva peça arriba a `main`,
/// substitueix aquí la línia que li toca per la crida a la seva, i res més. Fins aleshores, cada
/// lloc ensenya el que hi havia abans de la skin (el Viri de sempre, ranures buides) o el
/// marcador de l'esquelet.
internal object ProgressSlots {

    /// La Botiga (`SkinShopView`). S'hi arriba des de «Vesteix-lo» i des de la fitxa «Botiga».
    @Composable
    fun Shop() = PendingScreen()

    /// Els meus premis (`PersonalRewardsView`).
    @Composable
    fun PersonalRewards() = PendingScreen()

    /// La col·lecció de cartes (`CardsView`). Només a les builds de proves (`ViraReleaseGate.cards`).
    @Composable
    fun Cards() = PendingScreen()

    /// El Viri amb la skin posada, omplint el marc (`SkinPortrait(equipped:)`, que també dibuixa
    /// l'accessori). Sense skin, el Viri de sempre.
    @Composable
    fun Portrait(modifier: Modifier) {
        Image(
            painter = painterResource(R.drawable.viri_entry_waving),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }

    /// Una carta penjada al prestatge del perfil (`BadgeChip(item:size:)`). La clau és de
    /// `ViraProfile.pinnedBadgeKeys`; una clau que el catàleg no conegui no es dibuixa.
    @Composable
    fun PinnedBadge(key: String, size: Dp) = BadgeSlot(size)

    /// La fulla de personalitzar el perfil (`ProfileCustomizeSheet`): cartes i accessoris. Mentre
    /// no hi és, es tanca sola.
    @Composable
    fun Customize(onClose: () -> Unit) {
        LaunchedEffect(Unit) { onClose() }
    }
}

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
