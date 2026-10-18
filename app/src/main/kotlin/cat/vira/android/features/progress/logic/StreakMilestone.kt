package cat.vira.android.features.progress.logic

import cat.vira.android.core.data.model.Streak

/// Decideix si toca la pantalla de fita de ratxa (`StreakMilestone.swift`). Funció pura: torna la
/// ratxa amb el senyal marcat i la clau de la carta (`week_streak`, `month_streak`), que la
/// col·lecció de Botiga i premis sap dibuixar.
object StreakMilestone {
    const val WEEK_CARD = "week_streak"
    const val MONTH_CARD = "month_streak"

    /// Mai les dues alhora: si totes dues estan pendents (una còpia restaurada), guanya la gran.
    /// Amb la ratxa amagada, ni senyal ni pantalla.
    fun checkAndMark(streak: Streak): Pair<Streak, String?> = when {
        !streak.isVisible -> streak to null
        streak.bestDays >= 30 && !streak.hasCelebratedMonthStreak ->
            streak.copy(hasCelebratedMonthStreak = true) to MONTH_CARD
        streak.bestDays >= 7 && !streak.hasCelebratedWeekStreak ->
            streak.copy(hasCelebratedWeekStreak = true) to WEEK_CARD
        else -> streak to null
    }
}
