package cat.vira.android.features.emotions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.help.HelpBreathingHaptics
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/// Una sessió guiada: la veu, i a la pantalla el mateix que diu (`MindfulnessSessionView`).
///
/// Comença sola en entrar, perquè triar l'exercici ja és la decisió de fer-lo. «Pausa» i
/// «Continua» són l'única acció de la pantalla; «Para» a la barra la deixa a mitges sense cap
/// retret, i en acabar no hi ha premi: dues sortides i prou (`MindfulnessFinished`).
@Composable
internal fun MindfulnessSessionScreen(
    exercise: MindfulnessExercise,
    onAnother: () -> Unit,
    onCheckIn: () -> Unit,
    onFocus: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Es llegeix en crear la pantalla: si no, el primer fotograma ensenyava l'error.
    val script = remember(exercise) { MindfulnessScript.load(context, exercise) }
    val haptics = remember { HelpBreathingHaptics(context) }
    val player = remember(exercise) { MindfulnessPlayer(context) }
    val view = LocalView.current

    DisposableEffect(exercise) {
        if (script != null) {
            haptics.start()
            player.start(MindfulnessScript.audioId(context, exercise))
        }
        onDispose {
            player.stop()
            haptics.stop()
            view.keepScreenOn = false
        }
    }
    // El reproductor no avisa mentre sona: deu lectures per segon per al cercle i el text.
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.tick()
            delay(100)
        }
    }
    // Pantalla encesa mentre guia, menys a «Mòbil a dormir», que és per deixar-la.
    LaunchedEffect(player.isPlaying) {
        view.keepScreenOn = player.isPlaying && exercise != MindfulnessExercise.PHONE_SLEEP
    }
    LaunchedEffect(player.isFinished) { if (player.isFinished) onFinished() }
    val segment = script?.segment(player.currentTime)
    // El ritme també al palmell, només a la respiració i només mentre sona: en pausar es talla, i
    // en continuar dins del mateix tram vibra el que en queda.
    LaunchedEffect(segment?.start, player.isPlaying) {
        if (!player.isPlaying) {
            haptics.stop()
            haptics.start()
            return@LaunchedEffect
        }
        val current = segment ?: return@LaunchedEffect
        val phase = current.phase
        if (current.kind == MindfulnessScript.Kind.BREATH &&
            (phase == MindfulnessScript.Phase.INHALE || phase == MindfulnessScript.Phase.EXHALE)
        ) {
            // Arrodonit, no truncat: el tic de 100 ms arriba quan el tram ja ha començat.
            val remaining = (current.end - player.currentTime).roundToInt()
            if (remaining > 0) haptics.play(phase == MindfulnessScript.Phase.INHALE, remaining)
        }
    }

    MindfulnessSessionContent(exercise, script, player.currentTime, player.isPlaying, player.isFinished, player.failed,
        onToggle = player::togglePlayback, onAnother = onAnother, onCheckIn = onCheckIn, onFocus = onFocus, modifier = modifier)
}

/// La sessió dins de la pestanya, amb la barra: enrere i, mentre sona, «Para» (`MindfulnessSessionView`).
@Composable
internal fun MindfulnessSession(exercise: MindfulnessExercise, onClose: () -> Unit, onCheckIn: () -> Unit, onFocus: () -> Unit) {
    var finished by remember(exercise) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PushedHeader(
            mindfulnessString(exercise.titleKey),
            trailing = if (finished) null else stringResource(R.string.mindfulness_stop) to onClose,
            onBack = onClose,
        )
        MindfulnessSessionScreen(
            exercise,
            onAnother = onClose,
            onCheckIn = onCheckIn,
            onFocus = onFocus,
            onFinished = { finished = true },
            modifier = Modifier.weight(1f),
        )
    }
}

/// El que ensenya la sessió, sense reproductor: el fan servir la pantalla i les captures.
@Composable
internal fun MindfulnessSessionContent(
    exercise: MindfulnessExercise,
    script: MindfulnessScript?,
    currentTime: Double,
    isPlaying: Boolean,
    isFinished: Boolean,
    failed: Boolean,
    onToggle: () -> Unit,
    onAnother: () -> Unit,
    onCheckIn: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = when {
        isFinished -> 2
        failed || script == null -> 1
        else -> 0
    }
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(ViraMotion.standard()) togetherWith fadeOut(ViraMotion.standard()) },
        modifier = modifier.fillMaxSize().viraSectionBackground(ViraArea.Emotions),
        label = "mindfulnessSession",
    ) { current ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ViraSpacing.screenMargin)
                .padding(top = ViraSpacing.xs, bottom = viraTabBarClearance()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            when {
                current == 2 -> MindfulnessFinished(exercise, script?.steps ?: 0, onAnother, onCheckIn, onFocus)
                current == 1 || script == null -> ViraEmptyState(
                    title = stringResource(R.string.mindfulness_error_title),
                    message = stringResource(R.string.mindfulness_error_message),
                    modifier = Modifier.padding(top = ViraSpacing.xxl),
                )
                else -> {
                    val segment = script.segment(currentTime)
                    MindfulnessRunningCard(script, segment, currentTime)
                    ViraActionButton(
                        stringResource(if (isPlaying) R.string.mindfulness_pause else R.string.mindfulness_resume),
                        onToggle,
                        Modifier.fillMaxWidth(),
                    )
                    if (script.steps > 1) StepPills(script, segment?.step)
                }
            }
        }
    }
}

/// Els passos amb nom, en píndoles: fet amb ✓, l'actual marcat amb vora i negreta. Mai només el
/// color (`DESIGN_RULES.md` §4). TalkBack ja llegeix el pas a la targeta.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPills(script: MindfulnessScript, current: Int?) {
    val titles = remember(script) { script.segments.mapNotNull { it.stepTitle }.distinct() }
    FlowRow(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        titles.forEachIndexed { index, key ->
            val isDone = (current ?: -1) > index
            val isCurrent = current == index
            val ink = when {
                isCurrent -> ViraColor.textPrimary
                isDone -> ViraColor.onSurface(ViraArea.Emotions)
                else -> ViraColor.textSecondary
            }
            Row(
                Modifier
                    .background(ViraColor.surfaceElevated, CircleShape)
                    .then(if (isCurrent) Modifier.border(2.dp, ViraColor.accent(ViraArea.Emotions), CircleShape) else Modifier)
                    .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                if (isDone) EmotionGlyphIcon(EmotionGlyph.Checkmark, ink, 12.dp)
                Text(
                    mindfulnessString(key),
                    style = if (isCurrent || isDone) ViraFont.footnoteEmphasis else ViraFont.footnote,
                    color = ink,
                )
            }
        }
    }
}
