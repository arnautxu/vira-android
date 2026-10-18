package cat.vira.android.features.progress

import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.features.progress.logic.ViraProgress

/// La porta única per registrar una acció completada des de qualsevol pantalla
/// (`ViraProgress.record` de l'iPhone): monedes i XP **i** la ratxa del dia, alhora. Torna
/// (monedes concedides, XP guanyada), que és el que celebra la pantalla que l'ha cridat.
///
/// **Mai des de medicació**: `CoinReason` no en té el cas. I només en completar: desmarcar no resta
/// res enlloc.
suspend fun recordProgress(backend: ViraBackend, reason: CoinReason): Pair<Int, Int> {
    val coins = ViraProgress(ViraStore(backend.db)).record(reason)
    return coins to (ViraRewards.rule(reason)?.coins ?: 0)
}
