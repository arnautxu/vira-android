package cat.vira.android.features.plan

import java.time.LocalDate

/// El que Pla necessita de fora de la seva carpeta, i que a l'iPhone arriba per l'entorn:
/// la captura ràpida (Avui), la finestra de "Desfer" amb la recompensa (l'arrel de l'app) i
/// la reprogramació dels avisos (Notificacions).
///
/// Tot té un valor per defecte que no fa res, perquè la pestanya funcioni i es pugui provar abans
/// que cada peça arribi; l'arrel de l'app el connecta a les peces reals quan hi siguin.
data class PlanHost(
    /// Obre la captura ràpida (`QuickCaptureView(initialDay:initialPeriod:)`).
    val onCapture: (day: LocalDate, period: DayPeriod?) -> Unit = { _, _ -> },
    /// Obre la finestra de "Desfer" (`presentCompletion`). La recompensa la concedeix l'arrel,
    /// només si ningú la desfà; la medicació no hi passa mai.
    val onCompleted: (PlanCompletion) -> Unit = {},
    /// Tanca la finestra sense concedir res (`undoCompletion`).
    val onUndone: (id: String) -> Unit = {},
    /// La finestra oberta ara mateix, si n'hi ha (`activeCompletion`): la fila que la té ensenya
    /// "Fet · Desfer" en lloc del títol.
    val activeCompletionId: String? = null,
    /// Un hàbit o una pauta ha canviat: cal refer la finestra d'avisos
    /// (`ViraNotifications.reschedule`).
    val onScheduleChanged: () -> Unit = {},
)

/// Una tasca o un hàbit acabat de marcar (`PendingCompletion` de l'iPhone).
data class PlanCompletion(
    val id: String,
    val kind: Kind,
    val title: String,
    /// Desfà la marca. La finestra de "Desfer" la crida si l'usuari s'hi repensa.
    val undo: () -> Unit,
) {
    /// `CoinReason.planItem` i `.habit`: el que l'arrel passa a `ViraProgress.record`.
    enum class Kind { PlanItem, Habit }
}
