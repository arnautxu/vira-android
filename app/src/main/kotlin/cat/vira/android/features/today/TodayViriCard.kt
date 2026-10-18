package cat.vira.android.features.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraIdleBreath
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec

/// El que diu Viri en un moment concret, i què es pot fer des d'allà (`TodayViriMoment`).
internal data class TodayViriMomentUi(
    /// Canvia quan canvia el missatge, perquè l'entrada s'animi només llavors.
    val id: String,
    val scene: String,
    val title: String,
    val message: String,
    val actions: List<Action> = emptyList(),
) {
    data class Action(val title: String, val icon: ImageVector? = null, val run: () -> Unit)
}

/// Viri et parla a dalt d'Avui (`TodayViriCard`): una escena del món lila, una bombolla amb una o
/// dues frases fetes amb les dades del dia i, com a molt, dos botons. Es pot plegar.
@Composable
internal fun TodayViriCard(
    moment: TodayViriMomentUi,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = ViraColor.brandAccent.copy(alpha = 0.18f), spotColor = ViraColor.brandAccent.copy(alpha = 0.18f))
            .clip(shape)
            .background(ViraColor.surface(ViraArea.Today), shape)
            .padding(horizontal = ViraSpacing.s),
    ) {
        TodayCardDisclosure(isExpanded, onExpandedChange, ink = ViraColor.brandAccent) {
            Text(
                stringResource(R.string.today_viri_card_title),
                style = ViraFont.headline,
                color = ViraColor.brandAccent,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(isExpanded) {
            AnimatedContent(
                targetState = moment,
                contentKey = { it.id },
                transitionSpec = {
                    (fadeIn(viraSpec(ViraMotion.entrance())) + scaleIn(viraSpec(ViraMotion.entrance()), initialScale = 0.97f)) togetherWith
                        fadeOut(viraSpec(ViraMotion.standard()))
                },
                label = "viriMoment",
            ) { shown -> MomentContent(shown) }
        }
    }
}

@Composable
private fun MomentContent(moment: TodayViriMomentUi) {
    val sceneHeight = if (LocalViraEnvironment.current.isAccessibilitySize) 120.dp else 168.dp
    Column {
        Image(
            painter = painterResource(todayDrawable(moment.scene, R.drawable.viri_suggest_capture_1)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight)
                .clip(RoundedCornerShape(0.dp))
                .viraIdleBreath(),
        )
        Column(
            Modifier.padding(start = ViraSpacing.s, end = ViraSpacing.s, bottom = ViraSpacing.s),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Bubble(moment, Modifier.offset(y = -ViraSpacing.xxl))
            if (moment.actions.isNotEmpty()) {
                FlowRow(
                    Modifier.offset(y = -ViraSpacing.xxl).padding(bottom = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                ) {
                    moment.actions.forEachIndexed { index, action -> ViriButton(action, isPrimary = index == 0) }
                }
            }
        }
    }
}

@Composable
private fun Bubble(moment: TodayViriMomentUi, modifier: Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val spoken = stringResource(R.string.today_viri_a11y, "${moment.title} ${moment.message}")
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = ViraColor.brandAccent.copy(alpha = 0.14f), spotColor = ViraColor.brandAccent.copy(alpha = 0.14f))
            .background(ViraColor.surfaceElevated, shape)
            .border(1.dp, ViraColor.brandAccent.copy(alpha = 0.10f), shape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
    ) {
        Text(moment.title, style = ViraFont.title, color = ViraColor.brandAccent)
        Text(moment.message, style = ViraFont.body, color = ViraColor.textPrimary)
    }
}

/// Un botó «gras» de la bombolla: el principal ple de lila, el secundari blanc.
@Composable
private fun ViriButton(action: TodayViriMomentUi.Action, isPrimary: Boolean) {
    val ink = if (isPrimary) ViraColor.onAccent else ViraColor.brandAccent
    val fill = if (isPrimary) Modifier.background(ViraGradient.accent(ViraArea.Today, ViraGradient.Axis.Vertical), CircleShape)
    else Modifier.background(ViraColor.surfaceElevated, CircleShape)
    Row(
        Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .shadow(6.dp, CircleShape, ambientColor = ViraColor.brandAccent.copy(alpha = if (isPrimary) 0.28f else 0.12f), spotColor = ViraColor.brandAccent.copy(alpha = if (isPrimary) 0.28f else 0.12f))
            .clip(CircleShape)
            .then(fill)
            .viraPressable(onClick = action.run)
            .padding(horizontal = ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        action.icon?.let { Icon(it, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp)) }
        Text(action.title, style = ViraFont.headline, color = ink, maxLines = 1)
    }
}

/// Capçalera plegable de les targetes d'Avui (`TodayCardDisclosureButton`): el títol, «Obre» o
/// «Plega» en text i la fletxa. TalkBack en diu l'estat.
@Composable
internal fun TodayCardDisclosure(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    ink: androidx.compose.ui.graphics.Color,
    label: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val state = stringResource(if (isExpanded) R.string.today_card_expanded else R.string.today_card_collapsed)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable { onExpandedChange(!isExpanded) }
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        label()
        Text(
            stringResource(if (isExpanded) R.string.today_card_collapse else R.string.today_card_expand),
            style = ViraFont.caption,
            color = ink,
        )
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = ink,
            )
        }
    }
}
