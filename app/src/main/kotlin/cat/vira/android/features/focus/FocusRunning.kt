package cat.vira.android.features.focus

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.ceil

/// La sessió en marxa (`FocusSessionView.runningContent` + `FocusRingView`): Viri a sang dins de
/// l'anell, les xifres grans a sota i dues píndoles amb l'estat i el progrés en text.
///
/// Mentre corre, Viri **no diu res**: una frase seria una distracció en l'únic moment del producte
/// que demana el contrari. Només parla en pausa, una frase neutra.
///
/// `now` el passa qui crida perquè les captures siguin fixes; en marxa, `rememberFocusClock` el fa
/// avançar un cop per segon.
@Composable
fun FocusRunningContent(
    state: FocusTimerState,
    now: Instant,
    modeName: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilitySize = LocalViraEnvironment.current.isAccessibilitySize
    BoxWithConstraints(modifier.fillMaxSize()) {
        val byWidth = maxWidth * if (accessibilitySize) 0.46f else 0.76f
        val byHeight = maxHeight * if (accessibilitySize) 0.26f else 0.44f
        val side = minOf(byWidth, byHeight, 320.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            FocusRingView(state, now, side, modeName)
            Spacer(Modifier.weight(1f))
            FocusControls(
                run = state.run,
                onPause = onPause,
                onResume = onResume,
                onFinish = onFinish,
                modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin).padding(bottom = ViraSpacing.l),
            )
        }
    }
}

/// Un rellotge de pantalla que avança cada segon mentre la sessió corre. L'estat de veritat sempre
/// surt de `FocusTimerState`; això només fa redibuixar.
@Composable
fun rememberFocusClock(running: Boolean): Instant {
    var now by remember { mutableStateOf(ViraTime.now()) }
    LaunchedEffect(running) {
        now = ViraTime.now()
        while (running) {
            delay(1000 - (System.currentTimeMillis() % 1000))
            now = ViraTime.now()
        }
    }
    return now
}

@Composable
private fun FocusRingView(state: FocusTimerState, now: Instant, side: Dp, modeName: String?) {
    val paused = state.run == FocusTimerState.Run.PAUSED
    val accessibilitySize = LocalViraEnvironment.current.isAccessibilitySize
    val phase = phaseText(state, modeName)
    val remaining = stringResource(R.string.focus_a11y_remaining, state.remainingMinutesText(now))
    val planned = state.plannedSeconds / 60
    val done = minOf(planned, (state.elapsed(now) / 60).toInt())
    val bubble = stringResource(R.string.focus_paused_viri)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        Box(Modifier.size(side)) {
            FocusRingDial(state, now, side, paused)
            // La bombolla trepitja la vora de l'anell: es llegeix com que surt de Viri.
            if (paused && !accessibilitySize) {
                FocusViriBubble(
                    bubble,
                    Modifier.widthIn(max = side * 0.62f).offset(x = -side * 0.12f, y = -ViraSpacing.l),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = phase
                stateDescription = remaining
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            Text(
                timeText(state, now),
                style = ViraFont.timer,
                color = ViraColor.textPrimary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            val pills: @Composable () -> Unit = {
                FocusPill(phase, glyph = if (paused) FocusGlyphKind.Pause else null, accent = true)
                FocusPill(stringResource(R.string.focus_progress, done, planned))
            }
            if (accessibilitySize) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { pills() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { pills() }
            }
            if (paused && accessibilitySize) {
                FocusViriBubble(bubble, Modifier.padding(horizontal = ViraSpacing.screenMargin))
            }
        }
    }
}

/// Viri estudiant dins la bombolla, i l'anell del temps fet. En pausa, tot el medalló perd color.
@Composable
private fun FocusRingDial(state: FocusTimerState, now: Instant, side: Dp, paused: Boolean) {
    val line = FocusMedallionMetrics.lineWidth(side)
    val desaturate = if (paused) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) }) else null
    Box(Modifier.size(side), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.viri_suggest_focus_2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = desaturate,
            modifier = Modifier
                .size(side - line * 3.2f)
                .shadow(side * 0.06f, CircleShape, ambientColor = ViraColor.accent(ViraArea.Focus), spotColor = ViraColor.accent(ViraArea.Focus))
                .clip(CircleShape),
        )
        FocusRing(side, state.progress(now), isContinuous = !LocalInspectionMode.current)
    }
}

@Composable
private fun phaseText(state: FocusTimerState, modeName: String?): String = when {
    state.run == FocusTimerState.Run.PAUSED -> stringResource(R.string.focus_state_paused)
    state.kind == FocusKind.FOCUS -> modeName ?: stringResource(R.string.focus_kind_focus)
    state.kind == FocusKind.SHORT_BREAK -> stringResource(R.string.focus_kind_short_break)
    else -> stringResource(R.string.focus_kind_long_break)
}

/// En marxa, com el compte enrere natiu de l'iPhone («24:59», «9:59»); quiet, amb dos dígits.
private fun timeText(state: FocusTimerState, now: Instant): String {
    val total = ceil(state.remaining(now)).toInt()
    return if (state.run == FocusTimerState.Run.RUNNING) {
        "%d:%02d".format(total / 60, total % 60)
    } else {
        "%02d:%02d".format(total / 60, total % 60)
    }
}

/// Per a les captures i les proves: un estat ja començat fa `elapsed` segons.
internal fun previewState(run: FocusTimerState.Run, elapsedSeconds: Long, now: Instant, plannedSeconds: Int = 25 * 60): FocusTimerState {
    var state = FocusTimerState(plannedSeconds = plannedSeconds).start(now.minusSeconds(elapsedSeconds))
    if (run == FocusTimerState.Run.PAUSED) state = state.pause(now)
    return state
}
