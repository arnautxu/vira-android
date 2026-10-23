package cat.vira.android.features.progress.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.EmotionsSection
import cat.vira.android.app.PlanSection
import cat.vira.android.app.ProgressDestination
import cat.vira.android.app.ViraDestination
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTabTitle
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.entry.ViraTutorialTarget
import cat.vira.android.features.entry.viraTutorialTarget
import cat.vira.android.features.progress.logic.ChallengeItem
import cat.vira.android.features.progress.logic.ViraLevel

/// El repte de la portada amb el seu estat al cicle actual.
internal data class FeaturedChallenge(val item: ChallengeItem, val progress: Int, val claimed: Boolean)

/// Tot el que dibuixa el Perfil, ja llegit de les dades i dels ajustos.
internal data class ProfileUi(
    val nickname: String = "",
    val counter: ProgressCounter? = null,
    val streak: Streak? = null,
    /// «Mostrar progrés»: sense, marxen el nivell i la frase que el diu.
    val progressVisible: Boolean = true,
    /// «Mostrar botiga»: sense, marxen «Vesteix-lo» i la fitxa de la Botiga.
    val shopVisible: Boolean = true,
    val challengesEnabled: Boolean = true,
    val featured: FeaturedChallenge? = null,
    val cardsAvailable: Boolean = false,
    val friendsAvailable: Boolean = false,
)

/// La pestanya Viri: Perfil (`ProgressTabView`). Tres zones: qui ets, el que has fet i on pots
/// anar.
///
/// **La ratxa mai és el component central** (`VIRI_AND_GAMIFICATION.md` §2.3.1 punt 5): primer es
/// veuen els acumulats, que no baixen mai, i la ratxa s'obre des de la fitxa de millor ratxa. Qui
/// obre aquesta pantalla un dia dolent ha de trobar primer el que ha fet.
@Composable
internal fun ProfileContent(
    ui: ProfileUi,
    onNicknameCommit: (String) -> Unit,
    onOpen: (ProgressDestination) -> Unit,
    onNavigate: (ViraDestination) -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Rewards)
            // Tocar un punt quiet de la pantalla també desa el malnom (perd el focus).
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .verticalScroll(rememberScrollState()),
    ) {
        TitleRow(ui.shopVisible) { onOpen(ProgressDestination.Shop) }
        Column(
            Modifier.padding(horizontal = ViraSpacing.screenMargin).padding(top = ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xl),
        ) {
            ProfileHeader(
                nickname = ui.nickname,
                level = ViraLevel.progress(ui.counter?.totalXP ?: 0),
                progressVisible = ui.progressVisible,
                onNicknameCommit = onNicknameCommit,
                onCustomize = onCustomize,
            )
            WhatYouHaveDone(ui.counter, ui.streak, onOpenStreak = { onOpen(ProgressDestination.Streak) }, onNavigate = onNavigate)
            if (ui.challengesEnabled && ui.featured != null) {
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                    ProgressSectionHeader(stringResource(R.string.profile_challenge_today))
                    FeaturedChallengeCard(ui.featured, onClick = { onOpen(ProgressDestination.Challenges) })
                }
            }
            Destinations(ui, onOpen)
        }
        Spacer(Modifier.height(viraTabBarClearance()))
    }
}

/// El títol a l'esquerra i «Vesteix-lo» a la dreta, que porta a la Botiga, on es posa una skin.
@Composable
private fun TitleRow(shopVisible: Boolean, onDress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.screenMargin).heightIn(min = ViraTouch.minimum),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Text(
                stringResource(R.string.tab_viri_short),
                style = ViraFont.largeTitle,
                color = ViraColor.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        } else {
            ViraTabTitle(stringResource(R.string.tab_viri_short), Modifier.weight(1f))
        }
        if (shopVisible) {
            ViraActionButton(
                text = stringResource(R.string.profile_dress),
                onClick = onDress,
                hierarchy = ViraActionHierarchy.Textual,
                area = ViraArea.Rewards,
            )
        }
    }
}

// Zona 2 · El que has fet

private data class CountTile(
    @StringRes val title: Int,
    val value: Int,
    @DrawableRes val pictogram: Int,
    val area: ViraArea,
    val action: (() -> Unit)?,
)

