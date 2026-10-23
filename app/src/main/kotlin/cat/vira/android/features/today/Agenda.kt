package cat.vira.android.features.today

import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.model.IntakeStatus
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.form
import cat.vira.android.core.data.model.status
import java.text.Collator
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/// Agregació del dia a partir de les tres fonts amb hora: tasques, esdeveniments i medicació
/// (`Agenda.swift`). Fora de la pantalla perquè és la lògica amb més risc i s'ha de poder provar.
///
/// Els hàbits no hi són: no tenen hora, i dins d'una llista ordenada per hora queien sempre al
/// final i deixaven de veure's. Tenen bloc propi (`TodayHabitsStrip`).
class Agenda(
    planItems: List<PlanItem>,
    medications: List<Medication>,
    intakes: List<MedicationIntake>,
    day: Instant,
    zone: ZoneId,
    calendarEvents: List<TodayCalendarEvent> = emptyList(),
) {
    val entries: List<AgendaEntry>

    init {
        val result = mutableListOf<AgendaEntry>()

        // Els del calendari del sistema: per a l'usuari **són** el seu dia. No s'editen ni
        // s'esborren des d'aquí, i la fila ho diu.
        // Un esdeveniment que és **la mateixa tasca** (mateix títol, mateixa hora) no surt dues
        // vegades: es queda la de Vira, que és la que es pot editar i fa sumar la ratxa
        // (Arnau, 2026-10-22: «Agafar cartera» dues vegades a les 7:30).
        val planKeys = planItems.mapNotNull { item -> item.time?.let { matchKey(item.title, it) } }.toSet()
        for (event in calendarEvents.filter { matchKey(it.title, it.start) !in planKeys }) {
            result += AgendaEntry(
                id = "cal-${event.id}",
                title = event.title,
                subtitle = event.calendarName,
                time = event.start,
                isDone = event.isLocallyDone,
                rank = TodayPlanRules.defaultRank(event.start, zone),
                kind = AgendaEntry.Kind.Calendar(event),
            )
        }

        for (item in planItems) {
            result += AgendaEntry(
                id = "plan-${item.id}",
                title = item.title,
                time = item.time,
                isDone = item.status == PlanItemStatus.DONE,
                // L'ordre que l'usuari hagi decidit al calendari mana també aquí.
                rank = TodayPlanRules.rank(item, zone),
                kind = AgendaEntry.Kind.Plan(item),
            )
        }

        for (slot in TodayPlanRules.medicationSlots(medications, intakes, day, zone)) {
            result += AgendaEntry(
                id = "med-${slot.medication.id}-${slot.scheduledAt.epochSecond}",
                title = slot.medication.name,
                subtitle = slot.medication.dose,
                time = slot.scheduledAt,
                // Una presa saltada no compta com a feta, però tampoc com a pendent amb retret.
                isDone = slot.intake?.status == IntakeStatus.TAKEN,
                rank = TodayPlanRules.defaultRank(slot.scheduledAt, zone),
                kind = AgendaEntry.Kind.Medication(slot.medication),
            )
        }

        entries = result.sortedWith(order)
    }

    val isEmpty: Boolean get() = entries.isEmpty()

    /// Pendents primer, fets al final.
    val ordered: List<AgendaEntry> get() = ordered(holding = null)

    /// El mateix ordre, però **retenint** una fila a la seva posició de pendent mentre dura el
    /// «Desfer»: el missatge s'ha de poder llegir quiet, i la fila baixa quan es tanca.
    fun ordered(holding: String?, now: Instant = Instant.now()): List<AgendaEntry> {
        val held = holding?.let { id -> entries.firstOrNull { it.id == id && it.isDone } }
        val pending = now(now) + rest(now)
        if (held == null) return pending + done
        return (pending + held).sortedWith(order) + done.filter { it.id != held.id }
    }

    /// El que toca ara mateix: pendents amb hora dins de la propera hora.
    fun now(now: Instant = Instant.now()): List<AgendaEntry> {
        val cutoff = now.plus(Duration.ofHours(1))
        return entries.filter { !it.isDone && (it.time?.let { time -> time <= cutoff } ?: false) }
    }

    fun rest(now: Instant = Instant.now()): List<AgendaEntry> {
        val nowIds = now(now).map { it.id }.toSet()
        return entries.filter { !it.isDone && it.id !in nowIds }
    }

    val done: List<AgendaEntry> get() = entries.filter { it.isDone }

    companion object {
        private val collator: Collator = Collator.getInstance()

        /// Per posició dins del dia; els empats, pel títol en l'ordre de l'idioma.
        private val order = Comparator<AgendaEntry> { lhs, rhs ->
            if (lhs.rank == rhs.rank) collator.compare(lhs.title, rhs.title) else lhs.rank.compareTo(rhs.rank)
        }

        /// Títol sense majúscules, accents ni espais de les vores, i l'hora al minut. És el que fa
        /// que una tasca i el seu esdeveniment es reconeguin com la mateixa cosa.
        fun matchKey(title: String, time: Instant): String {
            val folded = java.text.Normalizer.normalize(title.trim(), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase(java.util.Locale.ROOT)
            return "${time.epochSecond.floorDiv(60)}|$folded"
        }
    }
}

data class AgendaEntry(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val time: Instant?,
    val isDone: Boolean,
    /// Posició dins del dia, a l'escala de `PlanItemOrder`.
    val rank: Int,
    val kind: Kind,
) {
    sealed interface Kind {
        data class Plan(val item: PlanItem) : Kind
        data class Medication(val medication: cat.vira.android.core.data.model.Medication) : Kind
        /// Ve del calendari del sistema: es mostra, no s'edita.
        data class Calendar(val event: TodayCalendarEvent) : Kind
    }

    /// Els esdeveniments externs no s'editen ni s'esborren: Vira no n'és la propietària.
    val isReadOnly: Boolean get() = kind is Kind.Calendar

    /// Tot es pot marcar. Un esdeveniment extern, només dins de Vira.
    val canToggle: Boolean get() = true

    val isExternal: Boolean get() = kind is Kind.Calendar
    val isMedication: Boolean get() = kind is Kind.Medication

    /// L'ítem del pla que hi ha darrere, quan n'hi ha. Una presa no ho és: es porta des de Medicació.
    val planItem: PlanItem? get() = (kind as? Kind.Plan)?.item

    /// Pictograma de **tipus**: el que va triar l'usuari per a les tasques, la forma del fàrmac per
    /// a la medicació i el calendari per als esdeveniments externs.
    val pictogramName: String
        get() = when (kind) {
            is Kind.Plan -> kind.item.symbolName
            is Kind.Medication -> "picto-med-${kind.medication.form.raw}"
            is Kind.Calendar -> "picto-calendar"
        }

    /// Etiqueta de tipus en text. El color mai és l'única distinció entre categories.
    @get:StringRes
    val typeLabel: Int
        get() = when (kind) {
            is Kind.Plan -> if (kind.item.time != null) R.string.type_event else R.string.type_task
            is Kind.Medication -> R.string.type_medication
            is Kind.Calendar -> R.string.type_calendar
        }
}
