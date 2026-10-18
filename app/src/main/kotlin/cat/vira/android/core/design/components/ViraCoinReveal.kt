package cat.vira.android.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/// El guany dins de la fila que l'acaba de revelar (`ViraCoinReveal` de l'iPhone, 2026-10-18): la
/// ViriCoin surt gran i girant amb raigs, el «+3» compta fins al que has guanyat i l'XP entra al
/// costat.
///
/// La moneda es dibuixa més gran del que ocupa, i els raigs i la llum van per sota sense ocupar
/// res: la fila no pot créixer mentre revela, o tot el que hi ha a sota saltaria.
///
/// Amb Reduir moviment no gira ni compta: entra sencera amb el fos de `ViraInlineCompletion`.
/// El vol de les sis monedes cap a la capçalera (`ViraCoinFlight`) és de l'arrel de l'app, no
/// d'aquí.
@Composable
fun ViraCoinReveal(coins: Int, xp: Int, foreground: Color, modifier: Modifier = Modifier) {
    val environment = LocalViraEnvironment.current
    val animates = !environment.reduceMotion
    var t by remember { mutableFloatStateOf(if (animates) 0f else SETTLE_TIME) }
    if (animates) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (t < SETTLE_TIME) {
                withFrameNanos { now -> t = min(SETTLE_TIME, (now - start) / 1e9f) }
            }
        }
    }
    val description = viraRewardText(coins, xp)
    val content: @Composable () -> Unit = {
        if (coins > 0) CoinAndAmount(coins, foreground) { if (animates) t else SETTLE_TIME }
        if (xp > 0) XpChip(xp, foreground, animated = coins > 0) { if (animates) t else SETTLE_TIME }
    }
    val semantics = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }
    if (environment.isAccessibilitySize) {
        Column(semantics, Arrangement.spacedBy(ViraSpacing.xxs), Alignment.CenterHorizontally) { content() }
    } else {
        Row(semantics, Arrangement.spacedBy(ViraSpacing.s, Alignment.CenterHorizontally), Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun CoinAndAmount(coins: Int, foreground: Color, time: () -> Float) {
    val coinSize = 24.dp * LocalDensity.current.fontScale
    val veil = ViraColor.lightArtworkVeil
    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(coinSize).drawBehind {
                val t = time()
                val cs = coinSize.toPx()
                val glow = 1 - progress(t, 0.2f, 0.9f)
                if (glow > 0f) {
                    drawCircle(
                        Brush.radialGradient(listOf(veil.copy(alpha = 0.7f * glow), Color.Transparent), center, cs * 1.6f),
                        radius = cs * 1.6f,
                    )
                }
                val rays = easeOut(progress(t, 0f, 0.2f)) * (1 - progress(t, 0.7f, 0.5f))
                if (rays > 0f) {
                    val length = cs * 1.1f
                    val width = 3.dp.toPx()
                    // Els raigs s'apaguen cap a fora: la màscara radial de l'iPhone, aquí com a
                    // degradat de cada raig.
                    val brush = Brush.radialGradient(
                        0f to foreground.copy(alpha = 0.35f * rays),
                        0.25f to foreground.copy(alpha = 0.35f * rays),
                        1f to Color.Transparent,
                        center = center,
                        radius = cs * 2.4f,
                    )
                    scale(0.4f + 0.7f * easeOut(progress(t, 0f, 0.35f)), center) {
                        rotate(t * 70f, center) {
                            repeat(12) { index ->
                                rotate(index * 30f, center) {
                                    drawRoundRect(
                                        brush,
                                        topLeft = Offset(center.x - width / 2, center.y - cs * 0.75f - length),
                                        size = Size(width, length),
                                        cornerRadius = CornerRadius(width / 2),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) {
            ViraPictogram(
                ViraPictogramName.viriCoin,
                size = coinSize,
                modifier = Modifier.graphicsLayer {
                    val t = time()
                    val pop = backOut(progress(t, 0f, 0.32f)) * (1 + 0.12f * bump(t, 0.3f, 0.2f))
                    val spin = easeOut(progress(t, 0f, 0.5f)) * 720f
                    scaleX = max(cos(spin * PI.toFloat() / 180f), 0.02f) * pop * DRAWN_SCALE
                    scaleY = pop * DRAWN_SCALE
                },
            )
        }
        val t = time()
        val counted = (coins * easeOut(progress(t, 0.05f, 0.3f))).roundToInt()
        Text(
            stringResource(R.string.reward_coins_short, max(1, min(coins, counted))),
            style = ViraFont.headline.copy(fontFeatureSettings = "tnum"),
            color = foreground,
            modifier = Modifier.graphicsLayer {
                val s = 1 + 0.3f * bump(time(), 0.35f, 0.3f)
                scaleX = s
                scaleY = s
            },
        )
    }
}

@Composable
private fun XpChip(xp: Int, foreground: Color, animated: Boolean, time: () -> Float) {
    val entry = if (animated) progress(time(), 0.2f, 0.25f) else 1f
    Row(
        Modifier
            .offset(x = 10.dp * (1 - easeOut(entry)))
            .graphicsLayer { alpha = entry }
            .background(foreground.copy(alpha = 0.12f), CircleShape)
            .padding(top = ViraSpacing.xxs, bottom = ViraSpacing.xxs, start = ViraSpacing.xxs, end = ViraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViraPictogram(ViraPictogramName.xp, size = 20.dp)
        Text(
            stringResource(R.string.toast_reward_xp, xp),
            style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum"),
            color = foreground,
        )
    }
}

private const val DRAWN_SCALE = 1.5f
/// Quan ja no es mou res i el rellotge pot parar.
private const val SETTLE_TIME = 1.6f

// Les corbes de `ViraCoinMotion` de l'iPhone.
private fun progress(t: Float, from: Float, over: Float): Float = ((t - from) / over).coerceIn(0f, 1f)
private fun easeOut(x: Float): Float = 1 - (1 - x).pow(3)
private fun backOut(x: Float): Float {
    val c = 1.9f
    return 1 + (c + 1) * (x - 1).pow(3) + c * (x - 1).pow(2)
}
private fun bump(t: Float, at: Float, duration: Float): Float {
    if (t < at || t > at + duration) return 0f
    val x = (t - at) / duration
    return sin(PI.toFloat() * x) * exp(-3 * x)
}
