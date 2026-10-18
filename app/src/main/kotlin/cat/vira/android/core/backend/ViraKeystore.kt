package cat.vira.android.core.backend

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/// Testimonis de la sessió, xifrats amb una clau del Keystore d'Android (el Keychain de l'iPhone).
///
/// La clau no surt mai del maquinari i Android l'esborra en desinstal·lar l'app: a diferència de
/// l'iPhone, on el Keychain sobreviu a l'app (`ViraSession.discardSessionIfReinstalled`), aquí una
/// instal·lació neta no hereta mai el compte de qui la tenia abans. El que es desa als
/// `SharedPreferences` és només el text xifrat.
class ViraKeystore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(account: String): String? {
        val stored = preferences.getString(account, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_LENGTH))
            String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
        } catch (_: Exception) {
            // Una clau que ja no casa (restaurada d'un altre telèfon, Keystore buidat): el que hi ha
            // és il·legible i val més oblidar-ho que arrossegar-ho.
            delete(account)
            null
        }
    }

    fun write(account: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit().putString(account, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    fun delete(account: String) {
        preferences.edit().remove(account).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val SESSION = "session"
        const val CODE_VERIFIER = "codeVerifier"
        private const val FILE = "vira.auth"
        private const val PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "cat.vira.android.auth"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
    }
}

/// On l'SDK de Supabase desa la sessió: al `ViraKeystore`, no als `SharedPreferences` en clar que
/// faria servir per defecte.
class ViraKeystoreSessionManager(private val keystore: ViraKeystore) : SessionManager, CodeVerifierCache {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) =
        keystore.write(ViraKeystore.SESSION, json.encodeToString(UserSession.serializer(), session))

    override suspend fun loadSession(): UserSession? = keystore.read(ViraKeystore.SESSION)?.let {
        try {
            json.decodeFromString(UserSession.serializer(), it)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteSession() = keystore.delete(ViraKeystore.SESSION)

    override suspend fun saveCodeVerifier(codeVerifier: String) = keystore.write(ViraKeystore.CODE_VERIFIER, codeVerifier)
    override suspend fun loadCodeVerifier(): String? = keystore.read(ViraKeystore.CODE_VERIFIER)
    override suspend fun deleteCodeVerifier() = keystore.delete(ViraKeystore.CODE_VERIFIER)
}
