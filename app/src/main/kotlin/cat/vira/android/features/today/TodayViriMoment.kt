package cat.vira.android.features.today

import androidx.compose.ui.graphics.vector.ImageVector
import cat.vira.android.core.data.model.PlanItem

/// El que diu Viri en un moment concret, i què es pot fer des d'allà (`TodayViriMoment`). El
/// dibuixa `TodayViriPill`, a sobre de «El teu dia».
internal data class TodayViriMomentUi(
    /// Canvia quan canvia el missatge, perquè l'entrada s'animi només llavors.
    val id: String,
    val scene: String,
    val title: String,
    val message: String,
    val actions: List<Action> = emptyList(),
    /// Viri proposa alguna cosa que la llista no fa (passar a demà, desfer-ho): la píndola s'obre
    /// sola. La resta es queda plegada fins que la toques.
    val proposes: Boolean = false,
) {
    data class Action(val title: String, val icon: ImageVector? = null, val run: () -> Unit)
}

/// Què diu Viri a dalt d'Avui, decidit sense pantalla (`TodayView+Viri.swift`). Quatre moments,
/// per aquest ordre:
/// 1. **Nit** (des de les 20:00): tanca el dia i proposa passar a demà el que ha quedat. Només si
///    avui hi havia alguna cosa.
/// 2. **Dia buit**: proposa apuntar alguna cosa.
/// 3. **Tot fet**: ho celebra i no demana res més.
/// 4. **Ara toca**: la següent cosa, amb el botó que li toca.
///
/// Les frases són preparades; cap botó escriu res que no es pugui desfer.
internal sealed interface TodayViriPlan {

    data class Night(
        /// L'última tasca passada a demà, per poder-la desfer des de la mateixa bombolla.
        val moved: PlanItem?,
        val doneCount: Int,
        val habitsDone: Int,
        /// La primera tasca pendent, que Viri proposa passar a demà.
        val moveCandidate: PlanItem?,
    ) : TodayViriPlan

    data class Empty(val isMorning: Boolean) : TodayViriPlan

    data class AllDone(val habitsLeft: Int) : TodayViriPlan

    data class Next(
        val entry: AgendaEntry,
        val isGentle: Boolean,
        /// El «bon dia»: res fet, cap salt i abans de migdia.
        val isFirstLook: Boolean,
        val isMorning: Boolean,
        val doneCount: Int,
        val total: Int,
        val habitsTotal: Int,
        val canSkip: Boolean,
    ) : TodayViriPlan {
        /// Una tasca sense hora es proposa amb Concentra't; la resta, amb «Fet» o «Presa».
        val isFocusTask: Boolean get() = entry.planItem?.time == null && entry.planItem != null
    }

    companion object {
        fun decide(
            agenda: Agenda,
            pending: List<AgendaEntry>,
            hour: Int,
            habitsTotal: Int,
            habitsDone: Int,
            skips: Int,
            isGentle: Boolean,
            moved: PlanItem?,
        ): TodayViriPlan {
            val doneCount = agenda.entries.count { it.isDone }
            val isMorning = hour < TodayPlanRules.Period.Midday.startHour
            val hasSomethingToday = !agenda.isEmpty || habitsTotal > 0
            if (hour >= TodayPlanRules.Period.Night.startHour && hasSomethingToday) {
                return Night(moved, doneCount, habitsDone, pending.firstNotNullOfOrNull { it.planItem })
            }
            if (agenda.isEmpty) return Empty(isMorning)
            if (pending.isEmpty()) return AllDone(habitsTotal - habitsDone)
            val entry = if (isGentle) pending.firstOrNull { it.isMedication } ?: pending[0] else pending[skips % pending.size]
            return Next(
                entry = entry,
                isGentle = isGentle,
                isFirstLook = doneCount == 0 && skips == 0 && isMorning,
                isMorning = isMorning,
                doneCount = doneCount,
                total = agenda.entries.size,
                habitsTotal = habitsTotal,
                canSkip = pending.size > 1,
            )
        }
    }
}
