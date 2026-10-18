package cat.vira.android.features.progress

import cat.vira.android.core.resources.ViraVideo
import cat.vira.android.features.progress.logic.StreakMilestone
import cat.vira.android.features.progress.logic.StreakSceneVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/// `ViraStreakSceneVideoTests` de l'iPhone: un clip diferent cada dia, i la sèrie torna a començar.
class StreakSceneVideoTest {

    @Test fun laSerieRecorreOnzeEscenesITornaACommencar() {
        assertNull(StreakSceneVideo.scene(0))
        assertEquals(ViraVideo.StreakDay02, StreakSceneVideo.scene(1))
        assertEquals(ViraVideo.StreakDay14, StreakSceneVideo.scene(11))
        assertEquals(ViraVideo.StreakDay02, StreakSceneVideo.scene(12))
    }

    @Test fun dosDiesSeguitsMaiTenenElMateixClip() {
        (1..40).forEach { assertTrue(StreakSceneVideo.scene(it) != StreakSceneVideo.scene(it + 1)) }
    }

    @Test fun elDiaNormalProtegeixElTextIElsDescartatsNoSurten() {
        (1..30).forEach { days ->
            val backdrop = StreakSceneVideo.celebrationBackdrop(days, null, soundEnabled = true)!!
            assertTrue(backdrop.protectsText)
            assertFalse(backdrop.sound)
            assertFalse(backdrop.video.name.endsWith("01") || backdrop.video.name.endsWith("06") || backdrop.video.name.endsWith("10"))
        }
    }

    @Test fun lesFitesConservenElSeuVideoFoscIAmbSo() {
        val week = StreakSceneVideo.celebrationBackdrop(7, StreakMilestone.WEEK_CARD, soundEnabled = true)!!
        assertEquals(ViraVideo.ViriStreakWeek, week.video)
        assertTrue(week.isDark && week.sound && week.textOnTop)
        val month = StreakSceneVideo.celebrationBackdrop(30, StreakMilestone.MONTH_CARD, soundEnabled = false)!!
        assertEquals(ViraVideo.ViriStreakMonth, month.video)
        assertTrue(month.isDark)
        assertFalse(month.sound || month.textOnTop)
    }
}
