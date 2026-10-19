package cat.vira.android.features.today

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.features.plan.PlanWrites
import java.time.ZoneId

/// El que Avui escriu, sempre a través de `ViraStore` perquè tot quedi datat i viatgi.
///
/// Només «Passa a demà», que és d'Avui. Marcar, desfer, registrar una presa, esborrar i capturar
/// són del Pla (`PlanActions`).
internal class TodayActions(private val store: ViraStore, private val zone: () -> ZoneId = ZoneId::systemDefault) {

    private val db get() = store.db

    /// Passa una tasca a demà a la mateixa hora. Retorna com era, per poder-ho desfer.
    /// Amb el pany i la transacció del Pla: una sincronització no pot entrar entre llegir i escriure.
    suspend fun moveToTomorrow(item: PlanItem): PlanItem? = PlanWrites.locked(db) {
        val current = db.planItems().byId(item.id) ?: return@locked null
        val tomorrow = TodayPlanRules.addDays(current.day, 1, zone())
        val time = current.time?.let { TodayPlanRules.addDays(it, 1, zone()) }
        store.save(
            db.planItems(),
            current.copy(day = tomorrow, time = time, sortIndex = TodayPlanRules.defaultRank(time, zone())),
        )
        current
    }

    suspend fun undoMove(original: PlanItem) = PlanWrites.locked(db) {
        val current = db.planItems().byId(original.id) ?: return@locked
        store.save(db.planItems(), current.copy(day = original.day, time = original.time, sortIndex = original.sortIndex))
    }
}
