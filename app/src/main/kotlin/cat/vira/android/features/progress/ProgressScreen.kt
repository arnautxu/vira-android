package cat.vira.android.features.progress

import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.sound.ViraSound
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.features.shop.AccessoryItem
import cat.vira.android.features.progress.ui.WelcomeChallengeUi
import cat.vira.android.features.progress.logic.ViraWelcomeChallenge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraNavigator
import cat.vira.android.app.ProgressDestination
import cat.vira.android.app.ViraReleaseGate
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ChallengePeriod
import cat.vira.android.features.progress.logic.StreakMonth
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.logic.ViraChallenges
import cat.vira.android.features.progress.logic.ViraDays
import cat.vira.android.features.progress.logic.ViraProgress
import cat.vira.android.features.progress.ui.ChallengeRowState
import cat.vira.android.features.progress.ui.ChallengesContent
import cat.vira.android.features.progress.ui.ChallengesUi
import cat.vira.android.features.progress.ui.FeaturedChallenge
import cat.vira.android.features.progress.ui.ProfileContent
import cat.vira.android.features.progress.ui.ProfileUi
import cat.vira.android.features.progress.ui.ProgressBackButton
import cat.vira.android.features.progress.ui.StreakContent
import cat.vira.android.features.progress.ui.StreakUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields

