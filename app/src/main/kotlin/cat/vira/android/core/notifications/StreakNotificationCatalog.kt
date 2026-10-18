package cat.vira.android.core.notifications

import cat.vira.android.R
import java.time.LocalDate

/// El que diu Viri quan surt a la pantalla bloquejada (`StreakNotificationCatalog` de l'iPhone).
///
/// Va a part de les frases de la targeta de ratxa: la targeta parla amb el dia ja resolt, i això
/// surt **abans**, amb el dia encara per tancar.
///
/// ## Les tres regles que manen aquí
/// 1. **Mai diu què falta.** Ni el check-in, ni el nom d'un hàbit, ni res de medicació: parla
///    del **dia**, que no és una dada de ningú. Per això no té cap variant «amb detall», ni sota
///    interruptor.
/// 2. **Mai culpa, ni compta el que no s'ha fet** (`VIRI_AND_GAMIFICATION.md` §2.3.2).
/// 3. **Una frase diferent cada dia**: 31 per a l'avís del vespre.
object StreakNotificationCatalog {

    /// Què ha de dir l'avís d'aquesta franja.
    sealed interface Kind {
        /// El dia encara no compta i queda estona.
        data object Nudge : Kind
        /// S'ha gastat un dia guardat i la ratxa segueix sencera.
        data object Protected : Kind
        /// La ratxa ha tornat a zero. El rècord es conserva i el text no en fa cap drama.
        data object Reset : Kind
        /// Fita de ratxa assolida, amb el nombre de dies.
        data class Milestone(val days: Int) : Kind
    }

    // Llistes escrites a mà i no muntades amb `getIdentifier`: el reductor de recursos de la
    // versió de botiga esborraria els textos que només es demanen pel nom.
    private val nudge = listOf(
        R.string.notification_streak_nudge_1,
        R.string.notification_streak_nudge_2,
        R.string.notification_streak_nudge_3,
        R.string.notification_streak_nudge_4,
        R.string.notification_streak_nudge_5,
        R.string.notification_streak_nudge_6,
        R.string.notification_streak_nudge_7,
        R.string.notification_streak_nudge_8,
        R.string.notification_streak_nudge_9,
        R.string.notification_streak_nudge_10,
        R.string.notification_streak_nudge_11,
        R.string.notification_streak_nudge_12,
        R.string.notification_streak_nudge_13,
        R.string.notification_streak_nudge_14,
        R.string.notification_streak_nudge_15,
        R.string.notification_streak_nudge_16,
        R.string.notification_streak_nudge_17,
        R.string.notification_streak_nudge_18,
        R.string.notification_streak_nudge_19,
        R.string.notification_streak_nudge_20,
        R.string.notification_streak_nudge_21,
        R.string.notification_streak_nudge_22,
        R.string.notification_streak_nudge_23,
        R.string.notification_streak_nudge_24,
        R.string.notification_streak_nudge_25,
        R.string.notification_streak_nudge_26,
        R.string.notification_streak_nudge_27,
        R.string.notification_streak_nudge_28,
        R.string.notification_streak_nudge_29,
        R.string.notification_streak_nudge_30,
        R.string.notification_streak_nudge_31,
    )
    private val protected = listOf(
        R.string.notification_streak_protected_1,
        R.string.notification_streak_protected_2,
        R.string.notification_streak_protected_3,
        R.string.notification_streak_protected_4,
        R.string.notification_streak_protected_5,
        R.string.notification_streak_protected_6,
        R.string.notification_streak_protected_7,
        R.string.notification_streak_protected_8,
        R.string.notification_streak_protected_9,
        R.string.notification_streak_protected_10,
    )
    private val reset = listOf(
        R.string.notification_streak_reset_1,
        R.string.notification_streak_reset_2,
        R.string.notification_streak_reset_3,
        R.string.notification_streak_reset_4,
        R.string.notification_streak_reset_5,
        R.string.notification_streak_reset_6,
        R.string.notification_streak_reset_7,
        R.string.notification_streak_reset_8,
        R.string.notification_streak_reset_9,
        R.string.notification_streak_reset_10,
    )
    private val milestone = listOf(
        R.string.notification_streak_milestone_1,
        R.string.notification_streak_milestone_2,
        R.string.notification_streak_milestone_3,
        R.string.notification_streak_milestone_4,
        R.string.notification_streak_milestone_5,
        R.string.notification_streak_milestone_6,
        R.string.notification_streak_milestone_7,
        R.string.notification_streak_milestone_8,
    )

    /// Títol compartit amb la resta d'avisos: només la marca. Mai diu de què va.
    fun title(text: ViraNotificationText) = text.get(R.string.notification_generic_title)

    /// Determinista pel dia: la mateixa frase si l'avís es reprograma deu vegades el mateix dia, i
    /// una de diferent l'endemà. L'índex és el número de dia des de l'1 de gener de l'any 1, com
    /// `ordinality(of: .day, in: .era)` a l'iPhone.
    fun body(kind: Kind, day: LocalDate, text: ViraNotificationText): String {
        val index = dayOrdinal(day)
        return when (kind) {
            Kind.Nudge -> text.get(nudge.pick(index))
            Kind.Protected -> text.get(protected.pick(index))
            Kind.Reset -> text.get(reset.pick(index))
            is Kind.Milestone -> text.get(milestone.pick(index), kind.days)
        }
    }

    /// Dia 1 = 0001-01-01.
    fun dayOrdinal(day: LocalDate): Long = day.toEpochDay() - LocalDate.of(1, 1, 1).toEpochDay() + 1

    private fun List<Int>.pick(index: Long) = this[Math.floorMod(index, size.toLong()).toInt()]
}
