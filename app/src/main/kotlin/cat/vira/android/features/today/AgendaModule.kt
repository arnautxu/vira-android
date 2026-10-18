package cat.vira.android.features.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraCompletionCheck
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
import cat.vira.android.core.design.tokens.viraSpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/// El mòdul blau del dia (`AgendaModule`), **el protagonista d'Avui**: capçalera amb «El teu dia»,
/// el recompte en text i en barra i el «+»; les files; i un peu discret per obrir el calendari.
/// Quatre cantonades iguals: és un contenidor de contingut, no una targeta d'acció.
@Composable
internal fun AgendaModule(
    entries: List<AgendaEntry>,
    completion: TodayCompletion,
    zone: ZoneId,
    onToggle: (AgendaEntry) -> Unit,
    onEdit: (PlanItem) -> Unit,
    onDelete: (PlanItem) -> Unit,
    onAdd: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val area = ViraArea.Plan
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ViraGradient.card(area), shape)
            .animateContentSize(if (LocalViraEnvironment.current.reduceMotion) ViraMotion.reduced() else ViraMotion.settle()),
    ) {
        Header(entries, onAdd)
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.today_empty_message),
                style = ViraFont.footnote,
                color = ViraColor.onCard(area),
                modifier = Modifier.fillMaxWidth().padding(ViraSpacing.m),
            )
        } else {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = ViraColor.onCard(area).copy(alpha = 0.25f))
                androidx.compose.runtime.key(entry.id) {
                    AgendaModuleRow(entry, completion, zone, onToggle = { onToggle(entry) }, onEdit = onEdit, onDelete = onDelete)
                }
            }
        }
        Footer(onOpenCalendar)
    }
}

@Composable
private fun Header(entries: List<AgendaEntry>, onAdd: () -> Unit) {
    val ink = ViraColor.onCard(ViraArea.Plan)
    val done = entries.count { it.isDone }
    val progress by animateFloatAsState(
        if (entries.isEmpty()) 0f else done.toFloat() / entries.size,
        viraSpec(ViraMotion.settle()),
        label = "dayProgress",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(ink.copy(alpha = 0.12f))
            .padding(start = ViraSpacing.l, end = ViraSpacing.l, top = ViraSpacing.m, bottom = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(
                    stringResource(R.string.today_day_title),
                    style = ViraFont.title,
                    color = ink,
                    modifier = Modifier.semantics { heading() },
                )
                if (entries.isNotEmpty()) {
                    Text(stringResource(R.string.today_progress_done, done, entries.size), style = ViraFont.footnote, color = ink)
                }
            }
            Spacer(Modifier.width(ViraSpacing.xs))
            val add = stringResource(R.string.action_add)
            Box(
                Modifier
                    .size(ViraTouch.minimum)
                    .clip(CircleShape)
                    .background(ViraColor.background.copy(alpha = 0.55f), CircleShape)
                    .viraPressable(onClickLabel = add, onClick = onAdd)
                    .semantics { contentDescription = add },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ink)
            }
        }
        if (entries.isNotEmpty()) {
            // La barra acompanya el text «1 de 3 fetes», no el substitueix.
            LinearProgressIndicator(
                progress = { progress },
                color = ink,
                trackColor = ink.copy(alpha = 0.2f),
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().clearSemantics(),
            )
        }
    }
}

@Composable
private fun Footer(onOpenCalendar: () -> Unit) {
    val ink = ViraColor.onCard(ViraArea.Plan)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(ink.copy(alpha = 0.12f))
            .viraPressable(onClick = onOpenCalendar)
            .padding(vertical = ViraSpacing.s),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.home_open_calendar), style = ViraFont.callout, color = ink)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
    }
}

