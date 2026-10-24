package cat.vira.android.features.today

import androidx.compose.runtime.Composable
import cat.vira.android.features.add.ViraAddKind
import cat.vira.android.features.add.ViraAddSheet
import cat.vira.android.features.plan.DayPeriod
import java.time.Instant
import java.time.ZoneId

/// Captura ràpida: el full «Afegir» obert a la cara **Tasca** (`QuickCaptureView`, Arnau, 2026-10-18).
///
/// Es queda amb aquest nom perquè l'obren Avui (el «+» i «Per a demà») i el calendari del Pla, i
/// tots volen el mateix: escriure una cosa i prou. El full i les cares viuen a `features/add/`.
@Composable
fun QuickCaptureScreen(
    onDismiss: () -> Unit,
    /// Dia inicial. El calendari hi passa el dia triat perquè no s'hagi de tornar a triar.
    initialDay: Instant? = null,
    /// Franja de destí quan s'afegeix des d'un forat concret del calendari.
    initialPeriod: DayPeriod? = null,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    ViraAddSheet(
        onDismiss = onDismiss,
        kind = ViraAddKind.Task,
        initialDay = initialDay?.atZone(zone)?.toLocalDate(),
        initialPeriod = initialPeriod,
    )
}
