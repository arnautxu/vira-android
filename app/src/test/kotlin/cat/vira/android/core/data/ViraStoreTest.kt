package cat.vira.android.core.data

import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.model.PlanAssistanceDetail
import cat.vira.android.core.data.model.PlanAssistanceLink
import cat.vira.android.core.data.model.PlanItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ViraStoreTest {

    private val now = Instant.parse("2026-10-18T10:00:00Z")
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db, clock = { now })

    @After fun close() = db.close()

    @Test
    fun crearDataLaFilaExceptesiPortaElSentinella() = runBlocking {
        val item = store.create(db.planItems(), PlanItem(title = "Deures", updatedAt = Instant.EPOCH))
        assertEquals(now, item.updatedAt)
        val marker = store.create(db.planItems(), PlanItem(title = "Marcador", updatedAt = ViraTime.distantPast))
        assertEquals(ViraTime.distantPast, marker.updatedAt)
        assertEquals(now, store.save(db.planItems(), marker).updatedAt)
    }

    @Test
    fun elsSingletonsNeixenAmbElSentinella() = runBlocking {
        assertEquals(ViraTime.distantPast, store.counter().updatedAt)
        assertEquals(ViraTime.distantPast, store.streak().updatedAt)
        assertEquals(ViraTime.distantPast, store.profile().updatedAt)
        assertEquals(1, db.progressCounters().all().size)
        store.counter()
        assertEquals(1, db.progressCounters().all().size)
    }

    @Test
    fun esborrarUnaTascaDeixaLapidaIDeslligaElQuePenjava() = runBlocking {
        val item = store.create(db.planItems(), PlanItem(title = "Gran"))
        val other = store.create(db.planItems(), PlanItem(title = "Després"))
        store.create(db.assistanceDetails(), PlanAssistanceDetail(id = item.id))
        store.create(db.assistanceDetails(), PlanAssistanceDetail(id = other.id, parentPlanItemID = item.id, stepOrder = 1))
        store.create(db.assistanceLinks(), PlanAssistanceLink(id = other.id, beforePlanItemID = item.id))

        store.delete(item)

        assertEquals(listOf(other.id), db.planItems().all().map { it.id })
        assertEquals(listOf(item.id), db.tombstones().recordIDs(ViraStore.TABLE_PLAN_ITEMS))
        val step = db.assistanceDetails().all().single()
        assertEquals(other.id, step.id)
        assertNull(step.parentPlanItemID)
        assertNull(step.stepOrder)
        assertNull(db.assistanceLinks().all().single().beforePlanItemID)
    }
}
