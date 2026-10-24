package cat.vira.android.features.emotions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraAlphaVideo
import cat.vira.android.core.design.components.viraIdleBreath
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraGradient

/// La cara d'una emoció: el cercle rosa amb Viri a sang (`EmotionFace` de l'iPhone).
///
/// Surt a tres llocs —la graella del check-in, la capçalera del full de detall i l'historial— i
/// totes han de ser la mateixa peça: el cap tot just toca la vora de dalt i la figura omple el
/// cercle fins a baix. Sempre amagada a TalkBack: el nom de l'emoció sempre l'acompanya.
///
/// `background`: per defecte, el degradat de targeta d'Emocions. Sobre un mòdul que ja és aquest
/// degradat (el dia de l'historial) el cercle hi desapareixeria, i allà va sobre la superfície.
@Composable
fun EmotionFace(
    emotion: Emotion,
    size: Dp,
    modifier: Modifier = Modifier,
    background: Brush? = null,
) {
    FaceCircle(size, modifier, background ?: ViraGradient.card(ViraArea.Emotions)) {
        FaceArt(emotion, size)
    }
}

/// La mateixa cara **en moviment**, per a la capçalera del full que s'obre en tocar una emoció
/// (`EmotionFaceAnimated`). És qui diu, sense text, de quina emoció parla tot el que ve a sota.
///
/// Viri **fent** l'emoció, en bucle. A sota hi ha la cara quieta, que és la que es veu mentre el
/// vídeo arrenca, a les captures i amb Reduir moviment (on no es mou res, ni vídeo ni respiració).
/// La mida és fixa i no creix amb la lletra: el que ha de créixer és la intensitat.
@Composable
fun EmotionFaceAnimated(emotion: Emotion, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    FaceCircle(size, modifier, ViraGradient.card(ViraArea.Emotions)) {
        Box(Modifier.fillMaxSize().viraIdleBreath()) { FaceArt(emotion, size) }
        val video = emotion.video
        if (!reduceMotion && video != null) {
            ViraAlphaVideo(video, Modifier.fillMaxSize(), loopStart = 0.0, fill = true)
        }
    }
}

@Composable
private fun FaceCircle(size: Dp, modifier: Modifier, background: Brush, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.TopCenter,
        content = content,
    )
}

@Composable
private fun BoxScope.FaceArt(emotion: Emotion, size: Dp) {
    val still = emotion.still
    if (still != null) {
        // El fotograma del vídeo és una escena sencera: omple el cercle.
        Image(painterResource(still), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        // Enquadrament original del retall: la figura omple el cercle a sang.
        Image(
            painterResource(emotion.cutout),
            null,
            Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).size(size * 1.15f),
            contentScale = ContentScale.Fit,
        )
    }
}
