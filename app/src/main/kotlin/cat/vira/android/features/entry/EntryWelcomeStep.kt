package cat.vira.android.features.entry

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay

/// Pas 1: dos botons i prou (`EntryWelcomeStep.swift`). El botó diu **"Vull començar a…"** amb el
/// final canviant —organitzar-me, les emocions, concentrar-me—: les tres coses que fa Vira.
///
/// `onSignIn` i `onParent` són `null` sense backend: sense comptes no volen dir res.
@Composable
fun EntryWelcomeStep(onStart: () -> Unit, onSignIn: (() -> Unit)?, onParent: (() -> Unit)? = null) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val tails = listOf(
        stringResource(R.string.onboarding_welcome_cta_organize),
        stringResource(R.string.onboarding_welcome_cta_emotions),
        stringResource(R.string.onboarding_welcome_cta_focus),
    )
    val prefix = stringResource(R.string.onboarding_welcome_cta_prefix)
    var index by remember { mutableIntStateOf(0) }

    // **Una sola volta** i torna a la primera: un text que es reescriu sol sense parar és el que
    // WCAG 2.2.2 no permet. Amb TalkBack no gira: el que es veu ha de coincidir amb el que se sent.
    LaunchedEffect(Unit) {
        if (inPreview) return@LaunchedEffect
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) return@LaunchedEffect
        repeat(tails.size) {
            delay(2_600)
            index = (index + 1) % tails.size
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(ViraRadius.s)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ViraTouch.comfortable)
                .clip(shape)
                .background(ViraColor.accent(ViraArea.Today), shape)
                .viraPressable(onClick = onStart)
                // Una sola etiqueta, amb la primera opció: un botó que es reescriu sol cada tres
                // segons seria impossible de seguir.
                .clearAndSetSemantics {
                    contentDescription = "$prefix ${tails[0]}"
                    role = Role.Button
                }
                .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        ) {
            Text(prefix, style = ViraFont.headline, color = ViraColor.onAccent, textAlign = TextAlign.Center)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                    } else {
                        (slideInVertically(ViraMotion.standard()) { it } + fadeIn(ViraMotion.standard())) togetherWith
                            (slideOutVertically(ViraMotion.standard()) { -it } + fadeOut(ViraMotion.standard()))
                    }
                },
                label = "welcomeTail",
            ) { shown ->
                Text(tails[shown], style = ViraFont.headline, color = ViraColor.onAccent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        if (onSignIn != null) {
            EntrySecondaryButton(stringResource(R.string.account_have_account_short), onSignIn)
        }
        if (onParent != null) {
            EntryParentLink(onParent)
        }
    }
}

/// "Sóc pare o mare": un enllaç discret a la benvinguda. Qui és pare o mare el troba, i el jove no
/// el respon mai. Sempre sobre una superfície, mai text pelat sobre el degradat.
@Composable
fun EntryParentLink(onClick: () -> Unit) {
    EntryCapsuleButton(stringResource(R.string.account_role_parent_link), onClick, Modifier.padding(top = ViraSpacing.xxs), bold = true)
}
