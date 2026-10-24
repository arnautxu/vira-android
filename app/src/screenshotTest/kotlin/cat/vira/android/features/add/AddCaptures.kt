package cat.vira.android.features.add

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/// El full «Afegir» a l'ample d'un iPhone (393 dp) i l'alt d'una pantalla.
@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 2400, locale = "es", fontScale = 2f)
annotation class AddCaptures

private val zone: ZoneId = ZoneId.of("Europe/Madrid")
private val now: Instant = LocalDateTime.of(2026, 10, 19, 9, 0).atZone(zone).toInstant()
private val today: LocalDate = LocalDate.of(2026, 10, 19)
private val actions = AddSheetActions()
private val none: @Composable () -> Unit = {}

@Composable
private fun Sheet(content: @Composable () -> Unit) = ViraCapture {
    ViraTheme(reduceMotion = true) {
        Box(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Plan)) { content() }
    }
}

// Tasca

@PreviewTest @AddCaptures @Composable
fun AddTaskWhat() = ViraCapture { ViraTheme(reduceMotion = true) {
    AddSheetContent(ViraAddKind.Task, actions, onDismiss = {}, zone = zone, now = now)
} }

/// El pas «Quan» amb els tres moments d'un toc, el calendari amb punts i «Sense hora».
@PreviewTest @AddCaptures @Composable
fun AddTaskWhen() = Sheet {
    TaskAddFlow(
        actions, {}, none, {}, zone = zone, now = now, initialStep = TaskAddStep.When, initialTitle = "Treball de català",
        initialDayCounts = mapOf(today to 1, today.plusDays(1) to 3, today.plusDays(4) to 2),
    )
}

/// «Demà a les 17:30» a la frase: el dia marcat al calendari i el rellotge de roda obert.
@PreviewTest @AddCaptures @Composable
fun AddTaskWhenClock() = Sheet {
    TaskAddFlow(
        actions, {}, none, {}, zone = zone, now = now, initialStep = TaskAddStep.When, initialTitle = "Dentista demà a les 17:30",
        initialDayCounts = mapOf(today.plusDays(1) to 1),
    )
}

/// «La partim?»: la tasca sembla gran i Viri proposa partir-la abans de Fet.
@PreviewTest @AddCaptures @Composable
fun AddTaskSplit() = Sheet {
    TaskAddFlow(actions, {}, none, {}, zone = zone, now = now, initialStep = TaskAddStep.Split, initialTitle = "Treball de socials")
}

@PreviewTest @AddCaptures @Composable
fun AddTaskDone() = Sheet {
    TaskAddFlow(
        actions, {}, none, {}, zone = zone, now = now, initialStep = TaskAddStep.Done,
        initialAdded = listOf(AddedTask("Treball de català", "picto-plan-write", today.plusDays(1), LocalTime.of(17, 0), emptyList())),
    )
}

// Hàbit

@PreviewTest @AddCaptures @Composable
fun AddHabitWhat() = ViraCapture { ViraTheme(reduceMotion = true) {
    AddSheetContent(ViraAddKind.Habit, actions, onDismiss = {}, zone = zone, now = now)
} }

@PreviewTest @AddCaptures @Composable
fun AddHabitDays() = Sheet {
    HabitAddFlow(actions, {}, none, {}, zone = zone, now = now, initialStep = HabitAddStep.Days, initialTitle = "Llegir 10 min")
}

@PreviewTest @AddCaptures @Composable
fun AddHabitReminder() = Sheet {
    HabitAddFlow(actions, {}, none, {}, zone = zone, now = now, initialStep = HabitAddStep.Reminder, initialTitle = "Llegir 10 min")
}

@PreviewTest @AddCaptures @Composable
fun AddHabitDone() = Sheet {
    HabitAddFlow(
        actions, {}, none, {}, zone = zone, now = now, initialStep = HabitAddStep.Done, initialTitle = "Llegir 10 min",
        initialCreated = Habit(title = "Llegir 10 min", symbolName = "picto-habit-read"),
    )
}

// Medicació: neutra, sense Viri ni premis.

@PreviewTest @AddCaptures @Composable
fun AddMedicationWhat() = ViraCapture { ViraTheme(reduceMotion = true) {
    AddSheetContent(ViraAddKind.Medication, actions, onDismiss = {}, zone = zone, now = now)
} }

@PreviewTest @AddCaptures @Composable
fun AddMedicationTime() = Sheet {
    MedicationAddFlow(actions, {}, none, {}, initialStep = MedicationAddStep.Time, initialName = "La del matí", initialTimes = listOf(8 * 60, 21 * 60))
}

@PreviewTest @AddCaptures @Composable
fun AddMedicationReminder() = Sheet {
    MedicationAddFlow(actions, {}, none, {}, initialStep = MedicationAddStep.Reminder, initialName = "La del matí")
}
