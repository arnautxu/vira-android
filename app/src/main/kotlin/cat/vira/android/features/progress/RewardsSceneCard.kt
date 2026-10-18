package cat.vira.android.features.progress

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.ui.overlapTop

/// Mòdul protagonista de Reptes i Els meus premis (`RewardsSceneCard`, `DESIGN_RULES.md` §3 i §5):
/// una escena de Viri a dalt, la bombolla que en surt i, a sota, el resum sobre la targeta de color.
///
/// El text va a la bombolla o a la targeta, mai sobre la il·lustració. La frase de Viri és ≤ 15
/// paraules i parla del que fa l'usuari en segona persona (`CONTENT_STRATEGY.md` §2.5).
///
/// També la fan servir altres àrees (el calendari buit de Pla): `area` en tria la targeta, sempre
/// amb els tokens de l'àrea i cap color nou. El contingut s'hi escriu amb el text `onCard` de l'àrea.
@Composable
fun RewardsSceneCard(
    @DrawableRes scene: Int,
    message: String,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Rewards,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    // Prou alta perquè Viri es vegi sencer; amb lletra d'accessibilitat cedeix lloc al text.
    val sceneHeight = if (LocalViraEnvironment.current.isAccessibilitySize) 120.dp else 184.dp
    val accent = ViraColor.accent(area)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = accent.copy(alpha = 0.18f), spotColor = accent.copy(alpha = 0.18f))
            .clip(shape)
            .background(ViraGradient.card(area)),
    ) {
        Image(
            painter = painterResource(scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(sceneHeight),
        )
        Column(
            Modifier.padding(start = ViraSpacing.m, end = ViraSpacing.m, bottom = ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            // Trepitja la vora de l'escena: es llegeix com que surt de Viri.
            ViriBubble(message, area, Modifier.overlapTop(ViraSpacing.xxl))
            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(color = ViraColor.onCard(area))) {
                content()
            }
        }
    }
}

/// La bombolla de Viri: placa blanca amb la frase, l'única aparició expressiva de la pantalla.
@Composable
internal fun ViriBubble(message: String, area: ViraArea, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val accent = ViraColor.accent(area)
    val spoken = stringResource(R.string.today_viri_a11y, message)
    Text(
        message,
        style = ViraFont.headline,
        color = ViraColor.textPrimary,
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = accent.copy(alpha = 0.16f), spotColor = accent.copy(alpha = 0.16f))
            .background(ViraColor.surfaceElevated, shape)
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
            .clearAndSetSemantics { contentDescription = spoken },
    )
}

/// Píndola blanca de la targeta (`RewardsPill`, `DESIGN_RULES.md` §4): sempre amb text, mai només
/// color.
@Composable
fun RewardsPill(text: String, modifier: Modifier = Modifier, showsCoin: Boolean = false) {
    Row(
        modifier = modifier
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        if (showsCoin) ViraPictogram(R.drawable.picto_shop, size = 18.dp, scalesWithText = true)
        Text(text, style = ViraFont.footnoteEmphasis, color = ViraColor.textPrimary)
    }
}

/// Barra de progrés amb el compte sempre en text al costat o a sota (mai només la barra).
/// `onCard`: sobre la targeta de color, pista i farciment amb la tinta de la targeta.
@Composable
fun RewardsProgressBar(fraction: Float, modifier: Modifier = Modifier, onCard: Boolean = false) {
    val clamped = fraction.coerceIn(0f, 1f)
    val ink = ViraColor.onCard(ViraArea.Rewards)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(if (onCard) ink.copy(alpha = 0.18f) else ViraColor.surface(ViraArea.Rewards)),
    ) {
        if (clamped > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .then(
                        if (onCard) Modifier.background(ink)
                        else Modifier.background(ViraGradient.accent(ViraArea.Rewards, ViraGradient.Axis.Horizontal)),
                    ),
            )
        }
    }
}
