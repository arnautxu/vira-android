package cat.vira.android

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraIncomingLinks
import cat.vira.android.app.RootScreen
import cat.vira.android.app.ViraAppLock
import cat.vira.android.app.ViraOnboardingGate
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.ViraTheme

/// `FragmentActivity` i no `ComponentActivity` a seques: el diàleg de l'empremta n'és un fragment.
class MainActivity : FragmentActivity() {
    private val backend get() = (application as ViraApp).backend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si Android ha tancat l'app amb el navegador obert, la tornada arriba amb estat desat: es
        // fa servir igual. El que ja havia obert sessió abans de tancar-se no es torna a enviar.
        backend.rememberAdoptedCallback(savedInstanceState?.getString(ADOPTED_CALLBACK))
        // Una activitat recreada (un gir, un procés tancat) torna a portar l'intent original: l'enllaç
        // de vinculació ja s'havia repartit i no s'ha de tornar a omplir. L'auth es protegeix sol.
        // El que encara no s'havia gastat sí que torna: si el procés ha mort, `incomingLinks` és nou.
        handleCallback(intent, isRestored = savedInstanceState != null)
        savedInstanceState?.getString(PENDING_LINK)?.let { incomingLinks.deliver(Uri.parse(it)) }
        setContent {
            // Ajustos → Aparença. `auto` (per defecte) deixa que mani el sistema.
            val appearance by rememberPreference(backend.preferences, ViraPreferences.APPEARANCE, "auto")
            val dark = when (appearance) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            // Les icones de la barra d'estat segueixen l'aparença triada, no la del sistema: amb
            // «fosc» sobre un mòbil clar quedarien fosques sobre la capçalera fosca.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            CompositionLocalProvider(
                LocalViraBackend provides backend,
                LocalViraIncomingLinks provides incomingLinks,
            ) {
                ViraTheme(darkTheme = dark) {
                    ViraOnboardingGate { ViraAppLock { RootScreen() } }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ADOPTED_CALLBACK, backend.lastAdoptedCallback)
        outState.putString(PENDING_LINK, incomingLinks.pending.value?.toString())
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
    private fun handleCallback(intent: Intent?, isRestored: Boolean = false) {
        val uri = intent?.data ?: return
        if (uri.scheme == "vira" && uri.host == "auth-callback") {
            backend.handleCallback(uri)
        } else if (!isRestored) {
            incomingLinks.deliver(uri)
        }
    }

    /// Els enllaços que obren l'app i no són de l'auth. Viuen al procés, no a l'activitat: un gir de
    /// pantalla abans que el panell el llegeixi no l'ha de perdre.
    private val incomingLinks get() = (application as ViraApp).incomingLinks

    private companion object {
        const val ADOPTED_CALLBACK = "cat.vira.android.adoptedCallback"
        const val PENDING_LINK = "cat.vira.android.pendingLink"

        // Els vels de la barra de navegació de tres botons, els mateixos que posa `enableEdgeToEdge()`.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
