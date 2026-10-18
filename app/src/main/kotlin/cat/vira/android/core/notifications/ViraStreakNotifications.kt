package cat.vira.android.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit
import cat.vira.android.core.notifications.StreakNotificationCatalog.Kind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Els avisos de Viri sobre la ratxa (`ViraStreakNotifications` de l'iPhone).
///
/// Va a part dels recordatoris perquè és una altra cosa: allò són avisos d'una pauta que l'usuari
/// ha configurat; això és **la veu de Viri**, i s'apaga per separat
/// (`ViraNotificationPreferences.VIRI_MESSAGES_ENABLED`).
///
/// ## Un avís al dia com a molt
/// Una sola franja diària a l'hora triada (20:30 de sortida) que porta **una** de quatre coses,
/// per aquest ordre: la fita, la ratxa salvada, la ratxa que ha tornat a zero, o l'avís que encara
/// hi ha dia. Mai dues el mateix dia, i **mai cap si el dia ja compta** i no hi ha res a explicar.
///
/// ## El que no diu mai
/// **Què et falta.** El text surt de `StreakNotificationCatalog` i parla del dia, no de les accions.
///
/// La ratxa en si (si es veu, si el dia ja compta, la reconciliació) és de `features/progress`:
/// aquí arriba ja calculada (`ViraStreakState`).
object ViraStreakNotifications {

    const val PREFIX = "streak-"

    /// Cinc, dins del repartiment de 64 (26 + 14 + 8 + 3 + 5 = 56).
    const val BUDGET = 5
    const val WINDOW_DAYS = 7

    // MARK: Esdeveniments pendents d'explicar

    /// Una cosa que ha passat i que la propera franja ha de dir.
    sealed interface Event {
        val kind: Kind
        data object Protected : Event { override val kind = Kind.Protected }
        data object Reset : Event { override val kind = Kind.Reset }
        data class Milestone(val days: Int) : Event { override val kind = Kind.Milestone(days) }
    }

    // Sense el prefix `settings.` a posta, com a l'iPhone: és «què li queda per dir a Viri en
    // aquest telèfon», no una dada ni una preferència de l'usuari.
    private const val EVENT_KEY = "streak.pendingEvent"
    private const val EVENT_DAYS_KEY = "streak.pendingEventDays"
    private const val EVENT_DATE_KEY = "streak.pendingEventAt"

    /// Deixa constància que la propera franja ha de dir això. L'últim guanya: si el mateix dia es
    /// salva la ratxa i després arriba una fita, es diu la fita — és la notícia.
    fun record(event: Event, preferences: SharedPreferences, now: Instant) {
        preferences.edit {
            putString(
                EVENT_KEY,
                when (event) {
                    Event.Protected -> "protected"
                    Event.Reset -> "reset"
                    is Event.Milestone -> "milestone"
                },
            )
            putLong(EVENT_DATE_KEY, now.toEpochMilli())
            if (event is Event.Milestone) putInt(EVENT_DAYS_KEY, event.days)
        }
    }

    /// L'esdeveniment pendent, si la franja que l'havia de portar encara no ha passat. Un de
    /// caducat s'esborra: no ha d'aparèixer una setmana més tard fora de context.
    fun pendingEvent(preferences: SharedPreferences, time: ViraClock, now: Instant, zone: ZoneId): Event? {
        val raw = preferences.getString(EVENT_KEY, null) ?: return null
        val recorded = Instant.ofEpochMilli(preferences.getLong(EVENT_DATE_KEY, 0))
        val slot = slots(recorded, time, zone).firstOrNull()
        if (slot == null || !slot.isAfter(now)) {
            clearPendingEvent(preferences)
            return null
        }
        return when (raw) {
            "protected" -> Event.Protected
            "reset" -> Event.Reset
            "milestone" -> Event.Milestone(preferences.getInt(EVENT_DAYS_KEY, 0))
            else -> null
        }
    }

    fun clearPendingEvent(preferences: SharedPreferences) = preferences.edit {
        remove(EVENT_KEY)
        remove(EVENT_DAYS_KEY)
        remove(EVENT_DATE_KEY)
    }

    // MARK: Càlcul de la finestra

    /// Les franges que queden per davant: una per dia, la d'avui només si encara no ha passat.
    fun slots(now: Instant, time: ViraClock, zone: ZoneId): List<Instant> {
        val today = LocalDate.ofInstant(now, zone)
        return (0 until WINDOW_DAYS).mapNotNull { offset ->
            val fire = today.plusDays(offset.toLong()).atTime(time.hour, time.minute).atZone(zone).toInstant()
            fire.takeIf { it.isAfter(now) }
        }
    }

    fun requests(
        now: Instant,
        event: Event?,
        dayIsOpen: Boolean,
        muted: ClosedOpenRange?,
        time: ViraClock,
        zone: ZoneId,
        text: ViraNotificationText,
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        val planned = mutableListOf<ViraPlannedNotification>()
        for ((index, fire) in slots(now, time, zone).withIndex()) {
            // Un mode de concentració que silencia Vira: dins de la seva finestra, res.
            if (muted?.contains(fire) == true) continue
            val day = LocalDate.ofInstant(fire, zone)
            val kind = when {
                // La notícia va a la primera franja, tant si el dia ja compta com si no.
                index == 0 && event != null -> event.kind
                // El dia d'avui ja compta i no hi ha res a explicar: no es diu res.
                day == today && !dayIsOpen -> continue
                else -> Kind.Nudge
            }
            planned += ViraPlannedNotification(
                id = "$PREFIX${day.year}-${day.monthValue}-${day.dayOfMonth}",
                fireAtMillis = fire.toEpochMilli(),
                title = StreakNotificationCatalog.title(text),
                body = StreakNotificationCatalog.body(kind, day, text),
                channel = ViraNotificationChannel.VIRI,
                // És la franja del vespre: on estalviar-se obrir l'app val més.
                actions = ViraActionSet.VIRI,
            )
        }
        return planned.take(BUDGET)
    }
}

/// El que els avisos de Viri necessiten saber de la ratxa (`features/progress`).
data class ViraStreakState(
    /// Amagar la ratxa (§2.3.1.6) se n'emporta també els avisos.
    val isVisible: Boolean,
    /// Al dia d'avui encara li falta alguna cosa per comptar.
    val dayIsOpen: Boolean,
)

/// El resultat de posar la ratxa al dia en segon pla (`ViraStreak.reconcile`).
enum class ViraStreakReconcile { UNCHANGED, PROTECTED, RESET }
