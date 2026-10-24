package cat.vira.android.features.focus

import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.design.components.ViraPictogram
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import java.util.UUID

/// Preparació d'una sessió (`FocusSetupView`): **títol a dalt, medalló al centre òptic, accions
/// ancorades a baix**. L'aire que queda entre el medalló i el panell és intencionat: el fons
/// il·lustrat el converteix en atmosfera.
///
/// Dos tocs des d'Avui fins a «en marxa»: la pantalla s'obre amb els valors per defecte (10 min) i
/// «Comença» ho arrenca.
@Composable
fun FocusSetupContent(
    modes: List<FocusMode>,
    mode: FocusMode,
    minutes: Int,
    tasks: List<PlanItem>,
    linkedItemID: UUID?,
    notificationsDenied: Boolean,
    onSelectMode: (FocusMode) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectTask: (PlanItem?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    /// Les monedes que donarà la sessió (`FocusRewardLine`), o `null` si avui ja no en dona.
    rewardCoins: Int? = null,
) {
    val accessibilitySize = LocalViraEnvironment.current.isAccessibilitySize
    BoxWithConstraints(modifier) {
        // Acotat per l'amplada **i** per l'alçada; a lletra gran baixa molt més, perquè manen les lletres.
        val byWidth = maxWidth * if (accessibilitySize) 0.46f else 0.68f
        val byHeight = maxHeight * if (accessibilitySize) 0.26f else 0.34f
        val side = minOf(byWidth, byHeight, 290.dp)
        // Amb l'alçada mínima de la pantalla, l'espai sobrant cau entre el medalló i el panell; si
        // no hi cap (lletra gran), es desplaça.
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(top = ViraSpacing.s, bottom = ViraSpacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    stringResource(R.string.focus_title),
                    style = ViraFont.largeTitle,
                    color = ViraColor.accent(ViraArea.Focus),
                    modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin).semantics { heading() },
                )
                Spacer(Modifier.height(ViraSpacing.xxl))
                FocusHero(modes, mode, minutes, side, onSelectMode)
                FocusModeLabels(mode, Modifier.padding(top = ViraSpacing.m).padding(horizontal = ViraSpacing.screenMargin))
            }
            Column {
                Spacer(Modifier.height(ViraSpacing.xxl))
                FocusLaunchPanel(
                    mode = mode,
                    minutes = minutes,
                    tasks = tasks,
                    linkedItemID = linkedItemID,
                    notificationsDenied = notificationsDenied,
                    rewardCoins = rewardCoins,
                    onSelectMinutes = onSelectMinutes,
                    onSelectTask = onSelectTask,
                    onStart = onStart,
                    modifier = Modifier.padding(horizontal = ViraSpacing.xs),
                )
            }
        }
    }
}

/// L'esfera de durada queda **darrere** i quieta; els medallons del carrusel llisquen per davant.
@Composable
private fun FocusHero(modes: List<FocusMode>, mode: FocusMode, minutes: Int, side: Dp, onSelect: (FocusMode) -> Unit) {
    Box(Modifier.fillMaxWidth().height(side), contentAlignment = Alignment.Center) {
        // La claror és el doble d'ampla que el medalló i no ha de comptar per a la mida de res.
        FocusBreathingHalo(side * FocusMedallionMetrics.HALO_SCALE, Modifier.requiredSize(side * FocusMedallionMetrics.HALO_SCALE))
        FocusDial(side, minutes)
        FocusModeCarousel(modes, mode.id, side, onSelect)
    }
}

/// El nom del mode i la seva frase, sobre la seva pròpia superfície: és la capa de contrast d'aquest
/// text, i el nom anomena el pictograma (un dibuix no és una etiqueta).
@Composable
private fun FocusModeLabels(mode: FocusMode, modifier: Modifier = Modifier) {
    val label = mode.label()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.focusPlate().padding(horizontal = ViraSpacing.l, vertical = ViraSpacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
        ) {
            // Una sola paraula llarga no es parteix: es queda en una línia.
            Text(
                label,
                style = ViraFont.headline,
                color = ViraColor.onSurface(ViraArea.Focus),
                maxLines = if (label.contains(' ')) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (mode.id == FocusModeID.BuiltIn(FocusKind.FOCUS)) {
                Text(
                    stringResource(R.string.focus_pomodoro_summary),
                    style = ViraFont.footnote,
                    color = ViraColor.onSurface(ViraArea.Focus).copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/// El panell de sortida (`FocusLaunchPanel`): tot el que es tria abans de començar, en una sola
/// targeta a l'abast del polze. El medalló diu **quin** mode i el panell diu **com**.
///
/// Sense «Apps en pausa»: a Android no hi ha bloqueig d'apps (`CLAUDE.md`), i a l'iPhone de Release
/// tampoc.
@Composable
fun FocusLaunchPanel(
    mode: FocusMode,
    minutes: Int,
    tasks: List<PlanItem>,
    linkedItemID: UUID?,
    notificationsDenied: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectTask: (PlanItem?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    rewardCoins: Int? = null,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, shape, ambientColor = ViraColor.accent(ViraArea.Focus), spotColor = ViraColor.accent(ViraArea.Focus))
            .background(ViraColor.surface(ViraArea.Focus), shape)
            .padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        FocusTimeStepper(mode, minutes, onSelectMinutes)
        if (mode.kind == FocusKind.FOCUS && rewardCoins != null) FocusRewardLine(rewardCoins)
        // Un selector buit no és una opció: és una decisió de més.
        if (mode.showsLinkedTask && tasks.isNotEmpty()) {
            FocusTaskTile(tasks, linkedItemID, onSelectTask)
        }
        if (notificationsDenied) {
            Text(
                stringResource(R.string.focus_notification_denied),
                style = ViraFont.footnote,
                color = ViraColor.onSurface(ViraArea.Focus),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ViraActionButton(
            stringResource(R.string.focus_start),
            onStart,
            Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(ViraRadius.s), ambientColor = ViraColor.accent(ViraArea.Focus), spotColor = ViraColor.accent(ViraArea.Focus)),
            area = ViraArea.Focus,
        )
    }
}

/// «+15 ViriCoins a partir de 10 min» (`FocusRewardLine`, 2026-10-22, punts de fricció): el llindar
/// del premi es diu abans de començar, no es descobreix en acabar. Amb el sostre del dia ja tocat no
/// surt: prometre monedes que no arriben seria pitjor que callar.
@Composable
private fun FocusRewardLine(coins: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViraPictogram(R.drawable.picto_shop, size = 20.dp, scalesWithText = true)
        Text(
            stringResource(R.string.focus_reward_hint, coins, ViraRewards.FOCUS_MINIMUM_MINUTES),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.onSurface(ViraArea.Focus),
        )
    }
}
