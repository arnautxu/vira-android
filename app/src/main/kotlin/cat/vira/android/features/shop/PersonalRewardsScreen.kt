package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.launch

/// «Els meus premis» (`PersonalRewardsView`, `USER_FLOWS.md` F20): l'usuari es posa les seves
/// pròpies recompenses i se les dona amb les ViriCoins que ja ha guanyat. Sense contingut de
/// l'app ni economia pròpia: un títol, un cost i un botó.
///
/// Bescanviar és una despesa, no una acció destructiva: un diàleg lleuger abans i la celebració
/// de Viri en confirmar. Arxivar és reversible i no demana confirmació: mantenir premut o l'acció
/// de TalkBack.
@Composable
fun PersonalRewardsScreen(onBack: (() -> Unit)? = null) {
    val data = rememberShopData()
    val scope = rememberCoroutineScope()
    val counter by data.counter.collectAsStateWithLifecycle(null)
    val all by data.personalRewards.collectAsStateWithLifecycle(emptyList())
    val rewards = all.filter { !it.isArchived }.sortedBy { it.createdAt }

    var isCreating by remember { mutableStateOf(false) }
    var pendingRedeem by remember { mutableStateOf<PersonalReward?>(null) }
    // Es guarda el títol i no el premi: arxivar-lo mentrestant no ha de buidar la celebració.
    var celebrating by remember { mutableStateOf<String?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    val balance = counter?.coinBalance ?: 0

    PersonalRewardsContent(
        rewards = rewards,
        balance = balance,
        saveFailed = saveFailed,
        onBack = onBack,
        onCreate = { isCreating = true },
        onRedeem = { pendingRedeem = it },
        onArchive = { reward -> scope.launch { saveFailed = runCatching { data.personalRewardsLogic.archive(reward) }.isFailure } },
    )

    if (isCreating) {
        PersonalRewardEditor(
            onSave = { title, cost ->
                isCreating = false
                scope.launch { saveFailed = runCatching { data.personalRewardsLogic.create(title, cost) }.isFailure }
            },
            onDismiss = { isCreating = false },
        )
    }
    pendingRedeem?.let { reward ->
        AlertDialog(
            onDismissRequest = { pendingRedeem = null },
            containerColor = ViraColor.surfaceElevated,
            titleContentColor = ViraColor.textPrimary,
            textContentColor = ViraColor.textSecondary,
            title = { Text(stringResource(R.string.rewards_personal_confirm_title, reward.title), style = ViraFont.headline) },
            text = { Text(stringResource(R.string.rewards_personal_confirm_message, reward.cost.viraFormatted()), style = ViraFont.body) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRedeem = null
                    scope.launch {
                        val result = runCatching { data.personalRewardsLogic.redeem(reward) }
                        saveFailed = result.isFailure
                        if (result.getOrDefault(false)) celebrating = reward.title
                    }
                }) { Text(stringResource(R.string.rewards_personal_redeem), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Today)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRedeem = null }) {
                    Text(stringResource(R.string.action_cancel), style = ViraFont.body, color = ViraColor.accent(ViraArea.Today))
                }
            },
        )
    }
    celebrating?.let { title ->
        Dialog(
            onDismissRequest = { celebrating = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            RewardCelebrationScreen(title, onContinue = { celebrating = null })
        }
    }
}

/// «Els meus premis» sense estat. Les captures la fan servir directament.
@Composable
fun PersonalRewardsContent(
    rewards: List<PersonalReward>,
    balance: Int,
    onCreate: () -> Unit,
    onRedeem: (PersonalReward) -> Unit,
    onArchive: (PersonalReward) -> Unit,
    modifier: Modifier = Modifier,
    saveFailed: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val affordable = rewards.count { balance >= it.cost }
    // Dues columnes de fitxes; a mides d'accessibilitat, una sola perquè el nom no es talli.
    val columns = if (isLarge) 1 else 2
    Box(modifier.fillMaxSize().viraSectionBackground(ViraArea.Rewards)) {
        Column(Modifier.fillMaxSize()) {
            ShopNavigationBar(null, onBack, area = ViraArea.Rewards) { ShopWallet(balance) }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                TitleRow(showsNew = rewards.isNotEmpty(), isLarge = isLarge, onCreate = onCreate)
                if (saveFailed) ShopSaveError()
                Hero(rewards, balance, affordable, isLarge, onCreate)
                if (rewards.isNotEmpty()) {
                    Text(
                        stringResource(R.string.rewards_personal_section),
                        style = ViraFont.headline, color = ViraColor.textPrimary,
                        modifier = Modifier.padding(top = ViraSpacing.xs).padding(horizontal = ViraSpacing.xxs)
                            .semantics { heading() },
                    )
                    val cells: List<PersonalReward?> = rewards + listOf(null)
                    ShopGrid(cells, columns, spacing = ViraSpacing.s) { reward, cellModifier ->
                        if (reward == null) {
                            PersonalRewardAddTile(onCreate, cellModifier)
                        } else {
                            ArchivableTile(reward, balance, { onRedeem(reward) }, { onArchive(reward) }, cellModifier)
                        }
                    }
                    Text(
                        stringResource(R.string.rewards_personal_archive_hint),
                        style = ViraFont.footnote, color = ViraColor.textSecondary,
                        modifier = Modifier.padding(horizontal = ViraSpacing.xs),
                    )
                }
                Spacer(Modifier.height(viraTabBarClearance()))
            }
        }
    }
}

