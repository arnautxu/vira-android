package cat.vira.android.features.progress.logic

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.Streak
import java.time.Instant

/// Mecànica de la ratxa amb dies guardats, sense base de dades (`ViraStreak.swift`).
///
/// Cada regla de `VIRI_AND_GAMIFICATION.md` §2.3.1 hi és per evitar un dany concret:
/// 1. El dia compta quan hi ha **les tres coses**: un hàbit, un registre emocional i una tasca
///    (`DayRequirement`; qui ho comprova és `ViraProgress`).
/// 2. **2 dies guardats gratuïts al mes**, que es gasten **sols** en perdre un dia. Se'n poden
///    comprar més (200 ViriCoins).
/// 3. **Si no n'hi ha prou, la ratxa es reinicia sense drama.** El rècord no es toca mai.
/// 4. **Qualsevol acció descongela Viri** a l'instant, encara que el dia no arribi a comptar.
///
/// Cap funció d'aquí resta res ni registra cap «fracàs». Les funcions són pures: prenen una
/// ratxa i en tornen una de nova, i qui les crida la desa.
object StreakRules {

    /// Fites que Viri pot comentar en un avís. Són les mateixes que desbloquegen carta.
    val milestoneDays = listOf(7, 30, 100)

    /// Les tres accions que ha de tenir el dia. La interfície diu **el que queda**, mai el que
    /// «no s'ha fet» (`CONTENT_STRATEGY.md`).
    enum class DayRequirement { HABIT, EMOTION, TASK }

    /// Què ha passat en posar la ratxa al dia.
    sealed interface ReconcileOutcome {
        /// Res a dir: o no hi havia dies perduts, o no hi havia ratxa.
        data object Unchanged : ReconcileOutcome
        /// S'han gastat dies guardats i la ratxa segueix sencera.
        data class Protected(val days: Int) : ReconcileOutcome
        /// La ratxa ha tornat a zero. `best` és el rècord, que no es toca mai.
        data class Reset(val best: Int) : ReconcileOutcome
    }

    /// Resultat de comptar un dia.
    data class Counted(
        val streak: Streak,
        /// Si el dia era nou (puja `ProgressCounter.daysActiveCount`).
        val isNewDay: Boolean,
        /// Fita de `milestoneDays` a què s'acaba d'arribar, si la ratxa és visible.
        val milestone: Int? = null,
    )

    /// Reposa els dies guardats gratuïts en canviar de mes, amb sostre (`Streak.MAX_FREEZES`):
    /// vint dies guardats farien la ratxa impossible de perdre i deixaria de voler dir res.
    fun refreshMonthlyGrant(streak: Streak, now: Instant, days: ViraDays): Streak {
        val key = ViraStore.monthKey(now, days.zone)
        // Només en **avançar** de mes: un rellotge endarrerit no pot regalar dies guardats.
        if (key <= streak.lastGrantMonth) return streak
        return streak.copy(
            lastGrantMonth = key,
            freezesUsedThisMonth = 0,
            freezesAvailable = minOf(Streak.MAX_FREEZES, streak.freezesAvailable + Streak.MONTHLY_FREE_GRANT),
        )
    }

    /// Descongela Viri i posa els dies guardats al dia, **sense** tocar el comptador. És el que fa
    /// una sessió de concentració, i qualsevol acció quan al dia encara li falta alguna cosa.
    fun touch(streak: Streak, now: Instant, days: ViraDays): Streak =
        refreshMonthlyGrant(streak, now, days).copy(isFrozen = false)

