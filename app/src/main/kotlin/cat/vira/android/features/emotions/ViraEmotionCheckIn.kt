package cat.vira.android.features.emotions

import androidx.room.withTransaction
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry

/// Únic punt per convertir una selecció **confirmada** del check-in en un registre real
/// (`ViraEmotionCheckIn` de l'iPhone).
///
/// La pantalla pot mantenir una candidata sense desar-la. Quan l'usuari confirma, aquí historial,
/// ratxa, recompenses i recompte avancen junts **una sola vegada**: tot va dins d'una transacció, i
/// si el disc rebutja el desat no queda res a mitges. Qualsevol altre camí que registri una emoció
/// (els avisos, per exemple) ha de passar per aquí.
object ViraEmotionCheckIn {

    /// L'XP, les monedes i la ratxa (`ViraProgress.record(.emotion)` de l'iPhone). Els porta el fil
    /// de Viri i ratxa; fins que arribin a `main`, la crida és buida.
    // TODO(emocions): substituir per `ViraProgress.record(store, CoinReason.EMOTION)` en fusionar.
    internal var progress: suspend (ViraStore, CoinReason) -> Unit = { _, _ -> }

    suspend fun record(
        store: ViraStore,
        emotion: Emotion,
        intensity: EmotionIntensity? = null,
        note: String? = null,
    ): EmotionEntry = store.db.withTransaction {
        val entry = store.create(
            store.db.emotionEntries(),
            EmotionEntry(emotionRaw = emotion.raw, intensityRaw = intensity?.raw, note = note?.trim()?.ifEmpty { null }),
        )
        progress(store, CoinReason.EMOTION)
        val counter = store.counter()
        store.save(store.db.progressCounters(), counter.copy(emotionCheckinsCount = counter.emotionCheckinsCount + 1))
        entry
    }
}
