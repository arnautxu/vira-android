package cat.vira.android.features.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec
import java.text.NumberFormat

// Les peces de la Botiga (`ShopGameKit` i `ShopGameControls` de l'iPhone): l'estructura és de joc
// (graella per raresa, botó de preu, barra), l'estètica és la de Vira. Res d'ofertes amb rellotge,
// cofres a l'atzar ni monedes de pagament (`VIRI_AND_GAMIFICATION.md` §2.2.1 i §2.5).

/// Un número amb la separació de milers de l'idioma (`formatted()` de l'iPhone).
fun Int.viraFormatted(): String = NumberFormat.getIntegerInstance().format(this)

/// Els símbols del sistema que fa servir la Botiga, dibuixats: Android no té els SF Symbols.
enum class ShopSymbol { Circle, Sparkles, Diamond, Crown, Star, Check, Plus, Minus, Lock }

/// Un [ShopSymbol] en línia amb el text, a la mida de la lletra. Decoratiu: el nom el porta el text.
@Composable
fun ShopSymbolIcon(symbol: ShopSymbol, color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val side = size * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(modifier.size(side)) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (symbol) {
            ShopSymbol.Circle -> drawCircle(color, radius = w * 0.42f, style = stroke)
            ShopSymbol.Sparkles -> {
                drawPath(sparkle(Offset(w * 0.42f, w * 0.55f), w * 0.4f), color)
                drawPath(sparkle(Offset(w * 0.8f, w * 0.2f), w * 0.18f), color)
            }
            ShopSymbol.Diamond -> drawPath(
                Path().apply {
                    moveTo(w * 0.5f, w * 0.06f); lineTo(w * 0.92f, w * 0.5f)
                    lineTo(w * 0.5f, w * 0.94f); lineTo(w * 0.08f, w * 0.5f); close()
                },
                color,
            )
            ShopSymbol.Crown -> drawPath(
                Path().apply {
                    moveTo(w * 0.08f, w * 0.25f); lineTo(w * 0.3f, w * 0.5f); lineTo(w * 0.5f, w * 0.15f)
                    lineTo(w * 0.7f, w * 0.5f); lineTo(w * 0.92f, w * 0.25f); lineTo(w * 0.82f, w * 0.82f)
                    lineTo(w * 0.18f, w * 0.82f); close()
                },
                color,
            )
            ShopSymbol.Star -> drawPath(star(w), color)
            ShopSymbol.Check -> drawPath(
                Path().apply {
                    moveTo(w * 0.18f, w * 0.52f); lineTo(w * 0.42f, w * 0.74f); lineTo(w * 0.84f, w * 0.28f)
                },
                color,
                style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            ShopSymbol.Plus -> {
                drawLine(color, Offset(w * 0.5f, w * 0.12f), Offset(w * 0.5f, w * 0.88f), w * 0.14f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.12f, w * 0.5f), Offset(w * 0.88f, w * 0.5f), w * 0.14f, StrokeCap.Round)
            }
            ShopSymbol.Minus -> drawLine(color, Offset(w * 0.12f, w * 0.5f), Offset(w * 0.88f, w * 0.5f), w * 0.14f, StrokeCap.Round)
            ShopSymbol.Lock -> {
                drawRoundRect(color, topLeft = Offset(w * 0.18f, w * 0.44f), size = androidx.compose.ui.geometry.Size(w * 0.64f, w * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
                drawArc(color, 180f, 180f, false, topLeft = Offset(w * 0.3f, w * 0.12f), size = androidx.compose.ui.geometry.Size(w * 0.4f, w * 0.6f), style = Stroke(width = w * 0.12f))
            }
        }
    }
}

private fun sparkle(center: Offset, radius: Float) = Path().apply {
    val k = radius * 0.22f
    moveTo(center.x, center.y - radius)
    quadraticTo(center.x + k, center.y - k, center.x + radius, center.y)
    quadraticTo(center.x + k, center.y + k, center.x, center.y + radius)
    quadraticTo(center.x - k, center.y + k, center.x - radius, center.y)
    quadraticTo(center.x - k, center.y - k, center.x, center.y - radius)
    close()
}

private fun star(w: Float) = Path().apply {
    val c = Offset(w / 2f, w * 0.54f)
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) w * 0.48f else w * 0.2f
        val a = Math.toRadians(-90.0 + i * 36.0)
        val p = Offset(c.x + (r * Math.cos(a)).toFloat(), c.y + (r * Math.sin(a)).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/// La forma de totes les fitxes de la Botiga: **la de les targetes de Vira**. Degradat de l'àrea,
/// radi gran i vora fina del to del text. Res de marcs gruixuts ni llavis.
fun Modifier.shopFrame(area: ViraArea, ink: Color, radius: Dp = ViraRadius.l): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(ViraGradient.card(area), shape)
        .border(1.dp, ink.copy(alpha = 0.18f), shape)
}

/// Etiqueta d'una raresa o secció amb el seu símbol. La forta és la píndola de capçalera (degradat
/// i `onCard`); la suau va sobre una targeta, amb la superfície de l'àrea.
@Composable
fun ShopRibbon(title: String, symbol: ShopSymbol, area: ViraArea, modifier: Modifier = Modifier, isStrong: Boolean = false) {
    val shape = RoundedCornerShape(if (isStrong) ViraRadius.l else ViraRadius.s)
    val ink = if (isStrong) ViraColor.onCard(area) else ViraColor.onSurface(area)
    val style = if (isStrong) ViraFont.headline else ViraFont.caption
    Row(
        modifier = modifier
            .then(if (isStrong) Modifier.background(ViraGradient.card(area), shape) else Modifier.background(ViraColor.surface(area), shape))
            .padding(horizontal = if (isStrong) ViraSpacing.m else ViraSpacing.xs, vertical = if (isStrong) ViraSpacing.xs else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ShopSymbolIcon(symbol, ink, size = if (isStrong) 16.dp else 12.dp)
        Text(title, style = style, color = ink)
    }
}

/// Capçalera d'una secció: la píndola i, a la dreta, el que costa o quantes en tens.
@Composable
fun ShopSectionHeader(
    title: String,
    symbol: ShopSymbol,
    area: ViraArea,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShopRibbon(title, symbol, area, isStrong = true)
        Spacer(Modifier.weight(1f).defaultMinSize(minWidth = ViraSpacing.xs))
        trailing()
    }
}

/// El preu d'una peça amb la moneda, per a la dreta d'una capçalera.
@Composable
fun ShopCoinAmount(amount: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
        ViraPictogram(R.drawable.picto_shop, size = 18.dp, scalesWithText = true)
        Text(amount.viraFormatted(), style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary)
    }
}

