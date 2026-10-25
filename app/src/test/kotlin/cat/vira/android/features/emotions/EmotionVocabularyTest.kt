package cat.vira.android.features.emotions

import cat.vira.android.core.data.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/// Les sis emocions agradables del 2026-10-23 (`Emotion.swift` de l'iPhone).
class EmotionVocabularyTest {

    private val added = listOf(Emotion.CALM, Emotion.PROUD, Emotion.MOTIVATED, Emotion.GRATEFUL, Emotion.EXCITED, Emotion.RELIEVED)

    @Test
    fun dinouEmocionsAmbElMateixValorEnCruQueLiPhone() {
        assertEquals(19, Emotion.entries.size)
        assertEquals(listOf("calm", "proud", "motivated", "grateful", "excited", "relieved"), added.map { it.raw })
    }

    @Test
    fun laGraellaDeSortidaSonTresAgradablesITresDesagradables() {
        assertEquals(listOf(Emotion.CONTENT, Emotion.CALM, Emotion.PROUD, Emotion.SAD, Emotion.ANXIOUS, Emotion.ANGRY), Emotion.primary)
        assertEquals(listOf(false, false, false, true, true, true), Emotion.primary.map { it.isUnpleasant })
    }

    @Test
    fun lesNovesNoOfereixenAjudaNiTenenVideo() {
        added.forEach {
            assertFalse(offersSupport(it, EmotionIntensity.VERY_MUCH))
            assertNull(it.video)
            assertNull(it.still)
        }
    }
}
