package cat.vira.android.features.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// L'escenari de la Botiga (`ShopStage`, Arnau, 2026-10-21: «la botiga hauria de ser una experiència
/// de gamificació, visualment espectacular»).
///
/// Viri al mig, entre cortines i amb un focus a sobre. A dalt de la Botiga hi surt amb la skin que
/// porta; al provador (`ShopFittingRoom`) s'hi posa la que has tocat, abans de comprar-la.
///
/// **Quin Viri es dibuixa.** Fins que les skins vestides arribin amb Google Play, Android no té el
/// Viri retallat de cada skin (el paquet de l'iPhone). Amb el Viri de sempre, surt ell sobre un
/// pedestal; amb una skin, l'escena de la Botiga a sang entre les cortines, com a l'iPhone quan el
/// paquet encara no hi és: mai un forat ni el Viri de sempre fent veure que porta una skin.
///
/// Decoratiu: la informació (nom, raresa, preu) és fora, en text. A mides d'accessibilitat les
/// etiquetes van a sota de l'escenari i Viri no hi diu res: amb la lletra gran tapaven la cara.
@Composable
fun ShopStage(
    item: SkinItem?,
    modifier: Modifier = Modifier,
    isTryingOn: Boolean = false,
    height: Dp = 300.dp,
    /// El que diu Viri a dalt de l'escenari, si diu res. Primera persona i ≤15 paraules.
    line: String? = null,
) {
    val environment = LocalViraEnvironment.current
    val large = environment.isAccessibilitySize
    val spoken = if (item == null) stringResource(R.string.shop_default) else {
        val wearing = stringResource(R.string.shop_stage_a11y, stringResource(item.nameRes))
        if (isTryingOn) "$wearing, ${stringResource(R.string.shop_stage_trying)}" else wearing
    }
    // Cada skin entra com una peça nova: es veu canviar, no saltar.
    AnimatedContent(
        targetState = item,
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        transitionSpec = {
            if (environment.reduceMotion) fadeIn() togetherWith fadeOut()
            else (scaleIn(initialScale = 0.92f) + fadeIn()) togetherWith fadeOut()
        },
        contentKey = { it?.key ?: "default" },
        label = "stage",
    ) { shown ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Stage(shown, isTryingOn, height, line.takeIf { !large }, showsLabels = !large)
            if (large) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                    LabelChips(shown, isTryingOn)
                }
            }
        }
    }
}

@Composable
private fun Stage(item: SkinItem?, isTryingOn: Boolean, height: Dp, line: String?, showsLabels: Boolean) {
    val area = ViraArea.Today
    val shape = RoundedCornerShape(ViraRadius.dramatic * 0.6f)
    val veil = ViraColor.lightArtworkVeil
    Box(Modifier.fillMaxWidth().height(height).clip(shape).background(ViraGradient.card(area))) {
        // El focus de dalt.
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(veil.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width / 2, 0f), radius = height.toPx() * 0.9f,
                ),
            )
        }
        if (item == null) {
            val pedestal = ViraColor.onCard(area).copy(alpha = 0.22f)
            Canvas(
                Modifier.align(Alignment.BottomCenter).padding(bottom = height * 0.06f)
                    .width(height * 0.62f).height(height * 0.1f),
            ) { drawOval(pedestal) }
            Image(
                painterResource(R.drawable.viri_hero), null,
                Modifier.align(Alignment.BottomCenter).padding(bottom = height * 0.1f).height(height * 0.74f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                Modifier.matchParentSize().padding(horizontal = CURTAIN_WIDTH * 0.6f).clipToBounds(),
            ) { SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize()) }
        }
        Curtains(area)
        if (showsLabels) {
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = ViraSpacing.xl + ViraSpacing.xxs)
                    .padding(horizontal = ViraSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                    LabelChips(item, isTryingOn)
                }
            }
            if (line != null) Bubble(line, height, Modifier.align(Alignment.TopEnd))
        }
    }
}

