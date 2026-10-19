package cat.vira.android.features.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

/// Les pantalles de debò que la primera acció obre i que són d'altres fils: el check-in sencer
/// (Emocions) i la sessió de focus (Concentra't). Les posa `app/` quan existeixen; mentrestant el
/// pas obre el marcador de l'esquelet, i tancar-lo sense registre deixa el flux on era, igual que
/// tancar el check-in a l'iPhone.
///
/// - `checkIn(onRecorded, onClose)`: el check-in, que crida `onRecorded` en confirmar un registre.
/// - `focus(seconds, onClose)`: la sessió amb el rellotge ja en marxa, que crida `onClose` en tancar-se.
class EntryFirstActionHosts(
    val checkIn: (@Composable (onRecorded: () -> Unit, onClose: () -> Unit) -> Unit)? = null,
    val focus: (@Composable (seconds: Int, onClose: () -> Unit) -> Unit)? = null,
)

val LocalEntryFirstActionHosts = staticCompositionLocalOf { EntryFirstActionHosts() }

/// E5 · La primera acció, triada a E2 (`EntryFirstActionStep.swift`). Cada variant fa servir **el
/// camí real** de l'app, mai una còpia. **No hi ha cap variant de medicació**: medicació no dona
/// res i no és una acció per celebrar.
@Composable
fun EntryFirstActionStep(intent: EntryStartIntent, services: EntryServices, onDone: (EntryFirstActionOutcome) -> Unit) {
    when (intent) {
        EntryStartIntent.ORGANIZE -> EntryFirstTaskStep(services, onDone)
        EntryStartIntent.EMOTIONS -> EntryFirstCheckInStep(services, onDone)
        EntryStartIntent.FOCUS -> EntryFirstFocusStep(services, onDone)
    }
}

private val SUGGESTIONS = listOf(
    R.string.onboarding_task_suggestion_homework,
    R.string.onboarding_task_suggestion_tidy,
    R.string.onboarding_task_suggestion_training,
)

