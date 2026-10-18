package cat.vira.android.app

import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.components.CompletionPhase
import cat.vira.android.core.design.components.PendingCompletion
import cat.vira.android.features.progress.logic.ChallengeItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompletionQueueTest {

    private val committed = mutableListOf<CoinReason>()

    private val deferrals = mutableListOf<Boolean>()

    private fun TestScope.queue(
        firstHabit: Boolean = true,
        commitMillis: Long = 0,
        challenge: ChallengeMoment? = null,
    ) = CompletionQueue(
        scope = backgroundScope,
        commit = { reason, _ -> delay(commitMillis); committed += reason; 3 to 3 },
        isFirstHabitOfDay = { firstHabit },
        undoWindowMillis = { 2_000 },
        challengeCompleted = { _, _ -> challenge },
    ).also { it.deferStreak = { deferred -> deferrals += deferred } }

    @Test
    fun `una acció nova a mitja concessió no deixa la ratxa retinguda`() = runTest {
        val queue = queue(commitMillis = 1_000)
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_500)
        queue.present(PendingCompletion("plan-2", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(deferrals.count { it }, deferrals.count { !it })
    }

    @Test
    fun `la ratxa es reté mentre es concedeix i s'allibera si no s'obre res`() = runTest {
        val queue = queue()
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(listOf(true, false), deferrals)
    }

    @Test
    fun `el moment de repte surt un cop per cicle i reté la ratxa`() = runTest {
        val item = ChallengeItem.catalog.first()
        val queue = queue(challenge = ChallengeMoment(item, item.threshold, "${item.key}-0"))
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(item, queue.challengeMoment?.item)
        assertEquals(listOf(true), deferrals)

        queue.challengeMoment = null
        queue.present(PendingCompletion("plan-2", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_001)
        runCurrent()
        assertNull(queue.challengeMoment)
    }

    @Test
    fun `desfer dins de la finestra no concedeix res`() = runTest {
        val queue = queue()
        var undone = false
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = { undone = true }))
        advanceTimeBy(1_000)
        queue.undo("plan-1")
        // `advanceUntilIdle` no fa córrer la feina de `backgroundScope`: el temps s'avança a mà.
        advanceTimeBy(10_000)
        runCurrent()
        assertTrue(undone)
        assertTrue(committed.isEmpty())
        assertNull(queue.active)
    }

    @Test
    fun `passada la finestra es concedeix i es revela`() = runTest {
        val queue = queue()
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(listOf(CoinReason.PLAN_ITEM), committed)
        assertEquals(CompletionPhase.Revealing(3, 3), queue.active?.phase)
        advanceTimeBy(1_801)
        runCurrent()
        assertNull(queue.active)
    }

    @Test
    fun `una altra acció dona l'anterior per feta`() = runTest {
        val queue = queue()
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        queue.present(PendingCompletion("habit-1", CoinReason.HABIT, onUndo = {}))
        runCurrent()
        assertEquals(listOf(CoinReason.PLAN_ITEM), committed)
        assertEquals("habit-1", queue.active?.id)
    }

    @Test
    fun `desfer una altra fila no toca l'activa`() = runTest {
        val queue = queue()
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        queue.undo("plan-2")
        assertEquals("plan-1", queue.active?.id)
    }

    @Test
    fun `només el primer hàbit del dia obre la celebració`() = runTest {
        val first = queue(firstHabit = true)
        first.present(PendingCompletion("habit-1", CoinReason.HABIT, onUndo = {}, title = "Aigua"))
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals("Aigua", first.habitCelebration?.title)

        val second = queue(firstHabit = false)
        second.present(PendingCompletion("habit-2", CoinReason.HABIT, onUndo = {}, title = "Llegir"))
        advanceTimeBy(2_001)
        runCurrent()
        assertNull(second.habitCelebration)
    }

    @Test
    fun `una acció nova mentre l'anterior es concedeix no la concedeix dues vegades`() = runTest {
        val queue = queue(commitMillis = 500)
        queue.present(PendingCompletion("plan-1", CoinReason.PLAN_ITEM, onUndo = {}))
        advanceTimeBy(2_100)
        queue.present(PendingCompletion("habit-1", CoinReason.HABIT, onUndo = {}))
        // `advanceUntilIdle` no fa córrer la feina de `backgroundScope`: el temps s'avança a mà.
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(listOf(CoinReason.PLAN_ITEM, CoinReason.HABIT), committed)
        assertNull(queue.active)
    }
}
