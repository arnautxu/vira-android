package cat.vira.android.features.help

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.ViraTileStyle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing

/// On és l'usuari dins d'Ajuda ara. Sense pila: totes les subpantalles surten de l'arrel.
enum class HelpDestination { Home, Breathe, Contact, Grounding, Write, Phones }

/// «Ajuda ara» (`HelpNowView`, `USER_FLOWS.md` F23): flux gradual, **mai un marcatge
/// automàtic**, i **sense telèfons**: la fitxa hi és amb la píndola «Aviat» i porta a «Encara no hi
/// és» fins que hi hagi una llista real i validada (D-39, `PRIVACY_AND_SAFETY.md` §7).
///
/// - `onClose`: «Fet» a l'arrel, o Enrere del sistema des de l'arrel.
/// - `onOpenCheckIn`: Ancoratge i Escriure porten a «Com estàs?» d'Emocions. Qui presenta Ajuda ara
///   l'ha de tancar i obrir el registre (a l'iPhone, `navigate(.emotions(.checkIn))` i `dismiss()`).
///   Mentre ningú ho connecti, tanca Ajuda ara i prou.
@Composable
fun HelpScreen(
    onClose: () -> Unit,
    onOpenCheckIn: () -> Unit = onClose,
    initialDestination: HelpDestination = HelpDestination.Home,
) {
    var destination by rememberSaveable { mutableStateOf(initialDestination) }
    val home = { destination = HelpDestination.Home }
    BackHandler { if (destination == HelpDestination.Home) onClose() else home() }

    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
            } else {
                val forward = targetState != HelpDestination.Home
                slideInHorizontally { if (forward) it else -it / 3 } togetherWith
                    slideOutHorizontally { if (forward) -it / 3 else it }
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "help",
    ) { current ->
        when (current) {
            HelpDestination.Home -> HelpHomeContent(onClose) { destination = it }
            HelpDestination.Breathe -> HelpBreatheContent(home, backLabel = stringResource(R.string.action_help_now))
            HelpDestination.Contact -> HelpContactContent(home)
            HelpDestination.Grounding -> HelpGroundingContent(home, onOpenCheckIn)
            HelpDestination.Write -> HelpWriteContent(home, onOpenCheckIn)
            HelpDestination.Phones -> HelpPage(title = null, onBack = home) {
                ViraEmptyState(
                    title = stringResource(R.string.help_phones_title),
                    message = stringResource(R.string.help_phones_pending_message),
                    icon = { ViraPictogram(R.drawable.picto_cal_call, size = 56.dp) },
                )
            }
        }
    }
}

/// L'arrel: escena de Viri amb «Respira 1 min» com a acció principal, la resta en fitxes a un toc,
/// les idees ràpides aquí mateix i la nota que Vira no és cap professional. Sense pastilla de perfil
/// ni selector: aquí es tria i prou.
@Composable
private fun HelpHomeContent(onClose: () -> Unit, open: (HelpDestination) -> Unit) {
    HelpPage(stringResource(R.string.action_help_now), onDone = onClose) {
        HelpViriScene(stringResource(R.string.help_viri), R.drawable.viri_meditating_cutout) {
            ViraActionButton(stringResource(R.string.help_breathe_start), { open(HelpDestination.Breathe) }, Modifier.fillMaxWidth())
        }

        HelpSectionHeader(stringResource(R.string.help_more))
        ViraTileGrid(HelpOption.entries) { option, modifier ->
            val tag = option.tag
            ViraTile(
                area = ViraArea.Today,
                pictogram = option.pictogram,
                title = stringResource(option.title),
                subtitle = option.subtitle?.let { stringResource(it) },
                style = ViraTileStyle.Plain,
                foot = if (tag != null) { { ViraTag(stringResource(tag)) } } else null,
                modifier = modifier.viraPressable { open(option.destination) },
            )
        }

        HelpSectionHeader(stringResource(R.string.help_tips))
        // Dues columnes no aguanten la lletra gran: a mides d'accessibilitat, una sota l'altra.
        if (LocalViraEnvironment.current.isAccessibilitySize) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { Tips(Modifier) }
        } else {
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                Tips(Modifier.weight(1f))
            }
        }

        HelpFootnote(stringResource(R.string.settings_about_medical))
    }
}

@Composable
private fun Tips(modifier: Modifier) {
    HelpInfoRow(R.drawable.picto_habit_water, stringResource(R.string.help_resources_water), modifier)
    HelpInfoRow(R.drawable.picto_sun, stringResource(R.string.help_resources_move), modifier)
}

/// Les quatre fitxes de sota l'escena. Respirar no hi és: ja és el botó de Viri.
private enum class HelpOption(
    val destination: HelpDestination,
    @DrawableRes val pictogram: Int,
    @StringRes val title: Int,
    @StringRes val subtitle: Int?,
    @StringRes val tag: Int?,
) {
    Contact(HelpDestination.Contact, R.drawable.picto_plan_talk, R.string.help_contact_title, R.string.help_contact_subtitle, null),
    Grounding(HelpDestination.Grounding, R.drawable.picto_habit_walk, R.string.help_grounding_title, R.string.help_grounding_subtitle, R.string.help_grounding_tag),
    Write(HelpDestination.Write, R.drawable.picto_journal, R.string.help_write_title, R.string.help_write_subtitle, R.string.help_write_tag),
    Phones(HelpDestination.Phones, R.drawable.picto_cal_call, R.string.help_phones_title, null, R.string.help_soon),
}
