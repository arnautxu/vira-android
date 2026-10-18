package cat.vira.android.features.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import kotlin.math.roundToInt

/// La targeta d'una carta (`CardFace`): sempre un rectangle vertical 1:1,5 amb vora, tingui
/// il·lustració pròpia o no. Sense art, el pictograma va a dins com a motiu provisional: el que
/// falta és la il·lustració, no la carta. La comparteixen el moment en què es guanya, la col·lecció
/// a pantalla completa i la graella, perquè la carta es vegi igual a totes tres.
///
/// L'ample creix amb la lletra (com el `@ScaledMetric` de l'iPhone), sense passar de l'espai que hi
/// ha. Amb un modificador de mesura i no `BoxWithConstraints`, perquè la graella iguala alçades
/// amb mesures intrínseques. El gruix de la vora el porta la raresa: sobreviu al blanc i negre.
@Composable
fun CardFace(card: CardItem, modifier: Modifier = Modifier, baseWidth: Dp = 196.dp) {
    val target = baseWidth * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val shape = RoundedCornerShape(ViraRadius.l)
    val edge = ViraColor.onCard(ViraArea.Rewards)
    val border = card.rarity.borderWidth.dp
    Box(
        modifier
            .clearAndSetSemantics { }
            .layout { measurable, constraints ->
                var w = minOf(target.roundToPx(), constraints.maxWidth)
                if (constraints.hasBoundedHeight) w = minOf(w, (constraints.maxHeight / ASPECT).toInt())
                val h = (w * ASPECT).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(w, h) { placeable.place(0, 0) }
            }
            .shadow(target * 0.06f, shape, ambientColor = edge.copy(alpha = 0.28f), spotColor = edge.copy(alpha = 0.28f))
            .clip(shape)
            .background(ViraGradient.card(ViraArea.Rewards)),
        contentAlignment = Alignment.Center,
    ) {
        val art = card.art
        if (art != null) {
            Image(painterResource(art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(card.pictogram), null, Modifier.fillMaxSize(0.56f), contentScale = ContentScale.Fit)
        }
        // La vora va per sobre de la il·lustració.
        Box(Modifier.fillMaxSize().border(border, edge.copy(alpha = 0.35f), shape))
    }
}

/// Proporció de la targeta, igual per a totes les cartes.
private const val ASPECT = 1.5f
