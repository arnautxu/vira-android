package cat.vira.android.features.entry

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import androidx.compose.runtime.staticCompositionLocalOf
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import java.time.LocalDate

/// Pistes d'un sol ús, la primera vegada que s'entra a un lloc (`ViraTips.swift`, Arnau, 2026-10-23).
///
/// L'iPhone fa servir TipKit, que és local; aquí són unes preferències del telèfon, tampoc no
/// surten enlloc. Cada pista surt una vegada, es pot tancar i no torna, i n'hi ha **una al dia**
/// com a molt (`displayFrequency(.daily)`): la que ja ha sortit avui es queda fins que la tanques.
///
/// ## On no surten
/// Mai a Ajuda ara, a medicació ni a un camí de crisi. Només a Concentra't, Pla, Emocions i
/// Mindfulness, i només un cop passat el tomb. Les de Pla i Emocions, a més, només a qui ha
/// saltat el tomb: qui l'ha vist ja ho sap. La de Siri no hi és: a Android no hi ha Siri.
enum class ViraTip(
    val id: String,
    @StringRes val title: Int,
    @StringRes val message: Int,
    @DrawableRes val pictogram: Int,
    val onlyIfSkipped: Boolean,
) {
    FOCUS("focus", R.string.tip_focus_title, R.string.tip_focus_message, R.drawable.picto_timer, false),
    PLAN_ADD("planAdd", R.string.tip_plan_title, R.string.tip_plan_message, R.drawable.picto_plan_check, true),
    EMOTION("emotion", R.string.tip_emotion_title, R.string.tip_emotion_message, R.drawable.picto_heart, true),
    MINDFULNESS("mindfulness", R.string.tip_mindfulness_title, R.string.tip_mindfulness_message, R.drawable.picto_habit_breathe, false);

    val dismissedKey: String get() = "settings.tip.$id.dismissed"
}

/// Les preferències de l'arrel. Sense arrel (captures d'una pantalla sola), `null`: cap pista.
val LocalViraTipPreferences = staticCompositionLocalOf<SharedPreferences?> { null }

/// Les regles de quan surt una pista, a part perquè es puguin provar.
object ViraTipRules {
    const val SHOWN_DAY = "settings.tip.shownDay"
    const val SHOWN_ID = "settings.tip.shownId"

    fun isEligible(tip: ViraTip, completed: Boolean, skipped: Boolean, dismissed: Boolean): Boolean =
        completed && (!tip.onlyIfSkipped || skipped) && !dismissed

    /// Una al dia: avui pot sortir si avui no n'ha sortit cap altra.
    fun fitsToday(tip: ViraTip, shownDay: String?, shownId: String?, today: LocalDate): Boolean =
        shownDay != today.toString() || shownId == tip.id

    fun markShown(preferences: SharedPreferences, tip: ViraTip, today: LocalDate) {
        preferences.edit().putString(SHOWN_DAY, today.toString()).putString(SHOWN_ID, tip.id).apply()
    }

    fun dismiss(preferences: SharedPreferences, tip: ViraTip) {
        preferences.edit().putBoolean(tip.dismissedKey, true).apply()
    }
}

/// Una pista en línia (`ViraTipRow`), amb les condicions de sempre: tomb passat, no saltat si és
/// de Pla o Emocions, no tancada i cap altra pista avui.
@Composable
fun ViraTipRow(tip: ViraTip, modifier: Modifier = Modifier) {
    val preferences = LocalViraTipPreferences.current ?: return
    val completed by rememberPreference(preferences, ViraPreferences.TUTORIAL_COMPLETED, false)
    val skipped by rememberPreference(preferences, EntryPreferenceKeys.TUTORIAL_SKIPPED, false)
    val dismissed by rememberPreference(preferences, tip.dismissedKey, false)
    if (!ViraTipRules.isEligible(tip, completed, skipped, dismissed)) return

    // El dia es mira en obrir la pantalla: una pista que ja hi és no desapareix a mitjanit.
    val today = remember { LocalDate.now() }
    val fits = remember(tip) {
        ViraTipRules.fitsToday(
            tip,
            preferences.getString(ViraTipRules.SHOWN_DAY, null),
            preferences.getString(ViraTipRules.SHOWN_ID, null),
            today,
        )
    }
    if (!fits) return
    LaunchedEffect(tip) { ViraTipRules.markShown(preferences, tip, today) }

    ViraTipCard(tip, onClose = { ViraTipRules.dismiss(preferences, tip) }, modifier)
}

/// La targeta: pictograma, títol, una línia i la creu per tancar-la.
@Composable
fun ViraTipCard(tip: ViraTip, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val shape = RoundedCornerShape(ViraRadius.m)
    Row(
        modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, shape)
            .padding(start = ViraSpacing.m, top = ViraSpacing.xs, bottom = ViraSpacing.m, end = ViraSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Column(
            Modifier.weight(1f).padding(top = ViraSpacing.xs).semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            if (!stacked) ViraPictogram(tip.pictogram, size = 32.dp)
            Text(stringResource(tip.title), style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(stringResource(tip.message), style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        ViraTipClose(onClose)
    }
}

@Composable
private fun ViraTipClose(onClose: () -> Unit) {
    val label = stringResource(R.string.action_close)
    val ink = ViraColor.textSecondary
    Box(
        Modifier
            .size(ViraTouch.minimum)
            .viraPressable(onClickLabel = label, role = Role.Button, onClick = onClose)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(ink, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(ink, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}
