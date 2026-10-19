package cat.vira.android.features.plan

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.core.data.model.PlanItemRecurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/// Els botons dels avisos de Pla: el mateix que la pantalla, cap doble recompensa i cap per a la medicació.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlanNotificationEffectsTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 1, 15)
    private val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val store = ViraStore(db)
    private val effects = PlanNotificationEffects(store, zone = { zone })

    @After fun close() = db.close()

    @Test
    fun unHabitFetDesDeLAvisNomesEsRegistraIEsPagaUnCop() = runBlocking {
        val habit = Habit(title = "Llegir")
        store.create(db.habits(), habit)
        val day = PlanDay.at(today, 9, 0, zone)
        effects.completeHabit(habit.id, day)
        effects.completeHabit(habit.id, day)
        assertEquals(1, db.habitLogs().all().size)
        assertEquals(1, db.coinTransactions().all().size)
    }

    @Test
    fun unaTascaJaFetaNoEsTornaAPagar() = runBlocking {
        val item = PlanItem(title = "Deures", day = PlanDay.start(today, zone))
        store.create(db.planItems(), item)
        effects.completePlanItem(item.id)
        effects.completePlanItem(item.id)
        assertEquals(PlanItemStatus.DONE, db.planItems().byId(item.id)?.status)
        assertEquals(1, db.coinTransactions().all().size)
    }

    @Test
    fun laMedicacioNoDonaResIUnBotoRepetitNoLaDesmarca() = runBlocking {
        val medication = Medication(name = "M", timesInMinutes = listOf(8 * 60))
        store.create(db.medications(), medication)
        val at = PlanDay.at(today, 8, 0, zone)
        effects.recordIntake(medication.id, at, IntakeStatus.TAKEN)
        effects.recordIntake(medication.id, at, IntakeStatus.TAKEN)
        assertEquals(IntakeStatus.TAKEN, db.medicationIntakes().all().single().status)
        assertTrue(db.coinTransactions().all().isEmpty())
    }

    @Test
    fun dosAvisosAlhoraNoFanDuesRepeticionsNiDuesRecompenses() = runBlocking {
        val item = PlanItem(title = "Deures", day = PlanDay.start(today, zone), recurrenceRaw = PlanItemRecurrence.WEEKLY.raw)
        store.create(db.planItems(), item)
        List(4) { async(Dispatchers.Default) { effects.completePlanItem(item.id) } }.awaitAll()
        assertEquals(2, db.planItems().all().size)
        assertEquals(1, db.coinTransactions().all().size)
    }

    @Test
    fun reobrirITornarATancarNoFaUnaSegonaRepeticio() = runBlocking {
        val item = PlanItem(title = "Deures", day = PlanDay.start(today, zone), recurrenceRaw = PlanItemRecurrence.WEEKLY.raw)
        store.create(db.planItems(), item)
        effects.completePlanItem(item.id)
        val done = db.planItems().byId(item.id)!!
        store.save(db.planItems(), done.copy(statusRaw = PlanItemStatus.PENDING.raw, completedAt = null))
        effects.completePlanItem(item.id)
        assertEquals(2, db.planItems().all().size)
    }
}
