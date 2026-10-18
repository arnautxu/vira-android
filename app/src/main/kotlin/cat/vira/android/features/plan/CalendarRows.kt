package cat.vira.android.features.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.annotation.DrawableRes
import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.LocalActiveCompletion
import cat.vira.android.core.design.components.LocalUndoCompletion
import cat.vira.android.core.design.components.ViraCompletionCheck
import cat.vira.android.core.design.components.ViraInlineCompletion
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val area = ViraArea.Plan

/// Fila del calendari dins de la targeta de la seva franja (`CalendarRow`): hora a l'esquerra en
/// una columna fixa, pictograma, títol i casella. Tocar la fila sencera és "fet"; editar i esborrar
/// van per lliscar (i per les accions de TalkBack).
///
/// Mentre dura la finestra de "Desfer", el títol i la casella es converteixen en "Fet · Desfer" i
/// després en la revelació; l'hora i el pictograma es queden per saber de quina fila es tracta.
@Composable
fun CalendarRow(item: PlanItem, onToggle: () -> Unit, modifier: Modifier = Modifier, zone: ZoneId = ZoneId.systemDefault()) {
    val id = PlanCompletionIds.item(item)
    val active = LocalActiveCompletion.current
    val undo = LocalUndoCompletion.current
    val phase = active?.takeIf { it.id == id }?.phase
    val isDone = item.status == PlanItemStatus.DONE
    val ink = ViraColor.onCard(area)
    val scale = LocalDensity.current.fontScale
    val iconAlpha by animateFloatAsState(if (isDone) 0.5f else 1f, ViraMotion.quick(), label = "calendarRowIcon")
    val markLabel = stringResource(if (isDone) R.string.a11y_mark_pending else R.string.a11y_mark_done)

    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable)
            .viraPressable(onClickLabel = markLabel, onClick = onToggle)
            .padding(vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        TimeLabel(item, ink, Modifier.width(46.dp * scale), zone)
        ViraPictogram(item.symbolName, Modifier.alpha(iconAlpha).clearAndSetSemantics { }, size = 34.dp * scale)
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.alpha(if (phase == null) 1f else 0f)
                    .then(if (phase != null) Modifier.clearAndSetSemantics { } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                Text(
                    item.title,
                    style = ViraFont.callout.copy(textDecoration = if (isDone) TextDecoration.LineThrough else null),
                    color = ink,
                    modifier = Modifier.weight(1f),
                )
                ViraCompletionCheck(isDone, Modifier.clearAndSetSemantics { }, ink = ink)
            }
            if (phase != null) ViraInlineCompletion(phase, ink, onUndo = { undo(id) })
        }
    }
}

/// Sense hora, un guió: la columna no desapareix i els títols queden alineats.
@Composable
private fun TimeLabel(item: PlanItem, ink: Color, modifier: Modifier, zone: ZoneId) {
    val time = item.time
    if (time != null) {
        Text(
            PlanFormat.time(time.atZone(zone).toLocalTime()),
            style = ViraFont.footnoteEmphasis,
            color = ink,
            maxLines = 1,
            modifier = modifier,
        )
    } else {
        Text("–", style = ViraFont.footnote, color = ink, maxLines = 1, modifier = modifier.clearAndSetSemantics { })
    }
}

/// "Afegir aquí" a l'última fila de cada franja (`AddSlotRow`, variant compacta): la franja ja
/// la diu la capçalera de la targeta; TalkBack sí que la sent, perquè hi arriba fila a fila.
@Composable
fun AddSlotRow(period: DayPeriod, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.calendar_add_here)
    val spoken = "$label, ${stringResource(period.title)}"
    val ink = ViraColor.onCard(area)
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken; role = Role.Button }
            .padding(vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        PlanGlyph(PlanGlyphShape.Plus, ink, size = 16.dp * LocalDensity.current.fontScale, weight = 0.16f)
        Text(label, style = ViraFont.callout, color = ink)
    }
}

