package cat.vira.android.features.today

import cat.vira.android.R
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// L'ordre de l'agenda d'Avui i el calendari del sistema a dins. Calca `AgendaOrderTests` i
/// `AgendaCalendarTests` de l'iPhone.
class AgendaTest {

    private val zone = ZoneId.of("Europe/Madrid")

    /// Dilluns 3 d'agost del 2026.
    private val day: Instant = LocalDate.of(2026, 8, 3).atStartOfDay(zone).toInstant()

    private fun time(hour: Int): Instant = day.atZone(zone).withHour(hour).toInstant()

    private fun item(title: String, hour: Int? = null, done: Boolean = false, sortIndex: Int = 0) = PlanItem(
        title = title,
        day = day,
        time = hour?.let(::time),
        statusRaw = if (done) PlanItemStatus.DONE.raw else PlanItemStatus.PENDING.raw,
        sortIndex = sortIndex,
    )

    private fun agenda(items: List<PlanItem>, medications: List<Medication> = emptyList(), events: List<TodayCalendarEvent> = emptyList()) =
        Agenda(items, medications, emptyList(), day, zone, events)

    /// Un instant abans del dia, perquè cap ítem caigui a «ara» i l'ordre sigui només el del dia.
    private val early: Instant = day.minusSeconds(86_400)

    // Ordre de l'agenda

    @Test
    fun lOrdreManualDelCalendariEsNotaAAvui() {
        // El que deixa `PlanItemOrder.move` en passar «Dutxa» davant d'«Institut».
        val first = item("Institut", hour = 8, sortIndex = (9 * 60 + 1) * 100)
        val second = item("Dutxa", hour = 9, sortIndex = (8 * 60 + 1) * 100)
        assertEquals(listOf("Dutxa", "Institut"), agenda(listOf(first, second)).entries.map { it.title })
    }

    @Test
    fun unaPresaEsSituaPerLaSevaHoraEntreElsItemsDelPla() {
        val medication = Medication(name = "Elvanse", dose = "30 mg", formRaw = MedicationForm.CAPSULE.raw, timesInMinutes = listOf(9 * 60))
        val result = agenda(listOf(item("Institut", hour = 8), item("Dinar", hour = 14)), listOf(medication))
        assertEquals(listOf("Institut", "Elvanse", "Dinar"), result.entries.map { it.title })
    }

    @Test
    fun unaTascaSenseHoraNoCauAlFinalDelDia() {
        val result = agenda(listOf(item("Sopar", hour = 21), item("Trucar a l'àvia")))
        assertEquals(listOf("Trucar a l'àvia", "Sopar"), result.entries.map { it.title })
    }

    @Test
    fun unItemMarcatBaixaAlFinalSenseFinestraOberta() {
        val result = agenda(listOf(item("Institut", hour = 8, done = true), item("Dutxa", hour = 9)))
        assertEquals(listOf("Dutxa", "Institut"), result.ordered(holding = null, now = early).map { it.title })
    }

    @Test
    fun mentreDuraElDesferLItemMarcatNoEsMou() {
        val first = item("Institut", hour = 8, done = true)
        val result = agenda(listOf(first, item("Dutxa", hour = 9)))
        assertEquals(listOf("Institut", "Dutxa"), result.ordered(holding = "plan-${first.id}", now = early).map { it.title })
    }

    @Test
    fun retenirUnItemNoArrossegaElsAltresFets() {
        val first = item("Institut", hour = 8, done = true)
        val result = agenda(listOf(first, item("Dutxa", hour = 9), item("Sopar", hour = 21, done = true)))
        assertEquals(
            listOf("Institut", "Dutxa", "Sopar"),
            result.ordered(holding = "plan-${first.id}", now = early).map { it.title },
        )
    }

    @Test
    fun unIdentificadorQueJaNoHiEsNoAlteraLOrdre() {
        val result = agenda(listOf(item("Institut", hour = 8, done = true), item("Dutxa", hour = 9)))
        assertEquals(listOf("Dutxa", "Institut"), result.ordered(holding = "plan-inexistent", now = early).map { it.title })
    }

    // Calendari del sistema

    private fun event(title: String, hour: Int) = TodayCalendarEvent(id = title, title = title, start = time(hour), calendarName = "Institut")

    @Test
    fun unEsdevenimentExternSOrdenaPerHoraAmbLaResta() {
        val result = agenda(listOf(item("Deures", hour = 17)), events = listOf(event("Examen", 9)))
        assertEquals(listOf("Examen", "Deures"), result.entries.map { it.title })
    }

    @Test
    fun unEsdevenimentExternNoEsPotEditar() {
        val entry = agenda(emptyList(), events = listOf(event("Examen", 9))).entries.single()
        assertTrue(entry.isReadOnly)
        assertNull(entry.planItem)
        assertFalse(entry.isDone)
    }

    @Test
    fun unEsdevenimentExternDiuDOnVe() {
        val entry = agenda(emptyList(), events = listOf(event("Examen", 9))).entries.single()
        assertEquals("Institut", entry.subtitle)
        assertEquals(R.string.type_calendar, entry.typeLabel)
    }

    @Test
    fun senseCalendariLAgendaEsLaDAbans() {
        val result = agenda(listOf(item("Deures", hour = 17)))
        assertEquals(1, result.entries.size)
        assertFalse(result.entries.single().isReadOnly)
    }

    // Preses

    @Test
    fun unaPautaQueNoTocaAquellDiaNoGeneraCapPresa() {
        // Dilluns és 2 a la numeració de l'iPhone; la pauta només és de diumenge (1).
        val medication = Medication(name = "Elvanse", timesInMinutes = listOf(9 * 60), weekdays = listOf(1))
        assertTrue(agenda(emptyList(), listOf(medication)).isEmpty)
    }

    @Test
    fun unEsdevenimentQueEsLaMateixaTascaNoSurtDuesVegades() {
        // El que passa en enviar una tasca al calendari: torna com a esdeveniment.
        val result = agenda(listOf(item("Agafar cartera", hour = 7)), events = listOf(event(" agafar CARTERA ", 7), event("Institut", 8)))
        assertEquals(listOf("Agafar cartera", "Institut"), result.entries.map { it.title })
        assertTrue(result.entries.first().kind is AgendaEntry.Kind.Plan)
    }

    @Test
    fun laClauIgnoraAccentsIMajusculesPeroNoLHora() {
        assertEquals(Agenda.matchKey("Música", time(7)), Agenda.matchKey(" musica ", time(7)))
        assertFalse(Agenda.matchKey("Música", time(7)) == Agenda.matchKey("Música", time(8)))
    }
}
