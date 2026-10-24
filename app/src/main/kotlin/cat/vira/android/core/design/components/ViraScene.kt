package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/// Una escena quadrada de Viri (`viri_suggest_*`, `viri_home_*`) dins d'un bàner més ample que
/// alt, amb Viri sencer (`ViraSceneBanner` de l'iPhone; Arnau, 2026-10-21: «als hàbits surt un
/// Viri tallat», «a Mindfulness Viri es fa gran»).
///
/// L'alçada va lligada a l'ample (`heightRatio`), així que l'escena es veu a la mateixa escala a
/// tots els mòbils, i la finestra baixa o puja fins a Viri (`ViraSceneFraming`). `hiddenBottom`
/// és el que tapa la bombolla que trepitja l'escena: Viri es centra en el que queda a la vista.
/// Sense respiració: una animació que no s'acaba mai agafava també el canvi de mida en aparèixer.
@Composable
fun ViraSceneBanner(
    @DrawableRes scene: Int,
    heightRatio: Float,
    modifier: Modifier = Modifier,
    hiddenBottom: Dp = 0.dp,
) {
    val name = sceneName(scene)
    Layout(
        content = {
            Image(painterResource(scene), contentDescription = null, contentScale = ContentScale.Crop)
        },
        // Decorativa: el text del costat ja diu el que cal.
        modifier = modifier.clipToBounds().clearAndSetSemantics { },
    ) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = (width * heightRatio).roundToInt()
        val image = measurables.first().measure(Constraints.fixed(width, width))
        val top = ViraSceneFraming.top(name, heightRatio, hiddenBottom.toPx() / maxOf(width, 1))
        layout(width, height) { image.place(0, -(top * width).roundToInt()) }
    }
}

/// Una escena quadrada en miniatura: el tros on hi ha Viri sencer, amb les cantonades arrodonides
/// (`ViraSceneTile`). Una escena que no és a `ViraSceneFraming.tiles` es veu sencera.
@Composable
fun ViraSceneTile(@DrawableRes scene: Int, side: Dp, modifier: Modifier = Modifier) {
    val name = sceneName(scene)
    Layout(
        content = {
            Image(painterResource(scene), contentDescription = null, contentScale = ContentScale.Crop)
        },
        modifier = modifier
            .size(side)
            .clip(RoundedCornerShape(side * 0.22f))
            .clearAndSetSemantics { },
    ) { measurables, constraints ->
        val window = ViraSceneFraming.tile(name)
        val box = constraints.maxWidth
        val full = (box / window.width).roundToInt()
        val image = measurables.first().measure(Constraints.fixed(full, full))
        layout(box, box) { image.place(-(window.left * full).roundToInt(), -(window.top * full).roundToInt()) }
    }
}

/// El nom del recurs (`viri_suggest_habit_1`), que és la clau de `ViraSceneFraming`.
@Composable
private fun sceneName(@DrawableRes scene: Int): String {
    val resources = LocalContext.current.resources
    return remember(scene) { resources.getResourceEntryName(scene) }
}

/// On és Viri a cada escena quadrada, de dalt a baix, en fracció del costat (`ViraSceneFraming` de
/// l'iPhone). Mesurat sobre la il·lustració de base; les skins en mantenen la composició. Una
/// escena que no hi és es queda centrada.
object ViraSceneFraming {
    val viri: Map<String, ClosedFloatingPointRange<Float>> = mapOf(
        "viri_suggest_habit_1" to 0.39f..0.89f,
        "viri_suggest_mindfulness_1" to 0.30f..0.84f,
        // Amb el coixí: Viri hi seu a sobre.
        "viri_suggest_mindfulness_2" to 0.40f..0.86f,
        // Amb la taula on escriu.
        "viri_suggest_focus_2" to 0.36f..0.72f,
        "viri_suggest_wind_down_1" to 0.33f..0.86f,
        "viri_suggest_wind_down_2" to 0.26f..0.85f,
        // A la taula, amb les targetes que parteix.
        "viri_suggest_breakdown_1" to 0.08f..0.72f,
        "viri_suggest_capture_1" to 0.30f..0.84f,
        "viri_suggest_habit_2" to 0.30f..0.72f,
        // Amb el tauler del sol.
        "viri_suggest_habit_3" to 0.28f..0.80f,
        "viri_home_done" to 0.28f..0.82f,
    )

    /// El quadrat de cada escena on hi cap Viri sencer, en fracció del costat.
    val tiles: Map<String, Rect> = mapOf(
        "viri_home_morning" to Rect(0.155f, 0.15f, 0.155f + 0.72f, 0.15f + 0.72f),
        "viri_home_done" to Rect(0.19f, 0.20f, 0.19f + 0.62f, 0.20f + 0.62f),
        "viri_suggest_capture_1" to Rect(0.01f, 0.27f, 0.01f + 0.63f, 0.27f + 0.63f),
        // Assegut a la taula: la taula talla Viri a posta.
        "viri_suggest_focus_1" to Rect(0.24f, 0.10f, 0.24f + 0.70f, 0.10f + 0.70f),
        "viri_suggest_breakdown_1" to Rect(0.22f, 0.04f, 0.22f + 0.74f, 0.04f + 0.74f),
        "viri_suggest_breakdown_3" to Rect(0.17f, 0.31f, 0.17f + 0.46f, 0.31f + 0.46f),
        "viri_suggest_wind_down_2" to Rect(0.17f, 0.23f, 0.17f + 0.70f, 0.23f + 0.70f),
        "viri_scene_plan_empty" to Rect(0.19f, 0.31f, 0.19f + 0.62f, 0.31f + 0.62f),
        "viri_suggest_habit_1" to Rect(0f, 0.35f, 0.61f, 0.35f + 0.61f),
        "viri_suggest_habit_3" to Rect(0f, 0.22f, 0.62f, 0.22f + 0.62f),
    )

    /// Prou alçada, en fracció de l'ample, perquè Viri hi càpiga sencer amb una mica d'aire.
    fun heightRatio(name: String, atLeast: Float): Float {
        val span = viri[name] ?: return atLeast
        return maxOf(atLeast, span.endInclusive - span.start + 0.10f).coerceAtMost(1f)
    }

    /// On comença la finestra, en fracció del costat, sense sortir mai de l'escena.
    fun top(name: String, heightRatio: Float, hiddenBottom: Float): Float {
        val visible = heightRatio - hiddenBottom
        val span = viri[name] ?: 0.25f..0.75f
        val top = (span.start + span.endInclusive) / 2 - visible / 2
        return top.coerceIn(0f, maxOf(1f - heightRatio, 0f))
    }

    fun tile(name: String): Rect = tiles[name] ?: Rect(0f, 0f, 1f, 1f)
}
