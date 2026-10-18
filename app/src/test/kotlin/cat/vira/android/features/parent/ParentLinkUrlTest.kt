package cat.vira.android.features.parent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/// Els enllaços de vinculació (`ViraUniversalLinkTests` de l'iPhone).
///
/// Es prova perquè aquí es barregen dues formes d'URL amb estructures diferents —a
/// `vira://link/CODI` el segment `link` és l'`host`, i a `https://vira.cat/link/CODI` és part del
/// camí— i perquè el mateix punt d'entrada rep també els enllaços d'auth, que **no** ha de tocar.
class ParentLinkUrlTest {

    @Test
    fun lEnllacQueSEnviaPortaElCodi() = assertEquals("A1B2C3", ParentLinkUrl.code("https://vira.cat/link/A1B2C3"))

    @Test
    fun lEsquemaPropiTambe() = assertEquals("A1B2C3", ParentLinkUrl.code("vira://link/A1B2C3"))

    @Test
    fun elCodiEsNormalitzaAMajuscules() = assertEquals("A1B2C3", ParentLinkUrl.code("https://vira.cat/link/a1b2c3"))

    /// El cas que trencaria l'auth si es fes malament: si aquest en tornés un codi, la tornada de
    /// Supabase no arribaria mai a `ViraSession.handleCallback`.
    @Test
    fun unEnllacDAuthNoEsConfonAmbUnaVinculacio() = assertNull(ParentLinkUrl.code("vira://auth-callback?code=abc123"))

    @Test
    fun unAltreDominiNoCompta() = assertNull(ParentLinkUrl.code("https://exemple.com/link/A1B2C3"))

    @Test
    fun senseCodiDarrereNoHiHaCodi() {
        assertNull(ParentLinkUrl.code("https://vira.cat/link/"))
        assertNull(ParentLinkUrl.code("https://vira.cat/"))
    }

    @Test
    fun unaInvitacioDAmistatNoEsUnaVinculacio() = assertNull(ParentLinkUrl.code("https://vira.cat/friend/A1B2C3"))

    @Test
    fun lEnllacQueEsComparteixEsConstrueixIgualQueEsLlegeix() {
        val url = ParentLinkUrl.url("a1b2c3")
        assertEquals("https://vira.cat/link/A1B2C3", url)
        assertEquals("A1B2C3", ParentLinkUrl.code(url))
    }
}
