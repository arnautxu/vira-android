package cat.vira.android.features.entry

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlin.math.roundToInt

/// L'anella blanca amb el resplendor lila, i el número a la cantonada (`ViraTutorialSpotRing`).
///
/// El número de la peça **no creix amb el text**: amb lletra gran tapava peces de 48 dp com el «+».
/// És un duplicat visual; el número que es llegeix és el de la targeta, que sí que creix.
@Composable
fun ViraTutorialSpotRing(spot: ViraTutorialSpot, width: Dp) {
    val environment = LocalViraEnvironment.current
    val density = LocalDensity.current
    val highContrast = environment.increasedContrast
    val ring = ViraColor.lightArtworkVeil
    val glow = ViraColor.brand
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        val rect = spot.rect
        val corner = if (spot.target.isTab) CornerRadius(rect.height / 2f) else CornerRadius(ViraRadius.l.toPx())
        if (!highContrast) {
            // El resplendor: una vora lila ampla i suau per sota de l'anella.
            drawRoundRect(glow.copy(alpha = 0.35f), rect.topLeft, rect.size, corner, style = Stroke(8.dp.toPx()))
        }
        drawRoundRect(ring, rect.topLeft, rect.size, corner, style = Stroke((if (highContrast) 3.dp else 2.dp).toPx()))
    }
    val badge = 24.dp
    val widthPx = with(density) { width.toPx() }
    val badgePx = with(density) { badge.toPx() }
    val inset = with(density) { 4.dp.toPx() }
    ViraTutorialBadge(
        spot.number,
        side = badge,
        scales = false,
        modifier = Modifier.offset {
            IntOffset(
                (minOf(spot.rect.right - inset, widthPx - badgePx / 2f - inset) - badgePx / 2f).roundToInt(),
                (maxOf(spot.rect.top + inset, badgePx / 2f) - badgePx / 2f).roundToInt(),
            )
        },
    )
}

/// El número dins del cercle lila, amb la vora blanca. A la targeta creix amb la lletra; sobre la
/// peça, no (`scales`).
@Composable
fun ViraTutorialBadge(number: Int, side: Dp, scales: Boolean, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    val size = if (scales) minOf(side * fontScale, side * 2) else side
    val style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum").let {
        if (scales) it else it.copy(fontSize = it.fontSize / fontScale)
    }
    Box(
        modifier
            .size(size)
            .background(ViraColor.accent(ViraArea.Today), CircleShape)
            .then(if (scales) Modifier else Modifier.border(2.dp, ViraColor.lightArtworkVeil, CircleShape))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", style = style, color = ViraColor.onAccent, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
    }
}

/// «Toca aquí» a sobre d'una peça que es pot tocar (la pestanya o el cercle de «Prova-ho»): una
/// anella que batega cap enfora, la píndola i el toc. La pestanya batega poc; el cercle, més.
/// Amb Reduir moviment, cap batec. Per a TalkBack no hi és: la targeta ja té «Següent».
@Composable
fun ViraTutorialTapHint(rect: Rect, width: Dp, isCircle: Boolean, onTap: () -> Unit) {
    val environment = LocalViraEnvironment.current
    val density = LocalDensity.current
    val ring = ViraColor.lightArtworkVeil
    val minimum = with(density) { 48.dp.toPx() }
    val touch = Rect(rect.center, maxOf(rect.width, minimum) / 2f).let {
        if (isCircle) it else Rect(rect.left, rect.top, rect.right, rect.bottom)
    }
    // El toc: una caixa invisible a sobre de la peça.
    Box(
        Modifier
            .offset { IntOffset(touch.left.roundToInt(), touch.top.roundToInt()) }
            .size(with(density) { touch.width.toDp() }, with(density) { touch.height.toDp() })
            .clearAndSetSemantics { }
            .pointerInput(onTap) { detectTapGestures { onTap() } },
    )
    if (!environment.reduceMotion) {
        val transition = rememberInfiniteTransition(label = "tutorialPulse")
        // Un batec cap enfora i torna de cop: és una crida, no una respiració.
        val phase by transition.animateFloat(
            0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "tutorialPulsePhase",
        )
        val grow = if (isCircle) 0.6f else 0.18f
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val scale = 1f + grow * phase
            val size = rect.size * scale
            val topLeft = rect.center - androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            drawRoundRect(
                ring.copy(alpha = 0.9f * (1f - phase)),
                topLeft, size, CornerRadius(size.height / 2f),
                style = Stroke(2.dp.toPx()),
            )
        }
    }
    ViraTutorialTapPill(rect, width, gap = if (isCircle) ViraSpacing.s else ViraSpacing.xs)
}

/// La píndola «Toca aquí», penjada per la base just a sobre de la peça. Com el número, creix poc:
/// amb lletra gran tapava la pestanya que assenyala.
@Composable
private fun ViraTutorialTapPill(rect: Rect, width: Dp, gap: Dp) {
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val capped = minOf(fontScale, 1.35f)
    val style = ViraFont.footnoteEmphasis.let { it.copy(fontSize = it.fontSize * capped / fontScale) }
    val widthPx = with(density) { width.toPx() }
    val edge = with(density) { 80.dp.toPx() }
    val gapPx = with(density) { gap.toPx() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val centerX = rect.center.x.coerceIn(edge, widthPx - edge)
                    placeable.place((centerX - placeable.width / 2f).roundToInt(), (rect.top - gapPx - placeable.height).roundToInt())
                }
            }
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs)
            .clearAndSetSemantics { },
    ) {
        EntryGlyphIcon(EntryGlyph.TAP, ViraColor.textPrimary, side = 14.dp, scalesWithText = false)
        Text(stringResource(R.string.tutorial_tap_here), style = style, color = ViraColor.textPrimary, maxLines = 1, softWrap = false)
    }
}
