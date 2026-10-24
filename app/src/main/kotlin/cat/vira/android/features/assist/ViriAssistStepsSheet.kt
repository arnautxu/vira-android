package cat.vira.android.features.assist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraSceneTile
import cat.vira.android.core.design.components.viraIdleBreath
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.resources.ViraLanguage
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val area = ViraArea.Plan

/// «Parteix-la amb Viri» (`ViriAssistStepsSheet`): la tasca a dalt en una targeta, Viri pensant i,
/// quan respon, els passos en files per triar. Arriben desmarcats; res es desa fins a «Afegir», la
/// tasca original es queda i hi ha Desfer. Cap XP ni moneda per fer-lo servir.
@Composable
fun ViriAssistStepsSheet(item: PlanItem, onDismiss: () -> Unit) {
    val backend = LocalViraBackend.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val flow = remember(item.id) {
        val remote = ViriAssistRemote(backend.session, { ViraLanguage.current(context) })
        ViriAssistStepsFlow(scope, remote::steps, AssistanceCommit(ViraStore(backend.db)))
    }
    LaunchedEffect(item.id) { flow.generate(item.title) }
    // La tasca tal com és a la base de dades: «Afegir» comprova que no hagi canviat. El full
    // «Afegir» l'acaba d'escriure, i l'escriptura pot arribar un moment després.
    val original by produceState<PlanItem?>(null, item.id) {
        repeat(30) {
            backend.db.planItems().byId(item.id)?.let { value = it; return@produceState }
            delay(100)
        }
    }
    // Si canvia el compte mentre és obert, el full es tanca: res del compte anterior es desa.
    val generation = remember { backend.session.dataGeneration }
    val sessionState by backend.session.state.collectAsState()
    LaunchedEffect(sessionState) { if (generation != backend.session.dataGeneration) onDismiss() }
    DisposableEffect(flow) { onDispose { flow.cancel() } }
    ViriAssistSheet(onDismiss) { ViriAssistStepsContent(item, original, flow, onDismiss) }
}

/// `original`, la tasca llegida de la base de dades; mentre no hi és, «Afegir» espera.
@Composable
fun ViriAssistStepsContent(item: PlanItem, original: PlanItem?, flow: ViriAssistStepsFlow, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            ViraActionButton(
                stringResource(if (flow.saved) R.string.action_done else R.string.action_cancel), onClose,
                hierarchy = ViraActionHierarchy.Textual, area = area,
            )
            Text(
                stringResource(R.string.viri_assist_steps_title),
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            TaskCard(item)
            if (flow.isThinking) ThinkingScene() else Prompt(stringResource(promptText(flow)))
            if (flow.steps.isNotEmpty()) StepList(flow)
            if (!flow.saved && !flow.working) MoreButtons(flow) { flow.generate(item.title) }
            Text(stringResource(R.string.assistance_steps_original), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            if (flow.saved) {
                ViraActionButton(stringResource(R.string.action_done), onClose, Modifier.fillMaxWidth(), area = area)
                ViraActionButton(
                    stringResource(R.string.action_undo), flow::undo, Modifier.fillMaxWidth(),
                    hierarchy = ViraActionHierarchy.Secondary, area = area,
                )
            } else if (flow.steps.isNotEmpty()) {
                val count = flow.selectedCount
                val label = if (count == 0) stringResource(R.string.viri_assist_steps_add_zero)
                else pluralStringResource(R.plurals.viri_assist_steps_add, count, count)
                ViraActionButton(
                    label,
                    { original?.let { flow.save(it.id, it.title, it.updatedAt) {} } },
                    Modifier.fillMaxWidth(), area = area, enabled = flow.canSave && original != null,
                )
            }
        }
    }
}

private fun promptText(flow: ViriAssistStepsFlow): Int = when {
    flow.saved -> R.string.viri_assist_steps_saved
    flow.message != null && flow.steps.isEmpty() -> flow.message!!
    else -> R.string.viri_assist_steps_ready
}

@Composable
private fun TaskCard(item: PlanItem) {
    val day = remember(item.day) { item.day.atZone(ZoneId.systemDefault()).toLocalDate() }
    val locale = viraLocale()
    Row(
        Modifier.fillMaxWidth()
            .background(ViraGradient.card(area), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (!LocalViraEnvironment.current.isAccessibilitySize) {
            Box(Modifier.background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)).padding(ViraSpacing.xs)) {
                ViraPictogram(item.symbolName, size = 40.dp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(item.title, style = ViraFont.title, color = ViraColor.onCard(area))
            Text(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)), style = ViraFont.callout, color = ViraColor.onCard(area))
        }
    }
}

/// Mentre pensa: Viri gran amb la bombolla. A lletra gran, Viri petit i la bombolla al costat.
@Composable
private fun ThinkingScene() {
    val text = stringResource(R.string.viri_assist_steps_thinking)
    if (LocalViraEnvironment.current.isAccessibilitySize) return Prompt(text)
    Box(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = text }) {
        Image(
            painterResource(R.drawable.viri_suggest_breakdown_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(ViraRadius.l)).viraIdleBreath(),
        )
        Text(
            text,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = Modifier.align(Alignment.BottomStart).padding(ViraSpacing.m).padding(end = 90.dp)
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        )
    }
}

/// Viri petit i la bombolla (`QuickCaptureViriPrompt`).
@Composable
private fun Prompt(text: String) {
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraSceneTile(R.drawable.viri_suggest_breakdown_1, 64.dp)
        Text(
            text,
            style = ViraFont.body,
            color = ViraColor.textPrimary,
            modifier = Modifier.weight(1f)
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        )
    }
}

@Composable
private fun StepList(flow: ViriAssistStepsFlow) {
    Column(
        Modifier.fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        flow.steps.forEach { step ->
            ViriAssistStepRow(
                step, isLocked = flow.saved,
                onToggle = { flow.toggle(step.id) },
                onEdit = { flow.edit(step.id, it) },
                onRemove = { flow.remove(step.id) },
            )
        }
        (flow.validationMessage ?: flow.message)?.let {
            Text(stringResource(it), style = ViraFont.footnote, color = ViraColor.textSecondary, modifier = Modifier.padding(horizontal = ViraSpacing.xs))
        }
    }
}

/// «Uns altres» torna a demanar i conserva els marcats; «Escriu-ne un», a mà.
@Composable
private fun MoreButtons(flow: ViriAssistStepsFlow, onMore: () -> Unit) {
    val more = stringResource(if (flow.steps.isEmpty()) R.string.viri_assist_retry else R.string.viri_assist_steps_more)
    val write = stringResource(R.string.viri_assist_steps_write)
    val canWrite = flow.steps.size < AssistanceDraft.MAXIMUM_STEPS
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraActionButton(more, onMore, Modifier.fillMaxWidth(), hierarchy = ViraActionHierarchy.Secondary, area = area)
            ViraActionButton(write, flow::addManualStep, Modifier.fillMaxWidth(), hierarchy = ViraActionHierarchy.Secondary, area = area, enabled = canWrite)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraActionButton(more, onMore, Modifier.weight(1f), hierarchy = ViraActionHierarchy.Secondary, area = area)
            ViraActionButton(write, flow::addManualStep, Modifier.weight(1f), hierarchy = ViraActionHierarchy.Secondary, area = area, enabled = canWrite)
        }
    }
}
