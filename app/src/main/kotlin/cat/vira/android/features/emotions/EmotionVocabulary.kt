package cat.vira.android.features.emotions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.emotion
import cat.vira.android.core.resources.ViraVideo

/// El que l'iPhone posa dins d'`Emotion` i `EmotionIntensity` (`Vira/Models/Emotion.swift`) i que
/// a Android no és dada sinó presentació: el nom, la cara i el vídeo de cada emoció.
///
/// L'enum en cru viu a `core/data` perquè la fila que es sincronitza sigui la mateixa; el que
/// cal per pintar-la, aquí.

/// Les sis de sortida: tres agradables i tres desagradables (Arnau, 2026-10-23). Qui ja ha
/// personalitzat la graella conserva la seva (`EmotionPreferences`).
val Emotion.Companion.primary: List<Emotion>
    get() = listOf(Emotion.CONTENT, Emotion.CALM, Emotion.PROUD, Emotion.SAD, Emotion.ANXIOUS, Emotion.ANGRY)

/// Les emocions desagradables poden oferir una sortida d'ajuda si la intensitat és alta.
/// No serveix per jutjar-les ni per ordenar-les: no hi ha emocions millors ni pitjors.
val Emotion.isUnpleasant: Boolean
    get() = when (this) {
        Emotion.CONTENT, Emotion.CALM, Emotion.PROUD, Emotion.EUPHORIC, Emotion.MOTIVATED,
        Emotion.GRATEFUL, Emotion.EXCITED, Emotion.RELIEVED -> false
        Emotion.ANGRY, Emotion.ANXIOUS, Emotion.CONFUSED, Emotion.LONELY, Emotion.SCARED,
        Emotion.ASHAMED, Emotion.SAD, Emotion.TIRED, Emotion.LOW_ENERGY, Emotion.UNMOTIVATED,
        Emotion.BLOCKED -> true
    }

@get:StringRes
val Emotion.label: Int
    get() = when (this) {
        Emotion.CONTENT -> R.string.emotion_content
        Emotion.CALM -> R.string.emotion_calm
        Emotion.PROUD -> R.string.emotion_proud
        Emotion.MOTIVATED -> R.string.emotion_motivated
        Emotion.GRATEFUL -> R.string.emotion_grateful
        Emotion.EXCITED -> R.string.emotion_excited
        Emotion.RELIEVED -> R.string.emotion_relieved
        Emotion.ANGRY -> R.string.emotion_angry
        Emotion.ANXIOUS -> R.string.emotion_anxious
        Emotion.SAD -> R.string.emotion_sad
        Emotion.EUPHORIC -> R.string.emotion_euphoric
        Emotion.LONELY -> R.string.emotion_lonely
        Emotion.SCARED -> R.string.emotion_scared
        Emotion.ASHAMED -> R.string.emotion_ashamed
        Emotion.CONFUSED -> R.string.emotion_confused
        Emotion.TIRED -> R.string.emotion_tired
        Emotion.LOW_ENERGY -> R.string.emotion_low_energy
        Emotion.UNMOTIVATED -> R.string.emotion_unmotivated
        Emotion.BLOCKED -> R.string.emotion_blocked
    }

/// El primer fotograma del vídeo de l'emoció (`stillAssetName`): una escena sencera que omple el
/// cercle. Les quatre d'energia i les sis agradables noves encara no en tenen i cauen al retall
/// (`cutout`).
@get:DrawableRes
val Emotion.still: Int?
    get() = when (this) {
        Emotion.CONTENT -> R.drawable.viri_emotion_content_still
        Emotion.ANGRY -> R.drawable.viri_emotion_angry_still
        Emotion.ANXIOUS -> R.drawable.viri_emotion_anxious_still
        Emotion.SAD -> R.drawable.viri_emotion_sad_still
        Emotion.EUPHORIC -> R.drawable.viri_emotion_euphoric_still
        Emotion.LONELY -> R.drawable.viri_emotion_lonely_still
        Emotion.SCARED -> R.drawable.viri_emotion_scared_still
        Emotion.ASHAMED -> R.drawable.viri_emotion_ashamed_still
        Emotion.CONFUSED -> R.drawable.viri_emotion_confused_still
        Emotion.TIRED, Emotion.LOW_ENERGY, Emotion.UNMOTIVATED, Emotion.BLOCKED,
        Emotion.CALM, Emotion.PROUD, Emotion.MOTIVATED, Emotion.GRATEFUL, Emotion.EXCITED, Emotion.RELIEVED -> null
    }

