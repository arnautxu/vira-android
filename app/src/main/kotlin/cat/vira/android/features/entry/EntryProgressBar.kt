package cat.vira.android.features.entry

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.viraSpec

/// Barra de progrés del flux, **a la capçalera** (`EntryProgressBar.swift`), amb un Viri petit
/// que hi camina. Pista `surface(Rewards)` i farciment `accent(Today)` horitzontal.
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
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(MARKER)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        val filled = maxOf(MARKER / 2, maxWidth * animated)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(10.dp)
                .background(ViraColor.surface(ViraArea.Rewards), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(filled)
                .height(10.dp)
                .background(ViraGradient.accent(ViraArea.Today, ViraGradient.Axis.Horizontal), CircleShape),
        )
        Image(
            painter = painterResource(R.drawable.viri_hero),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = minOf(filled, maxWidth) - MARKER / 2)
                .size(MARKER)
                .shadow(3.dp, CircleShape, ambientColor = ViraColor.shadow, spotColor = ViraColor.shadow)
                .background(ViraColor.surfaceElevated, CircleShape)
                .padding(3.dp),
        )
    }
}

private val MARKER = 28.dp
