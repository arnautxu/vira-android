package cat.vira.android.features.help

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cat.vira.android.core.data.ViraPreferences
import kotlin.math.roundToInt

/// La respiració guiada, al palmell (`ViraBreathingHaptics` de l'iPhone).
///
/// No cal un cop, cal una **textura**: una intensitat que puja durant els quatre segons d'inspirar
/// i baixa durant els quatre d'expirar. Qui obre «Ajuda ara» pot estar en un lloc on no vulgui
/// mirar la pantalla; un ritme al palmell deixa fer l'exercici amb els ulls tancats.
///
/// ## Es degrada sense dir res
/// Un mòbil sense control d'intensitat (només sabria fer un brunzit pla, que crida l'atenció en lloc
/// d'acompanyar), la vibració de Vira apagada a Ajustos o el sistema que la refusa: en tots els casos
/// l'exercici continua igual amb el cercle i el text, que ja el porten sencers.
class HelpBreathingHaptics(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    private var isRunning = false

    val isEnabled: Boolean
        get() = vibrator?.let { it.hasVibrator() && it.hasAmplitudeControl() } == true &&
            ViraPreferences.open(appContext).getBoolean(HAPTICS_ENABLED, true)

    fun start() {
        isRunning = isEnabled
    }

    fun stop() {
        if (isRunning) runCatching { vibrator?.cancel() }
        isRunning = false
    }

    /// Un tram: la intensitat puja (inspirar) o baixa (expirar) al llarg de tota la seva durada. Es
    /// dispara un cop per tram, no per segon, perquè encadenar-lo deixaria un forat a cada límit.
    fun play(inhale: Boolean, seconds: Int) {
        if (!isRunning) return
        val (timings, amplitudes) = rampWaveform(inhale, seconds * 1000L)
        // Sense el permís de vibrar, o amb el sistema que no ho deixa, l'exercici segueix igual.
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1)) }
    }

    companion object {
        /// Mateixa clau que l'iPhone (`ViraPreferences.hapticsEnabled`); l'interruptor és d'Ajustos.
        const val HAPTICS_ENABLED = "settings.hapticsEnabled"

        private const val STEP_MILLIS = 100L
        private const val LOW = 0.05f
        private const val HIGH = 0.6f

        /// La corba en graons de 100 ms, de 0,05 a 0,6 (o al revés) de la intensitat màxima: els
        /// mateixos extrems que la corba de l'iPhone.
        fun rampWaveform(inhale: Boolean, durationMillis: Long): Pair<LongArray, IntArray> {
            val steps = (durationMillis / STEP_MILLIS).toInt().coerceAtLeast(1)
            val timings = LongArray(steps) { STEP_MILLIS }
            val amplitudes = IntArray(steps) { index ->
                val progress = if (steps == 1) 1f else index / (steps - 1).toFloat()
                val level = if (inhale) LOW + (HIGH - LOW) * progress else HIGH - (HIGH - LOW) * progress
                (level * 255).roundToInt().coerceIn(1, 255)
            }
            return timings to amplitudes
        }
    }
}
