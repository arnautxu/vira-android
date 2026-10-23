package cat.vira.android.features.today

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Tres hàbits d'un toc per a qui encara no en té cap (`TodayHabitSuggestions`, Arnau,
/// 2026-10-22, punts de fricció). La ratxa demana un hàbit cada dia i l'entrada no en crea cap.
/// Cada dia, sense avís. **Crear no dona res**: les monedes surten en marcar-lo.
internal enum class TodayHabitSuggestion(@StringRes val title: Int, val symbolName: String) {
    WATER(R.string.today_habits_suggest_water, "picto-habit-water"),
    READ(R.string.today_habits_suggest_read, "picto-habit-read"),
    STRETCH(R.string.today_habits_suggest_stretch, "picto-habit-stretch"),
    ;

    /// L'hàbit pel camí de sempre, sense recordatori. El títol es desa ja traduït: és de l'usuari.
    fun habit(title: String): Habit = Habit(title = title, symbolName = symbolName)

    companion object {
        /// Quant es queda «Desfer» a la vista: el mateix que el de marcar una cosa feta.
        const val UNDO_MILLIS = 6_000L
    }
}

/// La fila buida d'«Hàbits d'avui» quan no n'hi ha cap enlloc: una frase i les tres fitxes.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodayHabitSuggestions(onAdd: (TodayHabitSuggestion, String) -> Unit, onOpenHabits: () -> Unit) {
    val area = ViraArea.Plan
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(stringResource(R.string.today_habits_suggest_lead), style = ViraFont.footnote, color = ViraColor.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
        ) {
            TodayHabitSuggestion.entries.forEach { suggestion ->
                val title = stringResource(suggestion.title)
                val spoken = stringResource(R.string.today_habits_suggest_a11y, title)
                Row(
                    Modifier
                        .defaultMinSize(minHeight = ViraTouch.minimum)
                        .clip(CircleShape)
                        .background(ViraColor.surfaceElevated, CircleShape)
                        .viraPressable { onAdd(suggestion, title) }
                        .clearAndSetSemantics {
                            contentDescription = spoken
                            role = Role.Button
                        }
                        .padding(horizontal = ViraSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = ViraColor.accent(area), modifier = Modifier.size(16.dp))
                    ViraPictogram(suggestion.symbolName, size = 24.dp)
                    Text(title, style = ViraFont.footnoteEmphasis, color = ViraColor.onSurface(area), modifier = Modifier.wrapContentHeight())
                }
            }
        }
        Text(
            stringResource(R.string.today_habits_suggest_other),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.accent(area),
            modifier = Modifier
                .defaultMinSize(minHeight = ViraTouch.minimum)
                .viraPressable(onClick = onOpenHabits)
                .padding(vertical = ViraSpacing.s),
        )
    }
}

/// «Afegit · Desfer», sota la tira, després de tocar una idea.
@Composable
internal fun TodayAddedHabitRow(habit: Habit, onUndo: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.today_habits_suggest_added, habit.title),
            style = ViraFont.footnote,
            color = ViraColor.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(ViraSpacing.xs))
        Text(
            stringResource(R.string.action_undo),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.accent(ViraArea.Plan),
            modifier = Modifier
                .defaultMinSize(minHeight = ViraTouch.minimum)
                .viraPressable(onClick = onUndo)
                .padding(vertical = ViraSpacing.s),
        )
    }
}
