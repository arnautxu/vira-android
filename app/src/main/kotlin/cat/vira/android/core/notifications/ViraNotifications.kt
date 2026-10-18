package cat.vira.android.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.InvalidationTracker
import cat.vira.android.R
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// La porta d'entrada dels avisos: el que criden les pantalles, l'arrencada i el segon pla.
///
/// `reschedule` és idempotent, com a l'iPhone: treu els avisos propis i torna a posar els que
/// toquen. S'ha de cridar cada cop que canvia què cal recordar (marcar, editar un hàbit o una
/// pauta, canviar un interruptor), en tornar a primer pla i des del treball de segon pla.
object ViraNotifications {

    /// L'extra que l'arrencada de l'app llegeix per saber quina pantalla obrir després d'un toc.
    const val EXTRA_DESTINATION = "cat.vira.notification.destination"
    const val DESTINATION_TODAY = "today"
    const val DESTINATION_CHECK_IN = "checkIn"

    /// El permís d'Android 13+. **Es demana en context**, quan l'usuari encén un avís, mai a
    /// l'arrencada. Un cop denegat dues vegades, Android ja no torna a preguntar: la pantalla ho
    /// ha d'explicar en comptes de callar.
    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    private val reminderPrefixes = listOf(
        ViraNotificationPlanner.HABIT_PREFIX,
        ViraNotificationPlanner.MEDICATION_PREFIX,
        ViraNotificationPlanner.PLAN_PREFIX,
        ViraNotificationPlanner.EMOTION_PREFIX,
    )
    private val streakPrefixes = listOf(ViraStreakNotifications.PREFIX)

    /// Si ara mateix es poden publicar avisos (permís concedit i no apagats als Ajustos del
    /// sistema). No demana res.
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /// Refà els recordatoris i els avisos de Viri.
    ///
    /// Les passades van d'una en una: el treball de segon pla i un botó de l'avís poden arribar
    /// alhora, i una passada vella que acabés l'última tornaria a posar un avís que la nova ja
    /// havia tret (una cosa feta mentrestant).
    suspend fun reschedule(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val host = context.notificationHost ?: return
        passes.withLock {
            rescheduleReminders(context, host.notificationDatabase, now, zone)
            rescheduleStreak(context, host, now, zone)
        }
    }

    private val passes = Mutex()

