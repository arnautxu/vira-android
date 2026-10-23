package cat.vira.android.features.shop

import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

/// L'empenta de la Botiga (`ShopCollection`, Arnau, 2026-10-22: «encara trobo que li falta un push
/// al procés»): la col·lecció que creix, els objectius que venen després i el ritme cap a l'objectiu.
///
/// Tot es calcula al telèfon a partir del que ja hi ha (propietats, preus i el llibre de monedes).
/// Res d'això es desa ni viatja. Cap compte enrere, cap data límit, cap comparació amb ningú
/// (`VIRI_AND_GAMIFICATION.md` §2.5): el ritme és una estimació teva, no un termini.
object ShopCollection {

    /// Les skins que compten per a la col·lecció: les que es venen i les retirades que ja tens.
    fun catalog(owned: Set<String>): List<SkinItem> =
        SkinItem.catalog.filter { it.artIsAvailable && (!it.isRetired || it.key in owned) }

    /// Quantes en tens de les que hi ha.
    fun progress(items: List<SkinItem>, owned: Set<String>): Pair<Int, Int> =
        items.count { it.key in owned } to items.size

    /// Els objectius que proposem després de comprar-ne una: **una per raresa** que encara et falta,
    /// la primera del catàleg, i com a molt tres. Amb quatre rareses obertes, es queden les dues més
    /// a prop i la més alta: una que t'arriba aviat i una per somiar.
    fun nextGoals(catalog: List<SkinItem>, owned: Set<String>): List<SkinItem> {
        val firstPerTier = SkinTier.entries.mapNotNull { tier ->
            catalog.firstOrNull { it.tier == tier && !it.isRetired && it.key !in owned }
        }
        if (firstPerTier.size <= 3) return firstPerTier
        return firstPerTier.take(2) + firstPerTier.last()
    }

    /// La destacada de dalt (`ShopFeaturedCard.featured`): la que has marcat amb «La vull», si encara
    /// no la tens i es ven; si no, la següent que pots tenir. `null` si ja les tens totes.
    fun featured(catalog: List<SkinItem>, owned: Set<String>, wish: String): Pair<SkinItem, Boolean>? {
        catalog.firstOrNull { it.key == wish }?.let { wished ->
            if (wished.artIsAvailable && !wished.isRetired && wished.key !in owned) return wished to true
        }
        return SkinItem.next(catalog, owned)?.let { it to false }
    }

    // Ritme

    /// Els dies que mira el ritme: prou per no dependre d'un dia bo o dolent.
    const val PACE_WINDOW_DAYS = 14L
    /// Amb menys dies amb monedes que aquests, no hi ha prou per estimar res.
    const val MINIMUM_ACTIVE_DAYS = 3
    /// Més enllà d'aquí, l'estimació desanima més que no ajuda: no es diu.
    const val LONGEST_ESTIMATE_DAYS = 60

    /// Les monedes que guanyes de mitjana per dia, comptant els darrers catorze dies (també els que
    /// no en vas guanyar cap). `null` si encara no hi ha prou història.
    fun dailyPace(transactions: List<CoinTransaction>, now: Instant, zone: ZoneId = ZoneId.systemDefault()): Double? {
        val start = now.atZone(zone).toLocalDate().minusDays(PACE_WINDOW_DAYS).atStartOfDay(zone).toInstant()
        val earned = transactions.filter {
            it.amount > 0 && CoinReason.from(it.reasonRaw).isEarning && it.createdAt >= start && it.createdAt <= now
        }
        val activeDays = earned.map { it.createdAt.atZone(zone).toLocalDate() }.toSet()
        if (activeDays.size < MINIMUM_ACTIVE_DAYS) return null
        return earned.sumOf { it.amount }.toDouble() / PACE_WINDOW_DAYS
    }

    /// Uns quants dies, al teu ritme, per arribar-hi. `null` si ja t'hi arribes, si no hi ha ritme o
    /// si seria tan lluny que no paga la pena dir-ho.
    fun daysToReach(missing: Int, pace: Double?): Int? {
        if (missing <= 0 || pace == null || pace <= 0) return null
        val days = ceil(missing / pace).toInt()
        return days.takeIf { it <= LONGEST_ESTIMATE_DAYS }
    }

    /// L'objectiu que s'acaba d'atènyer amb aquest canvi de saldo, si n'hi ha (`ViraShopGoalBanner`).
    /// Només en **creuar** el preu i un sol cop per objectiu.
    fun goalReached(wish: String, from: Int, to: Int, owned: Set<String>, announced: String): SkinItem? {
        if (wish.isEmpty() || wish == announced || wish in owned) return null
        val item = SkinItem.item(wish) ?: return null
        if (!item.artIsAvailable || item.isRetired) return null
        return item.takeIf { from < it.price && to >= it.price }
    }
}

/// Les monedes que entren fent coses (no les compres, que són negatives).
val CoinReason.isEarning: Boolean
    get() = when (this) {
        CoinReason.PLAN_ITEM, CoinReason.HABIT, CoinReason.EMOTION, CoinReason.FOCUS, CoinReason.CHALLENGE -> true
        CoinReason.PURCHASE_SKIN, CoinReason.PURCHASE_FREEZE, CoinReason.PERSONAL_REWARD,
        CoinReason.PURCHASE_ACCESSORY, CoinReason.PURCHASE_BADGE -> false
    }
