package cat.vira.android.features.progress

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.Streak
import cat.vira.android.features.progress.logic.StreakRules
import cat.vira.android.features.progress.logic.StreakRules.ReconcileOutcome
import cat.vira.android.features.progress.logic.ViraDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// `ViraStreakTests` de l'iPhone, la part de la mecànica (les tres condicions del dia són a
/// `ViraProgressTest`).
///
/// El que es protegeix aquí no és aritmètica: és que **res no penalitzi mai**.
class StreakRulesTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val days = ViraDays(zone, DayOfWeek.MONDAY)
    /// 15 de gener: lluny del canvi de mes.
    private val day0 = LocalDate.of(2026, 1, 15)

    private fun day(offset: Long): Instant = day0.plusDays(offset).atTime(10, 0).atZone(zone).toInstant()

    /// Una ratxa nova, com la crea `ViraStore.streak()`: el mes d'avui ja repartit.
    private fun fresh(freezes: Int = Streak.MONTHLY_FREE_GRANT) =
        Streak(freezesAvailable = freezes, lastGrantMonth = ViraStore.monthKey(day(0), zone))

    private fun Streak.count(offset: Long) = StreakRules.countDay(this, day(offset), days).streak

    @Test fun elPrimerDiaPosaLaRatxaA1() {
        val s = fresh().count(0)
        assertEquals(1, s.currentDays)
        assertEquals(1, s.bestDays)
    }

    @Test fun registrarDeuVegadesElMateixDiaComptaUnDia() {
        var s = fresh()
        repeat(10) { s = s.count(0) }
        assertEquals(1, s.currentDays)
        assertEquals(1, s.activeDays.size)
    }

    @Test fun diesConsecutiusSumen() {
        var s = fresh()
        for (i in 0L until 5L) s = s.count(i)
        assertEquals(5, s.currentDays)
    }

    @Test fun unDiaPerdutGastaUnDiaGuardatILaRatxaEsManté() {
        val s = fresh().count(0).count(1)
        val after = s.count(3)
        assertEquals(s.freezesAvailable - 1, after.freezesAvailable)
        assertEquals(3, after.currentDays)
    }

    @Test fun senseDiesGuardatsEsReiniciaPeroElRecordEsConserva() {
        var s = fresh(freezes = 0)
        for (i in 0L until 7L) s = s.count(i)
        assertEquals(7, s.currentDays)
        s = s.count(11)
        assertEquals("es reinicia i avui ja compta", 1, s.currentDays)
        assertEquals("el rècord no baixa mai", 7, s.bestDays)
    }

    @Test fun siNoBastenNoSeNGastaCap() {
        val s = fresh(freezes = 2).count(0).count(4)
        assertEquals(2, s.freezesAvailable)
        assertEquals(1, s.currentDays)
        assertTrue(s.protectedDays.isEmpty())
        assertFalse(s.isFrozen)
    }

    @Test fun siBastenJustesCobreixenTotsElsDies() {
        val s = fresh(freezes = 2).count(0).count(3)
        assertEquals(0, s.freezesAvailable)
        assertEquals(2, s.protectedDays.size)
        assertEquals(2, s.currentDays)
        assertFalse(s.isFrozen)
        // Els coberts són els dos dies d'abans d'avui, a les 00:00 de la zona.
        assertEquals(
            setOf(day0.plusDays(1), day0.plusDays(2)),
            s.protectedDays.map(days::date).toSet(),
        )
    }

    @Test fun obrirLAppDespresDUnDiaPerdutMostraViriCongelat() {
        val (s, outcome) = StreakRules.reconcile(fresh().count(0), day(2), days)
        assertTrue(s.isFrozen)
        assertEquals(1, s.currentDays)
        assertEquals(ReconcileOutcome.Protected(1), outcome)
        // Tornar-la a posar al dia el mateix dia no cobra res més.
        val (again, second) = StreakRules.reconcile(s, day(2), days)
        assertEquals(ReconcileOutcome.Unchanged, second)
        assertEquals(s.freezesAvailable, again.freezesAvailable)
    }

    @Test fun qualsevolAccioDescongelaViri() {
        val (frozen, _) = StreakRules.reconcile(fresh().count(0), day(2), days)
        assertTrue(frozen.isFrozen)
        val s = frozen.count(2)
        assertFalse(s.isFrozen)
        assertEquals(2, s.currentDays)
        assertFalse(StreakRules.touch(frozen, day(2), days).isFrozen)
        assertEquals("descongelar no és comptar", 1, StreakRules.touch(frozen, day(2), days).currentDays)
    }

    @Test fun reiniciarAlObrirTornaElRecord() {
        val s = fresh(freezes = 0).count(0).count(1)
        val (reset, outcome) = StreakRules.reconcile(s, day(5), days)
        assertEquals(ReconcileOutcome.Reset(2), outcome)
        assertEquals(0, reset.currentDays)
        assertEquals(2, reset.bestDays)
    }

    @Test fun laReposicioMensualTeSostre() {
        val s = Streak(freezesAvailable = Streak.MAX_FREEZES, lastGrantMonth = 1)
        val next = StreakRules.countDay(s, day0.plusMonths(1).atStartOfDay(zone).toInstant(), days).streak
        assertEquals(Streak.MAX_FREEZES, next.freezesAvailable)
    }

    @Test fun laReposicioMensualDistingeixAnys() {
        val s = Streak(freezesAvailable = 0, lastGrantMonth = ViraStore.monthKey(day(-365), zone))
        assertEquals(Streak.MONTHLY_FREE_GRANT, s.count(0).freezesAvailable)
    }

    @Test fun lesFitesNomesSurtenElDiaQueSHiArribaIAmbLaRatxaVisible() {
        var s = fresh()
        val milestones = mutableListOf<Int>()
        for (i in 0L until 8L) {
            val counted = StreakRules.countDay(s, day(i), days)
            counted.milestone?.let(milestones::add)
            s = counted.streak
        }
        assertEquals(listOf(7), milestones)

        var hidden = fresh().copy(isVisible = false)
        for (i in 0L until 6L) hidden = hidden.count(i)
        assertNull(StreakRules.countDay(hidden, day(6), days).milestone)
    }

    @Test fun nomesEsPotComprarPerSotaDelSostre() {
        assertTrue(StreakRules.canBuyFreeze(fresh(freezes = 1)))
        assertFalse(StreakRules.canBuyFreeze(fresh(freezes = Streak.MAX_FREEZES)))
    }
}
