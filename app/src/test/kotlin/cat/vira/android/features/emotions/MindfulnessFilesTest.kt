package cat.vira.android.features.emotions

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/// Els àudios i els guions de Mindfulness (`MindfulnessScriptTests` de l'iPhone): el que es
/// trencaria sense fer soroll en tornar-los a exportar. Un fitxer que falta en un idioma, un text
/// que surt com a clau crua o una durada que ja no és la que diu la llista.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MindfulnessFilesTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private fun script(exercise: MindfulnessExercise, language: String): MindfulnessScript {
        val script = MindfulnessScript.load(context, exercise, language)
        assertNotNull("falta el guió de ${exercise.raw} en $language", script)
        return script!!
    }

    @Test
    fun cadaExerciciTeGuioIAudioEnElsTresIdiomes() {
        for (exercise in MindfulnessExercise.entries) for (language in MindfulnessScript.languages) {
            val script = script(exercise, language)
            assertEquals(exercise.raw, script.id)
            assertEquals(language, script.language)
            assertNotEquals("falta l'àudio de ${exercise.raw} en $language", 0, MindfulnessScript.audioId(context, exercise, language))
        }
    }

    /// La llista diu «3 min»: l'àudio no pot durar-ne cinc ni un.
    @Test
    fun laDuradaEsLaQueDiuLaLlista() {
        for (exercise in MindfulnessExercise.entries) for (language in MindfulnessScript.languages) {
            val minutes = script(exercise, language).duration / 60
            assertTrue("${exercise.raw}/$language dura $minutes min", abs(minutes - exercise.minutes) <= 0.75)
        }
    }

    @Test
    fun elsTrossosVanSeguitsISenseForats() {
        for (exercise in MindfulnessExercise.entries) for (language in MindfulnessScript.languages) {
            val script = script(exercise, language)
            assertTrue(script.segments.isNotEmpty())
            script.segments.zipWithNext { previous, next ->
                assertTrue(previous.start < previous.end)
                assertTrue(abs(previous.end - next.start) < 0.05)
            }
            assertTrue(script.segments.last().end <= script.duration + 0.5)
            assertEquals(script.steps, script.segments.mapNotNull { it.step }.toSet().size)
        }
    }

    @Test
    fun capTextDelGuioSurtComAClauCrua() {
        for (exercise in MindfulnessExercise.entries) {
            val script = script(exercise, "ca")
            val keys = listOf(exercise.titleKey, exercise.summaryKey) +
                script.segments.map { it.text } + script.segments.mapNotNull { it.stepTitle }
            for (key in keys) {
                assertNotEquals("$key no és al catàleg", 0, MindfulnessText.id(context.resources, key, context.packageName))
            }
        }
    }
}