/// Una fila (`AgendaModuleRow`). La casella marca fet; la resta de la fila obre l'edició; esborrar
/// viu a la premuda llarga, lluny del gest més freqüent. El «Fet · Desfer» surt **dins** de la
/// mateixa fila mentre dura la finestra.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaModuleRow(
    entry: AgendaEntry,
    completion: TodayCompletion,
    zone: ZoneId,
    onToggle: () -> Unit,
    onEdit: (PlanItem) -> Unit,
    onDelete: (PlanItem) -> Unit,
) {
    val ink = ViraColor.onCard(ViraArea.Plan)
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val phase = completion.phaseFor(entry.id)
    var menuOpen by remember { mutableStateOf(false) }
    val item = entry.planItem

    val markLabel = stringResource(if (entry.isDone) R.string.a11y_mark_pending else R.string.a11y_mark_done)
    val editLabel = stringResource(R.string.plan_edit)
    val deleteLabel = stringResource(R.string.plan_delete)

    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (item != null) editLabel else null,
                onClick = { item?.let(onEdit) },
                onLongClick = if (item != null) ({ menuOpen = true }) else null,
            )
            .semantics(mergeDescendants = true) {
                // El menú de premuda llarga TalkBack no l'obre: les accions també són aquí.
                customActions = buildList {
                    add(CustomAccessibilityAction(markLabel) { onToggle(); true })
                    if (item != null) {
                        add(CustomAccessibilityAction(editLabel) { onEdit(item); true })
                        add(CustomAccessibilityAction(deleteLabel) { onDelete(item); true })
                    }
                }
            }
            .padding(start = if (stacked) ViraSpacing.m else ViraSpacing.l, end = ViraSpacing.s, top = ViraSpacing.xs, bottom = ViraSpacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            if (!stacked) {
                TimeLabel(entry.time, zone, ink, Modifier.width(52.dp))
                RowPictogram(entry, size = 34.dp)
            }
            Box(Modifier.weight(1f)) {
                Row(
                    Modifier.alpha(if (phase == null) 1f else 0f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                        if (stacked) {
                            // Apilat, l'hora i la casella pugen a una línia sobre el títol: així el
                            // títol té tota la fila i una paraula llarga no es parteix.
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                                RowPictogram(entry, size = 34.dp)
                                TimeLabel(entry.time, zone, ink)
                                Spacer(Modifier.weight(1f))
                                Check(entry, ink, onToggle)
                            }
                        }
                        Text(
                            entry.title,
                            style = ViraFont.callout,
                            color = ink,
                            textDecoration = if (entry.isDone) TextDecoration.LineThrough else null,
                        )
                        if (entry.planItem == null) {
                            Text(stringResource(entry.typeLabel), style = ViraFont.footnote, color = ink)
                        }
                    }
                    if (!stacked) Check(entry, ink, onToggle)
                }
                if (phase != null) {
                    TodayInlineCompletion(phase, ink, onUndo = { completion.undo(entry.id) })
                }
            }
        }
        if (item != null) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(editLabel) }, onClick = { menuOpen = false; onEdit(item) })
                DropdownMenuItem(
                    text = { Text(deleteLabel, color = ViraColor.error) },
                    onClick = { menuOpen = false; onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun RowPictogram(entry: AgendaEntry, size: androidx.compose.ui.unit.Dp) {
    val alpha by animateFloatAsState(if (entry.isDone) 0.5f else 1f, viraSpec(ViraMotion.standard()), label = "pictoAlpha")
    ViraPictogram(
        todayDrawable(entry.pictogramName, if (entry.isMedication) R.drawable.picto_medication else R.drawable.picto_flag),
        size = size,
        scalesWithText = !LocalViraEnvironment.current.isAccessibilitySize,
        modifier = Modifier.alpha(alpha),
    )
}

/// La medicació no celebra: el mòdul no dona XP ni ViriCoins ni ratxa (D-27).
@Composable
private fun Check(entry: AgendaEntry, ink: androidx.compose.ui.graphics.Color, onToggle: () -> Unit) {
    if (!entry.canToggle) return
    ViraCompletionCheck(
        isDone = entry.isDone,
        celebrates = !entry.isMedication && !entry.isExternal,
        ink = ink,
        modifier = Modifier.clip(CircleShape).viraPressable(role = Role.Checkbox, onClick = onToggle).clearSemantics(),
    )
}

/// L'hora a l'esquerra, en una columna fixa; sense hora, un guió perquè els títols quedin alineats.
@Composable
internal fun TimeLabel(time: Instant?, zone: ZoneId, ink: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    if (time != null) {
        Text(formatTime(time, zone), style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum"), color = ink, maxLines = 1, modifier = modifier)
    } else {
        Text("–", style = ViraFont.footnote, color = ink, modifier = modifier.clearSemantics())
    }
}

@Composable
internal fun formatTime(time: Instant, zone: ZoneId): String {
    val locale = viraLocale()
    return remember(time, zone, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time.atZone(zone))
    }
}

/// Amagat a TalkBack: la fila ja ho diu sencer.
internal fun Modifier.clearSemantics(): Modifier = clearAndSetSemantics { }
