package cat.vira.android.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
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

/// Recordatoris i missatges de Viri (`USER_FLOWS.md` F24, `SettingsNotificationsSection`).
///
/// **Recordatoris** és el mestre, i per sota hi ha Hàbits, Medicació, Tasques del pla i Registre
/// emocional —aquest amb hora pròpia, perquè no surt de cap pauta de l'usuari—. **Missatges de
/// Viri** va a part: és gamificació, i tota la gamificació s'ha de poder apagar sense perdre res més.
///
/// Aquí només es desen les preferències, amb els noms de l'iPhone: qui programa els avisos és el fil
/// de Notificacions (`core/notifications`), que les llegeix i reprograma quan canvien.
@Composable
fun SettingsNotificationsSection() {
    var reminders by rememberBooleanPreference(ViraPreferences.REMINDERS_ENABLED, true)
    var habits by rememberBooleanPreference(SettingsKeys.REMINDERS_HABITS, true)
    var medication by rememberBooleanPreference(SettingsKeys.REMINDERS_MEDICATION, true)
    var plan by rememberBooleanPreference(SettingsKeys.REMINDERS_PLAN, true)
    var emotion by rememberBooleanPreference(SettingsKeys.REMINDERS_EMOTION, true)
    var emotionHour by rememberIntPreference(SettingsKeys.EMOTION_REMINDER_HOUR, SettingsKeys.EMOTION_DEFAULT_HOUR)
    var emotionMinute by rememberIntPreference(SettingsKeys.EMOTION_REMINDER_MINUTE, SettingsKeys.EMOTION_DEFAULT_MINUTE)
    var detail by rememberBooleanPreference(ViraPreferences.NOTIFICATION_DETAIL_ENABLED, true)
    // El fàrmac va a part i neix apagat: no pot penjar del mateix interruptor que l'hàbit.
    var medicationDetail by rememberBooleanPreference(SettingsKeys.NOTIFICATION_DETAIL_MEDICATION, false)
    var viri by rememberBooleanPreference(SettingsKeys.VIRI_MESSAGES, true)
    var viriHour by rememberIntPreference(SettingsKeys.VIRI_MESSAGE_HOUR, SettingsKeys.VIRI_DEFAULT_HOUR)
    var viriMinute by rememberIntPreference(SettingsKeys.VIRI_MESSAGE_MINUTE, SettingsKeys.VIRI_DEFAULT_MINUTE)

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
    // encès que no farà mai res. Només compta el que hi ha en aquesta secció.
    val isDenied = remember(revision, reminders, viri) {
        !isPreview && (reminders || viri) && !notificationsAllowed(context)
    }

    SettingsGroup(header = stringResource(R.string.settings_notifications_section)) {
        row {
            SettingsToggleRow(stringResource(R.string.settings_notifications_reminders), reminders, {
                reminders = it
                if (it) askIfNeeded()
            })
        }
    }

    if (reminders) {
        SettingsGroup(header = stringResource(R.string.settings_notifications_categories)) {
            row { SettingsToggleRow(stringResource(R.string.settings_notifications_category_habits), habits, { habits = it }, pictogram = R.drawable.picto_habits) }
            row { SettingsToggleRow(stringResource(R.string.settings_notifications_category_medication), medication, { medication = it }, pictogram = R.drawable.picto_medication) }
            row { SettingsToggleRow(stringResource(R.string.settings_notifications_category_plan), plan, { plan = it }, pictogram = R.drawable.picto_plan_check) }
            row { SettingsToggleRow(stringResource(R.string.settings_notifications_category_emotion), emotion, { emotion = it }, pictogram = R.drawable.picto_journal) }
            if (emotion) {
                row {
                    SettingsTimeRow(stringResource(R.string.settings_notifications_emotion_time), emotionHour, emotionMinute) { hour, minute ->
                        emotionHour = hour
                        emotionMinute = minute
                    }
                }
            }
        }

        SettingsGroup(header = stringResource(R.string.settings_notifications_details)) {
            row {
                SettingsToggleRow(
                    stringResource(R.string.settings_notifications_detail), detail, { detail = it },
                    hint = stringResource(R.string.settings_notifications_detail_hint),
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

    SettingsGroup {
        row {
            SettingsToggleRow(
                stringResource(R.string.settings_notifications_viri), viri, {
                    viri = it
                    if (it) askIfNeeded()
                },
                hint = stringResource(R.string.settings_notifications_viri_hint),
                pictogram = R.drawable.viri_entry_waving,
            )
        }
        if (viri) {
            row {
                SettingsTimeRow(stringResource(R.string.settings_notifications_viri_time), viriHour, viriMinute) { hour, minute ->
                    viriHour = hour
                    viriMinute = minute
                }
            }
        }
        if (isDenied) {
            row { SettingsBlockedRow(stringResource(R.string.habits_reminder_denied)) { openNotificationSettings(context) } }
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
