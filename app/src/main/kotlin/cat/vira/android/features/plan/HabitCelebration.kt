package cat.vira.android.features.plan

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraAlphaVideo
import cat.vira.android.core.design.components.ViraCelebrationScreen
import cat.vira.android.core.design.components.ViraRewardTally
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.resources.ViraVideo

/// La celebració a pantalla completa del **primer hàbit del dia** (`HabitCelebrationView`): "Hàbit
/// fet!", el nom de l'hàbit i el que s'ha guanyat. La cua de l'arrel decideix quan surt; aquí
/// només es dibuixa. Amb el sostre diari ple, només l'XP.
///
/// A l'iPhone el clip va de fons a sang; mentre `ViraCelebrationScreen` no té fons de vídeo, va
/// a l'espai de l'heroi.
@Composable
fun HabitCelebrationScreen(title: String, coins: Int, xp: Int, onContinue: () -> Unit) {
    ViraCelebrationScreen(
        caption = stringResource(R.string.habit_done_title),
        message = title,
        onContinue = onContinue,
        area = ViraArea.Rewards,
        extra = {
            if (coins > 0 || xp > 0) {
                ViraRewardTally(coins, xp, iconSize = 28.dp, style = ViraFont.title, color = ViraColor.onCard(ViraArea.Rewards))
            }
        },
    ) {
        ViraAlphaVideo(
            ViraVideo.HabitDone,
            Modifier.fillMaxWidth().heightIn(max = 280.dp).aspectRatio(720f / 1280f),
        )
    }
}
