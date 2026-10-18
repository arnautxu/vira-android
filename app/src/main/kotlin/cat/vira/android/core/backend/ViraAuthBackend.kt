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

    /// Google amb el selector de comptes del telèfon: el testimoni el dona Android i Supabase el
    /// verifica amb el mateix `nonce` que s'ha enviat a Google.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): ViraStoredSession

    /// Tornada de `vira://auth-callback?code=…` (OAuth, confirmació de correu o recuperació).
    suspend fun session(fromCallback: Uri): ViraStoredSession

    /// Sessió vàlida, renovant-la si cal.
    suspend fun session(): ViraStoredSession

    /// Sessió renovada ara mateix, encara que no calgui: porta les metadades d'aquest moment.
    suspend fun refreshedSession(): ViraStoredSession

    /// Sessió tal com està desada, sense tocar la xarxa. Pot estar caducada.
    suspend fun storedSession(): ViraStoredSession?

    suspend fun signOut()

    /// Oblida la sessió desada al telèfon sense parlar amb el servidor (el compte ja no hi és).
    suspend fun forgetSession()

    suspend fun updateMetadata(metadata: ViraUserMetadata): ViraUser
}

/// Pel navegador amb PKCE, tornant a `vira://auth-callback`. Google només hi va quan el selector
/// de comptes no es pot fer servir (`ViraGoogleSignIn`).
enum class ViraOAuthProvider { GOOGLE, APPLE }
