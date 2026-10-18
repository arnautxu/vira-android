package cat.vira.android.core.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
class ViraGuardianConsent(private val rest: ViraRestClient) {

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
}
