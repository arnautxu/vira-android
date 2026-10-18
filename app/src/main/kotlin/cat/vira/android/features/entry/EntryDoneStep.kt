package cat.vira.android.features.entry

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay

/// E6 · Fet. La celebració de la primera acció, i **només del que ha passat** (`EntryDoneStep.swift`).
///
/// - Una tasca afegida: la píndola "A Avui". Afegir no dona monedes, i dir-ho seria mentir.
/// - Un check-in o un focus: el que hagi donat el comptador pel seu camí de sempre. Si el sostre
///   o el llindar del focus no han donat res, no surt res.
/// - **Cap "Dia 1" ni cap menció de la ratxa**: una sola acció no pot completar un dia.
///
/// Viri hi surt com l'estàtua daurada amb confeti i anelles (`EntryStage`). No té botó enrere: una
/// celebració no es desfà. El nivell encara no existeix a Android; quan hi sigui, aquest pas l'ha
/// de marcar com a celebrat (`consumeLevel` a l'iPhone).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDoneStep(outcome: EntryFirstActionOutcome?, prefs: EntryPreferences, onContinue: () -> Unit) {
    val chips = entryDoneChips(outcome, prefs.progressVisible.value)
    val viriLine = stringResource(R.string.onboarding_viri_done)
    val announcement = (listOf(viriLine) + chips).joinToString(". ")
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val context = LocalContext.current
    val celebrates = prefs.celebrationsEnabled.value

    LaunchedEffect(Unit) {
        if (celebrates) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        // L'anunci espera que TalkBack acabi de dir el canvi de pas, que si no el trepitja.
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isEnabled != true) return@LaunchedEffect
        delay(600)
        @Suppress("DEPRECATION")
        view.announceForAccessibility(announcement)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.m)) {
        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = chips.joinToString(", ") },
            ) {
                chips.forEach { chip ->
                    Text(
                        chip,
                        style = ViraFont.headline,
                        color = ViraColor.textPrimary,
                        modifier = Modifier
                            .defaultMinSize(minHeight = ViraTouch.minimum)
                            .background(ViraColor.surfaceElevated, CircleShape)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs),
                    )
                }
            }
        }
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)
    }
}

/// Les píndoles, ja escrites. Les de recompensa respecten "Ensenya el progrés" d'Ajustos.
@Composable
private fun entryDoneChips(outcome: EntryFirstActionOutcome?, progressVisible: Boolean): List<String> {
    if (outcome == null) return emptyList()
    if (outcome.taskTitle != null) return listOf(stringResource(R.string.onboarding_done_task))
    if (!progressVisible) return emptyList()
    val reward = outcome.reward
    return buildList {
        if (reward.coins > 0) add(stringResource(R.string.focus_done_coins, reward.coins))
        if (reward.xp > 0) add(stringResource(R.string.toast_reward_xp, reward.xp))
    }
}
