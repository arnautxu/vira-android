package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import kotlin.coroutines.cancellation.CancellationException

/// Què ha deixat la primera acció. És **el que ha passat de debò**, i és l'únic que la celebració
/// pot ensenyar (`EntryDoneStep`): cap recompensa inventada, cap "dia 1".
sealed interface EntryFirstActionOutcome {
    /// Una tasca afegida a Avui. Afegir no dona res: les monedes van en completar-la.
    data class Task(val title: String) : EntryFirstActionOutcome
    /// Un registre emocional confirmat, amb el que hagi donat el camí de sempre.
    data class CheckIn(val earned: EntryReward) : EntryFirstActionOutcome
    /// Una sessió de focus. Cancel·lada també compta com a començada, sense recompensa.
    data class Focus(val completed: Boolean, val earned: EntryReward) : EntryFirstActionOutcome

    val reward: EntryReward
        get() = when (this) {
            is Task -> EntryReward.NONE
            is CheckIn -> earned
            is Focus -> earned
        }

    val taskTitle: String? get() = (this as? Task)?.title
}

/// Monedes i XP guanyades entre dos moments, llegides del comptador real.
///
/// **No es calcula res aquí.** Les dues accions que paguen (check-in i focus) ja paguen pel seu
/// camí de sempre; el flux només mira què ha canviat al comptador. Així la celebració ensenya
/// exactament el que l'acció hauria donat fora del flux, també quan no dona res.
data class EntryReward(val coins: Int, val xp: Int) {
    val isEmpty: Boolean get() = coins <= 0 && xp <= 0

    fun earned(since: EntryReward) = EntryReward(maxOf(0, coins - since.coins), maxOf(0, xp - since.xp))

    companion object {
        val NONE = EntryReward(0, 0)
    }
}

/// Declara el permís del tutor al servidor. Separat de la vista perquè la regla que protegeix
/// —**el pas no avança si no ha escrit**— s'ha de poder provar sense xarxa ni pantalla.
class EntryConsentWriter(
    /// Estat demanat i, per a `PENDING`, el correu del tutor.
    private val declare: suspend (ViraConsentStatus, String?) -> Unit,
) {
    enum class Plan {
        /// No hi ha tram: s'ha de demanar abans de res.
        NEEDS_AGE,
        /// 16 o més: es concedeix sense preguntar res (`not_required` al servidor).
        GRANT,
        /// 12–15: cal el correu d'una persona adulta.
        ASK_GUARDIAN,
    }

    /// Escriu el que toqui i, **només si ha anat bé**, crida `onWritten`. Torna l'error, si n'hi
    /// ha hagut, perquè la vista el pugui ensenyar.
    suspend fun submit(ageRange: String, guardianEmail: String, onWritten: () -> Unit): ViraBackendError? {
        try {
            when (plan(ageRange)) {
                Plan.NEEDS_AGE -> return null
                Plan.GRANT -> declare(ViraConsentStatus.GRANTED, null)
                Plan.ASK_GUARDIAN -> declare(ViraConsentStatus.PENDING, guardianEmail)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: ViraBackendException) {
            return error.error
        } catch (_: Exception) {
            return ViraBackendError.UNAVAILABLE
        }
        onWritten()
        return null
    }

    companion object {
        fun plan(ageRange: String): Plan {
            val range = ViraAgeRange.from(ageRange) ?: return Plan.NEEDS_AGE
            return if (range == ViraAgeRange.EARLY) Plan.ASK_GUARDIAN else Plan.GRANT
        }
    }
}
