package cat.vira.android.app

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.room.withTransaction
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.features.progress.LevelUpCelebration
import cat.vira.android.features.progress.StreakCelebration
import cat.vira.android.features.progress.logic.StreakMessageCatalog
import cat.vira.android.features.progress.logic.StreakWeek
import cat.vira.android.features.progress.logic.ViraDays
import cat.vira.android.features.progress.logic.ViraLevel
import cat.vira.android.features.progress.logic.ViraProgress
import cat.vira.android.features.shop.CardItem
import cat.vira.android.features.shop.CardUnlockedScreen
import cat.vira.android.features.shop.ViraCardCelebrations
import cat.vira.android.features.settings.ViraSoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

/// Les celebracions de ratxa, de carta i de nivell (`ViraCelebrationQueue` de l'iPhone): quan
/// surten, quina guanya si en volen sortir dues alhora, i com s'esperen si hi ha una altra pantalla
/// completa al davant.
///
/// És **reactiu**: cap pantalla no demana una celebració. Són efecte d'una dada global
/// (`Streak.currentDays`, `ProgressCounter.totalXP`), i per això viu a l'arrel i escolta.
///
/// I **recuperable**: el dia es pot completar sense l'app al davant (des d'un avís o un widget), i
/// llavors `currentDays` ja ha pujat abans que res escolti. Per això el dia celebrat es desa
/// (`streak.lastCelebratedDay`) i la ratxa també es prova en obrir l'app i en tornar-hi.
@Stable
internal class CelebrationQueue(
    private val backend: ViraBackend,
    private val scope: CoroutineScope,
) {
    private val preferences: SharedPreferences get() = backend.preferences
    private val store get() = ViraStore(backend.db)
    private val cards get() = ViraCardCelebrations(preferences)
    private fun days() = ViraDays(ZoneId.systemDefault())

    var streakCelebration by mutableStateOf<StreakCelebrationState?>(null)
        private set
    private var pendingStreak: StreakCelebrationState? = null
    /// La carta de la fita, que surt **en tancar** la de ratxa: el que has fet i el que t'endus.
    var badgeCelebration by mutableStateOf<CardItem?>(null)
        private set
    /// Una carta guanyada per qualsevol altre comptador.
    var cardCelebration by mutableStateOf<CardItem?>(null)
        private set
    var levelCelebration by mutableStateOf<Int?>(null)
        private set
    private var pendingLevel: Int? = null

    /// Quantes pantalles demanen que la ratxa esperi. **Un comptador i no un booleà**: n'hi pot
    /// haver dues alhora, i la primera que acabés alliberaria la retenció de l'altra.
    private var deferralCount = 0
    var tutorialVisible = false
        set(value) {
            if (field == value) return
            field = value
            if (!value) presentAllPending()
        }
    var celebrationsEnabled = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) dropAll(markToday = true)
        }
    var allowsLevel = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                pendingLevel = null
                levelCelebration = null
            }
        }

    var streak: Streak? = null
        private set
    var counter: ProgressCounter? = null
        private set
    /// Les dues dades arriben per camins separats. Les cartes ja guanyades només es poden donar per
    /// vistes quan se saben **totes dues**: sembrar amb la ratxa per arribar les tornaria a treure.
    private var streakLoaded = false
    private var counterLoaded = false
    /// La carta de la fita es busca a la base. Mentrestant, la ratxa és qui la posseeix i la cua
    /// general no la pot ensenyar pel seu compte.
    private var streakLookupPending = false

    private val isDeferred get() = tutorialVisible || deferralCount > 0

    fun defer(deferred: Boolean) {
        deferralCount = maxOf(0, deferralCount + if (deferred) 1 else -1)
        if (!isDeferred) {
            presentPendingStreak()
            presentPendingLevel()
        }
    }

    // Ratxa

    /// La ratxa ha canviat. Cada dia que avança (també el primer, i un reinici que torna a
    /// començar) es celebra, no només als llindars amb carta.
    fun onStreak(value: Streak?) {
        val isFirstLoad = !streakLoaded
        val old = streak?.currentDays
        streak = value
        streakLoaded = true
        if (value?.isVisible != true) {
            pendingStreak = null
            streakCelebration = null
        }
        if (old != null && value != null && old < value.currentDays) enqueueStreak(value.currentDays)
        // La primera lectura és com obrir l'app: el dia pot haver pujat sense ningú escoltant.
        if (isFirstLoad) checkStreakOnAppear()
        presentNextCard()
    }

    /// El dia d'avui que encara no s'ha celebrat aquí, en obrir l'app o en tornar-hi. Mira **el
    /// dia**, no el número: reprendre una ratxa perduda torna a 1, i aquell dia també es celebra.
    fun checkStreakOnAppear() {
        val streak = streak ?: return
        val last = streak.lastActiveDay ?: return
        val days = days()
        if (streak.currentDays > 0 && days.startOfDay(last) == days.startOfDay(ViraTime.now())) {
            enqueueStreak(streak.currentDays)
        }
    }

    /// El segell del dia va aquí i no en ensenyar-la: si l'app mor amb la pantalla oberta, la festa
    /// no torna a la propera obertura.
    private fun enqueueStreak(days: Int) {
        val today = days().startOfDay(ViraTime.now()).epochSecond
        val streak = streak
        if (!celebrationsEnabled || streak?.isVisible != true) {
            markCelebrated(today)
            return
        }
        if (lastCelebratedDay() == today || streakCelebration != null || pendingStreak != null) return
        markCelebrated(today)
        // La frase es tria **aquí**, en l'esdeveniment: rotar-la escriu a les preferències, i fer-ho
        // mentre es dibuixa torna a dibuixar en bucle.
        val message = StreakMessageCatalog(StreakMessageCatalog.Preferences(preferences))
            .advance(StreakMessageCatalog.State.RUNNING, days, hasRecordAhead = streak.bestDays > days)
        val week = StreakWeek.of(streak, days().date(ViraTime.now()), days())
        streakLookupPending = true
        scope.launch {
            val card = try {
                if (ViraReleaseGate.cards) ViraProgress(store).takeStreakMilestone() else null
            } finally {
                streakLookupPending = false
            }
            pendingStreak = StreakCelebrationState(days, week, message, card)
            presentPendingStreak()
            presentNextCard()
        }
    }

    private fun presentPendingStreak() {
        if (!celebrationsEnabled || streak?.isVisible != true) {
            pendingStreak = null
            return
        }
        val pending = pendingStreak ?: return
        if (isDeferred || streakCelebration != null) return
        streakCelebration = pending
        pendingStreak = null
    }

    fun finishStreak() {
        val earned = streakCelebration?.cardKey?.let(CardItem::item)
        streakCelebration = null
        if (earned != null) {
            // Ja s'ensenya aquí: marcada, la comprovació general no la torna a treure tot seguit.
            cards.markAsSeen(earned.key)
            badgeCelebration = earned
        } else {
            presentPendingLevel()
            presentNextCard()
        }
    }

    fun finishBadge() {
        badgeCelebration = null
        presentPendingLevel()
        presentNextCard()
    }

    // Nivell

    /// L'XP puja i creua un nivell encara no celebrat. Es marca **abans** d'ensenyar res: si
    /// l'usuari surt a mig confeti, no torna a sortir.
    fun onCounter(value: ProgressCounter?) {
        val oldXP = counter?.totalXP
        counter = value
        counterLoaded = true
        // El nivell primer i la carta després: totes dues són pantalles completes, i la carta és la
        // que millor espera.
        checkLevel(value, oldXP)
        presentNextCard()
    }

    private fun checkLevel(value: ProgressCounter?, oldXP: Int?) {
        if (value == null || oldXP == null || oldXP >= value.totalXP) return
        val level = ViraLevel.level(value.totalXP)
        if (!allowsLevel) {
            if (level > value.lastCelebratedLevel) markLevel(value, level)
            return
        }
        if (level <= value.lastCelebratedLevel || levelCelebration != null || pendingLevel != null) return
        markLevel(value, level)
        pendingLevel = level
        presentPendingLevel()
    }

    private fun markLevel(counter: ProgressCounter, level: Int) {
        val marked = counter.copy(lastCelebratedLevel = level)
        this.counter = marked
        // Sobre la fila d'ara i no la de la foto: una concessió o una baixada entremig no es pot trepitjar.
        scope.launch {
            backend.db.withTransaction {
                val latest = backend.db.progressCounters().all().firstOrNull() ?: return@withTransaction
                if (latest.lastCelebratedLevel < level) {
                    store.saveDeviceOnly(backend.db.progressCounters(), latest.copy(lastCelebratedLevel = level))
                }
            }
        }
    }

    /// La ratxa guanya si totes dues volen sortir alhora; el nivell surt en tancar-la.
    private fun presentPendingLevel() {
        if (!allowsLevel) {
            pendingLevel = null
            return
        }
        val pending = pendingLevel ?: return
        if (isDeferred || streakCelebration != null || levelCelebration != null ||
            badgeCelebration != null || cardCelebration != null
        ) return
        levelCelebration = pending
        pendingLevel = null
    }

    fun finishLevel() {
        levelCelebration = null
        presentNextCard()
    }

    // Cartes

    /// La primera vegada, el que ja estigui guanyat es dona per vist: una festa per cada carta que
    /// ja es tenia seria el contrari del que vol dir celebrar.
    private fun presentNextCard() {
        if (!ViraReleaseGate.cards || !streakLoaded || !counterLoaded) return
        if (!celebrationsEnabled || !cards.isSeeded) {
            if (counter != null) cards.absorb(counter, streak)
            return
        }
        if (isDeferred || streakLookupPending || pendingStreak != null ||
            streakCelebration != null || badgeCelebration != null ||
            levelCelebration != null || cardCelebration != null
        ) return
        val next = cards.pending(counter, streak).firstOrNull() ?: return
        cards.markAsSeen(next.key)
        cardCelebration = next
    }

    fun finishCard() {
        cardCelebration = null
        presentPendingLevel()
        // Encadenades i no alhora: si un moment n'ha desbloquejat dues, la segona surt ara.
        presentNextCard()
    }

    val ownedCards: Int get() = maxOf(1, CardItem.unlocked(counter, streak).size)

    private fun presentAllPending() {
        presentPendingStreak()
        presentPendingLevel()
        presentNextCard()
    }

    private fun dropAll(markToday: Boolean) {
        pendingStreak = null
        streakCelebration = null
        pendingLevel = null
        levelCelebration = null
        badgeCelebration = null
        cardCelebration = null
        if (markToday) markCelebrated(days().startOfDay(ViraTime.now()).epochSecond)
        if (ViraReleaseGate.cards && counter != null && streakLoaded) cards.absorb(counter, streak)
    }

    private fun lastCelebratedDay() = preferences.getLong(LAST_CELEBRATED_STREAK_DAY, 0)
    private fun markCelebrated(day: Long) = preferences.edit().putLong(LAST_CELEBRATED_STREAK_DAY, day).apply()

    private companion object {
        /// Mateix nom que l'iPhone (`ViraPreferences.lastCelebratedStreakDay`). Aquí, l'inici del dia
        /// en segons.
        const val LAST_CELEBRATED_STREAK_DAY = "streak.lastCelebratedDay"
    }
}

