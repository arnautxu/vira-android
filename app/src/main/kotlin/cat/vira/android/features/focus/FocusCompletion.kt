package cat.vira.android.features.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraConfettiBurst
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay

/// Final de la sessió (`FocusCompletionView`): completada o interrompuda, cada una amb el seu to.
///
/// Completada: targeta verda amb Viri i la medalla, la frase en bombolla, i els minuts i les monedes
/// en píndoles; a sota, el descans com a fitxa. Interrompuda: Viri tranquil a la seva taula, sense
/// cap gest de decepció ni llenguatge de fallada, i els minuts que sí que s'han fet. El descans es
/// proposa, mai s'imposa.
@Composable
fun FocusCompletionContent(
    completion: FocusSessionController.Completion,
    onStartBreak: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val celebrations = remember {
        inspection || ViraPreferences.open(context).getBoolean(ViraPreferences.CELEBRATIONS_ENABLED, true)
    }
    val celebrates = !completion.wasInterrupted && completion.coinsEarned > 0
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin)
                    .padding(top = ViraSpacing.s),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                if (completion.wasInterrupted) {
                    InterruptedCard(completion)
                } else {
                    CompletedCard(completion)
                    if (completion.suggestedBreak != null) BreakCard()
                }
            }
            Actions(completion, onStartBreak, onClose, Modifier.padding(horizontal = ViraSpacing.screenMargin).padding(bottom = ViraSpacing.s))
        }
        // Confeti només per a la fita real (completada amb monedes), mai a la sortida interrompuda.
        if (celebrates && celebrations && !inspection) {
            ViraConfettiBurst(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun CompletedCard(completion: FocusSessionController.Completion) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    // L'entrada: un «pop» amb `emphasis`, reservat a fites. Amb Reduir moviment, un fos.
    val inspection = LocalInspectionMode.current
    val appear = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, ViraMotion.emphasis()) }
    // Les monedes compten cap amunt un cop Viri ja ha entrat.
    var shownCoins by remember { mutableIntStateOf(if (reduceMotion) completion.coinsEarned else 0) }
    LaunchedEffect(completion.coinsEarned) {
        if (reduceMotion || inspection) { shownCoins = completion.coinsEarned; return@LaunchedEffect }
        delay(340)
        val counter = Animatable(0f)
        counter.animateTo(completion.coinsEarned.toFloat(), tween(500, easing = FastOutSlowInEasing)) { shownCoins = value.toInt() }
        shownCoins = completion.coinsEarned
    }
    val ink = ViraColor.onCard(ViraArea.Focus)
    val medalScale = if (reduceMotion) 1f else 0.6f + 0.4f * appear.value
    val medal: @Composable () -> Unit = {
        ViraPictogram(
            R.drawable.viri_medal,
            size = if (stacked) 120.dp else 150.dp,
            modifier = Modifier.graphicsLayer { scaleX = medalScale; scaleY = medalScale; alpha = appear.value },
        )
    }
    val bubble = stringResource(R.string.focus_done_viri)
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Focus), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (stacked) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { medal() }
            FocusViriBubble(bubble)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalAlignment = Alignment.Top) {
                medal()
                FocusViriBubble(bubble, Modifier.padding(top = ViraSpacing.s))
            }
        }
        Text(stringResource(R.string.focus_done_title), style = ViraFont.largeTitle, color = ink, modifier = Modifier.graphicsLayer { alpha = appear.value })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            modifier = Modifier.graphicsLayer { alpha = appear.value },
        ) {
            if (completion.minutesDone > 0) {
                FocusPill(stringResource(R.string.focus_minutes, completion.minutesDone), glyph = FocusGlyphKind.Clock)
            }
            if (completion.coinsEarned > 0) {
                FocusPill(stringResource(R.string.focus_done_coins, shownCoins), pictogram = R.drawable.picto_shop)
            }
        }
    }
}

@Composable
private fun BreakCard() {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Row(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.s)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViraPictogram(R.drawable.viri_chill, size = if (stacked) 64.dp else 88.dp, isScene = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(stringResource(R.string.focus_break_card_title), style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(stringResource(R.string.focus_break_card_message), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
    }
}

/// Viri a la seva taula, tranquil. Cap retret: la frase és la de sempre i els minuts fets són un fet.
@Composable
private fun InterruptedCard(completion: FocusSessionController.Completion) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.l)
    val ink = ViraColor.onCard(ViraArea.Focus)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ViraGradient.card(ViraArea.Focus), shape)
            .semantics(mergeDescendants = true) {},
    ) {
        Image(
            painter = painterResource(R.drawable.viri_suggest_focus_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(if (stacked) 140.dp else 210.dp),
        )
        // La bombolla trepitja la il·lustració, com a l'iPhone: el bloc de text puja i hi arrossega la resta.
        Column(
            Modifier.padding(ViraSpacing.m).pullUp(ViraSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            FocusViriBubble(stringResource(R.string.focus_interrupted_message))
            Text(stringResource(R.string.focus_interrupted_title), style = ViraFont.title, color = ink)
            if (completion.minutesDone > 0) {
                FocusPill(stringResource(R.string.focus_interrupted_minutes, completion.minutesDone), glyph = FocusGlyphKind.Clock)
            }
        }
    }
}

@Composable
private fun Actions(
    completion: FocusSessionController.Completion,
    onStartBreak: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    if (!completion.wasInterrupted && completion.suggestedBreak != null) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            ViraActionButton(
                stringResource(R.string.focus_break_suggest, completion.suggestedBreakMinutes),
                onStartBreak,
                Modifier.fillMaxWidth(),
                area = ViraArea.Focus,
            )
            ViraActionButton(
                stringResource(R.string.focus_break_skip),
                onClose,
                Modifier.fillMaxWidth(),
                hierarchy = ViraActionHierarchy.Textual,
                area = ViraArea.Focus,
            )
        }
    } else {
        ViraActionButton(stringResource(R.string.focus_close), onClose, modifier.fillMaxWidth(), area = ViraArea.Focus)
    }
}

/// Un marge negatiu per dalt (el `padding(.top, -x)` de SwiftUI): el contingut puja i ocupa menys.
private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}
