package cat.vira.android.features.parent

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// El mòdul protagonista del panell (`ParentViriCard` de l'iPhone, `DESIGN_RULES.md` §3 i §5):
/// Viri en una targeta lila amb la seva frase dins d'una placa blanca, mai sobre el degradat ni
/// sobre la il·lustració.
///
/// Una sola aparició per pantalla i una sola frase curta. Als pares Viri no els fa cap broma ni
/// parla del fill o la filla: explica què hi ha a la pantalla.
@Composable
internal fun ParentViriCard(line: String, @DrawableRes pose: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val bubble: @Composable (Modifier) -> Unit = { bubbleModifier ->
        Text(
            line,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = bubbleModifier
                .padding(ViraSpacing.m)
                .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        )
    }
    val viri: @Composable (Modifier) -> Unit = { imageModifier ->
        Image(
            painterResource(pose),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = imageModifier.widthIn(max = 170.dp).padding(top = ViraSpacing.m).offset(y = ViraSpacing.s),
        )
    }
    val card = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(ViraGradient.card(ViraArea.Today), shape)
        .clearAndSetSemantics { contentDescription = line }

    // A mides d'accessibilitat Viri passa a sota: al costat, la bombolla quedava en una columna
    // de dues paraules per línia.
    if (stacked) {
        Column(card, horizontalAlignment = Alignment.End) {
            bubble(Modifier.fillMaxWidth())
            viri(Modifier)
        }
    } else {
        Row(card, verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f).align(Alignment.CenterVertically)) { bubble(Modifier) }
            viri(Modifier.weight(0.8f, fill = false))
        }
    }
}
