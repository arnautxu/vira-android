package cat.vira.android.features.plan

import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/// Un hàbit toca aquell dia? Llista buida = cada dia (`Habit.isScheduled` de l'iPhone).
fun Habit.isScheduled(day: LocalDate): Boolean = weekdays.isEmpty() || PlanDay.iosWeekday(day) in weekdays

/// Els números que pinten Hàbits (`HabitProgress` de l'iPhone): què toca avui, com va la setmana
/// i el mes d'un hàbit.
///
/// Tot es compta a partir dels registres i de la programació. Un dia sense registre és un dia
/// sense registre, mai un "fallat" (`DATA_MODEL.md` §2.4): aquí només hi ha sumes de fets, i els
/// dies que no toquen no entren al denominador.
class HabitProgress(
    private val logs: List<HabitLog>,
    val today: LocalDate = PlanDay.today(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val firstDay: DayOfWeek = PlanDay.firstDayOfWeek(),
) {
    /// Els dies fets de cada hàbit, ja passats a data local.
    private val doneDays: Map<UUID, Set<LocalDate>> = logs
        .filter { it.habitId != null }
        .groupBy({ it.habitId!! }, { PlanDay.date(it.day, zone) })
        .mapValues { it.value.toSet() }

    fun days(habit: Habit): Set<LocalDate> = doneDays[habit.id].orEmpty()

    fun isDone(habit: Habit, day: LocalDate = today): Boolean = day in days(habit)

    /// El primer dia de la setmana local fins a l'últim, de la setmana d'avui.
    val currentWeek: List<LocalDate> get() = CalendarWeek.containing(today, firstDay).days

    fun isFuture(day: LocalDate): Boolean = day.isAfter(today)

    fun dueToday(habits: List<Habit>): List<Habit> = habits.filter { it.isScheduled(today) }

    /// Fracció d'hàbits programats fets aquell dia; `null` si aquell dia no en tocava cap.
    fun dayFraction(habits: List<Habit>, day: LocalDate): Double? {
        val due = habits.filter { it.isScheduled(day) }
        if (due.isEmpty()) return null
        return due.count { isDone(it, day) }.toDouble() / due.size
    }

    /// Fets i programats aquesta setmana, comptant només fins avui als programats perquè "3 de 3"
    /// el dimecres no sembli "3 de 7".
    fun week(habit: Habit): Pair<Int, Int> {
        val done = currentWeek.count { isDone(habit, it) }
        val due = currentWeek.count { !isFuture(it) && habit.isScheduled(it) }
        return done to maxOf(due, done)
    }

    fun daysThisMonth(habit: Habit): Int = days(habit).count { YearMonth.from(it) == YearMonth.from(today) }

    fun totalDays(habit: Habit): Int = days(habit).size

    /// Les caselles del mes d'avui, amb `null` per als buits d'abans del dia 1 perquè la primera
    /// fila comenci a la columna del seu dia de la setmana.
    val monthGrid: List<LocalDate?>
        get() {
            val month = YearMonth.from(today)
            val first = month.atDay(1)
            val leading = (first.dayOfWeek.value - firstDay.value + 7) % 7
            return List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        }

    /// Primer els que toquen avui i al final els que avui no toquen. Marcar-ne un no el mou de
    /// lloc: una targeta que salta sota el dit fa perdre el fil.
    fun ordered(habits: List<Habit>): List<Habit> = habits.sortedBy { if (it.isScheduled(today)) 0 else 1 }

    /// El registre d'aquell dia, si n'hi ha (el que desmarcar ha d'esborrar).
    fun log(habit: Habit, day: LocalDate = today): HabitLog? =
        logs.firstOrNull { it.habitId == habit.id && PlanDay.date(it.day, zone) == day }
}
