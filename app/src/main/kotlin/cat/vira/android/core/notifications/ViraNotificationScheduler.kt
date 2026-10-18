package cat.vira.android.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import java.time.Instant

/// Posa i treu els avisos al sistema: la meitat de `UNUserNotificationCenter` que Android no té.
///
/// Cada avís és una alarma exacta que, en sonar, publica la notificació
/// (`ViraNotificationAlarmReceiver`). Dues coses que iOS fa sol i aquí són feina nostra:
/// - **Saber què hi ha programat.** `AlarmManager` no deixa llistar-les, i sense llista no es pot
///   fer el «esborra els meus i torna'ls a posar» de l'iPhone. Les programades es desen aquí.
/// - **Sobreviure a apagar el telèfon.** Android esborra totes les alarmes en reiniciar;
///   `ViraNotificationBootReceiver` les torna a posar des d'aquesta mateixa llista.
///
/// Si el sistema no deixa posar alarmes exactes (Android 12+, permís retirat als Ajustos), es
/// posen inexactes: millor un avís uns minuts tard que cap.
class ViraNotificationScheduler(private val context: Context) {

    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    /// Les que hi ha programades ara mateix.
    fun armed(): List<ViraPlannedNotification> = synchronized(LOCK) { read() }

    /// Treu les programades d'aquests prefixos i posa les noves. Dues excepcions, sempre que
    /// `stillPending` digui que el que recorden encara està per fer:
    /// - **Els «Més tard» no es toquen**: són una petició de l'usuari, no part de la finestra, i la
    ///   propera passada no els ha de desfer. (A l'iPhone la passada que segueix a l'acció sí que
    ///   l'esborra, perquè el seu filtre és només pel prefix: és un error de l'iPhone, i aquí no es
    ///   reprodueix.)
    /// - **Les que ja haurien d'haver sonat** i encara no ho han fet: sense permís d'alarmes
    ///   exactes, Android les pot endarrerir uns minuts, i el planificador ja no les veu perquè
    ///   la seva hora és passada. Treure-les convertiria un avís tardà en cap avís.
    fun replace(
        prefixes: List<String>,
        planned: List<ViraPlannedNotification>,
        now: Instant = Instant.now(),
        stillPending: (ViraPlannedNotification) -> Boolean = { true },
    ) = synchronized(LOCK) {
        val graceStart = now.minus(OVERDUE_GRACE)
        val (stale, kept) = read().partition { n ->
            val exempt = n.isSnooze || (n.fireAt <= now && n.fireAt > graceStart)
            prefixes.any { n.id.startsWith(it) } && !(exempt && stillPending(n))
        }
        stale.forEach { cancelAlarm(it.id) }
        val fresh = planned.distinctBy { it.id }
        fresh.forEach(::arm)
        write(kept.filter { k -> fresh.none { it.id == k.id } } + fresh)
    }

    /// Treu-ho **tot**, «Més tard» inclosos (permís retirat, interruptor apagat, esborrat total,
    /// canvi de compte): cap avís pendent pot sonar amb el contingut d'abans.
    fun cancelAll(prefixes: List<String>) = synchronized(LOCK) {
        val (gone, kept) = read().partition { n -> prefixes.any { n.id.startsWith(it) } }
        gone.forEach { cancelAlarm(it.id) }
        write(kept)
    }

    /// Un avís solt (el «Més tard»). Substitueix el que tingui el mateix identificador.
    fun add(notification: ViraPlannedNotification) = synchronized(LOCK) {
        arm(notification)
        write(read().filter { it.id != notification.id } + notification)
    }

    /// Ja ha sonat: fora de la llista.
    fun forget(id: String) = synchronized(LOCK) {
        write(read().filter { it.id != id })
    }

    /// Després d'arrencar el telèfon: torna a posar les que encara han de sonar i oblida la resta.
    fun rearm(now: Instant) = synchronized(LOCK) {
        val future = read().filter { it.fireAt.isAfter(now) }
        future.forEach(::arm)
        write(future)
    }

    private fun arm(notification: ViraPlannedNotification) {
        val manager = alarms ?: return
        val pending = alarmIntent(notification.id, Json.encodeToString(ViraPlannedNotification.serializer(), notification))
        val at = notification.fireAtMillis
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun cancelAlarm(id: String) {
        alarms?.cancel(alarmIntent(id, null))
    }

    /// L'identificador va a l'URI perquè Android distingeixi un `PendingIntent` de l'altre: amb
    /// els extres no n'hi ha prou, i dues alarmes es trepitjarien.
    private fun alarmIntent(id: String, payload: String?): PendingIntent {
        val intent = Intent(context, ViraNotificationAlarmReceiver::class.java)
            .setData(Uri.parse("vira-notification://alarm/${Uri.encode(id)}"))
        if (payload != null) intent.putExtra(ViraNotificationAlarmReceiver.EXTRA_PAYLOAD, payload)
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun read(): List<ViraPlannedNotification> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(LIST, raw) }.getOrDefault(emptyList())
    }

    private fun write(list: List<ViraPlannedNotification>) {
        preferences.edit { putString(KEY, json.encodeToString(LIST, list)) }
    }

    companion object {
        /// Fitxer propi i fora de `vira`: no és cap preferència, i un canvi de compte el refà igual.
        const val FILE = "vira.notifications"
        private const val KEY = "armed"
        const val SNOOZE_SUFFIX = "-snooze"
        /// Quant d'endarreriment es tolera a una alarma inexacta abans de donar-la per perduda.
        private val OVERDUE_GRACE: java.time.Duration = java.time.Duration.ofMinutes(30)
        private val LOCK = Any()
        private val json = Json { ignoreUnknownKeys = true }
        private val LIST = kotlinx.serialization.builtins.ListSerializer(ViraPlannedNotification.serializer())
    }
}

val ViraPlannedNotification.isSnooze get() = id.endsWith(ViraNotificationScheduler.SNOOZE_SUFFIX)
