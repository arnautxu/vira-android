package cat.vira.android.features.emotions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.emotion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Agregació de l'historial emocional d'un període (`EmotionHistory` de l'iPhone).
///
/// Viu fora de la pantalla perquè és lògica de dates —la que es trenca en silenci amb canvis
/// d'hora, setmanes a cavall de dos mesos i dies sense cap registre— i s'ha de poder provar sola.
///
/// ## Un dia sense registres hi és igualment
/// `days` inclou **tots** els dies del període, també els buits: un dia sense registre és
/// absència, no fracàs (`DATA_MODEL.md` §2.4). Enlloc no hi ha, ni hi pot haver, cap "t'has
/// saltat un dia".
///
/// ## Cap valoració de les emocions
/// `counts` ordena per **freqüència**, mai per si l'emoció és agradable: no hi ha emocions millors
/// ni pitjors (`VIRI_AND_GAMIFICATION.md` §2.2.1, "prohibit: rànquings d'emocions"). Tampoc hi ha
/// cap anàlisi del text de les notes: seria vigilància.
class EmotionHistory(
    source: List<EmotionEntry>,
    range: EmotionHistoryRange,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    data class Day(val date: LocalDate, val entries: List<EmotionEntry>) {
        val isEmpty: Boolean get() = entries.isEmpty()
    }

    data class Count(val emotion: Emotion, val count: Int)

    /// Tots els dies del període, del més antic al més recent (avui), buits inclosos.
    val days: List<Day>
    /// Els registres del període, del més recent al més antic.
    val entries: List<EmotionEntry>
    /// Freqüència per emoció, de més a menys. Només les que hi apareixen.
    val counts: List<Count>

    init {
        val today = now.atZone(zone).toLocalDate()
        val dates = (range.days - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val start = dates.first().atStartOfDay(zone).toInstant()

        val inRange = source.filter { it.createdAt >= start }
        entries = inRange.sortedByDescending { it.createdAt }

        val byDay = entries.groupBy { it.createdAt.atZone(zone).toLocalDate() }
        days = dates.map { Day(it, byDay[it].orEmpty()) }

        // Desempat per l'ordre de la graella del check-in, no per res que suggereixi una
        // valoració: amb el mateix recompte, l'ordre ha de ser estable entre obertures.
        counts = inRange.groupingBy { it.emotion }.eachCount()
            .map { Count(it.key, it.value) }
            .sortedWith(compareByDescending<Count> { it.count }.thenBy { it.emotion.ordinal })
    }

    val isEmpty: Boolean get() = entries.isEmpty()

    /// Nombre màxim de registres en un sol dia.
    val busiestDayCount: Int get() = days.maxOfOrNull { it.entries.size } ?: 0

    /// Els dies que tenen alguna cosa, del més recent al més antic. És el que llista la pantalla.
    val daysWithEntries: List<Day> get() = days.filterNot { it.isEmpty }.reversed()
}

/// Finestra de l'historial. Dues, no cinc: triar-ne una no pot ser una decisió.
enum class EmotionHistoryRange(val days: Int, @StringRes val title: Int, @StringRes val heroTitle: Int) {
    WEEK(7, R.string.emotions_history_range_week, R.string.emotions_history_hero_week),
    MONTH(30, R.string.emotions_history_range_month, R.string.emotions_history_hero_month),
}

/// Quina escena surt a la capçalera, per franja del dia, com Inici (`EmotionHistoryArt`).
object EmotionHistoryArt {

    /// Matí de les 6 a les 13, tarda fins a les 20, nit la resta.
    fun band(hour: Int): String = when (hour) {
        in 6 until 13 -> "morning"
        in 13 until 20 -> "afternoon"
        else -> "night"
    }

    @DrawableRes
    fun drawable(hour: Int): Int = when (band(hour)) {
        "morning" -> R.drawable.viri_history_morning
        "afternoon" -> R.drawable.viri_history_afternoon
        else -> R.drawable.viri_history_night
    }
}
