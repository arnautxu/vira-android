package cat.vira.android.core.backend

import android.net.Uri

/// Sessió viva: tokens i qui és. `expiresAtEpochSeconds` es calcula en rebre-la.
data class ViraStoredSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val user: ViraUser,
)

/// Les operacions d'auth que fa servir Vira, en el seu vocabulari (`ViraAuthClient` de l'iPhone).
///
/// Té dues implementacions: la de debò, sobre l'SDK de Supabase (`ViraSupabaseAuth`), i la de les
/// proves, que no toca la xarxa. Tot el que en surt parla en `ViraBackendException`, mai en els
/// tipus de l'SDK: canviar de proveïdor demà tornaria a ser un sol fitxer.
interface ViraAuthBackend {

    /// Alta amb correu. `null` quan el projecte exigeix confirmar el correu abans d'obrir sessió,
    /// que és el cas de Vira.
    suspend fun signUp(email: String, password: String, metadata: ViraUserMetadata): ViraStoredSession?

    suspend fun signIn(email: String, password: String): ViraStoredSession

    suspend fun sendPasswordReset(email: String)

    /// Només amb la sessió que ha obert l'enllaç del correu: qui la canvia ha de tenir el correu.
    suspend fun updatePassword(password: String)

    /// Obre el navegador amb Google o Apple. La sessió arriba després, per `session(fromCallback:)`.
    suspend fun startOAuth(provider: ViraOAuthProvider)

    /// Tornada de `vira://auth-callback?code=…` (OAuth, confirmació de correu o recuperació).
    suspend fun session(fromCallback: Uri): ViraStoredSession

    /// Sessió vàlida, renovant-la si cal.
    suspend fun session(): ViraStoredSession

    /// Sessió tal com està desada, sense tocar la xarxa. Pot estar caducada.
    suspend fun storedSession(): ViraStoredSession?

    suspend fun signOut()

    suspend fun updateMetadata(metadata: ViraUserMetadata): ViraUser
}

/// Google i Apple van tots dos pel navegador amb PKCE i tornen a `vira://auth-callback`.
enum class ViraOAuthProvider { GOOGLE, APPLE }
