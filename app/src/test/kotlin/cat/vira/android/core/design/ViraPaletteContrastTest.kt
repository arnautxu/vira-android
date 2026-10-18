package cat.vira.android.core.design

import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraPalette
import cat.vira.android.core.design.tokens.ViraShopBackdrop
import cat.vira.android.core.design.tokens.ViraTone
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/// Les parelles de color que l'iPhone documenta com a validades, mesurades aquí amb la fórmula de
/// WCAG en clar i en fosc. Si un to es copia malament de `ViraColor.swift`, salta aquí abans que a
/// cap pantalla (`ACCESSIBILITY_REQUIREMENTS.md`: 4,5:1 per al text, 3:1 per als contorns).
class ViraPaletteContrastTest {

    private val modes = listOf(false, true)

    @Test
    fun cardTextReadsOnEveryToneOfTheCardGradient() = eachArea { area, dark ->
        val ink = ViraPalette.onCard(area)
        listOf(ViraPalette.cardFillStart(area), ViraPalette.cardFill(area), ViraPalette.cardFillEnd(area))
            .forEach { assertText("onCard $area", ink, it, dark) }
    }

    @Test
    fun accentLabelReadsOnBothEndsOfTheAccentGradient() = eachArea { area, dark ->
        assertText("onAccent $area", ViraPalette.onAccent, ViraPalette.accent(area), dark)
        assertText("onAccent $area fi", ViraPalette.onAccent, ViraPalette.accentEnd(area), dark)
    }

    @Test
    fun textReadsOnTheSectionBackground() = eachArea { area, dark ->
        listOf(ViraPalette.surface(area), ViraPalette.surfaceShift(area), ViraPalette.backgroundGrouped).forEach {
            assertText("textPrimary $area", ViraPalette.textPrimary, it, dark)
            assertText("textSecondary $area", ViraPalette.textSecondary, it, dark)
            assertText("onSurface $area", ViraPalette.onSurface(area), it, dark)
        }
    }

    @Test
    fun accentTextReadsOnWhiteSurfaces() = eachArea { area, dark ->
        assertText("accent $area", ViraPalette.accent(area), ViraPalette.background, dark)
        assertText("accent $area elevada", ViraPalette.accent(area), ViraPalette.surfaceElevated, dark)
    }

    @Test
    fun neutralTextReadsOnNeutralSurfaces() = modes.forEach { dark ->
        listOf(ViraPalette.background, ViraPalette.backgroundGrouped, ViraPalette.surfaceElevated).forEach {
            assertText("textPrimary", ViraPalette.textPrimary, it, dark)
            assertText("textSecondary", ViraPalette.textSecondary, it, dark)
            assertText("error", ViraPalette.error, it, dark)
        }
    }

    @Test
    fun controlBorderIsVisibleAgainstTheBackground() = modes.forEach { dark ->
        listOf(ViraPalette.background, ViraPalette.backgroundGrouped).forEach {
            assertRatio("controlBorder", ViraPalette.controlBorder, it, dark, 3.0)
        }
    }

    @Test
    fun shopTextReadsOnEveryBackdrop() = modes.forEach { dark ->
        ViraShopBackdrop.entries.forEach { backdrop ->
            val ink = ViraPalette.onShopBackdrop(backdrop)
            assertText("botiga $backdrop", ink, ViraPalette.shopBackdrop(backdrop), dark)
            assertText("botiga $backdrop fi", ink, ViraPalette.shopBackdropEnd(backdrop), dark)
        }
    }

    @Test
    fun intensityLabelReadsOnBothEndsOfEveryLevel() = (1..5).forEach { level ->
        assertText("intensitat $level", ViraPalette.onIntensity(level), ViraPalette.intensityTop(level), false)
        assertText("intensitat $level fi", ViraPalette.onIntensity(level), ViraPalette.intensityBottom(level), false)
    }

    private fun eachArea(check: (ViraArea, Boolean) -> Unit) =
        modes.forEach { dark -> ViraArea.entries.forEach { check(it, dark) } }

    private fun assertText(name: String, ink: ViraTone, ground: ViraTone, dark: Boolean) =
        assertRatio(name, ink, ground, dark, 4.5)

    private fun assertRatio(name: String, ink: ViraTone, ground: ViraTone, dark: Boolean, minimum: Double) {
        val ratio = contrast(ink.pick(dark), ground.pick(dark))
        val mode = if (dark) "fosc" else "clar"
        assertTrue("$name ($mode): ${"%.2f".format(ratio)}:1, cal $minimum:1", ratio >= minimum)
    }

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val v = ((argb shr shift) and 0xFF) / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
