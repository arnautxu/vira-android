package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
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
        val mailed = mutableListOf<String>()
        var continued = false
        var asked: ViraGuardianRequest? = null
    }

    private fun writer(
        log: Log,
        failure: ViraBackendError? = null,
        mailFailure: ViraBackendError? = null,
    ) = EntryConsentWriter(
        declare = { status, email ->
            if (failure != null) throw ViraBackendException(failure)
            log.written += status
            if (email != null) log.guardian += email
        },
        requestGuardian = { email ->
            if (mailFailure != null) throw ViraBackendException(mailFailure)
            log.mailed += email
            ViraGuardianRequest(ViraConsentStatus.PENDING, "ad•••@exemple.test", delivery = ViraGuardianRequest.Delivery.SENT)
        },
    )

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
    fun `12-15 envia el correu i ensenya on ha anat abans d'avancar`() = runBlocking {
        val log = Log()
        val error = writer(log).submit("12-15", "adult@exemple.test", onAsked = { log.asked = it }, onWritten = { log.continued = true })
        assertNull(error)
        assertEquals(listOf("adult@exemple.test"), log.mailed)
        assertEquals(ViraGuardianRequest.Delivery.SENT, log.asked?.delivery)
        // Avança la fulla «Correu enviat», amb «Continua»: no el pas tot sol.
        assertFalse(log.continued)
    }

    @Test
    fun `12-15 sense la funcio del correu desa el permis i diu que no ha sortit`() = runBlocking {
        val log = Log()
        val error = writer(log, mailFailure = ViraBackendError.UNAVAILABLE)
            .submit("12-15", "adult@exemple.test", onAsked = { log.asked = it }, onWritten = { log.continued = true })
        assertNull(error)
        assertEquals(listOf(ViraConsentStatus.PENDING), log.written)
        assertEquals(listOf("adult@exemple.test"), log.guardian)
        assertEquals(ViraGuardianRequest.Delivery.NOT_SENT, log.asked?.delivery)
        assertEquals("ad•••@exemple.test", log.asked?.guardian)
    }

    @Test
    fun `12-15 amb el correu del mateix compte no desa res`() = runBlocking {
        val log = Log()
        val error = writer(log, mailFailure = ViraBackendError.GUARDIAN_IS_YOU)
            .submit("12-15", "jo@exemple.test", onAsked = { log.asked = it }, onWritten = { log.continued = true })
        assertEquals(ViraBackendError.GUARDIAN_IS_YOU, error)
        assertTrue(log.written.isEmpty())
        assertNull(log.asked)
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
        val error = EntryConsentWriter({ _, _ -> error("xarxa") }, { error("xarxa") })
            .submit("16-20", "", onWritten = { log.continued = true })
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
