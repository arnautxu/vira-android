package cat.vira.android.features.today

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.ActiveCompletion
import cat.vira.android.core.design.components.CompletionPhase
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/// Avui a l'ample d'un iPhone (393 dp), prou alt per veure la pantalla sencera.
@Preview(name = "clar", widthDp = 393, heightDp = 1800, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 1800, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 4600, locale = "es", fontScale = 2f)
annotation class TodayCaptures

@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 2000, locale = "es", fontScale = 2f)
annotation class CaptureCaptures

private val zone: ZoneId = ZoneId.of("Europe/Madrid")
private fun at(hour: Int, minute: Int = 0): Instant = LocalDateTime.of(2026, 10, 19, hour, minute).atZone(zone).toInstant()
private val day = at(0)
private val tomorrow = day.plusSeconds(86_400)

private val reading = Habit(title = "Llegir 10 min", symbolName = "picto-habit-read")
private val water = Habit(title = "Beure aigua", symbolName = "picto-habit-water")
private val stretch = Habit(title = "Estiraments", symbolName = "picto-habit-stretch")
private val habits = listOf(reading, water, stretch)

private val essay = PlanItem(title = "Treball de català", day = day, symbolName = "picto-plan-write")
private val doneTask = PlanItem(title = "Preparar la motxilla", day = day, time = at(8), symbolName = "picto-plan-checklist", statusRaw = PlanItemStatus.DONE.raw)
private val maths = PlanItem(title = "Classe de mates", day = day, time = at(10), symbolName = "picto-cal-class")
private val football = PlanItem(title = "Entrenament", day = day, time = at(18, 30), symbolName = "picto-cal-workout")
private val dentist = PlanItem(title = "Dentista", day = tomorrow, time = tomorrow.plusSeconds(17 * 3600L), symbolName = "picto-cal-doctor")
private val pill = Medication(name = "Pastilla del matí", timesInMinutes = listOf(8 * 60 + 30))

private fun data(now: Instant, items: List<PlanItem> = listOf(essay, doneTask, maths, football, dentist), name: String = "Nil") = TodayData(
    now = now,
    zone = zone,
    planItems = items,
    habits = habits,
    habitLogs = listOf(HabitLog(day = day, habitId = water.id)),
    medications = listOf(pill),
    displayName = name,
)

@Composable
private fun Today(data: TodayData, completion: TodayCompletion = TodayCompletion()) {
    ViraTheme(reduceMotion = true) {
        TodayContent(data, completion, TodayCallbacks(), isViriExpanded = true, isTomorrowExpanded = true)
    }
}

/// El matí: la primera mirada del dia, amb el «bon dia» de Viri.
@PreviewTest @TodayCaptures @Composable
fun TodayMorning() = Today(data(at(7, 45), items = listOf(essay, maths.copy(statusRaw = PlanItemStatus.PENDING.raw), football, dentist)))

/// Migdia, amb una tasca marcada i la finestra de «Desfer» oberta a la seva fila.
@PreviewTest @TodayCaptures @Composable
fun TodayMidday() = Today(
    data(at(13, 15), items = listOf(essay, doneTask, maths.copy(statusRaw = PlanItemStatus.DONE.raw), football, dentist)),
    TodayCompletion(ActiveCompletion("plan-${maths.id}", CompletionPhase.Pending)),
)

/// Vespre: Viri tanca el dia i surt la targeta de demà.
@PreviewTest @TodayCaptures @Composable
fun TodayNight() = Today(data(at(21)))

/// Un dia sense res apuntat i sense nom.
@PreviewTest @TodayCaptures @Composable
fun TodayEmpty() = ViraTheme(reduceMotion = true) {
    TodayContent(
        TodayData(now = at(16), zone = zone),
        TodayCompletion(),
        TodayCallbacks(),
        isViriExpanded = true,
        isTomorrowExpanded = false,
    )
}

@PreviewTest @CaptureCaptures @Composable
fun QuickCapture() = ViraTheme(reduceMotion = true) {
    QuickCaptureContent(initialDay = null, initialPeriod = null, onCancel = {}, onSave = { _, _, _, _, _ -> }, zone = zone, now = at(9))
}

@PreviewTest @CaptureCaptures @Composable
fun QuickCaptureFromPlan() = ViraTheme(reduceMotion = true) {
    QuickCaptureContent(initialDay = tomorrow, initialPeriod = QuickCapturePeriod.Night, onCancel = {}, onSave = { _, _, _, _, _ -> }, zone = zone, now = at(9))
}
