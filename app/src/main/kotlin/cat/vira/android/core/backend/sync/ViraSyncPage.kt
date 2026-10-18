package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.ViraRestClient.Param
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/// Paginació per data exacta i clau, no per offset (`ViraSyncPage` de l'iPhone).
///
/// La frontera es llegeix **en cru** del JSON: una data convertida pot perdre els microsegons, i el
/// filtre de la pàgina següent no s'ho pot permetre.
object ViraSyncPage {

    fun boundary(row: JsonObject, tieColumns: List<String>): List<Pair<String, String>> =
        (listOf("updated_at") + tieColumns).map { column -> column to row.getValue(column).jsonPrimitive.content }

    /// Comparació lexicogràfica `(data, clau, subclau) > frontera`, en la sintaxi de PostgREST.
    fun after(fields: List<Pair<String, String>>): Param {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val branches = fields.indices.map { index ->
            val equals = fields.take(index).map { (column, value) -> "$column.eq.${literal(value)}" }
            val greater = "${fields[index].first}.gt.${literal(fields[index].second)}"
            if (equals.isEmpty()) greater else "and(${(equals + greater).joinToString(",")})"
        }
        return Param("or", "(${branches.joinToString(",")})")
    }
}

/// Columnes opcionals que porten les files completes. L'absència de valor és una ordre de buidar,
/// també quan **tot** el lot porta `null` (`ViraSyncNullColumns` de l'iPhone).
object ViraSyncNullColumns {
    fun forTable(table: String): Set<String> = when (table) {
        "plan_items" -> setOf("notes", "time", "completed_at", "reminder_offset_minutes")
        "plan_assistance_details" -> setOf("parent_plan_item_id", "step_order", "estimated_minutes")
        "plan_assistance_links" -> setOf("before_plan_item_id")
        "habits" -> setOf("notes", "reminder_time")
        "habit_logs" -> setOf("habit_id")
        "focus_profiles" -> setOf("ambient_sound")
        "focus_sessions" -> setOf("ended_at", "linked_item_title", "profile_id", "profile_name", "profile_break_minutes")
        "streaks" -> setOf("last_active_day")
        "emotion_entries" -> setOf("intensity", "note")
        "medications" -> setOf("dose", "ended_at")
        "medication_intakes" -> setOf("medication_id", "logged_at")
        else -> emptySet()
    }
}
