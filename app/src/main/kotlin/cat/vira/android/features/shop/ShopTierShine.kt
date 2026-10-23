package cat.vira.android.features.shop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius

/// Com brilla una fitxa de la Botiga segons la raresa (`ShopTierShine`, Arnau, 2026-10-21:
/// «visualment espectacular»). Les vitrines d'un joc: com més rara, més llum.
///
/// - Comuna: res, la fitxa de sempre.
/// - Especial: dues espurnes quietes a la cantonada.
/// - Èpica: un reflex que travessa la fitxa de tant en tant.
/// - Llegendària: una vora daurada que respira.
///
/// És decoració: la raresa ja la diuen el símbol i el nom de la secció, mai només aquesta llum. Amb
/// Reduir moviment, el reflex desapareix i la vora es queda quieta.
fun Modifier.shopTierShine(tier: SkinTier?): Modifier = composed {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val shape = RoundedCornerShape(ViraRadius.l)
    when (tier) {
        SkinTier.Special -> {
            val spark = ViraColor.lightArtworkVeil
            drawWithContent {
                drawContent()
                val small = 10.dp.toPx()
                val big = 16.dp.toPx()
                val pad = 8.dp.toPx()
                drawPath(sparklePath(Offset(size.width - pad - big / 2, pad + big / 2), big / 2), Color.Black.copy(alpha = 0.18f))
                drawPath(sparklePath(Offset(size.width - pad - big / 2, pad + big / 2), big / 2 * 0.9f), spark)
                drawPath(sparklePath(Offset(size.width - pad - big - 2.dp.toPx() - small / 2, pad + small / 2), small / 2), spark)
            }
        }
        SkinTier.Epic -> if (reduceMotion) this else {
            val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(
                initialValue = -1.2f, targetValue = 2.6f,
                animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing), RepeatMode.Restart),
                label = "sweep",
            )
            val light = ViraColor.lightArtworkVeil.copy(alpha = 0.4f)
            drawWithContent {
                drawContent()
                val radius = ViraRadius.l.toPx()
                val outline = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, radius, radius))
                }
                clipPath(outline) {
                    val band = size.width * 0.5f
                    translate(left = sweep * size.width) {
                        rotate(20f, pivot = Offset(band / 2, size.height / 2)) {
                            drawRect(
                                Brush.horizontalGradient(listOf(Color.Transparent, light, Color.Transparent), 0f, band),
                                topLeft = Offset(0f, -size.height), size = androidx.compose.ui.geometry.Size(band, size.height * 3),
                            )
                        }
                    }
                }
            }
        }
        SkinTier.Legendary -> {
            val gold = ViraColor.accent(ViraArea.Rewards)
            val glow = if (reduceMotion) 0.35f else {
                val value by rememberInfiniteTransition(label = "glow").animateFloat(
                    initialValue = 0.35f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
                    label = "glow",
                )
                value
            }
            this.shadow(if (reduceMotion) 4.dp else (4 + 4 * (glow - 0.35f) / 0.35f).dp, shape, clip = false,
                ambientColor = gold.copy(alpha = glow), spotColor = gold.copy(alpha = glow))
                .border(2.dp, gold, shape)
        }
        else -> this
    }
}

private fun sparklePath(center: Offset, radius: Float) = androidx.compose.ui.graphics.Path().apply {
    val k = radius * 0.22f
    moveTo(center.x, center.y - radius)
    quadraticTo(center.x + k, center.y - k, center.x + radius, center.y)
    quadraticTo(center.x + k, center.y + k, center.x, center.y + radius)
    quadraticTo(center.x - k, center.y + k, center.x - radius, center.y)
    quadraticTo(center.x - k, center.y - k, center.x, center.y - radius)
    close()
}
