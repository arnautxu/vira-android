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
    /// Marca la franja que porta una notícia (fita, ratxa salvada o a zero): una notícia endarrerida
    /// encara s'ha de dir encara que el dia ja compti; un recordatori, no.
    const val NEWS_SUFFIX = "-news"

    /// Cinc, dins del repartiment de 64 (26 + 14 + 8 + 3 + 5 = 56).
    const val BUDGET = 5
    const val WINDOW_DAYS = 7

    /// El mateix marge que les alarmes endarrerides.
    private val NEWS_GRACE: java.time.Duration get() = ViraNotificationScheduler.OVERDUE_GRACE

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
    private const val EVENT_MUTED_KEY = "streak.pendingEventMutedUntil"

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
            remove(EVENT_MUTED_KEY)
            if (event is Event.Milestone) putInt(EVENT_DAYS_KEY, event.days)
        }
    }

    /// L'esdeveniment pendent, si la franja que l'havia de portar encara no ha passat. Un de
    /// caducat s'esborra: no ha d'aparèixer una setmana més tard fora de context.
    ///
    /// La franja que el porta és la primera **que no silencia una concentració**: si la de l'endemà
    /// cau a mig mode silenciós, la notícia passa a la següent. Es desa fins on arriba el silenci,
    /// perquè quan la concentració s'acabi ja no se sap que aquella franja era muda i la notícia es
    /// donaria per caducada sense haver sonat. La data de l'esdeveniment no es toca: si la
    /// concentració s'acaba abans de la franja, la franja torna a ser bona.
    fun pendingEvent(
        preferences: SharedPreferences,
        time: ViraClock,
        now: Instant,
        zone: ZoneId,
        muted: ClosedOpenRange? = null,
        isArmed: (stamp: Long) -> Boolean = { true },
    ): Event? {
        val raw = preferences.getString(EVENT_KEY, null) ?: return null
        val recorded = Instant.ofEpochMilli(preferences.getLong(EVENT_DATE_KEY, 0))
        var silencedUntil = preferences.getLong(EVENT_MUTED_KEY, Long.MIN_VALUE)
        val slot = slots(recorded, time, zone).firstOrNull { candidate ->
            when {
                muted?.contains(candidate) == true -> {
                    silencedUntil = maxOf(silencedUntil, candidate.toEpochMilli())
                    false
                }
                // Ja ha passat i en planificar-la era muda: no ha sonat.
                !candidate.isAfter(now) && candidate.toEpochMilli() <= silencedUntil -> false
                else -> true
            }
        }
        if (silencedUntil != Long.MIN_VALUE) preferences.edit { putLong(EVENT_MUTED_KEY, silencedUntil) }
        if (slot == null) {
            clearPendingEvent(preferences)
            return null
        }
        if (!slot.isAfter(now)) {
            if (!now.isBefore(slot.plus(NEWS_GRACE))) {
                clearPendingEvent(preferences)
                return null
            }
            // Acabada de passar, l'alarma endarrerida d'aquesta notícia encara pot sonar: es
            // conserva (sense tornar-la a planificar) fins que sona o passa el marge.
            if (isArmed(recorded.toEpochMilli())) return null
            // Cap alarma la portava (s'ha apuntat just abans de la franja i la passada ha arribat
            // tard): passa a la franja següent. La data nova és també la marca nova de la notícia.
            preferences.edit { putLong(EVENT_DATE_KEY, slot.toEpochMilli()) }
            return pendingEvent(preferences, time, now, zone, muted, isArmed)
        }
        return when (raw) {
            "protected" -> Event.Protected
            "reset" -> Event.Reset
            "milestone" -> Event.Milestone(preferences.getInt(EVENT_DAYS_KEY, 0))
            else -> null
        }
    }

    /// Quina notícia és: la data en què es va apuntar. Viatja amb l'avís perquè una notícia
    /// endarrerida que una de més nova ha substituït no surti ni esborri la nova.
    fun pendingEventStamp(preferences: SharedPreferences): Long? =
        preferences.getLong(EVENT_DATE_KEY, Long.MIN_VALUE).takeIf { preferences.contains(EVENT_KEY) && it != Long.MIN_VALUE }

    fun clearPendingEvent(preferences: SharedPreferences) = preferences.edit {
        remove(EVENT_KEY)
        remove(EVENT_DAYS_KEY)
        remove(EVENT_DATE_KEY)
        remove(EVENT_MUTED_KEY)
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
        eventStamp: Long? = null,
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        val planned = mutableListOf<ViraPlannedNotification>()
        var news = event
        for (fire in slots(now, time, zone)) {
            // Un mode de concentració que silencia Vira: dins de la seva finestra, res.
            if (muted?.contains(fire) == true) continue
            val day = LocalDate.ofInstant(fire, zone)
            val kind = when {
                // La notícia va a la primera franja que sona, tant si el dia ja compta com si no.
                news != null -> news.kind.also { news = null }
                // El dia d'avui ja compta i no hi ha res a explicar: no es diu res.
                day == today && !dayIsOpen -> continue
                else -> Kind.Nudge
            }
            planned += ViraPlannedNotification(
                id = "$PREFIX${day.year}-${day.monthValue}-${day.dayOfMonth}" + if (kind is Kind.Nudge) "" else NEWS_SUFFIX,
                fireAtMillis = fire.toEpochMilli(),
                title = StreakNotificationCatalog.title(text),
                body = StreakNotificationCatalog.body(kind, day, text),
                channel = ViraNotificationChannel.VIRI,
                // És la franja del vespre: on estalviar-se obrir l'app val més.
                actions = ViraActionSet.VIRI,
                scheduledAtMillis = if (kind is Kind.Nudge) null else eventStamp,
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
