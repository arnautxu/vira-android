package cat.vira.android.core.backend

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/// Taules i funcions de Postgres via PostgREST (`/rest/v1`), amb HTTP pla
/// (`ViraRESTClient` + `ViraHTTPClient` de l'iPhone).
///
/// Aquí no hi ha cap comprovació de permisos: **totes** viuen a les polítiques RLS del servidor. Un
/// filtre de client és una comoditat, mai una protecció.
class ViraRestClient(
    private val config: ViraBackendConfig.Values? = ViraBackendConfig.current,
    private val http: OkHttpClient = defaultClient,
) {

    /// Una consulta és una llista de parells nom/valor, en ordre, com els `URLQueryItem`.
    data class Param(val name: String, val value: String)

    suspend fun select(table: String, query: List<Param>, accessToken: String): JsonArray {
        val items = if (query.any { it.name == "select" }) query else query + Param("select", "*")
        val body = send("GET", values().rest(table), items, accessToken, null, emptyMap())
        return ViraJson.json.parseToJsonElement(body.ifEmpty { "[]" }) as? JsonArray
            ?: throw ViraBackendException(ViraBackendError.UNAVAILABLE)
    }

    /// Escriptura idempotent: la sincronització pot repetir el mateix enviament sense duplicar res.
    ///
    /// Totes les files del lot porten **les mateixes claus** —les que falten s'omplen amb `null`—,
    /// o PostgREST rebutja el lot sencer amb «All object keys must match».
    suspend fun upsert(
        table: String,
        rows: List<JsonObject>,
        accessToken: String,
        onConflict: String = "id",
        nullColumns: Set<String> = emptySet(),
    ) {
        if (rows.isEmpty()) return
        val keys = rows.flatMap { it.keys }.toSet() + nullColumns
        val filled = JsonArray(rows.map { row -> JsonObject(keys.associateWith { row[it] ?: JsonNull }) })
        send(
            "POST", values().rest(table), listOf(Param("on_conflict", onConflict)), accessToken,
            filled.toString(),
            // `merge-duplicates` converteix l'INSERT en UPSERT; `minimal`, que no ens torni el lot.
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    suspend fun delete(table: String, query: List<Param>, accessToken: String) {
        send("DELETE", values().rest(table), query, accessToken, null, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun rpc(function: String, body: JsonElement, accessToken: String): String =
        send("POST", values().rpc(function), emptyList(), accessToken, body.toString(), emptyMap())

    private fun values() = config ?: throw ViraBackendException(ViraBackendError.NOT_CONFIGURED)

    private suspend fun send(
        method: String,
        base: String,
        query: List<Param>,
        accessToken: String?,
        body: String?,
        headers: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val config = values()
        val url = if (query.isEmpty()) base else base + "?" + query.joinToString("&") {
            "${encode(it.name)}=${encode(it.value)}"
        }
        val request = Request.Builder()
            .url(url)
            .method(method, body?.toRequestBody(jsonType))
            .header("apikey", config.anonKey)
            // Sense sessió, la clau anon fa d'autorització: és el que espera PostgREST per aplicar
            // les polítiques del rol anònim.
            .header("Authorization", "Bearer ${accessToken ?: config.anonKey}")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        val response = try {
            http.newCall(request).execute()
        } catch (error: IOException) {
            throw ViraBackendException(ViraBackendError.fromNetwork(error), error)
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val code = errorCode(text)
                // La ruta i el codi, sí: diuen quina taula i quina regla. **El text no**: PostgREST hi
                // posa el valor que ha fallat, i aquí aquest valor pot ser una nota del check-in.
                log("$method ${it.request.url.encodedPath} → ${it.code} ${code ?: "-"}")
                throw ViraBackendException(ViraBackendError.fromDatabase(it.code, code))
            }
            text
        }
    }

    private fun errorCode(text: String): String? = try {
        ViraJson.json.parseToJsonElement(text).jsonObject["code"]?.jsonPrimitive?.contentOrNull
    } catch (_: Exception) {
        null
    }

    private fun log(line: String) {
        try {
            Log.w("ViraBackend", line)
        } catch (_: RuntimeException) {
            // Proves de JVM sense Android a sota.
        }
    }

    companion object {
        private val jsonType = "application/json".toMediaType()

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
        }

        /// Codifica **tot** el que no és alfanumèric ni `-._~`. Un `+` sense codificar PostgREST el
        /// llegeix com un espai, i les dates exactes de les pàgines poden portar `+00:00`.
        fun encode(value: String): String = buildString {
            for (byte in value.toByteArray(Charsets.UTF_8)) {
                val char = byte.toInt().toChar()
                if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
                    append(char)
                } else {
                    append('%').append("%02X".format(byte.toInt() and 0xFF))
                }
            }
        }
    }
}
