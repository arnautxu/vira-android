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
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Peces visuals del flux d'entrada (`EntryFlowChrome.swift`).
///
/// ## Tractament (Arnau, 2026-10-21)
/// - **Viri té casa**: cada pas porta una escena sencera del món lila (`EntryScene`), a sang i
///   sota la capçalera, en lloc d'un retall sobre un degradat.
/// - **La fulla és blanca** (`surfaceElevated`, fosca en mode fosc), amb `textPrimary` i
///   `textSecondary`. El groc torna a ser només dels premis.
/// - El que no es negocia continua igual: **cap text sobre la il·lustració**. La frase de Viri va
///   en una bombolla (`EntryViriLine`), i els botons de la capçalera són superfícies.

/// El logotip horitzontal, **lila dins de la fulla** de la benvinguda. Porta nom per a TalkBack:
/// aquí és l'única cosa que diu de qui és l'app abans d'entrar-hi.
@Composable
fun EntryLogo(modifier: Modifier = Modifier, width: Dp = 96.dp) {
    Image(
        painter = painterResource(R.drawable.vira_logotype),
        contentDescription = "Vira",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(ViraColor.accent(ViraArea.Today)),
        modifier = modifier.width(width),
    )
}

/// Targeta blanca solta: la fan servir les captures de cada pas, que l'ensenyen sense l'escena.
@Composable
fun EntryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.18f), spotColor = ViraColor.shadow.copy(alpha = 0.18f))
            .background(ViraColor.surfaceElevated, shape)
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        content = content,
    )
}

/// Quant tapa la fulla l'escena per sota: prou perquè les cantonades arrodonides es vegin sobre la
/// il·lustració i no sobre un forat.
val EntrySheetOverlap = 30.dp

/// La fulla del flux, **enganxada al peu**, amb les dues cantonades de dalt arrodonides. Blanca en
/// clar i `surfaceElevated` fosc en fosc, com qualsevol full de l'app.
@Composable
fun Modifier.entrySheetBackground(): Modifier {
    val radius = ViraRadius.l + ViraSpacing.s
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    return this
        .shadow(16.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.12f), spotColor = ViraColor.shadow.copy(alpha = 0.12f))
        .background(ViraColor.surfaceElevated, shape)
}

/// Títol i missatge del pas, a dalt de la fulla.
@Composable
fun EntryHeading(title: String, message: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(title, style = ViraFont.title, color = ViraColor.textPrimary, modifier = Modifier.semantics { heading() })
        if (message != null) {
            Text(message, style = ViraFont.body, color = ViraColor.textSecondary)
        }
    }
}

/// Botó principal del pas: el lila d'acció de sempre.
///
/// Amb `isLoading` el text cedeix el lloc a un indicador, però **es queda** com a nom per a
/// TalkBack: el botó segueix dient què fa mentre ho fa. Desactivat, gris de text: el blanc sobre
/// el gris quasi no es veia al full blanc.
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

/// Enllaç secundari dins de la fulla ("Ara no", "Ja tinc compte", "Sóc pare o mare", "Obre
/// Ajustos"): `textSecondary` en negreta. És la sortida discreta; el botó lila ja marca on és
/// l'acció. A lletra gran fa dues línies abans que tallar-se.
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
            color = ViraColor.textSecondary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

/// Acció secundària a tota amplada ("Amb el correu"): contorn lila i text lila, sense farciment.
/// Pesa menys que el botó principal i més que un enllaç.
@Composable
fun EntrySecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.m)
    val accent = ViraColor.accent(ViraArea.Today)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .border(1.5.dp, accent, shape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.m),
    ) {
        Text(text, style = ViraFont.headline, color = accent, textAlign = TextAlign.Center)
    }
}

/// Píndola `surfaceElevated` amb text `textPrimary` en negreta: les accions de la capçalera
/// ("Saltar", "Ara no", "Entra"). Sempre una superfície sobre l'escena, i mai es talla.
@Composable
fun EntryCapsuleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .clip(CircleShape)
            .background(ViraColor.surfaceElevated, CircleShape)
            .viraPressable(onClick = onClick)
            .padding(horizontal = ViraSpacing.m),
    ) {
        Text(text, style = ViraFont.callout.copy(fontWeight = FontWeight.SemiBold), color = ViraColor.textPrimary, maxLines = 1, softWrap = false)
    }
}

/// Botó enrere de la capçalera: el chevron `textPrimary` dins d'un cercle `surfaceElevated` de 48 dp,
/// com «Saltar» a l'altra banda.
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
        EntryGlyphIcon(EntryGlyph.BACK, ViraColor.textPrimary, side = 20.dp)
    }
}

/// Contorn d'un camp dins de la fulla: el traç és el que diu on comença el control
/// (`controlBorder`, ≥3:1 sobre la fulla en clar i en fosc).
@Composable
fun Modifier.entryFieldBorder(): Modifier = border(1.5.dp, ViraColor.controlBorder, RoundedCornerShape(ViraRadius.m))

/// Fons d'una fila agrupada dins de la fulla (interruptors, la vista prèvia de l'avís): el gris de
/// les llistes de l'app, sense contorn.
@Composable
fun Modifier.entryGroupedRow(): Modifier = background(ViraColor.backgroundGrouped, RoundedCornerShape(ViraRadius.m))

/// Marc dels passos ensenyats sols (captures): el pas dins d'una targeta blanca.
@Composable
fun EntryStepPreview(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().background(ViraColor.backgroundGrouped)) {
        EntryCard(Modifier.padding(ViraSpacing.screenMargin), content = content)
    }
}
