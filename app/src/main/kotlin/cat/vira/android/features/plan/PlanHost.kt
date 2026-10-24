package cat.vira.android.features.plan

import cat.vira.android.core.design.components.PendingCompletion
import java.time.LocalDate

/// El que Pla necessita de fora de la seva carpeta, i que a l'iPhone arriba per l'entorn: la
/// finestra de "Desfer" amb la recompensa (`presentCompletion`, `undoCompletion`, de la cua de
/// l'arrel), la captura ràpida (Avui) i la reprogramació dels avisos (Notificacions).
///
/// `PlanScreen` el munta a partir dels `CompositionLocal` de l'app; les captures en fan servir un
/// de buit, que no fa res.
data class PlanHost(
    val present: (PendingCompletion) -> Unit = {},
    val undo: (id: String) -> Unit = {},
    /// Obre la captura ràpida (`QuickCaptureView(initialDay:initialPeriod:)`).
    val onCapture: (day: LocalDate, period: DayPeriod?) -> Unit = { _, _ -> },
    /// Un hàbit, una pauta o una presa ha canviat: cal refer la finestra d'avisos
    /// (`ViraNotifications.reschedule`).
    val onScheduleChanged: suspend () -> Unit = {},
    /// Una tasca o un hàbit acaba de quedar marcat (i ja compta): la nota de l'escala del dia
    /// (`ViraDaySound.playDone`). `closingDayOf` només per a una tasca, que pot tancar el dia.
    val onDone: suspend (closingDayOf: cat.vira.android.core.data.model.PlanItem?) -> Unit = {},
    /// Desmarcar sona com «Desfer» (`ViraSound.play(.undo)`).
    val onUndone: () -> Unit = {},
)
