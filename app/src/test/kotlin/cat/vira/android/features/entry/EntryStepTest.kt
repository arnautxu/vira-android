package cat.vira.android.features.entry

import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import cat.vira.android.features.entry.EntryStep.ACCOUNT
import cat.vira.android.features.entry.EntryStep.AGE
import cat.vira.android.features.entry.EntryStep.CONNECTIONS
import cat.vira.android.features.entry.EntryStep.CONSENT
import cat.vira.android.features.entry.EntryStep.DONE
import cat.vira.android.features.entry.EntryStep.FIRST_ACTION
import cat.vira.android.features.entry.EntryStep.NEW_PASSWORD
import cat.vira.android.features.entry.EntryStep.NICKNAME
import cat.vira.android.features.entry.EntryStep.NOTIFICATIONS
import cat.vira.android.features.entry.EntryStep.PASSWORD_RESET
import cat.vira.android.features.entry.EntryStep.PRIVACY
import cat.vira.android.features.entry.EntryStep.START
import cat.vira.android.features.entry.EntryStep.WELCOME

/// Quins passos del flux d'entrada es veuen, en quin ordre, i on porten "Saltar" i "Ara no"
/// (`EntryStepTests` de l'iPhone). La regla de fons: **a algú que ja ha contestat no se li torna
/// a preguntar**.
class EntryStepTest {

    private val id = UUID.randomUUID()

    private fun user(role: ViraAccountRole, consent: ViraConsentStatus) = ViraUser(id, "algu@exemple.test", role, consent)

    private fun visible(
        steps: List<EntryStep> = EntryStep.full,
        requiresAccount: Boolean,
        user: ViraUser?,
        pendingRole: ViraAccountRole? = null,
    ) = steps.filter { it.applies(requiresAccount, user, pendingRole) }

    @Test
    fun `el cami curt es el nou ordre, primer el valor i despres el compte`() {
        assertEquals(listOf(WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, ACCOUNT, CONSENT, NICKNAME, NOTIFICATIONS), EntryStep.full)
    }

    @Test
    fun `jove amb backend veu E1-E7 i, amb compte nou, E8-E10`() {
        assertEquals(
            listOf(WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, ACCOUNT, NICKNAME, NOTIFICATIONS),
            visible(requiresAccount = true, user = null),
        )
        assertEquals(
            listOf(WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, CONSENT, NICKNAME, NOTIFICATIONS),
            visible(requiresAccount = true, user = user(ViraAccountRole.TEEN, ViraConsentStatus.MISSING)),
        )
    }

    @Test
    fun `el pla de permis depen del tram`() {
        assertEquals(EntryConsentWriter.Plan.ASK_GUARDIAN, EntryConsentWriter.plan("12-15"))
        assertEquals(EntryConsentWriter.Plan.GRANT, EntryConsentWriter.plan("16-20"))
        assertEquals(EntryConsentWriter.Plan.GRANT, EntryConsentWriter.plan("21+"))
        assertEquals(EntryConsentWriter.Plan.NEEDS_AGE, EntryConsentWriter.plan(""))
    }

    @Test
    fun `pare o mare veu benvinguda, privacitat i compte`() {
        assertEquals(listOf(WELCOME, PRIVACY, ACCOUNT), visible(requiresAccount = true, user = null, pendingRole = ViraAccountRole.PARENT))
        val parent = user(ViraAccountRole.PARENT, ViraConsentStatus.GRANTED)
        assertEquals(listOf(WELCOME, PRIVACY), visible(requiresAccount = true, user = parent))
        for (step in listOf(START, AGE, FIRST_ACTION, DONE, CONSENT, NICKNAME, NOTIFICATIONS, CONNECTIONS)) {
            assertFalse(step.applies(true, parent))
            assertFalse(step.applies(true, null, ViraAccountRole.PARENT))
        }
    }

    @Test
    fun `ja tinc compte porta nomes el compte, i el permis si falta`() {
        assertEquals(listOf(ACCOUNT), visible(listOf(ACCOUNT), true, null))
        assertTrue(visible(listOf(ACCOUNT, CONSENT), true, user(ViraAccountRole.TEEN, ViraConsentStatus.GRANTED)).isEmpty())
        assertEquals(listOf(CONSENT), visible(listOf(CONSENT), true, user(ViraAccountRole.TEEN, ViraConsentStatus.MISSING)))
        assertTrue(visible(listOf(CONSENT), true, user(ViraAccountRole.TEEN, ViraConsentStatus.PENDING)).isEmpty())
    }

    @Test
    fun `sense backend ni compte ni permis, i la resta sencera`() {
        assertEquals(listOf(WELCOME, START, AGE, PRIVACY, FIRST_ACTION, DONE, NICKNAME, NOTIFICATIONS), visible(requiresAccount = false, user = null))
    }

    @Test
    fun `amb el permis resolt no es torna a demanar`() {
        for (consent in listOf(ViraConsentStatus.GRANTED, ViraConsentStatus.PENDING)) {
            assertFalse(CONSENT.applies(true, user(ViraAccountRole.TEEN, consent)))
        }
        val fresh = user(ViraAccountRole.TEEN, ViraConsentStatus.MISSING)
        assertTrue(CONSENT.applies(true, fresh))
        assertTrue(CONSENT.isMandatory(true, fresh))
        assertFalse(CONSENT.offersSkip(true, fresh))
    }