/// Les xifres que no baixen mai, en graella de 2×2: coses fetes, no puntuació (l'XP ja és a la
/// barra de nivell). Sessions de focus no té pantalla de detall i no es toca.
@Composable
private fun WhatYouHaveDone(
    counter: ProgressCounter?,
    streak: Streak?,
    onOpenStreak: () -> Unit,
    onNavigate: (ViraDestination) -> Unit,
) {
    val tiles = buildList {
        add(CountTile(R.string.progress_focus_sessions, counter?.focusSessionsCount ?: 0, R.drawable.picto_pomodoro, ViraArea.Focus, null))
        add(CountTile(R.string.progress_habits_done, counter?.habitsCompletedCount ?: 0, R.drawable.picto_habits, ViraArea.Plan) {
            onNavigate(ViraDestination.Plan(PlanSection.Habits))
        })
        add(CountTile(R.string.progress_checkins, counter?.emotionCheckinsCount ?: 0, R.drawable.picto_journal, ViraArea.Emotions) {
            onNavigate(ViraDestination.Emotions(EmotionsSection.History))
        })
        // Amb la ratxa amagada, tampoc el rècord.
        if (streak != null && streak.isVisible) {
            add(CountTile(R.string.profile_overview_best_streak, streak.bestDays, R.drawable.picto_flame, ViraArea.Today, onOpenStreak))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        ProgressSectionHeader(stringResource(R.string.progress_accumulated))
        ViraTileGrid(tiles) { tile, cellModifier ->
            val target = if (tile.pictogram == R.drawable.picto_flame) Modifier.viraTutorialTarget(ViraTutorialTarget.Streak) else Modifier
            CountTileView(tile, cellModifier.then(target))
        }
    }
}

/// La fitxa blanca d'un comptador (`ViraTile` `.plain`) amb la xifra gran del color de l'àrea.
@Composable
private fun CountTileView(tile: CountTile, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val contrast = LocalViraEnvironment.current.increasedContrast
    val shadowTint = ViraColor.textPrimary.copy(alpha = 0.05f)
    Box(
        modifier
            .shadow(3.dp, shape, ambientColor = shadowTint, spotColor = shadowTint)
            .background(ViraColor.surfaceElevated, shape)
            .then(if (contrast) Modifier.border(2.dp, ViraColor.textPrimary, shape) else Modifier)
            .then(if (tile.action != null) Modifier.viraPressable(onClick = tile.action) else Modifier)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(ViraSpacing.s), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            ViraPictogram(tile.pictogram, size = 48.dp, scalesWithText = true, modifier = Modifier.padding(bottom = ViraSpacing.xxs))
            Text(tile.value.formatted(), style = ViraFont.largeTitle, color = ViraColor.accent(tile.area))
            Text(stringResource(tile.title), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        if (tile.action != null) {
            Chevron(
                ChevronDirection.Right,
                ViraColor.textSecondary,
                size = 12.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(ViraSpacing.s),
            )
        }
    }
}

// Repte destacat

/// El repte de la portada (`FeaturedChallengeCard`): targeta groga, text `onCard`.
@Composable
private fun FeaturedChallengeCard(featured: FeaturedChallenge, onClick: () -> Unit) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val item = featured.item
    val shape = RoundedCornerShape(ViraRadius.l)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ViraTouch.comfortable)
            .background(ViraGradient.card(ViraArea.Rewards), shape)
            .viraPressable(onClick = onClick)
            .padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        ViraPictogram(item.pictogram, size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(stringResource(R.string.challenge_featured_eyebrow), style = ViraFont.caption, color = ink)
            Text(stringResource(item.title), style = ViraFont.headline, color = ink)
            val status = when {
                featured.claimed -> stringResource(R.string.challenge_claimed)
                featured.progress >= item.threshold -> stringResource(R.string.challenge_claim)
                else -> stringResource(R.string.challenge_progress, minOf(featured.progress, item.threshold), item.threshold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                if (!featured.claimed && featured.progress >= item.threshold) {
                    ViraPictogram(R.drawable.picto_game_gift, size = 18.dp, scalesWithText = true)
                }
                Text(status, style = ViraFont.footnote, color = ink)
            }
        }
        Chevron(ChevronDirection.Right, ink)
    }
}

// Zona 3 · On pots anar

private data class DestinationItem(
    val destination: ProgressDestination,
    val area: ViraArea,
    @DrawableRes val pictogram: Int,
    @StringRes val title: Int,
    @StringRes val detail: Int,
)

/// Les portes que surten d'aquí, en graella de fitxes de color: cada porta porta el color de la seva
/// família i es reconeix abans de llegir-la. El saldo no hi torna: ja és a la capçalera.
@Composable
private fun Destinations(ui: ProfileUi, onOpen: (ProgressDestination) -> Unit) {
    val items = buildList {
        if (ui.shopVisible) add(DestinationItem(ProgressDestination.Shop, ViraArea.Today, R.drawable.picto_game_chest, R.string.shop_title, R.string.profile_destination_shop_detail))
        if (ui.challengesEnabled) add(DestinationItem(ProgressDestination.Challenges, ViraArea.Rewards, R.drawable.picto_game_challenge, R.string.challenge_title, R.string.profile_destination_challenges_detail))
        add(DestinationItem(ProgressDestination.PersonalRewards, ViraArea.Emotions, R.drawable.picto_game_gift, R.string.rewards_personal_title, R.string.profile_destination_rewards_detail))
        if (ui.cardsAvailable) add(DestinationItem(ProgressDestination.Cards, ViraArea.Plan, R.drawable.picto_cards, R.string.profile_cards_title, R.string.profile_destination_cards_detail))
        if (ui.friendsAvailable) add(DestinationItem(ProgressDestination.Friends, ViraArea.Focus, R.drawable.picto_habit_friends, R.string.friends_title, R.string.profile_destination_friends_detail))
    }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        ProgressSectionHeader(stringResource(R.string.profile_destinations_title))
        ViraTileGrid(items) { item, cellModifier ->
            ViraTile(
                area = item.area,
                pictogram = item.pictogram,
                title = stringResource(item.title),
                subtitle = stringResource(item.detail),
                modifier = cellModifier
                    .viraPressable { onOpen(item.destination) }
                    .semantics(mergeDescendants = true) {},
            )
        }
    }
}
