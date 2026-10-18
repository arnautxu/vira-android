package cat.vira.android.features.emotions

import androidx.room.withTransaction
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.features.progress.logic.ViraProgress
import java.time.Instant

/// Únic punt per convertir una selecció **confirmada** del check-in en un registre real
/// (`ViraEmotionCheckIn` de l'iPhone).
///
/// La pantalla pot mantenir una candidata sense desar-la. Quan l'usuari confirma, aquí historial,
/// ratxa, recompenses i recompte avancen junts **una sola vegada**: tot va dins d'una transacció, i
/// si el disc rebutja el desat no queda res a mitges. Qualsevol altre camí que registri una emoció
/// (els avisos, per exemple) ha de passar per aquí.
object ViraEmotionCheckIn {

    suspend fun record(
        store: ViraStore,
        emotion: Emotion,
        intensity: EmotionIntensity? = null,
        note: String? = null,
        now: Instant = ViraTime.now(),
    ): EmotionEntry = store.db.withTransaction {
        // Un sol instant: un registre a mitjanit compta tot al mateix dia, entrada i ratxa.
        val entry = store.create(
            store.db.emotionEntries(),
            EmotionEntry(emotionRaw = emotion.raw, intensityRaw = intensity?.raw, note = note?.trim()?.ifEmpty { null }, createdAt = now),
        )
        // L'XP, les monedes i la ratxa (`ViraProgress.record(.emotion)` de l'iPhone), i el recompte.
        val progress = ViraProgress(store)
        progress.record(CoinReason.EMOTION, now)
        progress.countEmotionCheckIn()
        entry
    }
}