/// E5a · Una tasca per avui. Un camp i tres suggeriments que l'omplen.
@Composable
fun EntryFirstTaskStep(services: EntryServices, onDone: (EntryFirstActionOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var title by rememberSaveable { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val trimmed = title.trim()

    fun add() {
        if (isSaving || trimmed.isEmpty()) return
        isSaving = true
        failed = false
        scope.launch {
            val saved = try {
                services.addTask(trimmed)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                failed = true
                null
            }
            isSaving = false
            if (saved != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDone(EntryFirstActionOutcome.Task(saved))
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryField(
            label = stringResource(R.string.capture_title_placeholder),
            value = title,
            onValueChange = { title = it; failed = false },
            onSubmit = ::add,
        )
        val suggestions = SUGGESTIONS.map { stringResource(it) }
        // En fila a mida normal; a mides d'accessibilitat, una sota l'altra abans que truncar.
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                suggestions.forEach { suggestion ->
                    EntryChip(suggestion, trimmed == suggestion, { title = suggestion; failed = false }, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                suggestions.forEach { suggestion ->
                    EntryChip(suggestion, trimmed == suggestion, { title = suggestion; failed = false }, Modifier.weight(1f))
                }
            }
        }
        if (failed) EntryInlineWarning(stringResource(R.string.onboarding_task_error))
        EntryPrimaryButton(
            stringResource(R.string.onboarding_task_add),
            ::add,
            enabled = trimmed.isNotEmpty() && !isSaving,
            isLoading = isSaving,
        )
    }
}

/// E5b · Com estàs? Un botó que obre **el check-in de debò** a pantalla completa. Quan s'hi
/// confirma un registre el flux passa a E6; si es tanca sense registre, s'hi queda.
///
/// El registre fet aquí, abans del compte, **es queda al telèfon** fins que `ViraSyncScope` digui
/// que sí (interruptor + permís del tutor). Aquest pas no hi toca res.
@Composable
fun EntryFirstCheckInStep(services: EntryServices, onDone: (EntryFirstActionOutcome) -> Unit) {
    val host = LocalEntryFirstActionHosts.current.checkIn
    val scope = rememberCoroutineScope()
    var isPresented by rememberSaveable { mutableStateOf(false) }
    var before by rememberSaveable(stateSaver = EntryRewardSaver) { mutableStateOf(EntryReward.NONE) }

    EntryPrimaryButton(stringResource(R.string.suggestion_check_in_action), {
        scope.launch {
            before = services.rewardSnapshot()
            isPresented = true
        }
    })
    if (isPresented) {
        val close = { isPresented = false }
        val recorded = {
            isPresented = false
            scope.launch { onDone(EntryFirstActionOutcome.CheckIn(services.rewardSnapshot().earned(before))) }
            Unit
        }
        EntryFullScreen(onDismiss = close) {
            if (host != null) host(recorded, close) else EntryPendingHost(close)
        }
    }
}

/// E5c · Dos minuts? "Comencem" obre **la sessió de focus de debò** amb dos minuts i el rellotge
/// ja en marxa. Completada → E6 amb el que hagi donat; cancel·lada → E6 sense recompensa i **sense
/// cap text de culpa**. Sense cap sessió creada no ha passat res: es queda a E5.
@Composable
fun EntryFirstFocusStep(services: EntryServices, onDone: (EntryFirstActionOutcome) -> Unit) {
    val host = LocalEntryFirstActionHosts.current.focus
    val scope = rememberCoroutineScope()
    // Es desa el procés que l'ha obert, i no un sí o un no: girar la pantalla la torna a obrir sobre
    // la sessió viva, però si Android ha matat el procés el controlador de la sessió ja no hi és, i
    // tornar-la a obrir en començaria una altra de duplicada.
    var presentedInProcess by rememberSaveable { mutableStateOf("") }
    var before by rememberSaveable(stateSaver = EntryRewardSaver) { mutableStateOf(EntryReward.NONE) }
    // Desat com a mil·lisegons: si la pantalla gira a mig focus, la sessió viva s'ha de seguir
    // comptant des d'aquí, i no des d'un tall nou que la deixaria fora.
    var startedAtMillis by rememberSaveable { mutableStateOf(Instant.EPOCH.toEpochMilli()) }

    EntryPrimaryButton(stringResource(R.string.onboarding_focus_start), {
        scope.launch {
            before = services.rewardSnapshot()
            // Un segon de marge: la sessió es crea just després de presentar-se.
            startedAtMillis = entryNow().minusSeconds(1).toEpochMilli()
            presentedInProcess = ProcessToken
        }
    })
    if (presentedInProcess == ProcessToken) {
        val close: () -> Unit = {
            presentedInProcess = ""
            scope.launch {
                val completed = services.focusCompletedSince(Instant.ofEpochMilli(startedAtMillis)) ?: return@launch
                onDone(EntryFirstActionOutcome.Focus(completed, services.rewardSnapshot().earned(before)))
            }
        }
        EntryFullScreen(onDismiss = close) {
            if (host != null) host(FOCUS_SECONDS, close) else EntryPendingHost(close)
        }
    }
}

/// El que es tenia abans d'obrir la primera acció, perquè giri la pantalla i encara es pugui
/// comptar el que ha donat.
private val EntryRewardSaver = Saver<EntryReward, IntArray>(
    save = { intArrayOf(it.coins, it.xp) },
    restore = { EntryReward(it[0], it[1]) },
)

/// Diferent a cada procés: el que una pantalla desada porta d'un procés mort no hi coincideix.
private val ProcessToken = java.util.UUID.randomUUID().toString()

/// Els dos minuts del focus de la primera acció.
const val FOCUS_SECONDS = 120

@Composable
private fun EntryFullScreen(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(ViraColor.surfaceElevated)) { content() }
    }
}

/// El marcador de l'esquelet, amb una manera de sortir-ne, mentre la pantalla de debò no hi és.
@Composable
private fun EntryPendingHost(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        PendingScreen()
        EntryCapsuleButton(
            stringResource(R.string.action_close),
            onClose,
            Modifier.align(Alignment.TopEnd).padding(ViraSpacing.s),
            bold = true,
        )
    }
}