/// La raresa i «El teu Viri». Al provador només hi va la raresa (Arnau, 2026-10-22: «Treu
/// l'etiqueta» de «Provant»); TalkBack encara diu que te l'estàs emprovant.
@Composable
private fun LabelChips(item: SkinItem?, isTryingOn: Boolean) {
    if (item != null) ShopRibbon(stringResource(item.tier.nameRes), item.tier.symbol, item.tier.shopArea)
    if (!isTryingOn) {
        Text(
            stringResource(R.string.shop_stage_you), style = ViraFont.caption, color = ViraColor.textPrimary,
            maxLines = 1, softWrap = false,
            modifier = Modifier.background(ViraColor.surfaceElevated, CircleShape)
                .padding(horizontal = ViraSpacing.xs, vertical = 2.dp),
        )
    }
}

/// La bafarada de Viri, a dalt a la dreta, a tocar del cap.
@Composable
private fun Bubble(text: String, height: Dp, modifier: Modifier) {
    Text(
        text, style = ViraFont.footnote, color = ViraColor.textPrimary, textAlign = TextAlign.Center,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(end = CURTAIN_WIDTH + ViraSpacing.xxs, top = height * 0.22f)
            .widthIn(max = height * 0.36f)
            .shadow(6.dp, RoundedCornerShape(ViraRadius.s), clip = false, ambientColor = Color.Black.copy(alpha = 0.12f))
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.s))
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs),
    )
}

/// Les dues cortines de vellut i el volant de dalt.
@Composable
private fun Curtains(area: ViraArea) {
    val fill = ViraColor.cardFill(area)
    val fold = ViraColor.onCard(area).copy(alpha = 0.16f)
    Box(Modifier.fillMaxSize()) {
        ShopCurtain(fill, fold, Modifier.align(Alignment.CenterStart).width(CURTAIN_WIDTH).fillMaxHeight())
        ShopCurtain(fill, fold, Modifier.align(Alignment.CenterEnd).width(CURTAIN_WIDTH).fillMaxHeight().scale(scaleX = -1f, scaleY = 1f))
        Canvas(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter)) {
            val scallop = 32.dp.toPx()
            drawRect(fill, size = Size(size.width, size.height * 0.45f))
            var x = 0f
            while (x < size.width) {
                drawOval(fill, topLeft = Offset(x, 0f), size = Size(scallop, size.height))
                x += scallop
            }
        }
    }
}

/// Una cortina: plecs verticals del color de l'àrea, amb la cantonada de baix cap a dins arrodonida.
@Composable
private fun ShopCurtain(fill: Color, fold: Color, modifier: Modifier) {
    Canvas(modifier) {
        val shape = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset.Zero, size),
                    topLeft = CornerRadius.Zero, topRight = CornerRadius.Zero,
                    bottomRight = CornerRadius(size.width, size.width), bottomLeft = CornerRadius.Zero,
                ),
            )
        }
        clipPath(shape) {
            drawRect(fill)
            val step = size.width / 3
            for (index in 0 until 3) {
                drawRect(fold, topLeft = Offset(index * step + step * 0.55f, 0f), size = Size(step * 0.35f, size.height))
            }
        }
    }
}

/// Vellut a pantalla completa (`ShopVelvet`): plecs verticals del color de la raresa. És la cortina
/// del vestidor (`ShopDressingRoom`).
@Composable
fun ShopVelvet(area: ViraArea, modifier: Modifier = Modifier) {
    val fill = ViraColor.cardFill(area)
    val fold = ViraColor.onCard(area).copy(alpha = 0.18f)
    Canvas(modifier) {
        drawRect(fill)
        val step = 44.dp.toPx()
        var x = step * 0.6f
        while (x < size.width) {
            drawRect(fold, topLeft = Offset(x, 0f), size = Size(step * 0.32f, size.height))
            x += step
        }
    }
}

private val CURTAIN_WIDTH = 40.dp

/// Mida per defecte de l'escenari: a dalt de la Botiga i al provador.
internal fun shopStageHeight(isAccessibilitySize: Boolean, inFittingRoom: Boolean): Dp = when {
    inFittingRoom -> if (isAccessibilitySize) 220.dp else 320.dp
    else -> if (isAccessibilitySize) 200.dp else 280.dp
}
