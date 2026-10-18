package cat.vira.android.features.emotions

import cat.vira.android.core.data.model.Emotion
import java.util.UUID

/// Valor temporal del full de detall. No és una fila de Room i no pot entrar a l'historial fins
/// que l'usuari prem «Confirma».
data class EmotionCheckInDraft(
    val emotion: Emotion,
    val intensity: EmotionIntensity? = null,
    val note: String = "",
    val id: UUID = UUID.randomUUID(),
) {
    val normalizedNote: String? get() = note.trim().ifEmpty { null }

    val shouldOfferSupport: Boolean get() = offersSupport(emotion, intensity)
}
