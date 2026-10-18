package cat.vira.android.core.resources

import androidx.annotation.RawRes
import cat.vira.android.R

// Generat per scripts/export-videos.py. No es toca a mà.

/// Com va empaquetada la transparència d'un vídeo: H.264 no té canal alfa, i els clips que
/// a l'iPhone en porten surten amb el color i l'alfa (en grisos) a dues meitats del fotograma.
enum class ViraVideoAlpha {
    /// Sense transparència: el fotograma sencer és la imatge.
    None,
    /// Color a la meitat de dalt, alfa a la de sota.
    Stacked,
    /// Color a la meitat de l'esquerra, alfa a la de la dreta.
    SideBySide,
}

/// Els vídeos de l'iPhone (`Vira/Resources/*.mov`), amb la mida de la imatge (no del fitxer).
enum class ViraVideo(
    @RawRes val resource: Int,
    val alpha: ViraVideoAlpha,
    val width: Int,
    val height: Int,
    val framesPerSecond: Double,
    val frameCount: Int,
) {
    HabitDone(R.raw.habit_done, ViraVideoAlpha.None, 720, 1280, 24.0, 65),
    StreakBannerLoop(R.raw.streak_banner_loop, ViraVideoAlpha.None, 1080, 688, 24.0, 165),
    StreakDay02(R.raw.streak_day_02, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay03(R.raw.streak_day_03, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay04(R.raw.streak_day_04, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay05(R.raw.streak_day_05, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay07(R.raw.streak_day_07, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay08(R.raw.streak_day_08, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay09(R.raw.streak_day_09, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay11(R.raw.streak_day_11, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay12(R.raw.streak_day_12, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay13(R.raw.streak_day_13, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakDay14(R.raw.streak_day_14, ViraVideoAlpha.None, 360, 640, 20.0, 100),
    StreakFlameIgnite(R.raw.streak_flame_ignite, ViraVideoAlpha.Stacked, 600, 600, 60.0, 180),
    StreakFlameLoop(R.raw.streak_flame_loop, ViraVideoAlpha.Stacked, 256, 256, 20.0, 46),
    StreakLostLoop(R.raw.streak_lost_loop, ViraVideoAlpha.None, 1080, 676, 24.0, 238),
    ViriChillLoop(R.raw.viri_chill_loop, ViraVideoAlpha.None, 720, 720, 24.0, 480),
    ViriEmotionAngry(R.raw.viri_emotion_angry, ViraVideoAlpha.None, 540, 540, 24.0, 480),
    ViriEmotionAnxious(R.raw.viri_emotion_anxious, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionAshamed(R.raw.viri_emotion_ashamed, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionBlocked(R.raw.viri_emotion_blocked, ViraVideoAlpha.Stacked, 512, 512, 24.0, 144),
    ViriEmotionConfused(R.raw.viri_emotion_confused, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionContent(R.raw.viri_emotion_content, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionEuphoric(R.raw.viri_emotion_euphoric, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionLonely(R.raw.viri_emotion_lonely, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionLowEnergy(R.raw.viri_emotion_low_energy, ViraVideoAlpha.Stacked, 512, 512, 24.0, 144),
    ViriEmotionSad(R.raw.viri_emotion_sad, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionScared(R.raw.viri_emotion_scared, ViraVideoAlpha.None, 512, 512, 24.0, 144),
    ViriEmotionTired(R.raw.viri_emotion_tired, ViraVideoAlpha.Stacked, 512, 512, 24.0, 144),
    ViriEmotionUnmotivated(R.raw.viri_emotion_unmotivated, ViraVideoAlpha.Stacked, 512, 512, 24.0, 144),
    ViriFocusedLoop(R.raw.viri_focused_loop, ViraVideoAlpha.None, 720, 720, 24.0, 480),
    ViriMeditatingLoop(R.raw.viri_meditating_loop, ViraVideoAlpha.None, 720, 720, 24.0, 144),
    ViriStreakCelebrate(R.raw.viri_streak_celebrate, ViraVideoAlpha.None, 960, 1940, 30.0, 153),
    ViriStreakLoop(R.raw.viri_streak_loop, ViraVideoAlpha.None, 540, 840, 24.0, 68),
    ViriStreakMonth(R.raw.viri_streak_month, ViraVideoAlpha.None, 720, 1280, 23.5, 94),
    ViriStreakWeek(R.raw.viri_streak_week, ViraVideoAlpha.None, 720, 1280, 24.0, 240);
}
