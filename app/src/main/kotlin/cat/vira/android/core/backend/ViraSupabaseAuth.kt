@file:OptIn(kotlin.time.ExperimentalTime::class)

package cat.vira.android.core.backend

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.UUID

/// Auth de Supabase a través de l'SDK oficial (`supabase-kt`, només `Auth`), com l'iPhone amb
/// `supabase-swift`: el refresc, el PKCE i el mapa de codis de GoTrue els manté Supabase.
///
/// Google entra amb el selector de comptes del telèfon (`ViraGoogleSignIn`) i, si no es pot, pel
/// navegador. Apple sempre pel navegador. Tots dos tornen a `vira://auth-callback`, com a l'iPhone.
class ViraSupabaseAuth(
    context: Context,
    config: ViraBackendConfig.Values,
) : ViraAuthBackend {

    private val storage = ViraKeystoreSessionManager(ViraKeystore(context))

    val client: SupabaseClient = createSupabaseClient(config.baseUrl, config.anonKey) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = ViraBackendConfig.CALLBACK_SCHEME
            host = ViraBackendConfig.CALLBACK_HOST
            sessionManager = storage
            codeVerifierCache = storage
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
    }

    private val auth get() = client.auth

    override suspend fun signUp(email: String, password: String, metadata: ViraUserMetadata): ViraStoredSession? =
        mapping {
            auth.signUpWith(Email, redirectUrl = ViraBackendConfig.CALLBACK_URL) {
                this.email = email
                this.password = password
                this.data = metadata.json
            }
            auth.currentSessionOrNull()?.let { it.toVira(fallback = metadata) }
        }

    override suspend fun signIn(email: String, password: String): ViraStoredSession = mapping {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        current()
    }

    override suspend fun sendPasswordReset(email: String) = mapping {
        auth.resetPasswordForEmail(email, redirectUrl = ViraBackendConfig.CALLBACK_URL)
    }

    override suspend fun updatePassword(password: String) = mapping {
        auth.updateUser { this.password = password }
        Unit
    }

    override suspend fun startOAuth(provider: ViraOAuthProvider) = mapping {
        when (provider) {
            ViraOAuthProvider.GOOGLE -> auth.signInWith(Google, redirectUrl = ViraBackendConfig.CALLBACK_URL)
            ViraOAuthProvider.APPLE -> auth.signInWith(Apple, redirectUrl = ViraBackendConfig.CALLBACK_URL)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): ViraStoredSession = mapping {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
        current()
    }

    override suspend fun session(fromCallback: Uri): ViraStoredSession = mapping {
        // Un enllaç caducat o ja fet servir torna amb l'error a l'adreça, no amb un codi.
        val failure = fromCallback.getQueryParameter("error_code") ?: fromCallback.getQueryParameter("error")
        if (failure != null) {
            throw ViraBackendException(
                ViraBackendError.fromAuth(400, failure, fromCallback.getQueryParameter("error_description")),
            )
        }
        val code = fromCallback.getQueryParameter("code")
            ?: throw ViraBackendException(ViraBackendError.UNAVAILABLE)
        auth.exchangeCodeForSession(code)
        current()
    }

    override suspend fun session(): ViraStoredSession = mapping {
        auth.awaitInitialization()
        val session = auth.currentSessionOrNull()
            ?: throw ViraBackendException(ViraBackendError.SESSION_EXPIRED)
        if (session.expiresAt.epochSeconds - System.currentTimeMillis() / 1000 < REFRESH_MARGIN_SECONDS) {
            auth.refreshCurrentSession()
        }
        current()
    }

    override suspend fun storedSession(): ViraStoredSession? = try {
        auth.awaitInitialization()
        auth.currentSessionOrNull()?.toVira()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    override suspend fun signOut() = mapping { auth.signOut() }

    override suspend fun updateMetadata(metadata: ViraUserMetadata): ViraUser = mapping {
        auth.updateUser { data = metadata.json }.toVira()
    }

    private fun current(): ViraStoredSession =
        auth.currentSessionOrNull()?.toVira() ?: throw ViraBackendException(ViraBackendError.SESSION_EXPIRED)

    /// Tot el que surt d'aquí parla en `ViraBackendException`, mai en els tipus de l'SDK.
    private suspend fun <T> mapping(work: suspend () -> T): T = try {
        work()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ViraBackendException) {
        throw error
    } catch (error: Exception) {
        throw ViraBackendException(classify(error), error)
    }

    companion object {
        private const val REFRESH_MARGIN_SECONDS = 60

        fun classify(error: Throwable): ViraBackendError = when (error) {
            is AuthSessionMissingException -> ViraBackendError.SESSION_EXPIRED
            is AuthWeakPasswordException -> ViraBackendError.WEAK_PASSWORD
            is AuthRestException -> {
                val code = error.errorCode?.value ?: error.error
                if (code in sessionCodes) ViraBackendError.SESSION_EXPIRED
                else ViraBackendError.fromAuth(error.statusCode, code, error.errorDescription)
            }
            is RestException -> ViraBackendError.fromAuth(error.statusCode, error.error, error.description)
            is IOException -> ViraBackendError.OFFLINE
            else -> ViraBackendError.UNAVAILABLE
        }

        private val sessionCodes = setOf(
            "refresh_token_not_found", "refresh_token_already_used", "session_expired", "session_not_found",
        )
    }
}

private fun UserInfo.toVira(fallback: ViraUserMetadata = ViraUserMetadata()): ViraUser {
    val metadata = ViraUserMetadata.from(userMetadata)
    return ViraUser(
        id = UUID.fromString(id),
        email = email,
        role = metadata.role ?: fallback.role ?: ViraAccountRole.TEEN,
        consent = metadata.consent ?: fallback.consent ?: ViraConsentStatus.MISSING,
    )
}

/// `fallback`: un compte acabat de crear encara no té les metadades llegides de tornada, i val el que
/// s'acaba de demanar.
private fun UserSession.toVira(fallback: ViraUserMetadata = ViraUserMetadata()): ViraStoredSession {
    val info = user ?: throw ViraBackendException(ViraBackendError.UNAVAILABLE)
    return ViraStoredSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochSeconds = expiresAt.epochSeconds,
        user = info.toVira(fallback),
    )
}
