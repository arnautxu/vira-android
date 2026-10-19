package cat.vira.android.features.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cat.vira.android.core.design.viraLocale
import cat.vira.android.app.PlanSection
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationForm
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.ActiveCompletion
import cat.vira.android.core.design.components.CompletionPhase
import cat.vira.android.core.design.components.ProvideViraCompletions
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDate
import java.time.ZoneId

/// Captures de Pla per posar al costat de les de l'iPhone: cada cara plena i buida, en clar,
/// fosc i lletra al 200 %. El dia és fix (dimecres 14 d'octubre del 2026) perquè la captura no
/// canviï amb el calendari.
private val today = LocalDate.of(2026, 10, 14)
private val zone = ZoneId.systemDefault()

private object Sample {
    val reading = Habit(title = "Llegir 10 minuts", symbolName = "picto-habit-read")
    val water = Habit(title = "Beure aigua", symbolName = "picto-habit-water")
    val run = Habit(title = "Córrer", symbolName = "picto-habit-run", weekdays = listOf(2, 4, 6))
    val medication = Medication(name = "Elvanse", dose = "30 mg", formRaw = MedicationForm.CAPSULE.raw, timesInMinutes = listOf(8 * 60, 14 * 60))

    fun item(title: String, hour: Int?, symbol: String, done: Boolean = false, day: LocalDate = today) = PlanItem(
        title = title,
        day = PlanDay.start(day, zone),
        time = hour?.let { PlanDay.at(day, it, 0, zone) },
        symbolName = symbol,
        statusRaw = if (done) PlanItemStatus.DONE.raw else PlanItemStatus.PENDING.raw,
        sortIndex = PlanItemOrder.defaultRank(hour?.let { PlanDay.at(day, it, 0, zone) }, zone),
    )

    val items = listOf(
        item("Institut", 8, "picto-cal-class", done = true),
        item("Deures de mates", null, "picto-habit-study"),
        item("Anglès", 17, "picto-plan-talk"),
        item("Sopar amb la família", 21, "picto-cal-meal"),
        item("Entrenament", 18, "picto-cal-workout", day = today.plusDays(1)),
    )

    val logs = listOf(
        HabitLog(day = PlanDay.start(today, zone), habitId = reading.id),
        HabitLog(day = PlanDay.start(today.minusDays(1), zone), habitId = reading.id),
        HabitLog(day = PlanDay.start(today.minusDays(2), zone), habitId = water.id),
        HabitLog(day = PlanDay.start(today.minusDays(9), zone), habitId = reading.id),
    )

    val intakes = listOf(
        MedicationIntake(
            scheduledAt = PlanDay.at(today, 8, 0, zone),
            statusRaw = IntakeStatus.TAKEN.raw,
            loggedAt = PlanDay.at(today, 8, 10, zone),
            medicationId = medication.id,
        ),
    )

    val full = PlanSnapshot(items, listOf(reading, water, run), logs, listOf(medication), intakes)
}

@Composable
private fun Screen(
    section: PlanSection,
    snapshot: PlanSnapshot = Sample.full,
    active: ActiveCompletion? = null,
    calendarMode: CalendarMode = CalendarMode.Day,
    content: (@Composable (PlanActions) -> Unit)? = null,
) {
    ViraCapture { ViraTheme(reduceMotion = true) {
        val scope = rememberCoroutineScope()
        val actions = remember { PlanActions.preview(scope) }
        ProvideViraCompletions(present = {}, undo = {}, active = active) {
            Box(Modifier.fillMaxSize()) {
                if (content != null) {
                    content(actions)
                } else {
                    PlanContent(snapshot, actions, remember { PlanChrome() }, section, onSelect = {}, today = today, calendarMode = calendarMode)
                }
            }
        }
    } }
}

@PreviewTest @ViraScreenCaptures @Composable
fun CalendarDay() = Screen(PlanSection.Calendar)

@PreviewTest @ViraScreenCaptures @Composable
fun CalendarDayEmpty() = Screen(PlanSection.Calendar, PlanSnapshot())

@PreviewTest @ViraScreenCaptures @Composable
fun CalendarDayUndo() = Screen(
    PlanSection.Calendar,
    active = ActiveCompletion(PlanCompletionIds.item(Sample.items[2]), CompletionPhase.Pending),
)

@PreviewTest @ViraScreenCaptures @Composable
fun CalendarWeekView() = Screen(PlanSection.Calendar, calendarMode = CalendarMode.Week)

@PreviewTest @ViraScreenCaptures @Composable
fun Habits() = Screen(PlanSection.Habits)

@PreviewTest @ViraScreenCaptures @Composable
fun HabitsEmpty() = Screen(PlanSection.Habits, PlanSnapshot())

@PreviewTest @ViraScreenCaptures @Composable
fun HabitDetailScreen() = Screen(PlanSection.Habits) { actions ->
    HabitDetail(Sample.reading, HabitProgress(Sample.logs, today, zone, PlanDay.firstDayOfWeek(viraLocale())), actions, onEdit = {}, onClose = {}, zone = zone)
}

@PreviewTest @ViraScreenCaptures @Composable
fun Medication() = Screen(PlanSection.Medication)

@PreviewTest @ViraScreenCaptures @Composable
fun MedicationEmpty() = Screen(PlanSection.Medication, PlanSnapshot())

@PreviewTest @ViraScreenCaptures @Composable
fun HabitCelebration() = Screen(PlanSection.Habits) { HabitCelebrationScreen("Llegir 10 minuts", coins = 3, xp = 3, onContinue = {}) }
