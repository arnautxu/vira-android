package cat.vira.android.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.components.ActiveCompletion
import cat.vira.android.core.design.components.CompletionPhase
import cat.vira.android.core.design.components.PendingCompletion
import cat.vira.android.core.design.components.ProvideViraCompletions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/// La finestra de «Desfer» i la recompensa que ve després, per a **tota** l'app
/// (`ViraCompletionQueue` de l'iPhone). Viu a l'arrel perquè ha de sobreviure a un canvi de
/// pestanya: marcar un hàbit des d'Avui i des de Pla concedeix la mateixa cosa.
///
/// Cap vista pròpia: només porta el cronòmetre i publica l'estat. La fila amb l'`id` actiu és qui
/// dibuixa el «Desfer» i la revelació dins de la seva targeta.
///
/// Encara no hi és (arribarà amb els fils que en són amos): les monedes que volen cap a la
/// capçalera, el moment de repte i la retenció de la celebració de ratxa.
@Composable
fun ViraCompletionQueue(content: @Composable () -> Unit) {
    val backend = LocalViraBackend.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val celebrationsEnabled by rememberPreference(backend.preferences, ViraPreferences.CELEBRATIONS_ENABLED, true)

    val queue = remember(backend) {
        CompletionQueue(
            scope = scope,
            commit = { reason -> ViraFeatureSlots.commitReward(backend, reason) },
            isFirstHabitOfDay = {
                val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                backend.db.habitLogs().all().count { it.day == today } <= 1
            },
            undoWindowMillis = { if (usesAssistiveInput(context)) 5_000 else 2_000 },
            announce = { coins, xp -> announce(view, context, coins, xp) },
        )
    }
    queue.celebrationsEnabled = celebrationsEnabled

    ProvideViraCompletions(present = queue::present, undo = queue::undo, active = queue.active) {
        content()
        queue.habitCelebration?.let { celebration ->
            ViraFeatureSlots.HabitCelebration(celebration.title, celebration.coins, celebration.xp) {
                queue.habitCelebration = null
            }
        }
    }
}

/// El que necessita la celebració de l'hàbit.
internal data class HabitCelebration(val title: String, val coins: Int, val xp: Int)

/// La màquina d'estats, separada de la vista perquè es pugui provar.
@Stable
internal class CompletionQueue(
    private val scope: CoroutineScope,
    /// Concedeix la recompensa i retorna (monedes, XP).
    private val commit: suspend (CoinReason) -> Pair<Int, Int>,
    private val isFirstHabitOfDay: suspend () -> Boolean,
    private val undoWindowMillis: () -> Long,
    private val announce: (coins: Int, xp: Int) -> Unit = { _, _ -> },
) {
    var active by mutableStateOf<ActiveCompletion?>(null)
        private set
    var habitCelebration by mutableStateOf<HabitCelebration?>(null)
    var celebrationsEnabled = true

    private var pending: PendingCompletion? = null
    private var timeline: Job? = null

    fun present(completion: PendingCompletion) {
        timeline?.cancel()
        val existing = pending
        if (existing != null && existing.id != completion.id) {
            // L'usuari ja ha passat a una altra cosa: l'anterior es dona per feta, sense revelació.
            scope.launch { withContext(NonCancellable) { commitCompletion(existing) } }
        }
        pending = completion
        active = ActiveCompletion(completion.id, CompletionPhase.Pending)
        timeline = scope.launch { run(completion) }
    }

    fun undo(id: String) {
        val current = pending ?: return
        if (current.id != id) return
        timeline?.cancel()
        current.onUndo()
        pending = null
        active = null
    }

    /// 2 s de «Desfer» (5 amb TalkBack o accés amb interruptors), concessió i revelació, i 1,8 s
    /// més abans de desaparèixer sola.
    private suspend fun run(completion: PendingCompletion) {
        delay(undoWindowMillis())
        // A partir d'aquí la recompensa és seva: res no la pot aturar a mitges.
        val (coins, xp) = withContext(NonCancellable) { commitCompletion(completion) }
        // **Només el primer hàbit del dia** obre la celebració (Arnau, 2026-08-10).
        val opensHabitCelebration = completion.reason == CoinReason.HABIT && celebrationsEnabled &&
            completion.title != null && isFirstHabitOfDay()
        active = ActiveCompletion(completion.id, CompletionPhase.Revealing(coins, xp))
        announce(coins, xp)
        if (opensHabitCelebration) habitCelebration = HabitCelebration(completion.title!!, coins, xp)

        delay(REVEAL_MILLIS)
        if (pending === completion) {
            pending = null
            active = null
        }
    }

    private suspend fun commitCompletion(completion: PendingCompletion): Pair<Int, Int> {
        val result = commit(completion.reason)
        completion.onCommit?.invoke()
        return result
    }

    private companion object {
        const val REVEAL_MILLIS = 1_800L
    }
}

/// TalkBack o l'accés amb interruptors: arribar al «Desfer» amb el gest ja en gasta més de dos
/// segons (`ACCESSIBILITY_REQUIREMENTS.md`, temps suficient).
private fun usesAssistiveInput(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    if (!manager.isEnabled) return false
    return manager.isTouchExplorationEnabled ||
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id.contains("switchaccess", ignoreCase = true) }
}

/// La revelació és visual i curta: qui fa servir TalkBack no la veu passar, així que se li diu.
private fun announce(view: View, context: Context, coins: Int, xp: Int) {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    if (manager?.isTouchExplorationEnabled != true) return
    val text = listOfNotNull(
        coins.takeIf { it > 0 }?.let { context.getString(R.string.focus_done_coins, it) },
        xp.takeIf { it > 0 }?.let { context.getString(R.string.toast_reward_xp, it) },
    ).joinToString(", ")
    if (text.isNotEmpty()) view.announceForAccessibility(text)
}
