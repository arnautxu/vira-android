package cat.vira.android.features.parent

import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraJson
import cat.vira.android.core.backend.ViraRestClient
import cat.vira.android.core.backend.ViraRestClient.Param
import cat.vira.android.core.data.ViraTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.util.UUID

/// Totes les crides del panell de pares (`ViraParentLinkClient` de l'iPhone).
///
/// Cap filtre d'aquest fitxer és una protecció: qui pot llegir què ho decideixen les polítiques
/// RLS. Aquí els filtres només serveixen per no baixar-se coses que no calen.
class ParentLinkClient(
    private val rest: ViraRestClient = ViraRestClient(),
    private val clock: () -> Instant = ViraTime::now,
) {

    // Vincular

    /// El menor genera un codi de sis xifres, viu quinze minuts. Cap vincle comença sense això.
    suspend fun createInvite(accessToken: String): String =
        scalar(rest.rpc("create_link_invite", JsonObject(emptyMap()), accessToken))
            ?: throw ViraBackendException(ViraBackendError.UNAVAILABLE)

    /// El pare/mare l'introdueix. Torna l'identificador del fill/a vinculat.
    suspend fun redeem(code: String, accessToken: String): UUID {
        val body = JsonObject(mapOf("invite_code" to JsonPrimitive(code)))
        val raw = scalar(rest.rpc("redeem_link_invite", body, accessToken))
        return raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw ViraBackendException(ViraBackendError.NOT_ALLOWED)
    }

    /// Els vincles que et pertoquen. Un compte de fill/a en veu els seus; un de pare/mare, els
    /// seus. La mateixa consulta per als dos: la política decideix.
    suspend fun links(accessToken: String): List<ParentLink> =
        select("parent_links", listOf(Param("order", "created_at.asc")), ParentLink.serializer(), accessToken)

    suspend fun updateScopes(link: ParentLink, accessToken: String) =
        rest.upsert("parent_links", listOf(encode(ParentLink.serializer(), link)), accessToken)

    /// Trencar el vincle no esborra res del menor: només tanca la porta.
    suspend fun revoke(link: ParentLink, accessToken: String) {
        val closed = link.copy(
            revokedAt = clock(),
            scopeUsage = false, scopeHabits = false, scopeEmotions = false, scopeMedication = false,
        )
        rest.upsert("parent_links", listOf(encode(ParentLink.serializer(), closed)), accessToken)
    }

    // Missatges

    suspend fun messages(accessToken: String): List<ParentMessage> =
        select("parent_messages", recentFirst, ParentMessage.serializer(), accessToken)

    /// `parent_user_id` no s'envia: el posa el servidor amb `auth.uid()`.
    suspend fun send(body: String, childId: UUID, accessToken: String) {
        val row = JsonObject(mapOf(
            "id" to JsonPrimitive(UUID.randomUUID().toString()),
            "child_user_id" to JsonPrimitive(childId.toString()),
            "body" to JsonPrimitive(body),
        ))
        rest.upsert("parent_messages", listOf(row), accessToken)
    }

    suspend fun markRead(message: ParentMessage, accessToken: String) =
        rest.upsert("parent_messages", listOf(encode(ParentMessage.serializer(), message.copy(readAt = clock()))), accessToken)

    // Recordatoris proposats

    private val pending = listOf(Param("accepted_at", "is.null"), Param("dismissed_at", "is.null"))

    suspend fun reminders(accessToken: String): List<ParentReminder> =
        // Només els pendents, abans del límit: si no, cinquanta de resolts taparien un pendent més vell.
        select("parent_reminders", pending + recentFirst, ParentReminder.serializer(), accessToken)

    /// Neix sense `accepted_at` ni `dismissed_at`, i sense `parent_user_id`: això ho posa el servidor.
    suspend fun propose(title: String, day: Instant?, childId: UUID, accessToken: String) {
        val row = JsonObject(mapOf(
            "id" to JsonPrimitive(UUID.randomUUID().toString()),
            "child_user_id" to JsonPrimitive(childId.toString()),
            "title" to JsonPrimitive(title),
            "day" to (day?.let { JsonPrimitive(ViraTime.format(it)) } ?: JsonNull),
        ))
        rest.upsert("parent_reminders", listOf(row), accessToken)
    }

    suspend fun resolve(reminder: ParentReminder, accepted: Boolean, accessToken: String) {
        val now = clock()
        val resolved = if (accepted) reminder.copy(acceptedAt = now) else reminder.copy(dismissedAt = now)
        rest.upsert("parent_reminders", listOf(encode(ParentReminder.serializer(), resolved)), accessToken)
    }

    private suspend fun <T> select(table: String, query: List<Param>, serializer: KSerializer<T>, token: String): List<T> =
        try {
            ViraJson.json.decodeFromJsonElement(ListSerializer(serializer), rest.select(table, query, token))
        } catch (error: IllegalArgumentException) {
            throw ViraBackendException(ViraBackendError.UNAVAILABLE, error)
        }

    private fun <T> encode(serializer: KSerializer<T>, value: T): JsonObject =
        ViraJson.json.encodeToJsonElement(serializer, value).jsonObject

    companion object {
        private val recentFirst = listOf(Param("order", "created_at.desc"), Param("limit", "50"))

        /// El que torna una funció escalar de Postgres: `"123456"`, `42` o `null`. `null` és «no hi
        /// ha res», i qui el rep l'ha de tractar així, mai com un zero.
        internal fun scalar(raw: String): String? {
            val element: JsonElement = try {
                ViraJson.json.parseToJsonElement(raw.ifBlank { "null" })
            } catch (_: IllegalArgumentException) {
                return null
            }
            return if (element is JsonNull) null else (element as? JsonPrimitive)?.contentOrNull
        }
    }
}
