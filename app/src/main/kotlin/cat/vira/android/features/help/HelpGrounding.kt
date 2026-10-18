package cat.vira.android.features.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Ancoratge: el 5-4-3-2-1 dels sentits, **un pas cada cop** (`HelpGroundingView`, Arnau,
/// 2026-10-10). Cada pas diu el sentit i quantes coses, i només n'hi ha un a la vista. Les
/// respostes són locals i efímeres (es perden en sortir), sense diagnòstic ni historial, i escriure
/// és opcional: «Continuar» funciona amb el camp buit, perquè buscar-les ja és l'exercici.
///
/// `initialStep` i `initialAnswers` només serveixen per a les captures.
@Composable
fun HelpGroundingContent(
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    initialStep: Int = 0,
    initialAnswers: List<String> = emptyList(),
) {
    var step by remember { mutableIntStateOf(initialStep) }
    val answers = remember {
        mutableStateListOf(*Array(GroundingSense.count) { initialAnswers.getOrElse(it) { "" } })
    }
    val isFinished = GroundingSense.isFinished(step)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // En prémer «Continuar», el cursor torna al camp del pas nou; en acabar, el teclat se'n va.
    LaunchedEffect(step) {
        if (step == initialStep) return@LaunchedEffect
        if (isFinished) focusManager.clearFocus() else runCatching { focusRequester.requestFocus() }
    }

    HelpPage(stringResource(R.string.help_grounding_title), onBack = onBack) {
        GroundingCard(step, answers.getOrElse(step) { "" }, { answers[step] = it }, focusRequester)
        if (isFinished) {
            ViraActionButton(stringResource(R.string.help_check_in_action), onCheckIn, Modifier.fillMaxWidth())
        } else {
            val isLast = step == GroundingSense.count - 1
            ViraActionButton(
                stringResource(if (isLast) R.string.action_done else R.string.action_continue),
                onClick = { step += 1 },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Els passos ja fets, del més recent al primer, amb el que s'hi ha escrit (si s'hi ha escrit res).
        val done = GroundingSense.entries.take(step)
        if (done.isNotEmpty()) {
            HelpSectionHeader(stringResource(R.string.help_grounding_done))
            done.asReversed().forEach { sense ->
                HelpInfoRow(
                    sense.pictogram,
                    stringResource(sense.title),
                    detail = answers[sense.ordinal].trim().ifEmpty { null },
                )
            }
        }
        HelpFootnote(stringResource(R.string.help_private))
    }
}

@Composable
private fun GroundingCard(step: Int, answer: String, onAnswer: (String) -> Unit, focusRequester: FocusRequester) {
    val isFinished = GroundingSense.isFinished(step)
    val sense = GroundingSense.current(step)
    val ink = ViraColor.onCard(ViraArea.Today)
    Column(
        Modifier
            .fillMaxWidth()
            .background(ViraGradient.card(ViraArea.Today), RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        HelpProgress(
            leading = stringResource(R.string.help_grounding_tagline),
            trailing = stringResource(R.string.challenge_progress, GroundingSense.shownStep(step), GroundingSense.count),
            fraction = GroundingSense.shownStep(step) / GroundingSense.count.toFloat(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            ViraPictogram(if (isFinished) R.drawable.picto_habit_meditate else sense.pictogram, size = 56.dp, scalesWithText = true)
            Text(
                stringResource(if (isFinished) R.string.help_grounding_finished else sense.title),
                style = ViraFont.title,
                color = ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        if (!isFinished) {
            val label = stringResource(sense.title)
            BasicTextField(
                value = answer,
                onValueChange = onAnswer,
                textStyle = ViraFont.body.copy(color = ViraColor.textPrimary),
                cursorBrush = SolidColor(ViraColor.accent(ViraArea.Today)),
                minLines = 2,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    // Sense etiqueta, TalkBack només diria el text d'ajuda i no quin sentit toca.
                    .semantics { contentDescription = label },
                decorationBox = { field ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = ViraTouch.minimum)
                            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.s))
                            .padding(ViraSpacing.s),
                    ) {
                        if (answer.isEmpty()) {
                            Text(stringResource(R.string.help_grounding_placeholder), style = ViraFont.body, color = ViraColor.textSecondary)
                        }
                        field()
                    }
                },
            )
        }
    }
}
