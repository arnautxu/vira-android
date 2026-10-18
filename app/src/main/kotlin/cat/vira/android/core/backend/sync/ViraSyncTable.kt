package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.ViraJson
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.ViraSyncable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant

/// Fotografia d'una fila per enviar-la o rebre-la (`ViraSyncRow` de l'iPhone).
///
/// És una família a part dels DTO d'exportació: l'exportació és una còpia per a l'usuari, i la
/// sincronització necessita `updatedAt`, claus naturals i camps que l'exportació no mira.
interface ViraSyncRow {
    val updatedAt: Instant
}

/// Singleton acabat de crear que ningú ha tocat aquí. **No s'envia mai**: un comptador buit no ha
/// de trepitjar la història del compte.
val ViraSyncRow.isPlaceholder: Boolean get() = updatedAt == ViraTime.distantPast

/// Una taula del servidor i com es llegeix i s'escriu al telèfon.
///
/// Rep el lot sencer a `apply` i no fila a fila perquè cada taula pugui llegir els models locals
/// **una sola vegada** i indexar-los.
abstract class ViraSyncTable<R : ViraSyncRow>(
    val table: String,
    val layer: ViraSyncLayer,
    val serializer: KSerializer<R>,
    /// Columnes de la clau primària, per a l'`upsert`.
    val conflictTarget: String = "id",
) {
    abstract suspend fun local(db: ViraDatabase): List<R>
    abstract suspend fun apply(rows: List<R>, db: ViraDatabase)

    /// Després de la baixada sencera de la taula (totes les pàgines, o cap si no hi havia res de nou).
    open suspend fun afterPull(db: ViraDatabase) {}

    fun encode(row: R): JsonObject = ViraJson.json.encodeToJsonElement(serializer, row).jsonObject
    fun decode(row: JsonObject): R = ViraJson.json.decodeFromJsonElement(serializer, row)

    /// Les columnes de la clau, sense `user_id`: són les que desempaten una pàgina.
    val tieColumns: List<String> get() = conflictTarget.split(",").filter { it != "user_id" }

    /// Ordre de les pàgines: data i clau, perquè la següent comenci exactament on acaba l'anterior.
    val order: String get() = (listOf("updated_at") + tieColumns).joinToString(",") { "$it.asc" }
}

/// L'últim escriu (`ViraSyncApply.shouldOverwrite`). Si el que ja hi ha al telèfon és més nou, la
/// fila del servidor s'ignora.
///
/// **Amb una excepció: el sentinella sempre perd**, fins i tot contra una fila del servidor que
/// també el porti. És el que recupera les files que van quedar congelades a `0001-01-01`.
fun shouldOverwrite(row: ViraSyncRow, model: ViraSyncable<*>): Boolean {
    if (model.updatedAt == ViraTime.distantPast) return true
    return row.updatedAt > model.updatedAt
}
