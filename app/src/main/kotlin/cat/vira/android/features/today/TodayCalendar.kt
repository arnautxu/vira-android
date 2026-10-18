package cat.vira.android.features.today

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import cat.vira.android.R
import java.time.Instant
import java.time.ZoneId

/// Un esdeveniment del calendari del sistema, reduït al que Vira n'ensenya (`ViraCalendarEvent`).
/// No es desa, no es sincronitza i no s'exporta: viu el que dura la pantalla.
data class TodayCalendarEvent(
    val id: String,
    val title: String,
    val start: Instant,
    /// El calendari d'origen («Institut», «Família»): diu d'on ve sense que calgui preguntar-ho.
    val calendarName: String?,
    /// Estat local de Vira: el calendari d'Android no té «fet».
    val isLocallyDone: Boolean = false,
)

/// Els esdeveniments del calendari del sistema dins d'Avui (`ViraCalendar` de l'iPhone).
///
/// Llegeix i prou: no copia res a la base de dades i no escriu mai al calendari. Buit —mai un
/// error— sense l'interruptor d'Ajustos o sense el permís: una funció que depèn d'un permís del
/// sistema no pot fer que la pantalla sembli espatllada. El permís el demana qui encén
/// l'interruptor (Ajustos, Entrada), mai aquesta pantalla.
internal object TodayCalendar {

    /// Mateix nom que a l'iPhone (`ViraPreferences.calendarEnabled`).
    const val ENABLED_KEY = "settings.calendarEnabled"
    private const val DONE_KEY = "calendar.locallyCompletedEvents"

    fun isAuthorized(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun events(context: Context, preferences: SharedPreferences, day: Instant, zone: ZoneId): List<TodayCalendarEvent> {
        if (!preferences.getBoolean(ENABLED_KEY, false) || !isAuthorized(context)) return emptyList()
        val start = TodayPlanRules.startOfDay(day, zone)
        val end = TodayPlanRules.addDays(start, 1, zone)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start.toEpochMilli())
            ContentUris.appendId(it, end.toEpochMilli() - 1)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.ALL_DAY,
        )
        val done = completedKeys(preferences)
        val untitled = context.getString(R.string.calendar_event_untitled)
        return try {
            context.contentResolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        // Els de tot el dia no tenen hora i l'agenda ordena per hora: es queden fora.
                        if (cursor.getInt(4) == 1) continue
                        val begin = Instant.ofEpochMilli(cursor.getLong(2))
                        // Un esdeveniment repetit té el mateix `EVENT_ID` cada dia; amb l'inici,
                        // cada aparició és única.
                        val id = "${cursor.getLong(0)}-${begin.toEpochMilli()}"
                        add(
                            TodayCalendarEvent(
                                id = id,
                                title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: untitled,
                                start = begin,
                                calendarName = cursor.getString(3),
                                isLocallyDone = completionKey(id, start) in done,
                            ),
                        )
                    }
                }
            }.orEmpty()
        } catch (error: SecurityException) {
            Log.w("cat.vira.calendar", "Sense permís de calendari", error)
            emptyList()
        }
    }

    /// Marca local de Vira per tancar el dia sense fingir que s'ha editat el calendari. No surt
    /// del telèfon.
    fun toggleLocalCompletion(preferences: SharedPreferences, event: TodayCalendarEvent, day: Instant, zone: ZoneId) {
        val keys = completedKeys(preferences).toMutableSet()
        val key = completionKey(event.id, TodayPlanRules.startOfDay(day, zone))
        if (!keys.remove(key)) keys += key
        preferences.edit().putStringSet(DONE_KEY, keys).apply()
    }

    private fun completedKeys(preferences: SharedPreferences): Set<String> =
        preferences.getStringSet(DONE_KEY, emptySet()).orEmpty()

    private fun completionKey(id: String, dayStart: Instant) = "${dayStart.epochSecond}:$id"
}
