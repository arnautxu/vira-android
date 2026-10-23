package cat.vira.android.features.progress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraCompletionCheck
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.RewardsPill
import cat.vira.android.features.progress.RewardsProgressBar
import cat.vira.android.features.progress.RewardsSceneCard
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ChallengePeriod

/// Un repte amb el seu estat al cicle actual.
internal data class ChallengeRowState(val item: ChallengeItem, val count: Int, val claimed: Boolean) {
    val isCompleted: Boolean get() = count >= item.threshold
    val isReady: Boolean get() = !claimed && isCompleted
    /// Primer el que pots recollir, després el que està en marxa i al final el que ja és fet.
    val rank: Int get() = if (claimed) 2 else if (isCompleted) 0 else 1
}

/// Tot el que dibuixa Reptes.
internal data class ChallengesUi(
    val period: ChallengePeriod,
    val rows: List<ChallengeRowState>,
    val balance: Int,
    val saveFailed: Boolean = false,
    /// «Tres dies amb Vira», fins que es cobra; `null` un cop cobrat.
    val welcome: WelcomeChallengeUi? = null,
)

/// Reptes diaris i setmanals (`ChallengesView`, `VIRI_AND_GAMIFICATION.md` §2.6): objectius curts
/// sobre el que ja fas, **mai sobre medicació**. Sense penalització: un repte no recollit abans que
/// el cicle canviï no deixa cap rastre, i res de rellotge: la píndola diu «Tornen demà».
@Composable
internal fun ChallengesContent(
    ui: ChallengesUi,
    onBack: () -> Unit,
    onPeriod: (ChallengePeriod) -> Unit,
    onClaim: (ChallengeItem) -> Unit,
    modifier: Modifier = Modifier,
    onClaimWelcome: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Rewards)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = ViraSpacing.xs, end = ViraSpacing.screenMargin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressBackButton(onBack)
            Spacer(Modifier.weight(1f))
            ProgressWallet(ui.balance)
        }
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPageTitle(stringResource(R.string.challenge_title), ViraArea.Rewards)
            ViraSectionSwitcher(
                area = ViraArea.Rewards,
                items = listOf(
                    ViraSectionItem(ChallengePeriod.DAILY.name, stringResource(R.string.challenge_section_daily), R.drawable.picto_cal_sun),
                    ViraSectionItem(ChallengePeriod.WEEKLY.name, stringResource(R.string.challenge_section_weekly), R.drawable.picto_cal_week),
                ),
                selectedKey = ui.period.name,
                onSelect = { onPeriod(ChallengePeriod.valueOf(it)) },
                showsAllLabels = true,
                modifier = Modifier.padding(bottom = ViraSpacing.xs),
            )
            // El primer premi a l'abast (2026-10-22): a dalt fins que es cobra.
            ui.welcome?.let { ChallengesWelcomeCard(it, onClaimWelcome) }
            if (ui.saveFailed) ViraInlineError(stringResource(R.string.challenge_save_error))
            // Sense ítems no es dibuixa res: una targeta buida es llegiria com una pantalla trencada.
            if (ui.rows.isNotEmpty()) {
                ChallengesSummaryCard(
                    period = ui.period,
                    done = ui.rows.count { it.isCompleted },
                    total = ui.rows.size,
                    toClaim = ui.rows.filter { it.isReady }.sumOf { it.item.reward },
                    toEarn = ui.rows.filter { !it.isCompleted }.sumOf { it.item.reward },
                    modifier = Modifier.padding(bottom = ViraSpacing.xs),
                )
                ui.rows.sortedBy { it.rank }.forEach { row ->
                    ChallengeRow(row, onClaim = { onClaim(row.item) })
                }
            }
            Text(
                stringResource(R.string.challenge_how_it_counts),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
                modifier = Modifier.padding(horizontal = ViraSpacing.xs),
            )
        }
        Spacer(Modifier.height(viraTabBarClearance()))
    }
}

