package cat.vira.android.features.add

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.notifications.ViraNotifications
import java.time.LocalTime

/// «T'aviso a les 17:00?», just quan una tasca té hora (`ViraNotificationPermissionCard`, Arnau,
/// 2026-10-22, punts de fricció). El permís només es demanava a l'entrada, i qui hi deia «Ara no» no
/// el tornava a veure mai: aquí es demana **en context**.
///
/// Sense permís, la pregunta i «Avisa'm», que obre el diàleg del sistema. Si el diàleg ja no surt
/// (Android no torna a preguntar), o en un Android sense diàleg, on es torna a encendre. Amb el
/// permís, o amb els avisos apagats a Ajustos, res.
/// El diàleg del permís no treu l'app de primer pla, i la passada del desar ja s'ha fet sense
/// permís: sense una de nova, l'avís de la tasca que s'acaba de crear no sortiria. Va per
/// l'abast del procés perquè el full es pot tancar just després.
fun rescheduleAfterGrant(context: Context) {
    val app = context.applicationContext
    ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.Default) { ViraNotifications.reschedule(app) }
}

@Composable
fun AddNotificationPermissionCard(time: LocalTime, remindersOn: Boolean) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    var canPost by remember { mutableStateOf(!inPreview && runCatching { ViraNotifications.canPost(context) }.getOrDefault(true)) }
    var isDenied by remember { mutableStateOf(!inPreview && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }
    // Les captures no tenen activitat que obri el diàleg.
    val permission = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canPost = granted && ViraNotifications.canPost(context)
        isDenied = !canPost
        if (canPost) rescheduleAfterGrant(context)
    }
    if (!remindersOn || canPost) return

    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val text = @Composable { modifier: Modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            if (isDenied) {
                Text(stringResource(R.string.notification_permission_denied), style = ViraFont.footnote, color = ViraColor.textPrimary)
            } else {
                Text(stringResource(R.string.notification_permission_ask, clockText(time.hour * 60 + time.minute)), style = ViraFont.footnoteEmphasis, color = ViraColor.textPrimary)
                Text(stringResource(R.string.notification_permission_ask_support), style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
    }
    val button = @Composable {
        Text(
            stringResource(if (isDenied) R.string.notification_permission_settings else R.string.notification_permission_allow),
            style = ViraFont.footnoteEmphasis,
            color = ViraColor.onAccent,
            modifier = Modifier
                .defaultMinSize(minHeight = ViraTouch.minimum)
                .background(ViraColor.accent(ViraArea.Plan), CircleShape)
                .viraPressable {
                    if (isDenied || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        permission?.launch(ViraNotifications.PERMISSION)
                    }
                }
                .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.s),
        )
    }
    val icon = @Composable {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = ViraColor.accent(ViraArea.Plan), modifier = Modifier.size(22.dp))
    }
    val container = Modifier
        .fillMaxWidth()
        .background(ViraColor.surface(ViraArea.Plan), RoundedCornerShape(ViraRadius.m))
        .padding(ViraSpacing.s)
    if (stacked) {
        Column(container, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { icon(); text(Modifier.fillMaxWidth()); button() }
    } else {
        Row(container, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            icon(); text(Modifier.weight(1f)); button()
        }
    }
}
