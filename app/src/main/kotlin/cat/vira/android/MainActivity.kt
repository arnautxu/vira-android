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
        if (savedInstanceState == null) handleCallback(intent)
        setContent {
            ViraTheme {
                RootScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallback(intent)
    }

    /// Tornada del navegador (Google, Apple) o d'un enllaç del correu: `vira://auth-callback?…`.
    private fun handleCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        (application as ViraApp).backend.handleCallback(uri)
    }
}
