package cat.vira.android.features.add

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.plan.PlanDay
import cat.vira.android.features.plan.WeekdaySelection
import java.time.format.TextStyle
import java.util.Locale

@get:StringRes
val WeekdayPreset.title: Int
    get() = when (this) {
        WeekdayPreset.Everyday -> R.string.habits_when_everyday
        WeekdayPreset.Weekdays -> R.string.habits_when_weekdays
        WeekdayPreset.Weekend -> R.string.habits_when_weekend
    }

/// Els dies d'un hàbit dits en paraules (`HabitDays.summary`): «Cada dia», «Entre setmana» o «dl, dc, dv».
@Composable
fun weekdaysSummary(weekdays: Set<Int>): String {
    WeekdayPreset.matching(weekdays)?.let { return stringResource(it.title) }
    val locale = viraLocale()
    return WeekdaySelection.ordered(PlanDay.firstDayOfWeek(locale)).filter { it in weekdays }.joinToString(", ") { shortSymbol(it, locale) }
}

/// Inicial curta del dia per al cercle («dl», «dt»…). El nom sencer és per a TalkBack.
private fun shortSymbol(weekday: Int, locale: Locale): String =
    WeekdaySelection.dayOfWeek(weekday).getDisplayName(TextStyle.SHORT_STANDALONE, locale).trim('.')

/// Set cercles en una fila, un per dia (`WeekdayPicker`, Arnau, 2026-10-18), i les tres dreceres a
/// sota si cal. El triat porta marca a més del color. Quan no hi caben, salten de línia sense
/// baixar dels 48 dp. El conjunt buit vol dir «cada dia».
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWeekdayPicker(weekdays: Set<Int>, onChange: (Set<Int>) -> Unit, showsPresets: Boolean = true, area: ViraArea = ViraArea.Plan) {
    val locale = viraLocale()
    val ordered = WeekdaySelection.ordered(PlanDay.firstDayOfWeek(locale))
    val active = weekdays.ifEmpty { (1..7).toSet() }
    Column(
        Modifier.background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)).padding(ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ordered.forEach { weekday ->
                val isOn = weekday in active
                val name = WeekdaySelection.dayOfWeek(weekday).getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    .replaceFirstChar { it.titlecase(locale) }
                Box(
                    Modifier
                        .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
                        .viraPressable(role = Role.Button) {
                            val next = WeekdaySelection.toggling(weekday, !isOn, active)
                            onChange(if (next.size == 7) emptySet() else next)
                        }
                        .semantics(mergeDescendants = true) { contentDescription = name; selected = isOn },
                ) {
                    Box(
                        Modifier
                            .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
                            .background(if (isOn) ViraColor.accent(area) else ViraColor.surfaceElevated, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            shortSymbol(weekday, locale),
                            style = ViraFont.footnoteEmphasis,
                            color = if (isOn) ViraColor.onAccent else ViraColor.textSecondary,
                            modifier = Modifier.padding(horizontal = ViraSpacing.xxs),
                        )
                    }
                    if (isOn) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = ViraColor.accent(area),
                            modifier = Modifier.align(Alignment.TopEnd).offset(ViraSpacing.xxs, -ViraSpacing.xxs).size(14.dp)
                                .background(ViraColor.surfaceElevated, CircleShape),
                        )
                    }
                }
            }
        }
        if (showsPresets) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                WeekdayPreset.entries.forEach { preset ->
                    val isOn = preset.days == weekdays
                    Text(
                        stringResource(preset.title),
                        style = ViraFont.footnoteEmphasis,
                        color = ViraColor.onSurface(area),
                        modifier = Modifier
                            .defaultMinSize(minHeight = ViraTouch.minimum)
                            .background(ViraColor.surface(area), CircleShape)
                            .then(if (isOn) Modifier.border(2.dp, ViraColor.accent(area), CircleShape) else Modifier)
                            .viraPressable(role = Role.Button) { onChange(preset.days) }
                            .semantics { selected = isOn }
                            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.s),
                    )
                }
            }
        }
    }
}
