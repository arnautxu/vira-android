package cat.vira.android.core.notifications

import android.content.SharedPreferences
import cat.vira.android.core.data.ViraPreferences

/// Els interruptors i les hores dels avisos, amb **els mateixos noms que l'iPhone**
/// (`ViraPreferences.swift`) i els mateixos valors de fàbrica que hi registra `ViraApp.init`.
///
/// A l'iPhone, `register(defaults:)` fa que «mai triat» valgui `true`; aquí ho fa el segon
/// paràmetre de `getBoolean`, i per això els valors per defecte viuen al costat de les claus: una
/// pantalla que llegís la clau amb un altre per defecte veuria un interruptor que no és el real.
///
/// Totes porten el prefix `settings.`: un canvi de compte les torna a fàbrica
/// (`ViraDataEraser.resetPreferences`).
object ViraNotificationPreferences {

    /// Interruptor mestre dels recordatoris funcionals. Apagat, no en surt cap de cap categoria.
    const val REMINDERS_ENABLED = ViraPreferences.REMINDERS_ENABLED

    /// Les quatre categories de `USER_FLOWS.md` F24. Totes neixen **enceses**.
    const val REMINDERS_HABITS_ENABLED = "settings.remindersHabitsEnabled"
    const val REMINDERS_MEDICATION_ENABLED = "settings.remindersMedicationEnabled"
    const val REMINDERS_PLAN_ENABLED = "settings.remindersPlanEnabled"
    const val REMINDERS_EMOTION_ENABLED = "settings.remindersEmotionEnabled"

    /// Hora del recordatori de registre emocional. Sense valor desat, les 19:00.
    const val EMOTION_REMINDER_HOUR = "settings.emotionReminderHour"
    const val EMOTION_REMINDER_MINUTE = "settings.emotionReminderMinute"
    const val EMOTION_DEFAULT_HOUR = 19
    const val EMOTION_DEFAULT_MINUTE = 0

    /// El nom de l'hàbit o el títol de la tasca dins de l'avís. **Neix encès** (Arnau,
    /// 2026-08-13): un recordatori que diu «Tens un recordatori» no recorda res.
    const val NOTIFICATION_DETAIL_ENABLED = ViraPreferences.NOTIFICATION_DETAIL_ENABLED

    /// El **nom del fàrmac**, amb interruptor propi i **apagat** de fàbrica (Arnau, 2026-08-13).
    /// És una de les dues dades protegides (`PRIVACY_AND_SAFETY.md` §4.6) i la pantalla de bloqueig
    /// la veu qualsevol que passi pel costat: no pot penjar del mateix commutador que l'hàbit.
    const val NOTIFICATION_DETAIL_MEDICATION_ENABLED = "settings.notificationDetailMedicationEnabled"

    /// La veu de Viri (`VIRI_AND_GAMIFICATION.md` §3.5). Va **a part** de `REMINDERS_ENABLED`:
    /// és gamificació i s'ha de poder apagar sense perdre cap recordatori. Neix encesa.
    const val VIRI_MESSAGES_ENABLED = "settings.viriMessagesEnabled"
    const val VIRI_MESSAGE_HOUR = "settings.viriMessageHour"
    const val VIRI_MESSAGE_MINUTE = "settings.viriMessageMinute"
    const val VIRI_DEFAULT_HOUR = 20
    const val VIRI_DEFAULT_MINUTE = 30

    fun remindersEnabled(p: SharedPreferences) = p.getBoolean(REMINDERS_ENABLED, true)
    fun habitsEnabled(p: SharedPreferences) = p.getBoolean(REMINDERS_HABITS_ENABLED, true)
    fun medicationEnabled(p: SharedPreferences) = p.getBoolean(REMINDERS_MEDICATION_ENABLED, true)
    fun planEnabled(p: SharedPreferences) = p.getBoolean(REMINDERS_PLAN_ENABLED, true)
    fun emotionEnabled(p: SharedPreferences) = p.getBoolean(REMINDERS_EMOTION_ENABLED, true)
    fun detailEnabled(p: SharedPreferences) = p.getBoolean(NOTIFICATION_DETAIL_ENABLED, true)
    fun medicationDetailEnabled(p: SharedPreferences) = p.getBoolean(NOTIFICATION_DETAIL_MEDICATION_ENABLED, false)
    fun viriMessagesEnabled(p: SharedPreferences) = p.getBoolean(VIRI_MESSAGES_ENABLED, true)

    fun emotionTime(p: SharedPreferences) = ViraClock(
        p.getInt(EMOTION_REMINDER_HOUR, EMOTION_DEFAULT_HOUR),
        p.getInt(EMOTION_REMINDER_MINUTE, EMOTION_DEFAULT_MINUTE),
    )

    fun viriTime(p: SharedPreferences) = ViraClock(
        p.getInt(VIRI_MESSAGE_HOUR, VIRI_DEFAULT_HOUR),
        p.getInt(VIRI_MESSAGE_MINUTE, VIRI_DEFAULT_MINUTE),
    )
}

/// Una hora del dia, sense dia.
data class ViraClock(val hour: Int, val minute: Int)
