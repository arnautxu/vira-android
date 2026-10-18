package cat.vira.android.features.parent

import cat.vira.android.core.backend.ViraBackendConfig
import cat.vira.android.core.backend.ViraRestClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

/// El panell de pares, per la banda del client (`ViraParentLinkTests` de l'iPhone).
///
/// El que decideix qui veu què són les polítiques RLS de `0003_parent_link.sql`, i això no es pot
/// provar des d'aquí. El que sí que es pot provar és que el client no faci trampes: que no enviï cap
/// `parent_user_id`, que revocar tanqui tots els àmbits, i que un codi dolent no es converteixi en
/// un vincle.
class ParentLinkClientTest {

    private val server = MockWebServer()
    private lateinit var client: ParentLinkClient

    @Before
    fun setUp() {
        server.start()
        val config = ViraBackendConfig.Values(server.url("/").toString().trimEnd('/'), "anon")
        client = ParentLinkClient(ViraRestClient(config, OkHttpClient()))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun link(scopes: Boolean = true) = ParentLink(
        id = UUID.randomUUID(), childUserId = UUID.randomUUID(), parentUserId = UUID.randomUUID(),
        scopeUsage = scopes, scopeHabits = scopes, scopeEmotions = scopes, scopeMedication = scopes,
        createdAt = Instant.parse("2026-10-01T10:00:00Z"),
    )

    /// Els resolts no compten per al límit: un pendent vell no pot quedar tapat.
    @Test
    fun elsRecordatorisEsFiltrenPerPendentsAlServidor() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        client.reminders("token")
        val url = server.takeRequest().requestUrl!!
        assertEquals("is.null", url.queryParameter("accepted_at"))
        assertEquals("is.null", url.queryParameter("dismissed_at"))
    }

    @Test
    fun trencarElVincleTancaElsQuatreAmbitsNoNomesElVincle() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))
        client.revoke(link(), "token")

        val request = server.takeRequest()
        assertEquals("/rest/v1/parent_links", request.requestUrl!!.encodedPath)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"scope_emotions\":false"))
        assertTrue(body.contains("\"scope_medication\":false"))
        assertTrue(body.contains("\"scope_usage\":false"))
        assertTrue(body.contains("\"scope_habits\":false"))
        assertTrue(Regex("\"revoked_at\":\"[^\"]+\"").containsMatchIn(body))
    }

    @Test
    fun unMissatgeNoDiuDeQuiEsAixoHoPosaElServidor() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))
        client.send("Ànims!", UUID.randomUUID(), "token")

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("child_user_id"))
        assertFalse(body.contains("parent_user_id"))
    }

    @Test
    fun unRecordatoriProposatNeixSenseAcceptarNiDescartar() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))
        client.propose("Dutxa't", null, UUID.randomUUID(), "token")

        val body = server.takeRequest().body.readUtf8()
        assertFalse(body.contains("accepted_at"))
        assertFalse(body.contains("dismissed_at"))
        assertFalse(body.contains("parent_user_id"))
    }

    @Test
    fun unCodiQueElServidorNoReconeixNoCreaCapVincle() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"codi no vàlid"}"""))
        try {
            client.redeem("000000", "token")
            fail("Un codi rebutjat no pot tornar cap vincle")
        } catch (_: Exception) {
        }
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"invite_code\":\"000000\""))
    }

    @Test
    fun unaRespostaQueNoEsUnIdentificadorNoEsUnVincle() = runBlocking {
        server.enqueue(MockResponse().setBody("null"))
        try {
            client.redeem("123456", "token")
            fail("Sense identificador no hi ha vincle")
        } catch (_: Exception) {
        }
    }

    @Test
    fun elCodiNouEsLlegeixTalQual() = runBlocking {
        server.enqueue(MockResponse().setBody("\"482913\""))
        assertEquals("482913", client.createInvite("token"))
    }

    @Test
    fun elsVinclesEsLlegeixenAmbElsNomsDelServidor() = runBlocking {
        val child = UUID.randomUUID()
        server.enqueue(MockResponse().setBody(
            """[{"id":"${UUID.randomUUID()}","child_user_id":"$child","parent_user_id":"${UUID.randomUUID()}",
            "scope_usage":true,"scope_habits":false,"scope_emotions":false,"scope_medication":false,
            "created_at":"2026-10-01T10:00:00.123456+00:00","revoked_at":null}]""",
        ))
        val links = client.links("token")
        assertEquals(1, links.size)
        assertEquals(child, links.first().childUserId)
        assertTrue(links.first().isActive)
        assertFalse(ParentScope.EMOTIONS.isOpen(links.first()))
    }

    @Test
    fun unVincleRevocatDeixaDeComptarComAActiu() {
        assertTrue(link().isActive)
        assertFalse(link().copy(revokedAt = Instant.now()).isActive)
    }

    @Test
    fun unRecordatoriResoltDeixaDEstarPendent() {
        val reminder = ParentReminder(
            id = UUID.randomUUID(), childUserId = UUID.randomUUID(), parentUserId = UUID.randomUUID(),
            title = "Beure aigua", createdAt = Instant.now(),
        )
        assertTrue(reminder.isPending)
        assertFalse(reminder.copy(acceptedAt = Instant.now()).isPending)
        assertFalse(reminder.copy(dismissedAt = Instant.now()).isPending)
    }

    @Test
    fun unEscalarNullEsNullMaiZero() {
        assertNull(ParentLinkClient.scalar("null"))
        assertNull(ParentLinkClient.scalar(""))
        assertEquals("0", ParentLinkClient.scalar("0"))
        assertEquals("7", ParentLinkClient.scalar("7"))
        assertEquals("A1B2C3", ParentLinkClient.scalar("\"A1B2C3\""))
    }

    @Test
    fun unTextBuitOMassaLlargNoEsPotEnviar() {
        assertTrue(isValidCompose("Ànims!", ParentComposeMode.MESSAGE))
        assertFalse(isValidCompose("   ", ParentComposeMode.MESSAGE))
        assertTrue(isValidCompose("a".repeat(200), ParentComposeMode.MESSAGE))
        assertFalse(isValidCompose("a".repeat(201), ParentComposeMode.MESSAGE))
        assertFalse(isValidCompose("a".repeat(121), ParentComposeMode.REMINDER))
    }
}
