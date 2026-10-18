package cat.vira.android.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

/// Demana un permís del sistema. A les captures no hi ha activitat que el pugui demanar: no fa res.
@Composable
fun rememberPermissionRequest(onResult: (granted: Boolean) -> Unit): (String) -> Unit {
    if (LocalInspectionMode.current) return {}
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return { permission -> launcher.launch(permission) }
}

/// La pàgina d'aquesta app als ajustos del telèfon: on es tornen a donar els permisos denegats.
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
