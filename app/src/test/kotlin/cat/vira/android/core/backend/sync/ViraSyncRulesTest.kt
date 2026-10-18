package cat.vira.android.core.backend.sync

import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.PlanItem
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ViraSyncRulesTest {

    private val earlier = Instant.parse("2026-10-01T10:00:00Z")
    private val later = Instant.parse("2026-10-02T10:00:00Z")

    private fun row(at: Instant) = PlanItemSyncRow(PlanItem(title = "x", updatedAt = at))

    @Test
    fun guanyaElMesNou() {
        assertTrue(shouldOverwrite(row(later), PlanItem(updatedAt = earlier)))
        assertFalse(shouldOverwrite(row(earlier), PlanItem(updatedAt = later)))
        assertFalse(shouldOverwrite(row(earlier), PlanItem(updatedAt = earlier)))
    }

    @Test
    fun elSentinellaSemprePerd() {
        assertTrue(shouldOverwrite(row(ViraTime.distantPast), PlanItem(updatedAt = ViraTime.distantPast)))
        assertTrue(shouldOverwrite(row(earlier), PlanItem(updatedAt = ViraTime.distantPast)))
    }

    @Test
    fun elsMarcadorsNoSurtenMai() {
        val rows = listOf(row(ViraTime.distantPast), row(earlier), row(later))
        assertEquals(listOf(earlier, later), ViraSyncEngine.outgoing(rows, null).map { it.updatedAt })
        assertEquals(listOf(later), ViraSyncEngine.outgoing(rows, earlier).map { it.updatedAt })
    }

    @Test
    fun laFronteraEsComparaEnOrdre() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val json = buildJsonObject {
            put("updated_at", JsonPrimitive("2026-10-01T10:00:00.123456+00:00"))
            put("key", JsonPrimitive("viri-gold"))
            put("id", JsonPrimitive(id.toString()))
        }
        val boundary = ViraSyncPage.boundary(json, listOf("key"))
        assertEquals(listOf("updated_at" to "2026-10-01T10:00:00.123456+00:00", "key" to "viri-gold"), boundary)
        val param = ViraSyncPage.after(boundary)
        assertEquals("or", param.name)
        assertEquals(
            "(updated_at.gt.\"2026-10-01T10:00:00.123456+00:00\",and(updated_at.eq.\"2026-10-01T10:00:00.123456+00:00\",key.gt.\"viri-gold\"))",
            param.value,
        )
    }

    @Test
    fun lesTaulesAmbClauNaturalOrdenenPerLaClau() {
        assertEquals("updated_at.asc,id.asc", PlanItemSyncTable.order)
        assertEquals("updated_at.asc,key.asc", SkinOwnershipSyncTable.order)
        assertEquals("updated_at.asc,key.asc,cycle_start.asc", ChallengeClaimSyncTable.order)
    }
}
