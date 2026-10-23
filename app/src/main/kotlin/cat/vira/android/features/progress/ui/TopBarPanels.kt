package cat.vira.android.features.progress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.app.EmotionsSection
import cat.vira.android.app.LocalViraNavigator
import cat.vira.android.app.PlanSection
import cat.vira.android.app.ViraDestination
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTopBarFlame
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.progress.logic.StreakRules.DayRequirement
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.logic.ViraDays
import cat.vira.android.features.progress.logic.ViraProgress
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

// Els panells que baixen de la capçalera en tocar la ratxa o les monedes (`ViraTopBarPanels` de
// l'iPhone, Arnau, 2026-10-22). Diuen el que el número sol no pot dir i ofereixen un sol pas: anar a
// la pantalla de Ratxa o a la Botiga. El globus o el full els posa `ViraTopBar`.

/// El que encara li queda avui a la ratxa, la setmana, els dies que Viri pot salvar i el camí a la
/// pantalla de Ratxa. Només les coses que falten, cadascuna amb el camí on es fa; quan avui ja suma,
/// no n'hi ha cap.
@Composable
fun ViraTopBarStreakPanel(countsToday: Boolean, dismiss: () -> Unit) {
    val backend = LocalViraBackend.current
    val navigator = LocalViraNavigator.current
    val firstDay = WeekFields.of(viraLocale()).firstDayOfWeek
    val progress = remember(backend, firstDay) { ViraProgress(ViraStore(backend.db), firstDayOfWeek = { firstDay }) }
    val streak by backend.db.streaks().observe().collectAsStateWithLifecycle(null)
    val missing by produceState<Set<DayRequirement>?>(null) { value = progress.missingRequirements() }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = streak?.currentDays ?: 0

    StreakPanelContent(
        days = days,
        countsToday = countsToday,
        missing = missing,
        week = StreakWeek.of(streak, today, ViraDays(zone, firstDay)),
        today = today,
        firstDayOfWeek = firstDay,
        savedDays = streak?.freezesAvailable ?: 0,
        onGo = { requirement ->
            dismiss()
            navigator.navigate(requirement.destination)
        },
        onOpenStreak = {
            dismiss()
            navigator.navigate(ViraDestination.Streak)
        },
    )
}

/// La part sense estat, per a les captures.
@Composable
internal fun StreakPanelContent(
    days: Int,
    countsToday: Boolean,
    missing: Set<DayRequirement>?,
    week: StreakWeek,
    today: LocalDate,
    firstDayOfWeek: java.time.DayOfWeek,
    savedDays: Int,
    onGo: (DayRequirement) -> Unit,
    onOpenStreak: () -> Unit,
) {
    val title = pluralStringResource(R.plurals.topbar_streak_title, days, days)
    val state = stringResource(if (countsToday) R.string.topbar_streak_today_done else R.string.topbar_streak_today_pending)
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$title. $state" },
        ) {
            ViraTopBarFlame(36.dp, isLit = countsToday)
            Column {
                Text(title, style = ViraFont.title, color = ViraColor.textPrimary)
                Text(state, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }

        if (!missing.isNullOrEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                Text(
                    stringResource(R.string.topbar_streak_pending),
                    style = ViraFont.headline,
                    color = ViraColor.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                DayRequirement.entries.filter { it in missing }.forEach { requirement ->
                    RequirementRow(requirement) { onGo(requirement) }
                }
            }
        }

        StreakWeekCompact(week, today, firstDayOfWeek)

        Row(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(ViraColor.backgroundGrouped, RoundedCornerShape(ViraRadius.m))
                .padding(ViraSpacing.s),
        ) {
            ViraPictogram(R.drawable.picto_shield, size = 28.dp)
            Text(stringResource(R.string.streak_saved_days, savedDays), style = ViraFont.callout, color = ViraColor.textPrimary)
        }

        ViraActionButton(
            text = stringResource(R.string.topbar_streak_open),
            onClick = onOpenStreak,
            hierarchy = ViraActionHierarchy.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/// Una de les coses que encara li queden avui a la ratxa. Tota la fila porta on es fa
/// (`ViraStreakRequirementRow`).
@Composable
private fun RequirementRow(requirement: DayRequirement, onGo: () -> Unit) {
    val title = stringResource(requirement.title)
    val go = stringResource(R.string.topbar_streak_go)
    val shape = RoundedCornerShape(ViraRadius.m)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum + ViraSpacing.xs)
            .clip(shape)
            .background(ViraColor.backgroundGrouped, shape)
            .viraPressable(onClickLabel = go, onClick = onGo)
            .clearAndSetSemantics {
                contentDescription = title
                role = Role.Button
                onClick(go) { onGo(); true }
            }
            .padding(start = ViraSpacing.xs, end = ViraSpacing.s),
    ) {
        ViraPictogram(requirement.pictogram, size = 32.dp)
        Text(title, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(ViraSpacing.xs))
        Text(go, style = ViraFont.footnoteEmphasis, color = ViraColor.brandAccent)
        Chevron(ChevronDirection.Right, ViraColor.brandAccent, size = 12.dp)
    }
}

private val DayRequirement.title: Int
    get() = when (this) {
        DayRequirement.HABIT -> R.string.today_streak_req_habit
        DayRequirement.EMOTION -> R.string.today_streak_req_emotion
        DayRequirement.TASK -> R.string.today_streak_req_task
    }

private val DayRequirement.pictogram: Int
    get() = when (this) {
        DayRequirement.HABIT -> R.drawable.picto_habits
        DayRequirement.EMOTION -> R.drawable.viri_phone
        DayRequirement.TASK -> R.drawable.picto_flag
    }

/// On es fa cada cosa. La tasca, a l'agenda de Pla.
private val DayRequirement.destination: ViraDestination
    get() = when (this) {
        DayRequirement.HABIT -> ViraDestination.Plan(PlanSection.Habits)
        DayRequirement.EMOTION -> ViraDestination.Emotions(EmotionsSection.CheckIn)
        DayRequirement.TASK -> ViraDestination.Plan(PlanSection.Calendar)
    }

/// El saldo sencer, d'on surt i el camí a la Botiga.
@Composable
fun ViraTopBarCoinsPanel(coins: Int, dismiss: () -> Unit) {
    val navigator = LocalViraNavigator.current
    CoinsPanelContent(coins) {
        dismiss()
        navigator.navigate(ViraDestination.Shop)
    }
}

@Composable
internal fun CoinsPanelContent(coins: Int, onOpenShop: () -> Unit) {
    val title = stringResource(R.string.topbar_coins_title, NumberFormat.getIntegerInstance(viraLocale()).format(coins))
    val earn = stringResource(R.string.topbar_coins_earn)
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$title. $earn" },
        ) {
            ViraPictogram(R.drawable.picto_shop, size = 36.dp)
            Column {
                Text(title, style = ViraFont.title, color = ViraColor.textPrimary)
                Text(earn, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        ViraActionButton(
            text = stringResource(R.string.topbar_coins_open),
            onClick = onOpenShop,
            area = ViraArea.Today,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
