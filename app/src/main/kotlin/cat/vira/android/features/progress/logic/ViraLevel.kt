package cat.vira.android.features.progress.logic

import cat.vira.android.core.design.components.ViraLevelProgress

/// Nivell derivat de `ProgressCounter.totalXP` (`ViraLevel.swift`). Cap taula ni estat propi: es
/// calcula sempre en viu contra un comptador que només puja.
///
/// Decisió del 2026-08-06 (`VIRI_AND_GAMIFICATION.md` §2.1): un nivell numèric, **sense noms de
/// rang** («Nivell 7», mai «Expert»), i només a la pestanya Viri, mai a la barra de pestanyes.
object ViraLevel {

    /// XP acumulada per **arribar** a `level` (nombre triangular: `50·n·(n+1)`). Nivell 1 als
    /// 100 XP, 2 als 300, 3 als 600: la distància creix 100 XP cada vegada.
    fun thresholdXP(level: Int): Int = if (level <= 0) 0 else 50 * level * (level + 1)

    /// Nivell actual per a aquesta XP. Sense sostre: pujar de nivell per sempre és cosmètic pur.
    fun level(xp: Int): Int {
        if (xp <= 0) return 0
        var level = 0
        while (thresholdXP(level + 1) <= xp) level++
        return level
    }

    /// El que dibuixa `ViraLevelBar`.
    fun progress(xp: Int): ViraLevelProgress {
        val level = level(xp)
        val base = thresholdXP(level)
        val next = thresholdXP(level + 1)
        return ViraLevelProgress(
            level = level,
            totalXP = xp,
            currentLevelXP = maxOf(0, xp - base),
            xpToNext = next - maxOf(xp, base),
        )
    }

    /// Tram cosmètic de nivell: es desbloqueja sol en pujar, no es compra.
    enum class LevelFrame { BRONZE, SILVER, GOLD, DIAMOND }

    /// `null` per sota del nivell 1: cap marc fins que no hi ha cap nivell.
    fun frame(level: Int): LevelFrame? = when {
        level <= 0 -> null
        level <= 4 -> LevelFrame.BRONZE
        level <= 9 -> LevelFrame.SILVER
        level <= 19 -> LevelFrame.GOLD
        else -> LevelFrame.DIAMOND
    }
}
