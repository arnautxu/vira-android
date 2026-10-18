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
        loader = ParentChildSummaryLoader(ViraRestClient(config, OkHttpClient()), clock = { now })
    }

    @After
    fun tearDown() = server.shutdown()

    private fun link(usage: Boolean = false, habits: Boolean = false, emotions: Boolean = false, medication: Boolean = false) =
        ParentLink(UUID.randomUUID(), child, UUID.randomUUID(), usage, habits, emotions, medication, now)

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
        responses["/rest/v1/profiles"] = """[{"display_name":"Marc"}]"""
        responses["/rest/v1/rpc/parent_child_total_xp"] = "1240"
        responses["/rest/v1/streaks"] = """[{"current_days":12}]"""
        responses["/rest/v1/focus_sessions"] = """[
            {"started_at":"2026-10-17T10:00:00Z","completed":true},
            {"started_at":"2026-10-16T10:00:00Z","completed":false},
            {"started_at":"2026-10-01T10:00:00Z","completed":true}]"""
        responses["/rest/v1/habit_logs"] = """[{"day":"2026-10-15T00:00:00Z"},{"day":"2026-09-30T00:00:00Z"}]"""
        responses["/rest/v1/plan_items"] = """[{"status":"pending"},{"status":"done"},{"status":"pending"}]"""

        val summary = loader.load(link(usage = true, habits = true), "token")
        assertEquals("Marc", summary.displayName)
        assertEquals(1240, summary.totalXP)
        assertEquals(12, summary.streakDays)
        assertEquals(1, summary.focusSessionsLastWeek)
        assertEquals(1, summary.habitsDoneLastWeek)
        assertEquals(2, summary.tasksPending)
        assertNull(summary.emotionCheckinsLastWeek)
    }

    @Test
    fun elNomNomesEsLlegeixAmbLUsObert() = runBlocking {
        responses["/rest/v1/profiles"] = """[{"display_name":"  Jana "}]"""
        val names = loader.displayNames(listOf(link(usage = true), link(usage = false)), "token")
        assertEquals(mapOf(child to "Jana"), names)
        assertEquals(1, paths.count { it == "/rest/v1/profiles" })
    }
}
