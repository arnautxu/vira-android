package cat.vira.android.features.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val area = ViraArea.Plan

/// «Sense hora» o «A una hora» i, amb hora, el rellotge de roda (`QuickCaptureClock`, Arnau,
/// 2026-10-24: «m'agrada més el 2»). Dues rodes, l'hora i els minuts de 5 en 5, com les de l'iPhone:
/// el `TimePicker` de Material és un rellotge rodó, que l'Arnau va descartar a la maqueta.
@Composable
fun AddClock(hasTime: Boolean, minutes: Int, onNoTime: () -> Unit, onTimed: () -> Unit, onChange: (Int) -> Unit) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        val options = Modifier.fillMaxWidth().background(ViraColor.surface(area), RoundedCornerShape(ViraRadius.m)).padding(ViraSpacing.xxs)
        val none = @Composable { modifier: Modifier -> ClockOption(ViraPictogramName.calToday, stringResource(R.string.capture_time_none), !hasTime, modifier, onNoTime) }
        val timed = @Composable { modifier: Modifier -> ClockOption(ViraPictogramName.planAlarm, stringResource(R.string.add_task_when_at_time), hasTime, modifier, onTimed) }
        if (stacked) {
            Column(options, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) { none(Modifier.fillMaxWidth()); timed(Modifier.fillMaxWidth()) }
        } else {
            Row(options, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) { none(Modifier.weight(1f)); timed(Modifier.weight(1f)) }
        }
        AnimatedVisibility(hasTime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(
                    values = (0 until 24).toList(),
                    selected = minutes / 60,
                    label = stringResource(R.string.add_task_when_hour),
                    onSelect = { onChange(it * 60 + minutes % 60) },
                )
                Text(":", style = ViraFont.title, color = ViraColor.textPrimary, modifier = Modifier.clearAndSetSemantics { })
                // Una hora de la frase com les 17:07 es veu a les 17:05; només canvia si es mou la roda.
                WheelPicker(
                    values = (0 until 60 step 5).toList(),
                    selected = minutes % 60 / 5 * 5,
                    label = stringResource(R.string.add_task_when_minute),
                    onSelect = { onChange(minutes / 60 * 60 + it) },
                )
            }
        }
    }
}

@Composable
private fun ClockOption(pictogram: String, title: String, isOn: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.s)
    Row(
        modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .then(if (isOn) Modifier.shadow(4.dp, shape, spotColor = ViraColor.accent(area).copy(alpha = 0.16f)).background(ViraColor.surfaceElevated, shape) else Modifier)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isOn }
            .padding(horizontal = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
    ) {
        ViraPictogram(pictogram, size = 22.dp, scalesWithText = true)
        Text(title, style = ViraFont.headline, color = if (isOn) ViraColor.textPrimary else ViraColor.textSecondary)
    }
}

/// Una roda de números que s'atura sempre en un (`Picker` amb `.wheel` de l'iPhone). Es fa girar o
/// es toca un número; amb TalkBack, les accions «més» i «menys» la mouen d'un en un.
@Composable
private fun WheelPicker(values: List<Int>, selected: Int, label: String, onSelect: (Int) -> Unit) {
    val itemHeight: Dp = 40.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    val visible = 5
    val index = values.indexOf(selected).coerceAtLeast(0)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    val current by rememberUpdatedState(selected)
    // El que ve de fora (la frase, una franja) mou la roda; el que la roda tria puja cap amunt.
    LaunchedEffect(index) {
        if (!list.isScrollInProgress && list.firstVisibleItemIndex != index) list.scrollToItem(index)
    }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress to list.firstVisibleItemIndex }
            .drop(1)
            .distinctUntilChanged()
            .collect { (scrolling, first) ->
                if (!scrolling) values.getOrNull(first)?.takeIf { it != current }?.let(select)
            }
    }
    val text = remember(selected) { "%02d".format(selected) }
    val actions = listOf(
        CustomAccessibilityAction("+") { values.getOrNull(index + 1)?.let(select); true },
        CustomAccessibilityAction("−") { values.getOrNull(index - 1)?.let(select); true },
    )
    Box(
        Modifier
            .width(88.dp)
            .height(itemHeight * visible)
            .semantics {
                contentDescription = label
                stateDescription = text
                customActions = actions
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(itemHeight).background(ViraColor.surface(area), RoundedCornerShape(ViraRadius.s)))
        LazyColumn(
            state = list,
            flingBehavior = rememberSnapFlingBehavior(list),
            contentPadding = PaddingValues(vertical = itemHeight * (visible / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        ) {
            items(values.size) { position ->
                val value = values[position]
                Box(
                    Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .viraPressable(role = null) { scope.launch { list.animateScrollToItem(position) }; select(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "%02d".format(value),
                        style = if (value == selected) ViraFont.title else ViraFont.body,
                        color = if (value == selected) ViraColor.textPrimary else ViraColor.textSecondary,
                    )
                }
            }
        }
    }
}
