package cat.vira.android.features.settings

import android.app.KeyguardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/// Bloqueig de l'app (`SettingsPrivacySection`, `USER_FLOWS.md` F25).
///
/// Sense empremta, cara ni codi configurats al telèfon, l'ajust es queda apagat i inert: activar-lo
/// tancaria l'app per a qui no té cap manera de tornar-hi a entrar. Qui el fa complir en obrir l'app
/// és l'arrencada (`app/`); aquí només es desa.
@Composable
fun SettingsPrivacySection() {
    var lock by rememberBooleanPreference(ViraPreferences.BIOMETRIC_LOCK_ENABLED, false)
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val isAvailable = remember { isPreview || deviceIsSecure(context) }
    LaunchedEffect(isAvailable) { if (!isAvailable && lock) lock = false }

    val title = stringResource(R.string.settings_privacy_lock)
    SettingsGroup(header = stringResource(R.string.settings_privacy_section)) {
        row {
            SettingsToggleRow(lock, { lock = it }, enabled = isAvailable) {
                SettingsLabel(title, pictogram = R.drawable.picto_key)
            }
        }
        if (!isAvailable) row { SettingsNoteRow(stringResource(R.string.settings_privacy_lock_unavailable)) }
    }
}

private fun deviceIsSecure(context: Context): Boolean =
    context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

/// Dades i privacitat (`SettingsDataSection`, `PRIVACY_AND_SAFETY.md` §5): què desa Vira, exportar-ho
/// i esborrar-ho tot.
///
/// **Les dues formes que demana §5**: el JSON complet (tot, exacte, per a una màquina) i el PDF (per
/// a una persona). Dues files i no un selector: qui ve aquí ja sap si ho vol ensenyar o guardar.
/// A Android l'usuari tria on es desa (Drive, Baixades…), i des d'allà ho comparteix on vulgui.
@Composable
fun SettingsDataSection(onErased: () -> Unit) {
    val backend = LocalSettingsBackend.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isConfirmingErase by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }

    fun save(target: Uri?, make: suspend () -> File) {
        target ?: return
        scope.launch {
            error = attempt {
                val file = make()
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                            ?: throw IllegalStateException()
                    } finally {
                        // La còpia de treball porta emocions i medicació: no es queda a la memòria cau.
                        file.parentFile?.takeIf { it.name.startsWith("export-") }?.deleteRecursively() ?: file.delete()
                    }
                }
            }
        }
    }

    val saveJson = rememberDocumentSaver("application/json") { save(it) { backend.exportJson() } }
    val savePdf = rememberDocumentSaver("application/pdf") { save(it) { backend.exportPdf() } }
    val today = LocalDate.now()
    val errorText = error?.let { backendErrorMessage(it) }

    SettingsGroup(header = stringResource(R.string.settings_data_section)) {
        row { SettingsNoteRow(stringResource(R.string.settings_data_stored_message)) }
        row { SettingsActionRow(stringResource(R.string.settings_data_export), { saveJson("vira-$today.json") }, glyph = SettingsGlyphKind.Share) }
        row { SettingsActionRow(stringResource(R.string.settings_data_export_pdf), { savePdf("vira-$today.pdf") }, glyph = SettingsGlyphKind.Document) }
        if (errorText != null) row { ViraInlineError(errorText, Modifier.padding(ViraSpacing.m)) }
        row { SettingsActionRow(stringResource(R.string.settings_data_erase), { isConfirmingErase = true }, destructive = true) }
    }

    if (isConfirmingErase) {
        SettingsDeleteConfirmation(
            onDismiss = { isConfirmingErase = false },
            onErased = {
                isConfirmingErase = false
                onErased()
            },
        )
    }
}

/// El selector del sistema per desar un fitxer nou. A les captures no hi ha activitat: no fa res.
@Composable
private fun rememberDocumentSaver(mimeType: String, onPicked: (Uri?) -> Unit): (String) -> Unit {
    if (LocalInspectionMode.current) return {}
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType), onPicked)
    return { name -> launcher.launch(name) }
}