    /// Compta el dia. **No** comprova les tres condicions: és la mecànica de la cadena.
    /// Idempotent: deu vegades el mateix dia compten un dia.
    fun countDay(streak: Streak, now: Instant, days: ViraDays): Counted {
        var s = refreshMonthlyGrant(streak, now, days)
        val today = days.startOfDay(now)

        val lastActive = s.lastActiveDay
        if (lastActive == null) {
            // Primer dia de tots.
            return Counted(
                s.copy(
                    currentDays = 1,
                    bestDays = maxOf(s.bestDays, 1),
                    lastActiveDay = today,
                    isFrozen = false,
                    activeDays = s.activeDays.withDay(today),
                ),
                isNewDay = true,
            )
        }
        val last = days.startOfDay(lastActive)
        // Ja comptat avui. Tocar l'app descongela Viri: recuperació d'un sol toc. Un dia
        // **anterior** a l'últim (rellotge endarrerit, canvi de fus) també compta com a ja comptat:
        // si no, cada canvi de data inflaria la ratxa.
        if (today <= last) return Counted(s.copy(isFrozen = false), isNewDay = false)

        val missed = maxOf(0L, days.daysBetween(last, today) - 1).toInt()
        // Una ratxa ja reiniciada (0) no té res a protegir: no es gasta cap dia guardat.
        if (missed > 0 && s.currentDays > 0) s = spend(s, missed, today, days).first

        val current = if (s.currentDays == 0) 1 else s.currentDays + 1
        val milestone = current.takeIf { s.isVisible && it in milestoneDays }
        return Counted(
            s.copy(
                currentDays = current,
                bestDays = maxOf(s.bestDays, current),
                lastActiveDay = today,
                isFrozen = false,
                activeDays = s.activeDays.withDay(today),
            ),
            isNewDay = true,
            milestone = milestone,
        )
    }

    /// Posa la ratxa al dia en obrir l'app, **sense registrar res**: obrir-la i llegir «t'he
    /// guardat la ratxa» és exactament el moment en què el dia guardat ha de servir.
    fun reconcile(streak: Streak, now: Instant, days: ViraDays): Pair<Streak, ReconcileOutcome> {
        val s = refreshMonthlyGrant(streak, now, days)
        val today = days.startOfDay(now)
        val lastActive = s.lastActiveDay
        if (lastActive == null || s.currentDays <= 0) return s to ReconcileOutcome.Unchanged
        val last = days.startOfDay(lastActive)

        val missed = maxOf(0L, days.daysBetween(last, today) - 1).toInt()
        if (missed <= 0) return s to ReconcileOutcome.Unchanged

        val (spent, outcome) = spend(s, missed, today, days)
        // Els dies ja coberts no es tornen a cobrar la propera vegada.
        return spent.copy(lastActiveDay = days.addDays(today, -1)) to outcome
    }

    /// Gasta dies guardats pels dies perduts. **Tot o res**: si no n'hi ha prou per cobrir-los
    /// tots, no se'n gasta cap, perquè gastar-ne sense evitar el reinici li costaria dies (i
    /// monedes, si eren comprats) sense cap benefici.
    private fun spend(streak: Streak, missedDays: Int, today: Instant, days: ViraDays): Pair<Streak, ReconcileOutcome> {
        if (streak.freezesAvailable < missedDays) {
            // Reinici. `bestDays` **no es toca mai**: és el que es mostra quan això passa.
            return streak.copy(currentDays = 0, isFrozen = false) to ReconcileOutcome.Reset(streak.bestDays)
        }
        // Es cobreixen els dies **més propers a avui**: són els que mantenen la cadena.
        val covered = (1..missedDays).map { days.addDays(today, -it.toLong()) }
        val protected = streak.protectedDays + covered.filter { it !in streak.protectedDays }
        return streak.copy(
            freezesAvailable = streak.freezesAvailable - missedDays,
            freezesUsedThisMonth = streak.freezesUsedThisMonth + missedDays,
            isFrozen = true,
            protectedDays = protected,
        ) to ReconcileOutcome.Protected(missedDays)
    }

    /// Si es pot comprar un dia guardat més (sense comptar les monedes): no al sostre.
    fun canBuyFreeze(streak: Streak): Boolean = streak.freezesAvailable < Streak.MAX_FREEZES

    private fun List<Instant>.withDay(day: Instant): List<Instant> = if (day in this) this else this + day
}
