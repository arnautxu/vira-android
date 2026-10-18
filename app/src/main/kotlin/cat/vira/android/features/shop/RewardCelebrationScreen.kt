package cat.vira.android.features.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraCelebrationMoment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.features.progress.CelebrationPill
import cat.vira.android.features.progress.CelebrationSummaryCard
import cat.vira.android.features.progress.CelebrationSummaryRow

/// El moment de bescanviar un premi personal (`RewardCelebrationView`), amb Viri relaxat.
///
/// Aquí no hi ha xifra: bescanviar és una despesa, i celebrar el que et treuen de la cartera és el
/// llenguatge de botiga que `VIRI_AND_GAMIFICATION.md` §2.2.1 no vol. El protagonista és el que
/// has decidit tu que valia la pena. El títol l'ha escrit l'usuari i pot ser llarg.
@Composable
fun RewardCelebrationScreen(title: String, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val area = ViraArea.Rewards
    ViraCelebrationMoment(
        title = stringResource(R.string.celebration_reward_title),
        message = stringResource(R.string.celebration_reward_message),
        onContinue = onContinue,
        modifier = modifier,
        area = area,
        scene = {
            Image(painterResource(R.drawable.viri_chill), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        },
        summary = {
            CelebrationSummaryCard(area, pills = listOf(CelebrationPill(stringResource(R.string.celebration_reward_pill), isFilled = true))) {
                CelebrationSummaryRow(
                    pictogram = R.drawable.picto_game_gift,
                    headline = title,
                    area = area,
                    detail = stringResource(R.string.celebration_reward_chosen),
                    isNumber = false,
                )
            }
        },
    )
}
