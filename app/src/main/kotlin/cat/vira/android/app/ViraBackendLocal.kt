package cat.vira.android.app

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.core.backend.ViraBackend
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/// Les dades, el compte i la sincronització de l'execució, per a qualsevol pantalla
/// (`LocalViraBackend.current.db`, `.session`). L'equivalent del `modelContainer` i de
/// `.environment(session)` de l'iPhone. El posa `MainActivity`; les captures no en tenen.
val LocalViraBackend = staticCompositionLocalOf<ViraBackend> {
    error("LocalViraBackend només existeix dins de MainActivity")
}

/// Els enllaços que han obert l'app i no són de l'auth (`vira://link/…`, `https://vira.cat/link/…`).
/// L'últim es conserva: un enllaç pot arribar abans que la pantalla que l'entén s'hagi muntat.
val LocalViraIncomingLinks = staticCompositionLocalOf<SharedFlow<Uri>> { MutableSharedFlow() }
