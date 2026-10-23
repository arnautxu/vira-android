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
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay

/// E6 · Fet. La celebració de la primera acció, i **només del que ha passat** (`EntryDoneStep.swift`).
///
/// - Una tasca afegida: la píndola blava de Pla amb el seu títol, i a sobre "Ho tens a Avui. Quan
///   ho acabis, toca el cercle." (`EntryNavigator.message`). Afegir no dona monedes, i dir-ho seria mentir.
/// - Un check-in o un focus: el que hagi donat el comptador pel seu camí de sempre. Si el sostre
///   o el llindar del focus no han donat res, no surt res.
/// - **Cap "Dia 1" ni cap menció de la ratxa**: una sola acció no pot completar un dia.
///
/// Viri hi surt dins del coet (`viri_scene_launch`) amb confeti i anelles a sobre (`EntryScene`).
/// El consell de Siri de l'iPhone no hi és: Android no té Siri. No té botó enrere: una
/// celebració no es desfà. El nivell encara no existeix a Android; quan hi sigui, aquest pas l'ha
/// de marcar com a celebrat (`consumeLevel` a l'iPhone).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDoneStep(outcome: EntryFirstActionOutcome?, prefs: EntryPreferences, onContinue: () -> Unit) {
    val chips = entryDoneChips(outcome, prefs.progressVisible.value)
    val viriLine = stringResource(R.string.onboarding_viri_done)
    val announcement = (listOf(viriLine) + chips.map { it.text }).joinToString(". ")
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
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = chips.joinToString(", ") { it.text } },
            ) {
                chips.forEach { chip ->
                    Text(
                        chip.text,
                        style = ViraFont.headline,
                        color = ViraColor.onSurface(chip.area),
                        modifier = Modifier
                            .defaultMinSize(minHeight = ViraTouch.minimum)
                            .background(ViraColor.surface(chip.area), CircleShape)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xxs),
                    )
                }
            }
        }
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue)
    }
}

/// Una píndola: el text i de quina àrea és. La tasca va en blau de Pla; les recompenses, en el groc
/// dels premis.
private data class EntryDoneChip(val text: String, val area: ViraArea)

/// Les píndoles, ja escrites. Les de recompensa respecten "Ensenya el progrés" d'Ajustos.
@Composable
private fun entryDoneChips(outcome: EntryFirstActionOutcome?, progressVisible: Boolean): List<EntryDoneChip> {
    if (outcome == null) return emptyList()
    outcome.taskTitle?.let { title ->
        return listOf(EntryDoneChip(title.trim().ifEmpty { stringResource(R.string.onboarding_done_task) }, ViraArea.Plan))
    }
    if (!progressVisible) return emptyList()
    val reward = outcome.reward
    return buildList {
        if (reward.coins > 0) add(EntryDoneChip(stringResource(R.string.focus_done_coins, reward.coins), ViraArea.Rewards))
        if (reward.xp > 0) add(EntryDoneChip(stringResource(R.string.toast_reward_xp, reward.xp), ViraArea.Rewards))
    }
}
