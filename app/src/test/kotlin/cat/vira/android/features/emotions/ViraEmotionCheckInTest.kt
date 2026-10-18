package cat.vira.android.features.emotions

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.emotion
import cat.vira.android.features.progress.logic.StreakRules
import cat.vira.android.features.progress.logic.ViraProgress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// El registre emocional confirmat (`ViraEmotionCheckInTests` i `EmotionIntensityIntegrationTests`
/// de l'iPhone): el draft no entra mai a l'historial, i confirmar compta una sola vegada.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraEmotionCheckInTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = ViraDatabase.inMemory(context)
    private val store = ViraStore(db)
    private val preferences = ViraPreferences.open(context).also { it.edit().clear().commit() }

    @After fun close() = db.close()

    @Test
    fun unaSeleccioConfirmadaCreaHistorialIComptaUnaSolaVegada() = runBlocking {
        val entry = ViraEmotionCheckIn.record(store, Emotion.ANXIOUS)
        val stored = db.emotionEntries().all()
        assertEquals(listOf(entry.id), stored.map { it.id })
        assertEquals(Emotion.ANXIOUS, stored.single().emotion)
        assertNull(stored.single().intensity)
        assertEquals(1, store.counter().emotionCheckinsCount)
    }

    @Test
    fun laConfirmacioAmbDetallDesaUnaSolaEntradaINormalitzaLaNota() = runBlocking {
        val entry = ViraEmotionCheckIn.record(store, Emotion.SAD, EmotionIntensity.MUCH, "  Dia difícil  ")
        val stored = db.emotionEntries().all().single()
        assertEquals(entry.id, stored.id)
        assertEquals(EmotionIntensity.MUCH, stored.intensity)
        assertEquals("Dia difícil", stored.note)
        assertEquals(1, store.counter().emotionCheckinsCount)
    }

    /// Registrar suma monedes i XP i omple la condició d'emoció del dia per a la ratxa, amb el
    /// sostre diari de l'iPhone (10 monedes, dues vegades al dia).
    @Test
    fun registrarSumaMonedesIRatxa() = runBlocking {
        ViraEmotionCheckIn.record(store, Emotion.CONTENT)
        assertEquals(10, store.counter().coinBalance)
        assertEquals(10, store.counter().totalXP)
        assertFalse(StreakRules.DayRequirement.EMOTION in ViraProgress(store).missingRequirements())

        ViraEmotionCheckIn.record(store, Emotion.TIRED)
        ViraEmotionCheckIn.record(store, Emotion.SAD)
        assertEquals(20, store.counter().coinBalance)
        assertEquals(3, store.counter().emotionCheckinsCount)
    }

    @Test
    fun unaNotaNomesDEspaisNoEsDesa() = runBlocking {
        assertNull(ViraEmotionCheckIn.record(store, Emotion.CONTENT, note = "   ").note)
    }

    /// Cada nivell, també «Gens» i sense indicar, arriba a la fila tal com es va triar.
    @Test
    fun confirmarConservaCadaNivell() = runBlocking {
        (listOf(null) + EmotionIntensity.entries).forEach { level ->
            val draft = EmotionCheckInDraft(Emotion.SAD, level, "  Prova  ")
            val entry = ViraEmotionCheckIn.record(store, draft.emotion, draft.intensity, draft.normalizedNote)
            val stored = db.emotionEntries().byId(entry.id)!!
            assertEquals(level, stored.intensity)
            assertEquals("Prova", stored.note)
        }
        assertEquals(EmotionIntensity.entries.size + 1, store.counter().emotionCheckinsCount)
    }

    @Test
    fun elRegistreSurtDatatPerViatjar() = runBlocking {
        val entry = ViraEmotionCheckIn.record(store, Emotion.TIRED)
        assertTrue(entry.updatedAt > cat.vira.android.core.data.ViraTime.distantPast)
        assertTrue(store.counter().updatedAt > cat.vira.android.core.data.ViraTime.distantPast)
    }

    @Test
    fun elDraftNoEntraALHistorialAbansDeConfirmar() = runBlocking {
        val draft = EmotionCheckInDraft(Emotion.ANXIOUS, EmotionIntensity.VERY_MUCH, "  Ara mateix  ")
        assertTrue(db.emotionEntries().all().isEmpty())
        assertEquals("Ara mateix", draft.normalizedNote)
        assertTrue(draft.shouldOfferSupport)
    }

    @Test
    fun retirarUnaIntensitatAltaRetiraLOfertaDeSuport() {
        val draft = EmotionCheckInDraft(Emotion.ANXIOUS, EmotionIntensity.VERY_MUCH)
        assertTrue(draft.shouldOfferSupport)
        assertFalse(draft.copy(intensity = null).shouldOfferSupport)
    }

    /// Cap emoció agradable ofereix ajuda, per intensa que sigui; cap de desagradable sense intensitat.
    @Test
    fun lOfertaDAjudaNomesPerDesagradableIMoltIntensa() {
        assertFalse(offersSupport(Emotion.CONTENT, EmotionIntensity.VERY_MUCH))
        assertFalse(offersSupport(Emotion.EUPHORIC, EmotionIntensity.MUCH))
        assertFalse(offersSupport(Emotion.SAD, EmotionIntensity.QUITE))
        assertFalse(offersSupport(Emotion.SAD, null))
        assertTrue(offersSupport(Emotion.BLOCKED, EmotionIntensity.MUCH))
    }

    @Test
    fun laPersonalitzacioManteUnMinimIConservaLaRestaAUnaAltra() {
        assertEquals(Emotion.primary, EmotionPreferences.primary(preferences))
        assertTrue(EmotionPreferences.setPrimary(listOf(Emotion.TIRED, Emotion.SAD, Emotion.TIRED), preferences))

        val primary = EmotionPreferences.primary(preferences)
        val other = EmotionPreferences.other(primary)
        assertEquals(listOf(Emotion.TIRED, Emotion.SAD), primary)
        assertTrue(Emotion.CONTENT in other)
        assertTrue(primary.intersect(other.toSet()).isEmpty())
        assertEquals(Emotion.entries.toSet(), (primary + other).toSet())
        assertFalse(EmotionPreferences.setPrimary(emptyList(), preferences))
        assertEquals(primary, EmotionPreferences.primary(preferences))

        ViraDataEraser.resetPreferences(preferences)
        assertEquals(Emotion.primary, EmotionPreferences.primary(preferences))
    }

    @Test
    fun unaPreferenciaIllegibleTornaALesDeFabrica() {
        preferences.edit().putString(EmotionPreferences.PRIMARY_KEY, "res,ni-aixo").commit()
        assertEquals(Emotion.primary, EmotionPreferences.primary(preferences))
    }
}
