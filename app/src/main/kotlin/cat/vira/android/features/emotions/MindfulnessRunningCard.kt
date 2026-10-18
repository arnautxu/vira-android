package cat.vira.android.features.emotions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraAlphaVideo
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.resources.ViraVideo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/// La targeta rosa mentre sona: Viri meditant dins del cercle, el pas o la fase en gran, la frase
/// que diu la veu i el temps que queda en text i en barra (`MindfulnessRunningCard`).
///
/// Viri aquí és **neutre**, com a la respiració d'«Ajuda ara»: medita, no parla ni celebra.
@Composable
internal fun MindfulnessRunningCard(script: MindfulnessScript, segment: MindfulnessScript.Segment?, currentTime: Double) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    // A la respiració, la fase («Inspira»); en un pas amb nom, el pas; si no, el nom de l'exercici.
    val headlineKey = when {
        segment == null -> "mindfulness.${script.id}.title"
        segment.kind == MindfulnessScript.Kind.BREATH -> segment.text
        else -> segment.stepTitle ?: "mindfulness.${script.id}.title"
    }
    val line = segment?.takeIf { it.kind == MindfulnessScript.Kind.LINE }?.text
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Emotions), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        BreathCircle(fill(script, segment, currentTime), Modifier.padding(vertical = ViraSpacing.s))
        val headline = mindfulnessString(headlineKey)
        val spoken = line?.let { mindfulnessString(it) }
        AnimatedContent(
            targetState = headline to spoken,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            label = "mindfulnessLine",
        ) { (title, text) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                Text(title, style = ViraFont.largeTitle, color = ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                if (text != null) Text(text, style = ViraFont.body, color = ink, textAlign = TextAlign.Center)
            }
        }
        val step = segment?.step
        val leading = if (script.steps > 1 && step != null) stringResource(R.string.mindfulness_step_progress, step + 1, script.steps) else ""
        val seconds = max(kotlin.math.ceil(script.duration - currentTime).toInt(), 0)
        val clock = "%d:%02d".format(seconds / 60, seconds % 60)
        MindfulnessProgress(
            leading = leading,
            trailing = stringResource(R.string.mindfulness_remaining, clock),
            trailingDescription = stringResource(R.string.a11y_seconds_remaining, seconds),
            fraction = (currentTime / max(script.duration, 1.0)).toFloat(),
        )
    }
}

/// Ple de 0 a 1. A la respiració segueix la fase al mil·lisegon (el temps ve del reproductor);
/// entre frases es queda a mig camí, quiet.
internal fun fill(script: MindfulnessScript, segment: MindfulnessScript.Segment?, time: Double): Float {
    val phase = segment?.phase
    if (segment == null || segment.kind != MindfulnessScript.Kind.BREATH || phase == null) return 0.6f
    val eased = 0.5 - cos(script.progress(segment, time) * PI) / 2
    return when (phase) {
        MindfulnessScript.Phase.INHALE -> eased
        MindfulnessScript.Phase.HOLD -> 1.0
        MindfulnessScript.Phase.EXHALE -> 1 - eased
        MindfulnessScript.Phase.WAIT -> 0.0
    }.toFloat()
}

private const val CIRCLE_SIDE = 220f

/// Com a la respiració d'Ajuda ara: escala i no mida, perquè a dins hi ha un vídeo. Amb Reduir
/// moviment el cercle no creix: canvia d'opacitat, que no és desplaçament.
@Composable
private fun BreathCircle(target: Float, modifier: Modifier = Modifier) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    // El temps arriba deu cops per segon: l'animació curta tapa el salt entre lectures.
    val fill by animateFloatAsState(target, tween(120), label = "mindfulnessFill")
    val rest = 140f / CIRCLE_SIDE
    Box(
        modifier
            .graphicsLayer {
                val scale = if (reduceMotion) 180f / CIRCLE_SIDE else rest + (1f - rest) * fill
                scaleX = scale
                scaleY = scale
                alpha = if (reduceMotion) 0.45f + 0.55f * fill else 1f
            }
            .clearAndSetSemantics {}
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(ViraSpacing.s),
    ) {
        val inner = Modifier.size(CIRCLE_SIDE.dp).clip(CircleShape)
        if (!reduceMotion && !LocalInspectionMode.current) {
            ViraAlphaVideo(ViraVideo.ViriMeditatingLoop, inner, loopStart = 0.0, fill = true)
        } else {
            Image(painterResource(R.drawable.viri_meditating), null, inner, contentScale = ContentScale.Crop)
        }
    }
}

/// Progrés sobre la targeta: el text a dalt i la barra a sota (§4, mai només la barra).
@Composable
private fun MindfulnessProgress(leading: String, trailing: String, trailingDescription: String, fraction: Float) {
    val ink = ViraColor.onCard(ViraArea.Emotions)
    val height = 8.dp * LocalDensity.current.fontScale
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = listOf(leading, trailingDescription).filter { it.isNotEmpty() }.joinToString(", ")
        },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(leading, style = ViraFont.footnoteEmphasis, color = ink, modifier = Modifier.weight(1f))
            Text(trailing, style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum"), color = ink)
        }
        Box(Modifier.fillMaxWidth().height(height).background(ink.copy(alpha = 0.18f), CircleShape)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).background(ink, CircleShape))
        }
    }
}
