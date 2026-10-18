package cat.vira.android.features.plan

import cat.vira.android.core.data.model.PlanItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// `PlanItemOrder`, calcat de `PlanItemOrderTests` de l'iPhone: que reendreçar una franja no toqui
/// la resta del dia, i que els ítems que ningú ha mogut segueixin ordenats per hora.
///
/// Diferència d'API: a l'iPhone `move(from:to:)` fa servir el `toOffset` de SwiftUI (posició
/// abans de treure l'ítem); aquí `to` és la posició final. Les proves ho tradueixen.
class PlanItemOrderTest {

    private val zone = ZoneId.of("Europe/Madrid")

    /// Dilluns 3 d'agost del 2026.
    private val day = LocalDate.of(2026, 8, 3)

    private var created = Instant.parse("2026-08-01T00:00:00Z")

    private fun item(title: String, hour: Int? = null, minute: Int = 0): PlanItem {
        created = created.plusSeconds(1)
        return PlanItem(
            title = title,
            day = PlanDay.start(day, zone),
            time = hour?.let { PlanDay.at(day, it, minute, zone) },
            createdAt = created,
        )
    }

    /// Aplica els canvis que torna `move` a la llista, com faria la base de dades.
    private fun List<PlanItem>.applying(changes: List<PlanItem>): List<PlanItem> {
        val byId = changes.associateBy { it.id }
        return map { byId[it.id] ?: it }
    }

    private fun titles(items: List<PlanItem>) = PlanItemOrder.sorted(items, zone).map { it.title }

    @Test fun `sense ordre manual mana l'hora`() {
        val items = listOf(item("Sopar", 21), item("Dinar", 14), item("Institut", 8))
        assertEquals(listOf("Institut", "Dinar", "Sopar"), titles(items))
    }

    @Test fun `les tasques sense hora van al final del mati`() {
        val items = listOf(item("Dinar", 14), item("Trucar a l'àvia"), item("Institut", 8))
        assertEquals(listOf("Institut", "Trucar a l'àvia", "Dinar"), titles(items))
    }

    @Test fun `un item a les 00 00 no es confon amb un que no s'ha mogut mai`() {
        assertNotEquals(0, PlanItemOrder.defaultRank(item("Nit", 0).time, zone))
    }

    @Test fun `arrossegar canvia l'ordre encara que els items tinguin hora`() {
        val first = item("Institut", 8)
        val second = item("Dutxa", 9)
        val items = listOf(first, second)

        val moved = items.applying(PlanItemOrder.move(items, from = 1, to = 0, zone = zone))

        assertEquals(listOf("Dutxa", "Institut"), titles(moved))
        // L'hora no es toca: la fila segueix dient a quina hora és.
        assertEquals(second.time, moved.first { it.id == second.id }.time)
    }

    @Test fun `reendrecar una franja no mou res de les altres`() {
        val morning = listOf(item("Institut", 8), item("Dutxa", 9))
        val night = item("Sopar", 21)

        val moved = morning.applying(PlanItemOrder.move(morning, from = 1, to = 0, zone = zone))

        assertEquals(listOf("Dutxa", "Institut", "Sopar"), titles(moved + night))
    }

    @Test fun `moure el mateix grup dos cops no encalla l'ordre`() {
        var items = listOf(item("A", 8), item("B", 8), item("C", 8))

        items = PlanItemOrder.sorted(items.applying(PlanItemOrder.move(items, from = 2, to = 0, zone = zone)), zone)
        assertEquals(listOf("C", "A", "B"), items.map { it.title })

        // A l'iPhone, `from: 0, to: 3`: al final.
        items = items.applying(PlanItemOrder.move(items, from = 0, to = 2, zone = zone))
        assertEquals(listOf("A", "B", "C"), titles(items))
    }

    @Test fun `arrossegar a la nit deixa l'item a la nit`() {
        val morning = item("Institut", 8)
        val night = item("Sopar", 21)
        assertEquals(DayPeriod.Morning, PlanItemOrder.period(morning, zone))

        // A l'iPhone, `from: 0, to: 2`.
        val moved = listOf(morning, night).applying(PlanItemOrder.move(listOf(morning, night), from = 0, to = 1, zone = zone))

        // Sense això, la fila tornaria al bloc del matí i el gest semblaria trencat.
        assertEquals(DayPeriod.Night, PlanItemOrder.period(moved.first { it.id == morning.id }, zone))
        assertEquals(DayPeriod.Morning, PlanItemOrder.period(moved.first { it.id == night.id }, zone))
    }

    @Test fun `sense moure res la franja segueix sortint de l'hora`() {
        assertEquals(DayPeriod.Morning, PlanItemOrder.period(item("Institut", 8), zone))
        assertEquals(DayPeriod.Midday, PlanItemOrder.period(item("Dinar", 14), zone))
        assertEquals(DayPeriod.Night, PlanItemOrder.period(item("Sopar", 21), zone))
        // Les tasques sense hora, al matí, que és on el calendari les ensenya.
        assertEquals(DayPeriod.Morning, PlanItemOrder.period(item("Trucar"), zone))
    }

    @Test fun `un sol item no es pot reendrecar i no peta`() {
        val only = item("Institut", 8)
        assertTrue(PlanItemOrder.move(listOf(only), from = 0, to = 0, zone = zone).isEmpty())
    }

    /// El rang és el mateix número que calcula l'iPhone: un ítem reendreçat en un telèfon queda al
    /// mateix lloc a l'altre.
    @Test fun `el rang per defecte es el de l'iPhone`() {
        assertEquals((8 * 60 + 30 + 1) * 100, PlanItemOrder.defaultRank(item("x", 8, 30).time, zone))
        assertEquals(13 * 60 * 100 - 1, PlanItemOrder.defaultRank(null, zone))
    }
}
