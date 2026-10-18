package cat.vira.android.features.progress.logic

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import java.time.Instant

/// Cada quan es renova un repte. **Mai amb penalització en acabar-se el cicle**: un repte no
/// recollit simplement desapareix i el següent cicle en comença un de nou.
enum class ChallengePeriod { DAILY, WEEKLY }

/// Un repte del catàleg (`Challenge.swift`). Contingut de l'app, no dades d'usuari: el progrés es
/// calcula en viu contra el llibre major, i només es desa quan es recull (`ChallengeClaim`).
///
/// Cap repte de medicació: `CoinReason` no en té cap cas.
data class ChallengeItem(
    val key: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val pictogram: Int,
    val period: ChallengePeriod,
    /// El motiu que es compta (mai `CHALLENGE`, que és el que el repte mateix concedeix).
    val requirementReason: CoinReason,
    val threshold: Int,
    val reward: Int,
) {
    companion object {
        /// Catàleg de l'iPhone, en el mateix ordre: repte diari 50 ViriCoins, setmanal 200.
        val catalog = listOf(
            ChallengeItem("daily_emotion", R.string.challenge_daily_emotion_title, R.string.challenge_daily_emotion_description,
                R.drawable.picto_journal, ChallengePeriod.DAILY, CoinReason.EMOTION, threshold = 2, reward = 50),
            ChallengeItem("daily_habits", R.string.challenge_daily_habits_title, R.string.challenge_daily_habits_description,
                R.drawable.picto_habits, ChallengePeriod.DAILY, CoinReason.HABIT, threshold = 3, reward = 50),
            ChallengeItem("weekly_focus", R.string.challenge_weekly_focus_title, R.string.challenge_weekly_focus_description,
                R.drawable.picto_pomodoro, ChallengePeriod.WEEKLY, CoinReason.FOCUS, threshold = 3, reward = 200),
            ChallengeItem("weekly_habits", R.string.challenge_weekly_habits_title, R.string.challenge_weekly_habits_description,
                R.drawable.picto_habits, ChallengePeriod.WEEKLY, CoinReason.HABIT, threshold = 12, reward = 200),
            ChallengeItem("daily_plan", R.string.challenge_daily_plan_title, R.string.challenge_daily_plan_description,
                R.drawable.picto_plan_checklist, ChallengePeriod.DAILY, CoinReason.PLAN_ITEM, threshold = 3, reward = 50),
            ChallengeItem("daily_focus", R.string.challenge_daily_focus_title, R.string.challenge_daily_focus_description,
                R.drawable.picto_pomodoro, ChallengePeriod.DAILY, CoinReason.FOCUS, threshold = 1, reward = 50),
            ChallengeItem("weekly_emotion", R.string.challenge_weekly_emotion_title, R.string.challenge_weekly_emotion_description,
                R.drawable.picto_journal, ChallengePeriod.WEEKLY, CoinReason.EMOTION, threshold = 5, reward = 200),
            ChallengeItem("weekly_plan", R.string.challenge_weekly_plan_title, R.string.challenge_weekly_plan_description,
                R.drawable.picto_calendar, ChallengePeriod.WEEKLY, CoinReason.PLAN_ITEM, threshold = 15, reward = 200),
        )
    }
}

/// Progrés dels reptes (`ViraChallenges.swift`), sobre el llibre major que ja escriu `ViraRewards`:
/// quantes concessions d'un motiu hi ha hagut des de l'inici del cicle. Funcions pures; recollir
/// el premi és `ViraProgress.claim`.
object ViraChallenges {

    /// Inici del cicle: el dia, o el primer dia de la setmana del telèfon.
    fun cycleStart(period: ChallengePeriod, now: Instant, days: ViraDays): Instant = when (period) {
        ChallengePeriod.DAILY -> days.startOfDay(now)
        ChallengePeriod.WEEKLY -> days.start(days.weekStart(days.date(now)))
    }

    fun progress(item: ChallengeItem, ledger: List<CoinTransaction>, now: Instant, days: ViraDays): Int {
        val start = cycleStart(item.period, now, days)
        return ledger.count {
            it.reasonRaw == item.requirementReason.raw && it.amount > 0 && it.createdAt >= start && it.createdAt <= now
        }
    }

    fun isCompleted(item: ChallengeItem, ledger: List<CoinTransaction>, now: Instant, days: ViraDays): Boolean =
        progress(item, ledger, now, days) >= item.threshold

    fun isClaimed(item: ChallengeItem, claims: List<ChallengeClaim>, now: Instant, days: ViraDays): Boolean {
        val start = cycleStart(item.period, now, days)
        return claims.any { it.key == item.key && it.cycleStart == start }
    }

    /// El repte de la portada: primer un premi preparat per recollir; després, un repte pendent.
    /// Completar-lo no el pot fer desaparèixer abans de recollir-ne el premi.
    fun featuredDaily(ledger: List<CoinTransaction>, claims: List<ChallengeClaim>, now: Instant, days: ViraDays): ChallengeItem? {
        val daily = ChallengeItem.catalog.filter { it.period == ChallengePeriod.DAILY }
        return daily.firstOrNull { isCompleted(it, ledger, now, days) && !isClaimed(it, claims, now, days) }
            ?: daily.firstOrNull { !isClaimed(it, claims, now, days) }
            ?: daily.firstOrNull()
    }
}
