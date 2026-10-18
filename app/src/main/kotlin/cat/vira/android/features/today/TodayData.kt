package cat.vira.android.features.today

import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import java.time.Instant
import java.time.ZoneId

/// Tot el que Avui llegeix, ja filtrat pel dia (les `@Query` i el `@AppStorage` de `TodayView`).
/// Fora de la pantalla perquè les captures hi puguin posar un dia concret sense base de dades.
internal data class TodayData(
    val now: Instant,
    val zone: ZoneId,
    val planItems: List<PlanItem> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val intakes: List<MedicationIntake> = emptyList(),
    val calendarEvents: List<TodayCalendarEvent> = emptyList(),
    val displayName: String = "",
    /// Mòduls triats a l'entrada o a Ajustos (D-37). Apagar-ne un n'amaga el bloc, no n'esborra res.
    val habitsEnabled: Boolean = true,
    val medicationEnabled: Boolean = true,
    /// L'ítem que Viri acaba de passar a demà, tal com és ara, per poder-ho desfer.
    val moved: PlanItem? = null,
) {
    val today: Instant = TodayPlanRules.startOfDay(now, zone)
    val tomorrow: Instant = TodayPlanRules.addDays(today, 1, zone)
    val hour: Int = now.atZone(zone).hour

    val agenda: Agenda = agendaFor(today, calendarEvents)

    /// Els hàbits d'avui compten per a Viri encara que el mòdul estigui amagat, com a l'iPhone.
    val todayHabits: List<Habit> = habits.filter { !it.isArchived && TodayPlanRules.isScheduled(it, today, zone) }

    val doneHabitIds: Set<java.util.UUID> = habitLogs
        .filter { TodayPlanRules.startOfDay(it.day, zone) == today }
        .mapNotNull { it.habitId }
        .toSet()

    val habitsDone: Int = todayHabits.count { it.id in doneHabitIds }

    val pending: List<AgendaEntry> get() = agenda.now(now) + agenda.rest(now)

    val tomorrowAgenda: Agenda by lazy { agendaFor(tomorrow, emptyList()) }

    val tomorrowHabits: List<Habit> by lazy {
        if (habitsEnabled) habits.filter { !it.isArchived && TodayPlanRules.isScheduled(it, tomorrow, zone) } else emptyList()
    }

    /// Des de la tarda, o abans si el dia ja és fet, i només si demà hi ha alguna cosa.
    val showsTomorrow: Boolean
        get() {
            val dayIsDone = !agenda.isEmpty && pending.isEmpty()
            if (hour < 18 && !dayIsDone) return false
            return !tomorrowAgenda.isEmpty || tomorrowHabits.isNotEmpty()
        }

    fun viriPlan(skips: Int, isGentle: Boolean): TodayViriPlan = TodayViriPlan.decide(
        agenda = agenda,
        pending = pending,
        hour = hour,
        habitsTotal = todayHabits.size,
        habitsDone = habitsDone,
        skips = skips,
        isGentle = isGentle,
        moved = moved,
    )

    /// L'identificador de la finestra de «Desfer» d'un hàbit avui (`HabitCompletion`).
    fun habitCompletionId(habit: Habit): String = "habit-${habit.id}-${today.epochSecond}"

    private fun agendaFor(day: Instant, events: List<TodayCalendarEvent>) = Agenda(
        planItems = planItems.filter { TodayPlanRules.startOfDay(it.day, zone) == day },
        medications = if (medicationEnabled) {
            medications.filter { it.isActive && TodayPlanRules.isScheduled(it, day, zone) }
        } else {
            emptyList()
        },
        intakes = intakes,
        day = day,
        zone = zone,
        calendarEvents = events,
    )
}
