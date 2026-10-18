package cat.vira.android.features.shop

import android.content.SharedPreferences
import androidx.room.withTransaction
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.BadgeOwnership
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.SkinOwnership
import cat.vira.android.core.data.model.Streak

/// Com es gasten ViriCoins: `ViraRewards.spend` de core/data. Retorna `false` i no toca
/// res si no n'hi ha prou. Es passa com a funció perquè la Botiga no depengui de com es desa.
typealias ViraSpend = suspend (amount: Int, reason: CoinReason) -> Boolean

/// Compra i equipament d'skins (`ViraSkins`). Del catàleg només en surten les dades de l'usuari:
/// què té comprat i què porta posat.
class ViraSkins(private val store: ViraStore, private val spend: ViraSpend) {

    private val db get() = store.db

    suspend fun owned(): List<SkinOwnership> = db.skinOwnerships().all()

    suspend fun isOwned(key: String): Boolean = owned().any { it.key == key }

    /// La posada, o `null` per al Viri de sempre, que sempre és una opció vàlida i gratuïta.
    suspend fun equipped(): SkinItem? = owned().firstOrNull { it.isEquipped }?.let { SkinItem.item(it.key) }

    /// Compra una skin i l'equipa, tot o res. `false` si no té art, ja és teva o no hi ha prou
    /// monedes: llavors no canvia res, ni el saldo.
    suspend fun buy(item: SkinItem): Boolean = db.withTransaction {
        if (!item.artIsAvailable || isOwned(item.key)) return@withTransaction false
        if (!spend(item.price, CoinReason.PURCHASE_SKIN)) return@withTransaction false
        // Qui l'acaba de comprar se la vol posar ara; si no, la compra no es veuria enlloc.
        store.create(db.skinOwnerships(), SkinOwnership(key = item.key))
        equip(item.key)
        true
    }

    /// Equipa una skin comprada, o el Viri de sempre amb `null`. **Només una alhora.** Només es
    /// dona per canviada (i es data) la fila que canvia de veritat.
    suspend fun equip(key: String?) = db.withTransaction {
        owned().forEach { row ->
            val wanted = row.key == key
            if (row.isEquipped != wanted) store.save(db.skinOwnerships(), row.copy(isEquipped = wanted))
        }
    }
}

/// Compra i penjat de badges al banner del Perfil (`ViraBadges`). Un de **guanyat** no es compra mai;
/// un de **comprat** es té per sempre. Penjar i despenjar és gratis i reversible.
class ViraBadges(private val store: ViraStore, private val spend: ViraSpend) {

    private val db get() = store.db

    suspend fun owned(): List<BadgeOwnership> = db.badgeOwnerships().all()

    suspend fun isOwned(key: String): Boolean = owned().any { it.key == key }

    /// Si es pot penjar: comprat i pagat, o guanyat i desbloquejat.
    suspend fun isAvailable(item: BadgeItem, counter: ProgressCounter?, streak: Streak?): Boolean =
        if (item.isEarned) {
            CardItem.item(item.key)?.isUnlocked(counter, streak)
                ?: item.requirement?.let { it.current(counter, streak) >= it.target } ?: false
        } else {
            isOwned(item.key)
        }

    /// Compra un badge i el penja. `false` si ja el té, si no hi ha prou monedes o si és guanyat
    /// (pagar per una cosa que diu que te l'has treballat no té sentit).
    suspend fun buy(item: BadgeItem): Boolean = db.withTransaction {
        val price = item.price ?: return@withTransaction false
        if (isOwned(item.key)) return@withTransaction false
        if (!spend(price, CoinReason.PURCHASE_BADGE)) return@withTransaction false
        store.create(db.badgeOwnerships(), BadgeOwnership(key = item.key))
        pin(item.key)
        true
    }

    suspend fun pinnedKeys(): List<String> = store.profile().pinnedBadgeKeys

    suspend fun isPinned(key: String): Boolean = key in pinnedKeys()

    /// Penja un badge. Amb el prestatge ple **cau el més antic**: penjar mai és un error que
    /// t'obliga a despenjar-ne un altre abans. Els badges penjats són del telèfon: no es data.
    suspend fun pin(key: String) {
        val profile = store.profile()
        if (key in profile.pinnedBadgeKeys) return
        val keys = (profile.pinnedBadgeKeys + key).takeLast(SLOTS)
        store.saveDeviceOnly(db.profiles(), profile.copy(pinnedBadgeKeys = keys))
    }

