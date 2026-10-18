package cat.vira.android.core.backend.sync

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.backend.ViraAuthBackend
import cat.vira.android.core.backend.ViraBackendConfig
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraOAuthProvider
import cat.vira.android.core.backend.ViraRestClient
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.ViraStoredSession
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.backend.ViraUserMetadata
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.PlanItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Collections
import java.util.UUID

/// El motor sencer contra un servidor fals: què puja, què no puja mai i què baixa.
// L'Application buida: la de Vira engega WorkManager i la sincronització, que aquí no toquen.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraSyncEngineTest {

    private val userId = UUID.fromString("6f1c2a3b-0000-4000-8000-000000000001")
    private val server = MockWebServer()
    private val requests: MutableList<RecordedRequest> = Collections.synchronizedList(mutableListOf())
    /// Les files que el servidor torna per taula, en la primera pàgina.
    private val serverRows = mutableMapOf<String, String>()
    /// La fila de `guardian_consents` que torna el servidor (la de debò: la metadada no compta).
    private var serverConsent = "[]"
    private lateinit var session: ViraSession

    private lateinit var db: ViraDatabase
    private lateinit var preferences: SharedPreferences
    private lateinit var store: ViraStore

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val table = request.requestUrl!!.pathSegments.last()
                return when {
                    request.method == "GET" && table == "guardian_consents" -> MockResponse().setBody(serverConsent)
                    request.method == "GET" -> MockResponse().setBody(serverRows.remove(table) ?: "[]")
                    else -> MockResponse().setResponseCode(201)
                }
            }
        }
        server.start()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = ViraDatabase.inMemory(context)
        preferences = ViraPreferences.open(context).also { it.edit().clear().commit() }
        store = ViraStore(db)
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    private suspend fun engine(
        consent: ViraConsentStatus,
        serverSays: ViraConsentStatus = consent,
    ): ViraSyncEngine {
        serverConsent = when (serverSays) {
            ViraConsentStatus.GRANTED -> """[{"status":"granted"}]"""
            ViraConsentStatus.PENDING -> """[{"status":"pending"}]"""
            ViraConsentStatus.MISSING -> "[]"
        }
        val config = ViraBackendConfig.Values(server.url("/").toString().trimEnd('/'), "anon")
        val rest = ViraRestClient(config, OkHttpClient())
        session = ViraSession(FakeAuth(ViraUser(userId, "a@b.c", consent = consent)), db, preferences, rest)
        session.restore()
        return ViraSyncEngine(session, db, preferences, rest, store)
    }

    private fun bodies(table: String, method: String = "POST") =
        requests.filter { it.method == method && it.requestUrl!!.pathSegments.last() == table }

    @Test
    fun pujaElNucliIMaiLesEmocionsSensePermis() = runBlocking {
        store.create(db.planItems(), PlanItem(title = "Deures de mates"))
        store.create(db.emotionEntries(), EmotionEntry(emotionRaw = "calm"))

        val engine = engine(ViraConsentStatus.PENDING)
        engine.sync()

        assertTrue(engine.state.value is ViraSyncEngine.State.Succeeded)
        val upload = bodies("plan_items").single()
        assertTrue(upload.body.readUtf8().contains("Deures de mates"))
        assertEquals("plan_items", upload.requestUrl!!.pathSegments.last())
        assertEquals("id", upload.requestUrl!!.queryParameter("on_conflict"))
        // Ni pujada ni baixada: la capa sencera no existeix sense el permís.
        assertTrue(requests.none { it.requestUrl!!.pathSegments.last() == "emotion_entries" })
    }

    @Test
    fun ambPermisLesEmocionsPugen() = runBlocking {
        store.create(db.emotionEntries(), EmotionEntry(emotionRaw = "calm"))
        engine(ViraConsentStatus.GRANTED).sync()
        assertEquals(1, bodies("emotion_entries").size)
    }

    @Test
    fun laMetadadaSolaNoObreLesEmocions() = runBlocking {
        store.create(db.emotionEntries(), EmotionEntry(emotionRaw = "calm"))
        // Un client que s'hagi escrit `consent=granted` a la metadada, amb el servidor dient que no.
        val engine = engine(ViraConsentStatus.GRANTED, serverSays = ViraConsentStatus.PENDING)
        engine.sync()
        assertTrue(requests.none { it.requestUrl!!.pathSegments.last() == "emotion_entries" })
        assertEquals(ViraConsentStatus.PENDING, session.verifiedConsent)
        assertEquals(ViraConsentStatus.PENDING, session.user?.consent)
    }

    @Test
    fun elsComptadorsBuitsNoTrepitgenElCompte() = runBlocking {
        store.counter()
        store.streak()
        store.profile()
        engine(ViraConsentStatus.GRANTED).sync()
        assertTrue(bodies("progress_counters").isEmpty())
        assertTrue(bodies("streaks").isEmpty())
        assertTrue(bodies("profiles").isEmpty())
    }

    @Test
    fun laPrimeraPassadaBaixaAbansDePujarIFiltraPerCompte() = runBlocking {
        val id = UUID.randomUUID()
        serverRows["plan_items"] = """
            [{"id":"$id","title":"De l'iPhone","notes":null,"day":"2026-10-18T00:00:00+00:00","time":null,
              "status":"pending","completed_at":null,"reminder_offset_minutes":null,"recurrence":"none",
              "symbol_name":"picto-flag","sort_index":0,"created_at":"2026-10-17T10:00:00+00:00",
              "updated_at":"2026-10-17T10:00:00.123456+00:00","user_id":"$userId"}]
        """.trimIndent()
        store.create(db.planItems(), PlanItem(title = "D'aquí"))

        engine(ViraConsentStatus.GRANTED).sync()

        val planRequests = requests.filter { it.requestUrl!!.pathSegments.last() == "plan_items" }
        assertEquals("GET", planRequests.first().method)
        assertEquals("eq.$userId", planRequests.first().requestUrl!!.queryParameter("user_id"))
        val pulled = db.planItems().byId(id)!!
        assertEquals("De l'iPhone", pulled.title)
        assertEquals(Instant.parse("2026-10-17T10:00:00.123456Z"), pulled.updatedAt)
        assertEquals(2, db.planItems().all().size)
    }

    @Test
    fun elsEsborratsSurtenPrimer() = runBlocking {
        val item = store.create(db.planItems(), PlanItem(title = "Fora"))
        store.delete(item)
        engine(ViraConsentStatus.GRANTED).sync()

        val delete = requests.first { it.method == "DELETE" }
        assertEquals("in.(${item.id})", delete.requestUrl!!.queryParameter("id"))
        assertEquals("eq.$userId", delete.requestUrl!!.queryParameter("user_id"))
        assertTrue(db.tombstones().all().isEmpty())
    }

    @Test
    fun unEsborratQueNoHaArribatNoEsRessuscita() = runBlocking {
        val item = store.create(db.planItems(), PlanItem(title = "Fora"))
        store.delete(item)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val table = request.requestUrl!!.pathSegments.last()
                return when {
                    request.method == "DELETE" -> MockResponse().setResponseCode(503)
                    request.method == "GET" && table == "plan_items" -> MockResponse().setBody(
                        """[{"id":"${item.id}","title":"Fora","notes":null,"day":"2026-10-18T00:00:00+00:00",
                        "time":null,"status":"pending","completed_at":null,"reminder_offset_minutes":null,
                        "recurrence":"none","symbol_name":"picto-flag","sort_index":0,
                        "created_at":"2026-10-17T10:00:00+00:00","updated_at":"2030-01-01T00:00:00+00:00"}]""",
                    )
                    request.method == "GET" -> MockResponse().setBody("[]")
                    else -> MockResponse().setResponseCode(201)
                }
            }
        }
        engine(ViraConsentStatus.GRANTED).sync()

        assertTrue(db.planItems().all().isEmpty())
        assertEquals(listOf(item.id), db.tombstones().recordIDs("plan_items"))
    }

    @Test
    fun elsEsborratsVanATrossos() = runBlocking {
        repeat(ViraSyncEngine.TOMBSTONE_CHUNK + 1) { store.delete(store.create(db.planItems(), PlanItem(title = "$it"))) }
        engine(ViraConsentStatus.GRANTED).sync()
        assertEquals(2, requests.count { it.method == "DELETE" })
        assertTrue(db.tombstones().all().isEmpty())
    }

    @Test
    fun unEsborratRebutjatNoAturaElsDeLesAltresTaules() = runBlocking {
        store.delete(store.create(db.planItems(), PlanItem(title = "Fora")))
        store.delete(store.create(db.personalRewards(), cat.vira.android.core.data.model.PersonalReward(title = "Fora")))
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val table = request.requestUrl!!.pathSegments.last()
                return when {
                    request.method == "DELETE" && table == "plan_items" ->
                        MockResponse().setResponseCode(400).setBody("""{"code":"PGRST204"}""")
                    request.method == "GET" -> MockResponse().setBody("[]")
                    else -> MockResponse().setResponseCode(201)
                }
            }
        }
        val engine = engine(ViraConsentStatus.GRANTED)
        engine.sync()

        assertEquals(1, db.tombstones().recordIDs("plan_items").size)
        assertTrue(db.tombstones().recordIDs("personal_rewards").isEmpty())
        assertTrue(engine.state.value is ViraSyncEngine.State.Failed)
    }

    @Test
    fun unaTaulaQueFallaNoAturaLesAltres() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val table = request.requestUrl!!.pathSegments.last()
                return when {
                    table == "plan_items" -> MockResponse().setResponseCode(400).setBody("""{"code":"PGRST204"}""")
                    request.method == "GET" -> MockResponse().setBody("[]")
                    else -> MockResponse().setResponseCode(201)
                }
            }
        }
        store.create(db.habits(), cat.vira.android.core.data.model.Habit(title = "Llegir"))
        val engine = engine(ViraConsentStatus.GRANTED)
        engine.sync()

        assertEquals(1, bodies("habits").size)
        assertTrue(engine.state.value is ViraSyncEngine.State.Failed)
    }

    @Test
    fun siElPareFallaElFillEspera() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val table = request.requestUrl!!.pathSegments.last()
                return when {
                    table == "habits" -> MockResponse().setResponseCode(503)
                    request.method == "GET" -> MockResponse().setBody("[]")
                    else -> MockResponse().setResponseCode(201)
                }
            }
        }
        engine(ViraConsentStatus.GRANTED).sync()
        assertTrue(requests.none { it.requestUrl!!.pathSegments.last() == "habit_logs" })
        assertTrue(requests.any { it.requestUrl!!.pathSegments.last() == "focus_profiles" })
    }

    @Test
    fun unaSessioRebutjadaAturaLaPassadaITancaLaSessio() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return MockResponse().setResponseCode(401).setBody("""{"code":"PGRST301"}""")
            }
        }
        val engine = engine(ViraConsentStatus.GRANTED)
        engine.sync()
        assertEquals(ViraSyncEngine.State.Failed(cat.vira.android.core.backend.ViraBackendError.SESSION_EXPIRED), engine.state.value)
        // La consulta del permís no compta: si falla, no obre res i la passada segueix fins al 401.
        assertEquals(1, requests.count { it.requestUrl!!.pathSegments.last() != "guardian_consents" })
    }

    @Test
    fun unEsborratAMitgesTornaAPujarHoTot() = runBlocking {
        store.create(db.planItems(), PlanItem(title = "Deures"))
        val engine = engine(ViraConsentStatus.GRANTED)
        engine.sync()
        assertTrue(ViraSyncCursors(preferences).get("push.plan_items") != null)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.method == "DELETE") MockResponse().setResponseCode(503) else MockResponse().setBody("[]")
        }
        assertTrue(runCatching { engine.eraseEverything() }.isFailure)
        // El telèfon ho conserva i, sense cursors, la passada següent ho torna a enviar sencer.
        assertEquals(1, db.planItems().all().size)
        assertEquals(null, ViraSyncCursors(preferences).get("push.plan_items"))
    }

    @Test
    fun esborrarHoTotComencaPelServidor() = runBlocking {
        store.create(db.planItems(), PlanItem(title = "Deures"))
        val engine = engine(ViraConsentStatus.GRANTED)
        engine.eraseEverything()

        val deletes = requests.filter { it.method == "DELETE" }.map { it.requestUrl!!.pathSegments.last() }
        assertEquals(engine.syncedTables, deletes)
        assertEquals(18, deletes.size)
        assertTrue(db.planItems().all().isEmpty())
    }

    @Test
    fun unComptadorVellNoEsMenjaElLlibreMajor() = runBlocking {
        store.counter()
        db.coinTransactions().upsertRaw(listOf(CoinTransaction(amount = 50, updatedAt = Instant.parse("2026-10-01T10:00:00Z"))))
        serverRows["progress_counters"] = """[{"total_xp":0,"coin_balance":0,"coins_earned_total":0,
            "emotion_checkins_count":0,"focus_sessions_count":0,"habits_completed_count":0,"days_active_count":0,
            "updated_at":"2026-10-17T10:00:00+00:00","user_id":"$userId"}]"""
        engine(ViraConsentStatus.GRANTED).sync()
        assertEquals(50, store.counter().coinBalance)
    }

    @Test
    fun unaRatxaMesNovaNoPerdElsDiesQueNomesSabiaElServidor() = runBlocking {
        val mine = Instant.parse("2026-10-17T00:00:00Z")
        val theirs = Instant.parse("2026-10-10T00:00:00Z")
        db.streaks().upsertRaw(listOf(store.streak().copy(
            bestDays = 2, activeDays = listOf(mine), updatedAt = Instant.parse("2026-10-17T12:00:00Z"),
        )))
        serverRows["streaks"] = """[{"current_days":1,"best_days":9,"last_active_day":null,"freezes_available":2,
            "freezes_used_this_month":0,"last_grant_month":24322,"is_frozen":false,"protected_days":[],
            "active_days":["2026-10-10T00:00:00+00:00"],"is_visible":true,"has_celebrated_week_streak":true,
            "has_celebrated_month_streak":false,"updated_at":"2026-10-11T10:00:00+00:00","user_id":"$userId"}]"""
        engine(ViraConsentStatus.GRANTED).sync()
        val streak = store.streak()
        assertEquals(listOf(theirs, mine), streak.activeDays)
        assertEquals(9, streak.bestDays)
        assertTrue(streak.hasCelebratedWeekStreak)
        // I puja: és el que els altres telèfons encara no saben.
        assertTrue(bodies("streaks").isNotEmpty())
    }

    @Test
    fun elCursorNoBaixaDeLAnyU() {
        val cursors = ViraSyncCursors(preferences)
        cursors.set("pull.progress_counters", ViraTime.distantPast)
        assertEquals(ViraTime.distantPast, cursors.get("pull.progress_counters"))
    }

    @Test
    fun lesFilesPujenAmbLesClausDelServidor() {
        val row = PlanItemSyncTable.encode(PlanItemSyncRow(PlanItem(title = "x")))
        val keys = row.keys
        assertTrue("updated_at" in keys && "created_at" in keys && "sort_index" in keys)
        assertTrue(keys.none { it.any(Char::isUpperCase) })
        val array: JsonArray = Json.parseToJsonElement("[$row]").jsonArray
        assertEquals("x", array.first().jsonObject.getValue("title").jsonPrimitive.content)
    }

    private class FakeAuth(private val user: ViraUser) : ViraAuthBackend {
        private val session = ViraStoredSession("token", "refresh", Long.MAX_VALUE / 2, user)
        override suspend fun signUp(email: String, password: String, metadata: ViraUserMetadata) = session
        override suspend fun signIn(email: String, password: String) = session
        override suspend fun sendPasswordReset(email: String) = Unit
        override suspend fun updatePassword(password: String) = Unit
        override suspend fun startOAuth(provider: ViraOAuthProvider) = Unit
        override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String) = session
        override suspend fun session(fromCallback: Uri) = session
        override suspend fun session() = session
        override suspend fun refreshedSession() = session
        override suspend fun storedSession() = session
        override suspend fun signOut() = Unit
        override suspend fun forgetSession() = Unit
        override suspend fun updateMetadata(metadata: ViraUserMetadata) = user
    }
}
