package cat.vira.android.features.progress.logic

import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.CoinTransaction
import java.time.Instant
import java.time.ZoneId

/// «Tres dies amb Vira»: el primer premi a l'abast (`ViraWelcomeChallenge.swift`, Arnau,
/// 2026-10-22, punts de fricció).
///
/// A producció, la mediana era de 28 ViriCoins per dia actiu i l'article més barat de la botiga en
/// costa 300: el primer premi arribava a les dues o tres setmanes. Amb aquest repte, les ulleres
/// arriben cap al quart dia.
///
/// - **200 ViriCoins, un sol cop**, quan hi ha hagut **tres dies diferents** amb alguna acció que
///   dona monedes (tasca, hàbit, com estàs o focus). Ni seguits ni amb data límit: res de compte
///   enrere (`VIRI_AND_GAMIFICATION.md` §2.2.1).
/// - Medicació i Mindfulness no donen monedes i, per tant, no compten. Crear coses tampoc.
/// - Es desa com un `ChallengeClaim` sense cicle (`cycleStart` = l'època), de manera que se
///   sincronitza i no torna mai. Pagar-lo és `ViraProgress.claimWelcome`.
object ViraWelcomeChallenge {
    const val KEY = "welcome_three_days"
    const val DAYS = 3
    const val REWARD = 200

    /// Sense cicle: la data fixa fa que la fila sigui única per compte.
    val CYCLE_START: Instant = Instant.EPOCH

    /// Els motius que compten: fer alguna cosa. Mai `CHALLENGE` (seria comptar-se a si mateix) ni
    /// les compres.
    val countedReasons: Set<String> = setOf(
        CoinReason.PLAN_ITEM.raw, CoinReason.HABIT.raw, CoinReason.EMOTION.raw, CoinReason.FOCUS.raw,
    )

    /// Quants dies diferents amb alguna acció que compta, fins a `DAYS`.
    fun activeDays(ledger: List<CoinTransaction>, zone: ZoneId = ZoneId.systemDefault(), now: Instant? = null): Int =
        ledger.asSequence()
            .filter { it.amount > 0 && it.reasonRaw in countedReasons && (now == null || it.createdAt <= now) }
            .map { it.createdAt.atZone(zone).toLocalDate() }
            .toSet()
            .size
            .coerceAtMost(DAYS)

    fun isClaimed(claims: List<ChallengeClaim>): Boolean = claims.any { it.key == KEY }
}
