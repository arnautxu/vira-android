package cat.vira.android.features.assist

import cat.vira.android.core.backend.ViraBackendConfig
import cat.vira.android.core.backend.ViraJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/// El transport de les ajudes de Viri (`ViriAssistClient` i `ViraAssistanceClient.scope` de
/// l'iPhone): una petició a una funció del nostre servidor i prou. Sense memòria cau, galetes ni
/// redireccions, i mai es registra el cos.
class ViriAssistClient(
    private val config: ViraBackendConfig.Values? = ViraBackendConfig.current,
    private val http: OkHttpClient = defaultClient,
) {

    /// «Parteix-la»: els passos que proposa Viri per a un títol.
    suspend fun steps(request: JsonObject, token: String): List<ViriAssist.StepProposal> {
        val json = send("viri-assist", request, token, limit = 32_768, failure = ::assistFailure)
        return parseSteps(json) ?: throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
    }

    /// «És gran?» a Jev (C01), l'única pregunta a Jev oberta: només el títol i l'idioma.
    /// Torna `broad`, `concrete` o `unknown`.
    suspend fun scope(title: String, language: String, token: String): String {
        val requestId = UUID.randomUUID().toString()
        val body = JsonObject(
            mapOf(
                "request_id" to JsonPrimitive(requestId),
                "contract_version" to JsonPrimitive(2),
                "case_id" to JsonPrimitive(SCOPE_CASE),
                "locale" to JsonPrimitive(language.takeIf { it in listOf("ca", "es", "en") } ?: "en"),
                "text" to JsonPrimitive(title),
                "candidates" to JsonArray(emptyList()),
                "consent_version" to JsonPrimitive(JEV_CONSENT),
            ),
        )
        val json = send("assistance-evaluate", body, token, limit = 8_192, failure = ::assistFailure)
        return parseScope(json, requestId) ?: throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
    }

    private suspend fun send(
        function: String,
        body: JsonObject,
        token: String,
        limit: Int,
        failure: (Int) -> ViriAssist.Failure,
    ): JsonObject = withContext(Dispatchers.IO) {
        val config = config ?: throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
        val request = Request.Builder()
            .url("${config.baseUrl}/functions/v1/$function")
            .post(body.toString().toRequestBody(jsonType))
            .header("Authorization", "Bearer $token")
            .header("apikey", config.anonKey)
            .header("Cache-Control", "no-store")
            .build()
        val response = try {
            http.newCall(request).execute()
        } catch (_: IOException) {
            throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
        }
        response.use {
            if (it.code != 200) throw ViriAssist.FailureException(failure(it.code))
            val source = it.body?.source() ?: throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
            // Una resposta més llarga del que pot ser no es llegeix sencera.
            if (source.request(limit.toLong() + 1)) throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
            val text = source.readUtf8()
            runCatching { ViraJson.json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: throw ViriAssist.FailureException(ViriAssist.Failure.UNAVAILABLE)
        }
    }

    companion object {
        const val SCOPE_CASE = "C01"
        const val JEV_POLICY = "assistance-catalog-1"
        const val JEV_MODEL = "jev-1.13.0"
        /// Identificador del contracte que espera el servidor, no cap registre d'acceptació.
        const val JEV_CONSENT = "reviewed-fragments-2026-09-24"

        private val jsonType = "application/json".toMediaType()

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                // El servidor talla a 25 s; aquí una mica més perquè l'error que arribi sigui el seu.
                .callTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }

        fun assistFailure(status: Int) = when (status) {
            401 -> ViriAssist.Failure.SESSION
            403 -> ViriAssist.Failure.CONSENT
            429 -> ViriAssist.Failure.LIMIT
            else -> ViriAssist.Failure.UNAVAILABLE
        }

        /// Els passos, si la resposta és la de la política d'ara i del tipus demanat.
        fun parseSteps(json: JsonObject): List<ViriAssist.StepProposal>? {
            if (json.text("policy_version") != ViriAssist.POLICY || json.text("kind") != "steps") return null
            val steps = runCatching { json["steps"]?.jsonArray }.getOrNull() ?: return null
            return steps.mapNotNull { element ->
                val step = element as? JsonObject ?: return@mapNotNull null
                val title = step.text("title") ?: return@mapNotNull null
                val minutes = (step["minutes"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
                ViriAssist.StepProposal(title, minutes)
            }
        }

        /// La resposta de Jev, només si és la d'aquesta pregunta i diu una de les tres opcions.
        fun parseScope(json: JsonObject, requestId: String): String? {
            val results = runCatching { json["results"]?.jsonArray }.getOrNull() ?: return null
            val ok = json.text("request_id").equals(requestId, ignoreCase = true) &&
                (json["contract_version"] as? JsonPrimitive)?.intOrNull == 2 &&
                json.text("case_id") == SCOPE_CASE &&
                json.text("policy_version") == JEV_POLICY &&
                json.text("model_version") == JEV_MODEL &&
                results.size == 1
            if (!ok) return null
            val result = results.first() as? JsonObject ?: return null
            val value = result.text("value")
            val noScore = result["score"].let { it == null || it is JsonNull }
            return value.takeIf { result.text("id") == "result" && noScore && it in setOf("concrete", "broad", "unknown") }
        }

        private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
