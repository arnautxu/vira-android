package cat.vira.android.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// El camp del primer pas: la icona, que obre la fila d'icones, i el títol en gran. El retorn del
/// teclat és «Següent» i prou: no desa. El teclat no s'obre sol (Arnau, 2026-10-19: tapava mitja
/// pantalla); un toc al camp l'obre.
@Composable
fun AddTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    iconName: String,
    isPickingIcon: Boolean,
    onToggleIcon: () -> Unit,
    onNext: () -> Unit,
    area: ViraArea = ViraArea.Plan,
    focusRequester: FocusRequester? = null,
    /// El mini llapis sobre la icona, perquè s'entengui que es pot canviar (Arnau, 2026-10-24).
    showsPencil: Boolean = false,
    /// El que va al final del camp: el micròfon de la cara Tasca.
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val iconLabel = stringResource(R.string.add_task_icon_a11y)
    Row(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, shape)
            .border(2.dp, ViraColor.accent(area), shape)
            .padding(ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Box(
            Modifier
                .size(ViraTouch.minimum + 4.dp)
                .background(ViraColor.surface(area), RoundedCornerShape(ViraRadius.m))
                .viraPressable(role = Role.Button, onClick = onToggleIcon)
                .semantics { contentDescription = iconLabel; selected = isPickingIcon },
            contentAlignment = Alignment.Center,
        ) {
            ViraPictogram(iconName, size = 34.dp)
            if (showsPencil) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(5.dp, 5.dp)
                        .size(20.dp)
                        .background(ViraColor.accent(area), CircleShape)
                        .border(2.dp, ViraColor.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ViraColor.onAccent, modifier = Modifier.size(11.dp))
                }
            }
        }
        AddTextField(
            value, onValueChange, placeholder, ViraFont.title, onNext,
            Modifier.weight(1f).then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier), area,
        )
        trailing?.invoke()
    }
}

/// Un camp d'una línia amb el text d'ajuda a dins i «Següent» al teclat.
@Composable
fun AddTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Plan,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    label: String = placeholder,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style.copy(color = ViraColor.textPrimary),
        cursorBrush = SolidColor(ViraColor.accent(area)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = modifier.defaultMinSize(minHeight = ViraTouch.minimum).semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style, color = ViraColor.textSecondary)
                inner()
            }
        },
    )
}

/// La targeta groga de Fet (tasca i hàbit): què s'ha apuntat, quan, «Desfer» en lloc de cap
/// confirmació i el que donarà fer-ho.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDoneCard(
    newLabel: String,
    whenText: String,
    symbolName: String,
    title: String,
    rewardLead: String,
    coins: Int?,
    onUndo: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Rewards), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            ViraTag(newLabel)
            ViraTag(whenText)
        }
        Column(
            Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)).padding(ViraSpacing.s),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                ViraPictogram(symbolName, size = 36.dp)
                Text(title, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
                ViraActionButton(stringResource(R.string.action_undo), onClick = onUndo, hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Plan)
            }
            CreateRewardHint(rewardLead, coins)
        }
    }
}

/// Què hi guanyes **quan ho facis** (`CreateRewardHint`): les monedes i la ratxa en píndoles grogues.
/// Crear no dona res —es podria fer trampa creant i esborrant—, així que el text diu sempre «en
/// fer-la», mai «per afegir-la». `coins` és `null` quan avui ja s'ha arribat al sostre: dir «+3»
/// seria prometre una cosa que no arriba. La medicació no en porta mai.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRewardHint(lead: String, coins: Int?) {
    val coinsLabel = coins?.let { stringResource(R.string.rewards_personal_cost, it.toString()) }
    val streakLabel = stringResource(R.string.add_reward_streak_a11y)
    FlowRow(
        Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(lead, style = ViraFont.footnote, color = ViraColor.textSecondary)
        if (coins != null && coinsLabel != null) RewardPill(ViraPictogramName.viriCoin, "+$coins", coinsLabel)
        RewardPill(ViraPictogramName.streakFlame, stringResource(R.string.add_reward_streak), streakLabel)
    }
}

@Composable
private fun RewardPill(pictogram: String, text: String, label: String) {
    Row(
        Modifier
            .defaultMinSize(minHeight = 32.dp)
            .background(ViraColor.surface(ViraArea.Rewards), CircleShape)
            .padding(horizontal = ViraSpacing.s)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(pictogram, size = 20.dp, scalesWithText = true)
        Text(text, style = ViraFont.footnoteEmphasis, color = ViraColor.onSurface(ViraArea.Rewards))
    }
}
