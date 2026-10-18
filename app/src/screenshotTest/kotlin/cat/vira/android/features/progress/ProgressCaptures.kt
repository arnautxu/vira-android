package cat.vira.android.features.progress

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ChallengePeriod
import cat.vira.android.features.progress.logic.StreakMessageCatalog
import cat.vira.android.features.progress.logic.StreakMonth
import cat.vira.android.features.progress.logic.StreakRules.DayRequirement
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.ui.ChallengeRowState
import cat.vira.android.features.progress.ui.ChallengesContent
import cat.vira.android.features.progress.ui.ChallengesUi
import cat.vira.android.features.progress.ui.FeaturedChallenge
import cat.vira.android.features.progress.ui.ProfileContent
import cat.vira.android.features.progress.ui.ProfileUi
import cat.vira.android.features.progress.ui.StreakContent
import cat.vira.android.features.progress.ui.StreakUi
import com.android.tools.screenshot.PreviewTest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/// Pantalles senceres de la pestanya Viri, a l'ample d'un iPhone (393 dp) i prou altes perquè hi
/// càpiga tot el que llisca. La lletra gran va en castellà, que és l'idioma més llarg.
@Preview(name = "clar", widthDp = 393, heightDp = 1900, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 1900, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 4200, locale = "es", fontScale = 2f)
annotation class PageCaptures

/// Les pantalles completes de celebració, a la mida d'un iPhone (393 × 852 dp).
@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 852, locale = "es", fontScale = 2f)
annotation class MomentCaptures

/// Un dijous fix: les captures no poden canviar segons el dia que passa la CI.
private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)

@Composable
private fun Capture(content: @Composable () -> Unit) {
    ViraCapture { ViraTheme(reduceMotion = true) { Box(Modifier.fillMaxSize()) { content() } } }
}

private val counter = ProgressCounter(
    totalXP = 1240,
    coinBalance = 620,
    focusSessionsCount = 12,
    habitsCompletedCount = 30,
    emotionCheckinsCount = 9,
)
private val streak = Streak(currentDays = 4, bestDays = 14, freezesAvailable = 2)

// Perfil

@PreviewTest @PageCaptures @Composable
fun ProfileWithProgress() = Capture {
    ProfileContent(
        ui = ProfileUi(
            nickname = "Nil",
            counter = counter,
            streak = streak,
            featured = FeaturedChallenge(ChallengeItem.catalog[1], progress = 1, claimed = false),
            cardsAvailable = true,
            friendsAvailable = true,
        ),
        onNicknameCommit = {}, onOpen = {}, onNavigate = {}, onCustomize = {},
    )
}

@PreviewTest @PageCaptures @Composable
fun ProfileFirstDay() = Capture {
    ProfileContent(
        ui = ProfileUi(featured = FeaturedChallenge(ChallengeItem.catalog[0], progress = 0, claimed = false)),
        onNicknameCommit = {}, onOpen = {}, onNavigate = {}, onCustomize = {},
    )
}

// Ratxa

private fun week(active: Set<LocalDate>, protected: Set<LocalDate> = emptySet()): StreakWeek {
    val start = TODAY.minusDays((TODAY.dayOfWeek.value - 1).toLong())
    return StreakWeek((0L until 7L).map { start.plusDays(it) }, active, protected)
}

@PreviewTest @PageCaptures @Composable
fun StreakRunning() = Capture {
    // Quatre dies seguits fins ahir, amb un dia guardat al mig d'un tram anterior.
    val active = (1L..4L).map { TODAY.minusDays(it) }.toSet() + (8L..12L).map { TODAY.minusDays(it) }
    val protected = setOf(TODAY.minusDays(7), TODAY.minusDays(5))
    StreakContent(
        ui = StreakUi(
            streak = streak,
            today = TODAY,
            firstDayOfWeek = DayOfWeek.MONDAY,
            week = week(active, protected),
            month = StreakMonth.of(YearMonth.from(TODAY), active, protected, cat.vira.android.features.progress.logic.ViraDays(java.time.ZoneOffset.UTC, DayOfWeek.MONDAY)),
            missing = setOf(DayRequirement.TASK, DayRequirement.EMOTION),
            coinBalance = 620,
        ),
        onBack = {}, onShiftMonth = {}, onBuySavedDay = {},
    )
}

@PreviewTest @PageCaptures @Composable
fun StreakZero() = Capture {
    StreakContent(
        ui = StreakUi(
            streak = Streak(),
            today = TODAY,
            firstDayOfWeek = DayOfWeek.MONDAY,
            week = week(emptySet()),
            month = StreakMonth.of(YearMonth.from(TODAY), emptySet(), emptySet(), cat.vira.android.features.progress.logic.ViraDays(java.time.ZoneOffset.UTC, DayOfWeek.MONDAY)),
            missing = DayRequirement.entries.toSet(),
            coinBalance = 0,
        ),
        onBack = {}, onShiftMonth = {}, onBuySavedDay = {},
    )
}

// Reptes

@PreviewTest @PageCaptures @Composable
fun ChallengesDaily() = Capture {
    val daily = ChallengeItem.catalog.filter { it.period == ChallengePeriod.DAILY }
    ChallengesContent(
        ui = ChallengesUi(
            period = ChallengePeriod.DAILY,
            rows = listOf(
                ChallengeRowState(daily[0], count = 2, claimed = false),
                ChallengeRowState(daily[1], count = 1, claimed = false),
                ChallengeRowState(daily[2], count = 3, claimed = true),
                ChallengeRowState(daily[3], count = 0, claimed = false),
            ),
            balance = 620,
        ),
        onBack = {}, onPeriod = {}, onClaim = {},
    )
}

// Celebracions

@PreviewTest @MomentCaptures @Composable
fun StreakCelebrationDay() = Capture {
    StreakCelebration(
        streakDays = 5,
        week = week((0L..4L).map { TODAY.minusDays(it) }.toSet()),
        message = stringResource(StreakMessageCatalog.lines(StreakMessageCatalog.State.RUNNING).first().text),
        onContinue = {},
        today = TODAY,
        firstDayOfWeek = DayOfWeek.MONDAY,
        celebrationsEnabled = false,
    )
}

@PreviewTest @MomentCaptures @Composable
fun LevelUp() = Capture { LevelUpCelebration(level = 5, totalXP = 1540, onContinue = {}) }

@PreviewTest @MomentCaptures @Composable
fun ChallengeClaimed() = Capture { ChallengeCelebration(ChallengeItem.catalog[0], onContinue = {}) }

@PreviewTest @MomentCaptures @Composable
fun ChallengeReady() = Capture {
    ChallengeProgressMoment(ChallengeItem.catalog[1], progress = 3, onContinue = {}, onOpenChallenges = {})
}
