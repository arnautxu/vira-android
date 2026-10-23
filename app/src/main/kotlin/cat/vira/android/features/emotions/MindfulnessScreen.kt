package cat.vira.android.features.emotions

import cat.vira.android.core.design.components.ViraSceneBanner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.viraIdleBreath
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
import cat.vira.android.features.entry.ViraTip
import cat.vira.android.features.entry.ViraTipRow

/// Mindfulness: Viri et convida a respirar i, a sota, les tres categories (`MindfulnessView`).
///
/// Cada categoria té tres exercicis guiats amb veu (`MindfulnessCategoryContent`). «Respira 1 min»
/// és la respiració d'Ajuda ara, sense veu: la drecera curta; la versió guiada és a Ansietat.
@Composable
internal fun MindfulnessContent(
    onBreathe: () -> Unit,
    onCategory: (MindfulnessCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = ViraSpacing.xs, bottom = viraTabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        MindfulnessHero(onBreathe)
        ViraTipRow(ViraTip.MINDFULNESS)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap)) {
            Text(
                stringResource(R.string.emotions_mindfulness_need),
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            MindfulnessCategory.entries.forEach { category -> CategoryCard(category) { onCategory(category) } }
        }
        MindfulnessDisclaimer()
    }
}

/// Les tres categories, cadascuna amb una escena del món de Viri que diu de què va.
enum class MindfulnessCategory(@StringRes val title: Int, @StringRes val subtitle: Int, @DrawableRes val scene: Int) {
    ANXIETY(R.string.emotions_mindfulness_category_anxiety, R.string.emotions_mindfulness_category_anxiety_subtitle, R.drawable.viri_suggest_mindfulness_2),
    FOCUS(R.string.emotions_mindfulness_category_focus, R.string.emotions_mindfulness_category_focus_subtitle, R.drawable.viri_suggest_focus_2),
    DISCONNECT(R.string.emotions_mindfulness_category_disconnect, R.string.emotions_mindfulness_category_disconnect_subtitle, R.drawable.viri_suggest_wind_down_1),
}

/// Mòdul protagonista: escena a sang, bombolla que en surt i el botó de respirar.
@Composable
private fun MindfulnessHero(onBreathe: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Emotions), shape)) {
        ViraSceneBanner(R.drawable.viri_suggest_mindfulness_1, heightRatio = 0.66f, hiddenBottom = 16.dp)
        Column(Modifier.padding(ViraSpacing.m), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            MindfulnessBubble(stringResource(R.string.emotions_mindfulness_hero_bubble))
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(
                    stringResource(R.string.emotions_mindfulness_hero_title),
                    style = ViraFont.title,
                    color = ViraColor.onCard(ViraArea.Emotions),
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.emotions_mindfulness_subtitle), style = ViraFont.body, color = ViraColor.onCard(ViraArea.Emotions))
            }
            ViraActionButton(stringResource(R.string.emotions_mindfulness_breathe), onBreathe, Modifier.fillMaxWidth())
        }
    }
}

/// Fitxa d'una categoria: escena, nom, per a quan és i, en píndoles, quants exercicis té i quant
/// duren. Amb lletra
/// d'accessibilitat l'escena passa a sobre perquè el nom no quedi en una columna estreta.
@Composable
private fun CategoryCard(category: MindfulnessCategory, onClick: () -> Unit) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.l)
    val scene = @Composable {
        Image(
            painterResource(category.scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = (if (large) Modifier.fillMaxWidth().height(140.dp) else Modifier.size(96.dp))
                .clip(RoundedCornerShape(ViraRadius.m)),
        )
    }
    val text = @Composable { mod: Modifier ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(stringResource(category.title), style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(stringResource(category.subtitle), style = ViraFont.footnote, color = ViraColor.textSecondary)
            Row(Modifier.padding(top = ViraSpacing.xxs), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                MindfulnessPill(stringResource(R.string.mindfulness_exercises_count, category.exercises.size))
                MindfulnessPill(category.minutesRange())
            }
        }
    }
    val card = Modifier
        .fillMaxWidth()
        .heightIn(min = ViraTouch.comfortable)
        .shadow(3.dp, shape, ambientColor = ViraColor.textPrimary.copy(alpha = 0.05f), spotColor = ViraColor.textPrimary.copy(alpha = 0.05f))
        .background(ViraColor.surfaceElevated, shape)
        .viraPressable(onClick = onClick)
        .padding(ViraSpacing.xs)
    if (large) {
        Column(card, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { scene(); text(Modifier.fillMaxWidth()) }
    } else {
        Row(card, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            scene()
            text(Modifier.weight(1f))
            EmotionGlyphIcon(EmotionGlyph.Chevron, ViraColor.textSecondary, 16.dp * LocalDensity.current.fontScale)
        }
    }
}

/// Puja una peça cap amunt i s'emporta amb ella el que ve a sota (el `padding` negatiu de SwiftUI):
/// un `offset` la mouria, però deixaria el forat on era.
internal fun Modifier.pullUp(amount: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}
