package cat.vira.android.core.sound

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cat.vira.android.core.data.ViraPreferences
import kotlin.math.roundToInt

/// Una ona de vibració: trams en ms i amplituds de 0 a 255 (`VibrationEffect.createWaveform`).
class ViraHapticWave(val timings: LongArray, val amplitudes: IntArray)

/// El vocabulari hàptic de Vira (`ViraHaptics` i `ViraHapticPatterns` de l'iPhone).
///
/// Els casos són **semàntics**: el que canvia és el que ha passat, no quin efecte toca. Les
/// pantalles no criden el vibrador pel seu compte.
///
/// ## Quan sona un so
/// Un so de Vira porta la seva vibració (`ViraHapticPatternData`, de l'`.ahap` de l'iPhone). Si en
/// aquell moment també es demana una vibració genèrica (`DONE` en marcar, `SELECTION` en triar), es
/// notarien les dues; per això les genèriques que acompanyen un so esperen un instant i se
/// n'aparten si `claim` ha dit que el so ja vibra.
///
/// ## Es degrada sense dir res
/// Un mòbil sense control d'amplitud fa els efectes predefinits del sistema; sense vibrador, o amb
/// la vibració de Vira apagada (`settings.hapticsEnabled`), no passa res.
object ViraHaptics {

    sealed interface Feedback {
        /// Un toc de reconeixement: obrir una fila, prémer un xip.
        data object Tap : Feedback
        /// Moure's dins d'un selector.
        data object Selection : Feedback
        /// Triar un nivell d'1 a 5 (la intensitat d'una emoció): com més amunt, més fort.
        data class Intensity(val level: Int) : Feedback
        /// Marcar una cosa com a feta.
        data object Done : Feedback
        /// Desar o confirmar el que l'usuari ha escrit.
        data object Saved : Feedback
        /// Has guanyat una cosa: dos temps, perquè es distingeixi de `Saved`.
        data object Reward : Feedback
        /// La ViriCoin gran surt girant a la fila.
        data object CoinBurst : Feedback
        /// Una de les sis monedes (0…5) aterra a la capçalera, cada una més forta.
        data class CoinLanding(val index: Int) : Feedback
        /// No arribes al preu, el límit diari ja és ple: no és un error.
        data object Warning : Feedback
        data object Error : Feedback

        /// Les que poden anar amb un so de Vira. Els avisos, els errors i la intensitat mai.
        val accompaniesSound: Boolean
            get() = this !is Intensity && this != Warning && this != Error
    }

    fun play(context: Context, feedback: Feedback) {
        val app = context.applicationContext
        if (!isEnabled(app)) return
        if (!feedback.accompaniesSound) return fire(app, feedback)
        // Un instant de més és imperceptible, i deixa arribar el so que es demana just després.
        val asked = SystemClock.elapsedRealtime()
        main.postDelayed({ if (!soundVibrates(asked)) fire(app, feedback) }, 16)
    }

    /// Un so amb vibració pròpia la reclama: la genèrica demanada just abans o després no es
    /// dispara. Diu si el so ha de vibrar.
    fun claim(context: Context, sound: String, celebration: Boolean): Boolean {
        val app = context.applicationContext
        if (!isEnabled(app) || (celebration && !celebrationsEnabled(app)) || !has(sound)) return false
        claimedAt = SystemClock.elapsedRealtime()
        return true
    }

    fun has(sound: String): Boolean = ViraHapticPatternData.waves.containsKey(sound)

    /// La vibració enganxada al so `sound`, al mateix instant que comença a sonar.
    fun playPattern(context: Context, sound: String) {
        val wave = ViraHapticPatternData.waves[sound] ?: return
        val vibrator = vibrator(context) ?: return
        if (vibrator.hasAmplitudeControl()) {
            vibrate(vibrator, VibrationEffect.createWaveform(wave.timings, wave.amplitudes, -1))
        } else {
            // Sense amplitud, un cop per tram que sona: la melodia es nota igual.
            vibrate(vibrator, VibrationEffect.createWaveform(onOff(wave), -1))
        }
    }

