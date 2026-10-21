package cat.vira.android.core.backend

import java.io.IOException

/// Tot el que pot anar malament parlant amb el servidor, en el vocabulari de la interfície
/// (`ViraBackendError` de l'iPhone). Cap pantalla veu un 400: veu un d'aquests, i el text és
/// `messageKey` del catàleg (`backend.error.*`).
enum class ViraBackendError(val messageKey: String) {
    NOT_CONFIGURED("backend.error.notConfigured"),
    OFFLINE("backend.error.offline"),
    INVALID_CREDENTIALS("backend.error.credentials"),
    EMAIL_ALREADY_REGISTERED("backend.error.emailTaken"),
    WEAK_PASSWORD("backend.error.weakPassword"),
    INVALID_EMAIL("backend.error.invalidEmail"),
    EMAIL_NOT_CONFIRMED("backend.error.emailNotConfirmed"),
    TOO_MANY_ATTEMPTS("backend.error.tooManyAttempts"),
    SESSION_EXPIRED("backend.error.sessionExpired"),
    NOT_ALLOWED("backend.error.notAllowed"),
    CANCELLED("backend.error.cancelled"),
    REJECTED("backend.error.rejected"),
    /// El correu del tutor és el mateix del compte (`guardian-consent`, 2026-10-20).
    GUARDIAN_IS_YOU("backend.error.guardianIsYou"),
    UNAVAILABLE("backend.error.unavailable");

    val isRetryable: Boolean get() = this == OFFLINE || this == UNAVAILABLE || this == TOO_MANY_ATTEMPTS

    companion object {

        /// GoTrue. Un 400 aquí vol dir «la contrasenya no encaixa».
        fun fromAuth(status: Int, code: String?, message: String?): ViraBackendError {
            val haystack = "${code.orEmpty()} ${message.orEmpty()}".lowercase()
            return when {
                "already registered" in haystack || "user_already_exists" in haystack ||
                    "email_exists" in haystack -> EMAIL_ALREADY_REGISTERED
                "weak_password" in haystack || "password should be" in haystack -> WEAK_PASSWORD
                "email_address_invalid" in haystack || "validation_failed" in haystack -> INVALID_EMAIL
                "email_not_confirmed" in haystack || "not confirmed" in haystack -> EMAIL_NOT_CONFIRMED
                // L'ordre importa: un refresc rebutjat també diu `invalid_grant`, i dir-li
                // «credencials incorrectes» faria pensar que ha escrit malament una contrasenya
                // que ni tan sols li han demanat.
                "refresh" in haystack -> SESSION_EXPIRED
                "invalid_grant" in haystack || "invalid login" in haystack -> INVALID_CREDENTIALS
                else -> when (status) {
                    400, 401 -> INVALID_CREDENTIALS
                    403 -> NOT_ALLOWED
                    422 -> INVALID_EMAIL
                    429 -> TOO_MANY_ATTEMPTS
                    else -> UNAVAILABLE
                }
            }
        }

        /// PostgREST. Un 400 aquí vol dir «la fila no encaixa amb l'esquema», i **no** es reintenta:
        /// esperar no arregla una columna que no hi és (Arnau, 2026-08-13).
        fun fromDatabase(status: Int, code: String?): ViraBackendError = when {
            code == "PGRST301" || code == "PGRST302" -> SESSION_EXPIRED
            status == 401 -> SESSION_EXPIRED
            status == 403 -> NOT_ALLOWED
            status == 429 -> TOO_MANY_ATTEMPTS
            // Temps esgotat: no és la fila, és el camí. Es torna a provar.
            status == 408 -> UNAVAILABLE
            status in 400..499 -> REJECTED
            else -> UNAVAILABLE
        }

        /// Sense xarxa, temps esgotat, servidor inabastable: per a qui ho llegeix, tot és «no hi ha
        /// connexió», i es reintenta sol a la propera passada.
        @Suppress("UNUSED_PARAMETER")
        fun fromNetwork(error: IOException): ViraBackendError = OFFLINE
    }
}

/// Excepció que porta un `ViraBackendError`. És el que llancen el client HTTP, l'auth i la
/// sincronització; la interfície només en mira `error`.
class ViraBackendException(val error: ViraBackendError, cause: Throwable? = null) :
    Exception(error.name, cause)
