package cat.vira.android.features.progress.logic

import cat.vira.android.core.resources.ViraVideo

/// Les onze escenes animades de la ratxa activa (`ViraStreakSceneVideo.swift`).
///
/// La celebració diària recorre aquesta sèrie i la reinicia després de l'onzena escena: cada dia
/// de ratxa, un clip diferent. Les fites de setmana i mes conserven els seus vídeos propis.
object StreakSceneVideo {
    /// Selecció de l'Arnau (2026-09-15): fora els clips 01, 06 i 10.
    val scenes = listOf(
        ViraVideo.StreakDay02, ViraVideo.StreakDay03, ViraVideo.StreakDay04, ViraVideo.StreakDay05,
        ViraVideo.StreakDay07, ViraVideo.StreakDay08, ViraVideo.StreakDay09, ViraVideo.StreakDay11,
        ViraVideo.StreakDay12, ViraVideo.StreakDay13, ViraVideo.StreakDay14,
    )

    fun scene(days: Int): ViraVideo? = if (days <= 0) null else scenes[(days - 1) % scenes.size]

    /// Com es posa el vídeo a la pantalla de celebració, amb els mateixos valors que l'iPhone.
    data class Backdrop(
        val video: ViraVideo,
        val isDark: Boolean = false,
        val rise: Float = 0f,
        val sound: Boolean = false,
        val textOnTop: Boolean = false,
        val protectsText: Boolean = false,
    )

    fun celebrationBackdrop(days: Int, milestoneKey: String?, soundEnabled: Boolean): Backdrop? = when (milestoneKey) {
        // Les escenes alternen cel, foc i fons clars: el text necessita una base opaca.
        null -> scene(days)?.let { Backdrop(it, protectsText = true) }
            ?: Backdrop(ViraVideo.ViriStreakCelebrate, rise = 0.20f)
        StreakMilestone.WEEK_CARD -> Backdrop(ViraVideo.ViriStreakWeek, isDark = true, sound = soundEnabled, textOnTop = true)
        StreakMilestone.MONTH_CARD -> Backdrop(ViraVideo.ViriStreakMonth, isDark = true, sound = soundEnabled)
        else -> null
    }
}