/// Fila de la vista setmanal (`PlanItemRow`): casella, hora i tipus en text (mai només per color),
/// títol i notes.
@Composable
fun PlanItemRow(item: PlanItem, onToggle: () -> Unit, modifier: Modifier = Modifier, zone: ZoneId = ZoneId.systemDefault()) {
    val id = PlanCompletionIds.item(item)
    val active = LocalActiveCompletion.current
    val undo = LocalUndoCompletion.current
    val phase = active?.takeIf { it.id == id }?.phase
    val isDone = item.status == PlanItemStatus.DONE
    val markLabel = stringResource(if (isDone) R.string.a11y_mark_pending else R.string.a11y_mark_done)
    Box(
        modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable)
            .viraPressable(onClickLabel = markLabel, onClick = onToggle)
            .padding(vertical = ViraSpacing.xxs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.alpha(if (phase == null) 1f else 0f).then(if (phase != null) Modifier.clearAndSetSemantics { } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViraCompletionCheck(
                isDone,
                Modifier.clearAndSetSemantics { },
                doneTint = ViraColor.success,
                pendingTint = ViraColor.textSecondary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val meta = listOfNotNull(
                    item.time?.let { PlanFormat.time(it.atZone(zone).toLocalTime()) },
                    stringResource(if (item.time != null) R.string.type_event else R.string.type_task),
                ).joinToString(" ")
                Text(meta, style = ViraFont.footnote, color = ViraColor.textSecondary)
                Text(
                    item.title,
                    style = ViraFont.headline.copy(textDecoration = if (isDone) TextDecoration.LineThrough else null),
                    color = ViraColor.textPrimary,
                )
                item.notes?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = ViraFont.footnote, color = ViraColor.textSecondary)
                }
            }
        }
        if (phase != null) ViraInlineCompletion(phase, ViraColor.textPrimary, onUndo = { undo(id) })
    }
}

/// Lliscar la fila cap a l'esquerra descobreix **Editar** i **Esborra** (`swipeActions`), i
/// TalkBack les ofereix com a accions de la fila, que és per què no calen botons visibles.
/// Sense diàleg de confirmació: lliscar ja és deliberat.
@Composable
fun SwipeActionsRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val editLabel = stringResource(R.string.plan_edit)
    val deleteLabel = stringResource(R.string.plan_delete)
    val density = LocalDensity.current
    val revealWidth = with(density) { (ViraTouch.comfortable * 2 * density.fontScale.coerceAtMost(1.6f) + 40.dp).toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val shown by animateFloatAsState(offset, ViraMotion.quick(), label = "swipeReveal")
    fun close() { offset = 0f }

    Box(
        modifier.fillMaxWidth().semantics {
            customActions = listOf(
                CustomAccessibilityAction(editLabel) { onEdit(); true },
                CustomAccessibilityAction(deleteLabel) { onDelete(); true },
            )
        },
    ) {
        if (shown < 0f) {
            Row(Modifier.align(Alignment.CenterEnd).clearAndSetSemantics { }) {
                SwipeButton(editLabel, ViraColor.accent(area)) { close(); onEdit() }
                SwipeButton(deleteLabel, ViraColor.error) { close(); onDelete() }
            }
        }
        Box(
            Modifier.offset { IntOffset(shown.roundToInt(), 0) }
                .pointerInput(revealWidth) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offset = if (offset < -revealWidth / 2) -revealWidth else 0f },
                        onDragCancel = { close() },
                    ) { _, delta -> offset = (offset + delta).coerceIn(-revealWidth, 0f) }
                },
        ) { content() }
    }
}

@Composable
private fun SwipeButton(label: String, fill: Color, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(minWidth = ViraTouch.comfortable, minHeight = ViraTouch.comfortable)
            .background(fill)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = ViraFont.footnoteEmphasis, color = ViraColor.onAccent)
    }
}

/// La targeta blava d'una franja (`insetGrouped` amb `planGroupRow`): fons de Pla i separador del
/// to del text, com el mòdul del dia d'Inici. Translúcida si la franja és buida.
@Composable
fun PlanGroupCard(muted: Boolean = false, rows: List<@Composable () -> Unit>) {
    val shape = RoundedCornerShape(ViraRadius.s)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(ViraColor.cardFill(area).copy(alpha = if (muted) 0.4f else 1f), shape),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.onCard(area).copy(alpha = 0.25f))
            }
            Box(Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.xs, top = ViraSpacing.xxs, bottom = ViraSpacing.xxs)) {
                row()
            }
        }
    }
}

