package cat.vira.android.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Crear un premi personal (`PersonalRewardEditor`, `USER_FLOWS.md` F20): **un sol camp** —el que
/// vols fer o tenir— més el cost en ViriCoins. Sense editar-ne un d'existent: si el vols canviar,
/// l'arxives i en crees un altre. El sostre de 3.000 és el mateix de la Botiga
/// (`VIRI_AND_GAMIFICATION.md` §2.2.1).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRewardEditor(onSave: (title: String, cost: Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.background,
    ) {
        PersonalRewardEditorContent(onSave, onDismiss, Modifier.imePadding())
    }
}

/// El contingut de l'editor, separat del full perquè es pugui capturar.
@Composable
fun PersonalRewardEditorContent(
    onSave: (title: String, cost: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialTitle: String = "",
    focusesTitle: Boolean = true,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var cost by rememberSaveable { mutableIntStateOf(100) }
    val focus = remember { FocusRequester() }
    val canSave = title.isNotBlank()
    val save = { if (canSave) onSave(title.trim(), cost) }
    if (focusesTitle) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        modifier.fillMaxWidth().padding(horizontal = ViraSpacing.screenMargin).padding(bottom = ViraSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetTextButton(stringResource(R.string.action_cancel), onCancel)
            Text(
                stringResource(R.string.rewards_personal_new),
                style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SheetTextButton(stringResource(R.string.action_add), save, enabled = canSave, emphasized = true)
        }
        val placeholder = stringResource(R.string.rewards_personal_placeholder)
        Box(
            Modifier.fillMaxWidth()
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
                .padding(ViraSpacing.m),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
                cursorBrush = SolidColor(ViraColor.accent(ViraArea.Today)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    .semantics { contentDescription = placeholder },
                decorationBox = { inner ->
                    if (title.isEmpty()) Text(placeholder, style = ViraFont.body, color = ViraColor.textSecondary)
                    inner()
                },
            )
        }
        CostStepper(cost) { cost = it }
    }
}

/// El cost amb − i +, de 10 en 10 entre 10 i 3.000 (el `Stepper` de l'iPhone). Per a TalkBack és
/// un sol control ajustable.
@Composable
private fun CostStepper(cost: Int, onChange: (Int) -> Unit) {
    val range = ViraPersonalRewards.COST_RANGE
    val step = ViraPersonalRewards.COST_STEP
    val label = stringResource(R.string.rewards_personal_cost, cost.viraFormatted())
    Row(
        Modifier.fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs)
            .clearAndSetSemantics {
                stateDescription = label
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(cost.toFloat(), range.first.toFloat()..range.last.toFloat(), (range.last - range.first) / step - 1)
                setProgress { target ->
                    val snapped = (Math.round(target / step) * step).coerceIn(range.first, range.last)
                    onChange(snapped); true
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        ViraPictogram(R.drawable.picto_shop, size = 20.dp)
        Text(label, style = ViraFont.body.tabular(), color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
        StepButton(ShopSymbol.Minus, enabled = cost > range.first) { onChange((cost - step).coerceAtLeast(range.first)) }
        StepButton(ShopSymbol.Plus, enabled = cost < range.last) { onChange((cost + step).coerceAtMost(range.last)) }
    }
}

@Composable
private fun StepButton(symbol: ShopSymbol, enabled: Boolean, onClick: () -> Unit) {
    val ink = if (enabled) ViraColor.accent(ViraArea.Today) else ViraColor.textSecondary
    Box(
        Modifier.size(ViraTouch.minimum)
            .background(ViraColor.surfaceShift, CircleShape)
            .viraPressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ShopSymbolIcon(symbol, ink, size = 18.dp)
    }
}

@Composable
private fun SheetTextButton(text: String, onClick: () -> Unit, enabled: Boolean = true, emphasized: Boolean = false) {
    Box(
        Modifier.heightIn(min = ViraTouch.minimum)
            .viraPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ViraSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (emphasized) ViraFont.headline else ViraFont.body,
            color = if (enabled) ViraColor.accent(ViraArea.Today) else ViraColor.textSecondary,
        )
    }
}
