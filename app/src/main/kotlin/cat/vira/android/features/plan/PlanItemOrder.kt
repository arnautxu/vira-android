package cat.vira.android.features.plan

import cat.vira.android.core.data.model.PlanItem
import java.time.Instant
import java.time.ZoneId

/// Ordre dels ítems d'un dia: quin va abans, i què passa quan l'usuari els arrossega
/// (`PlanItemOrder` de l'iPhone).
///
/// ## Per què no n'hi ha prou amb l'hora
/// En TDAH, l'ordre en què vols atacar les coses no sempre és l'ordre del rellotge, i les tasques
/// sense hora quedarien totes amuntegades al mateix lloc. Per això hi ha `sortIndex`, i **mana
/// ell**: si l'usuari mou una fila, es queda on l'ha deixat encara que tingui hora.
///
/// ## L'ordre per defecte segueix sent el del rellotge
/// `sortIndex == 0` vol dir "ningú ho ha tocat", i llavors el rang es **dedueix** de l'hora. Així
/// els ítems que arriben de l'iPhone s'ordenen igual als dos telèfons fins que algú els mogui.
object PlanItemOrder {

    /// Rang deduït de l'hora, en centèsimes de minut del dia. L'escala deixa 99 posicions lliures
    /// entre dos minuts consecutius. El `+1` reserva el zero com a "sense ordre manual".
    fun defaultRank(time: Instant?, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (time == null) return UNTIMED_RANK
        val local = time.atZone(zone)
        return (local.hour * 60 + local.minute + 1) * 100
    }

    /// Les tasques sense hora van al final del Matí: després del que sí que té hora al matí, i
    /// just abans del migdia.
    val UNTIMED_RANK = DayPeriod.Midday.startHour * 60 * 100 - 1

    /// Rang efectiu: el manual si n'hi ha, i si no el de l'hora.
    fun rank(item: PlanItem, zone: ZoneId = ZoneId.systemDefault()): Int =
        if (item.sortIndex != 0) item.sortIndex else defaultRank(item.time, zone)

    /// Franja on cau un ítem. **Es deriva de la posició, no de l'hora**: un cop arrossegat,
    /// l'ítem s'ha de quedar on l'han deixat també quan això vol dir canviar de franja.
    fun period(item: PlanItem, zone: ZoneId = ZoneId.systemDefault()): DayPeriod =
        DayPeriod.containing(rank(item, zone) / 100 / 60)

    /// Ordena els ítems d'un dia. Els empats els desfà l'ordre de creació.
    fun sorted(items: List<PlanItem>, zone: ZoneId = ZoneId.systemDefault()): List<PlanItem> =
        items.sortedWith(compareBy<PlanItem>({ rank(it, zone) }, { it.createdAt }))

    /// Aplica un arrossegament de `from` a `to` (posició final, com a la llista resultant).
    ///
    /// Reparteix entre els ítems moguts **els rangs que aquell grup ja ocupava**, en comptes de
    /// renumerar-los d'un a n: un grup del matí no pot saltar per damunt de la nit, i el
    /// reendreçat d'una franja no toca cap altra fila del dia.
    ///
    /// Torna **només els ítems que canvien**, amb el `sortIndex` nou; qui crida els desa.
    ///
    /// - Parameter items: el grup **tal com es veu**, ja ordenat.
    fun move(items: List<PlanItem>, from: Int, to: Int, zone: ZoneId = ZoneId.systemDefault()): List<PlanItem> {
        if (items.size <= 1 || from !in items.indices) return emptyList()
        val reordered = items.toMutableList()
        val moved = reordered.removeAt(from)
        reordered.add(to.coerceIn(0, reordered.size), moved)

        val slots = items.map { rank(it, zone) }.sorted().toMutableList()
        // Estrictament creixent: dos ítems amb el mateix rang deixarien l'ordre en mans del
        // desempat per data de creació, que és justament el que l'usuari acaba de canviar.
        for (index in 1 until slots.size) {
            if (slots[index] <= slots[index - 1]) slots[index] = slots[index - 1] + 1
        }
        return reordered.zip(slots).mapNotNull { (item, slot) ->
            if (item.sortIndex != slot) item.copy(sortIndex = slot) else null
        }
    }
}
