package cat.vira.android.features.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cat.vira.android.R
import cat.vira.android.app.PlanSection
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.features.entry.ViraTip
import cat.vira.android.features.entry.ViraTipRow
import cat.vira.android.features.progress.RewardsSceneCard
import cat.vira.android.features.today.QuickCapturePeriod
import cat.vira.android.features.today.QuickCaptureScreen
import java.time.LocalDate
import java.time.ZoneId

/// Calendari (`CalendarView`): vista de dia per defecte i setmana com a llista agrupada.
///
/// - **La vista diària és la per defecte**: horitzó curt, que és el que funciona en TDAH.
/// - **Una targeta per franja** (Matí, Migdia, Nit) amb "Afegir aquí" sempre a l'última fila.
/// - **Un toc és "fet"**; editar i esborrar van per lliscar, i reendreçar pel botó de la barra.
/// - **Reendreçant, el dia es veu pla**: sense franges, perquè una fila es pugui deixar a
///   qualsevol lloc, i la franja on acaba surt de la posició (`PlanItemOrder.period`).
@Composable
fun CalendarSection(
    snapshot: PlanSnapshot,
    actions: PlanActions,
    chrome: PlanChrome,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    initialMode: CalendarMode = CalendarMode.Day,
) {
    var selection by rememberSaveable { mutableStateOf(today) }
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    val locale = viraLocale()
    val firstDay = PlanDay.firstDayOfWeek(locale)
    val week = CalendarWeek.containing(selection, firstDay)
    val counts = remember(snapshot.items) { snapshot.countsByDay(zone) }

    val reorderable = when (mode) {
        CalendarMode.Day -> snapshot.items(selection, zone).size > 1
        CalendarMode.Week -> week.days.any { snapshot.items(it, zone).size > 1 }
    }
    LaunchedEffect(reorderable) { chrome.canReorder = reorderable; if (!reorderable) chrome.reordering = false }
    // El mode de reendreçar és del dia que s'està mirant: canviar de dia o de vista el tanca.
    LaunchedEffect(selection, mode) { chrome.reordering = false }
    // El "+" de la barra obre la captura ràpida del dia triat.
    LaunchedEffect(chrome.adding) {
        if (chrome.adding == PlanSection.Calendar) {
            chrome.adding = null
            actions.capture(selection, null)
        }
    }

    Column(Modifier.fillMaxSize()) {
        CalendarHeader(selection, week, counts, today, mode, onSelect = { selection = it }, onMode = { mode = it })
        HorizontalDivider(color = ViraColor.separator)
        ViraTipRow(ViraTip.PLAN_ADD, Modifier.padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs))
        when (mode) {
            CalendarMode.Day -> DayContent(
                items = snapshot.items(selection, zone),
                reordering = chrome.reordering,
                actions = actions,
                onAdd = { period -> actions.capture(selection, period) },
                onEdit = { editing = it },
                zone = zone,
            )
            CalendarMode.Week -> WeekContent(week, snapshot, chrome.reordering, actions, onEdit = { editing = it }, zone)
        }
    }

    editing?.let { item -> PlanItemEditor(item, actions, onDismiss = { editing = null }) }
    // El "+" i "Afegir aquí" obren la captura ràpida d'Avui amb el dia i la franja ja posats.
    chrome.capture?.let { request ->
        QuickCaptureScreen(
            onDismiss = { chrome.capture = null },
            initialDay = PlanDay.start(request.day, zone),
            initialPeriod = request.period?.let { QuickCapturePeriod.valueOf(it.name) },
        )
    }
}

enum class CalendarMode(val title: Int) { Day(R.string.calendar_mode_day), Week(R.string.calendar_mode_week) }

