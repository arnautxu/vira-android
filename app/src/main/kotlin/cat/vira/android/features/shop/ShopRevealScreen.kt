package cat.vira.android.features.shop

import cat.vira.android.core.sound.ViraSound
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraCelebrationScreen
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius

/// La revelació en comprar una skin (`ShopRevealView`): la peça sobre una ràfega, amb el nom i la
/// raresa, a la pantalla de celebració de sempre amb el color de la raresa. La skin ja és posada en
/// sortir d'aquí: el botó només tanca. Cap «obre'n un altre»: és una celebració, no una tirada.
@Composable
fun ShopRevealScreen(item: SkinItem, onContinue: () -> Unit, celebrationsEnabled: Boolean = true) {
    ViraCelebrationScreen(
        caption = stringResource(R.string.shop_reveal_title),
        onContinue = onContinue,
        area = item.tier.shopArea,
        eyebrow = stringResource(item.tier.nameRes),
        message = stringResource(R.string.shop_reveal_message, stringResource(item.nameRes)),
        celebrationsEnabled = celebrationsEnabled,
        extra = { ShopViriLine(stringResource(item.tier.revealLineRes)) },
        sound = item.tier.revealSound,
    ) { RevealHero(item) }
}

/// La frase de Viri en sortir del vestidor: primera persona, ≤15 paraules, cap pressa.
val SkinTier.revealLineRes: Int
    get() = when (this) {
        SkinTier.Common -> R.string.shop_reveal_viri_common
        SkinTier.Special -> R.string.shop_reveal_viri_special
        SkinTier.Epic -> R.string.shop_reveal_viri_epic
        SkinTier.Legendary -> R.string.shop_reveal_viri_legendary
    }

/// El que diu Viri, en una bafarada. És l'única aparició expressiva de la pantalla.
@Composable
private fun ShopViriLine(text: String) {
    Text(
        text, style = ViraFont.callout, color = ViraColor.textPrimary, textAlign = TextAlign.Center,
        modifier = Modifier.background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
    )
}

/// Cada raresa té el seu so, de més curt i senzill a més ple. Mai sona a màquina escurabutxaques.
private val SkinTier.revealSound: ViraSound.Cue
    get() = when (this) {
        SkinTier.Common -> ViraSound.Cue.SHOP_COMMON
        SkinTier.Special -> ViraSound.Cue.SHOP_SPECIAL
        SkinTier.Epic -> ViraSound.Cue.SHOP_EPIC
        SkinTier.Legendary -> ViraSound.Cue.SHOP_LEGENDARY
    }

@Composable
private fun RevealHero(item: SkinItem) {
    val area = item.tier.shopArea
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    // Amb topall: a lletra gran la peça no pot empènyer el text i el botó fora de la pantalla.
    val width = (220.dp * LocalDensity.current.fontScale).coerceAtMost(300.dp)
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val angle by animateFloatAsState(
        targetValue = if (appeared && !reduceMotion) 20f else 0f,
        animationSpec = tween(6_000, easing = LinearEasing),
        label = "rays",
    )
    val rays = ViraColor.onCard(area).copy(alpha = 0.14f)
    Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        ShopRays(rays, Modifier.size(width * 2.2f).rotate(angle))
        if (item.tier == SkinTier.Legendary) {
            // L'anell daurat només el porten les llegendàries: és el graó que més brilla.
            Box(Modifier.size(width * 1.3f).border(3.dp, ViraColor.onCard(area).copy(alpha = 0.35f), CircleShape))
        }
        Box(Modifier.size(width).shopFrame(area, ViraColor.onCard(area), radius = ViraRadius.dramatic)) {
            SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize())
        }
    }
}

/// Ràfega de raigs, la de les botigues de joc. Decorativa.
@Composable
private fun ShopRays(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = maxOf(size.width, size.height) / 2f
        val count = 16
        val step = 360f / count
        val fade = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = minOf(radius, 260.dp.toPx()))
        val wedges = Path()
        for (i in 0 until count) {
            wedges.moveTo(center.x, center.y)
            wedges.arcTo(
                androidx.compose.ui.geometry.Rect(center, radius),
                startAngleDegrees = i * step, sweepAngleDegrees = step * 0.45f, forceMoveTo = false,
            )
            wedges.close()
        }
        clipPath(wedges) { drawRect(fade, size = Size(size.width, size.height)) }
    }
}
