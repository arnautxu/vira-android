package cat.vira.android.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.notifications.ViraNotifications
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.core.sound.ViraSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/// Avisos: un sol interruptor i, per a qui en té, la medicació (`SettingsNotificationsSection`,
/// Arnau, 2026-10-23: «les notis hauria de ser un toggle només» i «no vull que es pugui
/// personalitzar, només lo de medicació»).
///
/// - **«Avisos»** (`remindersEnabled`) ho porta tot menys la medicació: el matí, hàbits, tasques,
///   l'emocional i Viri. Les hores les posa Vira (el matí a les 8:00, el vespre après amb
///   `ViraEveningHour`), i les categories i el detall ja no es trien (`ViraSimpleSettings`).
/// - **Medicació**, a part i només si en tens: el seu interruptor no penja d'«Avisos» —qui ho apaga
///   tot continua tenint les preses— i el nom del fàrmac segueix apagat de fàbrica
///   (`PRIVACY_AND_SAFETY.md` §4.6).
@Composable
fun SettingsNotificationsSection() {
    val backend = LocalSettingsBackend.current
    val preferences = LocalSettingsPreferences.current
    var notifications by rememberBooleanPreference(ViraPreferences.REMINDERS_ENABLED, true)
    val medicationModule by rememberBooleanPreference(SettingsKeys.MODULE_MEDICATION, true)
    var medication by rememberBooleanPreference(SettingsKeys.REMINDERS_MEDICATION, true)
    // El fàrmac va a part i neix apagat: no pot penjar del mateix interruptor que l'hàbit.
    var medicationDetail by rememberBooleanPreference(SettingsKeys.NOTIFICATION_DETAIL_MEDICATION, false)
    val hasMedications by backend.hasMedications.collectAsStateOr(false)

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var revision by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    val requestPermission = rememberPermissionRequest { revision++ }
    fun askIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed(context)) {
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Un cop denegat el permís, Android ja no torna a preguntar. Callar deixaria un interruptor
    // encès que no farà mai res.
    val isDenied = remember(revision, notifications, medication) {
        !isPreview && (notifications || medication) && !notificationsAllowed(context)
    }
    // Qualsevol canvi d'aquí (o un permís que acaba d'arribar) canvia què s'ha de recordar.
    LaunchedEffect(revision, notifications, medication, medicationDetail) {
        if (!isPreview) withContext(Dispatchers.Default) { ViraNotifications.reschedule(context.applicationContext) }
    }

    SettingsGroup(
        footer = stringResource(
            if (notifications) R.string.settings_notifications_master_footer else R.string.settings_notifications_master_off,
        ),
    ) {
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_notifications_master), notifications, {
                    // Escriu també els missatges de Viri: així no poden quedar desaparellats.
                    ViraSimpleSettings.setNotifications(preferences, it)
                    notifications = it
                    if (it) askIfNeeded()
                },
                pictogram = R.drawable.picto_set_notifications,
            )
        }
    }

    if (medicationModule && hasMedications) {
        SettingsGroup(header = stringResource(R.string.settings_notifications_category_medication)) {
            row {
                SettingsToggleRow(
                    stringResource(R.string.settings_notifications_medication), medication, {
                        medication = it
                        if (it) askIfNeeded()
                    },
                    hint = stringResource(R.string.settings_notifications_medication_hint),
                    pictogram = R.drawable.picto_medication,
                )
            }
            if (medication) {
                row {
                    SettingsToggleRow(
                        stringResource(R.string.settings_notifications_detail_medication), medicationDetail, { medicationDetail = it },
                        hint = stringResource(R.string.settings_notifications_detail_medication_hint),
                    )
                }
            }
        }
    }

    if (isDenied) {
        SettingsGroup {
            row { SettingsBlockedRow(stringResource(R.string.habits_reminder_denied)) { openNotificationSettings(context) } }
        }
    }
}

/// So i vibració, en un sol interruptor (`SettingsSoundsSection`, Arnau, 2026-10-23). Venen de «Com
/// et vull motivar», que ha sortit d'Ajustos: queda el que fa falta de debò —treure el so a classe—
/// i va junt amb la vibració, perquè qui vol el telèfon callat també el vol quiet.
@Composable
fun SettingsSoundsSection() {
    val preferences = LocalSettingsPreferences.current
    var soundMode by rememberStringPreference(SettingsKeys.SOUND_MODE, ViraSoundMode.ALL.raw)
    val context = LocalContext.current
    SettingsGroup(header = stringResource(R.string.settings_sounds)) {
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_sounds_haptics), ViraSoundMode.from(soundMode) != ViraSoundMode.OFF, { isOn ->
                    ViraSimpleSettings.setSounds(preferences, isOn)
                    soundMode = (if (isOn) ViraSoundMode.ALL else ViraSoundMode.OFF).raw
                    // Un interruptor de so que no es nota en tocar-lo no es pot comprovar.
                    if (isOn) {
                        ViraSound.play(context, ViraSound.Cue.CHALLENGE)
                        ViraHaptics.play(context, ViraHaptics.Feedback.Done)
                    }
                },
                hint = stringResource(R.string.settings_sounds_hint),
            )
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
