package cat.vira.android.features.entry

import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraUser

/// Els passos del flux d'entrada, en ordre (`EntryStep` de l'iPhone).
///
/// ## Primer el valor, després el compte (Arnau, 2026-10-09)
/// Tres preguntes curtes, la privacitat, **una acció real** (una tasca, un check-in o dos minuts de
/// concentració) i, només llavors, el compte, que es presenta com "guardar el que acabes de fer".
///
/// El que no s'ha mogut ni es pot moure:
/// - **La privacitat va abans del correu.** També quan es prem "Saltar": el salt s'atura a la
///   privacitat si encara no s'ha vist (`skipTarget`).
/// - **El compte és obligatori amb backend**, i no té "Més tard".
/// - **El permís del tutor** (`CONSENT`) és la condició de `ViraSyncScope`: cap dada sensible
///   surt del telèfon abans, i no es pot saltar.
///
/// ## El rol ja no és un pas
/// El pare o la mare el contesta amb l'enllaç discret de la benvinguda, i aquell rol triat **abans**
/// de tenir compte és el `pendingRole` que filtra els passos del jove.
///
/// ## Els dos passos de la contrasenya no són del camí
/// `PASSWORD_RESET` i `NEW_PASSWORD` no surten a `full` ni a la barra: són una branca amb sortida.
///
/// ## Fora del camí curt
/// L'iPhone conserva mòduls, motivació i beta com a passos que ningú obre (tenen lloc a Ajustos, i
/// el feedback de testers està retirat des del 2026-09-17). Aquí només hi ha **connexions**,
/// perquè el tutorial en torna a ensenyar els interruptors (`EntryConnectionToggles`).
enum class EntryStep {
    WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, ACCOUNT, PASSWORD_RESET, NEW_PASSWORD,
    CONSENT, NICKNAME, NOTIFICATIONS,
    // Fora del camí curt:
    CONNECTIONS;

    /// Títol del pas. La primera acció en té un per variant i la celebració no en porta.
    @get:StringRes
    val title: Int
        get() = when (this) {
            WELCOME -> R.string.onboarding_welcome_title
            START -> R.string.onboarding_start_title
            AGE -> R.string.onboarding_age_title
            PRIVACY -> R.string.onboarding_privacy_title
            FIRST_ACTION -> R.string.onboarding_task_title
            DONE -> R.string.onboarding_viri_done
            ACCOUNT -> R.string.account_landing_title
            PASSWORD_RESET -> R.string.account_password_reset_title
            NEW_PASSWORD -> R.string.account_password_new_title
            CONSENT -> R.string.account_consent_title
            NICKNAME -> R.string.onboarding_personalization_title
            NOTIFICATIONS -> R.string.onboarding_reminders_title
            CONNECTIONS -> R.string.onboarding_connections_title
        }

    /// Missatge sota el títol. `null` quan el pas ja s'explica sol amb la frase de Viri.
    @get:StringRes
    val message: Int?
        get() = when (this) {
            ACCOUNT -> R.string.account_landing_subtitle
            PASSWORD_RESET -> R.string.account_password_reset_message
            NEW_PASSWORD -> R.string.account_password_new_message
            CONSENT -> R.string.account_consent_message
            CONNECTIONS -> R.string.onboarding_connections_message
            else -> null
        }

    /// Si aquest pas s'ha de veure, donat qui hi ha connectat. És **la regla que decideix què es
    /// torna a preguntar a algú que ja ha contestat**, i per això viu aquí i es prova sola.
    ///
    /// - `requiresAccount`: hi ha backend configurat. Sense ell no hi ha ni compte, ni rol, ni permís.
    /// - `pendingRole`: el rol triat a la benvinguda abans que hi hagi compte.
    fun applies(requiresAccount: Boolean, user: ViraUser?, pendingRole: ViraAccountRole? = null): Boolean {
        val isParent = user?.role == ViraAccountRole.PARENT || pendingRole == ViraAccountRole.PARENT
        return when (this) {
            WELCOME, PRIVACY -> true
            ACCOUNT -> requiresAccount && user == null
            // Només hi ha permís a demanar si hi ha compte obert d'un jove i encara no s'ha resolt.
            CONSENT -> needsConsent(user)
            // Un compte de pare o mare no té pla, ni emocions, ni ratxa.
            START, AGE, FIRST_ACTION, DONE, NICKNAME, NOTIFICATIONS, CONNECTIONS -> !isParent
            // Branques, no etapes.
            PASSWORD_RESET, NEW_PASSWORD -> false
        }
    }

    /// Els passos que no tenen "Saltar" i que el salt no pot passar per alt.
    fun isMandatory(requiresAccount: Boolean, user: ViraUser?, pendingRole: ViraAccountRole? = null): Boolean =
        when (this) {
            ACCOUNT -> ACCOUNT.applies(requiresAccount, user)
            CONSENT -> needsConsent(user)
            else -> false
        }

    /// Si la capçalera ofereix "Saltar". L'edat, amb backend, no es pot saltar **des d'ella
    /// mateixa** —el tram decideix el permís—; la primera acció té el seu "Ara no", i la
    /// celebració no se salta.
    fun offersSkip(requiresAccount: Boolean, user: ViraUser?, pendingRole: ViraAccountRole? = null): Boolean =
        when (this) {
            AGE -> !requiresAccount
            FIRST_ACTION, DONE, ACCOUNT, PASSWORD_RESET, NEW_PASSWORD -> false
            else -> !isMandatory(requiresAccount, user, pendingRole)
        }

    companion object {
        /// El camí de la primera obertura.
        ///
        /// **Sense el nom** (2026-10-22): eren deu passos abans de veure l'app, i el nom es tria
        /// després, a Viri. **Els avisos, just després de «Fet»** (Arnau, 2026-10-23): el moment amb
        /// més ganes de rebre un avís és quan acabes d'apuntar una cosa.
        val full: List<EntryStep> = listOf(
            WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, NOTIFICATIONS, ACCOUNT, CONSENT,
        )

        /// El pas que ve després de `current` dins de `steps`, o `null` si el flux s'acaba.
        fun next(
            after: EntryStep,
            steps: List<EntryStep>,
            requiresAccount: Boolean,
            user: ViraUser?,
            pendingRole: ViraAccountRole? = null,
        ): EntryStep? = following(after, steps).firstOrNull { it.applies(requiresAccount, user, pendingRole) }

        /// On porta "Saltar": al primer pas obligatori que quedi, o `null` si el flux es tanca.
        /// **Amb backend, el salt s'atura a la privacitat si encara no s'ha vist.**
        fun skipTarget(
            from: EntryStep,
            steps: List<EntryStep>,
            requiresAccount: Boolean,
            user: ViraUser?,
            pendingRole: ViraAccountRole? = null,
        ): EntryStep? = following(from, steps).firstOrNull { candidate ->
            (requiresAccount && candidate == PRIVACY) || candidate.isMandatory(requiresAccount, user, pendingRole)
        }

        /// On porta l'"Ara no" de la primera acció: **passa per sobre de la celebració**.
        fun afterSkippedAction(
            steps: List<EntryStep>,
            requiresAccount: Boolean,
            user: ViraUser?,
            pendingRole: ViraAccountRole? = null,
        ): EntryStep? = next(DONE, steps, requiresAccount, user, pendingRole)

        private fun following(current: EntryStep, steps: List<EntryStep>): List<EntryStep> =
            steps.dropWhile { it != current }.drop(1)

        private fun needsConsent(user: ViraUser?): Boolean =
            user != null && user.role == ViraAccountRole.TEEN && user.consent == ViraConsentStatus.MISSING
    }
}
