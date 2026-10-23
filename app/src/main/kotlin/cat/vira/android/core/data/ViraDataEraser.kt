package cat.vira.android.core.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import java.util.UUID

/// «Esborrar-ho tot» del telèfon (`PRIVACY_AND_SAFETY.md` §5, `USER_FLOWS.md` F25).
///
/// No hi ha paperera ni desfer: per això la pantalla que ho crida demana escriure una paraula.
/// Amb compte, l'esborrat de debò és `ViraSyncEngine.eraseEverything`, que buida **primer el
/// servidor** i després crida això; sense, es queda aquí.
///
/// El que Vira deixi fora de la base de dades (avisos programats, widgets) també és de l'usuari:
/// els fils amos ho netegen amb `alsoErase`, que s'executa després de buidar les taules.
object ViraDataEraser {

    suspend fun eraseEverything(
        db: ViraDatabase,
        preferences: SharedPreferences,
        alsoErase: suspend () -> Unit = {},
    ) {
        db.withTransaction {
            // Els fills abans que els pares, encara que la cascada ja ho faria.
            db.assistanceDetails().deleteAll()
            db.assistanceLinks().deleteAll()
            db.assistanceNotes().deleteAll()
            db.planItems().deleteAll()
            db.habitLogs().deleteAll()
            db.habits().deleteAll()
            db.medicationIntakes().deleteAll()
            db.medications().deleteAll()
            db.emotionEntries().deleteAll()
            db.focusSessions().deleteAll()
            db.focusProfiles().deleteAll()
            db.progressCounters().deleteAll()
            db.streaks().deleteAll()
            db.coinTransactions().deleteAll()
            db.personalRewards().deleteAll()
            db.skinOwnerships().deleteAll()
            db.accessoryOwnerships().deleteAll()
            db.badgeOwnerships().deleteAll()
            db.challengeClaims().deleteAll()
            db.profiles().deleteAll()
            // Les làpides tampoc: si ja no queda res, no hi ha res a propagar com a esborrat.
            db.tombstones().deleteAll()
        }
        alsoErase()

        preferences.edit().apply {
            listOf(
                ViraPreferences.APPEARANCE, ViraPreferences.CELEBRATIONS_ENABLED,
                ViraPreferences.REMINDERS_ENABLED, ViraPreferences.NOTIFICATION_DETAIL_ENABLED,
                ViraPreferences.BIOMETRIC_LOCK_ENABLED, ViraPreferences.CHALLENGES_ENABLED,
                ViraPreferences.FRIENDS_LAST_EVENT_AT,
            ).forEach(::remove)
        }.apply()
    }

    /// Buida el telèfon perquè hi entri **un altre compte** (Arnau, 2026-08-12).
    ///
    /// Les files que es pugen no porten `user_id` —el posa el servidor—, així que sense això la
    /// primera sincronització pujaria les dades de l'anterior al compte nou com si fossin seves.
    /// Només s'esborra quan el compte és un altre, mai en tancar sessió.
    suspend fun eraseForAccountSwitch(
        db: ViraDatabase,
        preferences: SharedPreferences,
        alsoErase: suspend () -> Unit = {},
    ) {
        // `eraseEverything` treu l'aparença, que és del telèfon i no de la persona: es guarda abans.
        val kept = preferences.all.filterKeys { it in deviceKeys }
        eraseEverything(db, preferences, alsoErase)
        resetPreferences(preferences)
        preferences.edit().apply {
            kept.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                }
            }
        }.apply()
    }

    /// Sense propietari anotat la resposta és **no**: qui feia servir l'app en local i ara es fa un
    /// compte té dades que són seves.
    fun isAccountSwitch(previousOwner: String?, incoming: UUID): Boolean =
        previousOwner != null && previousOwner != incoming.toString()

    /// Claus que **sobreviuen** a un canvi de compte. La llista és de conservar, no d'esborrar: així
    /// una preferència nova cau per defecte, que és el costat segur de l'error.
    private val deviceKeys = setOf(
        ViraPreferences.APPEARANCE,
        ViraPreferences.ONBOARDING_COMPLETED,
        ViraPreferences.TUTORIAL_COMPLETED,
        // El tomb saltat i «Aquí és Viri» ja vist: estat del telèfon, com el tomb (iPhone, 2026-10-22).
        "settings.tutorialSkipped",
        "settings.viriTabTipSeen",
    )

    val ownPrefixes = listOf("settings.", "cards.", "account.", "suggestions.", "sync.")

    fun resetPreferences(preferences: SharedPreferences) {
        val doomed = preferences.all.keys.filter { key ->
            ownPrefixes.any(key::startsWith) && key !in deviceKeys
        }
        preferences.edit().apply { doomed.forEach(::remove) }.apply()
    }
}
