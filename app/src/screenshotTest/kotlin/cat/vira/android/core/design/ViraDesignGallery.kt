package cat.vira.android.core.design

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraCelebrationMoment
import cat.vira.android.core.design.components.ViraCelebrationScreen
import cat.vira.android.core.design.components.ViraCompletionCheck
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraLevelBar
import cat.vira.android.core.design.components.ViraLevelProgress
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraProfileSummary
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.ViraTabBar
import cat.vira.android.core.design.components.ViraTabBarItem
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.ViraTileStyle
import cat.vira.android.core.design.components.ViraTopBar
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import com.android.tools.screenshot.PreviewTest

/// Galeria del sistema de disseny: cada peça en clar, fosc i lletra al 200 %, a l'ample d'un
/// iPhone (393 dp) perquè es pugui posar al costat de la captura de l'iPhone. Les imatges les fa
/// la CI (`updateDebugScreenshotTest`) i, un cop aprovades, són la prova de regressió.
@Preview(name = "clar", widthDp = 393, locale = "ca")
@Preview(name = "fosc", widthDp = 393, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, locale = "es", fontScale = 2f)
annotation class ViraCaptures

@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 852, locale = "es", fontScale = 2f)
annotation class ViraScreenCaptures

@Composable
private fun Gallery(area: ViraArea = ViraArea.Today, content: @Composable () -> Unit) {
    ViraTheme(reduceMotion = true) {
        Box(Modifier.fillMaxWidth().viraSectionBackground(area).padding(vertical = ViraSpacing.m)) { content() }
    }
}

@PreviewTest @ViraCaptures @Composable
fun Colors() = Gallery {
    Column(Modifier.padding(horizontal = ViraSpacing.screenMargin), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        ViraArea.entries.forEach { area ->
            Column(
                Modifier.fillMaxWidth().background(ViraGradient.card(area), RoundedCornerShape(ViraRadius.l)).padding(ViraSpacing.m),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                Text(area.name, style = ViraFont.headline, color = ViraColor.onCard(area))
                Text(stringResource(R.string.level_title, 7), style = ViraFont.footnote, color = ViraColor.onCard(area))
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.challenge_claim),
                        style = ViraFont.footnote,
                        color = ViraColor.onAccent,
                        modifier = Modifier.background(ViraGradient.accent(area), RoundedCornerShape(50)).padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs),
                    )
                    Box(Modifier.weight(1f).height(10.dp).background(ViraGradient.accent(area, ViraGradient.Axis.Horizontal), RoundedCornerShape(50)))
                }
            }
        }
    }
}

@PreviewTest @ViraCaptures @Composable
fun TopBar() = Gallery {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        ViraTopBar(ViraArea.Today, ViraProfileSummary("Arnau", coins = 620, streakDays = 12), {}, {}, {}) {
            ViraPictogram(R.drawable.viri_phone, size = 40.dp)
        }
        ViraTopBar(ViraArea.Plan, ViraProfileSummary("Arnau", coins = 0), {}, {}, {})
        ViraTopBar(ViraArea.Emotions, ViraProfileSummary("Arnau"), {}, {}, {})
    }
}

@PreviewTest @ViraCaptures @Composable
fun TabBar() = Gallery {
    ViraTabBar(
        items = listOf(
            ViraTabBarItem("today", stringResource(R.string.tab_today_short), R.drawable.tab_today),
            ViraTabBarItem("plan", stringResource(R.string.tab_plan_short), R.drawable.tab_plan),
            ViraTabBarItem("emotions", stringResource(R.string.tab_emotions_short), R.drawable.tab_emotions),
            ViraTabBarItem("viri", stringResource(R.string.tab_viri_short), R.drawable.picto_shop),
        ),
        selectedKey = "plan",
        focus = ViraTabBarItem("focus", stringResource(R.string.tab_focus_short), R.drawable.picto_focus),
        onTabTap = {},
        onFocus = {},
    )
}

@PreviewTest @ViraCaptures @Composable
fun SectionSwitcher() = Gallery(ViraArea.Plan) {
    val plan = listOf(
        ViraSectionItem("calendar", stringResource(R.string.plan_section_calendar), R.drawable.picto_calendar),
        ViraSectionItem("habits", stringResource(R.string.plan_section_habits), R.drawable.picto_habits),
        ViraSectionItem("medication", stringResource(R.string.plan_section_medication), R.drawable.picto_medication),
    )
    Column(Modifier.padding(horizontal = ViraSpacing.screenMargin), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        ViraSectionSwitcher(ViraArea.Plan, plan, "habits", {}, showsAllLabels = true)
        ViraSectionSwitcher(ViraArea.Plan, plan, "medication", {})
    }
}

