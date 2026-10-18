package cat.vira.android.app

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.core.data.model.CoinReason

/// Una acció «feta» que encara es pot desfer uns segons, abans que la recompensa es concedeixi de
/// veritat (`PendingCompletion` de l'iPhone, `ViraInlineCompletion.swift`).
///
/// Tocar una casella marca l'ítem **a l'instant**; només es retarda la concessió. L'XP i les
/// ViriCoins mai baixen, així que concedir-les al toc deixaria un «Desfer» que no desfà res.
data class PendingCompletion(
    /// Identitat de l'entitat marcada (p. ex. `"plan-$id"`): la fila amb el mateix `id` és la que
    /// mostra el «Desfer» i, després, la revelació.
    val id: String,
    val reason: CoinReason,
    /// Reverteix el marcatge (p. ex. torna la tasca a pendent).
    val onUndo: () -> Unit,
    /// Efecte propi del tipus d'acció, més enllà de la recompensa. Només si no s'ha desfet.
    val onCommit: (suspend () -> Unit)? = null,
    /// Nom de la cosa marcada, quan la celebració que ve després l'ha de dir (l'hàbit).
    val title: String? = null,
)

/// Fase visible de l'acció pendent, ja calculada per la cua: cada fila només l'ha de dibuixar.
sealed interface CompletionPhase {
    data object Pending : CompletionPhase
    data class Revealing(val coins: Int, val xp: Int) : CompletionPhase
}

/// L'única acció pendent de tota l'app en aquest instant, amb la seva fase.
data class ActiveCompletion(val id: String, val phase: CompletionPhase)

/// Marca una acció com a feta i n'obre la finestra de «Desfer». Si n'hi havia una altra pendent
/// d'una altra entitat, aquella es dona per definitiva i es concedeix sense revelació.
val LocalPresentCompletion = staticCompositionLocalOf<(PendingCompletion) -> Unit> { {} }

/// Desfà l'acció pendent si `id` hi coincideix, sense concedir res. Inofensiu si ja no és l'activa.
val LocalUndoCompletion = staticCompositionLocalOf<(String) -> Unit> { {} }

/// L'acció pendent ara mateix, si n'hi ha. Cada fila la compara amb el seu `id` per saber si li toca
/// el «Desfer» o la revelació **dins de la seva pròpia targeta**.
val LocalActiveCompletion = compositionLocalOf<ActiveCompletion?> { null }
