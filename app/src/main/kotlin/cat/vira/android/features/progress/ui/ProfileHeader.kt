package cat.vira.android.features.progress.ui

import cat.vira.android.core.sound.ViraSound
import cat.vira.android.core.sound.ViraHaptics
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraLevelProgress
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.features.progress.ProgressSlots
import cat.vira.android.features.progress.RewardsProgressBar
import cat.vira.android.features.progress.ViriBubble
import cat.vira.android.features.progress.logic.ViraLevel
import kotlin.math.roundToInt

/// Mòdul protagonista de la pestanya Viri (`ProfileHeaderView`, `DESIGN_RULES.md` §3): Viri amb la
/// skin posada, la bombolla que li surt, el malnom, el nivell i el prestatge de cartes. **Cap dada
/// real** (`PRIVACY_AND_SAFETY.md`): només el que l'usuari tria.
///
/// Targeta groga de recompenses amb el text `onCard`; el nivell en text **i** barra (§4). La
/// bombolla és l'única aparició expressiva de la pantalla i el text no va mai sobre la
/// il·lustració. Sense progrés visible (Ajustos) marxen el nivell i la frase que el diu.
@Composable
internal fun ProfileHeader(
    nickname: String,
    level: ViraLevelProgress,
    progressVisible: Boolean,
    onNicknameCommit: (String) -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val accent = ViraColor.accent(ViraArea.Rewards)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = accent.copy(alpha = 0.18f), spotColor = accent.copy(alpha = 0.18f))
            .clip(shape)
            .background(ViraGradient.card(ViraArea.Rewards)),
    ) {
        // 4:3: el retall és de dalt i de baix, que és on les skins tenen decorat.
        // Tocar Viri el fa dir alguna cosa: tres veuetes que roten. És un detall, no una acció: no
        // porta cap informació, així que TalkBack no l'anuncia.
        val context = LocalContext.current
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).pointerInput(Unit) {
                detectTapGestures {
                    ViraHaptics.play(context, ViraHaptics.Feedback.Tap)
                    ViraSound.playViri(context)
                }
            },
        ) {
            ProgressSlots.Portrait(Modifier.fillMaxSize())
        }
        Column(
            Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.m, bottom = ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ViriBubble(
                if (progressVisible) stringResource(R.string.profile_viri_bubble_level, level.level)
                else stringResource(R.string.profile_viri_bubble_plain),
                ViraArea.Rewards,
                Modifier.overlapTop(ViraSpacing.xxl),
            )
            NicknameRow(nickname, level.level, progressVisible, onNicknameCommit)
            if (progressVisible) LevelProgress(level)
            ProgressSlots.Shelf(onCustomize, ViraColor.onCard(ViraArea.Rewards), Modifier)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NicknameRow(nickname: String, level: Int, progressVisible: Boolean, onCommit: (String) -> Unit) {
    // A l'iPhone és un `ViewThatFits`: al costat si hi cap, a sota si no.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        NicknameField(nickname, onCommit, Modifier.weight(1f).widthIn(min = 160.dp))
        if (progressVisible) {
            ViraTag(
                text = stringResource(R.string.level_title, level),
                pictogram = ViraLevel.frame(level)?.pictogram ?: R.drawable.picto_level_up,
            )
        }
    }
}

/// L'únic camp editable de la pantalla. Es desa en perdre el focus o en prémer «Fet»: tocar un
/// punt quiet de la pantalla també el desa (ho fa `ProfileContent`).
@Composable
private fun NicknameField(nickname: String, onCommit: (String) -> Unit, modifier: Modifier = Modifier) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val focusManager = LocalFocusManager.current
    var draft by rememberSaveable(nickname) { mutableStateOf(nickname) }
    var focused by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.profile_nickname)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
        Text(label, style = ViraFont.footnote, color = ink)
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            textStyle = ViraFont.title.copy(color = ink),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ViraTouch.minimum)
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) {
                        val trimmed = draft.trim()
                        draft = trimmed
                        onCommit(trimmed)
                    }
                    focused = state.isFocused
                },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) {
                        Text(stringResource(R.string.profile_nickname_placeholder), style = ViraFont.title, color = ink.copy(alpha = 0.75f))
                    }
                    field()
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelProgress(level: ViraLevelProgress) {
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val spoken = stringResource(R.string.level_accessibility, level.level, (level.fraction * 100).roundToInt())
    Column(
        Modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
        ) {
            Text(stringResource(R.string.profile_level_next, level.level + 1), style = ViraFont.footnoteEmphasis, color = ink)
            Text(
                stringResource(R.string.level_progress, level.currentLevelXP, level.currentLevelXP + level.xpToNext),
                style = ViraFont.footnoteEmphasis,
                color = ink,
            )
        }
        RewardsProgressBar(level.fraction, onCard = true)
    }
}
