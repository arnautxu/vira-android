package cat.vira.android.features.entry

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec

/// Progrés del flux, **a la capçalera** (`EntryProgressBar.swift`, Arnau, 2026-10-21): una càpsula
/// `surfaceElevated` amb la barra i el text «2 de 9» al costat.
///
/// El text és el que ho fa clar: una barra sola diu "falta una mica", «2 de 9» diu quant. I així el
/// progrés no depèn només d'una forma. Pista `surface(Today)` i farciment `accent(Today)`.
///
/// Per a TalkBack és **un sol element**, "Pas 3 de 7". L'amplada és geometria: amb Reduir
/// moviment, cap animació.
@Composable
fun EntryProgressBar(fraction: Double, position: Int, total: Int, modifier: Modifier = Modifier) {
    val label = if (total > 0) {
        stringResource(R.string.onboarding_progress, position, total)
    } else {
        stringResource(R.string.onboarding_progress_unknown)
    }
    val animated by animateFloatAsState(
        targetValue = fraction.toFloat().coerceIn(0f, 1f),
        animationSpec = viraSpec(ViraMotion.settle(), decorative = true),
        label = "entryProgress",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .background(ViraColor.surfaceElevated, CircleShape)
            .padding(horizontal = ViraSpacing.m)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        BoxWithConstraints(Modifier.weight(1f).height(6.dp)) {
            val filled = maxOf(6.dp, maxWidth * animated)
            Box(Modifier.fillMaxWidth().height(6.dp).background(ViraColor.surface(ViraArea.Today), CircleShape))
            Box(Modifier.width(filled).height(6.dp).background(ViraColor.accent(ViraArea.Today), CircleShape))
        }
        if (total > 0) EntryProgressCount(position, total, header = true)
    }
}

/// «2 de 9», el text curt de la càpsula i de dalt de la fulla a lletra gran (on la càpsula no hi
/// cap). Allà va amagat a TalkBack: la capçalera ja diu la frase sencera.
@Composable
fun EntryProgressCount(position: Int, total: Int, modifier: Modifier = Modifier, header: Boolean = false) {
    Text(
        stringResource(R.string.onboarding_progress_short, position, total),
        style = (if (header) ViraFont.caption else ViraFont.headline).copy(fontFeatureSettings = "tnum"),
        color = ViraColor.textSecondary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}