/// Títol gran groc i, quan ja hi ha premis, «+ Nou» a la mateixa fila. Sense premis el botó és
/// dins de la targeta i no cal repetir-lo.
@Composable
private fun TitleRow(showsNew: Boolean, isLarge: Boolean, onCreate: () -> Unit) {
    val title = @Composable { m: Modifier -> ViraPageTitle(stringResource(R.string.rewards_personal_title), ViraArea.Rewards, m) }
    val button = @Composable {
        if (showsNew) {
            val label = stringResource(R.string.rewards_personal_new)
            Row(
                Modifier.defaultMinSize(minHeight = ViraTouch.minimum)
                    .background(ViraColor.surfaceElevated, CircleShape)
                    .viraPressable { onCreate() }
                    .semantics(mergeDescendants = true) { contentDescription = label }
                    .padding(horizontal = ViraSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                val ink = ViraColor.onSurface(ViraArea.Rewards)
                ShopSymbolIcon(ShopSymbol.Plus, ink, size = 16.dp)
                Text(stringResource(R.string.rewards_personal_new_short), style = ViraFont.headline, color = ink)
            }
        }
    }
    if (isLarge) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { title(Modifier); button() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            title(Modifier.weight(1f)); button()
        }
    }
}

@Composable
private fun Hero(rewards: List<PersonalReward>, balance: Int, affordable: Int, isLarge: Boolean, onCreate: () -> Unit) {
    val message = when {
        rewards.isEmpty() -> stringResource(R.string.rewards_personal_viri_empty)
        affordable > 0 -> stringResource(R.string.rewards_personal_viri_ready, affordable)
        else -> stringResource(R.string.rewards_personal_viri_saving)
    }
    RewardsSceneCard(R.drawable.viri_chill, message) {
        val ink = ViraColor.onCard(ViraArea.Rewards)
        val balanceText = balance.viraFormatted()
        val caption = stringResource(R.string.rewards_personal_balance)
        val summary = Modifier.semantics(mergeDescendants = true) { }
        if (isLarge) {
            Column(summary, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(balanceText, style = ViraFont.largeTitle.tabular(), color = ink, maxLines = 1)
                Text(caption, style = ViraFont.callout, color = ink)
            }
        } else {
            Row(summary, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                Text(balanceText, style = ViraFont.largeTitle.tabular(), color = ink, maxLines = 1, modifier = Modifier.alignByBaseline())
                Text(caption, style = ViraFont.callout, color = ink, modifier = Modifier.alignByBaseline())
            }
        }
        if (rewards.isEmpty()) {
            Text(stringResource(R.string.rewards_personal_empty_hint), style = ViraFont.body, color = ink)
            ViraActionButton("+ " + stringResource(R.string.rewards_personal_new), onCreate, Modifier.fillMaxWidth(), area = ViraArea.Today)
        } else {
            RewardsPill(stringResource(R.string.rewards_personal_ready_count, affordable, rewards.size))
        }
    }
}

/// Una fitxa amb arxivar a mà: mantenir premut, i l'acció amb nom per a TalkBack.
@Composable
private fun ArchivableTile(reward: PersonalReward, balance: Int, onRedeem: () -> Unit, onArchive: () -> Unit, modifier: Modifier) {
    val archiveLabel = stringResource(R.string.rewards_personal_archive)
    PersonalRewardTile(
        reward, balance, onRedeem,
        modifier
            .pointerInput(reward.id) { detectTapGestures(onLongPress = { onArchive() }) }
            .semantics { customActions = listOf(CustomAccessibilityAction(archiveLabel) { onArchive(); true }) },
    )
}
