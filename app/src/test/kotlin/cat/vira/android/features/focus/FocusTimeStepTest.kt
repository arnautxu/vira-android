package cat.vira.android.features.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/// El pas de cinc minuts del panell de sortida (`FocusTimeStepper`): mai surt del marge del mode i
/// un valor desalineat torna a la graella.
class FocusTimeStepTest {

    private val range = 5..60

    @Test fun pujaIBaixaDeCincEnCinc() {
        assertEquals(30, FocusTimeStep.next(25, 5, range))
        assertEquals(20, FocusTimeStep.next(25, -5, range))
    }

    @Test fun noSurtDelMarge() {
        assertNull(FocusTimeStep.next(60, 5, range))
        assertNull(FocusTimeStep.next(5, -5, range))
    }

    @Test fun unValorDesalineatTornaALaGraella() {
        assertEquals(10, FocusTimeStep.next(7, 5, range))
        assertEquals(5, FocusTimeStep.next(7, -5, range))
        assertEquals(5, FocusTimeStep.next(0, 5, range))
    }

    @Test fun senseMargeNoHiHaPas() {
        assertNull(FocusTimeStep.next(25, 5, null))
    }
}
