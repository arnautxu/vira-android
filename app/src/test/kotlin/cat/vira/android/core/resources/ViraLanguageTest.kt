package cat.vira.android.core.resources

import org.junit.Assert.assertEquals
import org.junit.Test

/// L'anglès és l'idioma de reserva des del 2026-10-21, com a l'iPhone.
class ViraLanguageTest {

    @Test
    fun `un idioma que Vira te es queda`() {
        assertEquals("ca", ViraLanguage.resolve(listOf("ca")))
        assertEquals("es", ViraLanguage.resolve(listOf("es")))
        assertEquals("en", ViraLanguage.resolve(listOf("en")))
    }

    @Test
    fun `es tria el primer de la llista que Vira te, com fa Android amb els recursos`() {
        assertEquals("es", ViraLanguage.resolve(listOf("de", "es", "ca")))
    }

    @Test
    fun `sense cap idioma de Vira, angles`() {
        assertEquals("en", ViraLanguage.resolve(listOf("de", "fr")))
        assertEquals("en", ViraLanguage.resolve(emptyList()))
    }
}
