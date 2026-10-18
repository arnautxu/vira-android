package cat.vira.android.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import cat.vira.android.R
import cat.vira.android.ViraApp
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.components.ActiveCompletion
import cat.vira.android.core.design.components.CompletionPhase
import cat.vira.android.core.design.components.PendingCompletion
import cat.vira.android.core.design.components.ProvideViraCompletions
import cat.vira.android.features.progress.ChallengeProgressMoment
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ViraChallenges
import cat.vira.android.features.progress.logic.ViraDays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/// La finestra de «Desfer» i la recompensa que ve després, per a **tota** l'app
/// (`ViraCompletionQueue` de l'iPhone). Viu a l'arrel perquè ha de sobreviure a un canvi de
/// pestanya: marcar un hàbit des d'Avui i des de Pla concedeix la mateixa cosa.
///
/// Cap vista pròpia: només porta el cronòmetre i publica l'estat. La fila amb l'`id` actiu és qui
/// dibuixa el «Desfer» i la revelació dins de la seva targeta.
///
/// Retén la celebració de ratxa mentre concedeix i mentre hi ha la celebració de l'hàbit o el
/// moment de repte a la pantalla (`deferStreakCelebration`): dues pantalles completes alhora es
/// trepitjarien. Encara no hi és: les monedes que volen cap a la capçalera.
@Composable
fun ViraCompletionQueue(content: @Composable () -> Unit) {
    val backend = LocalViraBackend.current
    val context = LocalContext.current
    val view = LocalView.current
    val celebrationsEnabled by rememberPreference(backend.preferences, ViraPreferences.CELEBRATIONS_ENABLED, true)
    val challengesEnabled by rememberPreference(backend.preferences, ViraPreferences.CHALLENGES_ENABLED, true)

    // La cua és del procés, no d'aquesta composició: un gir de pantalla a mitja finestra de
    // «Desfer» no ha de perdre la recompensa d'una cosa que ja s'ha marcat.
    val queue = (context.applicationContext as ViraApp).completions
    queue.celebrationsEnabled = celebrationsEnabled
    queue.challengesEnabled = challengesEnabled
    DisposableEffect(queue, view) {
        queue.announce = { coins, xp -> announce(view, context, coins, xp) }
        onDispose { queue.announce = { _, _ -> } }
    }

    ProvideViraCompletions(present = queue::present, undo = queue::undo, active = queue.active) {
        content()
        queue.habitCelebration?.let { celebration ->
            ViraFeatureSlots.HabitCelebration(celebration.title, celebration.coins, celebration.xp) {
                queue.habitCelebration = null
                queue.deferStreak(false)
            }
        }
        queue.challengeMoment?.let { moment ->
            val navigator = LocalViraNavigator.current
            val close = {
                queue.challengeMoment = null
                queue.deferStreak(false)
            }
            ViraFullScreen(onDismiss = close) {
                ChallengeProgressMoment(
                    item = moment.item,
                    progress = moment.progress,
                    onContinue = close,
                    onOpenChallenges = {
                        close()
                        navigator.navigate(ViraDestination.Challenges)
                    },
                )
            }
        }
    }
}

/// La cua de tot el procés, amb les recompenses de `backend`. La crea `ViraApp`.
internal fun processCompletionQueue(app: ViraApp, scope: CoroutineScope): CompletionQueue = CompletionQueue(
    scope = scope,
    commit = { reason, at -> ViraFeatureSlots.commitReward(app.backend, reason, at) },
    isFirstHabitOfDay = {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        app.backend.db.habitLogs().all().count { it.day == today } <= 1
    },
    undoWindowMillis = { undoWindowMillis(app) },
    challengeCompleted = { reason, shown -> completedChallenge(app, reason, shown) },
)

/// El primer repte d'aquest motiu que l'acció acaba de completar i encara no s'ha recollit.
private suspend fun completedChallenge(app: ViraApp, reason: CoinReason, shown: Set<String>): ChallengeMoment? {
    val db = app.backend.db
    val now = ViraTime.now()
    val days = ViraDays(ZoneId.systemDefault())
    val ledger = db.coinTransactions().all()
    val claims = db.challengeClaims().all()
    // La mateixa clau es pot recollir cada dia o setmana: l'inici del cicle separa «ja t'ho he
    // ensenyat avui» de «l'has tornat a completar la setmana següent».
    fun key(item: ChallengeItem) = "${item.key}-${ViraChallenges.cycleStart(item.period, now, days).epochSecond}"
    // Els ja ensenyats queden fora **en triar**: un diari sense recollir no pot tapar el setmanal.
    val item = ChallengeItem.catalog.firstOrNull {
        it.requirementReason == reason && key(it) !in shown &&
            ViraChallenges.isCompleted(it, ledger, now, days) && !ViraChallenges.isClaimed(it, claims, now, days)
    } ?: return null
    return ChallengeMoment(item = item, progress = ViraChallenges.progress(item, ledger, now, days), key = key(item))
}

/// El que necessita la celebració de l'hàbit.
internal data class HabitCelebration(val title: String, val coins: Int, val xp: Int)

/// Un repte que l'acció acaba d'obrir (`ChallengeProgressMoment` de l'iPhone).
internal data class ChallengeMoment(val item: ChallengeItem, val progress: Int, val key: String)

