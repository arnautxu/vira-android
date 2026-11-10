package cat.vira.android.features.parent

import cat.vira.android.core.backend.ViraBackendConfig
import cat.vira.android.core.backend.ViraRestClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Collections
import java.util.UUID

/// El resum que veu un pare o una mare: només el que li han obert, recomptes i mai les taules
/// sensibles (`20261018122414_emotion_counts_consent.sql`).
class ParentChildSummaryLoaderTest {

    private val now = Instant.parse("2026-10-18T12:00:00Z")
    private val child = UUID.randomUUID()
    private val server = MockWebServer()
    private val paths: MutableList<String> = Collections.synchronizedList(mutableListOf())
    /// Què torna cada camí. El que no hi és torna `[]`.
    private val responses = mutableMapOf<String, String>()
    private lateinit var loader: ParentChildSummaryLoader

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                paths += path
                return MockResponse().setBody(responses[path] ?: "[]")
            }
        }
        server.start()
        val config = ViraBackendConfig.Values(server.url("/").toString().trimEnd('/'), "anon")
        loader = ParentChildSummaryLoader(ViraRestClient(config, OkHttpClient()))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun link(usage: Boolean = false, habits: Boolean = false, emotions: Boolean = false, medication: Boolean = false) =
        ParentLink(UUID.randomUUID(), child, UUID.randomUUID(), usage, habits, emotions, medication, now)

    /// Comptar tasques no vol dir baixar-ne el títol ni les notes: el pare o la mare només rep
    /// números del servidor, mai files de cap taula del fill o filla.
    @Test
    fun nomesNumerosMaiFiles() = runBlocking {
        loader.load(link(usage = true, habits = true, emotions = true, medication = true), "token")
        loader.displayNames(listOf(link(usage = true)), "token")
        assertTrue(paths.isNotEmpty())
        assertTrue(paths.all { it.startsWith("/rest/v1/rpc/") })
    }

    @Test
    fun ambTotTancatNoEsPreguntaRes() = runBlocking {
        val summary = loader.load(link(), "token")
        assertEquals(ParentChildSummary(), summary)
        assertTrue(paths.isEmpty())
    }

    @Test
    fun lesEmocionsIMedicacioNomesPerFuncioMaiPerTaula() = runBlocking {
        responses["/rest/v1/rpc/parent_child_emotion_checkins_last_week"] = "5"
        responses["/rest/v1/rpc/parent_child_active_medications"] = "2"
        val summary = loader.load(link(emotions = true, medication = true), "token")

        assertEquals(5, summary.emotionCheckinsLastWeek)
        assertEquals(2, summary.activeMedications)
        val sensitive = listOf("emotion_entries", "medications", "medication_intakes", "progress_counters")
        assertFalse(paths.any { path -> sensitive.any { path.endsWith("/$it") } })
    }

    /// Àmbit obert però sense el permís del tutor: el servidor diu `null`, i això és «no ho pots
    /// veure», no un zero.
    @Test
    fun senseElPermisDelTutorElRecompteEsNullNoZero() = runBlocking {
        responses["/rest/v1/rpc/parent_child_emotion_checkins_last_week"] = "null"
        responses["/rest/v1/rpc/parent_child_active_medications"] = "null"
        val summary = loader.load(link(emotions = true, medication = true), "token")
        assertNull(summary.emotionCheckinsLastWeek)
        assertNull(summary.activeMedications)
    }

    @Test
    fun lUsIElsHabitsCompten() = runBlocking {
        responses["/rest/v1/rpc/parent_child_summary"] = """{"display_name":"Marc","total_xp":1240,"streak_days":12,
            "focus_last_week":1,"habits_last_week":1,"tasks_pending":2}"""

        val summary = loader.load(link(usage = true, habits = true), "token")
        assertEquals("Marc", summary.displayName)
        assertEquals(1240, summary.totalXP)
        assertEquals(12, summary.streakDays)
        assertEquals(1, summary.focusSessionsLastWeek)
        assertEquals(1, summary.habitsDoneLastWeek)
        assertEquals(2, summary.tasksPending)
        assertNull(summary.emotionCheckinsLastWeek)
    }

    /// Un àmbit que el servidor no dona (tancat des del telèfon del fill) surt tancat, encara que
    /// la còpia del vincle d'aquest telèfon digui que és obert.
    @Test
    fun manaElServidorNoLaCopiaDelVincle() = runBlocking {
        responses["/rest/v1/rpc/parent_child_summary"] = """{"habits_last_week":3,"tasks_pending":0}"""
        val summary = loader.load(link(usage = true, habits = true), "token")
        assertNull(summary.streakDays)
        assertNull(summary.totalXP)
        assertEquals(3, summary.habitsDoneLastWeek)
        assertEquals(0, summary.tasksPending)
    }

    @Test
    fun elNomNomesEsLlegeixAmbLUsObert() = runBlocking {
        val other = UUID.randomUUID()
        responses["/rest/v1/rpc/parent_child_names"] =
            """[{"child_user_id":"$child","display_name":"  Jana "},{"child_user_id":"$other","display_name":"Pau"}]"""
        val names = loader.displayNames(listOf(link(usage = true), link(usage = false)), "token")
        assertEquals(mapOf(child to "Jana"), names)
        assertEquals(1, paths.count { it == "/rest/v1/rpc/parent_child_names" })
    }

    @Test
    fun senseUsObertNoEsDemanaCapNom() = runBlocking {
        val names = loader.displayNames(listOf(link(usage = false)), "token")
        assertTrue(names.isEmpty())
        assertTrue(paths.isEmpty())
    }
}
