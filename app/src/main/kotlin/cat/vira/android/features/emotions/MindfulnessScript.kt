package cat.vira.android.features.emotions

import android.content.Context
import androidx.annotation.RawRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/// El guió amb temps d'un exercici en un idioma: què es diu i quan, perquè la pantalla ensenyi
/// exactament el que sona (`MindfulnessScript` de l'iPhone).
///
/// La pantalla no porta cap rellotge propi: pregunta al reproductor on és i en treu el tros
/// (`segment`). Així, pausar o una trucada al mig no poden desquadrar el text de la veu.
@Serializable
data class MindfulnessScript(
    val id: String,
    val language: String,
    val duration: Double,
    /// Passos amb nom («Les espatlles»). Zero vol dir que l'exercici és seguit i no es compta.
    val steps: Int,
    val segments: List<Segment>,
) {
    @Serializable
    data class Segment(
        val start: Double,
        val end: Double,
        val kind: Kind,
        /// Índex del pas (des de 0), o `null` abans del primer.
        val step: Int? = null,
        val stepTitle: String? = null,
        /// Clau del catàleg: la frase, o la paraula de la fase en una respiració.
        val text: String,
        val phase: Phase? = null,
    )

    @Serializable
    enum class Kind { @SerialName("line") LINE, @SerialName("breath") BREATH }

    @Serializable
    enum class Phase {
        @SerialName("inhale") INHALE,
        @SerialName("hold") HOLD,
        @SerialName("exhale") EXHALE,
        @SerialName("wait") WAIT,
    }

    fun segment(time: Double): Segment? {
        val first = segments.firstOrNull() ?: return null
        if (time < first.start) return first
        return segments.lastOrNull { it.start <= time } ?: first
    }

    /// Fins a quin punt s'ha fet el tros, de 0 a 1. Porta el cercle de la respiració.
    fun progress(segment: Segment, time: Double): Double {
        val length = segment.end - segment.start
        if (length <= 0) return 1.0
        return ((time - segment.start) / length).coerceIn(0.0, 1.0)
    }

    companion object {
        /// Els idiomes en què hi ha àudio. Un altre idioma del sistema cau al català, que és la
        /// llengua de partida del catàleg.
        val languages = listOf("ca", "es", "en")

        private val json = Json { ignoreUnknownKeys = true }

        /// L'idioma en què l'app ja ensenya els textos, perquè veu i pantalla coincideixin.
        fun currentLanguage(context: Context): String {
            val code = context.resources.configuration.locales[0].language
            return if (code in languages) code else "ca"
        }

        @RawRes
        fun scriptId(context: Context, exercise: MindfulnessExercise, language: String): Int =
            raw(context, "${exercise.resourceStem}_${language}_script")

        @RawRes
        fun audioId(context: Context, exercise: MindfulnessExercise, language: String = currentLanguage(context)): Int =
            raw(context, "${exercise.resourceStem}_$language")

        fun load(context: Context, exercise: MindfulnessExercise, language: String = currentLanguage(context)): MindfulnessScript? {
            val id = scriptId(context, exercise, language)
            if (id == 0) return null
            return runCatching {
                context.resources.openRawResource(id).bufferedReader().use { json.decodeFromString<MindfulnessScript>(it.readText()) }
            }.getOrNull()
        }

        fun decode(text: String): MindfulnessScript = json.decodeFromString(text)

        private fun raw(context: Context, name: String): Int =
            context.resources.getIdentifier(name, "raw", context.packageName)
    }
}
