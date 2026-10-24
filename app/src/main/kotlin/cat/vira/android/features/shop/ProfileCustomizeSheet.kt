package cat.vira.android.features.shop

import androidx.compose.ui.platform.LocalContext
import cat.vira.android.core.sound.ViraSound
import cat.vira.android.core.sound.ViraHaptics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.launch

/// Tota la personalització en un sol lloc (`ProfileCustomizeSheet`): malnom, skin posada i badges
/// penjats. No ven res: aquí només es tria entre el que ja tens, i el que no tens es veu amb el
/// motiu (requisit o preu), mai amagat.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCustomizeSheet(onDismiss: () -> Unit) {
    val data = rememberShopData()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val counter by data.counter.collectAsStateWithLifecycle(null)
    val streak by data.streak.collectAsStateWithLifecycle(null)
    val profile by data.profile.collectAsStateWithLifecycle(null)
    val skins by data.skins.collectAsStateWithLifecycle(emptyList())
    val badges by data.badges.collectAsStateWithLifecycle(emptyList())
    val tallies = rememberCardTallies(data.preferences)

    fun saveNickname(name: String) = scope.launch {
        val current = data.store.profile()
        val trimmed = name.trim()
        if (current.displayName != trimmed) data.store.save(data.db.profiles(), current.copy(displayName = trimmed))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.backgroundGrouped,
    ) {
        // La primera lectura del perfil arriba un instant tard: el camp neix quan ja hi és.
        profile?.let { loaded ->
            ProfileCustomizeContent(
                nickname = loaded.displayName,
                ownedSkins = skins.mapNotNull { SkinItem.item(it.key) },
                equippedKey = skins.firstOrNull { it.isEquipped }?.key,
                pinnedKeys = loaded.pinnedBadgeKeys,
                ownedBadgeKeys = badges.map { it.key }.toSet(),
                counter = counter,
                streak = streak,
                tallies = tallies,
                onDone = { name -> saveNickname(name); onDismiss() },
                onSaveNickname = { saveNickname(it) },
                onEquip = { key ->
                    ViraHaptics.play(context, ViraHaptics.Feedback.Done)
                    ViraSound.play(context, ViraSound.Cue.EQUIP)
                    scope.launch { data.skinsLogic.equip(key) }
                },
                onToggleBadge = { key -> scope.launch { data.badgesLogic.toggle(key) } },
                modifier = Modifier.imePadding(),
            )
        }
    }
}

/// La fulla sense estat, per a les captures.
@Composable
fun ProfileCustomizeContent(
    nickname: String,
    ownedSkins: List<SkinItem>,
    equippedKey: String?,
    pinnedKeys: List<String>,
    ownedBadgeKeys: Set<String>,
    counter: ProgressCounter?,
    streak: Streak?,
    onDone: (String) -> Unit,
    onSaveNickname: (String) -> Unit,
    onEquip: (String?) -> Unit,
    onToggleBadge: (String) -> Unit,
    modifier: Modifier = Modifier,
    tallies: Tallies = Tallies.EMPTY,
) {
    var draft by rememberSaveable { mutableStateOf(nickname) }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin).padding(bottom = ViraSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f))
            Text(
                stringResource(R.string.profile_customize), style = ViraFont.headline, color = ViraColor.textPrimary,
                textAlign = TextAlign.Center, modifier = Modifier.weight(3f).semantics { heading() },
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier.heightIn(min = ViraTouch.minimum).viraPressable { onDone(draft) }.padding(horizontal = ViraSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.action_done), style = ViraFont.headline, color = ViraColor.accent(ViraArea.Today)) }
            }
        }
        Section(stringResource(R.string.profile_nickname), R.drawable.picto_profile) {
            val placeholder = stringResource(R.string.profile_nickname_placeholder)
            val shape = RoundedCornerShape(ViraRadius.m)
            Box(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.comfortable)
                    .background(ViraColor.surface(ViraArea.Rewards), shape)
                    .border(1.dp, ViraColor.onSurface(ViraArea.Rewards).copy(alpha = 0.16f), shape)
                    .padding(ViraSpacing.m),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
                    cursorBrush = SolidColor(ViraColor.accent(ViraArea.Today)),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSaveNickname(draft) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text(placeholder, style = ViraFont.body, color = ViraColor.textSecondary)
                        inner()
                    },
                )
            }
        }
        // Només les que ja tens, i el Viri de sempre, que sempre hi és i és gratuït.
        Section(stringResource(R.string.profile_customize_skin), R.drawable.picto_profile) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                SkinChoice(null, equippedKey == null) { onEquip(null) }
                ownedSkins.forEach { skin -> SkinChoice(skin, equippedKey == skin.key) { onEquip(skin.key) } }
            }
            if (ownedSkins.isEmpty()) {
                Text(stringResource(R.string.profile_customize_skin_empty), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        Section(stringResource(R.string.profile_customize_badges), R.drawable.picto_game_badge) {
            Text(
                stringResource(R.string.profile_customize_badges_hint, ViraBadges.SLOTS.viraFormatted()),
                style = ViraFont.footnote, color = ViraColor.textSecondary,
            )
            BadgePickerGrid(counter, streak, pinnedKeys, ownedBadgeKeys, onToggleBadge, tallies = tallies)
        }
    }
}

