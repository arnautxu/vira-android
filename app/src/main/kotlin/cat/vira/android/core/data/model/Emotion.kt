package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Registre emocional (`EmotionEntry` de l'iPhone). Capa `emotions`: no surt del telèfon sense
/// l'interruptor **i** el permís del tutor (`ViraSyncScope`).
@Entity(tableName = "EmotionEntry", indices = [Index("createdAt")])
data class EmotionEntry(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val emotionRaw: String = Emotion.CONTENT.raw,
    /// 1 (gens) … 5 (moltíssim), com `EmotionIntensity` de l'iPhone.
    val intensityRaw: Int? = null,
    val note: String? = null,
    val createdAt: Instant = ViraTime.now(),
) : ViraSyncable<EmotionEntry> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

val EmotionEntry.emotion: Emotion get() = Emotion.from(emotionRaw)

/// Les dinou emocions de l'iPhone, amb el mateix valor en cru i el mateix ordre (el de la graella i
/// el d'«Una altra», no cap escala). Les sis agradables noves són del 2026-10-23.
enum class Emotion(val raw: String) {
    CONTENT("content"), CALM("calm"), PROUD("proud"), ANGRY("angry"), ANXIOUS("anxious"), SAD("sad"),
    EUPHORIC("euphoric"), MOTIVATED("motivated"), GRATEFUL("grateful"), EXCITED("excited"),
    RELIEVED("relieved"), LONELY("lonely"), SCARED("scared"), ASHAMED("ashamed"), CONFUSED("confused"), TIRED("tired"),
    LOW_ENERGY("lowEnergy"), UNMOTIVATED("unmotivated"), BLOCKED("blocked");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: CONTENT
    }
}
