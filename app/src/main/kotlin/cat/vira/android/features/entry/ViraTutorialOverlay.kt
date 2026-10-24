package cat.vira.android.features.entry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlin.math.roundToInt

/// Una peça il·luminada, ja numerada, en coordenades de la capa.
data class ViraTutorialSpot(val number: Int, val target: ViraTutorialTarget, val line: ViraTutorialLine, val rect: Rect)

/// El tomb dibuixat (`ViraTutorialOverlay.swift`, Arnau, 2026-10-21): cortina fosca, **un forat per
/// peça amb el seu número**, la pestanya que brilla amb «Toca aquí», i la targeta blanca amb Viri de
/// guia (`ViraTutorialCard`) a la franja lliure més gran (`ViraTutorialPlacement`).
///
/// - Els forats es buiden de la cortina (`BlendMode.Clear` dins d'una capa pròpia); les anelles
///   s'hi dibuixen a part, o el forat se les enduria.
/// - Amb contrast alt la cortina va més opaca i l'anella es fa més gruixuda: el que assenyala és
///   una **forma** i un **número**, no només la foscor.
/// - Una peça que no ha dit on és no té forat **ni frase**: la targeta només parla del que es veu.
/// - La cortina s'empassa tots els tocs menys el de la pestanya il·luminada, que fa «Següent», i el
///   del cercle de «Prova-ho». Per a TalkBack no hi són: la targeta ja té el botó.
@Composable
fun ViraTutorialOverlay(
    step: ViraTutorialStep,
    route: ViraTutorialRoute,
    frames: Map<ViraTutorialTarget, Rect>,
    prefs: EntryPreferences,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    context: ViraTutorialContext = ViraTutorialContext(),
    /// Marca la tasca de «Prova-ho» i torna com tornar-la a desmarcar.
    onTry: () -> (() -> Unit)? = { null },
) {
    val environment = LocalViraEnvironment.current
    val density = LocalDensity.current
    // Ha tocat el cercle d'Avui en aquesta parada: la frase passa a dir que ja és fet.
    var didTry by remember(step) { mutableStateOf(false) }
    // El cercle tocat i com desfer-ho: la frase diu «toca'l un altre cop» i ha de poder-se fer.
    var tried by remember(step) { mutableStateOf<Pair<Rect, () -> Unit>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // «Prova-ho» només si el cercle cau dins del forat: si no, la frase seria una promesa que la
    // cortina no deixa complir, i la parada diu el que diu sense tasca.
    val reachable = tryCircle(step, tutorialSpots(step, frames, context, origin), frames, context, didTry = false, origin) != null
    val shown = if (reachable || didTry) context.copy(didTry = didTry) else context.copy(tryTitle = null)
    val spots = tutorialSpots(step, frames, shown, origin)
    val tryCircle = tryCircle(step, spots, frames, context, didTry, origin)
    val scrim = ViraColor.shadow.copy(
        alpha = when {
            environment.increasedContrast -> 0.82f
            environment.isDark -> 0.74f
            else -> 0.62f
        },
    )
    val tabRadius = CornerRadius(with(density) { 999.dp.toPx() })
    val radius = CornerRadius(with(density) { ViraRadius.l.toPx() })

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                // Empassa els tocs: la pantalla del darrere no respon encara que se'n vegi un tros.
                .pointerInput(Unit) { detectTapGestures { } }
                .clearAndSetSemantics { },
        ) {
            drawRect(scrim)
            spots.forEach { spot ->
                val corner = if (spot.target.isTab) CornerRadius(minOf(tabRadius.x, spot.rect.height / 2f)) else radius
                drawRoundRect(scrim, spot.rect.topLeft, spot.rect.size, corner, blendMode = BlendMode.Clear)
            }
        }
        spots.forEach { ViraTutorialSpotRing(it, maxWidth) }
        spots.firstOrNull { it.target.isTab }?.let { ViraTutorialTapHint(it.rect, maxWidth, isCircle = false, onTap = onNext) }
        tryCircle?.let { rect ->
            ViraTutorialTapHint(rect, maxWidth, isCircle = true) {
                val undo = onTry()
                didTry = true
                tried = undo?.let { rect to it }
            }
        }
        tried?.takeIf { didTry }?.let { (rect, undo) ->
            ViraTutorialTapHint(rect, maxWidth, isCircle = true, quiet = true) {
                undo()
                didTry = false
                tried = null
            }
        }

        val heightPx = with(density) { maxHeight.toPx() }
        val top = WindowInsets.statusBars.getTop(density) + with(density) { ViraSpacing.s.toPx() }
        val bottom = heightPx - WindowInsets.navigationBars.getBottom(density) - with(density) { ViraSpacing.s.toPx() }
        val placement = placement(spots, top, bottom)
        val cardHeight = with(density) { placement.height.toDp() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .offset { IntOffset(0, placement.minY.roundToInt()) },
            contentAlignment = when (placement.anchor) {
                ViraTutorialPlacement.Anchor.TOP -> Alignment.TopCenter
                ViraTutorialPlacement.Anchor.BOTTOM -> Alignment.BottomCenter
                ViraTutorialPlacement.Anchor.CENTER -> Alignment.Center
            },
        ) {
            ViraTutorialCard(step, route, spots, prefs, onNext, onSkip)
        }
    }
}

