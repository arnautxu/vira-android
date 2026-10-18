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

    /// Treu les programades d'aquests prefixos i posa les noves. **Els «Més tard» no es toquen**:
    /// són una petició de l'usuari, no part de la finestra, i la propera passada no els ha de
    /// desfer. (A l'iPhone la passada que segueix a l'acció sí que l'esborra, perquè el seu filtre
    /// és només pel prefix: és un error de l'iPhone, i aquí no es reprodueix.)
    fun replace(prefixes: List<String>, planned: List<ViraPlannedNotification>) = synchronized(LOCK) {
        val (stale, kept) = read().partition { n -> !n.isSnooze && prefixes.any { n.id.startsWith(it) } }
        stale.forEach { cancelAlarm(it.id) }
        val fresh = planned.distinctBy { it.id }
        fresh.forEach(::arm)
        write(kept.filter { k -> fresh.none { it.id == k.id } } + fresh)
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

    /// Treu-ho tot (permís retirat, interruptor mestre apagat, esborrat total).
    fun cancelAll(prefixes: List<String>) = replace(prefixes, emptyList())

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
        private val LOCK = Any()
        private val json = Json { ignoreUnknownKeys = true }
        private val LIST = kotlinx.serialization.builtins.ListSerializer(ViraPlannedNotification.serializer())
    }
}

val ViraPlannedNotification.isSnooze get() = id.endsWith(ViraNotificationScheduler.SNOOZE_SUFFIX)