@Composable
private fun Section(title: String, pictogram: Int, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, shape).border(1.dp, ViraColor.separator, shape).padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
            ViraPictogram(pictogram, size = 24.dp, scalesWithText = true)
            Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
        }
        content()
    }
}

@Composable
private fun SkinChoice(item: SkinItem?, isEquipped: Boolean, onClick: () -> Unit) {
    val name = item?.let { stringResource(it.nameRes) } ?: stringResource(R.string.shop_default)
    val state = stringResource(if (isEquipped) R.string.shop_equipped else R.string.shop_wear)
    val shape = RoundedCornerShape(ViraRadius.m)
    Column(
        Modifier.width(112.dp)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = name; stateDescription = state; selected = isEquipped; role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            SkinPortrait(item, Modifier.size(104.dp).clip(shape)
                .border(if (isEquipped) 3.dp else 1.dp, if (isEquipped) ViraColor.accent(ViraArea.Rewards) else ViraColor.separator, shape))
            if (isEquipped) {
                Box(Modifier.padding(ViraSpacing.xs).size(28.dp).background(ViraGradient.accent(ViraArea.Rewards), CircleShape), contentAlignment = Alignment.Center) {
                    ShopSymbolIcon(ShopSymbol.Check, ViraColor.onAccent, size = 14.dp)
                }
            }
        }
        Text(name, style = ViraFont.footnote, color = ViraColor.textPrimary, textAlign = TextAlign.Center)
    }
}

/// El que pots penjar al banner (`BadgePickerGrid`): **només el que ja és teu**, les cartes guanyades i
/// les insígnies que vas comprar (des del 2026-10-22 la Botiga ja no en ven). L'estat mai és només la vora: «Penjat» també
/// en text.
@Composable
fun BadgePickerGrid(
    counter: ProgressCounter?,
    streak: Streak?,
    pinnedKeys: List<String>,
    ownedKeys: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    tallies: Tallies = Tallies.EMPTY,
) {
    fun available(item: BadgeItem) =
        if (item.isEarned) CardItem.item(item.key)?.isUnlocked(counter, streak, tallies) ?: false else item.key in ownedKeys
    val visible = BadgeItem.catalog.filter(::available)
    if (visible.isEmpty()) {
        Text(stringResource(R.string.profile_badges_none), style = ViraFont.callout, color = ViraColor.textSecondary, modifier = modifier.fillMaxWidth())
        return
    }
    val env = LocalViraEnvironment.current
    val columns = if (env.isAccessibilitySize) 1 else 2
    ShopGrid(visible, columns, modifier, spacing = ViraSpacing.rowGap) { item, cellModifier ->
        BadgePickerTile(item, available(item), item.key in pinnedKeys, { onToggle(item.key) }, cellModifier)
    }
}

@Composable
private fun BadgePickerTile(item: BadgeItem, available: Boolean, pinned: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.m)
    val name = stringResource(item.nameRes)
    val stateRes = when {
        pinned -> R.string.profile_badges_pinned
        !available -> R.string.profile_card_locked
        item.isEarned -> R.string.profile_card_unlocked
        else -> R.string.shop_owned
    }
    val state = stringResource(stateRes)
    val accent = ViraColor.accent(ViraArea.Rewards)
    Column(
        modifier.defaultMinSize(minHeight = 176.dp)
            .background(ViraColor.surfaceElevated, shape)
            .border(if (pinned) 2.dp else 1.dp, if (pinned) accent else ViraColor.separator, shape)
            .viraPressable(enabled = available, onClick = onClick)
            .clearAndSetSemantics { contentDescription = name; stateDescription = state; selected = pinned; role = Role.Button }
            .padding(ViraSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        BadgeChip(item, size = 64.dp, isAvailable = available)
        Text(name, style = ViraFont.callout, color = if (available) ViraColor.textPrimary else ViraColor.textSecondary, textAlign = TextAlign.Center)
        val card = CardItem.item(item.key)
        when {
            pinned -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                ShopSymbolIcon(ShopSymbol.Check, accent, size = 14.dp)
                Text(stringResource(R.string.profile_badges_pinned), style = ViraFont.footnote, color = accent)
            }
            available && card != null -> Text(
                stringResource(R.string.profile_badges_earned_rank, stringResource(card.rarity.labelRes)),
                style = ViraFont.footnote, color = accent, textAlign = TextAlign.Center,
            )
            available -> Text(stringResource(R.string.profile_badges_pin), style = ViraFont.callout, color = ViraColor.textSecondary)
            item.price != null -> Text(stringResource(R.string.shop_price, item.price.viraFormatted()), style = ViraFont.footnote.tabular(), color = ViraColor.textSecondary)
        }
    }
}
