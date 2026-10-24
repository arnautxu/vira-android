package cat.vira.android.features.assist

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.PlanItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// Desar els passos triats i desfer-los (`AssistanceCommitTests` de l'iPhone).
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AssistanceCommitTest {

    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db)

    @After fun close() = db.close()

    private val steps = listOf(
        AssistanceStep(text = "Triar el tema", isSelected = true),
        AssistanceStep(text = "Buscar fonts"),
        AssistanceStep(text = "Fer l'índex", isSelected = true),
    )

    @Test
    fun elsMarcatsPassenAlPlaPenjatsDeLaTascaIDesferElsTreu() = runBlocking {
        val parent = store.create(db.planItems(), PlanItem(title = "Treball de socials")).let { db.planItems().byId(it.id)!! }
        val commit = AssistanceCommit(store)
        commit.add(steps, parent.id, parent.title, parent.updatedAt)
        // Un segon toc és la mateixa operació.
        commit.add(steps, parent.id, parent.title, parent.updatedAt)

        val children = db.planItems().all().filter { it.id != parent.id }.sortedBy { it.createdAt }
        assertEquals(listOf("Triar el tema", "Fer l'índex"), children.map { it.title })
        assertTrue(children.all { it.day == parent.day })
        val details = db.assistanceDetails().all().associateBy { it.id }
        assertEquals(listOf(0, 1), children.map { details[it.id]?.stepOrder })
        assertTrue(children.all { details[it.id]?.parentPlanItemID == parent.id })

        commit.undo()
        assertEquals(listOf(parent.id), db.planItems().all().map { it.id })
        assertTrue(db.assistanceDetails().all().isEmpty())
    }

    @Test
    fun siLaTascaHaCanviatNoEsDesaRes() = runBlocking {
        val parent = store.create(db.planItems(), PlanItem(title = "Treball de socials")).let { db.planItems().byId(it.id)!! }
        try {
            AssistanceCommit(store).add(steps, parent.id, "Un altre títol", parent.updatedAt)
            fail("Havia de dir que ha canviat")
        } catch (error: AssistanceException) {
            assertEquals(AssistanceProblem.STALE, error.problem)
        }
        assertEquals(1, db.planItems().all().size)
    }

    @Test
    fun unPasEditatAturaElDesferSencer() = runBlocking {
        val parent = store.create(db.planItems(), PlanItem(title = "Treball de socials")).let { db.planItems().byId(it.id)!! }
        val commit = AssistanceCommit(store)
        commit.add(steps, parent.id, parent.title, parent.updatedAt)
        val child = db.planItems().all().first { it.id != parent.id }
        store.save(db.planItems(), child.copy(title = "Canviat"))
        try {
            commit.undo()
            fail("Havia d'aturar-se")
        } catch (error: AssistanceException) {
            assertEquals(AssistanceProblem.CHANGED_SINCE_SAVE, error.problem)
        }
        assertEquals(3, db.planItems().all().size)
    }
}
