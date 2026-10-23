package cat.vira.android.features.plan

import cat.vira.android.core.design.components.ViraSceneTile
import cat.vira.android.app.PlanSection
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.viraLocale
import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.LocalActiveCompletion
import cat.vira.android.core.design.components.LocalUndoCompletion
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraInlineCompletion
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPop
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val area = ViraArea.Plan

/// Hàbits (`HabitsView`): el resum d'avui a dalt i una graella de fitxes, les que avui toquen
/// primer. Tocar el cercle marca; tocar la fitxa obre el detall; mantenir-la premuda, editar i
/// arxivar.
@Composable
fun HabitsSection(snapshot: PlanSnapshot, actions: PlanActions, chrome: PlanChrome, today: LocalDate) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val habits = snapshot.activeHabits
    val firstDay = PlanDay.firstDayOfWeek(viraLocale())
    val progress = remember(snapshot.habitLogs, today, firstDay) { HabitProgress(snapshot.habitLogs, today, firstDay = firstDay) }

    LaunchedEffect(chrome.adding) {
        if (chrome.adding == PlanSection.Habits) {
            chrome.adding = null
            creating = true
        }
    }

    val detail = detailId?.let { id -> habits.firstOrNull { it.id.toString() == id } }
    if (detail != null) {
        BackHandler { detailId = null }
        HabitDetail(detail, progress, actions, onEdit = { editing = detail }, onClose = { detailId = null })
    } else if (habits.isEmpty()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = ViraSpacing.xxl, bottom = viraTabBarClearance())) {
            ViraEmptyState(
                title = stringResource(R.string.habits_empty_title),
                message = stringResource(R.string.habits_empty_message),
                actionTitle = stringResource(R.string.habits_new),
                onAction = { creating = true },
                icon = { ViraPictogram(R.drawable.picto_habits, size = 56.dp) },
            )
        }
    } else {
        HabitsList(habits, progress, actions, today, onOpen = { detailId = it.id.toString() }, onEdit = { editing = it }, onCreate = { creating = true })
    }

    if (creating) HabitEditor(null, actions, onDismiss = { creating = false })
    editing?.let { habit -> HabitEditor(habit, actions, onDismiss = { editing = null }) }
}

@Composable
private fun HabitsList(
    habits: List<Habit>,
    progress: HabitProgress,
    actions: PlanActions,
    today: LocalDate,
    onOpen: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    onCreate: () -> Unit,
) {
    val columns = if (LocalViraEnvironment.current.isAccessibilitySize) 1 else 2
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = ViraSpacing.xs, bottom = viraTabBarClearance() + ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        HabitsTodayHero(habits, progress)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            // A lletra gran la pista baixa a sota: al costat, "Avui" es partia per la meitat.
            val title = @Composable { modifier: Modifier ->
                Text(
                    stringResource(R.string.a11y_today),
                    style = ViraFont.headline,
                    color = ViraColor.textPrimary,
                    modifier = modifier.semantics { heading() },
                )
            }
            val hint = @Composable {
                Text(stringResource(R.string.habits_today_hint), style = ViraFont.footnoteEmphasis, color = ViraColor.accent(area))
            }
            if (LocalViraEnvironment.current.isAccessibilitySize) {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) { title(Modifier); hint() }
            } else {
                Row(verticalAlignment = Alignment.Bottom) { title(Modifier.weight(1f)); hint() }
            }
            val tiles: List<@Composable (Modifier) -> Unit> = progress.ordered(habits).map { habit ->
                @Composable { modifier: Modifier ->
                    HabitTile(
                        habit, progress, modifier,
                        onToggle = { actions.toggle(habit, today, progress) },
                        onOpen = { onOpen(habit) },
                        onEdit = { onEdit(habit) },
                        onArchive = { actions.archive(habit) },
                    )
                }
            } + listOf(@Composable { modifier: Modifier -> HabitAddTile(onCreate, modifier) })
            tiles.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
                    row.forEach { tile -> tile(Modifier.weight(1f).fillMaxSize()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/// El resum d'avui (`HabitsTodayHero`): anell amb "2/3", títol i frase que diu què queda, i la
/// setmana amb un punt per dia. Viri a la cantonada, amb la medalla quan tot és fet.
@Composable
fun HabitsTodayHero(habits: List<Habit>, progress: HabitProgress) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val due = progress.dueToday(habits)
    val done = due.count { progress.isDone(it) }
    val pending = due.filter { !progress.isDone(it) }
    val allDone = due.isNotEmpty() && pending.isEmpty()
    val ink = ViraColor.onCard(area)
    val shape = RoundedCornerShape(ViraRadius.l)
    val title = stringResource(
        when {
            due.isEmpty() -> R.string.habits_hero_rest_title
            allDone -> R.string.habits_hero_all_done_title
            pending.size == 1 -> R.string.habits_hero_one_left_title
            done == 0 -> R.string.habits_hero_start_title
            else -> R.string.habits_hero_some_left_title
        },
    )
    val message = when {
        due.isEmpty() -> stringResource(R.string.habits_hero_rest_message)
        allDone -> stringResource(R.string.habits_hero_all_done_message)
        pending.size == 1 -> stringResource(R.string.habits_hero_one_left_message, pending.first().title)
        else -> stringResource(R.string.habits_hero_left_message, pending.size)
    }

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().background(ViraGradient.card(area, ViraGradient.Axis.Vertical), shape).padding(ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                ProgressRing(done, due.size, stringResource(R.string.habits_hero_today))
                Column(
                    Modifier.weight(1f).padding(end = if (isLarge) 0.dp else 76.dp + ViraSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
                ) {
                    Text(title, style = ViraFont.title, color = ink, modifier = Modifier.semantics { heading() })
                    Text(message, style = ViraFont.footnote, color = ink)
                }
            }
            WeekDots(habits, progress)
        }
        if (!isLarge) {
            // Viri sencer en miniatura: posant el sol a l'hàbit o, tot fet, al puf amb la llista.
            ViraSceneTile(
                if (allDone) R.drawable.viri_home_done else R.drawable.viri_suggest_habit_3,
                side = 76.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(ViraSpacing.s),
            )
        }
    }
}

