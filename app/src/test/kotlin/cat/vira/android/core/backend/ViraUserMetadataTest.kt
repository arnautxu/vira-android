package cat.vira.android.core.backend

import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/// Les metadades de l'alta porten l'idioma per als correus de Supabase (`ViraAuthClientTests` de
/// l'iPhone, 2026-10-21).
class ViraUserMetadataTest {

    @Test
    fun `l'alta porta el rol, el permis i l'idioma`() {
        val json = ViraUserMetadata(ViraAccountRole.PARENT, ViraConsentStatus.GRANTED).signUpJson("es")
        assertEquals("parent", json.getValue("role").jsonPrimitive.content)
        assertEquals("granted", json.getValue("consent").jsonPrimitive.content)
        assertEquals("es", json.getValue("locale").jsonPrimitive.content)
    }
}
