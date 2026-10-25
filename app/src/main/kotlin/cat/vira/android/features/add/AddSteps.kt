package cat.vira.android.features.add

import java.util.Locale

/// Els passos de la cara Tasca (`QuickCaptureStep`, Arnau, 2026-10-19: «una pregunta cada
/// vegada»). **2026-10-24 (Arnau: «el dia i l'hora han d'anar en una sola pantalla», opció A + C)**:
/// **què** → **quan**, amb el dia i l'hora junts i tres moments d'un toc a dalt → **fet**. El que ja
/// se sap no es pregunta: si la frase diu «demà a les 5», o s'ha obert des d'un dia i una franja del
/// calendari, «Següent» desa directament.
///
/// **La partim?**: si la tasca sembla gran, abans de Fet Viri proposa partir-la en passos. No és
/// una pregunta més del recompte: surt amb el número de l'última i només quan el telèfon la veu gran.
enum class TaskAddStep {
    What, When, Split, Done;

    /// «1 de 2» a la capçalera. Fet no en porta: ja no és una pregunta. «La partim?» porta el de
    /// l'última, perquè no sumi una pregunta que la majoria de tasques no veuran.
    val number: Int? get() = when (this) {
        Done -> null
        Split -> QUESTION_COUNT
        else -> ordinal + 1
    }

    /// Només se salta «Quan» si se saben el dia i l'hora: amb el dia sol, «Quan» surt amb el dia ja
    /// marcat. `offersSplit`, que la tasca sembla gran i Viri pot proposar partir-la.
    fun next(dayIsGiven: Boolean, timeIsGiven: Boolean, offersSplit: Boolean = false): TaskAddStep {
        val end = if (offersSplit) Split else Done
        return when (this) {
            What -> if (dayIsGiven && timeIsGiven) end else When
            When -> end
            Split, Done -> Done
        }
    }

    /// On porta «Enrere». Des de Fet no se'n torna: la tasca ja és desada i té «Desfer». Des de
    /// «La partim?», a l'última pregunta que s'ha fet.
    fun previous(dayIsGiven: Boolean, timeIsGiven: Boolean = false): TaskAddStep? = when (this) {
        What, Done -> null
        When -> What
        Split -> if (dayIsGiven && timeIsGiven) What else When
    }

    companion object {
        const val QUESTION_COUNT = 2
    }
}

/// Els passos de crear un hàbit (`HabitAddStep`): tres preguntes i Fet.
enum class HabitAddStep {
    What, Days, Reminder, Done;

    val number: Int? get() = if (this == Done) null else ordinal + 1

    val previous: HabitAddStep? get() = when (this) {
        What, Done -> null
        Days -> What
        Reminder -> Days
    }

    val next: HabitAddStep get() = entries.getOrElse(ordinal + 1) { Done }
}

/// Els tres passos de crear una pauta (`MedicationAddStep`). No hi ha Fet: «Desar» tanca.
enum class MedicationAddStep {
    What, Time, Reminder;

    val number: Int get() = ordinal + 1
    val previous: MedicationAddStep? get() = entries.getOrNull(ordinal - 1)

    /// `null` després de l'últim: toca desar.
    val next: MedicationAddStep? get() = entries.getOrNull(ordinal + 1)
}

/// Minuts del dia (0…1439) amb − i + (`ViraClockMinutes`): l'hora de l'avís d'un hàbit i les de
/// les preses d'una pauta, com `Medication.timesInMinutes`.
object ClockMinutes {
    const val STEP = 15
    const val DAY_LENGTH = 24 * 60

    /// Avança o recula un pas i dona la volta a mitjanit: de les 23:45 es passa a les 0:00. Una
    /// hora desada a mà (8:10) s'arrodoneix al pas en moure-la, en lloc d'anar a 8:25.
    fun moved(minutes: Int, steps: Int): Int {
        val raw = minutes + steps * STEP
        val wrapped = ((raw % DAY_LENGTH) + DAY_LENGTH) % DAY_LENGTH
        return (wrapped / STEP) * STEP
    }

    /// L'hora de la presa nova: dues hores després de l'última. Si passa de mitjanit i aquella hora
    /// ja hi és, salta dues hores més: una fila repetida es perdria en desar i la pauta no seria la
    /// que es veu (`ViraClockMinutes.nextDose` de l'iPhone).
    fun nextDose(after: List<Int>): Int {
        var next = ((after.maxOrNull() ?: (9 * 60)) + 120) % DAY_LENGTH
        repeat(DAY_LENGTH / 120) { if (next in after) next = (next + 120) % DAY_LENGTH }
        return next
    }
}

/// Els dies d'un hàbit (1 = diumenge, com l'iPhone) amb les tres dreceres del pas «Quins dies»
/// (`WeekdayPicker.Preset`).
enum class WeekdayPreset(val days: Set<Int>) {
    Everyday(emptySet()),
    Weekdays(setOf(2, 3, 4, 5, 6)),
    Weekend(setOf(1, 7));

    companion object {
        /// La drecera que diuen aquests dies, si n'és una. Set dies és cada dia.
        fun matching(days: Set<Int>): WeekdayPreset? =
            entries.firstOrNull { it.days == days } ?: if (days.size == 7) Everyday else null
    }
}

/// «Les de sempre» del primer pas (`QuickCaptureUsual`): els títols que l'usuari repeteix, per
/// apuntar-los d'un toc. Res surt del telèfon: es calcula del pla local.
object CaptureUsual {
    data class Entry(val title: String, val symbolName: String)

    /// Els títols que surten com a mínim dues vegades, dels més repetits als menys; a igualtat, el
    /// més recent primer. `entries` ha d'anar del més recent al més antic.
    fun pick(entries: List<Entry>, limit: Int = 3): List<Entry> {
        val counts = linkedMapOf<String, Int>()
        val firstSeen = mutableMapOf<String, Pair<Int, Entry>>()
        entries.forEachIndexed { index, entry ->
            val key = entry.title.trim().lowercase(Locale.ROOT)
            if (key.isEmpty()) return@forEachIndexed
            counts[key] = (counts[key] ?: 0) + 1
            if (key !in firstSeen) firstSeen[key] = index to entry
        }
        return counts.filterValues { it >= 2 }.keys
            .sortedWith(compareByDescending<String> { counts.getValue(it) }.thenBy { firstSeen.getValue(it).first })
            .take(limit)
            .map { firstSeen.getValue(it).second }
    }
}
