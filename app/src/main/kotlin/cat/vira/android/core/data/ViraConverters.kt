package cat.vira.android.core.data

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/// Com es desen a SQLite els tipus que SwiftData guarda sols.
///
/// Les llistes (`weekdays`, `timesInMinutes`, `protectedDays`…) van en JSON dins d'una columna de
/// text: és el que fa SwiftData per sota, i el servidor també les té en `jsonb`.
class ViraConverters {

    @TypeConverter fun uuidToText(value: UUID?): String? = value?.toString()
    @TypeConverter fun textToUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter fun instantToMicros(value: Instant?): Long? = value?.let(ViraTime::toMicros)
    @TypeConverter fun microsToInstant(value: Long?): Instant? = value?.let(ViraTime::fromMicros)

    @TypeConverter fun intsToText(value: List<Int>): String = json.encodeToString(value)
    @TypeConverter fun textToInts(value: String): List<Int> = json.decodeFromString(value)

    @TypeConverter fun stringsToText(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun textToStrings(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter fun instantsToText(value: List<Instant>): String =
        json.encodeToString(value.map(ViraTime::toMicros))
    @TypeConverter fun textToInstants(value: String): List<Instant> =
        json.decodeFromString<List<Long>>(value).map(ViraTime::fromMicros)

    private companion object {
        val json = Json
    }
}
