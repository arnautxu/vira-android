package cat.vira.android.features.progress

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraCelebrationMoment
import cat.vira.android.core.design.components.ViraCelebrationScreen
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTouchTarget
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ChallengePeriod
import cat.vira.android.features.progress.logic.StreakMilestone
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.logic.ViraLevel
import cat.vira.android.features.progress.ui.ViraStreakFlame
import cat.vira.android.features.progress.ui.StreakWeekBar
import cat.vira.android.features.progress.ui.formatted
import cat.vira.android.features.progress.ui.pictogram
import java.time.DayOfWeek
import java.time.LocalDate

// Les pantalles completes de la pestanya Viri. **Qui decideix quan surten és l'arrel** (la cua de
// celebracions de `app/`, `ViraCelebrationQueue` de l'iPhone): aquí només hi ha què diuen.

/// Cada cop que la ratxa avança un dia (`StreakCelebrationView`). És **l'única** pantalla on la
/// ratxa és la protagonista; per això aquí, i només aquí, la xifra gegant i l'heroi gran.
///
/// - `cardKey`: `StreakMilestone.WEEK_CARD` o `MONTH_CARD` el dia que la ratxa creua el llindar
///   (`ViraProgress.takeStreakMilestone`, només amb les cartes disponibles); llavors l'heroi és la
///   carta i el seu nom va a la píndola.
/// - `message`: la frase de Viri **ja triada** (`StreakMessageCatalog.advance`). Es tria en
///   l'esdeveniment que encua la celebració, mai mentre es dibuixa: rotar-la escriu a les
///   preferències, i fer-ho durant una passada de dibuixat torna a dibuixar en bucle.
@Composable
fun StreakCelebration(
    streakDays: Int,
    week: StreakWeek,
    message: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    cardKey: String? = null,
    today: LocalDate = LocalDate.now(),
    firstDayOfWeek: DayOfWeek = week.days.first().dayOfWeek,
    celebrationsEnabled: Boolean = true,
    onShown: () -> Unit = {},
) {
    val cardTitle = when (cardKey) {
        StreakMilestone.WEEK_CARD -> stringResource(R.string.card_week_streak_title)
        StreakMilestone.MONTH_CARD -> stringResource(R.string.card_month_streak_title)
        else -> null
    }
    ViraCelebrationScreen(
        caption = pluralStringResource(R.plurals.streak_days, streakDays, streakDays),
        onContinue = onContinue,
        modifier = modifier,
        eyebrow = cardTitle,
        count = streakDays,
        message = message,
        celebrationsEnabled = celebrationsEnabled,
        onShown = onShown,
        extra = { StreakWeekBar(week, today, firstDayOfWeek, revealDelayMillis = 640) },
    ) {
        when (cardKey) {
            StreakMilestone.WEEK_CARD -> ViraPictogram(R.drawable.badge_week_streak, size = 200.dp, isScene = true)
            StreakMilestone.MONTH_CARD -> ViraPictogram(R.drawable.badge_month_streak, size = 200.dp, isScene = true)
            else -> StreakCelebrationHero(200.dp)
        }
    }
}

/// Viri saltant amb la flama als peus (`StreakCelebrationHero`). La flama baixa de rang: és la
/// marca de la ratxa, no un segon personatge. Un balanceig lent i curt; amb Reduir moviment, quiet.
@Composable
private fun StreakCelebrationHero(side: Dp) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val bob = if (reduceMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "viri")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ViraMotion.BREATH_MILLIS / 2), RepeatMode.Reverse),
            label = "bob",
        )
        value
    }
    Box(Modifier.size(side).clearAndSetSemantics {}, contentAlignment = Alignment.BottomStart) {
        ViraStreakFlame(side * 0.46f, Modifier.offset(x = -side * 0.06f, y = side * 0.04f))
        ViraPictogram(
            R.drawable.viri_hero,
            size = side,
            modifier = Modifier.graphicsLayer { translationY = -side.toPx() * 0.03f * bob },
        )
    }
}

