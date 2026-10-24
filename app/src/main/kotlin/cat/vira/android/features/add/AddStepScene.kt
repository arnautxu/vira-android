package cat.vira.android.features.add

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlin.math.roundToInt

/// Les escenes de Viri del full «Afegir», amb on és Viri de dalt a baix en fracció del costat
/// (`ViraSceneFraming`): la finestra del bàner baixa o puja fins a ell perquè surti sencer.
enum class AddScene(@DrawableRes val image: Int, val viriTop: Float, val viriBottom: Float) {
    Capture(R.drawable.viri_suggest_capture_1, 0.30f, 0.84f),
    Breakdown(R.drawable.viri_suggest_breakdown_1, 0.08f, 0.72f),
    Habit(R.drawable.viri_suggest_habit_2, 0.30f, 0.72f),
    Board(R.drawable.viri_suggest_habit_3, 0.28f, 0.80f),
    HabitDone(R.drawable.viri_suggest_habit_1, 0.39f, 0.89f),
    Done(R.drawable.viri_home_done, 0.28f, 0.82f);

    /// Prou alçada, en fracció de l'ample, perquè Viri hi càpiga sencer amb una mica d'aire.
    fun heightRatio(atLeast: Float): Float = minOf(maxOf(atLeast, viriBottom - viriTop + 0.10f), 1f)

    /// On comença la finestra, en fracció del costat, sense sortir mai de l'escena.
    fun top(heightRatio: Float, hiddenBottom: Float): Float {
        val visible = heightRatio - hiddenBottom
        val top = (viriTop + viriBottom) / 2 - visible / 2
        return top.coerceIn(0f, 1f - heightRatio)
    }
}

/// Mòdul protagonista d'un pas del full «Afegir» (`AddStepScene`): l'escena de Viri a tot l'ample
/// i la bombolla que en surt amb la pregunta. Cap text sobre la il·lustració, i la bombolla en
/// trepitja el terra perquè es llegeixi com que surt de Viri. A lletra gran l'escena es fa
/// petita i la bombolla baixa a sota, sense trepitjar-la.
@Composable
fun AddStepScene(
    scene: AddScene,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Plan,
    /// L'alçada mínima de l'escena en fracció de l'ample. Fet la vol més alta: és la celebració.
    heightRatio: Float = 0.56f,
) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.l)
    val glow = ViraColor.accent(area).copy(alpha = 0.16f)
    Column(modifier.fillMaxWidth()) {
        if (stacked) {
            Image(
                painterResource(scene.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(140.dp).clip(shape),
            )
        } else {
            SceneBanner(
                scene,
                scene.heightRatio(heightRatio),
                Modifier
                    .shadow(14.dp, shape, ambientColor = glow, spotColor = glow)
                    .background(ViraColor.surfaceElevated, shape)
                    .clip(shape),
            )
        }
        val spoken = stringResource(R.string.today_viri_a11y, listOfNotNull(title, subtitle).joinToString(" "))
        Column(
            Modifier
                .padding(horizontal = ViraSpacing.s)
                .then(if (stacked) Modifier.padding(top = ViraSpacing.s) else Modifier.overlapUp(BUBBLE_OVERLAP))
                .fillMaxWidth()
                .shadow(10.dp, shape, ambientColor = glow, spotColor = glow)
                .background(ViraColor.surfaceElevated, shape)
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                .clearAndSetSemantics { contentDescription = spoken; heading() },
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            Text(title, style = ViraFont.title, color = ViraColor.accent(area))
            if (subtitle != null) Text(subtitle, style = ViraFont.body, color = ViraColor.textPrimary)
        }
    }
}

/// Els punts de l'escena que tapa la bombolla.
private val BUBBLE_OVERLAP = 16.dp

/// Puja el que ve a sobre de l'anterior sense deixar-ne el forat a sota.
private fun Modifier.overlapUp(amount: androidx.compose.ui.unit.Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}

/// L'escena quadrada dins d'un bàner `heightRatio` vegades més baix que ample (`ViraSceneBanner`),
/// pujada fins a la finestra on és Viri.
@Composable
private fun SceneBanner(scene: AddScene, heightRatio: Float, modifier: Modifier) {
    val hidden = BUBBLE_OVERLAP
    Layout(
        content = {
            Image(painterResource(scene.image), contentDescription = null, contentScale = ContentScale.Crop)
        },
        modifier = modifier.fillMaxWidth().clipToBounds(),
    ) { measurables, constraints ->
        val side = constraints.maxWidth
        val height = (side * heightRatio).roundToInt()
        val top = scene.top(heightRatio, hidden.toPx() / side.coerceAtLeast(1))
        val image = measurables.first().measure(Constraints.fixed(side, side))
        layout(side, height) { image.place(0, -(top * side).roundToInt()) }
    }
}

/// Un pas sense escena (la medicació, `AddStepHeading`): la pregunta en gran i, a sobre, el nom del
/// que s'està creant si ja n'hi ha.
@Composable
fun AddStepHeading(title: String, eyebrow: String? = null, area: ViraArea = ViraArea.Plan) {
    Column(
        Modifier.fillMaxWidth().padding(top = ViraSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (!eyebrow.isNullOrEmpty()) Text(eyebrow, style = ViraFont.footnoteEmphasis, color = ViraColor.accent(area))
        Text(
            title,
            style = ViraFont.largeTitle,
            color = ViraColor.accent(area),
            modifier = Modifier.clearAndSetSemantics { contentDescription = title; heading() },
        )
    }
}
