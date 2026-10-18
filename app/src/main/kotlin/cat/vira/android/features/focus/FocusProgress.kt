package cat.vira.android.features.focus

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.features.progress.logic.ViraProgress
import java.time.Instant

/// El pont amb el progrés (`FocusSessionController.finish` de l'iPhone): Concentra't no compta
/// monedes ni toca la ratxa pel seu compte. 15 monedes a partir de 15 minuts, quatre cops al dia,
/// i una sessió completada descongela Viri. **No compta el dia** per si sola (Arnau, 2026-08-10).
internal object FocusProgress {
    suspend fun reward(store: ViraStore, session: FocusSession, now: Instant): Int =
        ViraProgress(store).recordFocus(session, now)
}
