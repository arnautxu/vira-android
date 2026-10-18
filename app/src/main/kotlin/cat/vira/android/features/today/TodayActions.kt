package cat.vira.android.features.today

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.recurrence
import cat.vira.android.core.data.model.status
import java.time.Instant
import java.time.ZoneId

/// El que Avui escriu, sempre a través de `ViraStore` perquè tot quedi datat i viatgi.
///
/// Calca `PlanItemCompletion`, `HabitCompletion`, `ViraMedication.record`, `ViraQuickCapture` i
/// «Passa a demà» de l'iPhone. Les tres primeres també són del fil Pla: quan les seves siguin a
/// `main`, Avui les crida i aquestes desapareixen.
internal class TodayActions(private val store: ViraStore, private val zone: () -> ZoneId = ZoneId::systemDefault) {

    private val db get() = store.db

    /// Marca o desmarca una tasca. Retorna `true` si l'ha marcada (i per tant hi ha «Desfer»).
    /// Una tasca setmanal o mensual crea la següent quan es tanca l'actual.
    suspend fun toggle(item: PlanItem): Boolean {
        val current = db.planItems().byId(item.id) ?: return false
        if (current.status == PlanItemStatus.DONE) {
            store.save(db.planItems(), current.copy(statusRaw = PlanItemStatus.PENDING.raw, completedAt = null))
            return false
        }
        store.save(db.planItems(), current.copy(statusRaw = PlanItemStatus.DONE.raw, completedAt = ViraTime.now()))
        TodayPlanRules.nextDay(current.recurrence, current.day, zone())?.let { next ->
            store.create(db.planItems(), copy(current, next).copy(recurrenceRaw = current.recurrenceRaw))
        }
        return true
    }

    /// El «Desfer» d'una tasca: torna a pendent sense res més.
    suspend fun reopen(item: PlanItem) {
        val current = db.planItems().byId(item.id) ?: return
        store.save(db.planItems(), current.copy(statusRaw = PlanItemStatus.PENDING.raw, completedAt = null))
    }

    /// Marca o desmarca un hàbit en un dia. Retorna el registre creat, o `null` si l'ha desmarcat.
    suspend fun toggle(habit: Habit, day: Instant): HabitLog? {
        val existing = db.habitLogs().all().firstOrNull {
            it.habitId == habit.id && TodayPlanRules.startOfDay(it.day, zone()) == day
        }
        if (existing != null) {
            store.delete(existing)
            return null
        }
        return store.create(db.habitLogs(), HabitLog(day = day, completedAt = ViraTime.now(), habitId = habit.id))
    }

    /// El que concedeix un hàbit més enllà de la recompensa, quan la finestra passa sense desfer-se.
    suspend fun countHabitCompleted() {
        val counter = store.counter()
        store.save(db.progressCounters(), counter.copy(habitsCompletedCount = counter.habitsCompletedCount + 1))
    }

    suspend fun undo(log: HabitLog) {
        db.habitLogs().byId(log.id)?.let { store.delete(it) }
    }

    /// Una presa, d'un toc. Tornar-hi la desmarca. **Cap recompensa**: la medicació no en dona
    /// (D-27), i aquest és el lloc on la temptació apareixeria.
    suspend fun recordTaken(medication: Medication, scheduledAt: Instant) {
        val intakes = db.medicationIntakes().all()
        val existing = TodayPlanRules.intake(medication, intakes, scheduledAt)
        if (existing != null) {
            if (existing.status == IntakeStatus.TAKEN) {
                store.delete(existing)
            } else {
                store.save(db.medicationIntakes(), existing.copy(statusRaw = IntakeStatus.TAKEN.raw, loggedAt = ViraTime.now()))
            }
            return
        }
        store.create(
            db.medicationIntakes(),
            MedicationIntake(
                scheduledAt = scheduledAt,
                statusRaw = IntakeStatus.TAKEN.raw,
                loggedAt = ViraTime.now(),
                medicationId = medication.id,
            ),
        )
    }

    /// Sense confirmació: l'esborrat ja demana premuda llarga i triar l'opció d'un menú.
    suspend fun delete(item: PlanItem) {
        db.planItems().byId(item.id)?.let { store.delete(it) }
    }

    /// Passa una tasca a demà a la mateixa hora. Retorna com era, per poder-ho desfer.
    suspend fun moveToTomorrow(item: PlanItem): PlanItem? {
        val current = db.planItems().byId(item.id) ?: return null
        val tomorrow = TodayPlanRules.addDays(current.day, 1, zone())
        val time = current.time?.let { TodayPlanRules.addDays(it, 1, zone()) }
        store.save(
            db.planItems(),
            current.copy(day = tomorrow, time = time, sortIndex = TodayPlanRules.defaultRank(time, zone())),
        )
        return current
    }

    suspend fun undoMove(original: PlanItem) {
        val current = db.planItems().byId(original.id) ?: return
        store.save(db.planItems(), current.copy(day = original.day, time = original.time, sortIndex = original.sortIndex))
    }

    /// Captura ràpida (`ViraQuickCapture.add`). `null` si el títol és buit. Les repeticions de
    /// «cada dia fins a…» es creen ara; les setmanals i mensuals, en tancar cada vegada.
    suspend fun capture(
        title: String,
        day: Instant,
        time: Instant?,
        symbolName: String,
        recurrence: PlanItemRecurrence,
    ): PlanItem? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val batch = TodayPlanRules.createsBatch(recurrence)
        val item = store.create(
            db.planItems(),
            PlanItem(
                title = trimmed,
                day = day,
                time = time,
                symbolName = symbolName,
                recurrenceRaw = if (batch) PlanItemRecurrence.NONE.raw else recurrence.raw,
            ),
        )
        if (batch) {
            TodayPlanRules.additionalDays(recurrence, day, zone()).forEach { store.create(db.planItems(), copy(item, it)) }
        }
        return item
    }

    /// Una còpia independent en un altre dia, amb la mateixa hora i el mateix avís.
    private fun copy(item: PlanItem, day: Instant) = PlanItem(
        title = item.title,
        notes = item.notes,
        day = day,
        time = TodayPlanRules.sameTime(item.time, day, zone()),
        symbolName = item.symbolName,
        reminderOffsetMinutes = item.reminderOffsetMinutes,
    )
}
