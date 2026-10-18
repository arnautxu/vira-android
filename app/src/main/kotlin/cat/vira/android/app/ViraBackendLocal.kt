package cat.vira.android.app

import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.core.backend.ViraBackend

/// Les dades, el compte i la sincronització de l'execució, per a qualsevol pantalla
/// (`LocalViraBackend.current.db`, `.session`). L'equivalent del `modelContainer` i de
/// `.environment(session)` de l'iPhone. El posa `MainActivity`; les captures no en tenen.
val LocalViraBackend = staticCompositionLocalOf<ViraBackend> {
    error("LocalViraBackend només existeix dins de MainActivity")
}
