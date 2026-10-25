package cat.vira.android.features.add

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import cat.vira.android.core.resources.ViraLanguage

/// Dir una tasca en lloc d'escriure-la: el micròfon del camp de la cara Tasca
/// (`QuickCaptureDictation`, Arnau, 2026-10-23: «el E m'agrada com a botó»).
///
/// El text entra al camp a mesura que es parla, i la frase es llegeix com si s'hagués escrit:
/// «demà a les 5» marca el dia i l'hora (`CapturePhrase`). No desa res sol.
///
/// **Res surt del telèfon.** Només el reconeixedor del dispositiu
/// (`SpeechRecognizer.createOnDeviceSpeechRecognizer`, Android 12+), i si el telèfon no el té per a
/// l'idioma de l'app el micròfon no surt: és millor no tenir-lo que enviar la veu a un servidor. Cap
/// àudio es desa. El permís del micròfon es demana el primer cop que es toca, mai abans.
@Stable
class TaskDictation(private val context: Context) {
    enum class State { Idle, Listening, Denied }

    var state by mutableStateOf(State.Idle)
        private set
    /// Si el micròfon surt. Es posa al dia en mirar què sap fer el telèfon (`checkAvailability`).
    var isAvailable by mutableStateOf(false)
        private set
    /// El que s'ha sentit fins ara, sencer a cada canvi.
    var onText: (String) -> Unit = {}

    private var recognizer: SpeechRecognizer? = null
    private val language: String = when (ViraLanguage.current(context)) {
        "es" -> "es-ES"
        "en" -> "en-US"
        else -> "ca-ES"
    }

    /// Android 13+ diu quins idiomes té instal·lats al telèfon; el 12 només si n'hi ha cap, i si
    /// l'idioma no hi és, el primer error el treu (`onError`).
    fun checkAvailability() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            isAvailable = true
            return
        }
        val probe = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        probe.checkRecognitionSupport(
            intent(),
            ContextCompat.getMainExecutor(context),
            object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    isAvailable = support.installedOnDeviceLanguages.any { it.equals(language, true) || it.substringBefore('-') == language.substringBefore('-') }
                    probe.destroy()
                }

                override fun onError(error: Int) {
                    probe.destroy()
                }
            },
        )
    }

    /// El permís, denegat: una línia diu que es pot escriure.
    fun denied() {
        state = State.Denied
    }

    /// Engega amb el permís ja donat. Un segon toc mentre escolta l'atura.
    fun start() {
        if (state == State.Listening || !isAvailable || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val listener = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = listener
        listener.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onPartialResults(partialResults: Bundle?) = receive(partialResults)
            override fun onResults(results: Bundle?) {
                receive(results)
                stop()
            }

            override fun onError(error: Int) {
                // Sense l'idioma al telèfon, el micròfon ja no surt: res no s'envia fora.
                if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                    isAvailable = false
                }
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) state = State.Denied
                stop()
            }
        })
        state = State.Listening
        runCatching { listener.startListening(intent()) }.onFailure { stop() }
    }

    fun stop() {
        recognizer?.let {
            runCatching { it.cancel() }
            it.destroy()
        }
        recognizer = null
        if (state == State.Listening) state = State.Idle
    }

    private fun receive(bundle: Bundle?) {
        if (state != State.Listening) return
        val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!text.isNullOrBlank()) onText(text)
    }

    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        // Quan la persona calla 2 s, s'atura sol.
        .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
        .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
}

/// El micròfon i l'aturada del botó (els de Material, que no són al joc d'icones bàsic de l'app).
internal object DictationIcons {
    val mic: androidx.compose.ui.graphics.vector.ImageVector by lazy {
        icon("Mic", "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z")
    }
    val stop: androidx.compose.ui.graphics.vector.ImageVector by lazy { icon("Stop", "M6,6h12v12H6z") }

    private fun icon(name: String, path: String) =
        androidx.compose.ui.graphics.vector.ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(androidx.compose.ui.graphics.vector.addPathNodes(path), fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black))
            .build()
}
