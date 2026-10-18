package cat.vira.android.features.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius

/// La targeta d'una carta (`CardFace`): sempre un rectangle vertical 1:1,5 amb vora, tingui
/// il·lustració pròpia o no. Sense art, el pictograma va a dins com a motiu provisional: el que
/// falta és la il·lustració, no la carta. La comparteixen el moment en què es guanya, la col·lecció
/// a pantalla completa i la graella, perquè la carta es vegi igual a totes tres.
///
/// L'ample creix amb la lletra (com el `@ScaledMetric` de l'iPhone), sense passar de l'espai que hi
/// ha. El gruix de la vora el porta la raresa: és la part que sobreviu al blanc i negre.
@Composable
fun CardFace(card: CardItem, modifier: Modifier = Modifier, baseWidth: Dp = 196.dp) {
    val scale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    BoxWithConstraints(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        val width = minOf(baseWidth * scale, maxWidth, maxHeight / ASPECT)
        val shape = RoundedCornerShape(ViraRadius.l)
        val edge = ViraColor.onCard(ViraArea.Rewards)
        Box(
            Modifier.size(width, width * ASPECT)
                .shadow(width * 0.06f, shape, ambientColor = edge.copy(alpha = 0.28f), spotColor = edge.copy(alpha = 0.28f))
                .clip(shape)
                .background(ViraGradient.card(ViraArea.Rewards))
                .border(card.rarity.borderWidth.dp, edge.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center,
        ) {
            val art = card.art
            if (art != null) {
                Image(painterResource(art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // La vora va per sobre de la il·lustració, no a sota.
                Box(Modifier.fillMaxSize().border(card.rarity.borderWidth.dp, edge.copy(alpha = 0.35f), shape))
            } else {
                ViraPictogram(card.pictogram, size = width * 0.56f, isScene = true)
            }
        }
    }
}

/// Proporció de la targeta, igual per a totes les cartes.
private const val ASPECT = 1.5f
