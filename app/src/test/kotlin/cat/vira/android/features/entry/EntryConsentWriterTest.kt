package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/// **El pas de permís no avança si no ha escrit** (`EntryFirstActionTests` de l'iPhone, §permís).
class EntryConsentWriterTest {

    private class Log {
        val written = mutableListOf<ViraConsentStatus>()
        val guardian = mutableListOf<String>()
        var continued = false
    }

    private fun writer(log: Log, failure: ViraBackendError? = null) = EntryConsentWriter { status, email ->
        if (failure != null) throw ViraBackendException(failure)
        log.written += status
        if (email != null) log.guardian += email
    }

    @Test
    fun `16 o mes escriu el permis i avanca`() = runBlocking {
        val log = Log()
        val error = writer(log).submit("16-20", "", onWritten = { log.continued = true })
        assertNull(error)
        assertEquals(listOf(ViraConsentStatus.GRANTED), log.written)
        assertTrue(log.guardian.isEmpty())
        assertTrue(log.continued)
    }

    @Test
    fun `16 o mes, si l'escriptura falla, no avanca`() = runBlocking {
        val log = Log()
        val error = writer(log, ViraBackendError.OFFLINE).submit("21+", "", onWritten = { log.continued = true })
        assertEquals(ViraBackendError.OFFLINE, error)
        assertFalse(log.continued)
    }

    @Test
    fun `12-15 demana el tutor, queda pendent i avanca`() = runBlocking {
        val log = Log()
        val error = writer(log).submit("12-15", "adult@exemple.test", onWritten = { log.continued = true })
        assertNull(error)
        assertEquals(listOf("adult@exemple.test"), log.guardian)
        assertEquals(listOf(ViraConsentStatus.PENDING), log.written)
        assertTrue(log.continued)
    }

    @Test
    fun `sense tram no escriu res ni avanca`() = runBlocking {
        val log = Log()
        writer(log).submit("", "", onWritten = { log.continued = true })
        assertTrue(log.written.isEmpty())
        assertFalse(log.continued)
    }

    @Test
    fun `un error desconegut es diu com a no disponible`() = runBlocking {
        val log = Log()
        val error = EntryConsentWriter { _, _ -> error("xarxa") }.submit("16-20", "", onWritten = { log.continued = true })
        assertEquals(ViraBackendError.UNAVAILABLE, error)
        assertFalse(log.continued)
    }

    @Test
    fun `la recompensa nomes compta el que ha crescut`() {
        assertEquals(EntryReward(5, 10), EntryReward(15, 30).earned(EntryReward(10, 20)))
        assertEquals(EntryReward.NONE, EntryReward(10, 20).earned(EntryReward(10, 20)))
        assertTrue(EntryFirstActionOutcome.Task("Deures").reward.isEmpty)
        assertEquals("Deures", EntryFirstActionOutcome.Task("Deures").taskTitle)
        assertNull(EntryFirstActionOutcome.CheckIn(EntryReward(1, 1)).taskTitle)
    }
}
