package cat.vira.android.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ViraTimeTest {

    @Test
    fun elFormatDeXarxaPortaMilisegonsIUtc() {
        assertEquals("2026-10-18T09:05:03.120Z", ViraTime.format(Instant.parse("2026-10-18T09:05:03.120456Z")))
    }

    @Test
    fun llegeixElQueTornaPostgrest() {
        assertEquals(Instant.parse("2026-10-18T09:05:03.123456Z"), ViraTime.parse("2026-10-18T09:05:03.123456+00:00"))
        assertEquals(Instant.parse("2026-10-18T09:05:03Z"), ViraTime.parse("2026-10-18T09:05:03Z"))
    }

    @Test
    fun elsMicrosegonsFanElViatgeSencer() {
        val instant = Instant.parse("2026-10-18T09:05:03.123456Z")
        assertEquals(instant, ViraTime.fromMicros(ViraTime.toMicros(instant)))
        assertEquals(ViraTime.distantPast, ViraTime.fromMicros(ViraTime.toMicros(ViraTime.distantPast)))
    }

    @Test
    fun elsConvertidorsDeRoomTornenElMateix() {
        val converters = ViraConverters()
        val id = UUID.randomUUID()
        assertEquals(id, converters.textToUuid(converters.uuidToText(id)))
        assertEquals(listOf(1, 3, 7), converters.textToInts(converters.intsToText(listOf(1, 3, 7))))
        assertEquals(listOf("home", "phone"), converters.textToStrings(converters.stringsToText(listOf("home", "phone"))))
    }
}
