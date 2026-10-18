package cat.vira.android.features.emotions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// En acabar un exercici: Viri amb la tassa i una frase, quant ha durat i dues sortides
/// (`MindfulnessFinishedView`).
///
/// Cap XP, moneda ni ratxa: el que s'ha fet no es puntua. Les sortides són les que tenen sentit
/// després de parar: dir com estàs, o fer-ne un altre. «Prepara't» acaba a la porta de la feina,
/// i el seu botó principal és Concentra't.
@Composable
internal fun MindfulnessFinished(
    exercise: MindfulnessExercise,
    steps: Int,
    onAnother: () -> Unit,
    onCheckIn: () -> Unit,
    onFocus: () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val artHeight = (260.dp * LocalDensity.current.fontScale).coerceAtMost(300.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Emotions), shape)) {
        Image(
            painterResource(R.drawable.viri_suggest_wind_down_2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(artHeight),
        )
        Column(Modifier.padding(ViraSpacing.m), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            val bubble = stringResource(R.string.mindfulness_done_bubble)
            val a11y = stringResource(R.string.today_viri_a11y, bubble)
            MindfulnessBubble(bubble, Modifier.semantics { contentDescription = a11y })
            Text(
                mindfulnessString(exercise.titleKey),
                style = ViraFont.title,
                color = ViraColor.onCard(ViraArea.Emotions),
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                MindfulnessPill(stringResource(R.string.mindfulness_minutes, exercise.minutes), ViraColor.surfaceElevated)
                if (steps > 1) MindfulnessPill(stringResource(R.string.mindfulness_step_progress, steps, steps), ViraColor.surfaceElevated)
            }
            Column(Modifier.padding(top = ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                if (exercise.leadsToFocus) {
                    ViraActionButton(stringResource(R.string.mindfulness_focus), onFocus, Modifier.fillMaxWidth())
                } else {
                    ViraActionButton(stringResource(R.string.mindfulness_check_in), onCheckIn, Modifier.fillMaxWidth())
                }
                ViraActionButton(
                    stringResource(R.string.mindfulness_again), onAnother, Modifier.fillMaxWidth(),
                    hierarchy = ViraActionHierarchy.Secondary,
                )
            }
        }
    }
}
