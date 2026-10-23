package cat.vira.android.features.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore

/// El que les pantalles de la Botiga llegeixen i amb què escriuen. Les lectures són fluxos de Room
/// (la pantalla es redibuixa sola quan canvien), i tota escriptura passa per `ViraStore`.
class ShopData(val db: ViraDatabase, val preferences: android.content.SharedPreferences) {
    val store = ViraStore(db)
    private val rewards = ViraRewards(store)
    private val spend: ViraSpend = { amount, reason -> rewards.spend(amount, reason) }

    val skinsLogic = ViraSkins(store, spend)
    val badgesLogic = ViraBadges(store, spend)
    val personalRewardsLogic = ViraPersonalRewards(store, spend)
    val cardCelebrations = ViraCardCelebrations(preferences)

    val counter = db.progressCounters().observe()
    val streak = db.streaks().observe()
    val skins = db.skinOwnerships().observeAll()
    val badges = db.badgeOwnerships().observeAll()
    val accessories = db.accessoryOwnerships().observeAll()
    val profile = db.profiles().observe()
    val personalRewards = db.personalRewards().observeAll()
    val transactions = db.coinTransactions().observeAll()
}

@Composable
fun rememberShopData(): ShopData {
    val context = LocalContext.current
    val backend = LocalViraBackend.current
    return remember(backend) { ShopData(backend.db, ViraPreferences.open(context)) }
}