/// Mòdul protagonista de Reptes (`ChallengesSummaryCard`): Viri, la bombolla i el resum del cicle
/// («2 de 4» en text i barra). La frase mai diu què falta ni ho compta com a retret.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengesSummaryCard(period: ChallengePeriod, done: Int, total: Int, toClaim: Int, toEarn: Int, modifier: Modifier = Modifier) {
    val message = stringResource(
        when {
            toClaim > 0 -> R.string.challenge_viri_ready
            done == total -> R.string.challenge_viri_done
            period == ChallengePeriod.DAILY -> R.string.challenge_viri_daily
            else -> R.string.challenge_viri_weekly
        },
    )
    val ink = ViraColor.onCard(ViraArea.Rewards)
    RewardsSceneCard(R.drawable.viri_suggest_habit_1, message, modifier) {
        FlowRow(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            itemVerticalAlignment = Alignment.Bottom,
        ) {
            Text(stringResource(R.string.challenge_progress, done, total), style = ViraFont.largeTitle, color = ink, maxLines = 1)
            Text(
                stringResource(if (period == ChallengePeriod.DAILY) R.string.challenge_summary_daily else R.string.challenge_summary_weekly),
                style = ViraFont.callout,
                color = ink,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        RewardsProgressBar(if (total == 0) 0f else done.toFloat() / total, onCard = true)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            if (toClaim > 0) {
                RewardsPill(stringResource(R.string.challenge_pill_to_claim, toClaim.formatted()), showsCoin = true)
            } else if (toEarn > 0) {
                RewardsPill(stringResource(R.string.challenge_pill_to_earn, toEarn.formatted()), showsCoin = true)
            }
            RewardsPill(
                stringResource(if (period == ChallengePeriod.DAILY) R.string.challenge_pill_renew_daily else R.string.challenge_pill_renew_weekly),
            )
        }
    }
}

/// Una fila de repte (`ChallengeRow`). Tres estats:
/// - **En marxa**: targeta blanca amb la barra i el compte en text («1 de 3»), mai només la barra.
/// - **A punt**: targeta groga amb el botó lila «Recull 50». L'única acció de la pantalla, i per
///   això és la que té color.
/// - **Recollit**: títol ratllat i cercle ple, com les files fetes d'«El teu dia».
@Composable
private fun ChallengeRow(row: ChallengeRowState, onClaim: () -> Unit) {
    val item = row.item
    val shape = RoundedCornerShape(ViraRadius.l)
    val primary = if (row.isReady) ViraColor.onCard(ViraArea.Rewards) else ViraColor.textPrimary
    val status = when {
        row.claimed -> stringResource(R.string.challenge_claimed)
        row.isReady -> stringResource(R.string.challenge_claim)
        else -> stringResource(R.string.challenge_progress, row.count, item.threshold)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (row.isReady) Modifier.background(ViraGradient.card(ViraArea.Rewards), shape)
                else Modifier.background(ViraColor.surfaceElevated, shape),
            )
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        val header: @Composable () -> Unit = {
            ViraPictogram(item.pictogram, size = 44.dp, modifier = Modifier.alpha(if (row.claimed) 0.5f else 1f))
        }
        val texts: @Composable (Modifier) -> Unit = { textModifier ->
            Column(textModifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(
                    stringResource(item.title),
                    style = ViraFont.headline.copy(textDecoration = if (row.claimed) TextDecoration.LineThrough else null),
                    color = if (row.claimed) ViraColor.textSecondary else primary,
                )
                Text(
                    if (row.claimed) stringResource(R.string.challenge_claimed_amount, item.reward.formatted())
                    else stringResource(item.description),
                    style = ViraFont.footnote,
                    color = if (row.isReady) primary else ViraColor.textSecondary,
                )
            }
        }
        val headerSemantics = Modifier.semantics(mergeDescendants = true) { stateDescription = status }
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(headerSemantics, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                header()
                texts(Modifier.fillMaxWidth())
                Trailing(row, primary)
            }
        } else {
            Row(headerSemantics, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                header()
                texts(Modifier.weight(1f))
                Trailing(row, primary)
            }
        }
        if (row.isReady) {
            ViraActionButton(
                text = stringResource(R.string.challenge_claim_amount, item.reward.formatted()),
                onClick = onClaim,
                area = ViraArea.Today,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (!row.claimed) {
            val shown = minOf(row.count, item.threshold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                RewardsProgressBar(shown.toFloat() / item.threshold, Modifier.weight(1f))
                Text(
                    stringResource(R.string.challenge_progress, shown, item.threshold),
                    style = ViraFont.footnoteEmphasis,
                    color = ViraColor.textSecondary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

@Composable
private fun Trailing(row: ChallengeRowState, primary: androidx.compose.ui.graphics.Color) {
    if (row.claimed) {
        ViraCompletionCheck(
            isDone = true,
            celebrates = false,
            doneTint = ViraColor.accent(ViraArea.Rewards),
            pendingTint = ViraColor.textSecondary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    } else {
        val label = stringResource(R.string.progress_coins, row.item.reward.formatted())
        Row(
            Modifier.clearAndSetSemantics { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            ViraPictogram(R.drawable.picto_shop, size = 22.dp, scalesWithText = true)
            Text(
                row.item.reward.formatted(),
                style = ViraFont.headline,
                color = if (row.isReady) primary else ViraColor.onSurface(ViraArea.Rewards),
            )
        }
    }
}
