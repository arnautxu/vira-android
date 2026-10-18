package cat.vira.android.features.focus

import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusSession
import java.time.Instant

/// El pont amb les recompenses (`ViraRewards.award(for:)` de l'iPhone): Concentra't no compta
/// monedes pel seu compte. 15 monedes a partir de 15 minuts, quatre cops al dia.
///
/// Falta el toc de ratxa (`ViraStreak.touch`), que és de `features/progress`: quan
/// `ViraProgress.recordFocus` sigui a `main`, aquesta crida passa a ser aquella, que fa les dues
/// coses. Una sessió completada **no compta el dia** per si sola (Arnau, 2026-08-10).
internal object FocusProgress {
    suspend fun reward(store: ViraStore, session: FocusSession, now: Instant): Int =
        ViraRewards(store).award(session, now)
}
