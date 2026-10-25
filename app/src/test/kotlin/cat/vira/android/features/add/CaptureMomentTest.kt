package cat.vira.android.features.add

import cat.vira.android.features.plan.DayPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/// Els moments d'un toc i el pas «Quan» (`QuickCaptureMomentTests` de l'iPhone, Arnau, 2026-10-24).
class CaptureMomentTest {

    /// Dimarts 20 d'octubre de 2026 a l'hora donada.
    private fun at(hour: Int, minute: Int = 0, day: Int = 20) = LocalDateTime.of(2026, 10, day, hour, minute)
    private fun moment(offset: Int, period: DayPeriod) = CaptureMoment(offset, period)

    @Test
    fun aPrimeraHoraHiEsTotAvuiITotDema() {
        assertEquals(
            listOf(moment(0, DayPeriod.Morning), moment(0, DayPeriod.Midday), moment(0, DayPeriod.Night),
                moment(1, DayPeriod.Morning), moment(1, DayPeriod.Midday), moment(1, DayPeriod.Night)),
            CaptureMoment.upcoming(at(7)),
        )
    }

    @Test
    fun mitjaHoraAbansJaNoEsUnMoment() {
        assertFalse(moment(0, DayPeriod.Morning) in CaptureMoment.upcoming(at(8, 31)))
        assertTrue(moment(0, DayPeriod.Morning) in CaptureMoment.upcoming(at(8, 30)))
    }

    @Test
    fun aLaNitNomesQuedaDema() {
        assertEquals(DayPeriod.entries.map { moment(1, it) }, CaptureMoment.upcoming(at(22)))
    }

    @Test
    fun senseHistorialElsTresMesPropers() {
        assertEquals(
            listOf(moment(0, DayPeriod.Night), moment(1, DayPeriod.Morning), moment(1, DayPeriod.Midday)),
            CaptureMoment.suggestions(at(16), emptyList()),
        )
    }

    @Test
    fun unMomentTriatDuesVegadesPassaAlDavant() {
        val history = listOf(moment(1, DayPeriod.Night), moment(1, DayPeriod.Night), moment(1, DayPeriod.Midday))
        assertEquals(
            listOf(moment(1, DayPeriod.Night), moment(0, DayPeriod.Night), moment(1, DayPeriod.Morning)),
            CaptureMoment.suggestions(at(16), history),
        )
    }

    @Test
    fun delsApresosElMesTriatPrimer() {
        val history = listOf(moment(1, DayPeriod.Morning), moment(1, DayPeriod.Morning)) + List(3) { moment(0, DayPeriod.Night) }
        assertEquals(listOf(moment(0, DayPeriod.Night), moment(1, DayPeriod.Morning)), CaptureMoment.suggestions(at(10), history).take(2))
    }

    @Test
    fun unApresQueJaHaPassatNoSurt() {
        val history = listOf(moment(0, DayPeriod.Morning), moment(0, DayPeriod.Morning))
        assertFalse(moment(0, DayPeriod.Morning) in CaptureMoment.suggestions(at(16), history))
    }

    @Test
    fun siLaFraseDiuElDiaLesFrangesDAquellDia() {
        assertEquals(DayPeriod.entries.map { moment(2, it) }, CaptureMoment.suggestions(at(16), emptyList(), fixedDayOffset = 2))
        assertEquals(listOf(moment(0, DayPeriod.Night)), CaptureMoment.suggestions(at(16), emptyList(), fixedDayOffset = 0))
    }

    @Test
    fun dOnSurtLHistorial() {
        val created = LocalDate.of(2026, 10, 20)
        assertEquals(moment(1, DayPeriod.Midday), CaptureMoment.of(LocalDate.of(2026, 10, 21), at(18, 30, day = 21), created))
        assertNull(CaptureMoment.of(LocalDate.of(2026, 10, 21), null, created))
        assertNull(CaptureMoment.of(LocalDate.of(2026, 10, 23), at(9, day = 23), created))
        assertEquals(at(21, day = 21), moment(1, DayPeriod.Night).resolved(created))
    }

    // El pas «Quan» (`QuickCaptureForm+When.swift`)

    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun unMomentDesaElDiaILaFranja() {
        val state = TaskAddState(today = today)
        state.updateTitle("Deures")
        state.pick(moment(1, DayPeriod.Night))
        assertEquals(today.plusDays(1), state.day)
        assertEquals(LocalTime.of(21, 0), state.time)
        assertEquals("Deures", state.titleToSave)
    }

    @Test
    fun siLaFraseJaDeiaElDiaElTitolEsDesaNet() {
        val state = TaskAddState(today = today)
        state.updateTitle("Dentista demà")
        state.pick(moment(1, DayPeriod.Morning))
        assertEquals("Dentista", state.titleToSave)
        assertEquals(LocalTime.of(9, 0), state.time)
    }

    @Test
    fun aUnaHoraComencaALaDeLaFranjaOALaSeguentMitjaHora() {
        val night = TaskAddState(today = today)
        night.choose(AddTime.Period(DayPeriod.Night))
        night.pickTime()
        assertEquals(LocalTime.of(21, 0), night.time)

        val soon = TaskAddState(today = today, now = LocalTime.of(16, 10))
        soon.pickTime()
        assertEquals(today, soon.day)
        assertEquals(LocalTime.of(17, 0), soon.time)

        val other = TaskAddState(today = today)
        other.choose(today.plusDays(3))
        other.pickTime()
        assertEquals(LocalTime.of(17, 0), other.time)
    }

    @Test
    fun tardAlVespreLaMitjaHoraSeguentJaEsDema() {
        val late = TaskAddState(today = today, now = LocalTime.of(23, 40))
        late.pickTime()
        assertEquals(today.plusDays(1), late.day)
        assertEquals(LocalTime.of(0, 30), late.time)
    }

    @Test
    fun lesRodesManenPerSobreDeLaFrase() {
        val state = TaskAddState(today = today)
        state.updateTitle("Dentista demà a les 17:30")
        state.setTime(18 * 60 + 15)
        assertEquals(LocalTime.of(18, 15), state.time)
        // La frase ja no es pot desar neta: se n'ha deixat l'hora.
        assertEquals("Dentista demà a les 17:30", state.titleToSave)
        state.pickNoTime()
        assertNull(state.time)
    }

    @Test
    fun senseResTriatDesaAvuiISenseHora() {
        val state = TaskAddState(today = today)
        state.updateTitle("Deures")
        assertEquals(today, state.day)
        assertNull(state.time)
        assertFalse(state.isDayChosen)
    }

    @Test
    fun ambLHoraDitaNoSurtenMoments() {
        val state = TaskAddState(today = today)
        state.updateTitle("Trucar a les 17:30")
        state.loadWhen(emptyMap(), emptyList(), at(9))
        assertTrue(state.moments.isEmpty())
    }

    @Test
    fun elCalendariNoEnsenyaElsDiesPassats() {
        val weeks = CalendarMonth.weeks(YearMonth.of(2026, 10), today, DayOfWeek.MONDAY)
        // Dimarts 20: la setmana d'avui comença amb dilluns en blanc, i les anteriors no hi són.
        assertEquals(listOf(null, today), weeks.first().take(2))
        assertEquals(LocalDate.of(2026, 10, 31), weeks.last()[5])
        assertEquals(2, weeks.size)
        // Un mes que ve, sencer.
        assertEquals(6, CalendarMonth.weeks(YearMonth.of(2026, 11), today, DayOfWeek.MONDAY).size)
    }
}
