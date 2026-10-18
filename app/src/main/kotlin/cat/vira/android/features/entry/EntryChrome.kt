package cat.vira.android.features.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Peces visuals del flux d'entrada (`EntryFlowChrome.swift`).
///
/// El tractament és el de marca: degradat lila fort a tota pantalla, Viri gran a sobre d'una
/// **fulla groga** enganxada al peu, i tot el text i els controls dins de la fulla. La regla que no
/// es relaxa: **cap text seu directament sobre el degradat ni sobre la il·lustració**, i mai blanc
/// sobre groc (`onCard(Rewards)`, el marró fosc del sistema). El botó d'acció segueix sent lila.

/// Degradat de marca a pantalla completa: l'escenari de tots els passos.
@Composable
fun EntryBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(ViraGradient.card(ViraArea.Today)))
}

/// El logotip horitzontal, blanc sobre el degradat. Porta nom per a TalkBack: aquí és l'única cosa
/// que diu de qui és l'app abans d'entrar-hi.
@Composable
fun EntryLogo(modifier: Modifier = Modifier, width: Dp = 148.dp) {
    Image(
        painter = painterResource(R.drawable.vira_logotype),
        contentDescription = "Vira",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(ViraColor.lightArtworkVeil),
        modifier = modifier.width(width),
    )
}

/// La pastilla groga, amb el mateix arrodoniment a les quatre cantonades. Es queda per als passos
/// ensenyats sols (captures) i per al tutorial.
@Composable
fun EntryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.18f), spotColor = ViraColor.shadow.copy(alpha = 0.18f))
            .background(ViraGradient.card(ViraArea.Rewards), shape)
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        content = content,
    )
}

/// La fulla groga del flux, **enganxada al peu** (Arnau, 2026-10-10): només té arrodonides les dues
/// cantonades de dalt, i Viri hi seu a sobre.
@Composable
fun Modifier.entrySheetBackground(): Modifier {
    val radius = ViraRadius.l + ViraSpacing.s
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    return this
        .shadow(20.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.18f), spotColor = ViraColor.shadow.copy(alpha = 0.18f))
        .background(ViraGradient.card(ViraArea.Rewards), shape)
}

/// Títol i missatge del pas, **dins** de la fulla.
@Composable
fun EntryHeading(title: String, message: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(title, style = ViraFont.title, color = ViraColor.onCard(ViraArea.Rewards), modifier = Modifier.semantics { heading() })
        if (message != null) {
            Text(message, style = ViraFont.body, color = ViraColor.onCard(ViraArea.Rewards))
        }
    }
}

/// Botó principal del pas. Lila: el groc és per a superfícies i el lila per a accions.
///
/// Amb `isLoading` el text cedeix el lloc a un indicador, però **es queda** com a nom per a
/// TalkBack: el botó segueix dient què fa mentre ho fa.
@Composable
fun EntryPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val active = enabled && !isLoading
    val shape = RoundedCornerShape(ViraRadius.s)
    val fill = if (enabled || isLoading) ViraColor.accent(ViraArea.Today) else ViraColor.separator
    val ink = if (enabled || isLoading) ViraColor.onAccent else ViraColor.textSecondary
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .background(fill, shape)
            .viraPressable(enabled = active, onClick = onClick)
            .semantics {
                contentDescription = text
                if (!active) disabled()
            }
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
    ) {
        Text(text, style = ViraFont.callout, color = ink, textAlign = TextAlign.Center, modifier = Modifier.alpha(if (isLoading) 0f else 1f))
        if (isLoading) {
            CircularProgressIndicator(color = ViraColor.onAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        }
    }
}

/// Enllaç secundari dins de la fulla ("Ara no", "Obre Ajustos"). Va en `onCard(Rewards)`, **no** en
/// lila: el lila sobre el groc queda per sota del 4,5:1, i el botó principal ja marca on és l'acció.
@Composable
fun EntryLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, centered: Boolean = true) {
    Box(
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
        modifier = modifier
            .then(if (centered) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(onClick = onClick),
    ) {
        Text(
            text,
            style = ViraFont.callout.copy(fontWeight = FontWeight.Bold),
            color = ViraColor.onCard(ViraArea.Rewards),
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

/// Acció secundària **sobre el degradat** ("Ja tinc compte"): píndola groga plena amb text fosc. El
/// text blanc pelat sobre el lila només hi donaria 3,1:1.
@Composable
fun EntrySecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(CircleShape)
            .background(ViraGradient.card(ViraArea.Rewards), CircleShape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.m),
    ) {
        Text(text, style = ViraFont.headline, color = ViraColor.onCard(ViraArea.Rewards), textAlign = TextAlign.Center)
    }
}

/// Píndola `surfaceElevated` amb text `accent(Today)` (5,51:1): les accions de la capçalera i
/// l'enllaç "Sóc pare o mare". Sempre sobre una superfície, mai text pelat sobre el degradat.
@Composable
fun EntryCapsuleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, bold: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated, CircleShape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.m),
    ) {
        Text(
            text,
            style = ViraFont.callout.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
            color = ViraColor.accent(ViraArea.Today),
        )
    }
}

/// Botó enrere de la capçalera: el chevron dins d'un cercle `surfaceElevated` de 48 dp.
@Composable
fun EntryBackButton(onClick: () -> Unit, label: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated, CircleShape)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        EntryGlyphIcon(EntryGlyph.BACK, ViraColor.accent(ViraArea.Today), side = 20.dp)
    }
}

/// Contorn de qualsevol control dins de la fulla: sobre el groc, un farciment clar no es
/// distingiria; el que els separa és el traç.
@Composable
fun Modifier.entryFieldBorder(): Modifier = border(
    1.5.dp,
    ViraColor.controlBorder(on = ViraColor.onCard(ViraArea.Rewards)),
    RoundedCornerShape(ViraRadius.m),
)

/// Marc dels passos ensenyats sols (captures): el pas dins de la pastilla, sobre el degradat.
@Composable
fun EntryStepPreview(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        EntryBackdrop(Modifier.matchParentSize())
        EntryCard(Modifier.padding(ViraSpacing.screenMargin), content = content)
    }
}
