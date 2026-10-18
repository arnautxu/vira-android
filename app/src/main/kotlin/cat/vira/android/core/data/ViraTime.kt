package cat.vira.android.core.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/// Dates tal com les entén l'iPhone, perquè les files que es sincronitzen siguin idèntiques.
///
/// Tres decisions que no s'han de tocar sense tocar també l'iPhone:
/// - **`distantPast`** és el `Date.distantPast` de Swift (`0001-01-01T00:00:00Z`): el sentinella
///   dels singletons que ningú ha tocat (`ViraSyncRow.isPlaceholder`).
/// - **Al JSON** les dates van en UTC amb tres decimals i `Z`, com `ISO8601FormatStyle
///   (includingFractionalSeconds: true)`. Un `+00:00` portaria un `+` a les consultes, que en una
///   URL vol dir espai.
/// - **A la base de dades** van en **microsegons** des de 1970: Postgres desa `timestamptz` amb
///   sis decimals, i comparar «l'últim escriu» amb mil·lisegons faria que dues files iguals
///   semblessin diferents.
object ViraTime {

    val distantPast: Instant = Instant.parse("0001-01-01T00:00:00Z")

    private val wire: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    /// Ara, retallat a microsegons: és la precisió que el servidor guarda.
    fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

    fun format(instant: Instant): String = wire.format(instant)

    /// Accepta el que torna Postgres (`…:00.123456+00:00`), el que escriu l'iPhone
    /// (`…:00.123Z`) i sense decimals. `null` si no és una data.
    fun parse(raw: String): Instant? = try {
        OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
    } catch (_: Exception) {
        null
    }

    fun toMicros(instant: Instant): Long =
        Math.addExact(Math.multiplyExact(instant.epochSecond, 1_000_000L), instant.nano / 1_000L)

    fun fromMicros(micros: Long): Instant =
        Instant.ofEpochSecond(Math.floorDiv(micros, 1_000_000L), Math.floorMod(micros, 1_000_000L) * 1_000L)
}
