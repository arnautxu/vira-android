package cat.vira.android.features.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Viri com a assistent, a sobre de «El teu dia» (`TodayViriPill`, Arnau, 2026-10-22). Plegada,
/// una píndola amb la cara de Viri i què toca ara. Quan Viri proposa alguna cosa que la llista no
/// fa (`proposes`) neix oberta, amb el missatge i les respostes; la resta s'obre tocant-la.
///
/// Abans Viri tenia una targeta fixa que repetia la llista: l'Arnau la va trobar «poc útil».
@Composable
internal fun TodayViriPill(moment: TodayViriMomentUi, status: String, modifier: Modifier = Modifier) {
    // Neix oberta o plegada segons el moment, i hi torna quan el moment canvia.
    var isOpen by rememberSaveable(moment.id) { mutableStateOf(moment.proposes) }
    val shape = RoundedCornerShape(ViraRadius.l)
    val shadow = if (isOpen) 12.dp else 6.dp
    val shadowColor = ViraColor.textPrimary.copy(alpha = if (isOpen) 0.08f else 0.04f)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(shadow, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .background(ViraColor.surfaceElevated.copy(alpha = if (isOpen) 1f else 0.72f), shape)
            .padding(start = ViraSpacing.xs, end = ViraSpacing.m, top = ViraSpacing.xs, bottom = if (isOpen) ViraSpacing.s else ViraSpacing.xs)
            .animateContentSize(ViraMotion.standard()),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        PillHeader(isOpen, status) { isOpen = !isOpen }
        // Sense lliscar des de dalt: a mig camí el missatge sortia per sobre de la píndola.
        AnimatedVisibility(isOpen, enter = fadeIn(ViraMotion.standard()), exit = fadeOut(ViraMotion.standard())) {
            PillContent(moment)
        }
    }
}

@Composable
private fun PillHeader(isOpen: Boolean, status: String, onToggle: () -> Unit) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val state = stringResource(if (isOpen) R.string.today_card_expanded else R.string.today_card_collapsed)
    val rotation by animateFloatAsState(if (isOpen) 180f else 0f, ViraMotion.standard(), label = "pillChevron")
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(onClick = onToggle)
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        TodayViriAvatar(if (isLarge) 52.dp else 40.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.today_viri_card_title), style = ViraFont.headline, color = ViraColor.textPrimary)
            if (!isOpen) {
                Text(
                    status,
                    style = ViraFont.footnote,
                    color = ViraColor.textSecondary,
                    maxLines = if (isLarge) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = ViraColor.textSecondary,
            modifier = Modifier.size(20.dp).rotate(rotation),
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PillContent(moment: TodayViriMomentUi) {
    val spoken = stringResource(R.string.today_viri_a11y, "${moment.title} ${moment.message}")
    Column(Modifier.padding(start = ViraSpacing.xxs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Column(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
        ) {
            Text(moment.title, style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(moment.message, style = ViraFont.body, color = ViraColor.textPrimary)
        }
        if (moment.actions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                moment.actions.forEach { TodayViriButton(it) }
            }
        }
    }
}

/// Un botó de Viri sobre la píndola blanca: blanc amb vora lila (`TodayViriButton` amb
/// `outlined`). Càpsula en una línia; amb lletra gran, rectangle arrodonit, que una càpsula de
/// tres línies es torna un ou.
@Composable
private fun TodayViriButton(action: TodayViriMomentUi.Action) {
    val isLarge = LocalViraEnvironment.current.isAccessibilitySize
    val shape = if (isLarge) RoundedCornerShape(ViraRadius.l) else CircleShape
    Row(
        Modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(shape)
            .background(ViraColor.surfaceElevated, shape)
            .border(1.5.dp, ViraColor.brandAccent.copy(alpha = 0.35f), shape)
            .viraPressable(onClick = action.run)
            .padding(horizontal = ViraSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        action.icon?.let { Icon(it, contentDescription = null, tint = ViraColor.brandAccent, modifier = Modifier.size(20.dp)) }
        Text(
            action.title,
            style = ViraFont.headline,
            color = ViraColor.brandAccent,
            maxLines = if (isLarge) Int.MAX_VALUE else 1,
            modifier = Modifier.padding(vertical = ViraSpacing.xxs),
        )
    }
}

/// La cara de Viri en un cercle lila: `viri-widget-afternoon` retallat pel cap i les espatlles, a
/// sang. El punt diu que està de guàrdia; és decoratiu.
@Composable
internal fun TodayViriAvatar(side: Dp) {
    // El quadrat de `viri-widget-afternoon` on hi ha el cap, en fracció del costat.
    val full = side / 0.70f
    Box(Modifier.size(side).clearAndSetSemantics { }) {
        Box(
            Modifier
                .size(side)
                .clip(CircleShape)
                .background(ViraColor.brandAccent.copy(alpha = 0.14f), CircleShape),
        ) {
            Image(
                painterResource(R.drawable.viri_widget_afternoon),
                contentDescription = null,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(full)
                    .offset(x = -full * 0.255f, y = -full * 0.108f),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(1.dp, 1.dp)
                .size(side * 0.26f)
                .background(ViraColor.brandAccent, CircleShape)
                .border(2.dp, ViraColor.surfaceElevated, CircleShape),
        )
    }
}
