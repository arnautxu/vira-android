package cat.vira.android.features.plan

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.features.progress.logic.ViraProgress
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/// Els botons dels avisos de Pla (`ViraNotificationActions.handle` de l'iPhone): el mateix camí que
/// la pantalla, sense la finestra de «Desfer», perquè des de l'avís no hi ha on ensenyar-la.
/// Els connecta l'arrel a `ViraNotificationEffects`. Passen pel mateix pany que la pantalla: dos
/// tocs a l'avís alhora no fan dues repeticions ni dues recompenses.
class PlanNotificationEffects(
    private val store: ViraStore,
    private val progress: ViraProgress = ViraProgress(store),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val db get() = store.db

    /// «Fet» d'un hàbit: si el dia ja té registre, res (ni registre doble ni segona recompensa).
    suspend fun completeHabit(habitId: UUID, day: Instant): Unit = PlanWrites.lock.withLock {
        val habit = db.habits().byId(habitId) ?: return@withLock
        val date = PlanDay.date(day, zone())
        if (db.habitLogs().all().any { it.habitId == habit.id && PlanDay.date(it.day, zone()) == date }) return@withLock
        store.create(db.habitLogs(), HabitLog(day = PlanDay.start(date, zone()), habitId = habit.id))
        progress.record(CoinReason.HABIT)
        progress.countHabitCompleted()
    }

    /// «Fet» d'una tasca: si ja està feta, res; si no, feta, la propera repetició i la recompensa.
    suspend fun completePlanItem(planItemId: UUID): Unit = PlanWrites.lock.withLock {
        val item = db.planItems().byId(planItemId) ?: return@withLock
        if (item.status == PlanItemStatus.DONE) return@withLock
        store.save(db.planItems(), item.copy(statusRaw = PlanItemStatus.DONE.raw, completedAt = ViraTime.now()))
        store.createNextOccurrence(item, zone())
        progress.record(CoinReason.PLAN_ITEM)
    }

    /// «Presa» o «Salta-la». **Cap recompensa**: ni monedes, ni XP, ni ratxa.
    suspend fun recordIntake(medicationId: UUID, scheduledAt: Instant, status: IntakeStatus): Unit = PlanWrites.lock.withLock {
        val medication = db.medications().byId(medicationId) ?: return@withLock
        val own = db.medicationIntakes().all().filter { it.medicationId == medicationId }
        val slot = MedicationSchedule.Slot(medication, scheduledAt, MedicationSchedule.intake(own, scheduledAt))
        // Des de l'avís, el mateix estat no desmarca: un botó vell no pot esborrar una presa.
        if (slot.status == status) return@withLock
        when (val change = ViraMedication.record(status, slot)) {
            is ViraMedication.Change.Create -> store.create(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Save -> store.save(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Delete -> store.delete(change.intake)
            ViraMedication.Change.Nothing -> Unit
        }
    }
}
