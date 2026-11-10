package cat.vira.android.features.parent

import cat.vira.android.core.backend.ViraJson
import cat.vira.android.core.backend.ViraRestClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.util.UUID

/// Carrega el resum (`ParentChildSummaryLoader` de l'iPhone). **Només números**: el servidor compta
/// (`parent_child_summary`, revisió del 2026-11-09) i les files del fill o filla (tasques amb títol i
/// notes, hàbits, sessions) no baixen mai al telèfon del pare o la mare. Les emocions i la medicació
/// van per les seves funcions, que a més miren el permís del tutor.
class ParentChildSummaryLoader(
    private val rest: ViraRestClient = ViraRestClient(),
) {

    suspend fun load(link: ParentLink, accessToken: String): ParentChildSummary {
        var summary = ParentChildSummary()
        val child = link.childUserId

        // Una clau que no arriba és un àmbit tancat, i això ho decideix el servidor, no la còpia
        // del vincle que hi hagi al telèfon.
        if (link.scopeUsage || link.scopeHabits) {
            val counts = rpcObject("parent_child_summary", child, accessToken)
            summary = summary.copy(
                displayName = counts?.string("display_name"),
                totalXP = counts?.int("total_xp"),
                streakDays = counts?.int("streak_days"),
                focusSessionsLastWeek = counts?.int("focus_last_week"),
                habitsDoneLastWeek = counts?.int("habits_last_week"),
                tasksPending = counts?.int("tasks_pending"),
            )
        }

        // Un recompte, no el contingut. Que un pare/mare sàpiga que el seu fill fa check-ins és
        // una cosa; llegir què hi ha escrit, una altra de molt més gran
        // (`PRIVACY_AND_SAFETY.md` §2). Per això el servidor no dona les files, només aquests dos
        // números (`20261018122414`), i sense el permís del tutor vigent torna `null`: això és
        // "no ho pots veure", no un zero.
        if (link.scopeEmotions) {
            summary = summary.copy(
                emotionCheckinsLastWeek = count("parent_child_emotion_checkins_last_week", child, accessToken),
            )
        }

        if (link.scopeMedication) {
            summary = summary.copy(
                activeMedications = count("parent_child_active_medications", child, accessToken),
            )
        }

        return summary
    }

    /// El pseudònim de cada fill o filla que hagi obert «Ús i ratxa», per a les targetes de
    /// l'arrel (`parent_child_names`). Sense aquest àmbit la targeta diu «Fill o filla».
    /// Un error aquí no para el panell: un nom que no arriba no amaga cap dada.
    suspend fun displayNames(links: List<ParentLink>, accessToken: String): Map<UUID, String> {
        val children = links.filter { it.scopeUsage }.map { it.childUserId }.toSet()
        if (children.isEmpty()) return emptyMap()
        val raw = runCatching { rest.rpc("parent_child_names", JsonObject(emptyMap()), accessToken) }.getOrNull()
            ?: return emptyMap()
        val rows = runCatching { ViraJson.json.parseToJsonElement(raw) as? JsonArray }.getOrNull() ?: return emptyMap()
        return rows.mapNotNull { it as? JsonObject }.mapNotNull { row ->
            val id = row.string("child_user_id")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val name = row.string("display_name")?.trim()
            if (id == null || id !in children || name.isNullOrEmpty()) null else id to name
        }.toMap()
    }

    /// `null` quan el servidor diu `null`: l'àmbit és obert però el permís del tutor no.
    private suspend fun count(function: String, child: UUID, token: String): Int? {
        val body = JsonObject(mapOf("child" to JsonPrimitive(child.toString())))
        return ParentLinkClient.scalar(rest.rpc(function, body, token))?.toIntOrNull()
    }

    private suspend fun rpcObject(function: String, child: UUID, token: String): JsonObject? {
        val body = JsonObject(mapOf("child" to JsonPrimitive(child.toString())))
        val raw = rest.rpc(function, body, token)
        return runCatching { ViraJson.json.parseToJsonElement(raw.ifBlank { "null" }) as? JsonObject }.getOrNull()
    }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
}
