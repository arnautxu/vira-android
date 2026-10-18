package cat.vira.android.features.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.util.UUID

/// Concentra't sencer, fase a fase, a l'ample i l'alt d'un iPhone. El rellotge és fix perquè les
/// captures no canviïn d'un dia per l'altre.
private val NOW = Instant.parse("2026-10-18T16:00:00Z")

@Composable
private fun Frame(phase: FocusSessionController.Phase, state: FocusTimerState, completion: FocusSessionController.Completion? = null, content: @Composable () -> Unit) {
    ViraTheme(reduceMotion = true) {
        val scope = rememberCoroutineScope()
        val controller = remember {
            FocusSessionController(store = null, scope = scope, clock = { NOW }).apply {
                linkedTitle = "Deures de mates"
                showForPreview(state, phase, completion)
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (phase == FocusSessionController.Phase.SETUP) FocusAmbience() else Box(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Focus))
            Column(Modifier.fillMaxSize()) {
                FocusHeader(controller, onClose = {}, onNewMode = {})
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusSetup() {
    val tasks = listOf(PlanItem(id = UUID(0, 1), title = "Deures de mates"), PlanItem(id = UUID(0, 2), title = "Llegir el llibre"))
    val modes = FocusMode.all(listOf(FocusProfile(id = UUID(0, 3), name = "Estudi llarg", focusMinutes = 45)))
    Frame(FocusSessionController.Phase.SETUP, FocusTimerState()) {
        FocusSetupContent(
            modes = modes, mode = modes[0], minutes = 25, tasks = tasks, linkedItemID = null, notificationsDenied = false,
            onSelectMode = {}, onSelectMinutes = {}, onSelectTask = {}, onStart = {},
        )
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusRunning() {
    val state = previewState(FocusTimerState.Run.RUNNING, elapsedSeconds = 6 * 60 + 18, now = NOW)
    Frame(FocusSessionController.Phase.RUNNING, state) {
        FocusRunningContent(state, NOW, modeName = null, onPause = {}, onResume = {}, onFinish = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusPaused() {
    val state = previewState(FocusTimerState.Run.PAUSED, elapsedSeconds = 12 * 60, now = NOW)
    Frame(FocusSessionController.Phase.RUNNING, state) {
        FocusRunningContent(state, NOW, modeName = null, onPause = {}, onResume = {}, onFinish = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusCompleted() {
    val completion = FocusSessionController.Completion(
        coinsEarned = 15, wasInterrupted = false, suggestedBreak = FocusKind.SHORT_BREAK, suggestedBreakMinutes = 5, minutesDone = 25,
    )
    Frame(FocusSessionController.Phase.COMPLETED, FocusTimerState(), completion) {
        FocusCompletionContent(completion, onStartBreak = {}, onClose = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusInterrupted() {
    val completion = FocusSessionController.Completion(
        coinsEarned = 0, wasInterrupted = true, suggestedBreak = null, suggestedBreakMinutes = 0, minutesDone = 9,
    )
    Frame(FocusSessionController.Phase.COMPLETED, FocusTimerState(), completion) {
        FocusCompletionContent(completion, onStartBreak = {}, onClose = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusModeEditor() = ViraTheme(reduceMotion = true) {
    FocusProfileEditor(profile = FocusProfile(name = "Estudi llarg", focusMinutes = 45, breakMinutes = 10), onSave = {}, onBack = {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun FocusModeEditorNew() = ViraTheme(reduceMotion = true) {
    FocusProfileEditor(profile = null, onSave = {}, onBack = {})
}

// Prova temporal: el carrusel sol i un medalló sol.
@PreviewTest @androidx.compose.ui.tooling.preview.Preview(widthDp = 393, heightDp = 600) @Composable
fun FocusCarouselProbe() = ViraTheme(reduceMotion = true) {
    Column {
        FocusModeCarousel(FocusMode.builtIns(), FocusMode.builtIns()[0].id, androidx.compose.ui.unit.Dp(260f)) {}
        FocusMedallionButton(FocusMode.builtIns()[0], androidx.compose.ui.unit.Dp(260f), true) {}
    }
}