/// Anell de progrés amb la xifra a dins (també el de la medicació, sense premi).
@Composable
fun ProgressRing(done: Int, total: Int, caption: String) {
    val ink = ViraColor.onCard(area)
    val track = ViraColor.lightArtworkVeil.copy(alpha = 0.55f)
    val fill = ViraColor.accent(area)
    val fraction by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, ViraMotion.standard(), label = "ring")
    // L'anell creix amb la lletra: "1/2 preses" a mida gran no hi cabia i trepitjava el traç.
    val diameter = 78.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.7f)
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(track, radius = (size.width - stroke) / 2, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done/$total", style = ViraFont.title, color = ink)
            Text(caption, style = ViraFont.caption, color = ink)
        }
    }
}

@Composable
private fun WeekDots(habits: List<Habit>, progress: HabitProgress) {
    val locale = viraLocale()
    val track = ViraColor.lightArtworkVeil.copy(alpha = 0.5f)
    val fill = ViraColor.accent(area)
    val onFill = ViraColor.onAccent
    val ring = ViraColor.brandAccent
    val ink = ViraColor.onCard(area)
    Row(
        Modifier.fillMaxWidth()
            .background(ViraColor.lightArtworkVeil.copy(alpha = 0.35f), RoundedCornerShape(ViraRadius.m))
            .padding(vertical = ViraSpacing.xs)
            .clearAndSetSemantics { },
    ) {
        progress.currentWeek.forEach { day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = ViraFont.caption, color = ink)
                val fraction = if (progress.isFuture(day)) null else progress.dayFraction(habits, day)
                val isToday = day == progress.today
                Box(
                    Modifier.size(34.dp).then(if (isToday) Modifier.border(2.dp, ring, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(26.dp).clip(CircleShape)) {
                        drawCircle(track)
                        val f = fraction ?: 0.0
                        if (f >= 1) {
                            drawCircle(fill)
                        } else if (f > 0) {
                            rotate(-90f) { drawArc(fill, 0f, 360f * f.toFloat(), true) }
                        }
                    }
                    if ((fraction ?: 0.0) >= 1) PlanGlyph(PlanGlyphShape.Check, onFill, size = 12.dp, weight = 0.18f)
                }
            }
        }
    }
}

