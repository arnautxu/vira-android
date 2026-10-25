package cat.vira.android.core.notifications

import android.content.Context
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.IntakeStatus
import java.time.Instant
import java.util.UUID

/// El que els avisos necessiten de la resta de l'app. L'implementa `ViraApp`.
///
/// Els receptors d'Android (l'alarma, els botons, l'arrencada del telèfon) es creen sols, sense
/// res a dins: aquesta és la manera d'arribar a la base de dades i a les accions de cada pantalla
/// sense que `core/notifications` en tingui una còpia.
interface ViraNotificationHost {
    val notificationDatabase: ViraDatabase
    val notificationEffects: ViraNotificationEffects
}

/// Les escriptures que fan els botons de l'avís, i el que cal saber de la ratxa.
///
/// A l'iPhone, `ViraNotificationActions.handle` crida **els mateixos camins que la interfície**
/// (`ViraProgress`, `ViraMedication`, `ViraEmotionCheckIn`…), no una còpia paral·lela: una
/// recompensa calculada diferent segons d'on ve l'acció seria un error impossible de veure. Aquí
/// aquests camins són de cada pantalla (`features/…`) i s'hi connecten en arrencar l'app. Cada
/// un ha de complir el que diu l'iPhone:
///
/// - **`completeHabit`**: si el dia ja té registre, res (ni registre doble ni segona recompensa);
///   si no, el registre, la recompensa i `habitsCompletedCount`. Sense «Desfer».
/// - **`completePlanItem`**: si ja està feta, res; si no, feta, la propera repetició i la
///   recompensa.
/// - **`recordIntake`**: `ViraMedication.record`. **Cap recompensa**: ni monedes, ni XP, ni ratxa.
/// - **`logEmotion`**: `ViraEmotionCheckIn.record`, sense intensitat ni nota.
/// - **`capture`**: la frase llegida com la captura ràpida (dia i hora). **Cap recompensa**: apuntar
///   una tasca no dona res, com a l'iPhone des del 2026-10-24; el premi és per fer-la.
/// - **`streakState`**: `null` si encara no se sap; aleshores Viri no programa res.
/// - **`afterChange`**: el que s'hagi de posar al dia després d'una acció (els widgets).
///
/// Sense connectar, cap botó escriu res: és millor que una escriptura a mitges.
class ViraNotificationEffects(
    val completeHabit: suspend (habitId: UUID, day: Instant) -> Unit = { _, _ -> },
    val completePlanItem: suspend (planItemId: UUID) -> Unit = {},
    val recordIntake: suspend (medicationId: UUID, scheduledAt: Instant, status: IntakeStatus) -> Unit = { _, _, _ -> },
    val logEmotion: suspend (emotion: Emotion) -> Unit = {},
    val capture: suspend (text: String) -> Unit = {},
    val streakState: suspend (now: Instant) -> ViraStreakState? = { null },
    val reconcileStreak: suspend (now: Instant) -> ViraStreakReconcile = { ViraStreakReconcile.UNCHANGED },
    val afterChange: suspend () -> Unit = {},
)

internal val Context.notificationHost: ViraNotificationHost?
    get() = applicationContext as? ViraNotificationHost
