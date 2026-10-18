package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/// On és l'XP dins del nivell. El calcula el fil de Viri i ratxa (`ViraLevel.progress`).
data class ViraLevelProgress(
    val level: Int,
    val totalXP: Int,
    val currentLevelXP: Int,
    val xpToNext: Int,
) {
    val fraction: Float
        get() = if (currentLevelXP + xpToNext <= 0) 0f else currentLevelXP.toFloat() / (currentLevelXP + xpToNext)
}

/// Barra de nivell de la pestanya Viri (`LevelBar`). Número pelat, sense nom de rang («Nivell 7»).
///
/// En pujar de nivell la barra **s'omple fins al final**, es buida d'un salt sense animar i torna a
/// omplir-se: mai es desfà cap enrere, perquè l'XP mai baixa (`VIRI_AND_GAMIFICATION.md` §2.2.1).
/// El número canvia quan la barra es buida, no abans. Amb Reduir moviment, directe al valor nou.
///
/// `frame` i `nextFrame` són els marcs de nivell (bronze, plata…) que tria el fil de Viri; sense
/// marc, el pictograma d'XP.
@Composable
fun ViraLevelBar(
    progress: ViraLevelProgress,
    modifier: Modifier = Modifier,
    @DrawableRes frame: Int = R.drawable.picto_xp,
    @DrawableRes nextFrame: Int = R.drawable.picto_level_up,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val shown = remember { Animatable(progress.fraction) }
    var shownLevel by remember { mutableIntStateOf(progress.level) }

    LaunchedEffect(progress) {
        val target = progress.fraction
        when {
            reduceMotion -> { shown.snapTo(target); shownLevel = progress.level }
            progress.level <= shownLevel -> { shownLevel = progress.level; shown.animateTo(target, ViraMotion.settle()) }
            else -> {
                shown.animateTo(1f, ViraMotion.settle())
                delay(120)
                // El buit i el ple són el mateix punt del recorregut: el salt no es veu.
                shown.snapTo(0f)
                shownLevel = progress.level
                shown.animateTo(target, ViraMotion.settle())
            }
        }
    }

    val shape = RoundedCornerShape(ViraRadius.l)
    val trackHeight = 12.dp * LocalDensity.current.fontScale
    val description = stringResource(
        R.string.level_accessibility,
        progress.level,
        (progress.fraction * 100).roundToInt(),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, shape)
            .border(1.dp, ViraColor.separator, shape)
            .padding(ViraSpacing.m)
            // Mai només la barra: TalkBack diu nivell i percentatge en paraules.
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Box(
                modifier = Modifier
                    .background(ViraColor.surface(ViraArea.Rewards), CircleShape)
                    .border(1.dp, ViraColor.accent(ViraArea.Rewards).copy(alpha = 0.32f), CircleShape)
                    .padding(ViraSpacing.xs),
            ) { ViraPictogram(frame, size = 34.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                Text(stringResource(R.string.level_title, shownLevel), style = ViraFont.headline, color = ViraColor.textPrimary)
                Text(stringResource(R.string.metrics_total_xp, progress.totalXP), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
            Text(
                stringResource(R.string.level_progress, progress.currentLevelXP, progress.currentLevelXP + progress.xpToNext),
                style = ViraFont.footnote,
                color = ViraColor.textSecondary,
                textAlign = TextAlign.End,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(ViraColor.surface(ViraArea.Rewards), CircleShape)
                .border(1.dp, ViraColor.onSurface(ViraArea.Rewards).copy(alpha = 0.16f), CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(shown.value.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(ViraGradient.card(ViraArea.Rewards, ViraGradient.Axis.Horizontal), CircleShape),
            )
        }

        // Dos marcs, no una meta: el nivell actual i el següent.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Milestone(frame, progress.level, isCurrent = true, modifier = Modifier.weight(1f))
            Box(Modifier.weight(1f).height(1.dp).background(ViraColor.separator))
            Milestone(nextFrame, progress.level + 1, isCurrent = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Milestone(@DrawableRes frame: Int, level: Int, isCurrent: Boolean, modifier: Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, if (isCurrent) Alignment.Start else Alignment.End),
    ) {
        ViraPictogram(frame, size = 22.dp)
        Text(
            stringResource(R.string.level_title, level),
            style = ViraFont.caption,
            color = if (isCurrent) ViraColor.onSurface(ViraArea.Rewards) else ViraColor.textSecondary,
            maxLines = 1,
        )
    }
}
