package cat.vira.android.features.today

import cat.vira.android.core.data.model.PlanItemRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

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
    fun cadaDiaFinsAlCapDeSetmanaAcabaDiumengeEnCatala() {
        // Dimecres 5 d'agost: dijous, divendres, dissabte i diumenge.
        val days = TodayPlanRules.additionalDays(PlanItemRecurrence.DAILY_UNTIL_WEEK_END, day(8, 5), zone, Locale.forLanguageTag("ca"))
        assertEquals(listOf(day(8, 6), day(8, 7), day(8, 8), day(8, 9)), days)
    }

    @Test
    fun cadaDiaFinsAFinalDeMesNoRepeteixElPrimer() {
        val days = TodayPlanRules.additionalDays(PlanItemRecurrence.DAILY_UNTIL_MONTH_END, day(8, 29), zone)
        assertEquals(listOf(day(8, 30), day(8, 31)), days)
    }

    @Test
    fun laSetmanalCreaLaSeguentAlTancarLActual() {
        assertEquals(day(8, 10), TodayPlanRules.nextDay(PlanItemRecurrence.WEEKLY, day(8, 3), zone))
        assertNull(TodayPlanRules.nextDay(PlanItemRecurrence.DAILY_UNTIL_WEEK_END, day(8, 3), zone))
    }

    @Test
    fun lesFrangesTallenALaUnaIALesVuit() {
        assertEquals(TodayPlanRules.Period.Morning, TodayPlanRules.Period.containing(12))
        assertEquals(TodayPlanRules.Period.Midday, TodayPlanRules.Period.containing(13))
        assertEquals(TodayPlanRules.Period.Night, TodayPlanRules.Period.containing(20))
    }
}