@PreviewTest @ViraCaptures @Composable
fun Tiles() = Gallery(ViraArea.Rewards) {
    ViraTileGrid(
        items = listOf(0, 1, 2),
        modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin),
    ) { index, modifier ->
        when (index) {
            0 -> ViraTile(
                ViraArea.Today, R.drawable.picto_game_chest, stringResource(R.string.shop_title), modifier,
                subtitle = stringResource(R.string.profile_destination_shop_detail),
            ) { ViraTag("620", pictogram = R.drawable.picto_shop) }
            1 -> ViraTile(
                ViraArea.Focus, R.drawable.picto_pomodoro, "18", modifier,
                subtitle = stringResource(R.string.progress_focus_sessions), style = ViraTileStyle.Plain,
            )
            else -> ViraTile(ViraArea.Emotions, R.drawable.picto_journal, stringResource(R.string.tab_emotions), modifier)
        }
    }
}

@PreviewTest @ViraCaptures @Composable
fun Buttons() = Gallery {
    Column(Modifier.padding(horizontal = ViraSpacing.screenMargin), verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap)) {
        ViraActionButton(stringResource(R.string.action_add), {}, Modifier.fillMaxWidth())
        ViraActionButton(stringResource(R.string.rewards_personal_redeem), {}, Modifier.fillMaxWidth(), area = ViraArea.Rewards)
        ViraActionButton(stringResource(R.string.action_cancel), {}, Modifier.fillMaxWidth(), ViraActionHierarchy.Secondary)
        ViraActionButton(stringResource(R.string.action_done), {}, Modifier.fillMaxWidth(), ViraActionHierarchy.Textual)
        ViraActionButton(stringResource(R.string.action_add), {}, Modifier.fillMaxWidth(), enabled = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ViraCompletionCheck(isDone = false, pendingTint = ViraColor.textSecondary)
            ViraCompletionCheck(isDone = true, doneTint = ViraColor.success)
            ViraTag("7 dies")
        }
    }
}

@PreviewTest @ViraCaptures @Composable
fun LevelBar() = Gallery(ViraArea.Rewards) {
    Column(Modifier.padding(horizontal = ViraSpacing.screenMargin), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        ViraLevelBar(ViraLevelProgress(level = 3, totalXP = 450, currentLevelXP = 150, xpToNext = 150))
        ViraLevelBar(ViraLevelProgress(level = 0, totalXP = 0, currentLevelXP = 0, xpToNext = 100))
    }
}

@PreviewTest @ViraCaptures @Composable
fun StatesAndTitles() = Gallery {
    Column(Modifier.padding(horizontal = ViraSpacing.screenMargin)) {
        ViraPageTitle(stringResource(R.string.shop_title), ViraArea.Today)
        ViraEmptyState(
            title = stringResource(R.string.today_empty_title),
            message = stringResource(R.string.today_empty_message),
            actionTitle = stringResource(R.string.action_add),
            onAction = {},
            icon = { ViraPictogram(R.drawable.picto_calendar, size = 64.dp) },
        )
        ViraInlineError(stringResource(R.string.a11y_metric_coins))
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun Celebration() = ViraTheme(reduceMotion = true) {
    ViraCelebrationScreen(
        caption = "50 ViriCoins",
        onContinue = {},
        eyebrow = stringResource(R.string.challenge_claim),
        count = 50,
        message = stringResource(R.string.level_title, 4),
        celebrationsEnabled = false,
    ) { ViraPictogram(R.drawable.picto_game_trophy, size = 180.dp) }
}

@PreviewTest @ViraScreenCaptures @Composable
fun CelebrationMoment() = ViraTheme(reduceMotion = true) {
    ViraCelebrationMoment(
        title = stringResource(R.string.level_title, 4),
        message = stringResource(R.string.metrics_total_xp, 450),
        onContinue = {},
        scene = { ViraPictogram(R.drawable.viri_meditating, size = 220.dp) },
        summary = { ViraLevelBar(ViraLevelProgress(level = 4, totalXP = 450, currentLevelXP = 50, xpToNext = 250)) },
    )
}
