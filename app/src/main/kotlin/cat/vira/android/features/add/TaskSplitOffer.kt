package cat.vira.android.features.add

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.features.assist.ViriTaskSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/// Quan i com es proposa «La partim?» a la cara Tasca (`QuickCaptureForm+Split` i `+Jev` de
/// l'iPhone).
///
/// «És gran?» es pregunta a Jev en tocar «Següent» al primer pas, mentre la persona tria el dia i
/// l'hora. Quan no queda res per preguntar, s'espera com a molt `ViriTaskSize.PATIENCE_MILLIS` i,
/// si no ha respost, mana la regla del telèfon. Una sèrie no es parteix: els passos penjarien només
/// de la primera.
@Stable
class TaskSplitOffer(private val actions: AddSheetActions, private val scope: CoroutineScope) {
    private var jevTitle by mutableStateOf("")
    private var jevSize by mutableStateOf<ViriTaskSize?>(null)
    private var asking: Job? = null

    /// Mentre s'espera Jev, «Següent» no fa res: un segon toc no ha de desar dues vegades.
    var waitsForJev by mutableStateOf(false)
        private set

    /// La tasca desada que espera el «D'acord» de «Viri pensa amb Claude».
    var consentFor by mutableStateOf<PlanItem?>(null)
        private set

    /// La tasca desada que Viri parteix en passos.
    var splitting by mutableStateOf<PlanItem?>(null)

    private fun jevSizeOf(title: String): ViriTaskSize? = if (jevTitle == title) jevSize else null

    fun offers(state: TaskAddState): Boolean =
        actions.viriAssistAvailable && state.recurrence == PlanItemRecurrence.NONE &&
            ViriTaskSize.looksBig(state.titleToSave, jevSizeOf(state.titleToSave))

    /// Pregunta un cop per títol, i només si «La partim?» es podria proposar. `true` si hi ha una
    /// resposta en camí o ja arribada per a aquest títol.
    fun ask(state: TaskAddState): Boolean {
        val title = state.titleToSave
        if (!actions.viriAssistAvailable || state.recurrence != PlanItemRecurrence.NONE ||
            !ViriTaskSize.isWorthAsking(title)
        ) return false
        if (jevTitle == title) return true
        asking?.cancel()
        jevTitle = title
        jevSize = null
        asking = scope.launch {
            val size = actions.jevTaskSize(title)
            if (jevTitle == title) jevSize = size
        }
        return true
    }

    /// Espera Jev o la paciència, el primer que arribi, i continua.
    fun waitThenAdvance(onAdvance: () -> Unit) {
        val pending = asking?.takeIf { it.isActive } ?: return onAdvance()
        waitsForJev = true
        scope.launch {
            val patience = launch { delay(ViriTaskSize.PATIENCE_MILLIS) }
            select<Unit> {
                pending.onJoin {}
                patience.onJoin {}
            }
            patience.cancel()
            waitsForJev = false
            onAdvance()
        }
    }

    /// Si Jev ha dit que és petita, ja s'ha respost: no es tornarà a proposar.
    fun rememberIfSmall(state: TaskAddState) {
        val item = state.justAdded.lastOrNull()?.items?.firstOrNull() ?: return
        if (jevSizeOf(item.title) == ViriTaskSize.SMALL) actions.markSplitAnswered(item.id)
    }

    /// «Sí, partim-la» o «Deixa-la així», amb la tasca ja desada. El sí obre els passos, darrere de
    /// l'avís de la primera vegada; si l'avís es tanca sense «D'acord», no passa res més.
    fun answer(state: TaskAddState, wantsSteps: Boolean) {
        val item = state.justAdded.lastOrNull()?.items?.firstOrNull() ?: return
        actions.markSplitAnswered(item.id)
        if (!wantsSteps) return
        if (actions.isAssistAccepted()) splitting = item else consentFor = item
    }

    fun accept() {
        actions.acceptAssist()
        splitting = consentFor
        consentFor = null
    }

    fun dismissConsent() { consentFor = null }
}
