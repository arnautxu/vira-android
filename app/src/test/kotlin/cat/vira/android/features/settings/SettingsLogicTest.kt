package cat.vira.android.features.settings

import androidx.compose.runtime.saveable.SaverScope
import cat.vira.android.features.settings.export.ViraResourceNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test
    fun elsAvisosNoCompténCategoriesSiElMestreEstaApagat() {
        val on = SettingsTally()
        assertEquals(SettingsTally.Count(5, 5), on.count(SettingsCategory.NOTIFICATIONS))
        val off = SettingsTally(reminders = false)
        // Només queden els missatges de Viri, que van a part.
        assertEquals(SettingsTally.Count(1, 5), off.count(SettingsCategory.NOTIFICATIONS))
    }

    @Test
    fun lAppIMotivacioCompténElsSeusInterruptors() {
        val tally = SettingsTally(mindfulnessModule = false, shopVisible = false, challenges = false)
        assertEquals(SettingsTally.Count(2, 3), tally.count(SettingsCategory.APP))
        assertEquals(SettingsTally.Count(2, 4), tally.count(SettingsCategory.MOTIVATION))
        assertNull(tally.count(SettingsCategory.PRIVACY))
        assertNull(tally.count(SettingsCategory.ACCOUNT))
    }

    @Test
    fun laCercaNoDistingeixMajusculesNiAccents() {
        val keywords = "notificacions, avisos, recordatoris, hàbits"
        assertTrue(SettingsCategory.matches("HABITS", keywords))
        assertTrue(SettingsCategory.matches("  notificacio ", keywords))
        assertFalse(SettingsCategory.matches("botiga", keywords))
        assertFalse(SettingsCategory.matches("   ", keywords))
    }

    @Test
    fun laParaulaDEsborrarEsComparaSenseMajusculesNiEspais() {
        assertTrue(eraseWordMatches(" esborrar ", "ESBORRAR"))
        assertFalse(eraseWordMatches("esborra", "ESBORRAR"))
        assertFalse(eraseWordMatches("", "ESBORRAR"))
    }

    @Test
    fun elsNomsDeRecursSonElsDeLExportador() {
        assertEquals("emotion_low_energy", ViraResourceNames.of("emotion.lowEnergy"))
        assertEquals("viri_skin_astronaut", ViraResourceNames.of("viri-skin-astronaut"))
        assertEquals("assistance_case_c11", ViraResourceNames.of("assistance.case.C11"))
        assertEquals("s_default", ViraResourceNames.of("default"))
    }

    @Test
    fun nomesSAcceptaUnaPoliticaHttpsConfigurada() {
        val values = mapOf(ViraLegalDocument.PRIVACY_POLICY.key to "https://example.test/privacy")
        assertEquals("https://example.test/privacy", ViraLegalDocuments.url(ViraLegalDocument.PRIVACY_POLICY, values))
        assertNull(ViraLegalDocuments.url(ViraLegalDocument.TERMS_OF_USE, values))
    }

    @Test
    fun noSObrenEsquemesInsegursNiValorsBuits() {
        listOf("", "privacy", "http://example.test/privacy", "javascript:alert(1)").forEach { value ->
            val values = mapOf(ViraLegalDocument.PRIVACY_POLICY.key to value)
            assertNull(value, ViraLegalDocuments.url(ViraLegalDocument.PRIVACY_POLICY, values))
        }
    }

    @Test
    fun laPilaDAjustosEsDesaIEsRecuperaIgual() {
        val stack = listOf(SettingsPage.Category(SettingsCategory.APP), SettingsPage.FocusModes)
        val saved = with(SettingsPage.stackSaver) { SaverScope { true }.save(stack) }
        assertEquals(stack, SettingsPage.stackSaver.restore(saved!!))
    }

    @Test
    fun esborrarTreuTotesLesClausDAjustos() {
        val keys = SettingsKeys::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }
            .filter { it.startsWith("settings.") }
        assertEquals(keys.toSet(), SettingsKeys.erasable.toSet())
    }
}
