package cat.vira.android.features.add

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.layout
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

/// La capçalera del full (`AddStepToolbar`): «Cancel·la» al primer pas, «Enrere» als altres i res
/// quan ja no hi ha pregunta. Al mig, «Afegir»; a la dreta, «1 de 3». Les tres cares la fan servir
/// igual, perquè passar d'una a l'altra no canviï on són els botons.
@Composable
fun AddTopBar(
    number: Int?,
    total: Int = 3,
    canGoBack: Boolean,
    /// Què diu el botó de l'esquerra quan no es pot tornar enrere; `null` no en posa cap.
    closeTitle: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    area: ViraArea = ViraArea.Plan,
) {
    Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable).padding(horizontal = ViraSpacing.xs)) {
        val leading = Modifier
            .align(Alignment.CenterStart)
            .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
        when {
            canGoBack -> Row(
                leading.viraPressable(onClick = onBack).padding(end = ViraSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = ViraColor.accent(area), modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.action_back), style = ViraFont.body, color = ViraColor.accent(area))
            }
            closeTitle != null -> Box(leading.viraPressable(onClick = onClose).padding(horizontal = ViraSpacing.xs), contentAlignment = Alignment.Center) {
                Text(closeTitle, style = ViraFont.body, color = ViraColor.accent(area))
            }
        }
        Text(
            stringResource(R.string.add_title),
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
        if (number != null) {
            Text(
                stringResource(R.string.add_step_count, number.toString(), total.toString()),
                style = ViraFont.footnoteEmphasis,
                color = ViraColor.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(ViraColor.surfaceElevated, CircleShape)
                    .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
            )
        }
    }
}

/// Els botons de baix d'un pas (`AddStepActions`): l'acció lila a sobre i, si n'hi ha, la textual a sota.
@Composable
fun AddStepActions(area: ViraArea = ViraArea.Plan, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceShift(area).copy(alpha = 0.94f))
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        content = content,
    )
}

/// La fila d'icones d'un toc, sota el camp (`QuickCaptureIconRow`): llisquen, i la triada porta una
/// marca a més del contorn.
@Composable
fun AddIconRow(
    icons: List<String>,
    selection: String,
    onSelect: (String) -> Unit,
    area: ViraArea = ViraArea.Plan,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        icons.forEach { name ->
            val isSelected = name == selection
            val label = ViraPictogramName.accessibilityLabel(name)?.let { stringResource(it) } ?: name
            val shape = RoundedCornerShape(ViraRadius.m)
            Box(
                Modifier
                    .size(52.dp)
                    .viraPressable(role = Role.Button) { onSelect(name) }
                    .semantics { contentDescription = label; selected = isSelected },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ViraColor.surfaceElevated, shape)
                        .then(if (isSelected) Modifier.border(2.5.dp, ViraColor.accent(area), shape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { ViraPictogram(name, size = 34.dp) }
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ViraColor.accent(area),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(ViraSpacing.xxs, -ViraSpacing.xxs)
                            .size(18.dp)
                            .background(ViraColor.surfaceElevated, CircleShape),
                    )
                }
            }
        }
    }
}

/// Una fila amb un valor que es mou amb − i + (`ViraStepperRow`): l'hora d'un avís o la d'una presa.
/// Amb dos tocs grossos n'hi ha prou, i no cal obrir cap rellotge. Si no hi cap, l'etiqueta va a
/// sobre dels botons. Si la fila es pot treure, porta la creu.
@Composable
fun AddStepperRow(
    pictogram: String,
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    area: ViraArea = ViraArea.Plan,
    isEmphasized: Boolean = true,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val removable = onRemove != null && removeLabel != null
    // Com `ViewThatFits`: en fila si l'etiqueta hi cap sencera; si no, a sobre dels botons.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = ViraFont.callout
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = LocalViraEnvironment.current.isAccessibilitySize || run {
            val labelWidth = with(density) { measurer.measure(label, labelStyle, softWrap = false).size.width.toDp() }
            val fixed = ViraSpacing.m + ViraSpacing.xs + 30.dp + ViraSpacing.s + ViraSpacing.xs +
                ViraTouch.minimum * 2 + 72.dp + ViraSpacing.xs * 2 + if (removable) ViraTouch.minimum + ViraSpacing.xs else 0.dp
            labelWidth + fixed > maxWidth
        }
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ViraTouch.comfortable)
                .background(ViraColor.surfaceElevated, shape)
                .then(if (isEmphasized) Modifier.border(2.dp, ViraColor.accent(area), shape) else Modifier)
                .padding(start = ViraSpacing.m, end = ViraSpacing.xs, top = ViraSpacing.xs, bottom = ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            val labelGroup = @Composable { modifier: Modifier ->
                Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    ViraPictogram(pictogram, size = 30.dp, scalesWithText = true)
                    Text(label, style = ViraFont.callout, color = ViraColor.textSecondary)
                }
            }
            val stepper = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    StepButton(isPlus = false, area = area, label = "$label −", onClick = onDecrement)
                    Text(
                        value,
                        style = ViraFont.title.copy(fontFeatureSettings = "tnum"),
                        color = ViraColor.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 72.dp).clearAndSetSemantics { contentDescription = "$label, $value" },
                    )
                    StepButton(isPlus = true, area = area, label = "$label +", onClick = onIncrement)
                }
            }
            if (stacked) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    labelGroup(Modifier)
                    stepper()
                }
            } else {
                labelGroup(Modifier.weight(1f))
                stepper()
            }
            if (onRemove != null && removeLabel != null) {
                Box(
                    Modifier.size(ViraTouch.minimum).viraPressable(onClickLabel = removeLabel, onClick = onRemove)
                        .semantics { contentDescription = removeLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = ViraColor.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/// El − i el + dibuixats: el joc d'icones bàsic d'Android no porta el menys.
@Composable
private fun StepButton(isPlus: Boolean, area: ViraArea, label: String, onClick: () -> Unit) {
    val ink = ViraColor.onSurface(area)
    Box(
        Modifier
            .size(ViraTouch.minimum)
            .background(ViraColor.surface(area), CircleShape)
            .viraPressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 2.5.dp.toPx()
            drawLine(ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
            if (isPlus) drawLine(ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke, StrokeCap.Round)
        }
    }
}

/// Hora de rellotge d'uns minuts del dia, amb el format de l'idioma de la pantalla.
@Composable
fun clockText(minutes: Int): String = cat.vira.android.features.plan.PlanFormat.minutes(minutes)

/// Surt del marge de la pantalla per les dues bandes: la fila d'icones llisca d'una vora a l'altra.
fun Modifier.bleed(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = (margin * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}
