package cat.vira.android.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing

/// Una acció «feta» que encara es pot desfer uns segons, abans que la recompensa es concedeixi de
/// veritat (`PendingCompletion` de l'iPhone).
///
/// El marcatge és instantani (casella, ratllat) i **només la concessió es retarda**: l'XP i les
/// ViriCoins mai baixen (`VIRI_AND_GAMIFICATION.md` §2.2.1), així que concedir-les a l'instant
/// deixaria un «Desfer» que no desfà. Si als 2 s ningú ho ha desfet, es concedeix i la mateixa
/// fila ho revela; si es desfà abans, no s'ha concedit mai res.
@Immutable
class PendingCompletion(
    /// Identitat de l'entitat marcada (p. ex. `"plan-${item.id}"`): la fila amb el mateix `id` és
    /// la que ensenya el «Desfer» i, després, la revelació.
    val id: String,
    val reason: CoinReason,
    /// Reverteix el marcatge visual.
    val onUndo: () -> Unit,
    /// Efecte propi del tipus d'acció, més enllà de la recompensa. Només es crida si la finestra
    /// passa sense desfer-se.
    val onCommit: (suspend () -> Unit)? = null,
    /// Nom de la cosa marcada, quan la celebració que ve després l'ha de dir (l'hàbit).
    val title: String? = null,
)

/// Fase visible d'una acció pendent, ja calculada per qui porta la cua: cada fila només l'ha de
/// dibuixar, no cronometrar-la.
@Immutable
sealed interface CompletionPhase {
    data object Pending : CompletionPhase
    data class Revealing(val coins: Int, val xp: Int) : CompletionPhase
}

/// L'única acció pendent de tota l'app en aquest instant, amb la seva fase.
@Immutable
data class ActiveCompletion(val id: String, val phase: CompletionPhase)

/// Marca una acció com a feta i n'obre la finestra de «Desfer». Si n'hi havia una altra pendent
/// per a una altra entitat, aquella es dona per definitiva (es concedeix sense revelar-la).
/// Per defecte no fa res: la cua de l'app (`ViraCompletionQueue`) la proporciona a l'arrel.
val LocalPresentCompletion = staticCompositionLocalOf<(PendingCompletion) -> Unit> { {} }

/// Desfà l'acció pendent si `id` hi coincideix, sense concedir-hi res. Inofensiu si ja no és
/// l'activa, perquè una fila el pugui cridar sense comprovar-ho abans.
val LocalUndoCompletion = staticCompositionLocalOf<(String) -> Unit> { {} }

/// L'acció pendent ara mateix, si n'hi ha. Cada fila la compara amb el seu `id` per saber si li
/// toca ensenyar el «Desfer» o la revelació **dins de la seva pròpia targeta**.
val LocalActiveCompletion = compositionLocalOf<ActiveCompletion?> { null }

/// Proporciona les tres peces de la cua de confirmacions a tot el que hi ha a sota.
@Composable
fun ProvideViraCompletions(
    present: (PendingCompletion) -> Unit,
    undo: (String) -> Unit,
    active: ActiveCompletion?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPresentCompletion provides present,
        LocalUndoCompletion provides undo,
        LocalActiveCompletion provides active,
        content = content,
    )
}

/// El contingut que substitueix el d'una fila mentre dura el «Desfer» i la revelació: mai una
/// targeta nova, mateixa forma i fons, només canvia què hi ha a dins.
///
/// Llisca d'una cosa a l'altra (la fila que deia «Fet · Desfer» ara diu «+3 +3 XP»): una
/// transformació, no un afegit. Amb Reduir moviment, només fos.
///
/// `foreground` és el color de text que ja feia servir la fila (p. ex. `ViraColor.onCard(area)`).
@Composable
fun ViraInlineCompletion(
    phase: CompletionPhase,
    foreground: Color,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    AnimatedContent(
        targetState = phase,
        modifier = modifier,
        contentKey = { it is CompletionPhase.Pending },
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
            } else {
                (slideInHorizontally(ViraMotion.standard()) { it } + fadeIn(ViraMotion.standard())) togetherWith
                    (slideOutHorizontally(ViraMotion.standard()) { -it } + fadeOut(ViraMotion.standard()))
            }
        },
        label = "inlineCompletion",
    ) { current ->
        when (current) {
            CompletionPhase.Pending -> PendingContent(foreground, onUndo)
            is CompletionPhase.Revealing -> ViraCoinReveal(current.coins, current.xp, foreground)
        }
    }
}

@Composable
private fun PendingContent(foreground: Color, onUndo: () -> Unit) {
    val undoLabel = stringResource(R.string.action_undo)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                customActions = listOf(CustomAccessibilityAction(undoLabel) { onUndo(); true })
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.action_done), style = ViraFont.headline, color = foreground)
        Spacer(Modifier.weight(1f).widthIn(min = ViraSpacing.s))
        Box(
            Modifier.viraTouchTarget().viraPressable(onClick = onUndo),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                undoLabel,
                style = ViraFont.headline.copy(textDecoration = TextDecoration.Underline),
                color = foreground,
            )
        }
    }
}

/// El que s'acaba de guanyar —ViriCoins i XP— en icona i xifra, a la celebració a pantalla
/// completa del primer hàbit del dia. El mateix guany surt també a la fila que el revela
/// (`ViraCoinReveal`), perquè la celebració tapa precisament aquella fila.
@Composable
fun ViraRewardTally(
    coins: Int,
    xp: Int,
    modifier: Modifier = Modifier,
    /// Mida de les icones; el text hi va a joc. La fila el vol discret, la celebració gran.
    iconSize: Dp = 20.dp,
    style: TextStyle = ViraFont.headline,
    color: Color = Color.Unspecified,
) {
    val description = viraRewardText(coins, xp)
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.l, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val digits = style.copy(fontFeatureSettings = "tnum")
        if (coins > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                ViraPictogram(ViraPictogramName.viriCoin, size = iconSize)
                Text(stringResource(R.string.focus_done_coins, coins), style = digits, color = color)
            }
        }
        if (xp > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                ViraPictogram(ViraPictogramName.xp, size = iconSize)
                Text(stringResource(R.string.toast_reward_xp, xp), style = digits, color = color)
            }
        }
    }
}

/// El guany dit sencer, per a TalkBack: «+3 ViriCoins, +3 XP».
@Composable
fun viraRewardText(coins: Int, xp: Int): String = listOfNotNull(
    if (coins > 0) stringResource(R.string.focus_done_coins, coins) else null,
    if (xp > 0) stringResource(R.string.toast_reward_xp, xp) else null,
).joinToString(", ")
