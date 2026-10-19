package cat.vira.android.features.today

import cat.vira.android.core.data.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/// Les regles de dia que fa servir Avui: rangs, repeticions i franges.
class TodayPlanRulesTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private fun day(month: Int, dayOfMonth: Int): Instant = LocalDate.of(2026, month, dayOfMonth).atStartOfDay(zone).toInstant()

    @Test
    fun elRangDeduitDeLHoraDeixaElZeroLliure() {
        assertEquals(100, TodayPlanRules.defaultRank(day(8, 3), zone))
        assertEquals((9 * 60 + 31) * 100, TodayPlanRules.defaultRank(day(8, 3).atZone(zone).withHour(9).withMinute(30).toInstant(), zone))
        assertEquals(13 * 60 * 100 - 1, TodayPlanRules.defaultRank(null, zone))
    }

    @Test
    fun lesFrangesTallenALaUnaIALesVuit() {
        assertEquals(TodayPlanRules.Period.Morning, TodayPlanRules.Period.containing(12))
        assertEquals(TodayPlanRules.Period.Midday, TodayPlanRules.Period.containing(13))
        assertEquals(TodayPlanRules.Period.Night, TodayPlanRules.Period.containing(20))
    }

    @Test
    fun lesPresesSonALHoraDeRellotgeElDiaDelCanviDHora() {
        val pill = Medication(name = "Pastilla", timesInMinutes = listOf(8 * 60, 21 * 60 + 30))
        // 29 de març (el dia té 23 hores) i 25 d'octubre (en té 25).
        for (date in listOf(day(3, 29), day(10, 25))) {
            val times = TodayPlanRules.medicationSlots(listOf(pill), emptyList(), date, zone)
                .map { it.scheduledAt.atZone(zone).toLocalTime() }
            assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(21, 30)), times)
        }
    }

    @Test
    fun unaHoraQueNoExisteElDiaQueSAvancaPassaALaSeguent() {
        // El 29 de març les 2:30 no existeixen: com a l'iPhone, la presa és a les 3:30.
        val pill = Medication(name = "Pastilla", timesInMinutes = listOf(2 * 60 + 30))
        val at = TodayPlanRules.medicationSlots(listOf(pill), emptyList(), day(3, 29), zone).single().scheduledAt
        assertEquals(LocalTime.of(3, 30), at.atZone(zone).toLocalTime())
    }
}
