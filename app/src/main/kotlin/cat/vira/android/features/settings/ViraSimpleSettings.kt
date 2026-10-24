package cat.vira.android.features.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.notifications.ViraNotificationPreferences

/// Ajustos més simples (`ViraSimpleSettings` de l'iPhone, Arnau, 2026-10-23): «no vull que es
/// pugui personalitzar, només lo de medicació» i «ratxa i monedes no s'ha de poder apagar».
///
/// Les claus es queden —el codi que les llegeix és a tot arreu—, però ja no hi ha cap fila que les
/// canviï. Qui les havia apagat abans d'aquesta versió les tindria apagades per sempre i sense cap
/// camí per tornar-les a encendre. Per això aquí es posen al valor de sortida **a cada arrencada**:
/// és idempotent i també tapa una ratxa amagada que torni d'un altre telèfon amb una versió vella.
///
/// - **Avisos**: un sol interruptor, `remindersEnabled`. Els missatges de Viri el segueixen; les
///   categories, el detall i les hores ja no es trien.
/// - **Medicació**: el seu interruptor i el nom del fàrmac no es toquen. Són l'única cosa que es
///   personalitza.
/// - **Ratxa, monedes, reptes, botiga i celebracions**: sempre visibles.
/// - **So i vibració**: un sol interruptor; «Només celebracions» passa a «Tots».
object ViraSimpleSettings {

    suspend fun apply(preferences: SharedPreferences, db: ViraDatabase) {
        val notificationsOn = ViraNotificationPreferences.remindersEnabled(preferences)
        preferences.edit {
            putBoolean(ViraNotificationPreferences.VIRI_MESSAGES_ENABLED, notificationsOn)
            putBoolean(SettingsKeys.STREAK_LIVE_ACTIVITY, notificationsOn)
            ALWAYS_ON.forEach { putBoolean(it, true) }
            // Les hores tornen a les de sortida: ja no es poden triar.
            REMOVED_HOURS.forEach(::remove)
            val mode = ViraSoundMode.from(preferences.getString(SettingsKeys.SOUND_MODE, null))
            val soundOn = mode != ViraSoundMode.OFF
            putString(SettingsKeys.SOUND_MODE, (if (soundOn) ViraSoundMode.ALL else ViraSoundMode.OFF).raw)
            putBoolean(SettingsKeys.HAPTICS, soundOn)
        }
        // Només les files que ja existeixen: crear la ratxa aquí la faria néixer abans d'hora.
        val store = ViraStore(db)
        db.streaks().all().filter { !it.isVisible }.forEach { store.save(db.streaks(), it.copy(isVisible = true)) }
    }

    /// L'interruptor d'«Avisos» amb tot el que el segueix. Ajustos i l'entrada passen per aquí
    /// perquè no puguin quedar desaparellats.
    fun setNotifications(preferences: SharedPreferences, isOn: Boolean) = preferences.edit {
        putBoolean(ViraPreferences.REMINDERS_ENABLED, isOn)
        putBoolean(ViraNotificationPreferences.VIRI_MESSAGES_ENABLED, isOn)
        putBoolean(SettingsKeys.STREAK_LIVE_ACTIVITY, isOn)
    }

    /// So i vibració van junts: qui treu el so a classe també vol el telèfon quiet.
    fun setSounds(preferences: SharedPreferences, isOn: Boolean) = preferences.edit {
        putString(SettingsKeys.SOUND_MODE, (if (isOn) ViraSoundMode.ALL else ViraSoundMode.OFF).raw)
        putBoolean(SettingsKeys.HAPTICS, isOn)
    }

    private val ALWAYS_ON = listOf(
        ViraNotificationPreferences.REMINDERS_HABITS_ENABLED,
        ViraNotificationPreferences.REMINDERS_PLAN_ENABLED,
        ViraNotificationPreferences.REMINDERS_EMOTION_ENABLED,
        ViraNotificationPreferences.REMINDERS_MORNING_ENABLED,
        ViraNotificationPreferences.NOTIFICATION_DETAIL_ENABLED,
        ViraPreferences.CELEBRATIONS_ENABLED,
        ViraPreferences.CHALLENGES_ENABLED,
        SettingsKeys.PROGRESS_VISIBLE,
        SettingsKeys.SHOP_VISIBLE,
        SKIN_SOUNDS_ENABLED,
    )

    private val REMOVED_HOURS = listOf(
        ViraNotificationPreferences.MORNING_REMINDER_HOUR,
        ViraNotificationPreferences.MORNING_REMINDER_MINUTE,
        ViraNotificationPreferences.EMOTION_REMINDER_HOUR,
        ViraNotificationPreferences.EMOTION_REMINDER_MINUTE,
    )

    /// «Sons de la skin» (iPhone, 2026-10-21). Encara no té lector a Android: es deixa al valor
    /// de sortida perquè el dia que n'hi hagi, sigui el mateix que a l'iPhone.
    private const val SKIN_SOUNDS_ENABLED = "settings.skinSoundsEnabled"
}
