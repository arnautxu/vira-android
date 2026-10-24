package cat.vira.android.features.assist

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.vira.android.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/// L'estat del full de passos (`AssistanceStartFlow` de l'iPhone, la part de Viri amb Claude):
/// demanar, triar, desar i desfer. Res es desa fins a «Afegir».
@Stable
class ViriAssistStepsFlow(
    private val scope: CoroutineScope,
    private val ask: suspend (String) -> List<ViriAssist.StepProposal>,
    private val commit: AssistanceCommit?,
) {
    val steps = mutableStateListOf<AssistanceStep>()
    var working by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    @get:StringRes var message by mutableStateOf<Int?>(null)
        private set
    private var job: Job? = null

    val selectedCount: Int get() = steps.count { it.isSelected }
    val isThinking: Boolean get() = working && steps.isEmpty()
    val canSave: Boolean get() = !saved && !working && AssistanceDraft.problem(steps) == null

    /// El que es diu sota els passos si els marcats no es poden desar tal com són.
    @get:StringRes val validationMessage: Int?
        get() = if (steps.none { it.isSelected }) null else when (AssistanceDraft.problem(steps)) {
            null -> null
            AssistanceProblem.DUPLICATE -> R.string.assistance_duplicate_steps
            AssistanceProblem.TOO_LONG -> R.string.assistance_long_step
            else -> R.string.assistance_empty_step
        }

    /// Demana passos. «Uns altres» torna a demanar i conserva els marcats i els escrits a mà.
    fun generate(title: String) {
        cancel()
        working = true
        message = null
        job = scope.launch {
            val result = try {
                Result.success(ask(title))
            } catch (error: CancellationException) {
                throw error
            } catch (error: ViriAssist.FailureException) {
                Result.failure(error)
            } catch (error: Exception) {
                Result.failure(ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE))
            }
            working = false
            result.onFailure { message = (it as ViriAssist.FailureException).failure.message }
            result.onSuccess { proposals ->
                val merged = AssistanceDraft.merged(steps.toList(), proposals, title)
                if (merged == null) {
                    message = R.string.assistance_generation_empty
                } else {
                    steps.clear()
                    steps += merged
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        working = false
    }

    fun toggle(id: UUID) = update(id) { it.copy(isSelected = !it.isSelected) }

    fun edit(id: UUID, text: String) = update(id) { it.copy(text = text) }

    fun remove(id: UUID) { steps.removeAll { it.id == id } }

    fun addManualStep() {
        cancel()
        if (steps.size >= AssistanceDraft.MAXIMUM_STEPS) return
        // El pas escrit a mà neix marcat: l'ha triat la persona. Els de Viri, no.
        steps += AssistanceStep(text = "", isSelected = true)
        message = null
    }

    fun save(parentId: UUID, originalTitle: String, updatedAt: Instant, onSaved: () -> Unit) {
        val commit = commit ?: return
        if (saved) return
        scope.launch {
            message = try {
                commit.add(steps.toList(), parentId, originalTitle, updatedAt)
                saved = true
                onSaved()
                null
            } catch (error: AssistanceException) {
                if (error.problem == AssistanceProblem.STALE) R.string.assistance_stale else R.string.assistance_save_error
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                R.string.assistance_save_error
            }
        }
    }

    fun undo() {
        val commit = commit ?: return
        scope.launch {
            message = try {
                commit.undo()
                saved = false
                null
            } catch (error: AssistanceException) {
                if (error.problem == AssistanceProblem.CHANGED_SINCE_SAVE) R.string.assistance_undo_conflict else R.string.assistance_save_error
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                R.string.assistance_save_error
            }
        }
    }

    private fun update(id: UUID, change: (AssistanceStep) -> AssistanceStep) {
        val index = steps.indexOfFirst { it.id == id }
        if (index >= 0) steps[index] = change(steps[index])
    }
}
