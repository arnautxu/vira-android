package cat.vira.android.core.notifications

import androidx.annotation.StringRes
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PlanItem
import java.time.Instant
import java.util.Locale

/// Els textos, separats de `Context` perquè el càlcul es pugui provar sense pantalla.
fun interface ViraNotificationText {
    fun get(@StringRes id: Int, vararg args: Any): String
}

/// Una fotografia del que hi ha a la base de dades, que és tot el que el càlcul necessita.
data class ViraNotificationInputs(
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val intakes: List<MedicationIntake> = emptyList(),
    val planItems: List<PlanItem> = emptyList(),
    val emotionEntries: List<EmotionEntry> = emptyList(),
    val focusSessions: List<FocusSession> = emptyList(),
)

/// Els interruptors d'Ajustos d'aquell moment (`ViraNotificationPreferences`) i el que decideix
/// els avisos que ningú no ha demanat (absència, «Avui no», descans de 12–15).
data class ViraNotificationSettings(
    /// «Avisos». Apagat, només queda la medicació.
    val general: Boolean = true,
    val habits: Boolean = true,
    val medication: Boolean = true,
    val plan: Boolean = true,
    val emotion: Boolean = true,
    val detail: Boolean = true,
    val medicationDetail: Boolean = false,
    val emotionTime: ViraClock = ViraClock(
        ViraNotificationPreferences.EMOTION_DEFAULT_HOUR, ViraNotificationPreferences.EMOTION_DEFAULT_MINUTE,
    ),
    /// «El teu dia», a les 8:00.
    val morning: Boolean = true,
    val morningTime: ViraClock = ViraClock(
        ViraNotificationPreferences.MORNING_DEFAULT_HOUR, ViraNotificationPreferences.MORNING_DEFAULT_MINUTE,
    ),
    /// L'últim cop que es va obrir l'app (`ViraAbsence`). Sense valor, ara.
    val since: Instant? = null,
    /// Fins quan calla Viri (`ViraViriQuiet`).
    val quietUntil: Instant? = null,
    /// El tram de 12 a 15 (`ViraRestHours`).
    val young: Boolean = false,
    /// Els missatges de Viri són encesos i la ratxa es veu: el vespre ja té el seu avís si l'absència
    /// i «Avui no» ho deixen (`ViraStreakNotifications.speaks`).
    val viriSpeaks: Boolean = false,
    /// L'idioma de la pantalla, per a l'hora que surt a l'avís del matí.
    val locale: Locale = Locale.getDefault(),
)

/// Un interval `[start, end)`, com el `Range<Date>` de Swift.
data class ClosedOpenRange(val start: Instant, val end: Instant) {
    fun contains(instant: Instant) = !instant.isBefore(start) && instant.isBefore(end)
}