/// La màquina d'estats, separada de la vista perquè es pugui provar.
@Stable
internal class CompletionQueue(
    private val scope: CoroutineScope,
    /// Concedeix la recompensa i retorna (monedes, XP). Rep l'instant de l'acció, no el de la
    /// concessió: marcar una cosa a les 23:59:59 compta per al dia en què s'ha marcat.
    private val commit: suspend (CoinReason, Instant) -> Pair<Int, Int>,
    private val isFirstHabitOfDay: suspend () -> Boolean,
    private val undoWindowMillis: () -> Long,
    /// El repte que l'acció acaba de completar, si n'hi ha cap per recollir.
    private val challengeCompleted: suspend (CoinReason, Set<String>) -> ChallengeMoment? = { _, _ -> null },
    private val clock: () -> Instant = ViraTime::now,
) {
    /// Retén (`true`) o allibera (`false`) la celebració de ratxa. La connecta la cua de
    /// celebracions.
    var deferStreak: (Boolean) -> Unit = {}
    var challengeMoment by mutableStateOf<ChallengeMoment?>(null)
    var challengesEnabled = true
    /// Un moment per repte i **cicle**: veure'l és útil, repetir-lo a cada hàbit no.
    private val shownChallengeKeys = mutableSetOf<String>()
    /// Quan s'ha tocat cada acció pendent.
    private val actionAt = mutableMapOf<PendingCompletion, Instant>()
    /// La revelació dita per TalkBack. La posa la pantalla visible: cal una vista on anunciar-la.
    var announce: (coins: Int, xp: Int) -> Unit = { _, _ -> }
    var active by mutableStateOf<ActiveCompletion?>(null)
        private set
    var habitCelebration by mutableStateOf<HabitCelebration?>(null)
    var celebrationsEnabled = true

    /// Encara dins de la finestra de «Desfer»: es pot desfer o, si n'arriba una altra, concedir.
    private var pending: PendingCompletion? = null
    /// Ja concedint-se o revelant-se: no es pot desfer ni tornar a concedir.
    private var revealing: PendingCompletion? = null
    private var timeline: Job? = null

    fun present(completion: PendingCompletion) {
        timeline?.cancel()
        val existing = pending
        if (existing != null && existing.id != completion.id) {
            // L'usuari ja ha passat a una altra cosa: l'anterior es dona per feta, sense revelació.
            scope.launch { withContext(NonCancellable) { commitCompletion(existing) } }
        }
        // La que ja es concedia acaba de concedir-se sola, però ja no es revela: ara mana la nova.
        revealing = null
        actionAt[completion] = clock()
        pending = completion
        active = ActiveCompletion(completion.id, CompletionPhase.Pending)
        timeline = scope.launch { run(completion) }
    }

    fun undo(id: String) {
        val current = pending ?: return
        if (current.id != id) return
        timeline?.cancel()
        current.onUndo()
        actionAt.remove(current)
        pending = null
        active = null
    }

    /// 2 s de «Desfer» (5 amb TalkBack o accés amb interruptors), concessió i revelació, i 1,8 s
    /// més abans de desaparèixer sola.
    private suspend fun run(completion: PendingCompletion) {
        delay(undoWindowMillis())
        // A partir d'aquí la recompensa és seva: res no la pot aturar a mitges, i surt de `pending`
        // abans de concedir-se perquè una acció nova no la torni a concedir.
        pending = null
        revealing = completion
        // Es reté **abans** de concedir: la ratxa pot avançar dins de la concessió, i llavors ja
        // seria tard. S'allibera de seguida si no s'obre res, i si s'obre, en tancar-se.
        deferStreak(true)
        // Qui retén és qui allibera: si una acció nova cancel·la aquesta, la retenció no pot quedar
        // penjada i apagar les celebracions fins que l'app torni a començar.
        var held = true
        try {
            val (coins, xp) = withContext(NonCancellable) { commitCompletion(completion) }
            val moment = nextChallengeMoment(completion.reason)
            // **Només el primer hàbit del dia** obre la celebració (Arnau, 2026-08-10).
            val opensHabitCelebration = completion.reason == CoinReason.HABIT && celebrationsEnabled &&
                completion.title != null && isFirstHabitOfDay()
            if (revealing !== completion) return
            active = ActiveCompletion(completion.id, CompletionPhase.Revealing(coins, xp))
            announce(coins, xp)
            when {
                // La retenció passa a la pantalla que s'obre: l'allibera en tancar-se.
                moment != null -> { challengeMoment = moment; held = false }
                opensHabitCelebration -> { habitCelebration = HabitCelebration(completion.title!!, coins, xp); held = false }
            }
        } finally {
            if (held) deferStreak(false)
        }

        delay(REVEAL_MILLIS)
        if (revealing === completion) {
            revealing = null
            active = null
        }
    }

    private suspend fun nextChallengeMoment(reason: CoinReason): ChallengeMoment? {
        if (!celebrationsEnabled || !challengesEnabled) return null
        val moment = challengeCompleted(reason, shownChallengeKeys.toSet()) ?: return null
        if (!shownChallengeKeys.add(moment.key)) return null
        return moment
    }

    private suspend fun commitCompletion(completion: PendingCompletion): Pair<Int, Int> {
        val result = commit(completion.reason, actionAt.remove(completion) ?: clock())
        completion.onCommit?.invoke()
        return result
    }

    private companion object {
        const val REVEAL_MILLIS = 1_800L
    }
}

/// 2 s, o el que l'usuari hagi demanat a Accessibilitat → «Temps per actuar». Amb TalkBack o
/// l'accés amb interruptors, 5 s com a mínim: arribar al «Desfer» amb el gest ja en gasta més de
/// dos (`ACCESSIBILITY_REQUIREMENTS.md`, temps suficient).
private fun undoWindowMillis(context: Context): Long {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return 2_000
    val requested = manager.getRecommendedTimeoutMillis(2_000, AccessibilityManager.FLAG_CONTENT_CONTROLS).toLong()
    return if (usesAssistiveInput(manager)) maxOf(requested, 5_000) else requested
}

private fun usesAssistiveInput(manager: AccessibilityManager): Boolean {
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
