package cat.vira.android.core.backend

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/// El selector de comptes de Google del telèfon (Credential Manager), en lloc del navegador.
///
/// El `nonce` va en dues formes: a Google, resumit (SHA-256); a Supabase, en cru. Supabase en
/// torna a fer el resum i el compara amb el que porta el testimoni: un testimoni robat no serveix.
class ViraGoogleSignIn(private val serverClientId: String) {

    sealed interface Result {
        data class Token(val idToken: String, val rawNonce: String) : Result
        /// L'usuari ha tancat el selector: no es fa res més.
        data object Cancelled : Result
        /// El telèfon no el pot fer servir (sense Google Play, sense comptes, mal configurat): el
        /// navegador encara funciona.
        data object Unavailable : Result
    }

    suspend fun request(activity: Activity): Result {
        val rawNonce = ByteArray(32).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        val option = GetSignInWithGoogleOption.Builder(serverClientId).setNonce(sha256(rawNonce)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
            } else {
                Result.Unavailable
            }
        } catch (_: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (_: GetCredentialException) {
            Result.Unavailable
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
