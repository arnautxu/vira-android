package cat.vira.android.core.notifications

import cat.vira.android.R
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/// L'avís del matí: «El teu dia» (`ViraMorningNotification` de l'iPhone, Arnau, 2026-10-22).
///
/// - **Un al dia**, a les 8:00. Segueix l'interruptor d'«Avisos».
/// - **Cap avís d'una cosa ja feta**: si el dia té tasques i ja són totes fetes, no surt.
/// - Només parla de les **tasques** del pla. Mai d'hàbits, emocions ni medicació, ni en recompte.
/// - El títol de la primera tasca només hi va amb el detall encès, com la resta d'avisos (§4.6).
/// - Només els dies normals de `ViraAbsence`: a qui fa tres dies que no obre l'app, «què toca avui»
///   no l'ajuda a tornar, i els dies espaiats són del vespre.
/// - Pressupost: **3** places (tres dies per endavant). Els dies que vénen es programen amb el que
///   se sap ara; la passada següent els refà.
object ViraMorningNotification {

    /// Què diu l'avís d'un dia.
    sealed interface Message {
        /// El dia és buit: convida a apuntar.
        data object Empty : Message
        /// Tasques pendents. `first` és la primera amb hora (títol i hora), si se'n pot dir.
        data class Pending(val count: Int, val first: Pair<String, String>?) : Message
    }

    /// La regla. `null` si aquell dia no n'hi ha d'haver.
    fun message(total: Int, pending: Int, firstTimed: Pair<String, String>?): Message? {
        if (total == 0) return Message.Empty
        if (pending <= 0) return null
        return Message.Pending(pending, firstTimed)
    }

    fun message(inputs: ViraNotificationInputs, day: LocalDate, zone: ZoneId, detail: Boolean, locale: Locale): Message? {
        val items = inputs.planItems.filter { LocalDate.ofInstant(it.day, zone) == day }
        val pending = items.filter { it.status == PlanItemStatus.PENDING }
        val first = pending
            .mapNotNull { item -> ViraNotificationPlanner.planFire(item, zone)?.let { item to it } }
            .minByOrNull { it.second }
        // L'hora de la tasca, no la de l'avís: «Mates, a les 9:00» encara que avisi abans.
        val firstTimed = if (detail) first?.let { (item, fire) ->
            val time = item.time?.let { ViraNotificationPlanner.at(day, it, zone) } ?: fire
            item.title to DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time.atZone(zone))
        } else null
        return message(items.size, pending.size, firstTimed)
    }

    fun body(message: Message, text: ViraNotificationText): String = when (message) {
        Message.Empty -> text.get(R.string.notification_morning_empty)
        is Message.Pending -> {
            val lead = if (message.count == 1) text.get(R.string.notification_morning_one)
            else text.get(R.string.notification_morning_many, message.count)
            message.first?.let { (title, time) -> lead + " " + text.get(R.string.notification_morning_first, title, time) } ?: lead
        }
    }

    fun requests(
        inputs: ViraNotificationInputs,
        settings: ViraNotificationSettings,
        now: Instant,
        zone: ZoneId,
        text: ViraNotificationText,
        muted: ClosedOpenRange?,
    ): List<ViraPlannedNotification> {
        val today = LocalDate.ofInstant(now, zone)
        return (0 until ViraNotificationPlanner.MORNING_BUDGET).mapNotNull { offset ->
            val day = today.plusDays(offset.toLong())
            val fire = day.atTime(settings.morningTime.hour, settings.morningTime.minute).atZone(zone).toInstant()
            if (!fire.isAfter(now) || muted?.contains(fire) == true) return@mapNotNull null
            if (ViraAbsence.allowance(day, settings.since ?: now, zone) != ViraAbsence.Allowance.NORMAL) return@mapNotNull null
            val said = message(inputs, day, zone, settings.detail, settings.locale) ?: return@mapNotNull null
            ViraPlannedNotification(
                id = "${ViraNotificationPlanner.MORNING_PREFIX}${day.year}-${day.monthValue}-${day.dayOfMonth}",
                fireAtMillis = fire.toEpochMilli(),
                title = text.get(R.string.notification_morning_title),
                body = body(said, text),
                // Parla de les tasques del pla: el seu canal.
                channel = ViraNotificationChannel.PLAN,
                actions = ViraActionSet.DAY,
            )
        }
    }
}