@Composable
private fun CalendarHeader(
    selection: LocalDate,
    week: CalendarWeek,
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    mode: CalendarMode,
    onSelect: (LocalDate) -> Unit,
    onMode: (CalendarMode) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(ViraColor.surface(ViraArea.Plan))
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mes i any sempre visibles: al prototip no quedava clar on eres.
            Text(monthYear(selection), style = ViraFont.title, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
            WeekButton(PlanGlyphShape.ChevronLeft, stringResource(R.string.calendar_previous_week)) { onSelect(selection.minusWeeks(1)) }
            val isToday = selection == today
            Box(
                Modifier.defaultMinSize(minHeight = ViraTouch.minimum)
                    .viraPressable(enabled = !isToday) { onSelect(today) }
                    .padding(horizontal = ViraSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.calendar_today),
                    style = ViraFont.callout,
                    color = if (isToday) ViraColor.textSecondary else ViraColor.accent(ViraArea.Plan),
                )
            }
            WeekButton(PlanGlyphShape.ChevronRight, stringResource(R.string.calendar_next_week)) { onSelect(selection.plusWeeks(1)) }
        }
        DayStrip(selection, week, counts, today, onSelect)
        PlanSegmented(
            options = CalendarMode.entries.map { it to stringResource(it.title) },
            selected = mode,
            onSelect = onMode,
        )
    }
}

@Composable
private fun WeekButton(shape: PlanGlyphShape, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(ViraTouch.minimum).viraPressable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        PlanGlyph(shape, ViraColor.accent(ViraArea.Plan), size = 18.dp, weight = 0.14f)
    }
}

@Composable
private fun DayContent(
    items: List<PlanItem>,
    reordering: Boolean,
    actions: PlanActions,
    onAdd: (DayPeriod) -> Unit,
    onEdit: (PlanItem) -> Unit,
    zone: ZoneId,
) {
    PlanScrollColumn {
        if (reordering) {
            ReorderList(items, actions, zone)
        } else {
            if (items.isEmpty()) {
                RewardsSceneCard(R.drawable.viri_scene_calendar, stringResource(R.string.plan_day_empty_viri), area = ViraArea.Plan) {}
            }
            DayPeriod.entries.forEach { period -> PeriodSection(period, items, actions, onAdd, onEdit, zone) }
        }
    }
}

@Composable
private fun PeriodSection(
    period: DayPeriod,
    dayItems: List<PlanItem>,
    actions: PlanActions,
    onAdd: (DayPeriod) -> Unit,
    onEdit: (PlanItem) -> Unit,
    zone: ZoneId,
) {
    val items = dayItems.filter { PlanItemOrder.period(it, zone) == period }
    Column {
        PlanGroupHeader(stringResource(period.title))
        PlanGroupCard(
            muted = items.isEmpty(),
            rows = items.map { item ->
                @Composable {
                    SwipeActionsRow(onEdit = { onEdit(item) }, onDelete = { actions.delete(item) }) {
                        CalendarRow(item, onToggle = { actions.toggle(item) }, zone = zone)
                    }
                }
            } + listOf(@Composable { AddSlotRow(period, onClick = { onAdd(period) }) }),
        )
    }
}

