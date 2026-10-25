package cat.vira.android.features.plan

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.features.progress.logic.ViraProgress
import cat.vira.android.features.progress.logic.ViraRewardOnce
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/// Els botons dels avisos de Pla (`ViraNotificationActions.handle` de l'iPhone): el mateix camí que
/// la pantalla, sense la finestra de «Desfer», perquè des de l'avís no hi ha on ensenyar-la.
/// Els connecta l'arrel a `ViraNotificationEffects`. Passen pel mateix pany que la pantalla: dos
/// tocs a l'avís alhora no fan dues repeticions ni dues recompenses.
class PlanNotificationEffects(
    private val store: ViraStore,
    /// El que ja s'ha premiat: tornar a marcar des de l'avís no torna a pagar.
    private val once: ViraRewardOnce,
    private val progress: ViraProgress = ViraProgress(store),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val db get() = store.db

    /// «Fet» d'un hàbit: si el dia ja té registre, res (ni registre doble ni segona recompensa).
    suspend fun completeHabit(habitId: UUID, day: Instant): Unit = PlanWrites.locked(db) {
        val habit = db.habits().byId(habitId) ?: return@locked
        val date = PlanDay.date(day, zone())
        // Un avís vell d'un hàbit arxivat o que ja no toca aquell dia no compta.
        if (habit.isArchived || !habit.isScheduled(date)) return@locked
        if (db.habitLogs().all().any { it.habitId == habit.id && PlanDay.date(it.day, zone()) == date }) return@locked
        store.create(db.habitLogs(), HabitLog(day = PlanDay.start(date, zone()), habitId = habit.id))
        // Sense finestra de «Desfer», la recompensa es concedeix de seguida i desmarcar des de l'app
        // no en resta res. Per això mateix, un hàbit que ja s'havia premiat avui no torna a pagar.
        progress.recordOnce(CoinReason.HABIT, ViraRewardOnce.habitKey(habit.id, date, zone()), once) ?: return@locked
        progress.countHabitCompleted()
    }

    /// «Fet» d'una tasca: si ja està feta, res; si no, feta, la propera repetició i la recompensa.
    suspend fun completePlanItem(planItemId: UUID): Unit = PlanWrites.locked(db) {
        val item = db.planItems().byId(planItemId) ?: return@locked
        // Només una de pendent: una de feta o descartada entremig no la torna a marcar un avís vell.
        if (item.status != PlanItemStatus.PENDING) return@locked
        store.save(db.planItems(), item.copy(statusRaw = PlanItemStatus.DONE.raw, completedAt = ViraTime.now()))
        store.createNextOccurrence(item, zone())
        progress.recordOnce(CoinReason.PLAN_ITEM, ViraRewardOnce.planItemKey(item.id), once)
    }

    /// «Presa» o «Salta-la». **Cap recompensa**: ni monedes, ni XP, ni ratxa.
    suspend fun recordIntake(medicationId: UUID, scheduledAt: Instant, status: IntakeStatus): Unit = PlanWrites.locked(db) {
        val medication = db.medications().byId(medicationId) ?: return@locked
        val own = db.medicationIntakes().all().filter { it.medicationId == medicationId }
        // Només un espai que la pauta d'ara encara té: un avís d'una hora canviada o d'un
        // tractament acabat no deixa una presa que cap pantalla ensenyaria.
        val minute = scheduledAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        val slot = MedicationSchedule.slots(medication, own, PlanDay.date(scheduledAt, zone()), zone())
            .firstOrNull { it.scheduledAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES) == minute }
            ?: return@locked
        // Des de l'avís, el mateix estat no desmarca, i una presa ja contestada (aquí o en un
        // altre telèfon) no la canvia un botó vell. "Més tard" encara espera resposta.
        if (slot.status == status || slot.status == IntakeStatus.TAKEN || slot.status == IntakeStatus.SKIPPED) return@locked
        when (val change = ViraMedication.record(status, slot)) {
            is ViraMedication.Change.Create -> store.create(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Save -> store.save(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Delete -> store.delete(change.intake)
            ViraMedication.Change.Nothing -> Unit
        }
    }
}
