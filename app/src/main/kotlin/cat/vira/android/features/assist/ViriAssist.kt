package cat.vira.android.features.assist

import android.content.SharedPreferences
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDateTime

/// Les ajudes de Viri amb Claude (`ViriAssist` de l'iPhone, Arnau 2026-10-19). A Android, de
/// moment, només «La partim?»: partir una tasca gran en passos.
///
/// L'app no parla mai amb Anthropic: envia el text que la persona ha triat a la funció
/// `viri-assist` del nostre servidor, que té la clau, comprova la sessió, el permís del tutor i la
/// quota, i torna una proposta ja validada. Aquí es construeix la petició i es torna a validar la
/// resposta: el servidor és qui mana, però cap camp estrany arriba a la pantalla.
///
/// Regles que no es relaxen: **mai** emocions, medicació ni Ajuda ara; **cap** XP ni moneda per
/// fer-la servir; Viri proposa i la persona tria què es desa, sempre amb Desfer.
object ViriAssist {
    const val POLICY = "viri-assist-1"

    /// El mateix sostre que el servidor per als passos: el que passi es retalla aquí.
    const val STEPS_TEXT_LIMIT = 200

    // Preferències, les mateixes claus que l'iPhone (`ViraPreferences`).

    /// El «D'acord» de «Viri pensa amb Claude».
    const val ACCEPTED = "settings.viriAssistAccepted"
    /// L'interruptor d'Ajustos, encès de fàbrica.
    const val ENABLED = "settings.viriAssistEnabled"
    /// Tasques a les quals ja s'ha respost «La partim?» (sí o no), separades per comes.
    const val SPLIT_ANSWERED = "settings.viriSplitDeclined"

    /// Amb compte de jove i el permís del tutor. Sense backend o sense sessió, les ajudes no
    /// existeixen. El servidor ho torna a comprovar tot: això només decideix si la pregunta surt.
    /// Premium no hi entra: Android és la beta, i a la beta de l'iPhone Premium és sempre actiu.
    fun canUse(session: ViraSession): Boolean {
        val user = session.user ?: return false
        if (user.role != ViraAccountRole.TEEN) return false
        return (session.verifiedConsent ?: user.consent) == ViraConsentStatus.GRANTED
    }

    fun isEnabled(preferences: SharedPreferences) = preferences.getBoolean(ENABLED, true)

    fun isAccepted(preferences: SharedPreferences) = preferences.getBoolean(ACCEPTED, false)

    /// Ja s'ha respost aquí: no es tornarà a proposar. Se'n guarden les últimes cent.
    fun markAnswered(preferences: SharedPreferences, id: java.util.UUID) {
        val stored = preferences.getString(SPLIT_ANSWERED, "").orEmpty()
        preferences.edit().putString(SPLIT_ANSWERED, declining(id.toString(), stored)).apply()
    }

    fun declining(id: String, stored: String): String {
        val list = stored.split(",").filter { it.isNotEmpty() }
        if (id in list) return stored
        return (list + id).takeLast(100).joinToString(",")
    }

    // Petició

    private val weekdays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    /// La petició de passos: el títol i prou, amb el dia i l'hora d'ara perquè els passos encaixin.
    fun stepsRequest(title: String, now: LocalDateTime, language: String): JsonObject = JsonObject(
        mapOf(
            "kind" to JsonPrimitive("steps"),
            "locale" to JsonPrimitive(language.takeIf { it in listOf("ca", "es", "en") } ?: "en"),
            "today" to JsonPrimitive(now.toLocalDate().toString()),
            "weekday" to JsonPrimitive(weekdays[now.dayOfWeek.value - 1]),
            "now" to JsonPrimitive("%02d:%02d".format(now.hour, now.minute)),
            "text" to JsonPrimitive(clean(title, STEPS_TEXT_LIMIT)),
        ),
    )

    /// Treu els caràcters de control que el servidor rebutja i retalla al sostre.
    fun clean(text: String, limit: Int): String {
        val kept = text.codePoints().filter { it >= 0x20 || it == '\n'.code || it == '\t'.code }.limit(limit.toLong()).toArray()
        return String(kept, 0, kept.size).trim()
    }

    // Resposta

    data class StepProposal(val title: String, val minutes: Int)

    enum class Failure(@StringRes val message: Int) {
        UNAVAILABLE(R.string.viri_assist_error),
        CONSENT(R.string.viri_assist_consent),
        LIMIT(R.string.viri_assist_limit),
        SESSION(R.string.viri_assist_session),
    }

    class FailureException(val failure: Failure) : Exception(failure.name)
}