/// El que necessita `StreakCelebration`, triat en encuar-la.
internal data class StreakCelebrationState(
    val days: Int,
    val week: StreakWeek,
    val message: Int,
    val cardKey: String?,
)

/// Escolta les dades i ensenya la celebració que toqui per sobre de `content`. S'aplica un sol
/// cop, a l'arrel. També hi posa la ratxa al dia en obrir l'app i en tornar-hi.
@Composable
internal fun ViraCelebrations(queue: CelebrationQueue) {
    val backend = LocalViraBackend.current
    val prefs = backend.preferences
    val celebrations by rememberPreference(prefs, ViraPreferences.CELEBRATIONS_ENABLED, true)
    val progressVisible by rememberPreference(prefs, PROGRESS_VISIBLE, true)
    val soundMode by rememberPreference(prefs, SOUND_MODE, ViraSoundMode.ALL.raw)
    queue.celebrationsEnabled = celebrations
    // Sense celebracions o amb el progrés amagat, el nivell no surt (`ViraCelebrationPolicy`).
    queue.allowsLevel = celebrations && progressVisible

    // Directament del flux i no d'un estat amb valor inicial: el `null` d'abans de la primera
    // lectura es confondria amb «no n'hi ha cap» i les cartes se sembrarien a mitges.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        backend.db.streaks().observe().flowWithLifecycle(lifecycle).collect { queue.onStreak(it) }
    }
    LaunchedEffect(lifecycle) {
        backend.db.progressCounters().observe().flowWithLifecycle(lifecycle).collect { queue.onCounter(it) }
    }

    // Arrencar de zero i tornar de segon pla: el dia es pot haver completat en qualsevol dels dos.
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        scope.launch {
            ViraProgress(ViraStore(backend.db)).reconcileOnLaunch()
            queue.checkStreakOnAppear()
        }
    }

    queue.streakCelebration?.let { celebration ->
        ViraFullScreen(onDismiss = queue::finishStreak) {
            StreakCelebration(
                streakDays = celebration.days,
                week = celebration.week,
                message = stringResource(celebration.message),
                onContinue = queue::finishStreak,
                cardKey = celebration.cardKey,
                soundEnabled = ViraSoundMode.from(soundMode) != ViraSoundMode.OFF,
            )
        }
    }
    queue.badgeCelebration?.let { card ->
        ViraFullScreen(onDismiss = queue::finishBadge) {
            CardUnlockedScreen(card, queue.ownedCards, onContinue = queue::finishBadge)
        }
    }
    queue.cardCelebration?.let { card ->
        ViraFullScreen(onDismiss = queue::finishCard) {
            CardUnlockedScreen(card, queue.ownedCards, onContinue = queue::finishCard)
        }
    }
    queue.levelCelebration?.let { level ->
        ViraFullScreen(onDismiss = queue::finishLevel) {
            LevelUpCelebration(level, totalXP = queue.counter?.totalXP ?: 0, onContinue = queue::finishLevel)
        }
    }
}

/// Una pantalla completa per sobre de tot (`fullScreenCover`). Enrere la tanca com «Continua».
@Composable
internal fun ViraFullScreen(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        content = content,
    )
}

private const val PROGRESS_VISIBLE = "settings.progressVisible"
private const val SOUND_MODE = "settings.soundMode"
