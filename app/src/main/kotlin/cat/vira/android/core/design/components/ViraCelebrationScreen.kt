package cat.vira.android.core.design.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/// El moment a pantalla completa d'una fita (`ViraCelebrationScreen`): ratxa, hàbit i botiga
/// (nivell, repte, premi i carta van amb `ViraCelebrationMoment`, `DESIGN_RULES.md` §5).
///
/// Fons a sang amb `cardGradient(area)` i **tot el text en `onCard(area)`, sense opacitat**: el
/// contrast va just i la jerarquia la fa la mida. Heroi amb la ràfega al darrere, píndola de
/// context, xifra gegant que compta amunt (decorativa: el peu ja la diu en paraules), missatge i
/// «Continua» ple del color del text. Confeti si `celebrationsEnabled`.
///
/// Amb Reduir moviment, tot apareix al seu lloc amb un fos i la xifra hi és directament.
/// `onShown` és on qui la mostra fa sonar el so i la vibració de la fita.
///
/// Encara sense el vídeo de fons de l'iPhone (`ViraCelebrationBackdrop`): arriba amb el
/// component de vídeo amb alfa.
@Composable
fun ViraCelebrationScreen(
    caption: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Rewards,
    eyebrow: String? = null,
    count: Int? = null,
    message: String? = null,
    celebrationsEnabled: Boolean = true,
    onShown: () -> Unit = {},
    extra: @Composable () -> Unit = {},
    hero: @Composable () -> Unit,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val ink = ViraColor.onCard(area)
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val shownCount = remember { Animatable(if (reduceMotion) (count ?: 0).toFloat() else 0f) }
    val announcement = listOfNotNull(eyebrow, caption, message).joinToString(". ")
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onShown()
        launch { appear.animateTo(1f, if (reduceMotion) ViraMotion.reduced() else ViraMotion.emphasis()) }
        if (count != null && !reduceMotion) {
            delay(420)
            shownCount.animateTo(count.toFloat(), tween(500))
        }
    }
    // Amb TalkBack, la pantalla diu què has aconseguit en obrir-se.
    LaunchedEffect(Unit) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) {
            delay(600)
            view.announceForAccessibility(announcement)
        }
    }

    fun Modifier.reveal(after: Float) = alpha(((appear.value - after) / (1f - after)).coerceIn(0f, 1f))

    Box(modifier.fillMaxSize().background(ViraGradient.card(area))) {
        // Llisca quan no hi cap (lletra al 200 % en un mòbil baix): el botó mai queda fora de la
        // pantalla. Quan hi cap, el contingut queda centrat entre dalt i el botó, com a l'iPhone.
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(ViraSpacing.screenMargin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                    modifier = Modifier.padding(horizontal = ViraSpacing.m),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            val s = if (reduceMotion) 1f else 0.6f + 0.4f * appear.value
                            scaleX = s; scaleY = s; alpha = appear.value.coerceIn(0f, 1f)
                        },
                    ) {
                        ViraCelebrationBurst(area = area)
                        hero()
                    }
                    if (eyebrow != null) {
                        Text(
                            eyebrow,
                            style = ViraFont.callout,
                            color = ink,
                            textAlign = TextAlign.Center,
                            // Només contorn: un farciment translúcid menjaria contrast al text.
                            modifier = Modifier.reveal(0.16f).border(1.5.dp, ink, CircleShape)
                                .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.reveal(0.28f)) {
                        if (count != null) {
                            Text(
                                "${shownCount.value.roundToInt()}",
                                style = ViraFont.timer,
                                color = ink,
                                maxLines = 1,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        }
                        Text(
                            caption,
                            style = if (count == null) ViraFont.title else ViraFont.headline,
                            color = ink,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (message != null) {
                        Text(message, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center, modifier = Modifier.reveal(0.44f))
                    }
                    Box(Modifier.padding(top = ViraSpacing.s)) { extra() }
                }
                // Ple amb el color del text i l'etiqueta amb el del fons: la mateixa parella, invertida.
                Text(
                    stringResource(R.string.streak_milestone_continue),
                    style = ViraFont.headline,
                    color = ViraColor.cardFill(area),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = ViraSpacing.l)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ViraTouch.comfortable)
                        .background(ink, CircleShape)
                        .viraPressable(onClick = onContinue)
                        .padding(vertical = ViraSpacing.m),
                )
            }
        }
        if (celebrationsEnabled) ViraConfettiBurst(Modifier.align(Alignment.Center), over = area)
    }
}
