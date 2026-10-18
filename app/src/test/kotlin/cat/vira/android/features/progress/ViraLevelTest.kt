package cat.vira.android.features.progress

import cat.vira.android.features.progress.logic.ViraLevel
import cat.vira.android.features.progress.logic.ViraLevel.LevelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/// `ViraLevelTests` de l'iPhone: més XP mai fa baixar el nivell i la barra es queda a [0, 1].
class ViraLevelTest {

    @Test fun senseXpElNivellEs0() = assertEquals(0, ViraLevel.level(0))

    @Test fun elsLlindarsSonExactes() {
        assertEquals(0, ViraLevel.level(99))
        assertEquals(1, ViraLevel.level(100))
        assertEquals(1, ViraLevel.level(101))
        assertEquals(1, ViraLevel.level(299))
        assertEquals(2, ViraLevel.level(300))
    }

    @Test fun mesXpMaiFaBaixarDeNivell() {
        var previous = 0
        for (xp in 0..5_000 step 37) {
            val level = ViraLevel.level(xp)
            assertTrue(level >= previous)
            previous = level
        }
    }

    @Test fun laFraccioSempreEsQuedaEntre0i1() {
        for (xp in 0..5_000 step 23) {
            val fraction = ViraLevel.progress(xp).fraction
            assertTrue(fraction in 0f..1f)
        }
    }

    @Test fun aLIniciDUnNivellLaFraccioEs0() {
        val progress = ViraLevel.progress(ViraLevel.thresholdXP(3))
        assertEquals(3, progress.level)
        assertEquals(0, progress.currentLevelXP)
        assertEquals(0f, progress.fraction)
        assertEquals(400, progress.xpToNext)
    }

    @Test fun elsMarcsSegueixenElsTrams() {
        assertNull(ViraLevel.frame(0))
        assertEquals(LevelFrame.BRONZE, ViraLevel.frame(1))
        assertEquals(LevelFrame.BRONZE, ViraLevel.frame(4))
        assertEquals(LevelFrame.SILVER, ViraLevel.frame(5))
        assertEquals(LevelFrame.SILVER, ViraLevel.frame(9))
        assertEquals(LevelFrame.GOLD, ViraLevel.frame(10))
        assertEquals(LevelFrame.GOLD, ViraLevel.frame(19))
        assertEquals(LevelFrame.DIAMOND, ViraLevel.frame(20))
        assertEquals(LevelFrame.DIAMOND, ViraLevel.frame(100))
    }
}