/// En pujar de nivell (`LevelUpCelebrationView`): Viri en coet i el marc que toca. **Sense
/// monedes addicionals**: el nivell és el moment en si, no un tercer generador de moneda.
///
/// `totalXP`: l'XP real. Si ja no encaixa amb el nivell que es celebra, es pren el llindar just.
@Composable
fun LevelUpCelebration(level: Int, totalXP: Int, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val frame = ViraLevel.frame(level)
    val xp = if (ViraLevel.level(totalXP) == level) totalXP else ViraLevel.thresholdXP(level)
    val span = ViraLevel.thresholdXP(level + 1) - ViraLevel.thresholdXP(level)
    val earned = xp - ViraLevel.thresholdXP(level)
    val pills = buildList {
        add(CelebrationPill(stringResource(R.string.celebration_level_eyebrow), isFilled = true))
        frame?.let {
            add(
                CelebrationPill(
                    stringResource(
                        when (it) {
                            ViraLevel.LevelFrame.BRONZE -> R.string.celebration_level_frame_bronze
                            ViraLevel.LevelFrame.SILVER -> R.string.celebration_level_frame_silver
                            ViraLevel.LevelFrame.GOLD -> R.string.celebration_level_frame_gold
                            ViraLevel.LevelFrame.DIAMOND -> R.string.celebration_level_frame_diamond
                        },
                    ),
                ),
            )
        }
    }
    ViraCelebrationMoment(
        title = stringResource(R.string.levelup_title, level),
        message = stringResource(R.string.levelup_message),
        onContinue = onContinue,
        modifier = modifier,
        area = ViraArea.Today,
        scene = { SceneImage(R.drawable.viri_scene_launch) },
    ) {
        CelebrationSummaryCard(ViraArea.Today, pills = pills) {
            CelebrationSummaryRow(
                frame?.pictogram ?: R.drawable.picto_game_level_up,
                level.formatted(),
                ViraArea.Today,
                detail = stringResource(R.string.celebration_level_now, level),
            )
            CelebrationProgressBar(
                if (span > 0) earned.toFloat() / span else 0f,
                stringResource(R.string.celebration_level_xp, earned.formatted(), span.formatted()),
                ViraArea.Today,
                trailing = stringResource(R.string.celebration_level_next, level + 1),
            )
        }
    }
}

/// En recollir un repte (`ChallengeCelebrationView`): Viri en bici i el que s'ha guanyat. **Cap
/// repte de medicació**: el catàleg no en té cap i aquesta pantalla no hi pot arribar mai.
@Composable
fun ChallengeCelebration(item: ChallengeItem, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    ViraCelebrationMoment(
        title = stringResource(R.string.celebration_challenge_title),
        message = stringResource(R.string.celebration_challenge_message),
        onContinue = onContinue,
        modifier = modifier,
        scene = { SceneImage(R.drawable.viri_scene_bike) },
    ) {
        CelebrationSummaryCard(
            ViraArea.Rewards,
            pills = listOf(
                CelebrationPill(periodPill(item.period), isFilled = true),
                CelebrationPill(stringResource(R.string.challenge_claimed)),
            ),
        ) {
            CelebrationSummaryRow(
                R.drawable.picto_game_coins,
                "+" + item.reward.formatted(),
                ViraArea.Rewards,
                detail = stringResource(R.string.celebration_challenge_coins, stringResource(item.title)),
            )
            CelebrationProgressBar(
                1f,
                stringResource(R.string.celebration_progress_count, item.threshold, item.threshold),
                ViraArea.Rewards,
                trailing = stringResource(if (item.period == ChallengePeriod.DAILY) R.string.challenge_section_daily else R.string.challenge_section_weekly),
            )
        }
    }
}

/// Quan una acció acaba de **completar** un repte encara no recollit (`ChallengeProgressMomentView`).
/// No dona res: la píndola diu «Per recollir» i les monedes es recullen a Reptes.
@Composable
fun ChallengeProgressMoment(
    item: ChallengeItem,
    progress: Int,
    onContinue: () -> Unit,
    onOpenChallenges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = minOf(progress, item.threshold)
    ViraCelebrationMoment(
        title = stringResource(R.string.celebration_challenge_title),
        message = stringResource(R.string.challenge_moment_message),
        onContinue = onContinue,
        modifier = modifier,
        scene = { SceneImage(R.drawable.viri_scene_bike) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            CelebrationSummaryCard(
                ViraArea.Rewards,
                pills = listOf(
                    CelebrationPill(periodPill(item.period), isFilled = true),
                    CelebrationPill(stringResource(R.string.challenge_moment_ready)),
                ),
            ) {
                CelebrationSummaryRow(
                    R.drawable.picto_game_coins,
                    "+" + item.reward.formatted(),
                    ViraArea.Rewards,
                    detail = stringResource(R.string.celebration_challenge_coins, stringResource(item.title)),
                )
                CelebrationProgressBar(
                    done.toFloat() / maxOf(item.threshold, 1),
                    stringResource(R.string.celebration_progress_count, done, item.threshold),
                    ViraArea.Rewards,
                )
            }
            Text(
                stringResource(R.string.challenge_moment_open),
                style = ViraFont.callout.copy(textDecoration = TextDecoration.Underline),
                color = ViraColor.accent(ViraArea.Rewards),
                modifier = Modifier
                    .viraTouchTarget()
                    .viraPressable {
                        onContinue()
                        onOpenChallenges()
                    }
                    .wrapCenter(),
            )
        }
    }
}

@Composable
private fun periodPill(period: ChallengePeriod): String =
    stringResource(if (period == ChallengePeriod.DAILY) R.string.celebration_challenge_daily else R.string.celebration_challenge_weekly)

@Composable
private fun SceneImage(scene: Int) {
    Image(
        painter = painterResource(scene),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

private fun Modifier.wrapCenter(): Modifier = this.then(Modifier.padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.s))
