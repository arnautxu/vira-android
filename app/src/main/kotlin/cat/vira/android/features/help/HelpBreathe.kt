package cat.vira.android.features.help

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraAlphaVideo
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.resources.ViraVideo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CIRCLE_SIDE = 220f

/// Respiració guiada de 60 s (`HelpBreatheView`, `USER_FLOWS.md` F23): cicle de 4 + 2 + 4 s, amb
/// Viri meditant dins del cercle. Viri és **neutre**: no diu res, no celebra i no reacciona a si
/// l'exercici s'acaba o es deixa a mitges.
///
/// **El cercle és la guia, no el clip**: el vídeo té el seu ritme i és ambient. Amb Reduir
/// moviment, el cercle no canvia de mida però sí d'opacitat, al mateix ritme, perquè qui no el veu
/// moure's també sàpiga on és dins del tram; i Viri es queda quiet.
@Composable
fun HelpBreatheContent(onClose: () -> Unit, backLabel: String = stringResource(R.string.action_back)) {
    val context = LocalContext.current
    val haptics = remember { HelpBreathingHaptics(context) }
    var elapsed by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    /// Cada «Una altra» en suma un i torna a engegar l'exercici sense sortir de la pantalla.
    var run by remember { mutableIntStateOf(0) }
    /// 0 buit, 1 ple. No es deriva de la fase: la fase ja és «Inspira» al segon zero, i el cercle
    /// naixia ple i es quedava quiet els quatre primers segons (iPhone, 2026-10-12).
    val fullness = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // En sortir de l'app (Inici, una altra app, pantalla bloquejada) el palmell para a l'instant;
    // el rellotge s'atura al bucle de sota i continua en tornar.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) haptics.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(run) {
        fun breathe(phase: BreathPhase) {
            haptics.play(inhale = phase == BreathPhase.Inhale, seconds = phase.seconds)
            launch {
                fullness.animateTo(
                    if (phase == BreathPhase.Inhale) 1f else 0f,
                    tween(phase.seconds * 1000, easing = FastOutSlowInEasing),
                )
            }
        }
        haptics.start()
        try {
            // El primer tram, abans que el rellotge avanci: si no, la vibració aniria un segon tard.
            breathe(BreathPhase.Inhale)
            while (elapsed < HelpBreathing.TOTAL_SECONDS) {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    haptics.stop()
                    lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.STARTED) }
                    haptics.start()
                }
                delay(1000)
                elapsed += 1
                HelpBreathing.stretchStartingAt(elapsed)?.let(::breathe)
            }
            isFinished = true
        } finally {
            haptics.stop()
        }
    }

    HelpPage(stringResource(R.string.help_breathe_title), onBack = onClose, backLabel = backLabel) {
        BreatheCard(elapsed, isFinished, fullness.value)
        if (isFinished) {
            ViraActionButton(
                stringResource(R.string.help_breathe_again),
                onClick = {
                    elapsed = 0
                    isFinished = false
                    run += 1
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ViraActionButton(
                stringResource(R.string.help_breathe_back),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Secondary,
            )
        } else {
            ViraActionButton(
                stringResource(R.string.help_breathe_stop),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Secondary,
            )
        }
    }
}

/// Targeta lila (`DESIGN_RULES.md` §3): el cercle amb Viri, la fase, les tres fases en píndoles i
/// el progrés en text (§4: «18 de 60 s», mai només la barra).
@Composable
internal fun BreatheCard(elapsed: Int, isFinished: Boolean, fullness: Float) {
    val phase = HelpBreathing.phase(elapsed)
    val remaining = HelpBreathing.TOTAL_SECONDS - elapsed
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Today), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        BreatheCircle(fullness, Modifier.padding(vertical = ViraSpacing.s))
        // Un fos, mai un desplaçament: aquesta paraula és la instrucció de l'exercici. TalkBack la
        // diu a cada canvi, perquè qui no la veu sàpiga quan toca expirar.
        Crossfade(targetState = if (isFinished) R.string.help_breathe_done else phase.title, label = "phase") { title ->
            Text(
                stringResource(title),
                style = ViraFont.largeTitle,
                color = ViraColor.onCard(ViraArea.Today),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (!isFinished) BreathPhasePills(phase)
        HelpProgress(
            leading = if (isFinished) stringResource(R.string.action_done) else stringResource(R.string.help_breathe_remaining, remaining),
            leadingDescription = if (isFinished) stringResource(R.string.action_done) else stringResource(R.string.a11y_seconds_remaining, remaining),
            trailing = stringResource(R.string.help_breathe_progress, elapsed, HelpBreathing.TOTAL_SECONDS),
            fraction = elapsed / HelpBreathing.TOTAL_SECONDS.toFloat(),
        )
    }
}

/// El cercle que porta el ritme. Creix i decreix amb l'escala, no amb la mida: a dins hi ha un
/// vídeo, i redimensionar-lo a cada fotograma el faria tornar a maquetar.
@Composable
private fun BreatheCircle(fullness: Float, modifier: Modifier = Modifier) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val rest = 140f / CIRCLE_SIDE
    Box(
        modifier
            .graphicsLayer {
                val scale = if (reduceMotion) 180f / CIRCLE_SIDE else rest + (1f - rest) * fullness
                scaleX = scale
                scaleY = scale
                // Ple en inspirar, mig en expirar: només amb Reduir moviment, que ja no creix.
                alpha = if (reduceMotion) 0.45f + 0.55f * fullness else 1f
            }
            .clearAndSetSemantics {}
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(ViraSpacing.s),
    ) {
        val inner = Modifier.size(CIRCLE_SIDE.dp).clip(CircleShape)
        if (!reduceMotion && !LocalInspectionMode.current) {
            ViraAlphaVideo(ViraVideo.ViriMeditatingLoop, inner, loopStart = 0.0, fill = true)
        } else {
            Image(
                painter = painterResource(R.drawable.viri_meditating),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = inner,
            )
        }
    }
}

/// Les tres fases en píndoles, amb la que toca marcada amb un punt i en negreta: mai només amb el
/// color (`DESIGN_RULES.md` §4). TalkBack ja llegeix la fase al títol.
@Composable
private fun BreathPhasePills(current: BreathPhase) {
    val pills = @Composable {
        BreathPhase.entries.forEach { phase ->
            val isCurrent = phase == current
            Row(
                Modifier
                    .background(ViraColor.surfaceElevated.copy(alpha = if (isCurrent) 1f else 0.7f), CircleShape)
                    .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                if (isCurrent) Box(Modifier.size(6.dp).background(ViraColor.textPrimary, CircleShape))
                Text(
                    "${stringResource(phase.title)} ${phase.seconds}",
                    style = if (isCurrent) ViraFont.footnoteEmphasis else ViraFont.footnote,
                    color = if (isCurrent) ViraColor.textPrimary else ViraColor.textSecondary,
                )
            }
        }
    }
    val hidden = Modifier.clearAndSetSemantics {}
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(hidden, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs), horizontalAlignment = Alignment.CenterHorizontally) { pills() }
    } else {
        Row(hidden, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { pills() }
    }
}

/// Entrada pròpia de la respiració (`HelpBreatheView` sol): el widget, el botó «Respira» del
/// panell ràpid i Mindfulness hi arriben sense passar per Ajuda ara.
@Composable
fun HelpBreatheScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize()) { HelpBreatheContent(onClose) }
}
