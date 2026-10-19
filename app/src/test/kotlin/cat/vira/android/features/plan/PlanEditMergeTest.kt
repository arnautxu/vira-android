package cat.vira.android.features.plan

import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/// Desar l'editor sobre la fila d'ara: el que l'editor no ha tocat no trepitja el que hagi arribat entremig.
class PlanEditMergeTest {

    private val original = PlanItem(title = "Deures", notes = null, symbolName = "picto-flag")

    @Test
    fun nomesEntraElQueLEditorHaCanviat() {
        val edited = original.copy(title = "Deures de mates")
        val current = original.copy(
            notes = "Del portàtil",
            statusRaw = PlanItemStatus.DONE.raw,
            completedAt = Instant.parse("2026-01-15T10:00:00Z"),
        )
        val merged = PlanEditMerge.merge(original, edited, current)
        assertEquals("Deures de mates", merged.title)
        assertEquals("Del portàtil", merged.notes)
        assertEquals(PlanItemStatus.DONE.raw, merged.statusRaw)
        assertEquals(current.completedAt, merged.completedAt)
    }

    @Test
    fun siLEditorIElServidorCanvienElMateixCampGuanyaLEditor() {
        val merged = PlanEditMerge.merge(original, original.copy(title = "A"), original.copy(title = "B"))
        assertEquals("A", merged.title)
    }
}
