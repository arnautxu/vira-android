package cat.vira.android.features.shop

import android.content.SharedPreferences
import androidx.room.withTransaction
import cat.vira.android.core.data.ViraCardTallies
import cat.vira.android.core.data.ViraDatabase
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
    suspend fun isAvailable(
        item: BadgeItem, counter: ProgressCounter?, streak: Streak?, tallies: Tallies = Tallies.EMPTY,
    ): Boolean =
        if (item.isEarned) {
            CardItem.item(item.key)?.isUnlocked(counter, streak, tallies)
                ?: item.requirement?.let { it.current(counter, streak, tallies) >= it.target } ?: false
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
///
/// Les cartes es miren amb els recomptes de les cartes d'aquestes mateixes preferències
/// (`ViraCardTallies`): qui avalua i qui recorda llegeixen del mateix lloc.
class ViraCardCelebrations(private val preferences: SharedPreferences) {

    private val tallies = ViraCardTallies(preferences)

    val seenKeys: Set<String>
        get() = list(SEEN_KEY).toSet()

    val isSeeded: Boolean
        get() = preferences.getBoolean(SEEDED_KEY, false)

    /// Les guanyades ara mateix, amb els recomptes d'ara.
    fun unlocked(counter: ProgressCounter?, streak: Streak?): List<CardItem> =
        CardItem.unlocked(counter, streak, tallies.snapshot())

    /// Es marca **abans** d'ensenyar-la: si l'app mor a mig confeti, la festa no torna.
    fun markAsSeen(keys: List<String>) {
        val current = list(SEEN_KEY)
        val added = keys.filter { it !in current }.distinct()
        if (added.isEmpty()) return
        preferences.edit().putString(SEEN_KEY, (current + added).joinToString(",")).apply()
    }

    fun markAsSeen(key: String) = markAsSeen(listOf(key))

    /// Dona per vist tot el que ja està guanyat, sense celebrar-ho. Ho ha de cridar la
    /// sincronització després d'escriure els comptadors que baixen del compte: el que ve del
    /// compte no és una fita d'ara.
    fun absorb(counter: ProgressCounter?, streak: Streak?) {
        markAsSeen(unlocked(counter, streak).map { it.key })
        preferences.edit().putBoolean(SEEDED_KEY, true).apply()
    }

    /// Les guanyades que encara no s'han ensenyat, en l'ordre del catàleg.
    fun pending(counter: ProgressCounter?, streak: Streak?): List<CardItem> {
        val seen = seenKeys
        return unlocked(counter, streak).filter { it.key !in seen }
    }

    /// Posa al dia els recomptes de les cartes (`ViraCardTallies.repair`) i decideix si el que surti
    /// guanyat ara és d'ara o d'abans. Es crida abans de mirar si hi ha cap carta per ensenyar.
    ///
    /// El dia que arriba una versió amb més cartes (de 16 a 67), qui ja porta mesos amb l'app en té
    /// moltes de guanyades de cop, i sense això li sortirien una darrere l'altra. El mateix quan la
    /// reparació puja un recompte: vol dir que les dades ja hi eren. Les cartes hi són a l'àlbum
    /// igualment; el que no hi ha és la festa d'una cosa que no acaba de passar.
    suspend fun prepare(db: ViraDatabase) {
        val raised = tallies.repair(db)
        val catalog = CardItem.catalog.map { it.key }
        val known = preferences.getString(KNOWN_KEY, null)?.split(',')?.toSet() ?: CardItem.firstCatalogKeys
        val grew = !known.containsAll(catalog)
        if ((raised || grew) && isSeeded) {
            absorb(db.progressCounters().all().firstOrNull(), db.streaks().all().firstOrNull())
        }
        if (grew) preferences.edit().putString(KNOWN_KEY, catalog.joinToString(",")).apply()
    }

    private fun list(key: String): List<String> =
        preferences.getString(key, null)?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    companion object {
        /// Mateixos noms que l'iPhone (`ViraPreferences.celebratedCardKeys`…). El prefix `cards.` fa
        /// que caiguin en canviar de compte.
        const val SEEN_KEY = "cards.celebratedKeys"
        const val SEEDED_KEY = "cards.celebratedSeeded"
        const val KNOWN_KEY = "cards.knownKeys"
    }
}

/// Què va a dalt de l'àlbum i quan porta l'etiqueta «Nova» (`ViraCardShelf`, Arnau, 2026-10-22).
///
/// Només la porta **l'última carta guanyada**, i només fins que l'obres. Si la portessin totes les
/// que no has obert, una sincronització que baixa vint cartes d'un altre telèfon ompliria l'àlbum
/// d'etiquetes, i una etiqueta que és a tot arreu ja no diu res.
///
/// L'ordre surt de les cartes celebrades, on cada carta s'afegeix quan es guanya (o quan s'absorbeix
/// sense festa). És d'aquest telèfon i no viatja: en un telèfon nou, la de dalt és la més cara.
class ViraCardShelf(private val preferences: SharedPreferences) {

    /// La carta de dalt de l'àlbum: l'última que s'ha guanyat d'entre les que tens.
    fun latest(owned: List<CardItem>): CardItem? {
        val ownedKeys = owned.map { it.key }.toSet()
        list(ViraCardCelebrations.SEEN_KEY).lastOrNull { it in ownedKeys }?.let { key ->
            return owned.first { it.key == key }
        }
        return owned.maxByOrNull { it.rarity }
    }

    /// Si la carta porta «Nova»: és la de dalt i encara no s'ha obert.
    fun isNew(card: CardItem, owned: List<CardItem>): Boolean {
        if (latest(owned)?.key != card.key) return false
        val opened = preferences.getString(OPENED_KEY, null) ?: return false
        return card.key !in opened.split(',')
    }

    fun markOpened(key: String) {
        val opened = list(OPENED_KEY)
        if (key.isEmpty() || key in opened) return
        preferences.edit().putString(OPENED_KEY, (opened + key).joinToString(",")).apply()
    }

    /// La primera vegada que s'obre l'àlbum, tot el que ja s'havia guanyat compta com a obert: la
    /// versió que porta l'àlbum no ha de posar «Nova» a una carta de fa mesos.
    fun seedIfNeeded() {
        if (preferences.contains(OPENED_KEY)) return
        preferences.edit().putString(OPENED_KEY, list(ViraCardCelebrations.SEEN_KEY).joinToString(",")).apply()
    }

    private fun list(key: String): List<String> =
        preferences.getString(key, null)?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    companion object {
        const val OPENED_KEY = "cards.openedKeys"
    }
}