/// Les peces que han dit on són, numerades en ordre, amb el forat una mica més gran que la peça.
@Composable
private fun tutorialSpots(
    step: ViraTutorialStep,
    frames: Map<ViraTutorialTarget, Rect>,
    context: ViraTutorialContext,
    origin: Offset,
): List<ViraTutorialSpot> {
    val density = LocalDensity.current
    val halo = with(density) { 6.dp.toPx() }
    return step.targets.zip(step.lines(context))
        .mapNotNull { (target, line) -> frames[target]?.let { Triple(target, line, it) } }
        .mapIndexed { index, (target, line, frame) ->
            var rect = frame.translate(-origin)
            target.maxHeight?.let { max ->
                val limit = with(density) { max.toPx() }
                if (rect.height > limit) rect = Rect(rect.left, rect.top, rect.right, rect.top + limit)
            }
            ViraTutorialSpot(index + 1, target, line, rect.inflate(halo))
        }
}

/// El cercle de la tasca que es pot provar, si cau dins del forat d'«El teu dia» i encara no s'ha tocat.
private fun tryCircle(
    step: ViraTutorialStep,
    spots: List<ViraTutorialSpot>,
    frames: Map<ViraTutorialTarget, Rect>,
    context: ViraTutorialContext,
    didTry: Boolean,
    origin: Offset,
): Rect? {
    if (step != ViraTutorialStep.TODAY || didTry || context.tryTitle == null) return null
    val frame = frames[ViraTutorialTarget.TodayCircle]?.translate(-origin) ?: return null
    val day = spots.firstOrNull { it.target == ViraTutorialTarget.TodayDay } ?: return null
    return if (day.rect.contains(frame.center)) frame else null
}

/// On va la targeta: les peces i, a sobre de la pestanya, l'espai de «Toca aquí».
@Composable
private fun placement(spots: List<ViraTutorialSpot>, top: Float, bottom: Float): ViraTutorialPlacement {
    val density = LocalDensity.current
    val hintRoom = with(density) { 52.dp.toPx() }
    val obstacles = spots.map { it.rect } + listOfNotNull(
        spots.firstOrNull { it.target.isTab }?.rect?.let { Rect(it.left, it.top - hintRoom, it.right, it.top) },
    )
    return ViraTutorialPlacement.place(
        obstacles,
        top = top,
        bottom = bottom,
        margin = with(density) { ViraSpacing.m.toPx() },
        minimum = with(density) { 260.dp.toPx() },
    )
}
