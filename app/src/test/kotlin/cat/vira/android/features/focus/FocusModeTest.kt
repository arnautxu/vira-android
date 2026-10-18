package cat.vira.android.features.focus

import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/// Fusió dels modes integrats amb els perfils (`FocusModeTests` de l'iPhone). El que es protegeix és
/// que la identitat d'un mode **no** és `FocusKind`: si algú les torna a lligar, aquestes proves cauen.
class FocusModeTest {

    private fun profile(name: String, focus: Int = 25, rest: Int = 5) =
        FocusProfile(name = name, focusMinutes = focus, breakMinutes = rest)

    @Test fun sensePerfilsSonElsTresIntegrats() {
        val modes = FocusMode.all(emptyList())
        assertEquals(listOf(FocusKind.FOCUS, FocusKind.SHORT_BREAK, FocusKind.LONG_BREAK), modes.map { it.kind })
        assertTrue(modes.all { it.profileID == null })
    }

    @Test fun elsPerfilsVanDespresDelsIntegratsEnOrdre() {
        val first = profile("Estudi")
        val second = profile("Dibuixar")
        val modes = FocusMode.all(listOf(first, second))
        assertEquals(5, modes.size)
        assertEquals(FocusModeID.BuiltIn(FocusKind.FOCUS), modes[0].id)
        assertEquals(FocusModeID.Profile(first.id), modes[3].id)
        assertEquals(FocusModeID.Profile(second.id), modes[4].id)
    }

    @Test fun cadaEntradaTeUnaIdentitatUnica() {
        val modes = FocusMode.all(listOf(profile("Estudi"), profile("Dibuixar")))
        assertEquals(modes.size, modes.map { it.id }.toSet().size)
    }

    @Test fun dosPerfilsAmbElMateixNomSonModesDiferents() {
        val modes = FocusMode.all(listOf(profile("Estudi"), profile("Estudi")))
        assertNotEquals(modes[3].id, modes[4].id)
        assertEquals(modes[3].kind, modes[4].kind)
    }

    @Test fun unPerfilSempreEsDeConcentracio() {
        assertEquals(FocusKind.FOCUS, FocusMode.of(profile("Estudi")).kind)
    }

    @Test fun elNomDUnPerfilEsElDeLUsuari() {
        assertEquals("Estudi de mates", FocusMode.of(profile("Estudi de mates")).name)
        assertNull(FocusMode.of(profile("Estudi de mates")).labelRes)
    }

    @Test fun laDuradaPerDefecteDUnPerfilEsLaSeva() {
        val mode = FocusMode.of(profile("Estudi", focus = 45))
        assertEquals(45, mode.defaultMinutes)
        assertEquals(45 * 60, mode.defaultSeconds)
    }

    @Test fun elsIntegratsProposenCincIQuinze() {
        val focus = FocusMode.builtIns()[0]
        assertEquals(5, focus.breakMinutes(FocusKind.SHORT_BREAK))
        assertEquals(15, focus.breakMinutes(FocusKind.LONG_BREAK))
    }

    @Test fun unPerfilTeUnSolDescans() {
        val mode = FocusMode.of(profile("Estudi", focus = 45, rest = 10))
        assertEquals(10, mode.breakMinutes(FocusKind.SHORT_BREAK))
        assertEquals(10, mode.breakMinutes(FocusKind.LONG_BREAK))
    }

    @Test fun unPerfilTeUnaSolaDuradaISensePas() {
        val mode = FocusMode.of(profile("Estudi", focus = 45))
        assertEquals(listOf(45), mode.durations)
        assertNull(mode.minuteRange)
        assertTrue(FocusMode.builtIns()[0].durations.size > 1)
    }

    @Test fun ambTrentaModesSonTresMesTrenta() {
        val modes = FocusMode.all((1..30).map { profile("Mode $it") })
        assertEquals(33, modes.size)
        assertEquals(33, modes.map { it.id }.toSet().size)
        assertTrue(modes.take(3).all { it.profileID == null })
        assertTrue(modes.drop(3).all { it.profileID != null })
    }

    @Test fun ambUnSolModeVaDespresDelsIntegrats() {
        val modes = FocusMode.all(listOf(profile("Estudi")))
        assertEquals(4, modes.size)
        assertEquals("Estudi", modes.last().name)
    }

    @Test fun laIdentitatEsPerIdNoPerValors() {
        val original = profile("Estudi")
        assertEquals(FocusMode.of(original), FocusMode.of(original.copy(name = "Un altre nom")))
    }

    @Test fun unModeNouSurtAmbVinticincICinc() {
        val profile = FocusProfile(name = "Estudi")
        assertEquals(25, profile.focusMinutes)
        assertEquals(5, profile.breakMinutes)
        assertTrue(profile.showsLinkedTask)
        assertEquals(false, profile.muteViraNotifications)
        assertNull(profile.ambientSound)
    }

    @Test fun laSessioEsQuedaElNomDelModeNoUnaReferencia() {
        val profile = profile("Estudi")
        val mode = FocusMode.of(profile)
        val session = FocusSession(plannedSeconds = mode.defaultSeconds, kindRaw = mode.kind.raw, profileID = mode.profileID, profileName = mode.name)
        profile.copy(name = "Un altre nom")
        assertEquals("Estudi", session.profileName)
        assertEquals(profile.id, session.profileID)
    }
}
