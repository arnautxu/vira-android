package cat.vira.android.features.emotions

import cat.vira.android.core.design.components.ViraSceneBanner
import cat.vira.android.core.sound.ViraHaptics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Una categoria de Mindfulness: l'escena de Viri a dalt i els seus tres exercicis, cadascun amb
/// nom, durada i una línia del que fa (`MindfulnessCategoryView`, `USER_FLOWS.md` F15).
@Composable
internal fun MindfulnessCategoryContent(
    category: MindfulnessCategory,
    onExercise: (MindfulnessExercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = viraTabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        ViraPageTitle(stringResource(category.title), ViraArea.Emotions)
        CategoryHero(category)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap)) {
            Text(
                stringResource(R.string.mindfulness_exercises_title),
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            category.exercises.forEachIndexed { index, exercise ->
                ExerciseRow(index + 1, exercise) {
                    ViraHaptics.play(context, ViraHaptics.Feedback.Selection)
                    onExercise(exercise)
                }
            }
        }
        MindfulnessDisclaimer()
    }
}

/// "Sempre amb disclaimer llegible" (`CONTENT_STRATEGY.md`): cos normal, mai `caption`.
@Composable
internal fun MindfulnessDisclaimer() {
    Text(
        stringResource(R.string.emotions_mindfulness_disclaimer),
        style = ViraFont.footnote,
        color = ViraColor.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/// La bombolla de Viri, que trepitja la vora de l'escena: es llegeix com que en surt.
@Composable
internal fun MindfulnessBubble(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Text(
        text,
        style = ViraFont.headline,
        color = ViraColor.textPrimary,
        modifier = modifier
            .pullUp(ViraSpacing.xxl)
            .shadow(8.dp, shape, ambientColor = ViraColor.textPrimary.copy(alpha = 0.12f), spotColor = ViraColor.textPrimary.copy(alpha = 0.12f))
            .background(ViraColor.surfaceElevated, shape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
    )
}

/// Una píndola de dada («3 min», «3 exercicis») sobre la superfície d'Emocions.
@Composable
internal fun MindfulnessPill(text: String, fill: Color = ViraColor.surface(ViraArea.Emotions)) {
    Text(
        text,
        style = ViraFont.footnoteEmphasis,
        color = ViraColor.onSurface(ViraArea.Emotions),
        modifier = Modifier
            .background(fill, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
    )
}

@Composable
private fun CategoryHero(category: MindfulnessCategory) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Emotions), shape)) {
        // Mateixa alçada a les tres categories; la finestra va a buscar Viri a cadascuna.
        ViraSceneBanner(category.scene, heightRatio = 0.65f, hiddenBottom = ViraSpacing.xxl)
        MindfulnessBubble(
            stringResource(R.string.mindfulness_category_bubble),
            Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.m, bottom = ViraSpacing.m),
        )
    }
}

/// Fila d'un exercici. Amb lletra d'accessibilitat el número passa a sobre i el text té tot
/// l'ample.
@Composable
private fun ExerciseRow(number: Int, exercise: MindfulnessExercise, onClick: () -> Unit) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.l)
    val badge = 40.dp * LocalDensity.current.fontScale
    val numberBadge = @Composable {
        Box(Modifier.size(badge).background(ViraColor.surface(ViraArea.Emotions), CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = ViraFont.headline, color = ViraColor.onSurface(ViraArea.Emotions))
        }
    }
    val text = @Composable { mod: Modifier ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(mindfulnessString(exercise.titleKey), style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(mindfulnessString(exercise.summaryKey), style = ViraFont.footnote, color = ViraColor.textSecondary)
            Box(Modifier.padding(top = ViraSpacing.xxs)) {
                MindfulnessPill(stringResource(R.string.mindfulness_minutes, exercise.minutes))
            }
        }
    }
    val card = Modifier
        .fillMaxWidth()
        .heightIn(min = ViraTouch.comfortable)
        .shadow(3.dp, shape, ambientColor = ViraColor.textPrimary.copy(alpha = 0.05f), spotColor = ViraColor.textPrimary.copy(alpha = 0.05f))
        .background(ViraColor.surfaceElevated, shape)
        .viraPressable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {}
        .padding(ViraSpacing.s)
    if (large) {
        Column(card, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { numberBadge(); text(Modifier.fillMaxWidth()) }
    } else {
        Row(card, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            numberBadge()
            text(Modifier.weight(1f))
            EmotionGlyphIcon(EmotionGlyph.Chevron, ViraColor.textSecondary, 16.dp * LocalDensity.current.fontScale)
        }
    }
}
