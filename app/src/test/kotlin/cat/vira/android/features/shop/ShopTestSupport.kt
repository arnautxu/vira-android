package cat.vira.android.features.shop

import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import java.time.Instant
import java.time.ZoneOffset

/// Una base de dades en memòria amb la Botiga connectada a l'economia de debò (`ViraRewards`).
internal class ShopFixture {
    val t0: Instant = Instant.ofEpochSecond(1_000_000)
    val db = ViraDatabase.inMemory(ApplicationProvider.getApplicationContext())
    val store = ViraStore(db, clock = { t0 }, zone = { ZoneOffset.UTC })
    private val rewards = ViraRewards(store, clock = { t0 }, zone = { ZoneOffset.UTC })
    private val spend: ViraSpend = { amount, reason -> rewards.spend(amount, reason) }

    val skins = ViraSkins(store, spend)
    val badges = ViraBadges(store, spend)
    val personal = ViraPersonalRewards(store, spend)

    suspend fun give(coins: Int) {
        store.save(db.progressCounters(), store.counter().copy(coinBalance = coins))
    }

    suspend fun balance(): Int = store.counter().coinBalance

    fun close() = db.close()
}
