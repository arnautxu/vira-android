package cat.vira.android.features.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Com està una connexió amb el mòbil (`ViraConnections.State`): apagada, funcionant, o encesa però
/// amb el permís denegat. Denegat **no l'apaga**: es queda encesa i diu on es torna a donar.
enum class ViraConnectionState { OFF, WORKING, BLOCKED }

/// «Connecta Vira amb el teu mòbil» (`SettingsConnectCard`, Arnau, 2026-11-24): a dalt d'Ajustos i
/// al pas de connexions de l'entrada, la mateixa targeta. Cada fila diu què fa i porta un botó
/// «Connecta» que obre el permís d'Android; res es connecta sol.
///
/// A Android només hi ha el **calendari**: Recordatoris (Siri) no té equivalent i Health Connect
/// arriba més endavant. Sense estat propi: qui la posa diu com està i què fa cada botó, perquè
/// Ajustos i l'entrada desen la mateixa clau des de dos llocs.
@Composable
fun SettingsConnectCard(
    calendar: ViraConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    showsHeader: Boolean = true,
) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .border(2.dp, ViraColor.brand, RoundedCornerShape(ViraRadius.l))
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        if (showsHeader) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                Text(
                    stringResource(R.string.settings_connect_title),
                    style = ViraFont.title,
                    color = ViraColor.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.settings_connect_subtitle), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
        ConnectRow(
            title = stringResource(R.string.settings_calendar_title),
            detail = stringResource(R.string.settings_connect_calendar_detail),
            pictogram = R.drawable.picto_calendar,
            state = calendar,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onOpenSettings = { openAppSettings(context) },
        )
    }
}

@Composable
private fun ConnectRow(
    title: String,
    detail: String,
    pictogram: Int,
    state: ViraConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // A lletra gran el botó baixa a sota del text: al costat, el títol quedava en una paraula per línia.
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        val label: @Composable (Modifier) -> Unit = { m ->
            Row(m.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                ViraPictogram(pictogram, size = 36.dp, scalesWithText = true)
                Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                    Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
                    Text(detail, style = ViraFont.footnote, color = ViraColor.textSecondary)
                }
            }
        }
        if (stacked) {
            label(Modifier.fillMaxWidth())
            ConnectControl(state, onConnect, onDisconnect, onOpenSettings)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                label(Modifier.weight(1f))
                ConnectControl(state, onConnect, onDisconnect, onOpenSettings)
            }
        }
        if (state == ViraConnectionState.BLOCKED) {
            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), modifier = Modifier.semantics(mergeDescendants = true) {}) {
                SettingsGlyph(SettingsGlyphKind.Warning, ViraColor.textSecondary, Modifier.padding(top = 1.dp), extent = 15.dp)
                Text(stringResource(R.string.settings_connect_blocked), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
    }
}

/// El control de la dreta: «Connecta» (primari), «Obre Ajustos» si el permís està denegat, i
/// «Connectat» amb la marca quan funciona, que en tocar-lo ofereix «Desconnecta». Mai només color:
/// la marca sempre va amb la paraula.
@Composable
private fun ConnectControl(
    state: ViraConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (state) {
        ViraConnectionState.OFF ->
            ViraActionButton(stringResource(R.string.settings_connect_action), onConnect, textStyle = ViraFont.headline)
        ViraConnectionState.BLOCKED ->
            ViraActionButton(stringResource(R.string.action_open_settings), onOpenSettings, hierarchy = ViraActionHierarchy.Secondary)
        ViraConnectionState.WORKING -> {
            var isOpen by remember { mutableStateOf(false) }
            Box {
                Row(
                    Modifier
                        .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
                        .viraPressable(onClickLabel = stringResource(R.string.settings_connect_disconnect)) { isOpen = true }
                        .padding(horizontal = ViraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                ) {
                    ViraPictogram(R.drawable.picto_plan_check, size = 22.dp, scalesWithText = true)
                    Text(stringResource(R.string.settings_connect_done), style = ViraFont.headline, color = ViraColor.success)
                }
                DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }, containerColor = ViraColor.surfaceElevated) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_connect_disconnect), style = ViraFont.body, color = ViraColor.error) },
                        onClick = { isOpen = false; onDisconnect() },
                    )
                }
            }
        }
    }
}

fun calendarAllowed(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

/// L'estat del calendari a partir de la clau i del permís. Es torna a mirar en tornar a primer pla:
/// el permís es pot donar o treure des dels ajustos del telèfon. A les captures, el permís no
/// existeix: encès vol dir que funciona.
@Composable
fun rememberCalendarConnection(isOn: Boolean): Pair<ViraConnectionState, () -> Unit> {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var revision by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    val state = remember(revision, isOn) {
        when {
            !isOn -> ViraConnectionState.OFF
            isPreview || calendarAllowed(context) -> ViraConnectionState.WORKING
            else -> ViraConnectionState.BLOCKED
        }
    }
    return state to { revision++ }
}

/// La targeta a Ajustos: encén la clau i demana el permís en el mateix toc.
@Composable
fun SettingsConnectSection() {
    var calendar by rememberBooleanPreference(SettingsKeys.CALENDAR, false)
    val context = LocalContext.current
    val (state, refresh) = rememberCalendarConnection(calendar)
    val requestPermission = rememberPermissionRequest { refresh() }
    SettingsConnectCard(
        calendar = state,
        onConnect = {
            calendar = true
            if (!calendarAllowed(context)) requestPermission(Manifest.permission.READ_CALENDAR)
        },
        onDisconnect = { calendar = false },
    )
}
