package cat.vira.android.features.progress

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.entry.ViraTutorialBadge

/// «Aquí és Viri», el primer cop que s'obre la pestanya (`ViriTabTip.swift`, Arnau, 2026-10-22).
///
/// Era una parada del tomb. Ara Viri s'explica aquí, **quan s'hi entra**: nivell, ratxa i botiga,
/// un número per cosa i «Entesos». Surt un sol cop per telèfon (`settings.viriTabTipSeen`) i
/// només un cop passat el tomb: mentre el tomb hi passa per sobre, la targeta s'espera.
///
/// Amb el progrés o la botiga amagats a Ajustos, la seva línia no hi és: no s'explica el que no
/// es veu. El Viri de la cantonada és el guia del tomb, no el de l'usuari: no es vesteix.
@Composable
fun rememberViriTabTipVisible(): Boolean {
    val preferences = LocalViraBackend.current.preferences
    val seen by rememberPreference(preferences, VIRI_TAB_TIP_SEEN, false)
    val tutorialCompleted by rememberPreference(preferences, ViraPreferences.TUTORIAL_COMPLETED, false)
    return !seen && tutorialCompleted
}

/// La cortina i la targeta. Qui la posa amaga a TalkBack el que queda a sota mentre `visible`.
@Composable
fun BoxScope.ViriTabTip(visible: Boolean) {
    val preferences = LocalViraBackend.current.preferences
    val progressVisible by rememberPreference(preferences, PROGRESS_VISIBLE, true)
    val shopVisible by rememberPreference(preferences, SHOP_VISIBLE, true)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // La mateixa cortina que el tomb, que no deixa tocar el que hi ha a sota.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ViraColor.shadow.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .clearAndSetSemantics { },
            )
            ViriTabTipCard(
                lines = viriTabTipLines(progressVisible, shopVisible),
                onDone = { preferences.edit().putBoolean(VIRI_TAB_TIP_SEEN, true).apply() },
            )
        }
    }
}

/// Les línies que toquen, en ordre (`ViriTabTip.lineKeys`). Separat perquè es pugui provar.
fun viriTabTipLines(progressVisible: Boolean, shopVisible: Boolean): List<Int> = buildList {
    if (progressVisible) add(R.string.viri_tip_level)
    add(R.string.viri_tip_streak)
    if (shopVisible) add(R.string.viri_tip_shop)
}

/// La targeta, a part perquè es pugui veure a les captures.
@Composable
fun ViriTabTipCard(lines: List<Int>, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val showsGuide = !LocalViraEnvironment.current.isAccessibilitySize
    val guideSide = 88.dp
    val shape = RoundedCornerShape(ViraRadius.l)
    // TalkBack va directe a la targeta, com un diàleg.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(modifier.padding(horizontal = ViraSpacing.screenMargin)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ViraColor.surfaceElevated, shape)
                .focusRequester(focus)
                .focusable()
                .padding(ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                Text(
                    stringResource(R.string.viri_tip_title),
                    style = ViraFont.title,
                    color = ViraColor.textPrimary,
                    modifier = Modifier
                        .padding(end = if (showsGuide) guideSide * 0.6f else 0.dp)
                        .semantics { heading() },
                )
                lines.forEachIndexed { index, line -> ViriTabTipLine(index + 1, line) }
            }
            ViraActionButton(stringResource(R.string.viri_tip_ok), onDone, Modifier.fillMaxWidth())
        }
        if (showsGuide) {
            Image(
                painterResource(R.drawable.viri_guide_thumbs),
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
private fun ViriTabTipLine(number: Int, @StringRes text: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
    ) {
        ViraTutorialBadge(number, side = 24.dp, scales = true)
        Text(stringResource(text), style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
    }
}

private const val VIRI_TAB_TIP_SEEN = "settings.viriTabTipSeen"
private const val PROGRESS_VISIBLE = "settings.progressVisible"
private const val SHOP_VISIBLE = "settings.shopVisible"
