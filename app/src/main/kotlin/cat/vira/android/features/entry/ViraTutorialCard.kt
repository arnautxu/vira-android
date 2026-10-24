package cat.vira.android.features.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// La targeta del tomb (`ViraTutorialCard.swift`, Arnau, 2026-10-21): blanca com les de l'app, amb
/// «2 de 3» i la barra a dalt, el títol, **una frase per número** (els mateixos números que porten
/// les peces il·luminades), el botó «Següent» i «Salta el tomb».
///
/// - **Viri fa de guia** des de la cantonada de dalt a la dreta: és l'única aparició expressiva de
///   la pantalla. Amb lletra gran no hi és: el text mana i necessita tota l'amplada.
/// - **La pregunta del principi** porta una escena sencera a dalt i el mapa de les quatre
///   pestanyes, i les seves sortides són «Som-hi» i «Ara no».
/// - Si el text no hi cap, el cos es desplaça i els botons es queden fixos a baix.
///
/// Viri i l'escena **no** es vesteixen amb la skin: són il·lustracions del tomb.
@Composable
fun ViraTutorialCard(
    step: ViraTutorialStep,
    route: ViraTutorialRoute,
    spots: List<ViraTutorialSpot>,
    prefs: EntryPreferences,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showsArt = !LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.l)
    val guideSide = 96.dp
    // Cada parada mou TalkBack al contingut nou.
    val focus = remember { FocusRequester() }
    LaunchedEffect(step) { runCatching { focus.requestFocus() } }

    Box(modifier.padding(horizontal = ViraSpacing.screenMargin)) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.25f), spotColor = ViraColor.shadow.copy(alpha = 0.25f))
                .background(ViraColor.surfaceElevated, shape)
                .clip(shape)
                .focusRequester(focus)
                .focusable()
                .padding(horizontal = ViraSpacing.m)
                .padding(top = if (step == ViraTutorialStep.INTRO && showsArt) 0.dp else ViraSpacing.m, bottom = ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                ViraTutorialCardBody(step, route, spots, prefs, showsArt, guideSide)
            }
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                EntryPrimaryButton(stringResource(route.action(step)), onNext)
                if (step == ViraTutorialStep.INTRO) {
                    EntryLinkButton(stringResource(R.string.action_not_now), onSkip)
                } else if (!route.isLast(step) && route.stops.size > 1) {
                    EntryLinkButton(stringResource(R.string.tutorial_skip), onSkip)
                }
            }
        }
        val guide = step.guide
        if (showsArt && guide != null) {
            Image(
                painterResource(guide),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -ViraSpacing.xs, y = -guideSide * 0.55f)
                    .size(guideSide)
                    .clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun ViraTutorialCardBody(
    step: ViraTutorialStep,
    route: ViraTutorialRoute,
    spots: List<ViraTutorialSpot>,
    prefs: EntryPreferences,
    showsArt: Boolean,
    guideSide: androidx.compose.ui.unit.Dp,
) {
    if (step == ViraTutorialStep.INTRO) {
        if (showsArt) {
            // La pregunta porta l'escena a sang, enganxada a dalt de la targeta.
            Image(
                painterResource(R.drawable.viri_scene_calendar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .layoutBleed(ViraSpacing.m)
                    .height(150.dp)
                    .clearAndSetSemantics { },
            )
        }
    } else if (route.stops.size > 1) {
        route.position(step)?.let { ViraTutorialProgress(it, route.stopCount) }
    }
    Text(
        stringResource(step.title),
        style = ViraFont.title,
        color = ViraColor.textPrimary,
        // Que el títol no passi per sota de Viri.
        modifier = Modifier
            .padding(end = if (showsArt && step.guide != null) guideSide * 0.6f else 0.dp)
            .semantics { heading() },
    )
    step.message?.let { Text(stringResource(it), style = ViraFont.body, color = ViraColor.textSecondary) }
    spots.forEach { ViraTutorialNumberedLine(it.number, it.line) }
    val note = step.note
    if (note != null && spots.isNotEmpty()) Text(stringResource(note), style = ViraFont.body, color = ViraColor.textPrimary)
    when (step) {
        ViraTutorialStep.INTRO -> ViraTutorialTabMap()
        ViraTutorialStep.INTEGRATIONS -> {
            EntryConnectionToggles(prefs)
            Text(stringResource(R.string.tutorial_integrations_outside), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        else -> Unit
    }
}

/// «2 de 3» i una barra curta. Per a TalkBack, «Pas 2 de 3».
@Composable
private fun ViraTutorialProgress(position: Int, total: Int) {
    val label = stringResource(R.string.onboarding_progress, position, total)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
    ) {
        EntryProgressCount(position, total, header = true)
        Box(Modifier.width(120.dp).height(6.dp).background(ViraColor.surface(ViraArea.Today), CircleShape)) {
            Box(
                Modifier
                    .width(120.dp * (position.toFloat() / total))
                    .height(6.dp)
                    .background(ViraColor.accent(ViraArea.Today), CircleShape),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/// Una línia numerada de la targeta: el número dins del cercle i la frase. Un sol element per a
/// TalkBack, que diu el número i la frase.
@Composable
fun ViraTutorialNumberedLine(number: Int, line: ViraTutorialLine) {
    val text = line.argument?.let { stringResource(line.text, it) } ?: stringResource(line.text)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
    ) {
        ViraTutorialBadge(number, side = 24.dp, scales = true)
        Text(text, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
    }
}
