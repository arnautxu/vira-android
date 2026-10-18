package cat.vira.android.core.backend.sync

import cat.vira.android.core.data.model.CoinReason
import java.time.Duration

/// El rastre del check-in emocional que viu fora de la capa d'emocions (Arnau, 2026-10-18; calca de
/// `ViraSyncRows+EmotionCounts.swift`).
///
/// `emotion_entries` no surt del telèfon sense l'interruptor **i** el permís del tutor
/// (`ViraSyncScope`), però el check-in deixa marques a la capa `CORE`: el recompte
/// `progress_counters.emotion_checkins_count`, una fila de `coin_transactions` amb motiu `emotion`
/// i el repte diari o setmanal d'emocions a `challenge_claims`. Juntes diuen quan i quantes vegades
/// s'ha registrat una emoció, i segueixen la mateixa porta que els registres.
///
/// - **Pujada**: el recompte no s'envia (camp absent, no zero, perquè no trepitgi el d'un altre
///   telèfon que sí que pot), i les monedes i els reptes d'emoció es queden aquí.
/// - **Baixada**: el recompte del servidor no substitueix el local; les monedes sí que entren,
///   perquè baixar-les no fa sortir res i el saldo s'ha de poder quadrar.
/// - **Quan s'obre la porta**, el motor torna a pujar la taula sencera una vegada
///   (`ViraSyncEngine.push`). Perquè aquesta segona pujada del comptador passi `keep_last_write`
///   (`0005`), la pujada sense recompte surt **un mil·lisegon més vella** que el model.
///
/// El servidor ho fa complir pel seu compte (`20261018120000_emotion_counts_consent.sql`).
///
/// La llavor «legacy» de l'iPhone (`seedLegacyEmotionReplay`) no hi és: cap build d'Android va
/// pujar mai aquestes files sense la porta.
object ViraEmotionTrace {
    /// Prou per quedar per sota al servidor (`timestamptz`) i res més.
    val heldUploadOffset: Duration = Duration.ofMillis(1)

    /// Els reptes que només es compleixen registrant una emoció (`ChallengeCatalog`).
    val challengeKeys = setOf("daily_emotion", "weekly_emotion")

    val coinReason = CoinReason.EMOTION.raw

    fun withholds(scope: ViraSyncScope) = !scope.allows(ViraSyncLayer.EMOTIONS)
}