    /// La invariant que es va trencar a l'iPhone: si el pas sortís sense compte on escriure,
    /// l'usuari respondria, no es desaria res i se li tornaria a demanar per sempre.
    @Test
    fun `el permis no surt mai si no hi ha res a escriure`() {
        assertFalse(CONSENT.applies(true, null))
        assertFalse(CONSENT.applies(false, null))
        for (consent in ViraConsentStatus.entries) {
            val person = user(ViraAccountRole.TEEN, consent)
            assertEquals(CONSENT.isMandatory(true, person), CONSENT.applies(true, person))
        }
    }

    @Test
    fun `el compte desapareix amb la sessio oberta i no es pot saltar`() {
        assertTrue(ACCOUNT.applies(true, null))
        assertFalse(ACCOUNT.applies(true, user(ViraAccountRole.TEEN, ViraConsentStatus.GRANTED)))
        assertFalse(ACCOUNT.applies(false, null))
        assertTrue(ACCOUNT.isMandatory(true, null))
        assertFalse(ACCOUNT.offersSkip(true, null))
        assertFalse(ACCOUNT.isMandatory(false, null))
    }

    @Test
    fun `saltar amb backend s'atura a la privacitat si no s'ha vist, i si no, al compte`() {
        for (from in listOf(WELCOME, START, AGE)) assertEquals(PRIVACY, EntryStep.skipTarget(from, EntryStep.full, true, null))
        for (from in listOf(PRIVACY, FIRST_ACTION, DONE)) assertEquals(ACCOUNT, EntryStep.skipTarget(from, EntryStep.full, true, null))
    }

    @Test
    fun `saltar sense backend tanca el flux`() {
        for (from in listOf(START, AGE, PRIVACY, FIRST_ACTION, DONE, NICKNAME, NOTIFICATIONS)) {
            assertNull(EntryStep.skipTarget(from, EntryStep.full, false, null))
        }
    }

    @Test
    fun `amb compte nou saltar no salta el permis`() {
        assertEquals(CONSENT, EntryStep.skipTarget(DONE, EntryStep.full, true, user(ViraAccountRole.TEEN, ViraConsentStatus.MISSING)))
    }

    @Test
    fun `ara no passa per sobre de la celebracio`() {
        assertEquals(ACCOUNT, EntryStep.afterSkippedAction(EntryStep.full, true, null))
        assertEquals(NICKNAME, EntryStep.afterSkippedAction(EntryStep.full, false, null))
    }

    @Test
    fun `l'edat nomes ofereix saltar sense backend, la primera accio i la celebracio mai`() {
        assertFalse(AGE.offersSkip(true, null))
        assertTrue(AGE.offersSkip(false, null))
        assertFalse(FIRST_ACTION.offersSkip(false, null))
        assertFalse(DONE.offersSkip(false, null))
        assertTrue(START.offersSkip(true, null))
    }

    @Test
    fun `avancar des de la privacitat porta a la primera accio, el pare al compte`() {
        assertEquals(FIRST_ACTION, EntryStep.next(PRIVACY, EntryStep.full, true, null))
        assertEquals(ACCOUNT, EntryStep.next(PRIVACY, EntryStep.full, true, null, ViraAccountRole.PARENT))
        assertEquals(PRIVACY, EntryStep.next(WELCOME, EntryStep.full, true, null, ViraAccountRole.PARENT))
    }

    @Test
    fun `la benvinguda i la privacitat es veuen sempre, i la privacitat va abans del compte`() {
        for (step in listOf(WELCOME, PRIVACY)) {
            assertTrue(step.applies(true, null))
            assertTrue(step.applies(true, null, ViraAccountRole.PARENT))
            assertTrue(step.applies(false, user(ViraAccountRole.PARENT, ViraConsentStatus.GRANTED)))
        }
        val full = EntryStep.full
        assertTrue(full.indexOf(PRIVACY) < full.indexOf(FIRST_ACTION) && full.indexOf(FIRST_ACTION) < full.indexOf(ACCOUNT))
    }

    @Test
    fun `les pantalles de contrasenya no son etapes del flux`() {
        for (step in listOf(PASSWORD_RESET, NEW_PASSWORD)) {
            assertFalse(step.applies(true, null))
            assertFalse(step.applies(true, user(ViraAccountRole.TEEN, ViraConsentStatus.MISSING)))
            assertFalse(step in EntryStep.full)
        }
    }

    @Test
    fun `Viri diu 15 paraules com a molt a cada pas, en els tres idiomes`() {
        for (folder in listOf("values-ca", "values-es", "values")) {
            val xml = File("src/main/res/$folder/strings_vira.xml").readText()
            val lines = Regex("""<string name="onboarding_viri_[a-z_]+">(.*?)</string>""").findAll(xml).map { it.groupValues[1] }.toList()
            assertEquals(folder, 14, lines.size)
            for (line in lines) assertTrue("$folder: $line", line.trim().split(Regex("\\s+")).size <= 15)
        }
    }

    @Test
    fun `els trams d'edat es toquen i el primer es el del permis`() {
        assertEquals(listOf("12-15", "16-20", "21+"), ViraAgeRange.entries.map { it.raw })
        assertEquals(ViraAgeRange.EARLY, ViraAgeRange.entries.first())
        assertEquals(EntryConsentWriter.Plan.ASK_GUARDIAN, EntryConsentWriter.plan(ViraAgeRange.EARLY.raw))
    }
}