/// Capçalera petita en majúscules d'una franja o d'un dia.
@Composable
fun PlanGroupHeader(text: String, uppercase: Boolean = true) {
    Text(
        if (uppercase) text.uppercase() else text,
        style = if (uppercase) ViraFont.footnoteEmphasis else ViraFont.footnote,
        color = ViraColor.textSecondary,
        modifier = Modifier.padding(start = ViraSpacing.m, bottom = ViraSpacing.xxs).semantics { heading() },
    )
}

/// Selector horitzontal de dies (`DayStrip`): la pastilla groga marca el dia triat, un anell el
/// d'avui i un punt si el dia té coses, **acompanyat del recompte** a TalkBack.
@Composable
fun DayStrip(
    selection: LocalDate,
    week: CalendarWeek,
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(ViraColor.cardFill(area).copy(alpha = 0.55f), CircleShape).padding(ViraSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        week.days.forEach { day ->
            DayCell(day, isSelected = day == selection, isToday = day == today, count = counts[day] ?: 0, Modifier.weight(1f)) {
                onSelect(day)
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, isSelected: Boolean, isToday: Boolean, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val locale = planLocale()
    val ink by animateColorAsState(
        if (isSelected) ViraColor.onCard(ViraArea.Rewards) else ViraColor.onCard(area),
        ViraMotion.quick(),
        label = "dayInk",
    )
    val fill by animateColorAsState(
        if (isSelected) ViraColor.cardFill(ViraArea.Rewards) else Color.Transparent,
        ViraMotion.quick(),
        label = "dayFill",
    )
    val dateText = day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
    val items = if (count == 0) stringResource(R.string.a11y_items_count_zero) else pluralStringResource(R.plurals.a11y_items_count, count, count)
    val spoken = listOfNotNull(stringResource(R.string.a11y_today).takeIf { isToday }, dateText, items).joinToString(", ")
    val todayRing = ViraColor.controlBorder(on = ViraColor.onCard(area))
    val dot = 5.dp * LocalDensity.current.fontScale

    Column(
        modifier.defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(CircleShape)
            .background(fill, CircleShape)
            .then(if (isToday && !isSelected) Modifier.border(2.dp, todayRing, CircleShape) else Modifier)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken; role = Role.Button; selected = isSelected }
            .padding(vertical = ViraSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = ViraFont.caption, color = ink.copy(alpha = 0.75f))
        Text("${day.dayOfMonth}", style = ViraFont.headline, color = ink)
        Box(Modifier.size(dot).alpha(if (count > 0) 1f else 0f).background(ink, CircleShape))
    }
}

/// Escena de Viri amb una bombolla que trepitja la vora (`RewardsSceneCard`). Provisional: quan
/// Viri i ratxa en publiqui la de tota l'app, aquesta marxa.
@Composable
fun PlanSceneCard(
    @DrawableRes scene: Int,
    message: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val spoken = stringResource(R.string.today_viri_a11y, message)
    Column(
        modifier.fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = ViraColor.accent(area).copy(alpha = 0.18f), spotColor = ViraColor.accent(area).copy(alpha = 0.18f))
            .clip(shape)
            .background(ViraGradient.card(area), shape),
    ) {
        Image(
            painterResource(scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(if (isLarge) 120.dp else 184.dp).semantics { invisibleToUser() },
        )
        Column(
            Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.m, bottom = ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Text(
                message,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.pullUp(ViraSpacing.xxl)
                    .fillMaxWidth()
                    .shadow(10.dp, shape, ambientColor = ViraColor.accent(area).copy(alpha = 0.16f), spotColor = ViraColor.accent(area).copy(alpha = 0.16f))
                    .background(ViraColor.surfaceElevated, shape)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                    .clearAndSetSemantics { contentDescription = spoken },
            )
            content()
        }
    }
}

/// Mes i any, amb la primera lletra en majúscula (`.dateTime.month(.wide).year()`).
@Composable
@ReadOnlyComposable
fun monthYear(date: LocalDate, locale: Locale = planLocale()): String =
    date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

/// "dilluns 5" per a les capçaleres de la setmana.
@Composable
@ReadOnlyComposable
fun weekdayDay(date: LocalDate, locale: Locale = planLocale()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE d", locale)).replaceFirstChar { it.titlecase(locale) }

/// Puja el contingut `distance` per sobre del seu lloc i li treu aquella alçada (el `.padding(.top, -x)`
/// de SwiftUI): la bombolla trepitja la vora de l'escena sense deixar un forat a sota.
fun Modifier.pullUp(distance: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = distance.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}
