package cat.vira.android.features.emotions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/// La part dels exercicis guiats que no depèn dels fitxers (`MindfulnessScriptTests` de l'iPhone).
class MindfulnessScriptTest {

    /// El començament de `mindfulness-breatheSquare-ca.json`, tal com el fa el generador.
    private val script = MindfulnessScript.decode(
        """
        {"id":"breatheSquare","language":"ca","duration":108.3,"steps":0,"segments":[
         {"start":0.6,"end":7.77,"kind":"line","step":null,"stepTitle":null,"text":"mindfulness.breatheSquare.line1"},
         {"start":7.77,"end":11.77,"kind":"breath","step":null,"stepTitle":null,"phase":"inhale","text":"help.breathe.inhale"},
         {"start":11.77,"end":15.77,"kind":"breath","step":null,"stepTitle":null,"phase":"hold","text":"help.breathe.hold"},
         {"start":15.77,"end":19.77,"kind":"breath","step":null,"stepTitle":null,"phase":"exhale","text":"help.breathe.exhale"},
         {"start":19.77,"end":23.77,"kind":"breath","step":null,"stepTitle":null,"phase":"wait","text":"mindfulness.breath.wait"}
        ]}
        """.trimIndent(),
    )

    @Test
    fun elTrosQueTocaSegonsElTemps() {
        val first = script.segments.first()
        assertEquals(first, script.segment(0.0))
        assertEquals(script.segments[1], script.segment(first.end + 0.01))
        assertEquals(script.segments.last(), script.segment(script.duration + 10))
        val breath = script.segments.first { it.kind == MindfulnessScript.Kind.BREATH }
        assertEquals(MindfulnessScript.Phase.INHALE, breath.phase)
        assertEquals(0.0, script.progress(breath, breath.start), 0.0)
        assertEquals(1.0, script.progress(breath, breath.end + 1), 0.0)
        assertNull(breath.step)
    }

    @Test
    fun elCercleSegueixLaFase() {
        val (inhale, hold, exhale, wait) = script.segments.drop(1)
        assertEquals(0f, fill(script, inhale, inhale.start), 0.001f)
        assertEquals(1f, fill(script, inhale, inhale.end), 0.001f)
        assertEquals(1f, fill(script, hold, hold.start), 0.001f)
        assertEquals(0f, fill(script, exhale, exhale.end), 0.001f)
        assertEquals(0f, fill(script, wait, wait.start), 0.001f)
        // Entre frases es queda a mig camí, quiet.
        assertEquals(0.6f, fill(script, script.segments.first(), 1.0), 0.001f)
    }

    @Test
    fun cadaCategoriaTeTresExercicis() {
        MindfulnessCategory.entries.forEach { assertEquals(3, it.exercises.size) }
        // Mindfulness no puntua ni porta enlloc més que a Concentra't des de «Prepara't».
        assertEquals(listOf(MindfulnessExercise.GET_READY), MindfulnessExercise.entries.filter { it.leadsToFocus })
    }

    @Test
    fun lesClausEsBusquenAmbLaRegladExportStrings() {
        assertEquals("mindfulness_breathe_square_line1", MindfulnessText.resourceName("mindfulness.breatheSquare.line1"))
        assertEquals("help_breathe_inhale", MindfulnessText.resourceName("help.breathe.inhale"))
        assertEquals("mindfulness_body_scan", MindfulnessExercise.BODY_SCAN.resourceStem)
        assertFalse(MindfulnessExercise.entries.any { it.resourceStem.any(Char::isUpperCase) })
    }
}
