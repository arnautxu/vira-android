package cat.vira.android.features.help

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpLogicTest {

    @Test
    fun elCicleEsDeQuatreDosQuatre() {
        assertEquals(10, HelpBreathing.cycleSeconds)
        assertEquals((0..3).map { BreathPhase.Inhale }, (0..3).map(HelpBreathing::phase))
        assertEquals(listOf(BreathPhase.Hold, BreathPhase.Hold), (4..5).map(HelpBreathing::phase))
        assertEquals((6..9).map { BreathPhase.Exhale }, (6..9).map(HelpBreathing::phase))
        assertEquals(BreathPhase.Inhale, HelpBreathing.phase(10))
        assertEquals(BreathPhase.Exhale, HelpBreathing.phase(59))
    }

    @Test
    fun cadaTramEsDisparaUnSolCop() {
        val starts = (0 until HelpBreathing.TOTAL_SECONDS).mapNotNull { second ->
            HelpBreathing.stretchStartingAt(second)?.let { second to it }
        }
        // Sis cicles: inspirar a 0, 10… i expirar a 6, 16…; aguantar no en té.
        assertEquals(12, starts.size)
        assertEquals(0 to BreathPhase.Inhale, starts[0])
        assertEquals(6 to BreathPhase.Exhale, starts[1])
        assertEquals(56 to BreathPhase.Exhale, starts.last())
    }

    @Test
    fun enAcabarNoComencaCapTram() {
        assertNull(HelpBreathing.stretchStartingAt(60))
        assertNull(HelpBreathing.stretchStartingAt(66))
        assertNull(HelpBreathing.stretchStartingAt(-1))
    }

    @Test
    fun laVibracioPujaIBaixaSenseApagarSe() {
        val (timings, inhale) = HelpBreathingHaptics.rampWaveform(inhale = true, durationMillis = 4000)
        assertEquals(40, timings.size)
        assertEquals(4000L, timings.sum())
        assertEquals(13, inhale.first())
        assertEquals(153, inhale.last())
        assertTrue(inhale.toList().zipWithNext().all { (a, b) -> b >= a })

        val (_, exhale) = HelpBreathingHaptics.rampWaveform(inhale = false, durationMillis = 4000)
        assertArrayEquals(inhale.reversedArray(), exhale)
        assertTrue(exhale.all { it in 1..255 })
    }

    @Test
    fun lAncoratgeVaDeCincAUn() {
        assertEquals(5, GroundingSense.count)
        assertEquals(GroundingSense.See, GroundingSense.current(0))
        assertEquals(GroundingSense.Taste, GroundingSense.current(4))
        assertEquals(GroundingSense.Taste, GroundingSense.current(5))
        assertEquals(1, GroundingSense.shownStep(0))
        assertEquals(5, GroundingSense.shownStep(5))
        assertFalse(GroundingSense.isFinished(4))
        assertTrue(GroundingSense.isFinished(5))
    }

    @Test
    fun aMissatgesNomesHiPassenXifresIMes() {
        assertEquals("+34600112233", smsDigits("+34 600 11 22 33"))
        assertEquals("934001122", smsDigits("(93) 400-11-22"))
        assertEquals("", smsDigits("Casa"))
    }
}