    /// L'alarma ha sonat. Va per la mateixa cua que les passades: entre treure-la de la llista i
    /// esborrar la notícia que porta, una passada la donaria per no programada i la tornaria a
    /// posar a l'endemà.
    suspend fun deliver(context: Context, notification: ViraPlannedNotification, now: Instant = Instant.now()) =
        passes.withLock {
            ViraNotificationScheduler(context).forget(notification.id)
            // Mai recorda una cosa que ja no està per fer, encara que la passada no hagi arribat
            // a treure l'alarma (o que la fila sigui d'un compte que ja no hi és).
            if (!isStillPending(context, notification, now = now)) return@withLock
            // Una notícia de Viri ja dita no es torna a programar si després es canvia l'hora de Viri.
            // Només si encara és la seva: una de més nova apuntada mentrestant no s'esborra.
            if (ViraNotificationAlarmReceiver.post(context, notification) &&
                notification.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX)
            ) {
                ViraStreakNotifications.clearPendingEvent(ViraPreferences.open(context), onlyIfStamp = notification.scheduledAtMillis)
            }
        }

    /// Si l'avís encara el vol l'usuari en el moment de sonar. Un «Més tard» o una alarma
    /// endarrerida poden sonar després d'apagar-ne la categoria: aleshores no surt.
    fun isWanted(context: Context, notification: ViraPlannedNotification): Boolean {
        val p = ViraPreferences.open(context)
        if (notification.channel == ViraNotificationChannel.VIRI) return ViraNotificationPreferences.viriMessagesEnabled(p)
        if (!ViraNotificationPreferences.remindersEnabled(p)) return false
        return when (notification.channel) {
            ViraNotificationChannel.MEDICATION -> ViraNotificationPreferences.medicationEnabled(p)
            ViraNotificationChannel.HABITS -> ViraNotificationPreferences.habitsEnabled(p)
            ViraNotificationChannel.PLAN -> ViraNotificationPreferences.planEnabled(p)
            ViraNotificationChannel.EMOTION -> ViraNotificationPreferences.emotionEnabled(p)
            ViraNotificationChannel.VIRI -> true
        }
    }

    /// Si el sistema deixa posar alarmes exactes. A Android 14+ el permís neix **retirat** i
    /// l'usuari l'ha de donar als Ajustos del sistema (`exactAlarmSettings`); sense ell, els avisos
    /// arriben igual però poden endarrerir-se uns minuts.
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    /// La pantalla del sistema on es donen les alarmes exactes a Vira (Android 12+). La fa servir
    /// Ajustos → Notificacions quan `canScheduleExact` és fals.
    fun exactAlarmSettings(context: Context): Intent? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null
        else Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))

    suspend fun rescheduleReminders(context: Context, db: ViraDatabase, now: Instant, zone: ZoneId) {
        val scheduler = ViraNotificationScheduler(context)
        val preferences = ViraPreferences.open(context)
        if (!canPost(context) || !ViraNotificationPreferences.remindersEnabled(preferences)) {
            scheduler.cancelAll(reminderPrefixes)
            refreshShown(context, reminderPrefixes) { true }
            return
        }
        val settings = ViraNotificationSettings(
            habits = ViraNotificationPreferences.habitsEnabled(preferences),
            medication = ViraNotificationPreferences.medicationEnabled(preferences),
            plan = ViraNotificationPreferences.planEnabled(preferences),
            emotion = ViraNotificationPreferences.emotionEnabled(preferences),
            detail = ViraNotificationPreferences.detailEnabled(preferences),
            medicationDetail = ViraNotificationPreferences.medicationDetailEnabled(preferences),
            emotionTime = ViraNotificationPreferences.emotionTime(preferences),
        )
        val inputs = snapshot(db)
        val planned = ViraNotificationPlanner.plan(inputs, settings, now, zone, text(context))
        // Un «Més tard» o una alarma endarrerida només sobreviuen si el que recorden encara està per
        // fer i no cauen dins d'una concentració que silencia Vira.
        val muted = ViraNotificationPlanner.muteWindow(inputs.focusSessions, now)
        scheduler.replace(reminderPrefixes, planned, now) {
            // Un «Més tard» no se l'emporta una concentració que encara no l'ha atrapat: si s'acaba
            // abans, ha de sonar. Si encara dura quan arribi, l'alarma ho torna a mirar i calla.
            ViraNotificationPlanner.isStillPending(it, inputs, zone, now) &&
                (it.isSnooze || muted?.contains(it.fireAt) != true)
        }
        // Els que ja són a la pantalla d'una cosa que s'ha marcat a l'app, o d'una categoria apagada,
        // fora: tenen botons que actuarien sobre una cosa resolta.
        refreshShown(context, reminderPrefixes) {
            !isWanted(context, it) || !ViraNotificationPlanner.isStillPending(it, inputs, zone, now)
        }
    }

    private suspend fun rescheduleStreak(context: Context, host: ViraNotificationHost, now: Instant, zone: ZoneId) {
        val scheduler = ViraNotificationScheduler(context)
        val preferences = ViraPreferences.open(context)
        val state = host.notificationEffects.streakState(now)
        // Amagar la ratxa se n'emporta també els avisos: si no es veu a la pantalla, no pot
        // sortir a la pantalla bloquejada.
        if (!canPost(context) || !ViraNotificationPreferences.viriMessagesEnabled(preferences) ||
            state == null || !state.isVisible
        ) {
            scheduler.cancelAll(streakPrefixes)
            refreshShown(context, streakPrefixes) { true }
            return
        }
        val time = ViraNotificationPreferences.viriTime(preferences)
        val muted = ViraNotificationPlanner.muteWindow(host.notificationDatabase.focusSessions().all(), now)
        val planned = ViraStreakNotifications.requests(
            now = now,
            event = ViraStreakNotifications.pendingEvent(preferences, time, now, zone, muted) { stamp ->
                scheduler.armed().any { it.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX) && it.scheduledAtMillis == stamp }
            },
            dayIsOpen = state.dayIsOpen,
            muted = muted,
            time = time,
            zone = zone,
            text = text(context),
            eventStamp = ViraStreakNotifications.pendingEventStamp(preferences),
        )
        // Un avís de Viri endarrerit no surt si el dia ja compta: diria que encara és obert.
        scheduler.replace(streakPrefixes, planned, now) {
            val news = it.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX)
            // Una notícia endarrerida només si encara és la d'ara: una de més nova la substitueix.
            (if (news) isCurrentNews(preferences, it) else state.dayIsOpen) && muted?.contains(it.fireAt) != true
        }
        // El mateix per al que ja és a la pantalla: un «encara hi ets a temps» d'un dia que ja compta
        // (o d'un dia passat) mentiria. Les notícies es queden: són certes encara que el dia es tanqui.
        val today = LocalDate.ofInstant(now, zone)
        refreshShown(context, streakPrefixes) {
            !it.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX) &&
                (!state.dayIsOpen || LocalDate.ofInstant(it.fireAt, zone) != today)
        }
    }

    /// Posa la ratxa al dia des del segon pla i deixa apuntat el que hagi passat. És l'únic camí
    /// que fa que «t'he guardat la ratxa» arribi **sense obrir l'app**; amb l'app oberta no
    /// s'apunta res, perquè la targeta d'Inici ja ho diu.
    suspend fun reconcileInBackground(context: Context, now: Instant = Instant.now()) {
        val host = context.notificationHost ?: return
        val preferences = ViraPreferences.open(context)
        when (host.notificationEffects.reconcileStreak(now)) {
            ViraStreakReconcile.UNCHANGED -> Unit
            ViraStreakReconcile.PROTECTED -> ViraStreakNotifications.record(ViraStreakNotifications.Event.Protected, preferences, now)
            ViraStreakReconcile.RESET -> ViraStreakNotifications.record(ViraStreakNotifications.Event.Reset, preferences, now)
        }
    }

    /// Una fita, una ratxa salvada o una que torna a zero: la propera franja de Viri ho dirà.
    /// La crida `features/progress`; després cal un `reschedule`.
    fun record(context: Context, event: ViraStreakNotifications.Event, now: Instant = Instant.now()) =
        ViraStreakNotifications.record(event, ViraPreferences.open(context), now)

    /// Esborrat total i canvi de compte: cap avís programat del compte anterior. Va per la mateixa
    /// cua que les passades: una que ja hagués llegit les dades d'abans no pot tornar a posar-les
    /// després d'esborrar.
    suspend fun cancelEverything(context: Context) = passes.withLock {
        val prefixes = reminderPrefixes + streakPrefixes
        ViraNotificationScheduler(context).cancelAll(prefixes)
        ViraStreakNotifications.clearPendingEvent(ViraPreferences.open(context))
        // Les categories, les hores i el detall tornen a fàbrica: un compte nou o un esborrat total
        // no hereten els avisos de l'anterior.
        ViraNotificationPreferences.reset(ViraPreferences.open(context))
        // També els que ja són a la pantalla: un avís de medicació del compte anterior no hi pot
        // quedar.
        refreshShown(context, prefixes) { true }
    }

    /// Si un avís de recordatori encara té sentit **ara**, contra la base de dades d'ara: el que
    /// comprova l'alarma just abans de publicar. Un avís d'un compte que ja no hi és (canvi de
    /// compte, esborrat total) o d'una cosa marcada des d'un altre telèfon no surt. Els de Viri
    /// (`streak-`) no hi passen: no recorden cap fila.
    suspend fun isStillPending(
        context: Context,
        notification: ViraPlannedNotification,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): Boolean {
        // Endarrerida més enllà del marge (Doze, estalvi de bateria): ja no recorda res a temps.
        if (now.isAfter(notification.fireAt.plus(ViraNotificationScheduler.OVERDUE_GRACE))) return false
        val host = context.notificationHost ?: return false
        val inputs = snapshot(host.notificationDatabase)
        // Una concentració que silencia Vira, començada després de l'última passada: tampoc.
        if (ViraNotificationPlanner.muteWindow(inputs.focusSessions, now)?.contains(now) == true) return false
        if (notification.id.startsWith(ViraStreakNotifications.PREFIX)) {
            // Amagar la ratxa se n'emporta els avisos; un recordatori d'un dia que ja compta, fora.
            val state = host.notificationEffects.streakState(now)
            if (state == null || !state.isVisible) return false
            return if (notification.id.endsWith(ViraStreakNotifications.NEWS_SUFFIX)) {
                isCurrentNews(ViraPreferences.open(context), notification)
            } else {
                // `dayIsOpen` parla d'avui: un recordatori d'ahir endarrerit passada la mitjanit, no.
                // I si mentrestant s'ha canviat l'hora de Viri, el de l'hora d'abans tampoc.
                val fire = notification.fireAt.atZone(zone)
                val time = ViraNotificationPreferences.viriTime(ViraPreferences.open(context))
                state.dayIsOpen && fire.toLocalDate() == LocalDate.ofInstant(now, zone) &&
                    fire.hour == time.hour && fire.minute == time.minute
            }
        }
        if (reminderPrefixes.none { notification.id.startsWith(it) }) return true
        return ViraNotificationPlanner.isStillPending(notification, inputs, zone, now)
    }

    private fun isCurrentNews(preferences: android.content.SharedPreferences, notification: ViraPlannedNotification) =
        notification.scheduledAtMillis != null &&
            notification.scheduledAtMillis == ViraStreakNotifications.pendingEventStamp(preferences)

    /// Les taules que decideixen què cal recordar. Qualsevol canvi —una pantalla, un botó d'avís,
    /// la sincronització que porta el que s'ha fet en un altre telèfon— refà la finestra, perquè
    /// cap avís recordi una cosa ja feta. `request` ajunta les ràfegues en una sola passada.
    fun watch(context: Context, db: ViraDatabase) {
        val appContext = context.applicationContext
        db.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(WATCHED_TABLES) {
                override fun onInvalidated(tables: Set<String>) = ViraNotificationWorker.request(appContext)
            },
        )
    }

    private val WATCHED_TABLES = arrayOf(
        "Habit", "HabitLog", "Medication", "MedicationIntake", "PlanItem", "EmotionEntry", "FocusSession",
        "Streak", "ProgressCounter",
    )

    /// Posa al dia els avisos propis que ja són a la pantalla: treu els que compleixen `doomed` i
    /// torna a publicar, en silenci, els que diuen més del que el detall d'ara permet (el nom del
    /// fàrmac). Només els propis (l'etiqueta és l'identificador); l'avís fix de Concentra't no.
    private fun refreshShown(context: Context, prefixes: List<String>, doomed: (ViraPlannedNotification) -> Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        for (shown in manager.activeNotifications) {
            val tag = shown.tag ?: continue
            if (prefixes.none { tag.startsWith(it) }) continue
            val payload = shown.notification.extras.getString(ViraNotificationAlarmReceiver.EXTRA_PAYLOAD)
            val notification = payload?.let {
                runCatching { Json.decodeFromString(ViraPlannedNotification.serializer(), it) }.getOrNull()
            }
            // Sense poder llegir què recorda, no es pot dir que encara toqui: fora.
            if (notification == null || doomed(notification)) {
                manager.cancel(tag, shown.id)
                continue
            }
            val current = presented(context, notification)
            if (current != notification) manager.notify(tag, shown.id, ViraNotificationAlarmReceiver.build(context, current))
        }
    }

    /// El text tal com ha de sortir **ara**. Un «Més tard» o una alarma endarrerida porten el text
    /// del moment en què es van programar; si mentrestant s'ha apagat el detall, el nom (del
    /// fàrmac sobretot) no pot sortir igualment.
    fun presented(context: Context, notification: ViraPlannedNotification): ViraPlannedNotification {
        val p = ViraPreferences.open(context)
        val detail = when (notification.channel) {
            ViraNotificationChannel.MEDICATION -> ViraNotificationPreferences.medicationDetailEnabled(p)
            ViraNotificationChannel.HABITS, ViraNotificationChannel.PLAN -> ViraNotificationPreferences.detailEnabled(p)
            ViraNotificationChannel.EMOTION, ViraNotificationChannel.VIRI -> true
        }
        return if (detail) notification
        else notification.copy(body = context.getString(R.string.notification_generic_body))
    }

    private suspend fun snapshot(db: ViraDatabase) = ViraNotificationInputs(
        habits = db.habits().all(),
        habitLogs = db.habitLogs().all(),
        medications = db.medications().all(),
        intakes = db.medicationIntakes().all(),
        planItems = db.planItems().all(),
        emotionEntries = db.emotionEntries().all(),
        focusSessions = db.focusSessions().all(),
    )

    private fun text(context: Context) = ViraNotificationText { id, args -> context.getString(id, *args) }
}
