package cat.vira.android.features.help

import androidx.annotation.StringRes
import cat.vira.android.R

/// Les tres fases de la respiració guiada (4 + 2 + 4 s), com `BreathPhase` de l'iPhone.
enum class BreathPhase(@StringRes val title: Int, val seconds: Int) {
    Inhale(R.string.help_breathe_inhale, 4),
    Hold(R.string.help_breathe_hold, 2),
    Exhale(R.string.help_breathe_exhale, 4),
}

/// El rellotge de la respiració guiada de 60 s (`HelpBreatheView`): quina fase toca a cada segon
/// i quan comença un tram nou. Sense XP, sense ratxa, sense recompensa: `VIRI_AND_GAMIFICATION.md`
/// prohibeix lligar cap mecànica de joc a un context d'ajuda.
object HelpBreathing {

    const val TOTAL_SECONDS = 60

    val cycleSeconds: Int = BreathPhase.entries.sumOf { it.seconds }

    fun phase(elapsed: Int): BreathPhase {
        val position = elapsed % cycleSeconds
        return when {
            position < BreathPhase.Inhale.seconds -> BreathPhase.Inhale
            position < BreathPhase.Inhale.seconds + BreathPhase.Hold.seconds -> BreathPhase.Hold
            else -> BreathPhase.Exhale
        }
    }

    /// El tram que **comença** en aquest segon, si n'hi ha cap: el cercle i la vibració es disparen
    /// un cop per tram, no per segon. Aguantar no en té: el cercle es queda ple i el palmell, quiet.
    fun stretchStartingAt(elapsed: Int): BreathPhase? {
        if (elapsed < 0 || elapsed >= TOTAL_SECONDS) return null
        return when (elapsed % cycleSeconds) {
            0 -> BreathPhase.Inhale
            BreathPhase.Inhale.seconds + BreathPhase.Hold.seconds -> BreathPhase.Exhale
            else -> null
        }
    }
}

/// Els cinc sentits de l'Ancoratge, de més a menys coses (`GroundingSense` de l'iPhone). El
/// pictograma acompanya; el text és qui ho diu.
enum class GroundingSense(@StringRes val title: Int, val pictogram: Int) {
    See(R.string.help_grounding_see, R.drawable.picto_sun),
    Touch(R.string.help_grounding_touch, R.drawable.picto_heart),
    Hear(R.string.help_grounding_hear, R.drawable.picto_habit_music),
    Smell(R.string.help_grounding_smell, R.drawable.picto_habit_plants),
    Taste(R.string.help_grounding_taste, R.drawable.picto_habit_fruit),
    ;

    companion object {
        val count: Int get() = entries.size

        /// El pas que es veu a la targeta: el darrer quan ja s'ha acabat.
        fun current(step: Int): GroundingSense = entries[step.coerceIn(0, count - 1)]

        /// El «n de 5» de la targeta, que no passa mai de 5.
        fun shownStep(step: Int): Int = (step + 1).coerceIn(1, count)

        fun isFinished(step: Int): Boolean = step >= count
    }
}

/// El número que es passa a Missatges: només xifres i el `+`, com `openMessages` de l'iPhone.
/// Una extensió després d'una pausa o espera (`,` `;` `p` `w`) no és part del número: s'hi
/// enganxaria i Missatges s'obriria amb un altre. Buit vol dir que no hi ha res a obrir.
fun smsDigits(phoneNumber: String): String =
    phoneNumber.takeWhile { it !in ",;pPwW" }.filter { it.isDigit() || it == '+' }