/// El saldo, a dalt a la dreta (`ShopWallet`): un sol lloc on es diu quant tens. La xifra roda en
/// comptes de saltar.
@Composable
fun ShopWallet(balance: Int, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(balance, viraSpec(ViraMotion.settle()), label = "wallet")
    val spoken = stringResource(R.string.progress_coins, balance.viraFormatted())
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ViraGradient.card(ViraArea.Rewards), CircleShape)
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(R.drawable.picto_shop, size = 24.dp, scalesWithText = true)
        Text(shown.viraFormatted(), style = ViraFont.headline.tabular(), color = ViraColor.onCard(ViraArea.Rewards))
    }
}

/// Dígits d'amplada fixa (`monospacedDigit`).
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/// La cara del botó de preu (`ShopPriceLabel`). No és un botó: tota la fitxa ho és.
sealed interface ShopPriceKind {
    data class Price(val amount: Int) : ShopPriceKind
    data class Missing(val amount: Int) : ShopPriceKind
    data object Equipped : ShopPriceKind
    data object Owned : ShopPriceKind
}

@Composable
fun ShopPriceLabel(kind: ShopPriceKind, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.s)
    val (fill, ink, stroke) = when (kind) {
        is ShopPriceKind.Price -> Triple(ViraColor.accent(ViraArea.Today), ViraColor.onAccent, Color.Transparent)
        is ShopPriceKind.Missing -> Triple(ViraColor.surfaceElevated, ViraColor.textSecondary, ViraColor.separator)
        else -> Triple(ViraColor.surface(ViraArea.Today), ViraColor.onSurface(ViraArea.Today), ViraColor.accent(ViraArea.Today))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(fill, shape)
            .border(if (kind == ShopPriceKind.Equipped) 2.dp else 1.dp, stroke, shape)
            .padding(horizontal = ViraSpacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        val style = ViraFont.callout.tabular()
        when (kind) {
            is ShopPriceKind.Price -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                ViraPictogram(R.drawable.picto_shop, size = 22.dp, scalesWithText = true)
                Text(kind.amount.viraFormatted(), style = style, color = ink, maxLines = 1)
            }
            is ShopPriceKind.Missing -> Text(
                stringResource(R.string.shop_missing_short, kind.amount.viraFormatted()),
                style = style, color = ink, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            ShopPriceKind.Equipped -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                ShopSymbolIcon(ShopSymbol.Check, ink, size = 16.dp)
                Text(stringResource(R.string.shop_equipped), style = style, color = ink, textAlign = TextAlign.Center)
            }
            ShopPriceKind.Owned -> Text(stringResource(R.string.shop_owned), style = style, color = ink, textAlign = TextAlign.Center)
        }
    }
}

/// Barra de progrés (`ShopProgressBar`): carril de la superfície de l'àrea, farciment daurat i la
/// xifra escrita a sobre, mai només la barra.
@Composable
fun ShopProgressBar(value: Int, total: Int, area: ViraArea, label: String, ink: Color, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f
    val track = 10.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Column(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Text(label, style = ViraFont.caption.tabular(), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.fillMaxWidth().height(track).clip(CircleShape)
                .background(ViraColor.surface(area))
                .border(1.dp, ViraColor.onSurface(area).copy(alpha = 0.16f), CircleShape),
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier.fillMaxWidth(fraction).height(track).clip(CircleShape)
                        .background(ViraGradient.card(ViraArea.Rewards, ViraGradient.Axis.Horizontal)),
                )
            }
        }
    }
}

/// Error de desar la compra (`SkinShopSaveError`).
@Composable
fun ShopSaveError(modifier: Modifier = Modifier) {
    cat.vira.android.core.design.components.ViraInlineError(stringResource(R.string.shop_save_error), modifier.fillMaxWidth())
}
