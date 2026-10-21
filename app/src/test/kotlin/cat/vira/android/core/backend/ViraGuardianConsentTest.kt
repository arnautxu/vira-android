package cat.vira.android.core.backend

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

/// El permís del tutor ve del servidor (`guardian_consents`) des del 2026-10-18 (`ViraGuardianConsentTests`
/// de l'iPhone): la traducció i el que s'envia a `declare_guardian_consent`.
class ViraGuardianConsentTest {

    private val server = MockWebServer()
    private lateinit var consent: ViraGuardianConsent

    @Before
    fun setUp() {
        server.start()
        val config = ViraBackendConfig.Values(server.url("/").toString().trimEnd('/'), "anon")
        consent = ViraGuardianConsent(ViraRestClient(config, OkHttpClient()), locale = { "es" })
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun nomesGrantedINotRequiredObren() {
        assertEquals(ViraConsentStatus.GRANTED, ViraConsentStatus.fromServer("granted"))
        assertEquals(ViraConsentStatus.GRANTED, ViraConsentStatus.fromServer("not_required"))
        assertEquals(ViraConsentStatus.PENDING, ViraConsentStatus.fromServer("pending"))
        assertEquals(ViraConsentStatus.DECLINED, ViraConsentStatus.fromServer("declined"))
        assertEquals(ViraConsentStatus.MISSING, ViraConsentStatus.fromServer(null))
        assertEquals(ViraConsentStatus.MISSING, ViraConsentStatus.fromServer("GRANTED"))
        assertEquals(ViraConsentStatus.MISSING, ViraConsentStatus.fromServer(""))
    }

    @Test
    fun senseFilaEsMissing() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        assertEquals(ViraConsentStatus.MISSING, consent.fetch("token"))
    }

    @Test
    fun un16NoEnviaCapCorreuIEsQuedaAmbElQueDiuElServidor() = runBlocking {
        // Qui ja s'havia declarat menor no passa a «no cal» per dir-ho ara: mana la resposta.
        server.enqueue(MockResponse().setBody("\"pending\""))
        val result = consent.declare(ViraConsentStatus.GRANTED, "tutor@exemple.cat", "token")
        assertEquals(ViraConsentStatus.PENDING, result)

        val request = server.takeRequest()
        assertEquals("/rest/v1/rpc/declare_guardian_consent", request.requestUrl!!.encodedPath)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("not_required", body.getValue("requested").jsonPrimitive.content)
        assertFalse("guardian" in body)
    }

    @Test
    fun unMenorEnviaElCorreuDelTutor() = runBlocking {
        server.enqueue(MockResponse().setBody("\"pending\""))
        assertEquals(ViraConsentStatus.PENDING, consent.declare(ViraConsentStatus.PENDING, "tutor@exemple.cat", "token"))
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("pending", body.getValue("requested").jsonPrimitive.content)
        assertEquals("tutor@exemple.cat", body.getValue("guardian").jsonPrimitive.content)
    }

    // El correu a la persona adulta (`guardian-consent`, 2026-10-20)

    @Test
    fun demanarElPermisEnviaElCorreuIlIdioma() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"status":"pending","guardian":"ad•••@exemple.cat","sent_at":"2026-10-20T10:00:00.123+00:00",""" +
                    """"expires_at":"2026-10-27T10:00:00+00:00","can_resend":true,"sent":true}""",
            ),
        )
        val result = consent.request("adult@exemple.cat", "token")
        assertEquals(ViraConsentStatus.PENDING, result.status)
        assertEquals(ViraGuardianRequest.Delivery.SENT, result.delivery)
        assertEquals("ad•••@exemple.cat", result.guardian)
        assertEquals(java.time.Instant.parse("2026-10-27T10:00:00Z"), result.expiresAt)
        assertTrue(result.canResend)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/functions/v1/guardian-consent", request.requestUrl!!.encodedPath)
        assertEquals("Bearer token", request.getHeader("Authorization"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("adult@exemple.cat", body.getValue("guardian").jsonPrimitive.content)
        assertEquals("es", body.getValue("locale").jsonPrimitive.content)
    }

    @Test
    fun tornarAEnviarNoPortaCorreu() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"status":"pending","guardian":null,"can_resend":false,"sent":false}"""))
        val result = consent.request(null, "token")
        assertEquals(ViraGuardianRequest.Delivery.NOT_SENT, result.delivery)
        assertNull(result.sentAt)
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertFalse("guardian" in body)
    }

    @Test
    fun elLimitNoEsUnErrorSinoUnEstat() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"status":"pending","guardian":"ad•••@x.cat","can_resend":false,"error":"rate_limited"}"""))
        val result = consent.request(null, "token")
        assertEquals(ViraGuardianRequest.Delivery.LIMIT_REACHED, result.delivery)
        assertEquals(ViraConsentStatus.PENDING, result.status)
    }

    @Test
    fun elCorreuDelMateixCompteEsDiu() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"same_email"}"""))
        assertFails(ViraBackendError.GUARDIAN_IS_YOU) { consent.request("jo@x.cat", "token") }
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"invalid_email"}"""))
        assertFails(ViraBackendError.INVALID_EMAIL) { consent.request("jo", "token") }
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"unavailable"}"""))
        assertFails(ViraBackendError.UNAVAILABLE) { consent.request("a@x.cat", "token") }
    }

    @Test
    fun elResumEsUnGet() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"status":"declined","guardian":"ad•••@x.cat","can_resend":false}"""))
        val result = consent.summary("token")
        assertEquals(ViraConsentStatus.DECLINED, result.status)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun elCorreuEsRetallaComAlServidor() {
        assertEquals("ma•••@gmail.com", ViraGuardianRequest.mask("mama.garcia@gmail.com"))
        assertEquals("a•••@x.cat", ViraGuardianRequest.mask("a@x.cat"))
        assertEquals("•••", ViraGuardianRequest.mask("@x.cat"))
    }

    private suspend fun assertFails(expected: ViraBackendError, block: suspend () -> Unit) {
        try {
            block()
            fail("Havia de fallar amb $expected")
        } catch (error: ViraBackendException) {
            assertEquals(expected, error.error)
        }
    }
}
