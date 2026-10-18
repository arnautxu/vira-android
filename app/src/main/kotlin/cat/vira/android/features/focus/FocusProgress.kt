package cat.vira.android.features.focus

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusSession
import java.time.Instant

/// El pont amb el progrés (`ViraRewards.award(for:)` i `ViraStreak.touch` de l'iPhone), que és de
/// `features/progress`: Concentra't no compta monedes ni dies pel seu compte.
///
/// Una sessió completada **no compta el dia** per si sola (Arnau, 2026-08-10): només descongela Viri.
internal object FocusProgress {
    suspend fun reward(store: ViraStore, session: FocusSession, now: Instant): Int = 0
}