/// El retall de Viri sense fons (`assetName`).
@get:DrawableRes
val Emotion.cutout: Int
    get() = when (this) {
        Emotion.CONTENT -> R.drawable.viri_emotion_content
        Emotion.CALM -> R.drawable.viri_emotion_calm
        Emotion.PROUD -> R.drawable.viri_emotion_proud
        Emotion.MOTIVATED -> R.drawable.viri_emotion_motivated
        Emotion.GRATEFUL -> R.drawable.viri_emotion_grateful
        Emotion.EXCITED -> R.drawable.viri_emotion_excited
        Emotion.RELIEVED -> R.drawable.viri_emotion_relieved
        Emotion.ANGRY -> R.drawable.viri_emotion_angry
        Emotion.ANXIOUS -> R.drawable.viri_emotion_anxious
        Emotion.SAD -> R.drawable.viri_emotion_sad
        Emotion.EUPHORIC -> R.drawable.viri_emotion_euphoric
        Emotion.LONELY -> R.drawable.viri_emotion_lonely
        Emotion.SCARED -> R.drawable.viri_emotion_scared
        Emotion.ASHAMED -> R.drawable.viri_emotion_ashamed
        Emotion.CONFUSED -> R.drawable.viri_emotion_confused
        Emotion.TIRED -> R.drawable.viri_emotion_tired
        Emotion.LOW_ENERGY -> R.drawable.viri_emotion_low_energy
        Emotion.UNMOTIVATED -> R.drawable.viri_emotion_unmotivated
        Emotion.BLOCKED -> R.drawable.viri_emotion_blocked
    }

/// Viri fent l'emoció, en moviment (`ViraAlphaVideo.Name.emotion`). Les sis agradables noves encara
/// no en tenen: com a l'iPhone (`ViraAlphaVideo.exists`), queda la cara quieta.
val Emotion.video: ViraVideo?
    get() = when (this) {
        Emotion.CALM, Emotion.PROUD, Emotion.MOTIVATED, Emotion.GRATEFUL, Emotion.EXCITED, Emotion.RELIEVED -> null
        Emotion.CONTENT -> ViraVideo.ViriEmotionContent
        Emotion.ANGRY -> ViraVideo.ViriEmotionAngry
        Emotion.ANXIOUS -> ViraVideo.ViriEmotionAnxious
        Emotion.SAD -> ViraVideo.ViriEmotionSad
        Emotion.EUPHORIC -> ViraVideo.ViriEmotionEuphoric
        Emotion.LONELY -> ViraVideo.ViriEmotionLonely
        Emotion.SCARED -> ViraVideo.ViriEmotionScared
        Emotion.ASHAMED -> ViraVideo.ViriEmotionAshamed
        Emotion.CONFUSED -> ViraVideo.ViriEmotionConfused
        Emotion.TIRED -> ViraVideo.ViriEmotionTired
        Emotion.LOW_ENERGY -> ViraVideo.ViriEmotionLowEnergy
        Emotion.UNMOTIVATED -> ViraVideo.ViriEmotionUnmotivated
        Emotion.BLOCKED -> ViraVideo.ViriEmotionBlocked
    }

/// Intensitat de cinc nivells amb etiqueta verbal (`EmotionIntensity`). `raw` és el valor que es
/// desa i viatja (1 … 5).
enum class EmotionIntensity(val raw: Int, @StringRes val label: Int) {
    NONE(1, R.string.intensity_1),
    LITTLE(2, R.string.intensity_2),
    QUITE(3, R.string.intensity_3),
    MUCH(4, R.string.intensity_4),
    VERY_MUCH(5, R.string.intensity_5);

    companion object {
        fun from(raw: Int?): EmotionIntensity? = entries.firstOrNull { it.raw == raw }
    }
}

val EmotionEntry.intensity: EmotionIntensity? get() = EmotionIntensity.from(intensityRaw)

/// Si l'emoció és desagradable i molt intensa, l'app **ofereix** una via d'ajuda. No alerta
/// ningú, no bloqueja res i no s'imposa.
fun offersSupport(emotion: Emotion, intensity: EmotionIntensity?): Boolean =
    emotion.isUnpleasant && (intensity == EmotionIntensity.MUCH || intensity == EmotionIntensity.VERY_MUCH)

val EmotionEntry.shouldOfferSupport: Boolean get() = offersSupport(emotion, intensity)
