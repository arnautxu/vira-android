package cat.vira.android.features.entry

import androidx.compose.ui.geometry.Rect
import cat.vira.android.R
import cat.vira.android.app.ViraTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/// El recorregut del tomb (`TutorialStepTests` de l'iPhone). El que protegeixen no és la cortina
/// —això és dibuix— sinó **l'ordre, les pestanyes i els números**: si algú hi afegeix una parada o
/// en canvia una peça, el forat s'obriria sobre una cosa i la targeta en diria una altra.
class ViraTutorialStepTest {

    @Test
    fun `totes les parades, en aquest ordre i sense widgets`() {
        assertEquals(
            listOf(
                ViraTutorialStep.INTRO, ViraTutorialStep.TODAY, ViraTutorialStep.PLAN,
                ViraTutorialStep.EMOTIONS, ViraTutorialStep.PROFILE, ViraTutorialStep.INTEGRATIONS,
            ),
            ViraTutorialStep.entries,
        )
    }

    @Test
    fun `el primer tomb es la pregunta i tres parades`() {
        val route = ViraTutorialRoute.FIRST
        assertEquals(listOf(ViraTutorialStep.INTRO, ViraTutorialStep.TODAY, ViraTutorialStep.PLAN, ViraTutorialStep.EMOTIONS), route.stops)
        assertEquals(ViraTutorialStep.TODAY, route.next(ViraTutorialStep.INTRO))
        assertEquals(ViraTutorialStep.PLAN, route.next(ViraTutorialStep.TODAY))
        assertEquals(ViraTutorialStep.EMOTIONS, route.next(ViraTutorialStep.PLAN))
        assertNull(route.next(ViraTutorialStep.EMOTIONS))
        assertEquals(3, route.stopCount)
        assertNull(route.position(ViraTutorialStep.INTRO))
        assertEquals(1, route.position(ViraTutorialStep.TODAY))
        assertEquals(3, route.position(ViraTutorialStep.EMOTIONS))
        assertTrue(route.isLast(ViraTutorialStep.EMOTIONS))
        assertFalse(ViraTutorialStep.PROFILE in route.stops)
    }

    @Test
    fun `el tomb repetit fa totes les parades i la pregunta sense numero`() {
        val route = ViraTutorialRoute.REPLAY
        assertEquals(ViraTutorialStep.entries, route.stops)
        assertEquals(5, route.stopCount)
        assertEquals(ViraTutorialStep.PROFILE, route.next(ViraTutorialStep.EMOTIONS))
        assertNull(route.next(ViraTutorialStep.INTEGRATIONS))
        assertEquals(5, route.position(ViraTutorialStep.INTEGRATIONS))
        assertFalse(route.isLast(ViraTutorialStep.EMOTIONS))
    }

    @Test
    fun `els botons diuen som-hi, seguent, comencem i, d'una parada sola, entesos`() {
        val route = ViraTutorialRoute.FIRST
        assertEquals(R.string.onboarding_welcome_start, route.action(ViraTutorialStep.INTRO))
        assertEquals(R.string.tutorial_action_next, route.action(ViraTutorialStep.TODAY))
        assertEquals(R.string.tutorial_action_start, route.action(ViraTutorialStep.EMOTIONS))
        val single = ViraTutorialRoute(listOf(ViraTutorialStep.PLAN))
        assertTrue(single.isLast(ViraTutorialStep.PLAN))
        assertEquals(R.string.viri_tip_ok, single.action(ViraTutorialStep.PLAN))
    }

    @Test
    fun `una frase per peca a cada parada`() {
        for (step in ViraTutorialStep.entries) assertEquals(step.toString(), step.targets.size, step.lines().size)
    }

    @Test
    fun `cada parada de seccio il·lumina la seva pestanya, la primera`() {
        assertEquals(ViraTutorialTarget.Tab(ViraTab.Plan), ViraTutorialStep.PLAN.targets.first())
        assertEquals(ViraTutorialTarget.Tab(ViraTab.Emotions), ViraTutorialStep.EMOTIONS.targets.first())
        assertEquals(ViraTutorialTarget.Tab(ViraTab.Progress), ViraTutorialStep.PROFILE.targets.first())
        assertNull(ViraTutorialStep.TODAY.tab)
        assertNull(ViraTutorialStep.INTEGRATIONS.tab)
    }

    @Test
    fun `les integracions, a avui i sense peces`() {
        assertTrue(ViraTutorialStep.INTEGRATIONS.targets.isEmpty())
        assertEquals(ViraTab.Today, ViraTutorialStep.INTEGRATIONS.screen)
        assertEquals(ViraTab.Plan, ViraTutorialStep.PLAN.screen)
        assertEquals(ViraTab.Progress, ViraTutorialStep.PROFILE.screen)
    }

