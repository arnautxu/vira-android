package cat.vira.android.features.focus

import cat.vira.android.core.data.model.FocusKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/// Motor pur del temporitzador (`FocusTimerStateTests` de l'iPhone): tot pren `now` per paràmetre,
/// així que cap prova espera de veritat.
class FocusTimerStateTest {

    private val t0 = Instant.ofEpochSecond(1_000_000)
    private fun at(seconds: Long) = t0.plusSeconds(seconds)

    @Test fun remainingALIniciEsElTempsPlanificat() {
        val state = FocusTimerState(plannedSeconds = 25 * 60).start(t0)
        assertEquals(25 * 60.0, state.remaining(t0), 0.0)
    }

    @Test fun remainingALaMeitatEsLaMeitat() {
        val state = FocusTimerState(plannedSeconds = 1000).start(t0)
        assertEquals(500.0, state.remaining(at(500)), 0.0)
    }

    @Test fun remainingMaiBaixaDeZero() {
        val state = FocusTimerState(plannedSeconds = 100).start(t0)
        assertEquals(0.0, state.remaining(at(500)), 0.0)
    }

    @Test fun progressQuedaEntreZeroIU() {
        var state = FocusTimerState(plannedSeconds = 100)
        assertEquals(0.0, state.progress(t0), 0.0)
        state = state.start(t0)
        assertEquals(1.0, state.progress(at(1000)), 0.0)
    }

    @Test fun laPausaCongelaElTempsRestant() {
        val state = FocusTimerState(plannedSeconds = 1000).start(t0).pause(at(200))
        assertEquals(state.remaining(at(200)), state.remaining(at(9000)), 0.0)
    }

    @Test fun laRepresaDesplacaElFinalElTempsPausat() {
        val state = FocusTimerState(plannedSeconds = 1000).start(t0).pause(at(200)).resume(at(9200))
        assertEquals(at(9200 + 800), state.endDate)
    }

    @Test fun pausaRepresaIPausaAcumulenBe() {
        val state = FocusTimerState(plannedSeconds = 1000).start(t0).pause(at(100)).resume(at(500)).pause(at(600))
        assertEquals(200.0, state.elapsed(at(9999)), 0.0)
    }

    @Test fun isFinishedEsExacteAlFinal() {
        val state = FocusTimerState(plannedSeconds = 100).start(t0)
        assertFalse(state.isFinished(at(99)))
        assertTrue(state.isFinished(at(100)))
    }

    @Test fun triarCanviaElTempsMostrat() {
        val state = FocusTimerState(kind = FocusKind.FOCUS, plannedSeconds = 25 * 60).select(FocusKind.SHORT_BREAK, 5 * 60)
        assertEquals(FocusKind.SHORT_BREAK, state.kind)
        assertEquals(5 * 60, state.plannedSeconds)
        assertEquals(5 * 60.0, state.remaining(t0), 0.0)
    }

    @Test fun triarUnCopComencadaNoFaRes() {
        val state = FocusTimerState(kind = FocusKind.FOCUS, plannedSeconds = 25 * 60).start(t0).select(FocusKind.SHORT_BREAK, 5 * 60)
        assertEquals(FocusKind.FOCUS, state.kind)
        assertEquals(25 * 60, state.plannedSeconds)
    }

    @Test fun comencarIAturarDeSeguidaNoEsAcabat() {
        val state = FocusTimerState(plannedSeconds = 100).start(t0).finish(at(1))
        assertTrue(state.elapsed(at(1)) >= 0)
        assertEquals(FocusTimerState.Run.FINISHED, state.run)
        assertFalse(state.isFinished(at(1)))
    }

    @Test fun elsMinutsRestantsArrodoneixenAmunt() {
        val state = FocusTimerState(plannedSeconds = 25 * 60).start(t0)
        assertEquals(25, state.remainingMinutesText(t0))
        assertEquals(25, state.remainingMinutesText(at(1)))
        assertEquals(1, state.remainingMinutesText(at(25 * 60 - 1)))
    }
}
