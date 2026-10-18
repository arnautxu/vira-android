package cat.vira.android.features.progress.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraCompletionCheck
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.components.viraTouchTarget
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.ViriBubble
import cat.vira.android.features.progress.logic.StreakMonth
import cat.vira.android.features.progress.logic.StreakRules.DayRequirement
import cat.vira.android.features.progress.logic.StreakWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/// Tot el que dibuixa la pantalla de la ratxa.
internal data class StreakUi(
    val streak: Streak?,
    val today: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val week: StreakWeek?,
    val month: StreakMonth?,
    /// El que encara falta per sumar el dia; `null` mentre es llegeix.
    val missing: Set<DayRequirement>?,
    val coinBalance: Int,
)

/// Detall de la ratxa (`StreakDetailView`): el número i la setmana, què demana avui, quants dies
/// et puc salvar i el mes sencer.
///
/// No hi ha comparació amb ningú, ni meta amb cofre penjat, ni escut setmanal: cada cosa la prohibeix
/// una norma del projecte (comparació social, comptes enrere, la mecànica d'un dia per dia).
@Composable
internal fun StreakContent(
    ui: StreakUi,
    onBack: () -> Unit,
    onShiftMonth: (Long) -> Unit,
    onBuySavedDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Rewards)
            .verticalScroll(rememberScrollState()),
    ) {
        ProgressBackButton(onBack, Modifier.padding(horizontal = ViraSpacing.xs))
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPageTitle(stringResource(R.string.streak_screen_title), ViraArea.Rewards)
            StreakHeroCard(ui.streak?.currentDays ?: 0, ui.streak?.bestDays ?: 0, ui.week, ui.today, ui.firstDayOfWeek)
            if (ui.missing != null) Requirements(ui.missing, Modifier.padding(top = ViraSpacing.xs))
            SavedDaysCard(ui.streak, ui.coinBalance, onBuySavedDay)
            MonthSection(ui, onShiftMonth)
        }
        Spacer(Modifier.height(viraTabBarClearance()))
    }
}

/// Capçalera de la ratxa (`StreakHeroCard`): el número, la flama i la setmana en una targeta groga.
/// A zero, una de les deu escenes de Viri preparant el foc, una per dia: convida, no retreu.
@Composable
internal fun StreakHeroCard(currentDays: Int, bestDays: Int, week: StreakWeek?, today: LocalDate, firstDayOfWeek: DayOfWeek) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val fontScale = LocalDensity.current.fontScale
    Column(Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Rewards))) {
        if (currentDays == 0) StartScene(today)
        Column(Modifier.padding(ViraSpacing.m), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                val spoken = pluralStringResource(R.plurals.streak_days, currentDays, currentDays)
                Column(
                    Modifier.weight(1f).semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
                ) {
                    Text(
                        currentDays.formatted(),
                        style = ViraFont.timer,
                        color = ink,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                    Text(
                        stringResource(if (currentDays == 1) R.string.streak_days_label_one else R.string.streak_days_label_other),
                        style = ViraFont.headline,
                        color = ink,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                    )
                    when {
                        bestDays > currentDays -> Text(pluralStringResource(R.plurals.streak_best, bestDays, bestDays), style = ViraFont.footnote, color = ink)
                        // Diu com s'encén, no què falta.
                        currentDays == 0 -> Text(stringResource(R.string.streak_hero_start), style = ViraFont.footnote, color = ink)
                    }
                }
                // La flama no es degrada amb els dies: a zero surt apagada i prou.
                ViraStreakFlame((96.dp * fontScale).coerceAtMost(140.dp), isLit = currentDays > 0)
            }
            if (week != null) StreakWeekBar(week, today, firstDayOfWeek)
        }
    }
}

@Composable
private fun StartScene(today: LocalDate) {
    val scenes = START_SCENES
    val height = if (LocalViraEnvironment.current.isAccessibilitySize) 120.dp else 184.dp
    Column {
        Image(
            painter = painterResource(scenes[today.dayOfYear % scenes.size]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(height),
        )
        ViriBubble(
            stringResource(R.string.streak_start_viri),
            ViraArea.Rewards,
            Modifier.padding(horizontal = ViraSpacing.m).overlapTop(ViraSpacing.xxl),
        )
    }
}

/// Les deu escenes de Viri preparant el foc (`viriStreakStartScenes`).
private val START_SCENES = listOf(
    R.drawable.viri_streak_start_01, R.drawable.viri_streak_start_02, R.drawable.viri_streak_start_03,
    R.drawable.viri_streak_start_04, R.drawable.viri_streak_start_05, R.drawable.viri_streak_start_06,
    R.drawable.viri_streak_start_07, R.drawable.viri_streak_start_08, R.drawable.viri_streak_start_09,
    R.drawable.viri_streak_start_10,
)

/// Les tres coses que demana el dia, amb les files d'«El teu dia». Sense això la regla és
/// invisible: fer una tasca i veure que la ratxa no es mou es llegeix com una app espatllada.
///
/// És la llista **del que queda**, sense cap marca de «no fet»: el que ja hi és queda ratllat i amb
/// la casella plena, i «1 de 3» ho diu en paraules.
@Composable
private fun Requirements(missing: Set<DayRequirement>, modifier: Modifier = Modifier) {
    val all = DayRequirement.entries
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Row(Modifier.padding(horizontal = ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (missing.isEmpty()) R.string.streak_requirements_done else R.string.streak_requirements_title).uppercase(currentLocale()),
                style = ViraFont.footnoteEmphasis,
                color = ViraColor.onSurface(ViraArea.Rewards),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                stringResource(R.string.streak_requirements_progress, all.size - missing.size, all.size),
                style = ViraFont.footnoteEmphasis,
                color = ViraColor.onSurface(ViraArea.Rewards),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m),
        ) {
            all.forEachIndexed { index, requirement ->
                RequirementRow(requirement, isDone = requirement !in missing)
                if (index < all.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = ViraSpacing.xxxl + ViraSpacing.xs), color = ViraColor.separator)
                }
            }
        }
    }
}