/// La pestanya Viri (`ProgressTabView`): el Perfil i les pantalles que se n'obren —ratxa, reptes,
/// botiga, premis, cartes, amics—, amb el camí a `LocalViraNavigator.current.progressPath` perquè
/// altres pantalles hi puguin entrar directament (Inici a la ratxa, la capçalera al Perfil).
///
/// En obrir-la posa la ratxa al dia (`ViraProgress.reconcileOnLaunch`): cobreix els dies perduts
/// amb dies guardats, o la reinicia sense drama, encara que l'usuari no faci res.
@Composable
fun ProgressScreen() {
    val navigator = LocalViraNavigator.current
    val backend = LocalViraBackend.current
    val firstDay = WeekFields.of(viraLocale()).firstDayOfWeek
    val progress = remember(backend, firstDay) { ViraProgress(ViraStore(backend.db), firstDayOfWeek = { firstDay }) }
    LaunchedEffect(progress) { progress.reconcileOnLaunch() }

    val path = navigator.progressPath
    val back: () -> Unit = { if (path.isNotEmpty()) path.removeAt(path.lastIndex) }
    BackHandler(enabled = path.isNotEmpty(), onBack = back)

    AnimatedContent(
        targetState = path.lastOrNull(),
        transitionSpec = {
            val forward = targetState != null && (initialState == null || path.size > 1)
            if (forward) (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            else (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
        },
        label = "progressPath",
    ) { destination ->
        when (destination) {
            null -> Box {
                // «Aquí és Viri», el primer cop (2026-10-22): era una parada del tomb. Mentre hi és,
                // el perfil de sota no es pot recórrer amb TalkBack.
                val tip = rememberViriTabTipVisible()
                Box(if (tip) Modifier.clearAndSetSemantics { } else Modifier) {
                    ProfileRoute(backend, onOpen = { path.add(it) })
                }
                ViriTabTip(tip)
            }
            ProgressDestination.Streak -> StreakRoute(backend, progress, back)
            ProgressDestination.Challenges -> ChallengesRoute(backend, progress, back)
            ProgressDestination.Shop -> ProgressSlots.Shop(back)
            ProgressDestination.PersonalRewards -> ProgressSlots.PersonalRewards(back)
            ProgressDestination.Cards ->
                if (ViraReleaseGate.cards) ProgressSlots.Cards(back) else ExternalPage(back) { Gated(R.string.profile_cards_title) }
            // Amics encara no és a Android: a les builds de proves, el mateix avís que a Google Play.
            ProgressDestination.Friends -> ExternalPage(back) { Gated(R.string.friends_title) }
        }
    }
}

// Perfil

@Composable
private fun ProfileRoute(backend: ViraBackend, onOpen: (ProgressDestination) -> Unit) {
    val navigator = LocalViraNavigator.current
    val db = backend.db
    val scope = rememberCoroutineScope()
    val store = remember(db) { ViraStore(db) }
    val profile by db.profiles().observe().collectAsStateWithLifecycle(null)
    val counter by db.progressCounters().observe().collectAsStateWithLifecycle(null)
    val streak by db.streaks().observe().collectAsStateWithLifecycle(null)
    val claims by db.challengeClaims().observeAll().collectAsStateWithLifecycle(emptyList())
    val ledger by db.coinTransactions().observeAll().collectAsStateWithLifecycle(emptyList())
    val progressVisible by rememberPreference(backend.preferences, PROGRESS_VISIBLE, true)
    val shopVisible by rememberPreference(backend.preferences, SHOP_VISIBLE, true)
    val challengesEnabled by rememberPreference(backend.preferences, ViraPreferences.CHALLENGES_ENABLED, true)
    var customizing by rememberSaveable { mutableStateOf(false) }

    // `rememberNow()` fa recompondre a mitjanit; el límit superior es llegeix a cada recomposició
    // perquè una fila nova del llibre (sincronitzada o d'una altra pantalla) compti a l'instant.
    rememberNow()
    val now = Instant.now()
    val days = ViraDays(ZoneId.systemDefault(), WeekFields.of(viraLocale()).firstDayOfWeek)
    val featured = ViraChallenges.featuredDaily(ledger, claims, now, days)?.let { item ->
        FeaturedChallenge(item, ViraChallenges.progress(item, ledger, now, days), ViraChallenges.isClaimed(item, claims, now, days))
    }

    ProfileContent(
        ui = ProfileUi(
            nickname = profile?.displayName.orEmpty(),
            counter = counter,
            streak = streak,
            progressVisible = progressVisible,
            shopVisible = shopVisible,
            challengesEnabled = challengesEnabled,
            featured = featured,
            cardsAvailable = ViraReleaseGate.cards,
            friendsAvailable = ViraReleaseGate.friends,
        ),
        onNicknameCommit = { name ->
            scope.launch {
                val current = store.profile()
                if (current.displayName != name) store.save(db.profiles(), current.copy(displayName = name))
            }
        },
        onOpen = onOpen,
        onNavigate = navigator::navigate,
        onCustomize = { customizing = true },
    )
    if (customizing) {
        Dialog(onDismissRequest = { customizing = false }) {
            ProgressSlots.Customize(onDismiss = { customizing = false })
        }
    }
}

// Ratxa

@Composable
private fun StreakRoute(backend: ViraBackend, progress: ViraProgress, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = backend.db
    val scope = rememberCoroutineScope()
    val streak by db.streaks().observe().collectAsStateWithLifecycle(null)
    val counter by db.progressCounters().observe().collectAsStateWithLifecycle(null)
    val habitLogs by db.habitLogs().observeAll().collectAsStateWithLifecycle(emptyList())
    val emotions by db.emotionEntries().observeAll().collectAsStateWithLifecycle(emptyList())
    val items by db.planItems().observeAll().collectAsStateWithLifecycle(emptyList())
    // Es calcula de les dades: desfer una acció es reflecteix sola.
    // El dia d'avui també és clau: passada la mitjanit, el que faltava ahir no és el que falta avui.
    val today = rememberNow().atZone(ZoneId.systemDefault()).toLocalDate()
    val missing by produceState<Set<cat.vira.android.features.progress.logic.StreakRules.DayRequirement>?>(null, habitLogs, emotions, items, today) {
        value = progress.missingRequirements()
    }
    // El mes que s'està mirant. Es pot retrocedir: el passat és el que dona valor al calendari.
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)

    val days = ViraDays(ZoneId.systemDefault(), WeekFields.of(viraLocale()).firstDayOfWeek)
    StreakContent(
        ui = StreakUi(
            streak = streak,
            today = today,
            firstDayOfWeek = days.firstDayOfWeek,
            week = StreakWeek.of(streak, today, days),
            month = StreakMonth.of(streak, month, days),
            missing = missing,
            coinBalance = counter?.coinBalance ?: 0,
        ),
        onBack = onBack,
        onShiftMonth = { monthText = month.plusMonths(it).toString() },
        onBuySavedDay = {
            scope.launch {
                val bought = progress.buyFreeze()
                ViraHaptics.play(context, if (bought) ViraHaptics.Feedback.Reward else ViraHaptics.Feedback.Warning)
                // «No arribes al preu» no sona: és un avís, no un error, i ja vibra.
                if (bought) ViraSound.play(context, ViraSound.Cue.SAVED_DAY)
            }
        },
    )
}

// Reptes

/// Has recollit les monedes d'un repte: el «ka-ching» i la moneda (`ViraSound.play(.coins)`).
private fun earned(context: android.content.Context) {
    ViraHaptics.play(context, ViraHaptics.Feedback.Reward)
    ViraSound.play(context, ViraSound.Cue.COINS)
}

@Composable
private fun ChallengesRoute(backend: ViraBackend, progress: ViraProgress, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = backend.db
    val scope = rememberCoroutineScope()
    val claims by db.challengeClaims().observeAll().collectAsStateWithLifecycle(emptyList())
    val ledger by db.coinTransactions().observeAll().collectAsStateWithLifecycle(emptyList())
    val counter by db.progressCounters().observe().collectAsStateWithLifecycle(null)
    val accessories by db.accessoryOwnerships().observeAll().collectAsStateWithLifecycle(emptyList())
    val celebrationsEnabled by rememberPreference(backend.preferences, ViraPreferences.CELEBRATIONS_ENABLED, true)
    var periodName by rememberSaveable { mutableStateOf(ChallengePeriod.DAILY.name) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var celebratingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val period = ChallengePeriod.valueOf(periodName)

    // `rememberNow()` fa recompondre a mitjanit; el límit superior es llegeix a cada recomposició
    // perquè una fila nova del llibre (sincronitzada o d'una altra pantalla) compti a l'instant.
    rememberNow()
    val now = Instant.now()
    val days = ViraDays(ZoneId.systemDefault(), WeekFields.of(viraLocale()).firstDayOfWeek)
    val rows = ChallengeItem.catalog.filter { it.period == period }.map {
        ChallengeRowState(it, ViraChallenges.progress(it, ledger, now, days), ViraChallenges.isClaimed(it, claims, now, days))
    }
    val balance = counter?.coinBalance ?: 0
    val welcome = if (ViraWelcomeChallenge.isClaimed(claims)) null else WelcomeChallengeUi(
        activeDays = ViraWelcomeChallenge.activeDays(ledger, days.zone, now),
        balance = balance,
        // Les ulleres, si encara no són teves i el saldo no hi arriba.
        firstItem = AccessoryItem.catalog.minByOrNull { it.price }
            ?.takeIf { item -> accessories.none { it.key == item.key } && balance < item.price },
    )
    ChallengesContent(
        ui = ChallengesUi(period, rows, balance, saveFailed, welcome),
        onBack = onBack,
        onPeriod = { periodName = it.name },
        onClaimWelcome = {
            scope.launch {
                try {
                    if (progress.claimWelcome()) {
                        saveFailed = false
                        earned(context)
                    }
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
        },
        onClaim = { item ->
            scope.launch {
                try {
                    if (progress.claim(item)) {
                        saveFailed = false
                        earned(context)
                        if (celebrationsEnabled) celebratingKey = item.key
                    }
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
        },
    )
    ChallengeItem.catalog.firstOrNull { it.key == celebratingKey }?.let { item ->
        Dialog(
            onDismissRequest = { celebratingKey = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ChallengeCelebration(item, onContinue = { celebratingKey = null })
        }
    }
}

// Pàgines d'altres fils

/// Una pàgina sense barra pròpia dins de la pestanya: el botó d'enrere a dalt i la pàgina a sota.
@Composable
private fun ExternalPage(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        ProgressBackButton(onBack, Modifier.padding(horizontal = ViraSpacing.xs))
        Box(Modifier.weight(1f)) { content() }
    }
}

/// Una funció construïda que encara no pot sortir en aquesta build (`PendingFeatureContent`).
@Composable
private fun Gated(title: Int) {
    ViraEmptyState(title = stringResource(title), message = stringResource(R.string.release_pending_message))
}

/// «Mostrar progrés» i «Mostrar botiga» d'Ajustos, amb el nom de l'iPhone. Les claus són del fil
/// d'Ajustos; aquí només es llegeixen.
private const val PROGRESS_VISIBLE = "settings.progressVisible"
private const val SHOP_VISIBLE = "settings.shopVisible"

/// L'instant d'ara, que **es torna a llegir en passar la mitjanit**: amb la pantalla oberta, el
/// dia, la setmana i els reptes canvien de cicle sense haver de sortir-ne.
@Composable
private fun rememberNow(): Instant {
    val now by produceState(Instant.now()) {
        while (true) {
            val zone = ZoneId.systemDefault()
            val next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
            delay(maxOf(1_000L, Duration.between(Instant.now(), next).toMillis() + 1_000L))
            value = Instant.now()
        }
    }
    return now
}
