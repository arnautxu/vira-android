package cat.vira.android.core.backend.sync

import android.content.SharedPreferences
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.data.ViraPreferences

/// Capa de sensibilitat d'una taula. Decideix si la fila pot sortir del telèfon.
enum class ViraSyncLayer {
    /// Pla, hàbits, focus, progrés, economia, perfil. Surt sempre que hi hagi sessió.
    CORE,
    /// Registres emocionals i notes.
    EMOTIONS,
    /// Medicació i preses.
    MEDICATION,
}

/// Què pot sortir del telèfon, i per què (`ViraSyncScope` de l'iPhone).
///
/// És l'únic lloc que ho decideix, i està separat del motor a propòsit: la regla que protegeix les
/// dades sensibles ha de ser petita, llegible i fàcil de provar.
///
/// Per a `EMOTIONS` i `MEDICATION` calen **tres** coses alhora (`PRIVACY_AND_SAFETY.md` §4.5b):
/// 1. sessió iniciada,
/// 2. l'interruptor d'Ajustos encès (de fàbrica ho està),
/// 3. el permís del tutor **concedit** (o no necessari). `PENDING` no compta.
///
/// Si en falta una, la capa no s'envia. No hi ha excepcions ni «només aquesta vegada».
data class ViraSyncScope(
    val isSignedIn: Boolean,
    val syncEmotions: Boolean,
    val syncMedication: Boolean,
    val consent: ViraConsentStatus,
) {
    fun allows(layer: ViraSyncLayer): Boolean {
        if (!isSignedIn) return false
        return when (layer) {
            ViraSyncLayer.CORE -> true
            ViraSyncLayer.EMOTIONS -> syncEmotions && hasConsent
            ViraSyncLayer.MEDICATION -> syncMedication && hasConsent
        }
    }

    private val hasConsent: Boolean get() = consent == ViraConsentStatus.GRANTED

    companion object {
        /// Estat d'ara mateix, segons la sessió i les preferències desades.
        fun current(session: ViraSession, preferences: SharedPreferences) = ViraSyncScope(
            isSignedIn = session.isSignedIn,
            syncEmotions = preferences.getBoolean(
                ViraPreferences.SYNC_EMOTIONS_ENABLED, ViraPreferences.SYNC_EMOTIONS_DEFAULT,
            ),
            syncMedication = preferences.getBoolean(
                ViraPreferences.SYNC_MEDICATION_ENABLED, ViraPreferences.SYNC_MEDICATION_DEFAULT,
            ),
            consent = session.user?.consent ?: ViraConsentStatus.MISSING,
        )
    }
}
