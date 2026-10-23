package cat.vira.android.features.add

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/// Treu el dia i l'hora d'una frase escrita i deixa el títol net (`ViraCapturePhrase.detect`): «dentista
/// demà a les 9» es desa com «dentista», demà a les 9:00, i el full no torna a preguntar el que ja diu.
///
/// L'iPhone fa la primera passada amb `NSDataDetector`, que Android no té. Aquí la fan unes quantes
/// paraules en català, castellà i anglès: avui, demà, passat demà i els dies de la setmana. La segona
/// passada, la de les hores («a les 8», «a las 20h», «at 5 pm»), és la mateixa expressió de l'iPhone.
///
/// Res d'això endevina: si no hi ha ni dia ni hora, la frase sencera es queda com a títol.
object CapturePhrase {

    /// `day` i `time` són `null` quan la frase no en diu res: una píndola d'«Avui» que ningú ha
    /// escrit seria mentir.
    data class Detection(val title: String, val day: LocalDate?, val time: LocalTime?) {
        companion object {
            val NONE = Detection("", null, null)
        }
    }

    fun detect(raw: String, today: LocalDate): Detection {
        val text = raw.trim()
        var title = text
        var day: LocalDate? = null
        var time: LocalTime? = null

        dayMatch(text, today)?.let { (found, range) ->
            day = found
            title = removing(range, text)
        }
        hourMatch(title)?.let { (found, range) ->
            time = found
            title = removing(range, title)
        }
        return Detection(title = title.ifEmpty { text }, day = day, time = time)
    }

    // Dies

    private const val NOT_LETTER_BEFORE = """(?<!\p{L})"""
    private const val NOT_LETTER_AFTER = """(?!\p{L})"""

    /// Per ordre: «passat demà» abans que «demà». «mañana» sola és demà; «de la mañana» és una franja.
    private val relativeDays: List<Pair<Regex, Long>> = listOf(
        """passat\s+demà|pasado\s+mañana|day\s+after\s+tomorrow""" to 2L,
        """demà|(?<!de\sla\s)mañana|tomorrow""" to 1L,
        """avui|hoy|today""" to 0L,
    ).map { (words, offset) -> word(words) to offset }

    private val weekdays: List<Pair<Regex, DayOfWeek>> = listOf(
        "dilluns|lunes|monday" to DayOfWeek.MONDAY,
        "dimarts|martes|tuesday" to DayOfWeek.TUESDAY,
        "dimecres|miércoles|miercoles|wednesday" to DayOfWeek.WEDNESDAY,
        "dijous|jueves|thursday" to DayOfWeek.THURSDAY,
        "divendres|viernes|friday" to DayOfWeek.FRIDAY,
        "dissabte|sábado|sabado|saturday" to DayOfWeek.SATURDAY,
        "diumenge|domingo|sunday" to DayOfWeek.SUNDAY,
    ).map { (words, weekday) -> word(words) to weekday }

    private fun word(alternatives: String) =
        Regex("$NOT_LETTER_BEFORE(?:$alternatives)$NOT_LETTER_AFTER", RegexOption.IGNORE_CASE)

    /// El primer dia que diu la frase. Un dia de la setmana és el proper que ve, i avui si és avui:
    /// «dilluns» un dimecres ja és el de la setmana que ve.
    private fun dayMatch(text: String, today: LocalDate): Pair<LocalDate, IntRange>? {
        val candidates = relativeDays.mapNotNull { (regex, offset) ->
            regex.find(text)?.let { today.plusDays(offset) to it.range }
        } + weekdays.mapNotNull { (regex, weekday) ->
            regex.find(text)?.let {
                val ahead = (weekday.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(ahead.toLong()) to it.range
            }
        }
        return candidates.minByOrNull { it.second.first }
    }

    // Hores

    /// «a les 8», «a les 8:30», «a las 20h», «at 5 pm», «a les 9 del matí». La franja del dia mana per
    /// damunt del número; sense franja, **l'hora es pren literal**: «a les 8» són les 8:00.
    private val hourPattern = Regex(
        """(?:\b(?:a\s+l(?:es|as|a)|at)\s+)(\d{1,2})(?:\s*[:.h]\s*(\d{2}))?\s*""" +
            """(h\b|am\b|pm\b|a\.m\.|p\.m\.|del\s+matí|de\s+la\s+tarda|de\s+la\s+nit|""" +
            """de\s+la\s+mañana|de\s+la\s+tarde|de\s+la\s+noche|in\s+the\s+morning|""" +
            """in\s+the\s+afternoon|in\s+the\s+evening)?""",
        setOf(RegexOption.IGNORE_CASE),
    )

    private fun hourMatch(text: String): Pair<LocalTime, IntRange>? {
        val match = hourPattern.find(text) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        if (hour > 24) return null
        val minute = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        val marker = match.groupValues[3].lowercase(Locale.ROOT)
        if (isAfternoon(marker) && hour < 12) hour += 12
        if ((marker.startsWith("am") || marker.startsWith("a.m") || "mat" in marker) && hour == 12) hour = 0
        if (hour >= 24 || minute >= 60) return null
        return LocalTime.of(hour, minute) to match.range
    }

    private fun isAfternoon(marker: String) =
        marker.startsWith("pm") || marker.startsWith("p.m") ||
            listOf("tarda", "tarde", "afternoon", "nit", "noche", "evening").any { it in marker }

    // Títol

    private const val TRIM = " ,;:-–—"
    private val orphans = listOf("el", "la", "l'", "a", "de", "at", "on", "the")

    /// Treu el tros que era la data i deixa el títol presentable: sense espais dobles i sense la
    /// preposició orfe que sovint queda penjada al final («dentista **el**», «call **at**»).
    private fun removing(range: IntRange, text: String): String {
        var result = text.removeRange(range).replace(Regex("""\s{2,}"""), " ").trim { it in TRIM }
        orphans.firstOrNull { result.lowercase(Locale.ROOT).endsWith(" $it") }?.let { orphan ->
            result = result.dropLast(orphan.length + 1)
        }
        return result.trim { it in TRIM }
    }
}
