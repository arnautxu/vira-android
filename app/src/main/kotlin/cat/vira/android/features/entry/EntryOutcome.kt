package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
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
    /// Desa el permís i envia el correu a la persona adulta (`guardian-consent`).
    private val requestGuardian: suspend (String) -> ViraGuardianRequest,
) {
    enum class Plan {
        /// No hi ha tram: s'ha de demanar abans de res.
        NEEDS_AGE,
        /// 16 o més: es concedeix sense preguntar res (`not_required` al servidor).
        GRANT,
        /// 12–15: cal el correu d'una persona adulta.
        ASK_GUARDIAN,
    }

    /// Escriu el que toqui i, **només si ha anat bé**, avança: el 16+ crida `onWritten`, i el 12–15
    /// crida `onAsked` amb on ha quedat el correu (la fulla ho ensenya abans d'avançar). Torna
    /// l'error, si n'hi ha hagut, perquè la vista el pugui ensenyar.
    suspend fun submit(
        ageRange: String,
        guardianEmail: String,
        onAsked: (ViraGuardianRequest) -> Unit = {},
        onWritten: () -> Unit,
    ): ViraBackendError? {
        try {
            when (plan(ageRange)) {
                Plan.NEEDS_AGE -> return null
                Plan.GRANT -> {
                    declare(ViraConsentStatus.GRANTED, null)
                    onWritten()
                }
                Plan.ASK_GUARDIAN -> onAsked(sendToGuardian(guardianEmail))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: ViraBackendException) {
            return error.error
        } catch (_: Exception) {
            return ViraBackendError.UNAVAILABLE
        }
        return null
    }

    /// Si la funció del correu no hi és o falla, el permís es desa pel camí d'abans
    /// (`declare_guardian_consent`) i el resultat diu que el correu no ha sortit. Un correu equivocat,
    /// el del mateix compte o sense xarxa no tenen camí alternatiu: es diuen.
    private suspend fun sendToGuardian(email: String): ViraGuardianRequest = try {
        requestGuardian(email)
    } catch (error: ViraBackendException) {
        if (error.error != ViraBackendError.UNAVAILABLE && error.error != ViraBackendError.REJECTED) throw error
        declare(ViraConsentStatus.PENDING, email)
        ViraGuardianRequest.unsent(email)
    }

    companion object {
        fun plan(ageRange: String): Plan {
            val range = ViraAgeRange.from(ageRange) ?: return Plan.NEEDS_AGE
            return if (range == ViraAgeRange.EARLY) Plan.ASK_GUARDIAN else Plan.GRANT
        }

        /// El rebut entra a la sessió abans que la pista canviï (`ViraAuthGate` ensenya el pas mentre
        /// n'hi ha), també pel camí de recanvi: si no, l'app sortiria un moment per sota del pas.
        fun live(services: EntryServices) = EntryConsentWriter(
            declare = { status, email ->
                val receipt = services.guardianReceipt.value
                if (status == ViraConsentStatus.PENDING && email != null) {
                    services.guardianReceipt.value = ViraGuardianRequest.unsent(email)
                }
                try {
                    services.declareConsent(status, email)
                } catch (error: Throwable) {
                    services.guardianReceipt.value = receipt
                    throw error
                }
            },
            requestGuardian = { services.requestGuardianConsent(it, showingReceipt = true) },
        )
    }
}

/// Què ha fet qui acaba d'entrar, sense el detall (`EntryFirstActionKind` de l'iPhone). Es desa a
/// `settings.entryFirstAction` perquè el tomb, que surt quan el flux ja no hi és, sàpiga de què parlar.
enum class EntryFirstActionKind(val raw: String) {
    TASK("task"), CHECK_IN("checkIn"), FOCUS("focus");

    companion object {
        fun of(outcome: EntryFirstActionOutcome): EntryFirstActionKind = when (outcome) {
            is EntryFirstActionOutcome.Task -> TASK
            is EntryFirstActionOutcome.CheckIn -> CHECK_IN
            is EntryFirstActionOutcome.Focus -> FOCUS
        }

        fun from(raw: String): EntryFirstActionKind? = entries.firstOrNull { it.raw == raw }
    }
}
