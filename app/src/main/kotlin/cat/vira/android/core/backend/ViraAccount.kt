package cat.vira.android.core.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/// Qui és qui, per a la interfície. Viu a `user_metadata` del compte perquè torni en canviar de
/// telèfon, però **no és un permís**: l'usuari el pot escriure, i cap política RLS el llegeix. El que
/// dona accés a un pare o una mare és la vinculació per codi (`parent_links`, migració 0003).
enum class ViraAccountRole(val raw: String) {
    /// L'adolescent que fa servir Vira.
    TEEN("teen"),
    /// Pare o mare, amb el seu propi compte.
    PARENT("parent");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw }
    }
}

/// Estat del permís del tutor (`PRIVACY_AND_SAFETY.md` §4.5b, D-29).
///
/// Mentre no sigui `GRANTED`, **cap dada sensible surt del telèfon**, decideixi el que decideixi
/// l'usuari als interruptors (`ViraSyncScope`).
enum class ViraConsentStatus(val raw: String) {
    /// No cal (16 anys o més) o ja atorgat.
    GRANTED("granted"),
    /// Demanat, encara sense resposta.
    PENDING("pending"),
    /// Ni demanat.
    MISSING("missing");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw }

        /// Traducció des de `guardian_consents.status`. Un valor que no es reconeix és `MISSING`:
        /// davant del dubte, la resposta és no.
        fun fromServer(status: String?) = when (status) {
            "granted", "not_required" -> GRANTED
            "pending" -> PENDING
            else -> MISSING
        }
    }
}

/// L'usuari, retallat al que Vira necessita. El correu és l'únic que Vira arriba a saber d'una
/// persona, i només perquè és la clau del compte.
data class ViraUser(
    val id: UUID,
    val email: String?,
    val role: ViraAccountRole = ViraAccountRole.TEEN,
    val consent: ViraConsentStatus = ViraConsentStatus.MISSING,
)

/// El que Vira desa a `user_metadata`. Només les claus que tenen valor: escriure `null` esborraria
/// la de l'altre telèfon.
data class ViraUserMetadata(val role: ViraAccountRole? = null, val consent: ViraConsentStatus? = null) {

    val json: JsonObject get() = JsonObject(buildMap {
        role?.let { put("role", JsonPrimitive(it.raw)) }
        consent?.let { put("consent", JsonPrimitive(it.raw)) }
    })

    companion object {
        fun from(json: JsonObject?): ViraUserMetadata = ViraUserMetadata(
            role = ViraAccountRole.from(json?.get("role")?.jsonPrimitive?.contentOrNull),
            consent = ViraConsentStatus.from(json?.get("consent")?.jsonPrimitive?.contentOrNull),
        )
    }
}
