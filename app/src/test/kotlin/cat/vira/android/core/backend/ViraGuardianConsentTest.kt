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
        consent = ViraGuardianConsent(ViraRestClient(config, OkHttpClient()))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun nomesGrantedINotRequiredObren() {
        assertEquals(ViraConsentStatus.GRANTED, ViraConsentStatus.fromServer("granted"))
        assertEquals(ViraConsentStatus.GRANTED, ViraConsentStatus.fromServer("not_required"))
        assertEquals(ViraConsentStatus.PENDING, ViraConsentStatus.fromServer("pending"))
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
}
