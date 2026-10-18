package cat.vira.android.features.focus

import cat.vira.android.core.data.model.FocusKind
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/// Estat pur d'un temporitzador de focus (`FocusTimerState.swift`).
///
/// El temps mai no és un comptador que s'incrementa: és **aritmètica de dates**. Tot el que llegeix
/// el temps pren `now` per paràmetre, així que es pot provar sense esperar i es comporta igual si
/// l'app ha estat tancada un minut o un dia: `reconcile` simplement torna a llegir l'hora.
///
/// Immutable: cada transició torna un estat nou, perquè Compose vegi el canvi.
data class FocusTimerState(
    val kind: FocusKind = FocusKind.FOCUS,
    val plannedSeconds: Int = 25 * 60,
    val run: Run = Run.IDLE,
    /// Inici del tram en marxa. `null` quan no corre (aturat, en pausa o acabat).
    val startedAt: Instant? = null,
    /// Segons acumulats de trams anteriors (abans de la pausa actual).
    val accumulated: Double = 0.0,
) {
    enum class Run { IDLE, RUNNING, PAUSED, FINISHED }

    // Transicions

    /// Només vàlid abans de començar. Triar "Descans" ha de canviar el temps que es veu.
    fun select(kind: FocusKind, seconds: Int): FocusTimerState =
        if (run != Run.IDLE) this else copy(kind = kind, plannedSeconds = seconds)

    fun start(at: Instant): FocusTimerState =
        if (run != Run.IDLE) this else copy(run = Run.RUNNING, startedAt = at, accumulated = 0.0)

    fun pause(at: Instant): FocusTimerState =
        if (run != Run.RUNNING) this else copy(accumulated = elapsed(at), startedAt = null, run = Run.PAUSED)

    fun resume(at: Instant): FocusTimerState =
        if (run != Run.PAUSED) this else copy(startedAt = at, run = Run.RUNNING)

    fun finish(at: Instant): FocusTimerState =
        copy(accumulated = elapsed(at), startedAt = null, run = Run.FINISHED)

    // Lectura

    /// Segons transcorreguts, mai negatius.
    fun elapsed(now: Instant): Double {
        val running = startedAt?.let { max(0.0, seconds(it, now)) } ?: 0.0
        return accumulated + running
    }

    fun remaining(now: Instant): Double = max(0.0, plannedSeconds - elapsed(now))

    fun progress(now: Instant): Double {
        if (plannedSeconds <= 0) return 1.0
        return min(1.0, max(0.0, elapsed(now) / plannedSeconds))
    }

    fun isFinished(now: Instant): Boolean = remaining(now) <= 0

    /// Moment en què el compte enrere arribaria a zero si continués sense pausa. Alimenta el
    /// cronòmetre de l'avís fix i l'alarma de final: `null` quan no hi ha cap tram en marxa.
    val endDate: Instant?
        get() = startedAt?.plusNanos(((plannedSeconds - accumulated) * 1_000_000_000).toLong())

    /// Minuts sencers arrodonits amunt, per a TalkBack: mai 0 min mentre queda temps real.
    fun remainingMinutesText(now: Instant): Int = ceil(remaining(now) / 60).toInt()

    private fun seconds(from: Instant, to: Instant): Double = Duration.between(from, to).toNanos() / 1e9
}
