package cat.vira.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraIncomingLinks
import cat.vira.android.app.RootScreen
import cat.vira.android.app.ViraOnboardingGate
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.ViraTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val backend get() = (application as ViraApp).backend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si Android ha tancat l'app amb el navegador obert, la tornada arriba amb estat desat: es
        // fa servir igual. El que ja havia obert sessió abans de tancar-se no es torna a enviar.
        backend.rememberAdoptedCallback(savedInstanceState?.getString(ADOPTED_CALLBACK))
        handleCallback(intent)
        setContent {
            // Ajustos → Aparença. `auto` (per defecte) deixa que mani el sistema.
            val appearance by rememberPreference(backend.preferences, ViraPreferences.APPEARANCE, "auto")
            val dark = when (appearance) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(
                LocalViraBackend provides backend,
                LocalViraIncomingLinks provides incomingLinks,
            ) {
                ViraTheme(darkTheme = dark) {
                    ViraOnboardingGate { RootScreen() }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ADOPTED_CALLBACK, backend.lastAdoptedCallback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Si Android tanca el procés a mig intercanvi, l'activitat nova ha de trobar aquest enllaç.
        setIntent(intent)
        handleCallback(intent)
    }

    /// Tornada del navegador (Google, Apple) o d'un enllaç del correu: `vira://auth-callback?…`.
    /// `ViraBackend` evita fer-la servir dos cops.
    ///
    /// Qualsevol altre enllaç (el de vinculació del panell de pares) es reparteix a `incomingLinks`
    /// abans de passar-lo a l'auth, que l'ignoraria en silenci.
    private fun handleCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "vira" && uri.host == "auth-callback") {
            backend.handleCallback(uri)
        } else {
            incomingLinks.tryEmit(uri)
        }
    }

    /// Els enllaços que obren l'app i no són de l'auth. Els llegeix qui els entén (el panell de
    /// pares, per al codi de vinculació).
    private val incomingLinks = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 1)

    private companion object {
        const val ADOPTED_CALLBACK = "cat.vira.android.adoptedCallback"
    }
}