    /// La fita: una remuntada de 500 ms que acompanya la xifra que compta, i un cop al final.
    /// `startDelayMs` és el que espera cada pantalla abans de comptar (420 la de fita, 340 la de
    /// focus). Sense celebracions, només el cop de `Saved`.
    fun celebrate(context: Context, startDelayMs: Long = 420) {
        val app = context.applicationContext
        if (!isEnabled(app)) return
        if (!celebrationsEnabled(app)) return fire(app, Feedback.Saved)
        val asked = SystemClock.elapsedRealtime()
        main.postDelayed({
            // El so de la fita ja porta la seva remuntada.
            if (soundVibrates(asked)) return@postDelayed
            val vibrator = vibrator(app) ?: return@postDelayed
            if (!vibrator.hasAmplitudeControl()) return@postDelayed fire(app, Feedback.Saved)
            val (timings, amplitudes) = rollWaveform()
            vibrate(vibrator, VibrationEffect.createWaveform(timings, amplitudes, -1))
        }, startDelayMs)
    }

    /// Deu graons de 50 ms que pugen de 0,3 a 1 amb un buit entre cada un, i l'èxit al final.
    fun rollWaveform(): Pair<LongArray, IntArray> {
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        repeat(ROLL_STEPS) { step ->
            val level = 0.3 + 0.7 * step / (ROLL_STEPS - 1)
            timings += 12L; amplitudes += (level * 255).roundToInt()
            timings += 38L; amplitudes += 0
        }
        timings += 30L; amplitudes += 255
        return timings.toLongArray() to amplitudes.toIntArray()
    }

    // Interruptors

    /// Mateixa clau que l'iPhone (`ViraPreferences.hapticsEnabled`); neix encès.
    const val HAPTICS_ENABLED = "settings.hapticsEnabled"
    const val CELEBRATIONS_ENABLED = "settings.celebrationsEnabled"

    private fun isEnabled(context: Context): Boolean =
        !ViraSoundEngine.isUnderTest && ViraPreferences.open(context).getBoolean(HAPTICS_ENABLED, true)

    private fun celebrationsEnabled(context: Context): Boolean =
        ViraPreferences.open(context).getBoolean(CELEBRATIONS_ENABLED, true)

    // So

    private var claimedAt: Long? = null
    /// Prou per cobrir la vibració i el so demanats en ordre invers dins del mateix gest.
    private const val CLAIM_WINDOW_MS = 150L

    private fun soundVibrates(asked: Long): Boolean = claimedAt?.let { it >= asked - CLAIM_WINDOW_MS } == true

    // Motor

    private const val ROLL_STEPS = 10
    private val main = Handler(Looper.getMainLooper())

    private fun fire(context: Context, feedback: Feedback) {
        val vibrator = vibrator(context) ?: return
        val amplitude = vibrator.hasAmplitudeControl()
        fun impact(level: Double, millis: Long = 14) {
            val effect = if (amplitude) {
                VibrationEffect.createOneShot(millis, (level * 255).roundToInt().coerceIn(1, 255))
            } else {
                VibrationEffect.createPredefined(if (level < 0.5) VibrationEffect.EFFECT_TICK else VibrationEffect.EFFECT_CLICK)
            }
            vibrate(vibrator, effect)
        }
        when (feedback) {
            Feedback.Tap -> impact(0.6)
            Feedback.Selection -> vibrate(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            is Feedback.Intensity -> impact(0.3 + 0.15 * (feedback.level.coerceIn(1, 5) - 1))
            Feedback.Done -> impact(1.0)
            Feedback.Saved -> vibrate(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            Feedback.Reward -> if (amplitude) {
                // «Ka-ching»: un cop sec i un de suau just darrere.
                vibrate(vibrator, VibrationEffect.createWaveform(longArrayOf(0, 16, 54, 14), intArrayOf(0, 204, 0, 128), -1))
            } else {
                vibrate(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            }
            Feedback.CoinBurst -> impact(0.8, 18)
            is Feedback.CoinLanding -> impact(0.35 + 0.1 * feedback.index.coerceIn(0, 5), 10)
            Feedback.Warning -> vibrate(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            Feedback.Error -> vibrate(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }

    /// `createWaveform(timings)` alterna apagat i encès, començant per apagat.
    fun onOff(wave: ViraHapticWave): LongArray {
        val out = mutableListOf<Long>()
        var on = false
        var span = 0L
        wave.timings.indices.forEach { index ->
            val isOn = wave.amplitudes[index] > 0
            if (isOn != on) { out += span; span = 0; on = isOn }
            span += wave.timings[index]
        }
        out += span
        return out.toLongArray()
    }

    private var cached: Vibrator? = null

    private fun vibrator(context: Context): Vibrator? = cached ?: runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }?.also { cached = it }

    /// Sense el permís de vibrar, o amb el sistema que no ho deixa, l'app segueix igual.
    private fun vibrate(vibrator: Vibrator, effect: VibrationEffect) {
        runCatching { vibrator.vibrate(effect) }
    }
}
