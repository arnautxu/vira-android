package cat.vira.android.features.parent

import cat.vira.android.core.backend.ViraRestClient
import cat.vira.android.core.backend.ViraRestClient.Param
import cat.vira.android.core.data.ViraTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.time.Duration
import java.time.Instant
import java.util.UUID

/// Carrega el resum (`ParentChildSummaryLoader` de l'iPhone). Cada consulta va per separat perquè
/// el servidor pot dir que no a unes i que sí a altres: és exactament el que ha de passar quan un
/// àmbit està tancat.
class ParentChildSummaryLoader(
    private val rest: ViraRestClient = ViraRestClient(),
    private val clock: () -> Instant = ViraTime::now,
) {

    suspend fun load(link: ParentLink, accessToken: String): ParentChildSummary {
        var summary = ParentChildSummary()
        val child = link.childUserId
        val weekAgo = clock().minus(Duration.ofDays(7))

        if (link.scopeUsage) {
            val profiles = rows("profiles", child, accessToken, "display_name")
            // Només l'XP, per una funció: la fila sencera de `progress_counters` porta també el
            // recompte d'emocions, que és de l'àmbit d'emocions (`20261018122414`).
            val xp = count("parent_child_total_xp", child, accessToken)
            val streaks = rows("streaks", child, accessToken, "current_days")
            val sessions = rows("focus_sessions", child, accessToken, "started_at,completed")
            summary = summary.copy(
                displayName = profiles.firstOrNull()?.string("display_name"),
                totalXP = xp,
                streakDays = streaks.firstOrNull()?.int("current_days") ?: 0,
                focusSessionsLastWeek = sessions.count {
                    (it.instant("started_at")?.isAfter(weekAgo) ?: false) && it.bool("completed") == true
                },
            )
        }

        if (link.scopeHabits) {
            val logs = rows("habit_logs", child, accessToken, "day")
            val items = rows("plan_items", child, accessToken, "status")
            summary = summary.copy(
                habitsDoneLastWeek = logs.count { it.instant("day")?.isAfter(weekAgo) ?: false },
                tasksPending = items.count { it.string("status") == "pending" },
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
    /// l'arrel. Sense aquest àmbit el perfil no es llegeix i la targeta diu «Fill o filla».
    /// Un error aquí no para el panell: un nom que no arriba no amaga cap dada.
    suspend fun displayNames(links: List<ParentLink>, accessToken: String): Map<UUID, String> = coroutineScope {
        links.filter { it.scopeUsage }.map { link ->
            async {
                val name = runCatching { rows("profiles", link.childUserId, accessToken, "display_name") }
                    .getOrNull()?.firstOrNull()?.string("display_name")?.trim()
                if (name.isNullOrEmpty()) null else link.childUserId to name
            }
        }.awaitAll().filterNotNull().toMap()
    }

    /// `null` quan el servidor diu `null`: l'àmbit és obert però el permís del tutor no.
    private suspend fun count(function: String, child: UUID, token: String): Int? {
        val body = JsonObject(mapOf("child" to JsonPrimitive(child.toString())))
        return ParentLinkClient.scalar(rest.rpc(function, body, token))?.toIntOrNull()
    }

    /// Només les columnes que fan falta per comptar: el títol o les notes d'una tasca no han de
    /// baixar mai al telèfon del pare o la mare, encara que la política del servidor les deixi.
    private suspend fun rows(table: String, child: UUID, token: String, columns: String): List<JsonObject> =
        rest.select(table, listOf(Param("user_id", "eq.$child"), Param("select", columns)), token)
            .mapNotNull { it as? JsonObject }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.bool(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.instant(key: String) = string(key)?.let(ViraTime::parse)
}
