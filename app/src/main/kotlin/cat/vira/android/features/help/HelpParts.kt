package cat.vira.android.features.help

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Peces que comparteixen les pantalles d'Ajuda ara (`HelpParts.swift`, redisseny de l'Arnau del
/// 2026-10-10). Tot va en l'àrea d'Inici, com a l'iPhone.

/// Contenidor comú d'una subpantalla d'ajuda (`helpPage` de l'iPhone): barra amb Enrere o Fet,
/// scroll, títol lila i fons d'Inici. Amb `title` nul no hi ha títol gran ni fons tenyit (la
/// pantalla de «Encara no hi és», com `PendingFeatureView`).
@Composable
internal fun HelpPage(
    title: String?,
    onBack: (() -> Unit)? = null,
    backLabel: String = stringResource(R.string.action_back),
    onDone: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (title != null) Modifier.viraSectionBackground(ViraArea.Today) else Modifier.background(ViraColor.backgroundGrouped)
    Column(Modifier.fillMaxSize().then(background)) {
        HelpNavBar(onBack, backLabel, onDone)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = ViraSpacing.screenMargin)
                .padding(bottom = ViraSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
        ) {
            if (title != null) ViraPageTitle(title, ViraArea.Today)
            content()
        }
    }
}

/// La barra de dalt: Enrere a l'esquerra a les subpantalles, Fet a la dreta a l'arrel. Sense
/// títol, com `viraPageChrome`: el títol és el gran de la pàgina.
@Composable
private fun HelpNavBar(onBack: (() -> Unit)?, backLabel: String, onDone: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = ViraTouch.minimum)
            .padding(horizontal = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) HelpBackButton(backLabel, onBack)
        Spacer(Modifier.weight(1f))
        if (onDone != null) {
            ViraActionButton(stringResource(R.string.action_done), onDone, hierarchy = ViraActionHierarchy.Textual)
        }
    }
}

@Composable
private fun HelpBackButton(label: String, onClick: () -> Unit) {
    val ink = ViraColor.accent(ViraArea.Today)
    Row(
        Modifier
            .heightIn(min = ViraTouch.minimum)
            .viraPressable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        val side = 18.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
        Canvas(Modifier.size(side)) {
            // `chevron.backward` del sistema.
            val stroke = size.width * 0.14f
            drawLine(ink, Offset(size.width * 0.68f, size.height * 0.1f), Offset(size.width * 0.28f, size.height * 0.5f), stroke, StrokeCap.Round)
            drawLine(ink, Offset(size.width * 0.28f, size.height * 0.5f), Offset(size.width * 0.68f, size.height * 0.9f), stroke, StrokeCap.Round)
        }
        Text(label, style = ViraFont.body, color = ink)
    }
}

/// Escena de Viri (`DESIGN_RULES.md` §5): degradat lila, la frase dins d'una placa blanca que en
/// surt —mai sobre el degradat ni sobre Viri— i, al peu, el que la pantalla hi vulgui posar.
///
/// Aquí Viri és **neutre**: medita o escriu, diu una sola frase sense humor i no celebra ni
/// reacciona a res del que faci l'usuari. A lletra d'accessibilitat, Viri passa a sota de la frase.
@Composable
internal fun HelpViriScene(
    line: String,
    @DrawableRes image: Int,
    imageWidth: Dp = 170.dp,
    footer: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val spoken = stringResource(R.string.today_viri_a11y, line)
    val plate = @Composable { modifier: Modifier ->
        Text(
            line,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = modifier
                .background(ViraColor.surfaceElevated, shape)
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                .semantics { contentDescription = spoken },
        )
    }
    val viri = @Composable { modifier: Modifier ->
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.widthIn(max = imageWidth),
        )
    }
    Column(
        Modifier.fillMaxWidth().background(ViraGradient.card(ViraArea.Today), shape).padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            plate(Modifier.fillMaxWidth())
            viri(Modifier)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                plate(Modifier.weight(1f))
                viri(Modifier.width(imageWidth))
            }
        }
        if (footer != null) footer()
    }
}

/// Progrés sobre una targeta de color: el text a dalt i la barra a sota (§4, mai només la barra).
@Composable
internal fun HelpProgress(
    leading: String,
    trailing: String,
    fraction: Float,
    leadingDescription: String = leading,
) {
    val ink = ViraColor.onCard(ViraArea.Today)
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$leadingDescription, $trailing" },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(leading, style = ViraFont.footnoteEmphasis, color = ink, modifier = Modifier.weight(1f))
            Text(trailing, style = ViraFont.footnoteEmphasis.copy(fontFeatureSettings = "tnum"), color = ink)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp * LocalDensity.current.fontScale)
                .background(ink.copy(alpha = 0.18f), CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp * LocalDensity.current.fontScale)
                    .background(ink, CircleShape),
            )
        }
    }
}

/// Fila blanca amb pictograma: idees ràpides, passos fets i notes de privacitat.
@Composable
internal fun HelpInfoRow(
    @DrawableRes pictogram: Int,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ViraTouch.comfortable)
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(pictogram, size = 36.dp, scalesWithText = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(title, style = ViraFont.callout, color = ViraColor.textPrimary)
            if (detail != null) Text(detail, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
    }
}

/// Línia de peu en gris, fora de cap targeta.
@Composable
internal fun HelpFootnote(text: String) {
    Text(
        text,
        style = ViraFont.footnote,
        color = ViraColor.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs),
    )
}

/// Capçalera d'un grup («Altres maneres», «Idees ràpides», «Ja fet»).
@Composable
internal fun HelpSectionHeader(text: String) {
    Text(
        text,
        style = ViraFont.headline,
        color = ViraColor.textPrimary,
        modifier = Modifier.padding(top = ViraSpacing.xs).semantics { heading() },
    )
}