@Composable
private fun RequirementRow(requirement: DayRequirement, isDone: Boolean) {
    val (picto, label) = when (requirement) {
        DayRequirement.HABIT -> R.drawable.picto_target to R.string.streak_requirement_habit
        DayRequirement.EMOTION -> R.drawable.tab_emotions to R.string.streak_requirement_emotion
        DayRequirement.TASK -> R.drawable.picto_cal_task to R.string.streak_requirement_task
    }
    val state = stringResource(if (isDone) R.string.streak_day_done else R.string.streak_day_pending)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.xxs)
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(picto, size = 32.dp, modifier = Modifier.alpha(if (isDone) 0.5f else 1f))
        Text(
            stringResource(label),
            style = ViraFont.callout.copy(textDecoration = if (isDone) TextDecoration.LineThrough else null),
            color = if (isDone) ViraColor.textSecondary else ViraColor.textPrimary,
            modifier = Modifier.weight(1f),
        )
        // Només ensenya l'estat: cada cosa es fa a la seva pantalla, no des d'aquí.
        ViraCompletionCheck(
            isDone = isDone,
            celebrates = false,
            doneTint = ViraColor.textPrimary,
            pendingTint = ViraColor.textSecondary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/// Quants dies pot salvar Viri i, si es pot, comprar-ne un més (`StreakSavedDaysCard`). Just sota
/// «Per sumar avui»: la protecció és el que fa la ratxa utilitzable amb TDAH.
@Composable
private fun SavedDaysCard(streak: Streak?, balance: Int, onBuy: () -> Unit) {
    val canBuy = streak != null && streak.freezesAvailable < Streak.MAX_FREEZES && balance >= Streak.FREEZE_PRICE
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraPictogram(R.drawable.picto_shield, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(stringResource(R.string.streak_saved_days, streak?.freezesAvailable ?: 0), style = ViraFont.headline, color = ViraColor.textPrimary)
                Text(stringResource(R.string.streak_saved_days_hint), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        if (canBuy) {
            // Comprar és una acció explícita; els gratuïts es gasten sols. Secundari: no és el que
            // la pantalla demana fer.
            ViraActionButton(
                text = stringResource(R.string.streak_saved_days_buy, Streak.FREEZE_PRICE.formatted()),
                onClick = onBuy,
                hierarchy = ViraActionHierarchy.Secondary,
                area = ViraArea.Rewards,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/// El mes, amb el canvi de mes i els dos recomptes en una línia a sota.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthSection(ui: StreakUi, onShift: (Long) -> Unit) {
    val locale = currentLocale()
    Column(
        Modifier
            .padding(top = ViraSpacing.xs)
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        val month = ui.month
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = (month?.month ?: YearMonth.from(ui.today))
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                .replaceFirstChar { it.titlecase(locale) }
            Text(title, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
            MonthButton(ChevronDirection.Left, stringResource(R.string.streak_month_previous), enabled = true) { onShift(-1) }
            // No es pot anar més enllà del mes actual: no hi ha res a veure al futur.
            MonthButton(
                ChevronDirection.Right,
                stringResource(R.string.streak_month_next),
                enabled = month != null && month.month < YearMonth.from(ui.today),
            ) { onShift(1) }
        }
        if (month != null) {
            AnimatedContent(targetState = month, label = "month") { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    StreakCalendar(shown, ui.today, ui.firstDayOfWeek)
                    FlowRow(
                        Modifier.semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                    ) {
                        Text(pluralStringResource(R.plurals.streak_month_practice_count, shown.practiceDays, shown.practiceDays), style = ViraFont.footnote, color = ViraColor.textSecondary)
                        Text(pluralStringResource(R.plurals.streak_month_saved_count, shown.protectedDays.size, shown.protectedDays.size), style = ViraFont.footnote, color = ViraColor.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthButton(direction: ChevronDirection, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) ViraColor.accent(ViraArea.Rewards) else ViraColor.textSecondary
    Box(
        Modifier
            .viraTouchTarget()
            .viraPressable(onClickLabel = null, enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Chevron(direction, tint, size = 18.dp)
    }
}
