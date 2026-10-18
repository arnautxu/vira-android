package cat.vira.android.features.emotions

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.R

/// Els nou exercicis guiats de Mindfulness, tres per categoria (`MindfulnessExercise` de l'iPhone).
///
/// El text i la veu surten de la mateixa font: cada exercici té un àudio i un guió amb temps per
/// idioma (`MindfulnessScript`), i aquí només hi ha el que la pantalla necessita abans de
/// començar. Cap exercici dona XP, monedes ni ratxa: és un moment per a l'usuari, no una tasca.
enum class MindfulnessExercise(val raw: String, val category: MindfulnessCategory, val minutes: Int) {
    BREATHE_SLOW("breatheSlow", MindfulnessCategory.ANXIETY, 1),
    BREATHE_SQUARE("breatheSquare", MindfulnessCategory.ANXIETY, 2),
    SENSES("senses", MindfulnessCategory.ANXIETY, 3),
    COUNT_TEN("countTen", MindfulnessCategory.FOCUS, 2),
    ONE_SOUND("oneSound", MindfulnessCategory.FOCUS, 1),
    GET_READY("getReady", MindfulnessCategory.FOCUS, 2),
    BODY_SCAN("bodyScan", MindfulnessCategory.DISCONNECT, 3),
    TENSE_RELEASE("tenseRelease", MindfulnessCategory.DISCONNECT, 3),
    PHONE_SLEEP("phoneSleep", MindfulnessCategory.DISCONNECT, 5);

    val titleKey: String get() = "mindfulness.$raw.title"
    val summaryKey: String get() = "mindfulness.$raw.summary"

    /// «Prepara't» acaba on comença la feina: el botó principal obre Concentra't.
    val leadsToFocus: Boolean get() = this == GET_READY

    /// Nom del recurs a `res/raw`, sense idioma: `mindfulness_breathe_slow`.
    val resourceStem: String get() = "mindfulness_" + MindfulnessText.resourceName(raw)
}

val MindfulnessCategory.exercises: List<MindfulnessExercise>
    get() = MindfulnessExercise.entries.filter { it.category == this }

/// «1–3 min» a la fitxa de la categoria.
@Composable
internal fun MindfulnessCategory.minutesRange(): String {
    val resources = LocalContext.current.resources
    val minutes = exercises.map { it.minutes }
    val low = minutes.min()
    val high = minutes.max()
    return if (low == high) resources.getString(R.string.mindfulness_minutes, low)
    else resources.getString(R.string.mindfulness_minutes_range, low, high)
}

/// Els textos del guió arriben com a claus de l'iPhone (`mindfulness.bodyScan.line3`,
/// `help.breathe.inhale`) i es busquen pel nom amb la mateixa regla que `export-strings.py`.
object MindfulnessText {
    fun resourceName(key: String): String = key
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    fun id(resources: Resources, key: String, packageName: String): Int =
        resources.getIdentifier(resourceName(key), "string", packageName)

    /// La clau tal qual si no és al catàleg: millor una pista llegible que un buit (les proves
    /// vigilen que no passi).
    fun string(context: Context, key: String): String {
        val id = id(context.resources, key, context.packageName)
        return if (id != 0) context.getString(id) else key
    }
}

@Composable
internal fun mindfulnessString(key: String): String = MindfulnessText.string(LocalContext.current, key)
