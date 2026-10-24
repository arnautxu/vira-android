package cat.vira.android.features.progress.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraCompletionCheck
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.RewardsProgressBar
import cat.vira.android.features.progress.logic.ViraWelcomeChallenge
import cat.vira.android.features.shop.AccessoryItem

/// El que dibuixa «Tres dies amb Vira»: els dies que ja compten i, si encara no són teves i el
/// saldo no hi arriba, les ulleres de la botiga.
internal data class WelcomeChallengeUi(
    val activeDays: Int,
    val balance: Int,
    val firstItem: AccessoryItem?,
) {
    val isReady: Boolean get() = activeDays >= ViraWelcomeChallenge.DAYS
}

/// «Tres dies amb Vira», a dalt de Reptes fins que es cobra (`ChallengesWelcomeCard`), i a sota el
/// primer article de la botiga a l'abast. Els dies es diuen amb text i marca, mai només amb color.
/// Res de rellotge ni de «queden»: el repte no caduca.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChallengesWelcomeCard(ui: WelcomeChallengeUi, onClaim: () -> Unit, modifier: Modifier = Modifier) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ViraGradient.card(ViraArea.Rewards), RoundedCornerShape(ViraRadius.l))
                .padding(ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                if (!LocalViraEnvironment.current.isAccessibilitySize) {
                    ViraPictogram(R.drawable.picto_flame, size = 44.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                    Text(stringResource(R.string.challenge_welcome_title), style = ViraFont.headline, color = ink)
                    Text(stringResource(R.string.challenge_welcome_description), style = ViraFont.footnote, color = ink)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                for (day in 1..ViraWelcomeChallenge.DAYS) DayChip(day, isDone = day <= ui.activeDays)
                Prize()
            }
            if (ui.isReady) {
                ViraActionButton(
                    text = stringResource(R.string.challenge_claim_amount, ViraWelcomeChallenge.REWARD.formatted()),
                    onClick = onClaim,
                    area = ViraArea.Today,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ui.firstItem?.let { ShopNearLine(it, ui.balance) }
    }
}

@Composable
private fun DayChip(day: Int, isDone: Boolean) {
    val label = stringResource(R.string.challenge_welcome_day, day)
    val state = stringResource(if (isDone) R.string.habits_legend_done else R.string.habits_legend_pending)
    val ink = if (isDone) ViraColor.onSurface(ViraArea.Rewards) else ViraColor.onCard(ViraArea.Rewards)
    Row(
        Modifier
            .defaultMinSize(minHeight = 32.dp)
            .background(if (isDone) ViraColor.surface(ViraArea.Rewards) else ViraColor.surfaceElevated.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = ViraSpacing.s)
            .clearAndSetSemantics { contentDescription = label; stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        // La marca és la que diu l'estat; el color només l'acompanya.
        ViraCompletionCheck(isDone = isDone, celebrates = false, doneTint = ink, pendingTint = ink, modifier = Modifier.size(18.dp))
        Text(label, style = ViraFont.footnoteEmphasis, color = ink, maxLines = 1)
    }
}

@Composable
private fun Prize() {
    val label = stringResource(R.string.progress_coins, ViraWelcomeChallenge.REWARD.formatted())
    Row(
        Modifier.clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(R.drawable.picto_shop, size = 22.dp, scalesWithText = true)
        Text(label, style = ViraFont.headline, color = ViraColor.onCard(ViraArea.Rewards))
    }
}

/// «Ulleres, 300 · Amb el repte, a tocar», amb la barra del saldo fins al preu.
@Composable
private fun ShopNearLine(item: AccessoryItem, balance: Int) {
    val progress = stringResource(R.string.challenge_progress, balance, item.price)
    Row(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.s)
            .semantics(mergeDescendants = true) { stateDescription = progress },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Image(painterResource(item.art), contentDescription = null, modifier = Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(
                stringResource(R.string.challenge_welcome_shop, stringResource(item.nameRes), item.price),
                style = ViraFont.footnoteEmphasis,
                color = ViraColor.textPrimary,
            )
            Text(stringResource(R.string.challenge_welcome_shop_support), style = ViraFont.footnote, color = ViraColor.textSecondary)
            RewardsProgressBar(balance.toFloat() / item.price)
        }
    }
}
