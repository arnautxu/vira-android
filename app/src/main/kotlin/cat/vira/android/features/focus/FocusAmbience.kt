package cat.vira.android.features.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraRadius

/// Fons de la preparació: el racó il·lustrat, amb una capa de contrast a la part alta
/// (`FocusAmbience.swift`).
///
/// L'asset té versió clara i fosca (`drawable-nodpi` i `drawable-night-nodpi`): la mateixa escena
/// amb una altra llum.
///
/// La capa **només va a dalt**, per al títol i la creu de tancar. Tot el que seu a la meitat baixa
/// porta la seva pròpia superfície (`focusPlate`), on el contrast el garanteixen els tokens i no cap
/// píxel de la imatge. Valors de l'iPhone: amb la capa al 92 %, el títol queda a 4,53:1 en clar,
/// que és el cas que mana; no la baixis sense tornar-ho a mesurar.
@Composable
fun FocusAmbience(modifier: Modifier = Modifier) {
    val environment = LocalViraEnvironment.current
    val topAlpha = if (environment.increasedContrast) 0.98f else 0.92f
    val drift = if (environment.reduceMotion) {
        1f
    } else {
        // Un 4 % d'escala en 14 s d'anada i 14 de tornada: el fons no és una foto, però no es nota.
        val transition = rememberInfiniteTransition(label = "ambience")
        transition.animateFloat(1f, 1.04f, infiniteRepeatable(tween(14_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift").value
    }
    val background = ViraColor.background
    Box(modifier.fillMaxSize().clipToBounds()) {
        Image(
            painter = painterResource(R.drawable.focus_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = drift; scaleY = drift },
        )
        // Plena fins al 23 % (on acaba el títol) i apagada del tot al 38 %.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = topAlpha),
                    0.23f to background.copy(alpha = topAlpha),
                    0.38f to background.copy(alpha = 0f),
                ),
            ),
        )
    }
}

/// La superfície sota un tros de text que, si no, seuria damunt la il·lustració (`FocusPlate`).
/// Opaca del tot: amb transparència, el contrast tornaria a dependre de la imatge.
@Composable
fun Modifier.focusPlate(block: Boolean = false): Modifier =
    background(ViraColor.surface(ViraArea.Focus), if (block) RoundedCornerShape(ViraRadius.l) else CircleShape)
