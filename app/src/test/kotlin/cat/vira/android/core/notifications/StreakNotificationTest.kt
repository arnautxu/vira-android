package cat.vira.android.core.notifications

import cat.vira.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Quan surt Viri a la pantalla bloquejada i què hi diu (`StreakNotificationTests` de l'iPhone).
///
/// La regla que defensen: **mai un avís d'un dia que ja compta**.
class StreakNotificationTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val time = ViraClock(20, 30)
    private val text = ViraNotificationText { id, args -> "$id|${args.joinToString()}" }

    /// Una hora abans de l'avís, un dia qualsevol.
    private val beforeTheSlot: Instant = LocalDate.of(2027, 1, 15).atTime(19, 30).atZone(zone).toInstant()

    private fun requests(event: ViraStreakNotifications.Event?, dayIsOpen: Boolean, muted: ClosedOpenRange? = null) =
        ViraStreakNotifications.requests(beforeTheSlot, event, dayIsOpen, muted, time, zone, text)

    private fun isToday(n: ViraPlannedNotification) =
        LocalDate.ofInstant(n.fireAt, zone) == LocalDate.ofInstant(beforeTheSlot, zone)

    @Test
    fun laFinestraEsDUnAvisPerDia() {
        val slots = ViraStreakNotifications.slots(beforeTheSlot, time, zone)
        assertEquals("set dies de finestra, un avís cada dia", 7, slots.size)
        assertEquals("mai dos avisos el mateix dia", 7, slots.map { LocalDate.ofInstant(it, zone) }.toSet().size)
        assertEquals("cronològic", slots.sorted(), slots)
    }

    @Test
    fun passadaLHoraDeLAvisElDAvuiJaNoEsPrograma() {
        val after = beforeTheSlot.plusSeconds(2 * 3600)
        val slots = ViraStreakNotifications.slots(after, time, zone)
        assertTrue(slots.all { it.isAfter(after) })
        assertEquals(6, slots.size)
    }

    @Test
    fun unDiaQueJaComptaNoRepCapAvis() {
        assertTrue(
            "recordar una cosa ja feta és el retret que l'app evita",
            requests(null, dayIsOpen = false).none(::isToday),
        )
    }

    @Test
    fun unDiaObertRepLAvisDelVespre() {
        val planned = requests(null, dayIsOpen = true)
        assertTrue(planned.any(::isToday))
        assertTrue("el pressupost de 5 no es pot passar: 26 + 14 + 8 + 3 + 5 = 56 de 64", planned.size <= 5)
    }

    @Test
    fun laNoticiaVaALaPrimeraFrancaEncaraQueElDiaJaCompti() {
        val planned = requests(ViraStreakNotifications.Event.Protected, dayIsOpen = false)
        val expected = StreakNotificationCatalog.body(
            StreakNotificationCatalog.Kind.Protected, LocalDate.ofInstant(beforeTheSlot, zone), text,
        )
        assertEquals(expected, planned.first().body)
        assertTrue("la ratxa salvada es diu avui", isToday(planned.first()))
        assertTrue("una sola vegada", planned.drop(1).none { it.body == expected })
    }

    @Test
    fun laFitaPortaElsDiesINoEsRepeteix() {
        val planned = requests(ViraStreakNotifications.Event.Milestone(30), dayIsOpen = true)
        assertTrue(planned.first().body.endsWith("|30"))
        assertTrue(planned.drop(1).none { it.body.endsWith("|30") })
    }

    @Test
    fun unaSessioQueSilenciaViriEsMenjaLAvisDeDins() {
        val first = ViraStreakNotifications.slots(beforeTheSlot, time, zone).first()
        val muted = ClosedOpenRange(first.minusSeconds(60), first.plusSeconds(60))
        val planned = requests(null, dayIsOpen = true, muted = muted)
        assertFalse(planned.any(::isToday))
        assertTrue(planned.isNotEmpty())
        assertTrue(planned.none { muted.contains(it.fireAt) })
    }

    @Test
    fun capAvisDiuElTitolDeResNomesLaMarca() {
        val title = text.get(R.string.notification_generic_title)
        assertTrue(requests(ViraStreakNotifications.Event.Reset, dayIsOpen = true).all { it.title == title })
    }

    @Test
    fun laFraseEsLaMateixaTotElDiaICanviaLEndema() {
        val day = LocalDate.of(2027, 1, 15)
        val a = StreakNotificationCatalog.body(StreakNotificationCatalog.Kind.Nudge, day, text)
        assertEquals(a, StreakNotificationCatalog.body(StreakNotificationCatalog.Kind.Nudge, day, text))
        assertTrue(a != StreakNotificationCatalog.body(StreakNotificationCatalog.Kind.Nudge, day.plusDays(1), text))
        assertEquals(1L, StreakNotificationCatalog.dayOrdinal(LocalDate.of(1, 1, 1)))
    }
}
