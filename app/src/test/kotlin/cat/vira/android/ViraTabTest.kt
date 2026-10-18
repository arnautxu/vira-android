package cat.vira.android

import cat.vira.android.app.ViraTab
import org.junit.Assert.assertEquals
import org.junit.Test

class ViraTabTest {
    @Test
    fun hiHaQuatrePestanyesEnElMateixOrdreQueLIphone() {
        assertEquals(listOf("today", "plan", "emotions", "progress"), ViraTab.entries.map { it.route })
    }
}
