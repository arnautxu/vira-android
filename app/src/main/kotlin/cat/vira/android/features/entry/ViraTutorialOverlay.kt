package cat.vira.android.features.entry

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.viraSpec

/// El tutorial dibuixat (`ViraTutorialOverlay.swift`): cortina fosca amb un forat sobre la icona
/// que toca, i la pastilla groga a sobre. Darrere hi ha l'app de debò, no el degradat.
///
/// - El forat es buida de la cortina (`BlendMode.Clear` dins d'una capa pròpia); l'anella blanca
///   s'hi dibuixa després, o el forat se l'enduria.
/// - Amb contrast alt, la cortina més opaca i l'anella més gruixuda: el que assenyala és una
///   **forma**, no només la diferència de foscor.
/// - La cortina s'empassa els tocs: la barra del darrere no respon encara que se'n vegi un tros.
/// - Amb Reduir moviment el forat salta al lloc; la pastilla sí que fon.
///
/// `spotlight` és on és la icona, en coordenades d'aquesta capa; `null` a integracions i widgets.
@Composable
fun ViraTutorialOverlay(
    step: ViraTutorialStep,
    spotlight: Rect?,
    prefs: EntryPreferences,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = LocalViraEnvironment.current
    val highContrast = environment.increasedContrast
    val density = LocalDensity.current
    val halo = with(density) { 6.dp.toPx() }
    val target = spotlight?.inflate(halo)
    val animatedHole by animateRectAsState(
        targetValue = target ?: Rect.Zero,
        animationSpec = viraSpec(ViraMotion.settle(), decorative = true),
        label = "tutorialHole",
    )
    val hole = if (target == null) null else if (environment.reduceMotion) target else animatedHole
    val curtain = ViraColor.shadow.copy(alpha = if (highContrast) 0.82f else 0.62f)
    val ringColor = ViraColor.lightArtworkVeil
    val ringWidth = with(density) { (if (highContrast) 3.dp else 2.dp).toPx() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(Unit) { detectTapGestures { } }
                .clearAndSetSemantics { },
        ) {
            drawRect(curtain)
            if (hole != null) {
                val radius = CornerRadius(hole.height / 2f)
                drawRoundRect(curtain, hole.topLeft, hole.size, radius, blendMode = BlendMode.Clear)
                drawRoundRect(
                    ringColor,
                    hole.topLeft + Offset(ringWidth / 2f, ringWidth / 2f),
                    hole.size.copy(hole.width - ringWidth, hole.height - ringWidth),
                    radius,
                    style = Stroke(ringWidth),
                )
            }
        }

        val heightPx = with(density) { maxHeight.toPx() }
        // La pastilla seu **just per damunt** del forat quan n'hi ha, i al mig quan no. No pot créixer
        // per sobre del forat ni sortir de la pantalla: amb el sostre es fa desplaçable.
        val bottomInset = if (spotlight == null) 0.dp else with(density) { (heightPx - spotlight.top).coerceAtLeast(0f).toDp() } + ViraSpacing.m
        val band = if (spotlight == null) maxHeight * 0.8f else maxOf(180.dp, with(density) { spotlight.top.toDp() } - ViraSpacing.xxl - ViraSpacing.m)
        val focus = remember { FocusRequester() }
        LaunchedEffect(step) { runCatching { focus.requestFocus() } }

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset),
            contentAlignment = if (spotlight == null) Alignment.Center else Alignment.BottomCenter,
        ) {
            Box(Modifier.heightIn(max = band).verticalScroll(rememberScrollState())) {
                EntryCard(
                    Modifier
                        .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.s)
                        .focusRequester(focus)
                        .focusable(),
                ) {
                    val ink = ViraColor.onCard(ViraArea.Rewards)
                    EntryHeading(stringResource(step.title), stringResource(step.message))
                    if (step == ViraTutorialStep.INTEGRATIONS) {
                        EntryConnectionToggles(prefs)
                        EntryTogglesFootnote()
                    }
                    // Els widgets no tenen interruptor: s'afegeixen des de la pantalla d'inici.
                    if (step == ViraTutorialStep.WIDGETS) {
                        Text(stringResource(R.string.tutorial_widgets_how), style = ViraFont.footnote, color = ink)
                    }
                    ViraTutorialDots(step)
                    EntryPrimaryButton(stringResource(step.action), onNext)
                    if (step.next != null) EntryLinkButton(stringResource(R.string.action_skip), onSkip)
                }
            }
        }
    }
}

/// Els punts de progrés. Purament visuals: el títol de cada parada ja diu on ets.
@Composable
private fun ViraTutorialDots(current: ViraTutorialStep) {
    Row(
        Modifier.fillMaxWidth().padding(top = ViraSpacing.xxs).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraTutorialStep.entries.forEach { step ->
            val color = if (step == current) ViraColor.accent(ViraArea.Today) else ViraColor.onCard(ViraArea.Rewards).copy(alpha = 0.3f)
            Box(Modifier.size(6.dp).background(color, CircleShape))
        }
    }
}
