package cat.vira.android.features.add

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemRecurrence
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.features.plan.DayPeriod
import cat.vira.android.features.plan.PlanDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/// L'hora d'una tasca: cap, una franja del dia (com les del Pla) o una hora exacta (`QuickCaptureTime`).
sealed interface AddTime {
    data object None : AddTime
    data class Period(val period: DayPeriod) : AddTime
    data object Exact : AddTime

    companion object {
        /// Les fitxes del pas Hora: les franges del Pla i l'hora exacta.
        val choices: List<AddTime> = DayPeriod.entries.map { Period(it) } + Exact
    }
}

/// Una tasca desada a la cadena de «+ Una altra», per ensenyar-la i poder-la desfer
/// (`QuickCaptureAdded`). `items` és la sèrie sencera si es repeteix.
data class AddedTask(
    val title: String,
    val symbolName: String,
    val day: LocalDate,
    val time: LocalTime?,
    val items: List<PlanItem>,
    val id: UUID = UUID.randomUUID(),
)

/// Les dades de la cara Tasca (`QuickCaptureForm` + `+Logic` + `+Flow`): què es desa, què ha llegit
/// la frase i com es passa d'un pas a l'altre. El títol és l'únic que cal; la resta té valor per
/// defecte, i el que la frase o el calendari ja diuen no es pregunta.
@Stable
class TaskAddState(
    val today: LocalDate,
    /// El dia del calendari des d'on s'ha obert: la frase no el pot moure.
    val initialDay: LocalDate? = null,
    /// La franja del forat del calendari on s'ha tocat.
    val initialPeriod: DayPeriod? = null,
    now: LocalTime = LocalTime.of(9, 0),
    initialStep: TaskAddStep = TaskAddStep.What,
    initialTitle: String = "",
) {
    var step by mutableStateOf(initialStep)
    var title by mutableStateOf("")
        private set
    var symbolName by mutableStateOf(ViraPictogramName.flag)
    var isPickingIcon by mutableStateOf(false)
    var selectedDay by mutableStateOf(initialDay ?: today)
        private set
    var isPickingDay by mutableStateOf(false)
    var timeChoice by mutableStateOf<AddTime>(initialPeriod?.let { AddTime.Period(it) } ?: AddTime.None)
        private set
    var exactTime by mutableStateOf(now.withSecond(0).withNano(0))
    var recurrence by mutableStateOf(PlanItemRecurrence.NONE)
    var detection by mutableStateOf(CapturePhrase.Detection.NONE)
        private set
    var dayWasChosen by mutableStateOf(initialDay != null)
        private set
    private var timeWasChosen by mutableStateOf(initialPeriod != null)
    private var ignoresDetectedDay by mutableStateOf(false)
    private var ignoresDetectedTime by mutableStateOf(false)
    val justAdded = mutableStateListOf<AddedTask>()

    init {
        if (initialTitle.isNotEmpty()) updateTitle(initialTitle)
    }

    val tomorrow: LocalDate get() = today.plusDays(1)
    val laterDays: List<LocalDate> get() = (2L..6L).map { today.plusDays(it) }
    val canSave: Boolean get() = title.isNotBlank()

    /// El dia que ha llegit la frase, si encara mana: tocar un dia a mà o la creu el treuen. La frase
    /// no sap d'un dia passat.
    val detectedDay: LocalDate?
        get() = if (dayWasChosen || ignoresDetectedDay) null else detection.day?.takeUnless { it.isBefore(today) }

    val detectedTime: LocalTime?
        get() = if (timeWasChosen || ignoresDetectedTime) null else detection.time

    /// El dia que es desarà.
    val day: LocalDate get() = detectedDay ?: selectedDay

    /// L'hora que es desarà, o `null`. Una franja es desa amb la seva hora per defecte, la mateixa
    /// que «Afegir aquí» del calendari.
    val time: LocalTime?
        get() = detectedTime ?: when (val choice = timeChoice) {
            AddTime.None -> null
            is AddTime.Period -> LocalTime.of(choice.period.defaultHour, 0)
            AddTime.Exact -> exactTime
        }

    /// El títol sense «demà a les 5» només si tot el que s'ha llegit s'ha fet servir. Si se n'ha tret
    /// un tros, la frase es desa tal com és: millor un títol llarg que un de mutilat.
    val titleToSave: String
        get() {
            val readSomething = detection.day != null || detection.time != null
            val usedDay = detection.day == null || detectedDay != null
            val usedTime = detection.time == null || detectedTime != null
            return if (readSomething && usedDay && usedTime && detection.title.isNotEmpty()) detection.title else title.trim()
        }

    val dayIsGiven: Boolean get() = initialDay != null || detectedDay != null
    val timeIsGiven: Boolean get() = initialPeriod != null || detectedTime != null

    /// «Desar» del pas Hora necessita una fitxa triada; si no, és «Sense hora».
    val hasTime: Boolean get() = detectedTime != null || timeChoice != AddTime.None

    /// «Per avui» només té sentit si el full no s'ha obert des d'un altre dia del calendari.
    val offersForToday: Boolean get() = initialDay?.let { it == today } ?: true

    fun isChosen(option: AddTime): Boolean =
        if (option == AddTime.Exact) detectedTime != null || timeChoice == AddTime.Exact
        else detectedTime == null && timeChoice == option

    fun updateTitle(text: String) {
        title = text
        val found = CapturePhrase.detect(text, today)
        // Si la frase ja no diu cap dia, la creu d'abans no ha de valer per al pròxim.
        if (found.day == null) ignoresDetectedDay = false
        if (found.time == null) ignoresDetectedTime = false
        detection = found
    }

    fun choose(day: LocalDate) {
        selectedDay = day
        dayWasChosen = true
        isPickingDay = false
    }

    fun choose(time: AddTime) {
        timeChoice = time
        timeWasChosen = true
    }

    /// Si la frase ja deia una hora, el rellotge comença per aquella.
    fun chooseExactTime() {
        detectedTime?.let { exactTime = it }
        choose(AddTime.Exact)
    }

    fun clearDetectedDay() { ignoresDetectedDay = true }
    fun clearDetectedTime() { ignoresDetectedTime = true }

    /// El pas que ve després d'aquest, saltant el que ja se sap.
    fun nextStep(): TaskAddStep = step.next(dayIsGiven, timeIsGiven)

    /// On porta «Enrere». De tornada al camp, la frase torna a manar: si ara diu «demà», que es llegeixi.
    fun goBack(): Boolean {
        val previous = step.previous(dayIsGiven) ?: return false
        if (previous == TaskAddStep.What) {
            if (initialDay == null) dayWasChosen = false
            if (initialPeriod == null) {
                timeWasChosen = false
                timeChoice = AddTime.None
            }
            isPickingDay = false
        }
        step = previous
        return true
    }

    /// Desa i ho apunta a la cadena. Crear no dona res: el premi surt a Fet com a «en fer-la».
    fun save(create: (PlanItem, PlanItemRecurrence) -> List<PlanItem>, zone: ZoneId): Boolean {
        if (!canSave) return false
        val savedDay = day
        val savedTime = time
        val item = PlanItem(
            title = titleToSave,
            day = PlanDay.start(savedDay, zone),
            time = savedTime?.let { PlanDay.at(savedDay, it.hour, it.minute, zone) },
            symbolName = symbolName,
        )
        val items = create(item, recurrence)
        if (items.isEmpty()) return false
        justAdded += AddedTask(item.title, symbolName, savedDay, savedTime, items)
        return true
    }

    /// «+ Una altra»: el camp buit i tot com en obrir el full. La repetició no es queda: la
    /// següent heretaria una sèrie sense que ningú la triés.
    fun another() {
        updateTitle("")
        ignoresDetectedDay = false
        ignoresDetectedTime = false
        symbolName = ViraPictogramName.flag
        isPickingIcon = false
        isPickingDay = false
        recurrence = PlanItemRecurrence.NONE
        selectedDay = initialDay ?: today
        dayWasChosen = initialDay != null
        timeChoice = initialPeriod?.let { AddTime.Period(it) } ?: AddTime.None
        timeWasChosen = initialPeriod != null
        step = TaskAddStep.What
    }

    /// «Desfer» de l'última: se n'emporta la sèrie sencera si n'era una.
    fun undoLast(delete: (List<PlanItem>) -> Unit): AddedTask? {
        val last = justAdded.lastOrNull() ?: return null
        delete(last.items)
        justAdded.removeAt(justAdded.lastIndex)
        return last
    }

    /// «Desfer» a Fet: s'esborra i es torna al camp amb el títol, per arreglar-lo si calia.
    fun undoFromDone(delete: (List<PlanItem>) -> Unit) {
        val last = undoLast(delete) ?: return
        updateTitle(last.title)
        step = TaskAddStep.What
    }
}