    suspend fun unpin(key: String) {
        val profile = store.profile()
        store.saveDeviceOnly(db.profiles(), profile.copy(pinnedBadgeKeys = profile.pinnedBadgeKeys - key))
    }

    suspend fun toggle(key: String) = if (isPinned(key)) unpin(key) else pin(key)

    companion object {
        /// Les ranures del prestatge. Un prestatge ple de tot no és una tria.
        const val SLOTS = 4
    }
}

/// Els premis personals (`PersonalRewardsView`): crear, arxivar i bescanviar.
class ViraPersonalRewards(private val store: ViraStore, private val spend: ViraSpend) {

    private val db get() = store.db

    /// Crea un premi. El títol buit no es desa; el cost es queda dins de [COST_RANGE].
    suspend fun create(title: String, cost: Int): PersonalReward? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        return store.create(db.personalRewards(), PersonalReward(title = trimmed, cost = cost.coerceIn(COST_RANGE)))
    }

    /// Arxivar, no esborrar: és reversible i no demana confirmació.
    suspend fun archive(reward: PersonalReward) {
        val current = db.personalRewards().byId(reward.id) ?: return
        store.save(db.personalRewards(), current.copy(isArchived = true))
    }

    /// Bescanvia un premi: gasta el cost i en compta un més. `false` si no hi ha prou monedes.
    suspend fun redeem(reward: PersonalReward): Boolean = db.withTransaction {
        val current = db.personalRewards().byId(reward.id) ?: return@withTransaction false
        if (!spend(current.cost, CoinReason.PERSONAL_REWARD)) return@withTransaction false
        store.save(db.personalRewards(), current.copy(redeemedCount = current.redeemedCount + 1))
        true
    }

    companion object {
        /// El sostre de 3.000 (§2.2.1): un premi no costa més que la peça més cara de la Botiga.
        val COST_RANGE = 10..3_000
        const val COST_STEP = 10
    }
}

/// Quines cartes ja s'han celebrat en aquest telèfon (`ViraCardCelebrations`). A les preferències i
/// no a la base de dades: «aquesta pantalla ja s'ha vist aquí» no viatja a un altre telèfon.
class ViraCardCelebrations(private val preferences: SharedPreferences) {

    val seenKeys: Set<String>
        get() = list().toSet()

    val isSeeded: Boolean
        get() = preferences.getBoolean(SEEDED_KEY, false)

    /// Es marca **abans** d'ensenyar-la: si l'app mor a mig confeti, la festa no torna.
    fun markAsSeen(keys: List<String>) {
        val current = list()
        val added = keys.filter { it !in current }.distinct()
        if (added.isEmpty()) return
        preferences.edit().putString(SEEN_KEY, (current + added).joinToString(",")).apply()
    }

    fun markAsSeen(key: String) = markAsSeen(listOf(key))

    /// Dona per vist tot el que ja està guanyat, sense celebrar-ho. Ho ha de cridar la
    /// sincronització després d'escriure els comptadors que baixen del compte: el que ve del
    /// compte no és una fita d'ara.
    fun absorb(counter: ProgressCounter?, streak: Streak?) {
        markAsSeen(CardItem.unlocked(counter, streak).map { it.key })
        preferences.edit().putBoolean(SEEDED_KEY, true).apply()
    }

    /// Les guanyades que encara no s'han ensenyat, en l'ordre del catàleg.
    fun pending(counter: ProgressCounter?, streak: Streak?): List<CardItem> {
        val seen = seenKeys
        return CardItem.unlocked(counter, streak).filter { it.key !in seen }
    }

    private fun list(): List<String> =
        preferences.getString(SEEN_KEY, null)?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    companion object {
        /// Mateixos noms que l'iPhone (`ViraPreferences.celebratedCardKeys`). El prefix `cards.` fa
        /// que caiguin en canviar de compte.
        const val SEEN_KEY = "cards.celebratedKeys"
        const val SEEDED_KEY = "cards.celebratedSeeded"
    }
}
