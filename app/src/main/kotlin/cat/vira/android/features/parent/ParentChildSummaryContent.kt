package cat.vira.android.features.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.ViraTileStyle
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// La cara **Resum** del detall (`ParentChildSummaryContent` de l'iPhone): la setmana en una
/// targeta groga (si «Ús i ratxa» és obert) i una fitxa per àmbit. Els tancats hi són igual, en
/// blanc i amb la clau.
///
/// Només recomptes. Que un pare/mare sàpiga que el seu fill fa check-ins és una cosa; llegir què hi
/// ha escrit, una altra de molt més gran (`PRIVACY_AND_SAFETY.md` §2).
@Composable
internal fun ParentChildSummaryContent(link: ParentLink, summary: ParentChildSummary, isLoading: Boolean) {
    // L'ús va a la targeta de dalt quan és obert; tancat, passa a la graella com els altres.
    val tiles = if (link.scopeUsage) {
        listOf(ParentScope.HABITS, ParentScope.EMOTIONS, ParentScope.MEDICATION)
    } else {
        listOf(ParentScope.HABITS, ParentScope.EMOTIONS, ParentScope.MEDICATION, ParentScope.USAGE)
    }
    Column(
        Modifier.alpha(if (isLoading) 0.45f else 1f),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap),
    ) {
        if (link.scopeUsage) ParentWeekCard(summary)
        ParentSectionHeader(stringResource(R.string.parent_open_section), Modifier.padding(top = ViraSpacing.s))
        ViraTileGrid(tiles) { scope, cellModifier -> Tile(scope, link, summary, cellModifier) }
        ParentFootnote(stringResource(R.string.parent_summary_emotions_footnote))
    }
}

@Composable
private fun Tile(scope: ParentScope, link: ParentLink, summary: ParentChildSummary, modifier: Modifier) {
    val area = scope.area
    val title = stringResource(scope.title)
    if (!scope.isOpen(link)) {
        val closed = stringResource(R.string.parent_scope_closed)
        val tag = stringResource(R.string.parent_tile_closed_tag)
        ViraTile(
            area, scope.pictogram, title, modifier.clearAndSetSemantics { contentDescription = "$title, $closed, $tag" },
            subtitle = closed, style = ViraTileStyle.Plain,
        ) { ViraTag(tag, pictogram = R.drawable.picto_key) }
        return
    }
    val locale = currentLocale()
    val value: Int? = when (scope) {
        ParentScope.USAGE -> summary.streakDays
        ParentScope.HABITS -> summary.habitsDoneLastWeek
        ParentScope.EMOTIONS -> summary.emotionCheckinsLastWeek
        ParentScope.MEDICATION -> summary.activeMedications
    }
    // `null` és «no ho pots veure», no un zero.
    val shown = value?.let { number(it, locale) } ?: "—"
    val unit = stringResource(
        when (scope) {
            ParentScope.USAGE -> R.string.parent_week_streak_unit
            ParentScope.HABITS -> R.string.parent_tile_habits_value
            ParentScope.EMOTIONS -> R.string.parent_tile_emotions_value
            ParentScope.MEDICATION -> R.string.parent_tile_medication_value
        },
    )
    val tag: String? = when (scope) {
        ParentScope.HABITS -> summary.tasksPending?.let { stringResource(R.string.parent_tile_tasks, it) }
        ParentScope.EMOTIONS -> stringResource(R.string.parent_tile_emotions_tag)
        else -> null
    }
    val ink = ViraColor.onCard(area)
    ViraTile(
        area, scope.pictogram, title,
        modifier.clearAndSetSemantics { contentDescription = listOfNotNull(title, "$shown $unit", tag).joinToString(", ") },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(shown, style = ViraFont.largeTitle.copy(fontFeatureSettings = "tnum"), color = ink)
            Text(unit, style = ViraFont.footnote, color = ink)
            if (tag != null) ViraTag(tag, Modifier.padding(top = ViraSpacing.xxs))
        }
    }
}

/// El color de l'àrea on viu cada cosa a l'app del jove (`DESIGN_RULES.md` §7).
private val ParentScope.area: ViraArea
    get() = when (this) {
        ParentScope.USAGE -> ViraArea.Rewards
        ParentScope.HABITS, ParentScope.MEDICATION -> ViraArea.Plan
        ParentScope.EMOTIONS -> ViraArea.Emotions
    }

/// La setmana, en groc com les recompenses de l'app del jove: ratxa en gran, focus i XP en
/// píndoles. Cap comparació amb res: són els números de la setmana, i prou.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParentWeekCard(summary: ParentChildSummary) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val locale = currentLocale()
    val title = stringResource(R.string.parent_week_title)
    val streak = summary.streakDays?.let { number(it, locale) } ?: "—"
    val unit = stringResource(R.string.parent_week_streak_unit)
    val focus = stringResource(R.string.parent_week_focus, summary.focusSessionsLastWeek ?: 0)
    val xp = stringResource(R.string.parent_week_xp, number(summary.totalXP ?: 0, locale))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ViraGradient.card(ViraArea.Rewards), shape)
            .clearAndSetSemantics { contentDescription = "$title, $streak $unit, $focus, $xp" },
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f).padding(ViraSpacing.m), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Text(title, style = ViraFont.headline, color = ink)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                Text(streak, style = ViraFont.timer, color = ink)
                Text(unit, style = ViraFont.headline, color = ink, modifier = Modifier.padding(bottom = ViraSpacing.s))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                ViraTag(focus, pictogram = R.drawable.picto_focus)
                ViraTag(xp, pictogram = R.drawable.picto_xp)
            }
        }
        if (!LocalViraEnvironment.current.isAccessibilitySize) {
            Image(
                painterResource(R.drawable.viri_entry_calendar), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.widthIn(max = 130.dp).offset(x = ViraSpacing.xs, y = ViraSpacing.s),
            )
        }
    }
}
