package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Mòdul protagonista de Reptes i Els meus premis (`RewardsSceneCard`, `DESIGN_RULES.md` §3 i §5):
/// escena de Viri a dalt, la bombolla que en surt i, a sota, el resum sobre la targeta de l'àrea. El
/// text va a la bombolla o a la targeta, mai sobre la il·lustració.
///
/// El contingut rep la tinta `onCard(area)` com a estil de text per defecte.
@Composable
fun RewardsSceneCard(
    @DrawableRes scene: Int,
    message: String,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Rewards,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    // Prou alta perquè Viri es vegi sencer; a mides d'accessibilitat cedeix lloc al text.
    val sceneHeight = if (LocalViraEnvironment.current.isAccessibilitySize) 120.dp else 184.dp
    val glow = ViraColor.accent(area).copy(alpha = 0.18f)
    Column(
        modifier.fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(ViraGradient.card(area), shape),
    ) {
        Image(
            painterResource(scene), null,
            Modifier.fillMaxWidth().height(sceneHeight).clearAndSetSemantics { },
            contentScale = ContentScale.Crop,
        )
        Column(
            Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.m, bottom = ViraSpacing.m)
                // La bombolla trepitja la vora de l'escena: es llegeix com que surt de Viri.
                .pullUp(ViraSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Bubble(message, area, Modifier)
            CompositionLocalProvider(LocalTextStyle provides ViraFont.body.copy(color = ViraColor.onCard(area))) {
                content()
            }
        }
    }
}

/// Puja el contingut i encongeix l'espai que ocupa en la mateixa mida (el `padding` negatiu de l'iPhone).
internal fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}

@Composable
private fun Bubble(message: String, area: ViraArea, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val glow = ViraColor.accent(area).copy(alpha = 0.16f)
    val spoken = stringResource(R.string.today_viri_a11y, message)
    Text(
        message, style = ViraFont.headline, color = ViraColor.textPrimary,
        modifier = modifier.fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = glow, spotColor = glow)
            .background(ViraColor.surfaceElevated, shape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
            .semantics { contentDescription = spoken },
    )
}

/// Píndola blanca de la targeta (`RewardsPill`): sempre amb text, mai només color.
@Composable
fun RewardsPill(text: String, modifier: Modifier = Modifier, showsCoin: Boolean = false) {
    Row(
        modifier.background(ViraColor.surfaceElevated, CircleShape).padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (showsCoin) ViraPictogram(R.drawable.picto_shop, size = 18.dp, scalesWithText = true)
        Text(text, style = ViraFont.footnoteEmphasis.tabular(), color = ViraColor.textPrimary)
    }
}

/// Barra de progrés amb el compte sempre en text al costat o a sota (`RewardsProgressBar`).
@Composable
fun RewardsProgressBar(fraction: Float, modifier: Modifier = Modifier, onCard: Boolean = false) {
    val area = ViraArea.Rewards
    Box(
        modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            .background(if (onCard) ViraColor.onCard(area).copy(alpha = 0.18f) else ViraColor.surface(area))
            .clearAndSetSemantics { },
    ) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier.fillMaxWidth(f).height(8.dp).clip(CircleShape).then(
                    if (onCard) Modifier.background(ViraColor.onCard(area))
                    else Modifier.background(ViraGradient.accent(area, ViraGradient.Axis.Horizontal)),
                ),
            )
        }
    }
}
