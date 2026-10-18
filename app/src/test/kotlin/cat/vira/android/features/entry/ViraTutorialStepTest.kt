package cat.vira.android.features.entry

import cat.vira.android.app.ViraTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/// El recorregut del tutorial (`TutorialStepTests` de l'iPhone): **l'ordre i les tres seccions**.
class ViraTutorialStepTest {

    @Test
    fun `tres seccions, les integracions i els widgets, en aquest ordre`() {
        assertEquals(
            listOf(ViraTutorialStep.PLAN, ViraTutorialStep.EMOTIONS, ViraTutorialStep.PROFILE, ViraTutorialStep.INTEGRATIONS, ViraTutorialStep.WIDGETS),
            ViraTutorialStep.entries,
        )
        assertEquals(ViraTutorialStep.EMOTIONS, ViraTutorialStep.PLAN.next)
        assertEquals(ViraTutorialStep.PROFILE, ViraTutorialStep.EMOTIONS.next)
        assertEquals(ViraTutorialStep.INTEGRATIONS, ViraTutorialStep.PROFILE.next)
        assertEquals(ViraTutorialStep.WIDGETS, ViraTutorialStep.INTEGRATIONS.next)
        assertNull(ViraTutorialStep.WIDGETS.next)
    }

    @Test
    fun `cada parada assenyala la seva icona, i inici mai`() {
        assertEquals(ViraTab.Plan, ViraTutorialStep.PLAN.tab)
        assertEquals(ViraTab.Emotions, ViraTutorialStep.EMOTIONS.tab)
        assertEquals(ViraTab.Progress, ViraTutorialStep.PROFILE.tab)
        assertNull(ViraTutorialStep.INTEGRATIONS.tab)
        assertNull(ViraTutorialStep.WIDGETS.tab)
        assertFalse(ViraTab.Today in ViraTutorialStep.entries.mapNotNull { it.tab })
    }
}
