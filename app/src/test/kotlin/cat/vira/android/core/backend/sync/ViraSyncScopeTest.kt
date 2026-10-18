package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.ViraConsentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/// Les combinacions de `ViraSyncScope`: emocions i medicació només surten amb l'interruptor **i**
/// el permís del tutor.
class ViraSyncScopeTest {

    @Test
    fun senseSessioNoPujaRes() {
        val scope = ViraSyncScope(isSignedIn = false, syncEmotions = true, syncMedication = true, consent = ViraConsentStatus.GRANTED)
        ViraSyncLayer.entries.forEach { assertEquals(false, scope.allows(it)) }
    }

    @Test
    fun ambSessioElNucliSempreHiEs() {
        for (consent in ViraConsentStatus.entries) {
            val scope = ViraSyncScope(isSignedIn = true, syncEmotions = false, syncMedication = false, consent = consent)
            assertEquals(true, scope.allows(ViraSyncLayer.CORE))
        }
    }

    @Test
    fun lesCapesSensiblesDemanenInterruptorIPermis() {
        for (consent in ViraConsentStatus.entries) for (emotions in listOf(true, false)) for (medication in listOf(true, false)) {
            val scope = ViraSyncScope(isSignedIn = true, syncEmotions = emotions, syncMedication = medication, consent = consent)
            val granted = consent == ViraConsentStatus.GRANTED
            assertEquals("emocions $emotions/$consent", emotions && granted, scope.allows(ViraSyncLayer.EMOTIONS))
            assertEquals("medicació $medication/$consent", medication && granted, scope.allows(ViraSyncLayer.MEDICATION))
        }
    }

    @Test
    fun unPermisPendentNoCompta() {
        val scope = ViraSyncScope(isSignedIn = true, syncEmotions = true, syncMedication = true, consent = ViraConsentStatus.PENDING)
        assertEquals(false, scope.allows(ViraSyncLayer.EMOTIONS))
        assertEquals(false, scope.allows(ViraSyncLayer.MEDICATION))
    }
}
