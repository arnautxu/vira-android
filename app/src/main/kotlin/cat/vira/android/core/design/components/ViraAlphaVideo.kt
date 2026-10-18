package cat.vira.android.core.design.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.os.PowerManager
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.resources.ViraVideo

/// Quin fotograma es veu quiet quan el vídeo no s'ha de reproduir (`ViraAlphaVideo.Poster`).
sealed interface ViraVideoPoster {
    /// El primer fotograma del bucle si n'hi ha, i si no l'últim del clip: d'un bucle interessa
    /// l'estat assentat (la flama ja encesa); d'un clip d'un sol cop, el final, on es quedaria igual.
    data object Natural : ViraVideoPoster

    data class Second(val seconds: Double) : ViraVideoPoster
}

/// Vídeo curt de Vira, amb transparència quan el clip en porta (`ViraAlphaVideo`).
///
/// - `loopStart`: segon al qual torna en acabar (la flama: l'encesa un cop, el parpelleig en
///   bucle). Amb `null` es reprodueix un sol cop i es queda a l'últim fotograma.
/// - `fill`: `false` hi cap sencer (una peça, un heroi); `true` omple i en retalla el que sobra
///   (un vídeo de fons).
/// - `isMuted`: silenciat **per defecte**. Amb `false` no demana el focus d'àudio, de manera que
///   la música de l'usuari no s'atura.
///
/// Es queda quiet al pòster, sense que qui el crida ho hagi de recordar, en tres casos, com a
/// l'iPhone:
/// - **Reduir moviment** (a Android, «Treu les animacions»): mai es mou.
/// - **Estalvi de bateria**, però **només els bucles**: les celebracions d'un sol cop són el
///   moment i no el decorat (Arnau, 2026-08-13).
/// - **App en segon pla**: només pausa, sense rebobinar, perquè tornar a l'app no reiniciï una
///   celebració ja vista.
///
/// Cada vídeo és un reproductor amb el seu descodificador: **cap vista que es repeteixi en una
/// llista o una graella el pot fer servir**. Per a les cel·les, il·lustració fixa.
///
/// Sempre amagat a TalkBack: és decoratiu. A les captures (i a la vista prèvia) no es dibuixa.
@OptIn(UnstableApi::class)
@Composable
fun ViraAlphaVideo(
    video: ViraVideo,
    modifier: Modifier = Modifier,
    loopStart: Double? = null,
    poster: ViraVideoPoster = ViraVideoPoster.Natural,
    fill: Boolean = false,
    isMuted: Boolean = true,
) {
    if (LocalInspectionMode.current) {
        Box(modifier)
        return
    }
    // Un altre clip o una altra forma de bucle és un reproductor i una superfície nous.
    key(video, loopStart, fill, isMuted) {
        AlphaVideo(video, modifier, loopStart, poster, fill, isMuted)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AlphaVideo(
    video: ViraVideo,
    modifier: Modifier,
    loopStart: Double?,
    poster: ViraVideoPoster,
    fill: Boolean,
    isMuted: Boolean,
) {
    val context = LocalContext.current
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isActive by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var powerSave by remember { mutableStateOf(context.isPowerSaving()) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(video.resource)))
            // Un bucle sencer el fa el reproductor sense cap tall; un bucle amb encesa torna a
            // `loopStart` en acabar (a sota).
            repeatMode = if (loopStart == 0.0) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (isMuted) 0f else 1f
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ false,
            )
            prepare()
        }
    }

    val suppressed = reduceMotion || (loopStart != null && powerSave)
    val state by rememberUpdatedState(Triple(suppressed, isActive, poster))
    val refresh: () -> Unit = remember(player) {
        {
            val (isSuppressed, active, still) = state
            when {
                isSuppressed -> {
                    player.pause()
                    player.seekTo(posterMillis(video, loopStart, still))
                }
                active -> player.play()
                else -> player.pause()
            }
        }
    }
    SideEffect { refresh() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && loopStart != null && loopStart > 0.0) {
                    // Torna a la part cíclica, no al principi: l'encesa és un esdeveniment. I no
                    // `play()` directe: si mentrestant s'ha encès l'estalvi o l'app ha marxat, no
                    // ha de tornar a arrencar.
                    player.seekTo((loopStart * 1000).toLong())
                    refresh()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isActive = true
                Lifecycle.Event.ON_STOP -> isActive = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // L'estalvi de bateria es pot encendre amb l'app oberta: el bucle s'atura sense esperar res més.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                powerSave = context.isPowerSaving()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextureView(viewContext).apply {
                // Sense això la vista es compon sobre negre i la transparència no serveix de res.
                isOpaque = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                val renderer = ViraVideoRenderer(
                    alpha = video.alpha,
                    contentAspect = video.width.toFloat() / video.height,
                    fill = fill,
                ) { surface -> player.setVideoSurface(surface) }
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) =
                        renderer.attach(surface, width, height)

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
                        renderer.resize(width, height)

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        player.setVideoSurface(null)
                        renderer.detach()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
                tag = renderer
            }
        },
        onRelease = { view -> (view.tag as? ViraVideoRenderer)?.release() },
    )
}

/// Pòster en mil·lisegons. La durada surt del catàleg (`frameCount / fps`) i no del reproductor,
/// que no la sap fins que el clip és a punt: l'últim fotograma és un fotograma abans del final.
private fun posterMillis(video: ViraVideo, loopStart: Double?, poster: ViraVideoPoster): Long {
    val seconds = when (poster) {
        is ViraVideoPoster.Second -> poster.seconds
        ViraVideoPoster.Natural -> loopStart ?: ((video.frameCount - 1) / video.framesPerSecond)
    }
    return (seconds * 1000).toLong().coerceAtLeast(0)
}

private fun Context.isPowerSaving(): Boolean =
    getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
