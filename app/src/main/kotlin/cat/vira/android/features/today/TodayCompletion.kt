package cat.vira.android.features.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cat.vira.android.core.design.components.ActiveCompletion
import cat.vira.android.core.design.components.CompletionPhase
import cat.vira.android.core.design.components.ViraInlineCompletion

/// La finestra de «Fet · Desfer» tal com Avui la llegeix (`activeCompletion` i `undoCompletion`
/// de l'iPhone). La cua que la cronometra i concedeix la recompensa és de l'arrel de l'app; aquí
/// només es diu quina fila està oberta i com desfer-la. Una classe i no els `CompositionLocal`
/// directament perquè les captures hi puguin posar una fila a mig desfer.
internal class TodayCompletion(
    val active: ActiveCompletion? = null,
    val undo: (String) -> Unit = {},
) {
    val activeId: String? get() = active?.id

    fun phaseFor(id: String): CompletionPhase? = active?.takeIf { it.id == id }?.phase
}

@Composable
internal fun TodayInlineCompletion(phase: CompletionPhase, ink: Color, onUndo: () -> Unit, modifier: Modifier = Modifier) =
    ViraInlineCompletion(phase, ink, onUndo, modifier)
