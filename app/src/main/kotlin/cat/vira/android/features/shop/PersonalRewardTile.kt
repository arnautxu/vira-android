package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.progress.RewardsPill
import cat.vira.android.features.progress.RewardsProgressBar

/// Una fitxa d'«Els meus premis» (`PersonalRewardTile`): regal a dalt, nom, preu en píndola i, al
/// peu, l'acció o el que falta.
/// - **Te'l pots donar**: targeta groga amb el botó lila «Bescanviar».
/// - **Encara no**: targeta blanca amb la barra i «620 / 800 · Et falten 180» en text. Cap botó
///   apagat que no diu per què no es pot tocar.
@Composable
fun PersonalRewardTile(reward: PersonalReward, balance: Int, onRedeem: () -> Unit, modifier: Modifier = Modifier) {
    val canAfford = balance >= reward.cost
    val shape = RoundedCornerShape(ViraRadius.l)
    val text = if (canAfford) ViraColor.onCard(ViraArea.Rewards) else ViraColor.textPrimary
    Column(
        modifier.fillMaxWidth()
            .then(if (canAfford) Modifier.background(ViraGradient.card(ViraArea.Rewards), shape) else Modifier.background(ViraColor.surfaceElevated, shape))
            .padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        ViraPictogram(R.drawable.picto_gift, size = 48.dp)
        Text(reward.title, style = ViraFont.headline, color = text)
        if (reward.redeemedCount > 0) {
            Text(
                stringResource(R.string.rewards_personal_redeemed_count, reward.redeemedCount),
                style = ViraFont.footnote, color = if (canAfford) text else ViraColor.textSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        val costLabel = stringResource(R.string.rewards_personal_cost, reward.cost.viraFormatted())
        RewardsPill(reward.cost.viraFormatted(), Modifier.clearAndSetSemantics { contentDescription = costLabel }, showsCoin = true)
        if (canAfford) {
            ViraActionButton(stringResource(R.string.rewards_personal_redeem), onRedeem, Modifier.fillMaxWidth(), area = ViraArea.Today)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                RewardsProgressBar(balance.toFloat() / maxOf(reward.cost, 1))
                Text(
                    "${balance.viraFormatted()} / ${reward.cost.viraFormatted()}",
                    style = ViraFont.footnoteEmphasis.tabular(), color = ViraColor.textSecondary,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Text(
                    stringResource(R.string.rewards_personal_missing, (reward.cost - balance).viraFormatted()),
                    style = ViraFont.footnoteEmphasis, color = ViraColor.textSecondary,
                )
            }
        }
    }
}

/// L'última fitxa de la graella: obre l'editor. Translúcida i amb vora de punts: un lloc per omplir.
@Composable
fun PersonalRewardAddTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = ViraColor.onSurface(ViraArea.Rewards)
    val dash = ViraColor.accent(ViraArea.Rewards).copy(alpha = 0.55f)
    val shape = RoundedCornerShape(ViraRadius.l)
    val label = stringResource(R.string.rewards_personal_new)
    Column(
        modifier.fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable * 2)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.45f), shape)
            .dashedBorder(dash, ViraRadius.l)
            .viraPressable(onClickLabel = label) { onClick() }
            .padding(vertical = ViraSpacing.s)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterVertically),
    ) {
        ShopSymbolIcon(ShopSymbol.Plus, ink, size = 28.dp)
        Text(label, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = ViraSpacing.s))
    }
}

/// Vora de punts arrodonida, dibuixada per sobre del contingut.
internal fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawWithContent {
    drawContent()
    val w = 2.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(w / 2f, w / 2f),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}
