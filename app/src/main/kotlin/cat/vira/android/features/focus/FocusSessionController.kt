package cat.vira.android.features.focus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.kind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

/// Orquestra una sessió de focus (`FocusSessionController.swift`): la màquina d'estats pura de
/// `FocusTimerState`, la persistència, l'avís fix, l'alarma de final i les ViriCoins.
///
/// L'ordre és sempre el mateix a cada transició: primer la persistència i l'estat, després els
/// efectes externs (`FocusEffects`), perquè una fallada del sistema no deixi mai la sessió a mitges.
///
/// L'estat és de Compose (`mutableStateOf`): la pantalla el llegeix directament, i l'avís fix el
/// llegeix de `FocusRuntime`.
class FocusSessionController(
    private val store: ViraStore?,
    private val scope: CoroutineScope,
    private val effects: FocusEffects = FocusEffects.None,
    private val clock: () -> Instant = ViraTime::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    kind: FocusKind = FocusKind.FOCUS,
    plannedSeconds: Int = 25 * 60,
    linkedTitle: String? = null,
    linkedItemID: UUID? = null,
) {
    enum class Phase { SETUP, RUNNING, COMPLETED }

    data class Completion(
        val coinsEarned: Int,
        val wasInterrupted: Boolean,
        val suggestedBreak: FocusKind?,
        /// Minuts del descans proposat, ja resolts per la sessió: la vista no en té cap d'escrit.
        val suggestedBreakMinutes: Int,
        /// Minuts sencers fets, completada o no: a la sortida interrompuda és el que sí que ha passat.
        val minutesDone: Int = 0,
    )

    var state by mutableStateOf(FocusTimerState(kind = kind, plannedSeconds = plannedSeconds))
        private set
    var phase by mutableStateOf(Phase.SETUP)
        private set
    var completion by mutableStateOf<Completion?>(null)
        private set
    var linkedTitle by mutableStateOf(linkedTitle)
    var linkedItemID by mutableStateOf(linkedItemID)

    /// El mode actiu. La màquina d'estats només coneix la fase; la identitat del mode viu aquí, que
    /// és qui la desa a la sessió i la fa servir en acabar.
    var mode by mutableStateOf(FocusMode.builtIns().firstOrNull { it.kind == kind } ?: FocusMode.builtIns()[0])
        private set

    private var session: FocusSession? = null
    private var starting = false

    /// La sessió oberta ara mateix, perquè la recuperació no la tanqui per sota.
    internal val openSessionID get() = session?.takeIf { it.endedAt == null }?.id

    /// Completa la sessió tota sola quan el temps s'esgota amb l'app viva. Amb el telèfon adormit,
    /// el rellotge d'aquest `delay` s'atura: per això hi ha també l'alarma de `FocusEffects`, i
    /// `reconcile` és idempotent.
    private var completionJob: Job? = null

    // Preparació

    /// Resol el perfil demanat per `FocusStartRequest.profileID`, si encara no ha començat.
    suspend fun applyRequestedProfile(id: UUID?) {
        if (id == null || store == null || state.run != FocusTimerState.Run.IDLE) return
        val profile = store.db.focusProfiles().byId(id) ?: return
        select(FocusMode.of(profile))
    }

    /// Tria un mode del carrusel. La fase i la durada van a la màquina d'estats.
    fun select(mode: FocusMode, seconds: Int? = null) {
        this.mode = mode
        state = state.select(mode.kind, seconds ?: mode.defaultSeconds)
    }

    /// Canvia només la durada del mode actiu (el − i el + del panell).
    fun select(seconds: Int) {
        state = state.select(mode.kind, seconds)
    }

    /// El botó de prova de DEBUG, que força una sessió curta sense perdre el mode.
    fun select(kind: FocusKind, seconds: Int) {
        if (mode.kind != kind) mode = FocusMode.builtIns().firstOrNull { it.kind == kind } ?: FocusMode.builtIns()[0]
        state = state.select(kind, seconds)
    }

    // Transicions

    suspend fun start() {
        if (store == null || phase == Phase.RUNNING || starting) return
        // Dos tocs seguits a «Comença» abans que la fila es desi crearien dues sessions.
        starting = true
        try {
            begin(store)
        } finally {
            starting = false
        }
    }

    private suspend fun begin(store: ViraStore) {
        val isProfile = mode.profileID != null
        val now = clock()
        val created = store.create(
            store.db.focusSessions(),
            FocusSession(
                startedAt = now,
                plannedSeconds = state.plannedSeconds,
                kindRaw = state.kind.raw,
                linkedItemTitle = if (mode.showsLinkedTask) linkedTitle else null,
                profileID = mode.profileID,
                profileName = if (isProfile) mode.name else null,
                mutesNotifications = mode.mutesNotifications,
                profileBreakMinutes = if (isProfile) mode.breakMinutes(FocusKind.SHORT_BREAK) else null,
            ),
        )
        session = created
        state = state.start(now)
        phase = Phase.RUNNING
        scheduleCompletion()
        effects.started(this)
    }

    fun pause() {
        if (state.run != FocusTimerState.Run.RUNNING) return
        state = state.pause(clock())
        completionJob?.cancel()
        effects.paused(this)
    }

    fun resume() {
        if (state.run != FocusTimerState.Run.PAUSED) return
        state = state.resume(clock())
        scheduleCompletion()
        effects.resumed(this)
    }

    /// Sessió interrompuda: es desa igual, amb `completed = false`, i no es penalitza.
    suspend fun stop() {
        if (state.run != FocusTimerState.Run.RUNNING && state.run != FocusTimerState.Run.PAUSED) return
        finish(completed = false, now = clock())
    }

    /// Cridat en tornar a primer pla i quan sona l'alarma: si el temps ja s'havia esgotat, la
    /// sessió es completa ara. Sense això, tornar a l'app deixaria una sessió eternament en marxa.
    suspend fun reconcile(now: Instant = clock()) {
        if (state.run != FocusTimerState.Run.RUNNING) return
        if (!state.isFinished(now)) return
        finish(completed = true, now = now)
    }

    private fun scheduleCompletion() {
        completionJob?.cancel()
        val end = state.endDate ?: return
        val millis = maxOf(0L, Duration.between(clock(), end).toMillis()) + 50
        completionJob = scope.launch {
            delay(millis)
            reconcile()
        }
    }

    private suspend fun finish(completed: Boolean, now: Instant) {
        val store = store ?: return
        val open = session ?: return
        // Acabada ara mateix i no fa estona amb l'app tancada: si no, l'avís ja ha sonat.
        val endsNow = state.endDate?.let { abs(Duration.between(it, now).seconds) < 5 } ?: false
        completionJob?.cancel()
        state = state.finish(now)
        val closed = store.save(store.db.focusSessions(), open.finished(at = now, completed = completed))
        session = closed

        val coins = if (completed) effects.reward(closed, now) else 0
        val suggested = if (completed && closed.kind == FocusKind.FOCUS) suggestedBreakKind(store, closed, now) else null
        completion = Completion(
            coinsEarned = coins,
            wasInterrupted = !completed,
            suggestedBreak = suggested,
            // De la sessió, no del mode actiu: el descans d'un perfil viatja amb la sessió.
            suggestedBreakMinutes = suggested?.let { closed.profileBreakMinutes ?: if (it == FocusKind.LONG_BREAK) 15 else 5 } ?: 0,
            minutesDone = (state.elapsed(now) / 60).toInt(),
        )
        phase = Phase.COMPLETED
        effects.finished(this, completed = completed, endsNow = endsNow, kind = closed.kind)
    }

    /// Cada 4a sessió de focus completada del dia suggereix un descans llarg; si no, un de curt.
    /// Un mode personalitzat sempre proposa el seu: té un sol valor de descans.
    private suspend fun suggestedBreakKind(store: ViraStore, closed: FocusSession, now: Instant): FocusKind {
        if (closed.profileID != null) return FocusKind.SHORT_BREAK
        val startOfDay = now.atZone(zone()).toLocalDate().atStartOfDay(zone()).toInstant()
        val count = store.db.focusSessions().all().count {
            it.kind == FocusKind.FOCUS && it.completed && !it.startedAt.isBefore(startOfDay)
        }
        return if (count > 0 && count % 4 == 0) FocusKind.LONG_BREAK else FocusKind.SHORT_BREAK
    }

    /// Comença el descans proposat: una sessió pròpia, no un sub-estat de la que s'acaba. El mode
    /// es conserva, encara que el descans no doni monedes.
    suspend fun startSuggestedBreak() {
        val done = completion ?: return
        val kind = done.suggestedBreak ?: return
        state = FocusTimerState(kind = kind, plannedSeconds = done.suggestedBreakMinutes * 60)
        completion = null
        session = null
        linkedTitle = null
        linkedItemID = null
        phase = Phase.SETUP
        start()
    }

    /// Per a les captures: la sessió en un estat concret, sense base de dades ni rellotge.
    internal fun showForPreview(state: FocusTimerState, phase: Phase, completion: Completion? = null, mode: FocusMode = this.mode) {
        this.state = state
        this.phase = phase
        this.completion = completion
        this.mode = mode
    }

    /// El que l'avís fix ha de dir: la fase, si està en pausa, i el final o el temps que queda.
    fun remainingSeconds(): Double = state.remaining(clock())
}
