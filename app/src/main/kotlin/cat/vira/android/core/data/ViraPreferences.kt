package cat.vira.android.core.data

import android.content.Context
import android.content.SharedPreferences

/// Claus de preferències, amb **els mateixos noms que l'iPhone** (`ViraPreferences.swift`).
///
/// Aquí hi ha les que fan servir les dades i la sincronització. Les de cada pantalla les afegeix el
/// fil amo de la pantalla, a la seva carpeta, amb el mateix nom que tenen a l'iPhone; el que importa
/// és el prefix (`settings.`, `cards.`, `account.`, `suggestions.`, `sync.`), perquè és el que fa
/// caure una preferència en canviar de compte (`ViraDataEraser.resetPreferences`).
object ViraPreferences {

    /// Fitxer de `SharedPreferences` de l'app: l'equivalent d'`UserDefaults.standard`.
    const val FILE = "vira"

    fun open(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Sincronització de dades sensibles. **Encesos de fàbrica** des del 2026-08-12 (Arnau): apagats,
    // el que l'usuari registrava no tornava mai en canviar de telèfon. El que protegeix els menors
    // és el permís del tutor, i aquest no depèn d'aquí (`ViraSyncScope`).
    const val SYNC_EMOTIONS_ENABLED = "settings.syncEmotionsEnabled"
    const val SYNC_MEDICATION_ENABLED = "settings.syncMedicationEnabled"
    const val SYNC_EMOTIONS_DEFAULT = true
    const val SYNC_MEDICATION_DEFAULT = true

    /// De quin compte són les dades que hi ha ara al telèfon (`ViraSession.claimLocalData`).
    const val DATA_OWNER_ACCOUNT_ID = "settings.dataOwnerAccountID"

    /// Prefix dels cursors de sincronització, per taula i direcció.
    const val SYNC_CURSOR_PREFIX = "sync.cursor."

    // Les que l'esborrat total torna als valors de fàbrica (`ViraDataEraser.eraseEverything`).
    const val APPEARANCE = "settings.appearance"
    const val CELEBRATIONS_ENABLED = "settings.celebrationsEnabled"
    const val REMINDERS_ENABLED = "settings.remindersEnabled"
    const val NOTIFICATION_DETAIL_ENABLED = "settings.notificationDetailEnabled"
    const val BIOMETRIC_LOCK_ENABLED = "settings.biometricLockEnabled"
    const val CHALLENGES_ENABLED = "settings.challengesEnabled"
    const val FRIENDS_LAST_EVENT_AT = "settings.friends.lastEventAt"

    // Les que descriuen el telèfon i no la persona: sobreviuen a un canvi de compte.
    const val ONBOARDING_COMPLETED = "settings.onboardingCompleted"
    const val TUTORIAL_COMPLETED = "settings.tutorialCompleted"
}
