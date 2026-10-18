package cat.vira.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cat.vira.android.app.RootScreen
import cat.vira.android.core.design.ViraTheme

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
            ViraTheme {
                RootScreen()
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
    private fun handleCallback(intent: Intent?) {
        backend.handleCallback(intent?.data ?: return)
    }

    private companion object {
        const val ADOPTED_CALLBACK = "cat.vira.android.adoptedCallback"
    }
}
