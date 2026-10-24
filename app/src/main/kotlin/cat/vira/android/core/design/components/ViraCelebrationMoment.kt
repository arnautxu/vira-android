package cat.vira.android.core.design.components

import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.sound.ViraSound
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/// El moment de nivell, repte, premi i carta (`CelebrationMomentScreen`, Arnau 2026-10-10):
/// **escena de Viri** a dalt, la bombolla que en surt amb el que diu Viri (títol i frase, junts
/// ≤15 paraules), la **targeta de resum** i el botó lila al peu. Fons suau de l'àrea; sense
/// confeti ni xifra que compta. Amb Reduir moviment, només fosos.
///
/// A lletra d'accessibilitat l'escena es fa petita i tot llisca, botó inclòs.
@Composable
fun ViraCelebrationMoment(
    title: String,
    message: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Rewards,
    onShown: () -> Unit = {},
    /// Repte, premi i carta comparteixen el «ta-da»; el nivell porta l'arpegi.
    sound: ViraSound.Cue = ViraSound.Cue.CHALLENGE,
    scene: @Composable () -> Unit,
    summary: @Composable () -> Unit,
) {
    val environment = LocalViraEnvironment.current
    val reduceMotion = environment.reduceMotion
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val bubble = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val summaryIn = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onShown()
        ViraHaptics.celebrate(context)
        // Amb la remuntada tàctil, que arrenca als 420 ms.
        ViraSound.play(context, sound, delayMs = 420)
        if (reduceMotion) return@LaunchedEffect
        launch { appear.animateTo(1f, ViraMotion.entrance()) }
        launch { delay(150); bubble.animateTo(1f, ViraMotion.entrance()) }
        launch { delay(300); summaryIn.animateTo(1f, ViraMotion.standard()) }
    }
    LaunchedEffect(Unit) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) {
            delay(600)
            view.announceForAccessibility("$title. $message")
        }
    }

    val shape = RoundedCornerShape(ViraRadius.l)
    val sceneView: @Composable (Modifier) -> Unit = { sceneModifier ->
        Box(
            sceneModifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = if (reduceMotion) 1f else 0.94f + 0.06f * appear.value
                    scaleX = s; scaleY = s; alpha = appear.value.coerceIn(0f, 1f)
                }
                .shadow(14.dp, shape, ambientColor = ViraColor.brandAccent.copy(alpha = 0.16f), spotColor = ViraColor.brandAccent.copy(alpha = 0.16f))
                .background(ViraColor.surfaceElevated, shape)
                .clip(shape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) { scene() }
    }
    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
            Bubble(
                title, message,
                Modifier
                    // La bombolla trepitja la vora de l'escena: surt de Viri. Com el `padding`
                    // negatiu de l'iPhone, s'enfila també a la maquetació, i el que ve darrere puja.
                    .pullUp(ViraSpacing.xxxl)
                    .padding(horizontal = ViraSpacing.s)
                    .graphicsLayer {
                        translationY = ViraSpacing.s.toPx() * (1f - bubble.value)
                        alpha = bubble.value.coerceIn(0f, 1f)
                    },
            )
            Box(Modifier.graphicsLayer { alpha = summaryIn.value.coerceIn(0f, 1f) }) { summary() }
        }
    }
    val button: @Composable () -> Unit = {
        ViraActionButton(
            stringResource(R.string.streak_milestone_continue),
            onContinue,
            modifier = Modifier.padding(top = ViraSpacing.l).fillMaxWidth(),
        )
    }

    Box(modifier.fillMaxSize().viraSectionBackground(area)) {
        if (environment.isAccessibilitySize) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(ViraSpacing.screenMargin),
            ) {
                sceneView(Modifier.height(180.dp))
                content()
                button()
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(ViraSpacing.screenMargin)) {
                // L'escena s'emporta tot l'espai que sobra: és la protagonista.
                sceneView(Modifier.weight(1f).heightIn(min = 180.dp))
                content()
                button()
            }
        }
    }
}

/// Puja el contingut `amount` i encongeix l'espai que ocupa en la mateixa mida.
private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}

@Composable
private fun Bubble(title: String, message: String, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Box(modifier.fillMaxWidth()) {
        // La punta, cap a Viri.
        Box(
            Modifier
                .offset(x = ViraSpacing.xxxl, y = -ViraSpacing.xs)
                .size(20.dp)
                .rotate(45f)
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraSpacing.xxs)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, shape, ambientColor = ViraColor.brandAccent.copy(alpha = 0.14f), spotColor = ViraColor.brandAccent.copy(alpha = 0.14f))
                .background(ViraColor.surfaceElevated, shape)
                .padding(horizontal = ViraSpacing.l, vertical = ViraSpacing.s)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
        ) {
            Text(title, style = ViraFont.title, color = ViraColor.brandAccent)
            Text(message, style = ViraFont.body, color = ViraColor.textPrimary)
        }
    }
}
