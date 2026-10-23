package cat.vira.android.core.sound

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import cat.vira.android.core.design.components.ViraSceneFraming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/// Els sons de Vira (`ViraSoundTests` de l'iPhone): un fitxer que falta, un so freqüent que
/// s'escola amb «Només celebracions» o l'escala que no torna a començar no fan soroll.
@RunWith(RobolectricTestRunner::class)
class ViraSoundTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allSounds: List<String>
        get() = ViraSound.Cue.entries.map { it.file } + ViraSound.scale + ViraSound.scaleCombo +
            ViraSound.viriVoices + ViraSound.coinLandings + ViraSound.entryNotes + listOf("sound-avis-vira")

    @Test
    fun everyCueHasItsFile() {
        allSounds.forEach { name ->
            val id = context.resources.getIdentifier(name.replace('-', '_'), "raw", context.packageName)
            assertNotEquals("falta $name", 0, id)
        }
    }

    /// Tots porten la vibració de l'iPhone, menys el comptador (va amb la remuntada) i la veu de
    /// Viri (és Viri qui parla, no una cosa que hagis fet tu).
    @Test
    fun everySoundHasItsVibration() {
        val silent = ViraSound.viriVoices + ViraSound.Cue.COUNT_UP.file
        allSounds.filter { it !in silent }.forEach { assertTrue("falta l'ona de $it", ViraHaptics.has(it)) }
        silent.forEach { assertFalse("$it no ha de vibrar", ViraHaptics.has(it)) }
    }

    @Test
    fun wavesAreValid() {
        ViraHapticPatternData.waves.forEach { (name, wave) ->
            assertEquals(name, wave.timings.size, wave.amplitudes.size)
            assertTrue(name, wave.amplitudes.all { it in 0..255 })
            assertTrue(name, wave.amplitudes.any { it > 0 })
            // Sense amplitud, la mateixa ona com a encès i apagat dura el mateix.
            assertEquals(name, wave.timings.sum(), ViraHaptics.onOff(wave).sum())
        }
    }

    @Test
    fun comboNeedsAPreviousQuickMark() {
        assertEquals("sound-tasca-3-combo", ViraSound.scaleFile(3, 1_000))
        assertEquals("sound-tasca-3", ViraSound.scaleFile(3, 4_000))
        assertEquals("sound-tasca-3", ViraSound.scaleFile(3, null))
        assertEquals("sound-tasca-1", ViraSound.scaleFile(1, 200))
        assertEquals("sound-tasca-2-combo", ViraSound.scaleFile(12, 1_000))
    }

    @Test
    fun scaleClimbsAndWraps() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1), (1..12).map { ViraSound.scaleIndex(it) })
        assertEquals(0, ViraSound.scaleIndex(0))
    }

    @Test
    fun nightModeHours() {
        assertTrue(listOf(22, 23, 0, 3, 6).all { ViraSoundEngine.isNight(it) })
        assertTrue(listOf(7, 12, 19, 21).none { ViraSoundEngine.isNight(it) })
    }

    /// ±8 cèntims és gairebé res: la nota no surt de l'escala.
    @Test
    fun variationStaysInTune() {
        assertEquals(1f, ViraSoundEngine.rate(0.0), 0.0001f)
        assertTrue(ViraSoundEngine.rate(8.0) in 1.004f..1.005f)
        assertTrue(ViraSoundEngine.rate(-8.0) in 0.995f..0.996f)
    }

    @Test
    fun celebrationsModeKeepsOnlyBigMoments() {
        val celebrations = ViraSound.Mode.CELEBRATIONS
        listOf(ViraSound.Cue.STREAK_TODAY, ViraSound.Cue.LEVEL_UP, ViraSound.Cue.FOCUS_END).forEach { assertTrue(celebrations.allows(it)) }
        listOf(ViraSound.Cue.SAVED, ViraSound.Cue.UNDO, ViraSound.Cue.COINS, ViraSound.Cue.COINS_LANDED).forEach { assertFalse(celebrations.allows(it)) }
        assertTrue(ViraSound.Cue.entries.none { ViraSound.Mode.OFF.allows(it) })
        assertTrue(ViraSound.Cue.entries.all { ViraSound.Mode.ALL.allows(it) })
        assertEquals(ViraSound.Mode.ALL, ViraSound.Mode.from(null))
        assertEquals(ViraSound.Mode.OFF, ViraSound.Mode.from("off"))
    }

    @Test
    fun dayClosesOnlyWithEverythingDone() {
        assertFalse(ViraDaySound.closesDay(1, 1))
        assertFalse(ViraDaySound.closesDay(3, 2))
        assertTrue(ViraDaySound.closesDay(3, 3))
        assertFalse(ViraDaySound.closesDay(0, 0))
    }

    /// La finestra del bàner va a buscar Viri, però mai surt de l'escena.
    @Test
    fun sceneWindowStaysInside() {
        ViraSceneFraming.viri.keys.forEach { name ->
            listOf(0.5f, 0.62f, 0.72f, 1f).forEach { ratio ->
                val top = ViraSceneFraming.top(name, ratio, 0.05f)
                assertTrue("$name $ratio", top >= 0f && top + ratio <= 1.0001f)
            }
        }
        // Una escena desconeguda queda centrada.
        assertEquals(0.25f, ViraSceneFraming.top("viri_x", 0.5f, 0f), 0.0001f)
        assertEquals(0.66f, ViraSceneFraming.heightRatio("viri_x", 0.66f), 0.0001f)
    }

    /// Les escenes de `ViraSceneFraming` existeixen amb aquest nom: si no, la finestra no la troba.
    @Test
    fun framedScenesExist() {
        (ViraSceneFraming.viri.keys + ViraSceneFraming.tiles.keys).forEach { name ->
            assertNotEquals("falta $name", 0, context.resources.getIdentifier(name, "drawable", context.packageName))
        }
    }
}
