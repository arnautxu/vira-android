package cat.vira.android.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.annotation.StringRes
import cat.vira.android.R
import kotlinx.serialization.Serializable
import java.time.Instant

/// Un avís decidit i pendent de sonar: el que a l'iPhone és un `UNNotificationRequest`.
///
/// Porta el text ja resolt, com el `UNMutableNotificationContent`: el que diu es decideix en
/// programar-lo, amb els interruptors de detall d'aquell moment, no en sonar. I porta **només
/// identificadors i dates** (`ViraNotificationActions.Key` de l'iPhone), mai el nom de l'hàbit
/// ni el del fàrmac fora del text: és el que el sistema guarda amb l'alarma.
///
/// Es desa en JSON (`ViraNotificationScheduler`) perquè Android, a diferència d'iOS, no recorda
/// les alarmes després d'apagar el telèfon ni deixa llistar-les: el registre és nostre.
@Serializable
data class ViraPlannedNotification(
    val id: String,
    val fireAtMillis: Long,
    val title: String,
    val body: String,
    val channel: ViraNotificationChannel,
    val actions: ViraActionSet,
    val habitId: String? = null,
    val dayMillis: Long? = null,
    val medicationId: String? = null,
    val scheduledAtMillis: Long? = null,
    val planItemId: String? = null,
) {
    val fireAt: Instant get() = Instant.ofEpochMilli(fireAtMillis)
}

/// Els botons que porta l'avís (`ViraNotificationActions.Category` de l'iPhone).
enum class ViraActionSet {
    /// Fet · Més tard.
    HABIT,
    /// Presa · Salta-la · Més tard.
    MEDICATION,
    /// Fet · Més tard.
    PLAN,
    /// L'avís del vespre de Viri **i** el del registre emocional: emocions de resposta ràpida.
    VIRI,
    /// «El teu dia», al matí: sense botons, un toc obre Inici.
    DAY,
}

/// Un canal per categoria, les cinc de `USER_FLOWS.md` F24 (CLAUDE.md d'Android).
///
/// El nom de cada canal és el mateix text que l'interruptor d'Ajustos, perquè als Ajustos del
/// sistema es reconegui la mateixa cosa. La medicació és l'única amb importància alta: és el
/// `.timeSensitive` de l'iPhone, l'únic avís de Vira que perd tot el sentit si arriba tard.
enum class ViraNotificationChannel(
    val id: String,
    @StringRes val label: Int,
    val importance: Int,
) {
    MEDICATION("cat.vira.medication", R.string.settings_notifications_category_medication, NotificationManager.IMPORTANCE_HIGH),
    HABITS("cat.vira.habits", R.string.settings_notifications_category_habits, NotificationManager.IMPORTANCE_DEFAULT),
    PLAN("cat.vira.plan", R.string.settings_notifications_category_plan, NotificationManager.IMPORTANCE_DEFAULT),
    EMOTION("cat.vira.emotion", R.string.settings_notifications_category_emotion, NotificationManager.IMPORTANCE_DEFAULT),
    VIRI("cat.vira.viri", R.string.settings_notifications_viri, NotificationManager.IMPORTANCE_DEFAULT);

    companion object {
        /// Crea (o reanomena, si ha canviat l'idioma) els cinc canals. Idempotent: Android no
        /// toca la importància ni el so d'un canal que ja existeix, que és el que vol l'usuari
        /// si els ha canviat als Ajustos del sistema.
        fun register(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            // El so de Vira, un de sol per a tots, que no diu de què és l'avís (`ViraSound.notificationSound`).
            // Pel nom i no pel número: el canal desa l'adreça per sempre, i el número pot canviar
            // d'una versió de l'app a la següent.
            val res = context.resources
            val sound = Uri.parse(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/" +
                    "${res.getResourceTypeName(R.raw.sound_avis_vira)}/${res.getResourceEntryName(R.raw.sound_avis_vira)}",
            )
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannels(
                entries.map { channel ->
                    NotificationChannel(channel.id, context.getString(channel.label), channel.importance).apply {
                        setSound(sound, attributes)
                    }
                },
            )
        }
    }
}
