package cat.vira.android.features.plan

import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.features.progress.logic.ViraProgress
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import cat.vira.android.core.design.components.PendingCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/// Tot el que pinta Pla, d'un cop: el que a l'iPhone són les `@Query` de cada cara.
data class PlanSnapshot(
    val items: List<PlanItem> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val intakes: List<MedicationIntake> = emptyList(),
) {
    /// Els hàbits vius: els arxivats no surten enlloc de la interfície, però es conserven.
    val activeHabits: List<Habit> get() = habits.filter { !it.isArchived }.sortedBy { it.createdAt }
    val activeMedications: List<Medication> get() = medications.filter { it.isActive }.sortedBy { it.createdAt }

    fun items(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<PlanItem> {
        // Per data local i no per l'instant: un ítem creat en un altre fus horari té la mitjanit
        // d'aquell fus, i `countsByDay` ja el posa en aquest dia.
        return PlanItemOrder.sorted(items.filter { PlanDay.date(it.day, zone) == day }, zone)
    }

    fun countsByDay(zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> =
        items.groupingBy { PlanDay.date(it.day, zone) }.eachCount()

    companion object {
        fun observe(db: ViraDatabase): Flow<PlanSnapshot> = combine(
            db.planItems().observeAll(),
            db.habits().observeAll(),
            db.habitLogs().observeAll(),
            db.medications().observeAll(),
            db.medicationIntakes().observeAll(),
        ) { items, habits, logs, medications, intakes -> PlanSnapshot(items, habits, logs, medications, intakes) }
    }
}

/// Les escriptures de Pla. Totes passen per `ViraStore`, que data i deixa làpida: un canvi desat
/// sense datar no viatjaria mai a l'altre telèfon.
///
/// No és un `ViewModel`: no hi ha estat, només el camí de cada acció fins a la base de dades i el
/// que l'arrel de l'app ha de saber (el "Desfer", els avisos).
class PlanActions(
    private val store: ViraStore?,
    private val scope: CoroutineScope,
    private val host: PlanHost,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private fun write(block: suspend ViraStore.() -> Unit) {
        val store = store ?: return
        // Una darrere l'altra i en l'ordre dels tocs: un "Desfer" no pot llegir abans que s'hagi
        // escrit el que desfà.
        scope.launch { PlanWrites.lock.withLock { store.block() } }
    }

    // Calendari

    /// Marca o desmarca (`PlanItemCompletion.toggle`). En marcar, l'estat canvia **a l'instant**,
    /// es crea la propera instància si es repeteix i la cua de l'arrel obre la finestra de
    /// "Desfer"; la recompensa, si ningú la desfà, la concedeix la cua. Desmarcar mai en resta cap.
    fun toggle(item: PlanItem) {
        val id = PlanCompletionIds.item(item)
        if (item.status == PlanItemStatus.DONE) {
            write {
                val current = db.planItems().byId(item.id) ?: return@write
                save(db.planItems(), current.copy(statusRaw = PlanItemStatus.PENDING.raw, completedAt = null))
            }
            host.undo(id)
        } else {
            // La propera repetició es fa ara (com a l'iPhone) i el "Desfer" se l'emporta: si no,
            // tornar-la a marcar en faria una segona per al mateix dia.
            var nextId: java.util.UUID? = null
            write {
                // La fila d'ara, no la de la pantalla: una sincronització entremig no es perd.
                val current = db.planItems().byId(item.id) ?: return@write
                if (current.status == PlanItemStatus.DONE) return@write
                save(db.planItems(), current.copy(statusRaw = PlanItemStatus.DONE.raw, completedAt = ViraTime.now()))
                PlanItemCompletion.nextOccurrence(current, zone)?.let {
                    create(db.planItems(), it)
                    nextId = it.id
                }
            }
            host.present(
                PendingCompletion(
                    id = id,
                    reason = CoinReason.PLAN_ITEM,
                    onUndo = {
                        write {
                            db.planItems().byId(item.id)?.let {
                                save(db.planItems(), it.copy(statusRaw = PlanItemStatus.PENDING.raw, completedAt = null))
                            }
                            nextId?.let { db.planItems().byId(it) }?.let { delete(it) }
                        }
                    },
                ),
            )
        }
    }

    /// Obre la captura ràpida del dia, amb la franja si s'hi arriba des d'un "Afegir aquí".
    fun capture(day: LocalDate, period: DayPeriod?) = host.onCapture(day, period)

    fun delete(item: PlanItem) = write { delete(item) }

    /// Només l'ordre: la resta de la fila es llegeix d'ara, perquè una sincronització entremig no es perdi.
    fun move(changed: List<PlanItem>) = write {
        changed.forEach { moved ->
            db.planItems().byId(moved.id)?.let { save(db.planItems(), it.copy(sortIndex = moved.sortIndex)) }
        }
    }

    /// Desa l'edició. Canviar l'hora o el dia refà l'ordre a partir del rellotge; si només canvia
    /// el títol o la icona, l'ordre no es toca.
    fun save(original: PlanItem, edited: PlanItem, recurrence: PlanItemRecurrence) = write {
        val reordered = if (original.time != edited.time || original.day != edited.day) {
            edited.copy(sortIndex = PlanItemOrder.defaultRank(edited.time, zone))
        } else {
            edited
        }
        val (item, copies) = PlanItemCompletion.applyRecurrence(reordered, recurrence, zone)
        save(db.planItems(), item)
        copies.forEach { create(db.planItems(), it) }
    }

    /// Un ítem nou (el pont de la captura ràpida): amb la repetició, com en desar.
    fun create(item: PlanItem, recurrence: PlanItemRecurrence) = write {
        val (first, copies) = PlanItemCompletion.applyRecurrence(item, recurrence, zone)
        create(db.planItems(), first)
        copies.forEach { create(db.planItems(), it) }
    }

    // Hàbits

    /// `HabitCompletion.toggle`: el registre s'insereix a l'instant i la recompensa (i el comptador
    /// d'hàbits fets) arriba només si ningú ho desfà.
    fun toggle(habit: Habit, day: LocalDate, progress: HabitProgress) {
        val id = PlanCompletionIds.habit(habit, day, zone)
        val existing = progress.log(habit, day)
        if (existing != null) {
            write {
                delete(existing)
                host.onScheduleChanged()
            }
            host.undo(id)
        } else {
            val log = HabitLog(day = PlanDay.start(day, zone), habitId = habit.id)
            write {
                create(db.habitLogs(), log)
                host.onScheduleChanged()
            }
            host.present(
                PendingCompletion(
                    id = id,
                    reason = CoinReason.HABIT,
                    onUndo = {
                        write {
                            db.habitLogs().byId(log.id)?.let { delete(it) }
                            host.onScheduleChanged()
                        }
                    },
                    onCommit = {
                        store?.let { ViraProgress(it).countHabitCompleted() }
                    },
                    title = habit.title,
                ),
            )
        }
    }

    fun save(habit: Habit, isNew: Boolean) = write {
        if (isNew) create(db.habits(), habit) else save(db.habits(), habit)
        host.onScheduleChanged()
    }

    fun archive(habit: Habit) = write {
        save(db.habits(), habit.copy(isArchived = true))
        host.onScheduleChanged()
    }

    // Medicació

    /// Registrar no dona res i no treu res: només deixa constància.
    fun record(status: IntakeStatus, slot: MedicationSchedule.Slot) = write {
        when (val change = ViraMedication.record(status, slot)) {
            is ViraMedication.Change.Create -> create(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Save -> save(db.medicationIntakes(), change.intake)
            is ViraMedication.Change.Delete -> delete(change.intake)
            ViraMedication.Change.Nothing -> Unit
        }
        host.onScheduleChanged()
    }

    fun save(medication: Medication, isNew: Boolean) = write {
        if (isNew) create(db.medications(), medication) else save(db.medications(), medication)
        host.onScheduleChanged()
    }

    fun end(medication: Medication) = write {
        save(db.medications(), ViraMedication.end(medication))
        host.onScheduleChanged()
    }

    companion object {
        /// Per a les captures i les previsualitzacions: no escriu res.
        fun preview(scope: CoroutineScope) = PlanActions(null, scope, PlanHost())
    }
}

/// Identificadors de la finestra de "Desfer", els mateixos de l'iPhone: `plan-<id>` i
/// `habit-<id>-<inici del dia en segons>`.
object PlanCompletionIds {
    fun item(item: PlanItem) = "plan-${item.id}"
    fun habit(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()) =
        "habit-${habit.id}-${PlanDay.start(day, zone).epochSecond}"
}
