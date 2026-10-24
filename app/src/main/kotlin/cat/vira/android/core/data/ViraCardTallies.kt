package cat.vira.android.core.data

import android.content.SharedPreferences
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.kind
import cat.vira.android.core.data.model.status
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/// Els recomptes de les cartes que no viuen a `ProgressCounter` (`ViraCardTallies` de l'iPhone,
/// Arnau, 2026-10-22).
///
/// Les sis escales noves (tasques, minuts de concentració, reptes, premis propis, skins i tornar)
/// volien un recompte que no baixi mai. Una columna nova al comptador demanava una migració de
/// Supabase, i no cal: tot el que compten **es pot refer** amb files que ja se sincronitzen. Per
/// això viuen a les preferències, amb dues regles:
/// - **Només pugen.** L'acció hi suma en el moment ([add]) i cada obertura els puja fins al que
///   diuen les dades ([repair]), mai al revés: esborrar una tasca feta no et treu la carta.
/// - **Prefix `cards.`**: canviar de compte els esborra amb la resta (`ViraDataEraser`), i un
///   telèfon nou els refà de les dades del compte.
///
/// Els reptes d'emocions **no compten**: les seves files passen per la porta del permís del tutor.
class ViraCardTallies(private val preferences: SharedPreferences) {

    enum class Kind(val raw: String) {
        TASKS_DONE("tasksDone"), FOCUS_SECONDS("focusSeconds"), CHALLENGES_DONE("challengesDone"),
        REWARDS_REDEEMED("rewardsRedeemed"), SKINS_OWNED("skinsOwned"), COMEBACKS("comebacks");

        /// Mateix nom que l'iPhone.
        val key: String get() = "cards.tally.$raw"
    }

    /// Una foto dels sis recomptes, per avaluar les cartes sense tornar a llegir les preferències.
    data class Snapshot(val values: Map<Kind, Int> = emptyMap()) {
        operator fun get(kind: Kind): Int = values[kind] ?: 0

        companion object {
            val EMPTY = Snapshot()
        }
    }

    fun value(kind: Kind): Int = preferences.getInt(kind.key, 0)

    fun snapshot(): Snapshot = Snapshot(Kind.entries.associateWith(::value))

    fun add(amount: Int, kind: Kind) {
        if (amount <= 0) return
        preferences.edit().putInt(kind.key, value(kind) + amount).apply()
    }

    /// Puja el recompte fins a `floor` si encara no hi arriba. Retorna si ha pujat.
    fun raise(kind: Kind, floor: Int): Boolean {
        if (floor <= value(kind)) return false
        preferences.edit().putInt(kind.key, floor).apply()
        return true
    }

    fun clear() {
        preferences.edit().apply { Kind.entries.forEach { remove(it.key) } }.apply()
    }

    /// Puja cada recompte fins al que diuen les dades del telèfon. Retorna si n'ha pujat algun: vol
    /// dir que hi havia dades d'abans (l'actualització que porta aquestes cartes, o un compte que
    /// acaba de baixar) i les cartes que en surtin no s'han de celebrar com si fossin d'ara.
    ///
    /// Les tasques fetes fa menys de [SETTLE] no hi compten: poden ser dins del «Desfer», amb la
    /// recompensa —i el seu [add]— encara per arribar. Comptades aquí i sumades després, dues tasques
    /// marcades dins de la mateixa finestra en feien tres. La passada següent ja les compta.
    suspend fun repair(db: ViraDatabase, zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): Boolean {
        val sessions = db.focusSessions().all().filter { it.completed && it.kind == FocusKind.FOCUS }
        val settled = now.minus(SETTLE)
        val floors = mapOf(
            Kind.TASKS_DONE to db.planItems().all().count {
                it.status == PlanItemStatus.DONE && (it.completedAt == null || !it.completedAt.isAfter(settled))
            },
            Kind.FOCUS_SECONDS to sessions.sumOf { maxOf(0, it.actualSeconds) },
            Kind.CHALLENGES_DONE to db.challengeClaims().all().count { countsChallenge(it.key) },
            Kind.REWARDS_REDEEMED to db.personalRewards().all().sumOf { maxOf(0, it.redeemedCount) },
            Kind.SKINS_OWNED to db.skinOwnerships().all().size,
            Kind.COMEBACKS to comebacks(db.streaks().all().firstOrNull()?.activeDays.orEmpty(), zone),
        )
        var raised = false
        for ((kind, floor) in floors) if (raise(kind, floor)) raised = true
        return raised
    }

    companion object {
        /// Dies seguits sense que el dia compti a partir dels quals tornar és «Tornem-hi».
        const val COMEBACK_GAP_DAYS = 7

        /// Més que el «Desfer» més llarg: 5 s a l'iPhone, fins a 2 min a Android amb «Temps per
        /// actuar» (`ViraCompletionQueue`).
        val SETTLE: Duration = Duration.ofMinutes(3)

        /// El de l'app, que hi posa `ViraApp` en arrencar (l'`UserDefaults.standard` de l'iPhone).
        /// Sense ell (les proves), [ViraRewards] no suma res en el moment i només compta [repair].
        @Volatile var shared: ViraCardTallies? = null

        /// Quants cops el dia ha tornat a comptar després de [COMEBACK_GAP_DAYS] o més dies sense.
        /// Funció pura sobre `Streak.activeDays`, que es guarda sencer i se sincronitza.
        fun comebacks(activeDays: List<Instant>, zone: ZoneId = ZoneId.systemDefault()): Int {
            val days = activeDays.map { it.atZone(zone).toLocalDate() }.toSortedSet().toList()
            return days.zipWithNext().count { (previous, next) ->
                ChronoUnit.DAYS.between(previous, next) - 1 >= COMEBACK_GAP_DAYS
            }
        }

        /// Un repte compta si no és d'emocions (vegeu la capçalera).
        fun countsChallenge(key: String): Boolean = !key.contains("emotion")
    }
}
