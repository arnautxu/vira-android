package cat.vira.android.features.entry

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.notifications.ViraNotificationWorker

/// Què se sap del permís d'avisos. Android no diu "encara no preguntat": ho sap el flux, que
/// recorda si ja l'ha demanat (`entry.notificationsAsked`).
enum class EntryNotificationStatus { NOT_DETERMINED, DENIED, AUTHORIZED }

/// E10: permís d'avisos **i de què avisa Vira** (`EntryNotificationsStep.swift`).
///
/// A dalt, la vista prèvia d'un avís. El pas diu el que passa de debò:
/// - **Sense respondre encara** → "Activa", i el flux espera el diàleg del sistema.
/// - **Denegat** → ho diu i ofereix els Ajustos del sistema, l'únic lloc on es pot canviar.
/// - **Ja concedit** → les categories i "Continua". Hàbits i medicació només surten amb el mòdul encès.
///
/// "Ara no" **no dispara mai el diàleg**: només "Activa" el dispara.
@Composable
fun EntryNotificationsStep(prefs: EntryPreferences, taskTitle: String?, onContinue: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val asked = prefs.boolean(EntryPreferenceKeys.NOTIFICATIONS_ASKED, false)
    var status by remember { mutableStateOf(if (inPreview) EntryNotificationStatus.NOT_DETERMINED else notificationStatus(context, asked.value)) }
    var awaitsAnswer by remember { mutableStateOf(false) }
    // Els avisos es refan en sortir del pas, surti per on surti (Continua, "Ara no", "Saltar" o
    // Enrere): els interruptors de les categories poden haver canviat. Va per WorkManager perquè
    // el flux pot tancar-se abans que la passada acabi.
    DisposableEffect(Unit) {
        onDispose { if (!inPreview) ViraNotificationWorker.request(context) }
    }

    // En tornar dels Ajustos del sistema, el permís pot haver canviat.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !inPreview) status = notificationStatus(context, asked.value)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        asked.value = true
        status = if (granted) EntryNotificationStatus.AUTHORIZED else notificationStatus(context, true)
        awaitsAnswer = false
        // Amb el permís concedit el pas **no** es tanca sol: ara és quan apareixen les categories.
        // Però els avisos ja es poden posar, per si l'app es tanca aquí mateix.
        if (granted) ViraNotificationWorker.request(context) else onContinue()
    }
    val canAsk = status == EntryNotificationStatus.NOT_DETERMINED
    val ink = ViraColor.onCard(ViraArea.Rewards)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
        EntryNotificationPreview(taskTitle)
        if (status == EntryNotificationStatus.DENIED) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                Text(stringResource(R.string.habits_reminder_denied), style = ViraFont.footnote, color = ink)
                EntryLinkButton(stringResource(R.string.action_open_settings), { context.openNotificationSettings() }, centered = false)
            }
        }
        if (status == EntryNotificationStatus.AUTHORIZED) EntryNotificationCategories(prefs)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            EntryPrimaryButton(
                stringResource(if (canAsk) R.string.action_activate else R.string.action_continue),
                {
                    if (!canAsk) {
                        onContinue()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        awaitsAnswer = true
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        status = notificationStatus(context, true)
                    }
                },
                isLoading = awaitsAnswer,
            )
            // "Ara no" només té sentit mentre hi hagi alguna cosa a dir que no.
            if (canAsk) EntryLinkButton(stringResource(R.string.action_not_now), onSkip)
        }
    }
}

@Composable
private fun EntryNotificationCategories(prefs: EntryPreferences) {
    val reminders = prefs.boolean(EntryPreferenceKeys.REMINDERS_ENABLED, true)
    val habitsModule = prefs.boolean(EntryPreferenceKeys.MODULE_HABITS_ENABLED, true)
    val medicationModule = prefs.boolean(EntryPreferenceKeys.MODULE_MEDICATION_ENABLED, true)
    val habits = prefs.boolean(EntryPreferenceKeys.REMINDERS_HABITS_ENABLED, true)
    val medication = prefs.boolean(EntryPreferenceKeys.REMINDERS_MEDICATION_ENABLED, true)
    val plan = prefs.boolean(EntryPreferenceKeys.REMINDERS_PLAN_ENABLED, true)
    val emotion = prefs.boolean(EntryPreferenceKeys.REMINDERS_EMOTION_ENABLED, true)
    val viri = prefs.boolean(EntryPreferenceKeys.VIRI_MESSAGES_ENABLED, true)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Text(stringResource(R.string.onboarding_notifications_categories), style = ViraFont.footnote, color = ViraColor.onCard(ViraArea.Rewards))
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            EntryToggleRow(stringResource(R.string.settings_notifications_reminders), reminders.value, { reminders.value = it })
            if (reminders.value) {
                if (habitsModule.value) EntryToggleRow(stringResource(R.string.settings_notifications_category_habits), habits.value, { habits.value = it })
                if (medicationModule.value) EntryToggleRow(stringResource(R.string.settings_notifications_category_medication), medication.value, { medication.value = it })
                EntryToggleRow(stringResource(R.string.settings_notifications_category_plan), plan.value, { plan.value = it })
                EntryToggleRow(stringResource(R.string.settings_notifications_category_emotion), emotion.value, { emotion.value = it })
            }
            EntryToggleRow(
                stringResource(R.string.settings_notifications_viri),
                viri.value,
                { viri.value = it },
                hint = stringResource(R.string.settings_notifications_viri_hint),
            )
        }
        EntryTogglesFootnote()
    }
}

/// Targeta que imita un avís, a dalt del pas: **què** arribaria abans de demanar el permís. Fa
/// servir el títol de la tasca de la primera acció, o una de mostra.
///
/// **Mai ensenya medicació ni cap emoció**, ni de mostra. Per a TalkBack diu que és un exemple.
@Composable
fun EntryNotificationPreview(taskTitle: String?) {
    val body = taskTitle?.trim().takeUnless { it.isNullOrEmpty() } ?: stringResource(R.string.onboarding_reminders_preview_sample)
    val label = stringResource(R.string.onboarding_reminders_preview_label, body)
    val iconShape = RoundedCornerShape(ViraRadius.s)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))
            .clearAndSetSemantics { contentDescription = label }
            .padding(ViraSpacing.s),
    ) {
        Image(
            painterResource(R.drawable.vira_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(32.dp).clip(iconShape).background(ViraGradient.card(ViraArea.Today), iconShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text("Vira", style = ViraFont.headline, color = ViraColor.textPrimary)
            Text(body, style = ViraFont.callout, color = ViraColor.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/// Android 13 o posterior pregunta per l'app; abans, els avisos hi són si no s'han apagat.
/// Després d'un "no", el sistema encara deixa tornar a preguntar si ofereix una explicació.
internal fun notificationStatus(context: android.content.Context, asked: Boolean): EntryNotificationStatus {
    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return EntryNotificationStatus.AUTHORIZED
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return EntryNotificationStatus.DENIED
    val activity = context.findActivity()
    val canAskAgain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
    return if (!asked || canAskAgain) EntryNotificationStatus.NOT_DETERMINED else EntryNotificationStatus.DENIED
}

private fun android.content.Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
