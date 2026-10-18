package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/// El moviment del flux: avançar, saltar, enrere, entrar i recuperar la contrasenya
/// (`EntryFlowView+Navigation.swift`).
class EntryNavigatorTest {

    private var user: ViraUser? = null
    private var finished = 0
    private val prefs = EntryPreferences.inMemory()

    private fun navigator(steps: List<EntryStep> = EntryStep.full, requiresAccount: Boolean = true, start: EntryStep? = null) =
        EntryNavigator(steps, prefs, { requiresAccount }, { user }, { finished += 1 }, start)

    @Test
    fun `el cami de sempre fins al compte`() {
        val nav = navigator()
        nav.startFresh()
        assertEquals(EntryStep.START, nav.step)
        assertFalse(nav.canGoBack)
        nav.advance(); assertEquals(EntryStep.AGE, nav.step)
        assertTrue(nav.canGoBack)
        nav.advance(); assertEquals(EntryStep.PRIVACY, nav.step)
        nav.advance(); assertEquals(EntryStep.FIRST_ACTION, nav.step)
        nav.completeFirstAction(EntryFirstActionOutcome.Task("Deures"))
        assertEquals(EntryStep.DONE, nav.step)
        assertFalse(nav.canGoBack)
        nav.advance(); assertEquals(EntryStep.ACCOUNT, nav.step)
        // Enrere des del compte no torna a una primera acció ja feta ni a la celebració.
        assertTrue(nav.canGoBack)
        nav.goBack(); assertEquals(EntryStep.PRIVACY, nav.step)
        // I tornant a continuar, la primera acció feta no es repeteix.
        nav.advance(); assertEquals(EntryStep.ACCOUNT, nav.step)
    }

    @Test
    fun `l'estat es recupera despres de recrear l'activitat`() {
        val nav = navigator()
        nav.startFresh(); nav.advance(); nav.advance(); nav.advance()
        nav.completeFirstAction(EntryFirstActionOutcome.Task("Deures"))
        val copy = navigator().apply { restore(nav.snapshot()) }
        assertEquals(nav.step, copy.step)
        assertEquals(nav.history, copy.history)
        assertEquals(EntryFirstActionOutcome.Task("Deures"), copy.outcome)
    }

    @Test
    fun `qui passa a registrar-se des de ja tinc compte veu la privacitat`() {
        val nav = navigator()
        nav.signInFromWelcome(); assertEquals(EntryStep.ACCOUNT, nav.step)
        nav.toggleSigningUp()
        assertTrue(nav.isSigningUp)
        assertEquals(EntryStep.PRIVACY, nav.step)
    }

    @Test
    fun `la recuperacio des del compte torna al compte i continua`() {
        val nav = navigator(start = EntryStep.ACCOUNT)
        nav.go(EntryStep.PASSWORD_RESET)
        nav.startRecovery(); assertEquals(EntryStep.NEW_PASSWORD, nav.step)
        user = ViraUser(UUID.randomUUID(), "algu@exemple.test", ViraAccountRole.TEEN, ViraConsentStatus.MISSING)
        nav.finishRecovery()
        assertEquals(0, finished)
        assertEquals(EntryStep.CONSENT, nav.step)
    }

    @Test
    fun `saltar per on comencem vol dir organitzar-se`() {
        val nav = navigator(start = EntryStep.START)
        nav.skip()
        assertEquals(EntryStartIntent.ORGANIZE.raw, prefs.startIntent.value)
        assertEquals(EntryStep.PRIVACY, nav.step)
    }

    @Test
    fun `en obrir sessio al compte s'avanca i ja no es torna enrere`() {
        val nav = navigator(start = EntryStep.ACCOUNT)
        user = ViraUser(UUID.randomUUID(), "algu@exemple.test", ViraAccountRole.TEEN, ViraConsentStatus.MISSING)
        nav.onSignedIn(isRecoveringPassword = false)
        assertEquals(EntryStep.CONSENT, nav.step)
        assertTrue(nav.history.isEmpty())
    }

    @Test
    fun `la recuperacio no avanca en obrir sessio, i en acabar continua`() {
        val nav = navigator(steps = listOf(EntryStep.ACCOUNT), start = EntryStep.ACCOUNT)
        user = ViraUser(UUID.randomUUID(), "algu@exemple.test", ViraAccountRole.TEEN, ViraConsentStatus.GRANTED)
        nav.onSignedIn(isRecoveringPassword = true)
        assertEquals(EntryStep.ACCOUNT, nav.step)
        nav.startRecovery()
        assertEquals(EntryStep.NEW_PASSWORD, nav.step)
        nav.finishRecovery()
        assertEquals(1, finished)
        assertTrue(prefs.onboardingCompleted.value)
    }

    @Test
    fun `ja tinc compte i entrar nomes deixa el permis`() {
        val nav = navigator()
        nav.signInFromWelcome()
        assertEquals(EntryStep.ACCOUNT, nav.step)
        assertFalse(nav.isSigningUp)
        assertFalse(nav.applies(EntryStep.NICKNAME))
        nav.toggleSigningUp()
        assertTrue(nav.isSigningUp)
        assertTrue(nav.applies(EntryStep.NICKNAME))
        nav.goBack()
        assertEquals(EntryStep.WELCOME, nav.step)
    }

    @Test
    fun `sóc pare o mare amaga els passos del jove`() {
        val nav = navigator()
        nav.startAsParent()
        assertEquals(EntryStep.PRIVACY, nav.step)
        nav.advance()
        assertEquals(EntryStep.ACCOUNT, nav.step)
        assertEquals(null, nav.viriLine)
    }

    @Test
    fun `la barra no retrocedeix en avancar i el flux sense backend s'acaba sol`() {
        val nav = navigator(requiresAccount = false, start = EntryStep.NOTIFICATIONS)
        val before = nav.progress
        nav.advance()
        assertEquals(1, finished)
        assertTrue(nav.progress >= before)
    }
}
