package cat.vira.android.features.emotions

import android.content.SharedPreferences
import androidx.core.content.edit
import cat.vira.android.core.data.model.Emotion

/// Preferències locals del primer nivell de cares del check-in (`EmotionPreferences` de l'iPhone).
///
/// El selector no elimina cap emoció: el que no queda aquí apareix a «Una altra». La clau viu sota
/// `settings.` perquè l'esborrat de dades (`ViraDataEraser.resetPreferences`) la cobreixi. L'iPhone
/// la desa com a llista; aquí, els valors en cru separats per comes, perquè un conjunt perdria
/// l'ordre de la graella.
object EmotionPreferences {
    const val PRIMARY_KEY = "settings.emotions.primary"

    fun primary(preferences: SharedPreferences): List<Emotion> {
        val stored = preferences.getString(PRIMARY_KEY, null) ?: return Emotion.primary
        val decoded = stored.split(',').mapNotNull { raw -> Emotion.entries.firstOrNull { it.raw == raw } }.distinct()
        return decoded.ifEmpty { Emotion.primary }
    }

    fun other(primary: List<Emotion>): List<Emotion> = Emotion.entries.filter { it !in primary }

    /// Desa una selecció vàlida i rebutja la buida perquè sempre quedi un camí directe.
    fun setPrimary(emotions: List<Emotion>, preferences: SharedPreferences): Boolean {
        val values = emotions.distinct()
        if (values.isEmpty()) return false
        preferences.edit { putString(PRIMARY_KEY, values.joinToString(",") { it.raw }) }
        return true
    }

    fun reset(preferences: SharedPreferences) = preferences.edit { remove(PRIMARY_KEY) }
}
