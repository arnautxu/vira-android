package cat.vira.android.core.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import cat.vira.android.core.backend.ViraGuardianRequest.Delivery
import java.time.Instant
import java.time.OffsetDateTime

/// El permís del tutor tal com el guarda el servidor (`guardian_consents`, D-29;
/// `ViraGuardianConsent` de l'iPhone).
///
/// Fins al 2026-10-18 el permís era `user_metadata.consent`, que qualsevol client pot escriure: un
/// client modificat es podia donar permís sol. Ara la font de veritat és la fila de
/// `guardian_consents`, i la RLS de les tres taules sensibles la comprova
/// (`20261018100641_guardian_consent_server_side.sql`).
///
/// El client **no pot escriure mai `granted`**. Només declara, via `declare_guardian_consent`:
/// - **12–15** → `pending`, amb el correu de la persona adulta.
/// - **16+** → `not_required`, que aquí es llegeix com `GRANTED` («concedit o no cal»). Si abans
///   ja s'havia declarat `pending`, el servidor no ho canvia i torna `pending`.
class ViraGuardianConsent(
    private val rest: ViraRestClient,
    /// L'idioma del correu: el de l'app de qui el demana (`ViraLanguage`).
    private val locale: () -> String = { "en" },
) {

    /// Estat del compte que obre la sessió. La RLS només en deixa veure la fila pròpia.
    suspend fun fetch(accessToken: String): ViraConsentStatus {
        val rows = rest.select("guardian_consents", listOf(ViraRestClient.Param("select", "status")), accessToken)
        val status = rows.firstOrNull()?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull
        return ViraConsentStatus.fromServer(status)
    }

    /// Declara el permís i torna **l'estat que ha quedat al servidor**, que pot no ser el demanat.
    suspend fun declare(
        requested: ViraConsentStatus,
        guardianEmail: String?,
        accessToken: String,
    ): ViraConsentStatus {
        val pending = requested == ViraConsentStatus.PENDING
        val body = JsonObject(buildMap {
            put("requested", JsonPrimitive(if (pending) "pending" else "not_required"))
            if (pending) put("guardian", guardianEmail?.let(::JsonPrimitive) ?: JsonNull)
        })
        val raw = rest.rpc("declare_guardian_consent", body, accessToken)
        val value = runCatching { Json.parseToJsonElement(raw).jsonPrimitive.contentOrNull }.getOrNull()
        return ViraConsentStatus.fromServer(value)
    }

    // El correu a la persona adulta (2026-10-20)

    /// Demana el permís per correu (funció `guardian-consent`). Amb `guardianEmail`, primer el
    /// declara —el servidor fa la mateixa crida que `declare`— i després envia l'enllaç; sense,
    /// torna a enviar-lo a qui ja hi era.
    ///
    /// El límit de tres correus al dia **no és un error**: torna l'estat amb `LIMIT_REACHED`, perquè
    /// el permís ja ha quedat demanat i la pantalla ho ha de poder dir.
    suspend fun request(guardianEmail: String?, accessToken: String): ViraGuardianRequest {
        val body = JsonObject(buildMap {
            guardianEmail?.let { put("guardian", JsonPrimitive(it)) }
            put("locale", JsonPrimitive(locale()))
        })
        return call("POST", body, accessToken)
    }

    /// A qui s'ha demanat i com va, per a Ajustos.
    suspend fun summary(accessToken: String): ViraGuardianRequest = call("GET", null, accessToken)

    /// No passa per la traducció de PostgREST: aquí el que compta és el motiu que la funció posa a
    /// `error` (`same_email`, `rate_limited`).
    private suspend fun call(method: String, body: JsonElement?, accessToken: String): ViraGuardianRequest {
        val reply = rest.function(method, "guardian-consent", body, accessToken)
        val json = runCatching { Json.parseToJsonElement(reply.body).jsonObject }.getOrNull()
        // Els errors només porten `error`: sense `status` no hi ha estat a llegir.
        val withStatus = json?.takeIf { it["status"]?.jsonPrimitive?.contentOrNull != null }
        fun fail(error: ViraBackendError): Nothing = throw ViraBackendException(error)
        return when (reply.status) {
            200 -> withStatus?.let { parse(it, if (it.bool("sent") == true) Delivery.SENT else Delivery.NOT_SENT) }
                ?: fail(ViraBackendError.UNAVAILABLE)
            429 -> withStatus?.let { parse(it, Delivery.LIMIT_REACHED) } ?: fail(ViraBackendError.TOO_MANY_ATTEMPTS)
            401 -> fail(ViraBackendError.SESSION_EXPIRED)
            403 -> fail(ViraBackendError.NOT_ALLOWED)
            422 -> fail(
                if (json?.text("error") == "same_email") ViraBackendError.GUARDIAN_IS_YOU else ViraBackendError.INVALID_EMAIL,
            )
            else -> fail(ViraBackendError.UNAVAILABLE)
        }
    }

    private fun parse(json: JsonObject, delivery: Delivery) = ViraGuardianRequest(
        status = ViraConsentStatus.fromServer(json.text("status")),
        guardian = json.text("guardian"),
        sentAt = json.date("sent_at"),
        expiresAt = json.date("expires_at"),
        canResend = json.bool("can_resend") ?: false,
        delivery = delivery,
    )

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.bool(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.date(key: String) =
        text(key)?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
}

/// On és el permís demanat per correu: el que ensenyen el pas del permís i Ajustos
/// (`ViraGuardianRequest` de l'iPhone).
data class ViraGuardianRequest(
    val status: ViraConsentStatus,
    /// El correu de la persona adulta, **retallat** (`ma•••@gmail.com`).
    val guardian: String? = null,
    val sentAt: Instant? = null,
    /// `null` amb el permís demanat vol dir que l'últim enllaç ha caducat.
    val expiresAt: Instant? = null,
    val canResend: Boolean = false,
    val delivery: Delivery = Delivery.NOT_SENT,
) {
    enum class Delivery {
        /// El correu acaba de sortir.
        SENT,
        /// No s'ha enviat res en aquesta crida (consulta, o ja hi havia resposta).
        NOT_SENT,
        /// Ja n'han sortit tres en 24 hores. El permís segueix demanat.
        LIMIT_REACHED,
    }

    companion object {
        /// El permís s'ha desat però la funció del correu no ha respost (`EntryConsentWriter`).
        fun unsent(guardian: String) =
            ViraGuardianRequest(ViraConsentStatus.PENDING, mask(guardian), canResend = true)

        /// La mateixa regla que el servidor: dues lletres i el domini.
        fun mask(email: String): String {
            val at = email.lastIndexOf('@')
            if (at <= 0) return "•••"
            return email.substring(0, at).take(2) + "•••" + email.substring(at)
        }
    }
}