/// Fitxa d'un hàbit (`HabitTile`): pictograma i cercle de marcar a dalt, nom, "2 de 3 aquesta
/// setmana" i una barra per dia. Feta, passa al degradat de Pla.
@Composable
fun HabitTile(
    habit: Habit,
    progress: HabitProgress,
    modifier: Modifier,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    val isDone = progress.isDone(habit)
    val id = PlanCompletionIds.habit(habit, progress.today)
    val phase = LocalActiveCompletion.current?.takeIf { it.id == id }?.phase
    val undo = LocalUndoCompletion.current
    val ink = if (isDone) ViraColor.onCard(area) else ViraColor.textPrimary
    val shape = RoundedCornerShape(ViraRadius.l)
    val (doneDays, dueDays) = progress.week(habit)
    val weekLine = stringResource(R.string.habits_tile_week, doneDays, dueDays)
    val state = stringResource(if (isDone) R.string.habits_legend_done else R.string.habits_legend_pending)
    val markLabel = stringResource(if (isDone) R.string.a11y_mark_pending else R.string.a11y_mark_done)
    val openHint = stringResource(R.string.habits_tile_open_hint)
    val editLabel = stringResource(R.string.habits_edit)
    val archiveLabel = stringResource(R.string.habits_archive)
    var menu by remember { mutableStateOf(false) }
    val elevated = ViraColor.surfaceElevated
    val gradient = ViraGradient.card(area, ViraGradient.Axis.Vertical)

    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 168.dp)
                .viraPop(isDone, scale = 1.04f)
                .clip(shape)
                .then(if (isDone) Modifier.background(gradient, shape) else Modifier.background(elevated, shape))
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onClickLabel = openHint)
                .clearAndSetSemantics {
                    contentDescription = habit.title
                    stateDescription = "$state, $weekLine"
                    role = Role.Button
                    onClick(openHint) { onOpen(); true }
                    customActions = listOf(
                        CustomAccessibilityAction(markLabel) { onToggle(); true },
                        CustomAccessibilityAction(editLabel) { onEdit(); true },
                        CustomAccessibilityAction(archiveLabel) { onArchive(); true },
                    )
                }
                .padding(ViraSpacing.s),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ViraPictogram(habit.symbolName, size = 56.dp)
                Spacer(Modifier.weight(1f))
                TileCheck(isDone, onToggle, Modifier.offset(x = ViraSpacing.xxs, y = -ViraSpacing.xxs))
            }
            Spacer(Modifier.weight(1f))
            Text(habit.title, style = ViraFont.headline, color = ink, maxLines = 2)
            Box {
                Text(
                    weekLine,
                    style = ViraFont.footnote,
                    color = if (isDone) ViraColor.onSurface(area) else ViraColor.textSecondary,
                    modifier = Modifier.alpha(if (phase == null) 1f else 0f),
                )
                if (phase != null) ViraInlineCompletion(phase, ink, onUndo = { undo(id) })
            }
            WeekBars(habit, progress, isDone)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(editLabel, style = ViraFont.body) }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(text = { Text(archiveLabel, style = ViraFont.body) }, onClick = { menu = false; onArchive() })
        }
    }
}

/// El cercle de marcar d'una fitxa: 34 dp a la vista, 48 de toc.
@Composable
fun TileCheck(isDone: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabledTint: Color = ViraColor.accent(area)) {
    Box(
        modifier.size(ViraTouch.minimum).viraPressable(role = Role.Checkbox, onClick = onClick).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(34.dp).viraPop(isDone, scale = 1.25f)
                .then(if (isDone) Modifier.background(enabledTint, CircleShape) else Modifier.border(2.5.dp, enabledTint, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) PlanGlyph(PlanGlyphShape.Check, ViraColor.onAccent, size = 16.dp, weight = 0.17f)
        }
    }
}

@Composable
private fun WeekBars(habit: Habit, progress: HabitProgress, isDone: Boolean) {
    val doneColor = ViraColor.accent(area)
    val empty = if (isDone) ViraColor.lightArtworkVeil.copy(alpha = 0.5f) else ViraColor.surface(area)
    Row(Modifier.widthIn(max = 120.dp).fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        progress.currentWeek.forEach { day ->
            val color = when {
                progress.isDone(habit, day) -> doneColor
                habit.isScheduled(day) -> empty
                else -> empty.copy(alpha = empty.alpha * 0.5f)
            }
            Box(Modifier.weight(1f).height(5.dp).background(color, CircleShape))
        }
    }
}

/// La fitxa de "Nou hàbit": vora discontínua, el color de Pla.
@Composable
fun HabitAddTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = ViraColor.accent(area)
    val label = stringResource(R.string.habits_new)
    Column(
        modifier.heightIn(min = 168.dp)
            .viraPressable(onClick = onClick)
            .dashedBorder(accent.copy(alpha = 0.45f), ViraRadius.l)
            .padding(ViraSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterVertically),
    ) {
        PlanGlyph(PlanGlyphShape.Plus, accent, size = 26.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale, weight = 0.13f)
        Text(label, style = ViraFont.headline, color = accent, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/// Vora discontínua arrodonida (`StrokeStyle(dash:)`).
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx()))),
    )
}