/// El dia sencer en una sola targeta: mantenir premuda una fila i arrossegar-la la deixa on sigui.
@Composable
private fun ReorderList(items: List<PlanItem>, actions: PlanActions, zone: ZoneId, showHint: Boolean = true) {
    var order by remember(items) { mutableStateOf(items) }
    var dragging by remember { mutableStateOf<java.util.UUID?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var startIndex by remember { mutableIntStateOf(0) }
    val heights = remember { mutableStateMapOf<java.util.UUID, Int>() }
    val shape = RoundedCornerShape(ViraRadius.s)
    val upLabel = stringResource(R.string.plan_reorder_move_up)
    val downLabel = stringResource(R.string.plan_reorder_move_down)
    // TalkBack no pot arrossegar: cada fila ofereix "Mou amunt" i "Mou avall".
    fun step(index: Int, delta: Int) {
        val target = index + delta
        if (target !in order.indices) return
        actions.move(PlanItemOrder.move(order, index, target, zone))
    }

    Column {
        if (showHint) ReorderHint()
        Column(Modifier.fillMaxWidth().background(ViraColor.cardFill(ViraArea.Plan), shape)) {
            order.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.onCard(ViraArea.Plan).copy(alpha = 0.25f))
                val isDragged = dragging == item.id
                Box(
                    Modifier.fillMaxWidth()
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                        .then(if (isDragged) Modifier.shadow(8.dp, shape).background(ViraColor.cardFill(ViraArea.Plan), shape) else Modifier)
                        .onSizeChanged { heights[item.id] = it.height }
                        .semantics {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(upLabel) { step(index, -1); true }.takeIf { index > 0 },
                                CustomAccessibilityAction(downLabel) { step(index, 1); true }.takeIf { index < order.lastIndex },
                            )
                        }
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = item.id
                                    dragOffset = 0f
                                    startIndex = order.indexOfFirst { it.id == item.id }
                                },
                                onDragEnd = {
                                    val from = startIndex
                                    val to = order.indexOfFirst { it.id == item.id }
                                    dragging = null
                                    dragOffset = 0f
                                    if (from != to) actions.move(PlanItemOrder.move(items, from, to, zone))
                                },
                                onDragCancel = { dragging = null; dragOffset = 0f; order = items },
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                // En passar la meitat de la fila veïna, s'intercanvien.
                                val current = order.indexOfFirst { it.id == item.id }
                                val next = order.getOrNull(current + 1)
                                val previous = order.getOrNull(current - 1)
                                if (next != null && dragOffset > (heights[next.id] ?: 0) / 2f) {
                                    dragOffset -= heights[next.id] ?: 0
                                    order = order.toMutableList().apply { add(current + 1, removeAt(current)) }
                                } else if (previous != null && dragOffset < -(heights[previous.id] ?: 0) / 2f) {
                                    dragOffset += heights[previous.id] ?: 0
                                    order = order.toMutableList().apply { add(current - 1, removeAt(current)) }
                                }
                            }
                        }
                        .padding(start = ViraSpacing.m, end = ViraSpacing.xs, top = ViraSpacing.xxs, bottom = ViraSpacing.xxs),
                ) {
                    CalendarRow(item, onToggle = { actions.toggle(item) }, zone = zone)
                }
            }
        }
    }
}

@Composable
private fun ReorderHint() {
    Text(
        stringResource(R.string.plan_reorder_hint),
        style = ViraFont.footnote,
        color = ViraColor.textSecondary,
        modifier = Modifier.padding(start = ViraSpacing.m, bottom = ViraSpacing.xs),
    )
}

/// Setmanal com a llista agrupada per dia, no com a graella: la graella era hostil a TalkBack i
/// no tenia model per a més d'un ítem per franja.
@Composable
private fun WeekContent(
    week: CalendarWeek,
    snapshot: PlanSnapshot,
    reordering: Boolean,
    actions: PlanActions,
    onEdit: (PlanItem) -> Unit,
    zone: ZoneId,
) {
    PlanScrollColumn {
        if (reordering) ReorderHint()
        week.days.forEach { day ->
            key(day) {
                val items = snapshot.items(day, zone)
                Column {
                    PlanGroupHeader(weekdayDay(day), uppercase = false)
                    val shape = RoundedCornerShape(ViraRadius.s)
                    // Reendreçar la setmana és reendreçar cada dia per separat, com a l'iPhone.
                    if (reordering && items.size > 1) ReorderList(items, actions, zone, showHint = false)
                    else Column(
                        Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, shape)
                            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs),
                    ) {
                        if (items.isEmpty()) {
                            Text(
                                stringResource(R.string.calendar_day_empty),
                                style = ViraFont.footnote,
                                color = ViraColor.textSecondary,
                                modifier = Modifier.defaultMinSize(minHeight = ViraTouch.minimum).padding(vertical = ViraSpacing.s),
                            )
                        } else {
                            items.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider(color = ViraColor.separator)
                                SwipeActionsRow(onEdit = { onEdit(item) }, onDelete = { actions.delete(item) }) {
                                    PlanItemRow(item, onToggle = { actions.toggle(item) }, zone = zone)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/// El cos del calendari. Una columna amb desplaçament i no una llista mandrosa: el dia són tres
/// targetes i la setmana set grups, i la llista mandrosa no es dibuixa a les proves de captura
/// (`PrefetchScheduler` peta amb `Build.FINGERPRINT` nul).
@Composable
private fun PlanScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = ViraSpacing.screenMargin,
            end = ViraSpacing.screenMargin,
            top = ViraSpacing.m,
            bottom = viraTabBarClearance() + ViraSpacing.m,
        ),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        content = content,
    )
}
