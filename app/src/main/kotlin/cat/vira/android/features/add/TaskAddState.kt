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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
    /// L'hora d'ara, per a «A una hora» i els moments. Les captures i les proves la deixen fixa.
    private val clock: () -> LocalTime = { now },
) {
    var step by mutableStateOf(initialStep)
    var title by mutableStateOf("")
        private set
    var symbolName by mutableStateOf(ViraPictogramName.flag)
    var isPickingIcon by mutableStateOf(false)
    var selectedDay by mutableStateOf(initialDay ?: today)
        private set
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
    /// Els tres moments d'un toc i els punts del calendari del pas «Quan» (`loadWhen`).
    var moments by mutableStateOf<List<CaptureMoment>>(emptyList())
        private set
    var dayCounts by mutableStateOf<Map<LocalDate, Int>>(emptyMap())
        private set

    init {
        if (initialTitle.isNotEmpty()) updateTitle(initialTitle)
    }

    val tomorrow: LocalDate get() = today.plusDays(1)
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

    /// Si s'ha triat un dia aquí, l'ha llegit la frase o l'ha passat el calendari. Si no, el dia és
    /// avui, ja marcat al calendari: «Desar» sempre desa.
    val isDayChosen: Boolean get() = dayWasChosen || detectedDay != null

    /// L'hora que es desarà, en minuts del dia, per a les rodes.
    val timeMinutes: Int get() = (time ?: exactTime).let { it.hour * 60 + it.minute }

    private val chosenPeriod: DayPeriod?
        get() = if (detectedTime == null) (timeChoice as? AddTime.Period)?.period else null

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

    // Pas «Quan» (`QuickCaptureForm+When.swift`)

    /// Els punts del calendari (`counts`, dia → quantes coses hi ha) i els tres moments, en entrar a
    /// «Quan». `history`, els moments de les tasques ja apuntades, de la més recent a la més antiga.
    fun loadWhen(counts: Map<LocalDate, Int>, history: List<CaptureMoment>, now: LocalDateTime) {
        dayCounts = counts
        // L'hora ja dita no es torna a proposar: només falta el dia, i el calendari és a sota.
        moments = if (timeIsGiven) emptyList() else CaptureMoment.suggestions(
            now = now,
            history = history,
            fixedDayOffset = if (dayIsGiven) ChronoUnit.DAYS.between(today, day).toInt() else null,
        )
    }

    /// Un moment és la resposta sencera: dia i franja. Qui crida desa.
    fun pick(moment: CaptureMoment) {
        val resolved = moment.resolved(today).toLocalDate()
        // Si la frase ja deia aquest dia, es queda manant ella: així el títol es desa net.
        if (detectedDay != resolved) choose(resolved)
        choose(AddTime.Period(moment.period))
    }

    fun pickNoTime() {
        if (!isDayChosen) choose(today)
        choose(AddTime.None)
    }

    /// «A una hora» comença a la que ja hi havia: la de la frase o la de la franja (Nit → 21:00). Si
    /// no n'hi havia cap, a la següent mitja hora rodona d'aquí a una estona (avui) o a les 17:00 (un
    /// altre dia). Mai a una hora sense relació amb el que ja s'havia triat.
    fun pickTime() {
        if (time != null) return
        if (!isDayChosen) choose(today)
        if (detectedTime == null) exactTime = chosenPeriod?.let { LocalTime.of(it.defaultHour, 0) } ?: suggestedExactTime()
        chooseExactTime()
    }

    /// Les rodes: moure-les vol dir que ara manen elles, no la frase.
    fun setTime(minutes: Int) {
        exactTime = LocalTime.of(minutes / 60, minutes % 60)
        if (detectedTime != null) ignoresDetectedTime = true
        choose(AddTime.Exact)
    }

    /// D'aquí a mitja hora, arrodonit a la mitja. Tard al vespre això ja és demà: el dia hi va
    /// també, perquè no quedi una hora passada d'avui.
    private fun suggestedExactTime(): LocalTime {
        if (day != today) return LocalTime.of(17, 0)
        val soon = today.atTime(clock()).plusMinutes(30).withSecond(0).withNano(0)
        val rounded = soon.plusMinutes(((30 - soon.minute % 30) % 30).toLong())
        if (rounded.toLocalDate() != today) choose(rounded.toLocalDate())
        return rounded.toLocalTime()
    }

    fun clearDetectedDay() { ignoresDetectedDay = true }
    fun clearDetectedTime() { ignoresDetectedTime = true }

    /// El pas que ve després d'aquest, saltant el que ja se sap.
    fun nextStep(offersSplit: Boolean = false): TaskAddStep = step.next(dayIsGiven, timeIsGiven, offersSplit)

    /// On porta «Enrere». De tornada al camp, la frase torna a manar: si ara diu «demà», que es llegeixi.
    fun goBack(): Boolean {
        val previous = step.previous(dayIsGiven, timeIsGiven) ?: return false
        if (previous == TaskAddStep.What) {
            if (initialDay == null) dayWasChosen = false
            if (initialPeriod == null) {
                timeWasChosen = false
                timeChoice = AddTime.None
            }
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
        // El títol desat ja no diu el dia ni l'hora de la frase: es tornen a posar a mà, perquè
        // arreglar el nom no se n'emporti el que ja s'havia triat.
        choose(last.day)
        last.time?.let { exactTime = it; choose(AddTime.Exact) }
        step = TaskAddStep.What
    }
}