    @Test
    fun `la frase d'avui canvia amb el que has fet, i provar-ho guanya`() {
        assertNotEquals(ViraTutorialContext().todayLine, ViraTutorialContext(firstAction = EntryFirstActionKind.TASK).todayLine)
        assertEquals(ViraTutorialLine(R.string.tutorial_today_try, "Deures"), ViraTutorialContext(tryTitle = "Deures").todayLine)
        val tried = ViraTutorialContext(EntryFirstActionKind.FOCUS, "Deures", didTry = true)
        assertEquals(ViraTutorialLine(R.string.tutorial_today_tried), tried.todayLine)
        assertNotEquals(tried.todayLine, tried.copy(didTry = false).todayLine)
    }
}

/// On va la targeta del tomb (`TutorialPlacementTests` de l'iPhone).
class ViraTutorialPlacementTest {

    private fun place(obstacles: List<Rect>) = ViraTutorialPlacement.place(obstacles, top = 0f, bottom = 800f, margin = 16f, minimum = 260f)

    private fun rect(x: Float, y: Float, width: Float, height: Float) = Rect(x, y, x + width, y + height)

    @Test
    fun `sense peces, al mig de la pantalla`() {
        assertEquals(ViraTutorialPlacement(0f, 800f, ViraTutorialPlacement.Anchor.CENTER), place(emptyList()))
    }

    @Test
    fun `nomes la pestanya a baix, la targeta s'hi posa a sobre`() {
        val placement = place(listOf(rect(100f, 740f, 80f, 50f)))
        assertEquals(ViraTutorialPlacement.Anchor.BOTTOM, placement.anchor)
        assertEquals(724f, placement.maxY)
    }

    @Test
    fun `peces a dalt i la pestanya a baix, la targeta va entre totes dues`() {
        val placement = place(listOf(rect(0f, 40f, 390f, 60f), rect(100f, 740f, 80f, 50f)))
        assertEquals(ViraTutorialPlacement.Anchor.TOP, placement.anchor)
        assertEquals(116f, placement.minY)
        assertEquals(724f, placement.maxY)
    }

    @Test
    fun `si cap franja hi arriba, a dalt de tot fins a la pestanya`() {
        val placement = place(listOf(rect(0f, 100f, 390f, 300f), rect(0f, 500f, 390f, 200f), rect(100f, 740f, 80f, 50f)))
        assertEquals(0f, placement.minY)
        assertEquals(724f, placement.maxY)
        assertEquals(ViraTutorialPlacement.Anchor.TOP, placement.anchor)
    }
}

/// El registre de peces: res es publica fins que el tomb comença, i una peça de diverses parts fa un
/// sol forat.
class ViraTutorialTargetsTest {

    @Test
    fun `les posicions es desen callades i es publiquen en comencar`() {
        val targets = ViraTutorialTargets()
        targets.report(ViraTutorialTarget.HelpNow, 0, Rect(0f, 0f, 10f, 10f))
        assertTrue(targets.frames.isEmpty())
        targets.startTracking()
        assertEquals(Rect(0f, 0f, 10f, 10f), targets.frames[ViraTutorialTarget.HelpNow])
        targets.stopTracking()
        assertTrue(targets.frames.isEmpty())
    }

    @Test
    fun `una peca de diverses parts fa un sol forat, i una part que marxa en surt`() {
        val targets = ViraTutorialTargets()
        targets.startTracking()
        targets.report(ViraTutorialTarget.EmotionFaces, 0, Rect(0f, 0f, 10f, 10f))
        targets.report(ViraTutorialTarget.EmotionFaces, 1, Rect(20f, 0f, 30f, 10f))
        assertEquals(Rect(0f, 0f, 30f, 10f), targets.frames[ViraTutorialTarget.EmotionFaces])
        targets.report(ViraTutorialTarget.EmotionFaces, 1, null)
        assertEquals(Rect(0f, 0f, 10f, 10f), targets.frames[ViraTutorialTarget.EmotionFaces])
    }

    @Test
    fun `la tasca per provar es la de dalt, i nomes mentre hi ha tomb`() {
        val targets = ViraTutorialTargets()
        targets.registerTry(1, ViraTutorialTargets.TryIt("Segona") {})
        targets.registerTry(0, ViraTutorialTargets.TryIt("Primera") {})
        assertNull(targets.tryIt)
        targets.startTracking()
        assertEquals("Primera", targets.tryIt?.title)
        targets.registerTry(0, null)
        assertEquals("Segona", targets.tryIt?.title)
    }
}
