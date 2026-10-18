package cat.vira.android.features.emotions

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/// El reproductor d'una sessió de Mindfulness: un sol fitxer per exercici (`MindfulnessPlayer` de
/// l'iPhone).
///
/// ## La veu demana l'àudio i el torna
/// La resta de l'app sona barrejada amb la música de l'usuari. Una veu que et guia no pot anar
/// així, i per això aquí es demana el focus d'àudio com a veu (`CONTENT_TYPE_SPEECH`): la música
/// s'atura mentre parla i torna en acabar. Una trucada atura la veu i, quan acaba, continua; uns
/// auriculars que es desconnecten la deixen en pausa, perquè no surti de cop per l'altaveu.
///
/// Saltar endavant o enrere desquadraria la respiració: no s'ofereix.
class MindfulnessPlayer(private val context: Context) {
    var currentTime by mutableDoubleStateOf(0.0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isFinished by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    private var player: ExoPlayer? = null

    fun start(@RawRes audio: Int) {
        stop()
        isFinished = false
        failed = false
        if (audio == 0) {
            Log.e(TAG, "Falta l'àudio de l'exercici")
            failed = true
            return
        }
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                currentTime = player.currentPosition / 1000.0
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) finish()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "No s'ha pogut reproduir: ${error.errorCodeName}")
                failed = true
                stop()
            }
        })
        player.setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/$audio"))
        player.prepare()
        player.playWhenReady = true
        this.player = player
    }

    fun play() {
        if (isFinished) return
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun togglePlayback() = if (isPlaying) pause() else play()

    /// La pantalla llegeix `currentTime`; el reproductor no avisa de res mentre sona. La pantalla
    /// la crida deu cops per segon mentre sona.
    fun tick() {
        val player = player ?: return
        currentTime = player.currentPosition / 1000.0
    }

    /// Sortir a mitges o en acabar: el focus d'àudio torna a qui el tenia.
    fun stop() {
        player?.release()
        player = null
        isPlaying = false
        currentTime = 0.0
    }

    private fun finish() {
        currentTime = (player?.duration ?: 0L).coerceAtLeast(0L) / 1000.0
        isPlaying = false
        isFinished = true
        player?.release()
        player = null
    }

    private companion object {
        const val TAG = "mindfulness"
    }
}
