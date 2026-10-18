package cat.vira.android.app

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.core.backend.ViraBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/// Les dades, el compte i la sincronització de l'execució, per a qualsevol pantalla
/// (`LocalViraBackend.current.db`, `.session`). L'equivalent del `modelContainer` i de
/// `.environment(session)` de l'iPhone. El posa `MainActivity`; les captures no en tenen.
val LocalViraBackend = staticCompositionLocalOf<ViraBackend> {
    error("LocalViraBackend només existeix dins de MainActivity")
}

/// Els enllaços que han obert l'app i no són de l'auth (`vira://link/…`, `https://vira.cat/link/…`).
///
/// N'hi ha com a molt un de pendent: es conserva fins que qui l'entén el gasta (`consume`), perquè
/// pot arribar abans que la pantalla s'hagi muntat, i un cop gastat no torna encara que la pantalla
/// es torni a crear.
class ViraIncomingLinks {
    private val _pending = MutableStateFlow<Uri?>(null)
    val pending: StateFlow<Uri?> = _pending.asStateFlow()

    fun deliver(uri: Uri) { _pending.value = uri }

    /// Marca l'enllaç com a fet servir. Inofensiu si ja n'ha arribat un altre.
    fun consume(uri: Uri) { _pending.compareAndSet(uri, null) }
}

val LocalViraIncomingLinks = staticCompositionLocalOf { ViraIncomingLinks() }
