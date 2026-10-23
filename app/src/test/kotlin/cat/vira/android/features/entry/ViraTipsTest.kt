package cat.vira.android.features.entry

import cat.vira.android.R
import cat.vira.android.features.progress.viriTabTipLines
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/// Quan surt una pista d'un sol ús i què diu «Aquí és Viri» (`ViriTabTipTests` de l'iPhone).
class ViraTipsTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun `cap pista abans d'acabar el tomb`() {
        ViraTip.entries.forEach { assertFalse(ViraTipRules.isEligible(it, completed = false, skipped = true, dismissed = false)) }
    }

    @Test
    fun `les de Pla i Emocions només a qui ha saltat el tomb`() {
        assertFalse(ViraTipRules.isEligible(ViraTip.PLAN_ADD, completed = true, skipped = false, dismissed = false))
        assertFalse(ViraTipRules.isEligible(ViraTip.EMOTION, completed = true, skipped = false, dismissed = false))
        assertTrue(ViraTipRules.isEligible(ViraTip.PLAN_ADD, completed = true, skipped = true, dismissed = false))
        assertTrue(ViraTipRules.isEligible(ViraTip.FOCUS, completed = true, skipped = false, dismissed = false))
        assertTrue(ViraTipRules.isEligible(ViraTip.MINDFULNESS, completed = true, skipped = false, dismissed = false))
    }

    @Test
    fun `una pista tancada no torna`() {
        assertFalse(ViraTipRules.isEligible(ViraTip.FOCUS, completed = true, skipped = true, dismissed = true))
    }

    @Test
    fun `una al dia, i la d'avui es queda`() {
        assertTrue(ViraTipRules.fitsToday(ViraTip.FOCUS, null, null, today))
        assertTrue(ViraTipRules.fitsToday(ViraTip.FOCUS, today.toString(), "focus", today))
        assertFalse(ViraTipRules.fitsToday(ViraTip.MINDFULNESS, today.toString(), "focus", today))
        assertTrue(ViraTipRules.fitsToday(ViraTip.MINDFULNESS, today.minusDays(1).toString(), "focus", today))
    }

    @Test
    fun `Viri explica només el que es veu`() {
        assertEquals(listOf(R.string.viri_tip_level, R.string.viri_tip_streak, R.string.viri_tip_shop), viriTabTipLines(true, true))
        assertEquals(listOf(R.string.viri_tip_streak), viriTabTipLines(false, false))
        assertEquals(listOf(R.string.viri_tip_streak, R.string.viri_tip_shop), viriTabTipLines(false, true))
    }
}
