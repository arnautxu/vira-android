package cat.vira.android.features.assist

import androidx.room.withTransaction
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.PlanAssistanceDetail
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.features.plan.PlanItemOrder
import java.text.Normalizer
import java.time.Instant
import java.util.UUID

/// Un pas proposat, editable (`AssistanceStep` de l'iPhone). Arriben desmarcats; el que s'escriu a
/// mà neix marcat, perquè l'ha triat la persona. Els minuts només s'ensenyen: la tasca no en desa.
data class AssistanceStep(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val isSelected: Boolean = false,
    val minutes: Int? = null,
)

/// Per què no es poden desar els passos marcats (`AssistanceValidationError`).
enum class AssistanceProblem { EMPTY, DUPLICATE, TOO_LONG, TOO_MANY, STALE, CHANGED_SINCE_SAVE }

class AssistanceException(val problem: AssistanceProblem) : Exception(problem.name)

object AssistanceDraft {
    const val MAXIMUM_STEPS = 30
    const val MAXIMUM_TITLE_LENGTH = 500

    /// Els títols dels marcats, nets, o el primer problema que ho impedeix.
    fun selectedTitles(steps: List<AssistanceStep>): List<String> {
        val selected = steps.filter { it.isSelected }
        if (selected.isEmpty()) throw AssistanceException(AssistanceProblem.EMPTY)
        if (selected.size > MAXIMUM_STEPS) throw AssistanceException(AssistanceProblem.TOO_MANY)
        val seen = mutableSetOf<String>()
        return selected.map { step ->
            val text = step.text.trim()
            if (text.isEmpty()) throw AssistanceException(AssistanceProblem.EMPTY)
            if (text.length > MAXIMUM_TITLE_LENGTH) throw AssistanceException(AssistanceProblem.TOO_LONG)
            if (!seen.add(fold(text))) throw AssistanceException(AssistanceProblem.DUPLICATE)
            text
        }
    }

    fun problem(steps: List<AssistanceStep>): AssistanceProblem? =
        try { selectedTitles(steps); null } catch (error: AssistanceException) { error.problem }

    /// Els passos de Viri, sense repetits —ni entre ells, ni amb els que ja hi són, ni amb la
    /// tasca— i desmarcats.
    fun viriSteps(proposals: List<ViriAssist.StepProposal>, excluding: List<String>): List<AssistanceStep> {
        val seen = excluding.map(::fold).toMutableSet()
        return proposals.mapNotNull { proposal ->
            val text = proposal.title.trim()
            if (text.isEmpty() || text.length > MAXIMUM_TITLE_LENGTH || !seen.add(fold(text))) null
            else AssistanceStep(text = text, minutes = proposal.minutes)
        }
    }

    /// «Uns altres» conserva els marcats i els escrits a mà, i hi afegeix els nous.
    fun merged(current: List<AssistanceStep>, proposals: List<ViriAssist.StepProposal>, title: String): List<AssistanceStep>? {
        val kept = current.filter { it.isSelected || it.minutes == null }
        val fresh = viriSteps(proposals, kept.map { it.text } + title)
        if (fresh.isEmpty()) return null
        return (kept + fresh).take(MAXIMUM_STEPS)
    }

    fun fold(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}

/// Una escriptura reversible (`AssistanceCommit` de l'iPhone): els passos marcats com a tasques del
/// mateix dia, penjades de l'original. Cap recompensa ni cap crida a fora.
class AssistanceCommit(private val store: ViraStore, private val clock: () -> Instant = Instant::now) {

    private var receipt: List<Pair<PlanItem, PlanAssistanceDetail>>? = null

    suspend fun add(steps: List<AssistanceStep>, parentId: UUID, expectedTitle: String, expectedUpdatedAt: Instant) {
        if (receipt != null) return // Un segon toc és la mateixa operació.
        val titles = AssistanceDraft.selectedTitles(steps)
        val db = store.db
        receipt = db.withTransaction {
            val parent = db.planItems().byId(parentId)
            if (parent == null || parent.title != expectedTitle || parent.updatedAt != expectedUpdatedAt ||
                parent.status != PlanItemStatus.PENDING
            ) throw AssistanceException(AssistanceProblem.STALE)
            val now = clock()
            titles.mapIndexed { offset, title ->
                val child = store.create(
                    db.planItems(),
                    PlanItem(
                        title = title,
                        day = parent.day,
                        // L'ordre de la persona no es toca: el dels passos va a part.
                        sortIndex = PlanItemOrder.rank(parent),
                        createdAt = now.plusMillis(offset.toLong()),
                    ),
                )
                val detail = store.create(
                    db.assistanceDetails(),
                    PlanAssistanceDetail(id = child.id, parentPlanItemID = parent.id, stepOrder = offset),
                )
                child to detail
            }
            // Tal com han quedat desades (la base de dades arrodoneix les hores): és el que
            // compararà Desfer.
        }.map { (child, detail) -> db.planItems().byId(child.id)!! to db.assistanceDetails().byId(detail.id)!! }
    }

    /// Tot o res. Un pas editat, fet o sincronitzat d'un altre telèfon atura el lot sencer.
    suspend fun undo() {
        val saved = receipt ?: throw AssistanceException(AssistanceProblem.STALE)
        val db = store.db
        val current = db.withTransaction {
            saved.map { (item, detail) -> db.planItems().byId(item.id) to db.assistanceDetails().byId(detail.id) }
        }
        if (current != saved) throw AssistanceException(AssistanceProblem.CHANGED_SINCE_SAVE)
        saved.forEach { (item, _) -> store.delete(item) }
        receipt = null
    }
}
