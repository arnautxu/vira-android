package cat.vira.android.core.backend

import cat.vira.android.core.data.ViraTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/// El JSON que parla amb PostgREST (`ViraJSON` de l'iPhone).
///
/// - Els noms de camp van en `snake_case` amb `@SerialName` a cada fila: són els de l'iPhone
///   després de `convertToSnakeCase`, escrits a mà perquè es vegin.
/// - Els `null` **s'escriuen sempre**: PostgREST exigeix que totes les files d'un lot tinguin les
///   mateixes claus, i un `null` és una ordre de buidar el camp (`ViraSyncNullColumns`).
/// - El que torna el servidor de més (`user_id`) s'ignora.
object ViraJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }
}

/// Dates en ISO 8601, com l'iPhone (`ViraTime`).
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("cat.vira.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(ViraTime.format(value))
    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString()
        return ViraTime.parse(raw) ?: throw IllegalArgumentException("Data il·legible: $raw")
    }
}

object UuidSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("cat.vira.UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}
