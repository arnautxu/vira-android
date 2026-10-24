package cat.vira.android.features.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.notifications.ViraNotificationPreferences
import cat.vira.android.features.plan.HabitProgress
import cat.vira.android.features.plan.PlanDay
import cat.vira.android.features.plan.rememberPlanActions
import java.time.LocalDate

/// El que el full «Afegir» necessita de fora: les escriptures, que són les del Pla (repeticions,
/// làpides, avisos i el «Desfer» de l'arrel), i dues lectures. Les captures en fan servir un de
/// buit, que no escriu res.
class AddSheetActions(
    /// Torna tots els ítems que es desen: la tasca i, si es repeteix, la seva sèrie.
    val createTask: (PlanItem, PlanItemRecurrence) -> List<PlanItem> = { item, _ -> listOf(item) },
    val deleteTasks: (List<PlanItem>) -> Unit = {},
    val saveHabit: (Habit) -> Unit = {},
    /// «Desfer» un hàbit acabat de crear: s'arxiva, que és el que fa treure un hàbit a tota l'app.
    val archiveHabit: (Habit) -> Unit = {},
    /// «Ja l'he fet avui»: el camí de sempre, amb el seu «Desfer» i la seva recompensa.
    val markHabitDoneToday: (Habit) -> Unit = {},
    val saveMedication: (Medication) -> Unit = {},
    /// Les monedes que donarà fer-ho aquell dia, o `null` si és avui i el sostre ja s'ha gastat.
    val coins: suspend (CoinReason, LocalDate) -> Int? = { reason, _ -> ViraRewards.rule(reason)?.coins },
    /// Les últimes tasques, de la més nova a la més vella, per a «Les de sempre».
    val recentTasks: suspend () -> List<PlanItem> = { emptyList() },
    /// Quantes coses hi ha cada dia d'avui fins a `until` (exclòs): els punts del calendari de «Quan».
    val dayCounts: suspend (LocalDate, LocalDate) -> Map<LocalDate, Int> = { _, _ -> emptyMap() },
    /// «Avisos» i els de tasques encesos a Ajustos: si no, la targeta del permís no surt.
    val planRemindersOn: Boolean = true,
    /// Els avisos de medicació encesos a Ajustos: si no, desar una pauta no demana el permís.
    val medicationRemindersOn: Boolean = true,
    /// El nom del fàrmac als avisos (Ajustos, apagat de fàbrica): si no hi surt, el pas de l'avís ho diu.
    val medicationNameInReminder: Boolean = false,
)

@Composable
fun rememberAddSheetActions(): AddSheetActions {
    val backend = LocalViraBackend.current
    val plan = rememberPlanActions()
    return remember(backend, plan) {
        val store = ViraStore(backend.db)
        AddSheetActions(
            createTask = plan::createAll,
            deleteTasks = plan::deleteAll,
            saveHabit = { plan.save(it, isNew = true) },
            archiveHabit = plan::archive,
            markHabitDoneToday = { plan.toggle(it, PlanDay.today(), HabitProgress(emptyList())) },
            saveMedication = { plan.save(it, isNew = true) },
            coins = { reason, day ->
                val rule = ViraRewards.rule(reason)
                when {
                    rule == null -> null
                    day != PlanDay.today() -> rule.coins
                    ViraRewards(store).grantsToday(reason) < rule.dailyCap -> rule.coins
                    else -> null
                }
            },
            recentTasks = { backend.db.planItems().all().sortedByDescending { it.createdAt }.take(300) },
            dayCounts = { from, until ->
                val zone = java.time.ZoneId.systemDefault()
                backend.db.planItems().all()
                    .map { PlanDay.date(it.day, zone) }
                    .filter { !it.isBefore(from) && it.isBefore(until) }
                    .groupingBy { it }.eachCount()
            },
            planRemindersOn = backend.preferences.getBoolean(ViraNotificationPreferences.REMINDERS_ENABLED, true) &&
                backend.preferences.getBoolean(ViraNotificationPreferences.REMINDERS_PLAN_ENABLED, true),
            medicationRemindersOn = backend.preferences.getBoolean(ViraNotificationPreferences.REMINDERS_MEDICATION_ENABLED, true),
            medicationNameInReminder = backend.preferences.getBoolean(ViraNotificationPreferences.NOTIFICATION_DETAIL_MEDICATION_ENABLED, false),
        )
    }
}
