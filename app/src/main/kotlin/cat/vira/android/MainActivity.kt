package cat.vira.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cat.vira.android.app.RootScreen
import cat.vira.android.core.design.ViraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si Android ha tancat l'app amb el navegador obert, la tornada arriba amb estat desat: es
        // fa servir igual, però només un cop (un gir de pantalla no l'ha de tornar a enviar).
        handled = savedInstanceState?.getString(HANDLED_CALLBACK)
        handleCallback(intent)
        setContent {
            ViraTheme {
                RootScreen()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(HANDLED_CALLBACK, handled)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallback(intent)
    }

    /// Tornada del navegador (Google, Apple) o d'un enllaç del correu: `vira://auth-callback?…`.
    private fun handleCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.toString() == handled) return
        handled = uri.toString()
        (application as ViraApp).backend.handleCallback(uri)
    }

    private var handled: String? = null

    private companion object {
        const val HANDLED_CALLBACK = "cat.vira.android.handledCallback"
    }
}
