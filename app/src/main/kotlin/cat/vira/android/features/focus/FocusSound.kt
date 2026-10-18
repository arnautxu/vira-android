package cat.vira.android.features.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences

/// Els sons de la sessió (`ViraSound.Cue` de l'iPhone: `focusStart`, `focusPause`, `focusResume`,
/// `focusEnd`, `breakEnd`, `saved`).
///
/// Segueix el mateix interruptor que l'iPhone (`settings.soundMode`: tots, només celebracions o
/// cap) i la mateixa sessió d'àudio: no atura la música de l'usuari.
object FocusSound {

    enum class Cue(@RawRes val resource: Int, val isCelebration: Boolean) {
        START(R.raw.sound_focus_comenca, false),
        PAUSE(R.raw.sound_focus_pausa, false),
        RESUME(R.raw.sound_focus_represa, false),
        END(R.raw.sound_focus_final, true),
        BREAK_END(R.raw.sound_descans_final, true),
        SAVED(R.raw.sound_desat, false),
    }

    private const val SOUND_MODE = "settings.soundMode"

    private var pool: SoundPool? = null
    private val loaded = mutableMapOf<Cue, Int>()
    private val ready = mutableSetOf<Int>()
    private var pendingPlay: Int? = null

    fun play(context: Context, cue: Cue) {
        val mode = ViraPreferences.open(context).getString(SOUND_MODE, "all")
        val allowed = when (mode) {
            "off" -> false
            "celebrations" -> cue.isCelebration
            else -> true
        }
        if (!allowed) return
        val sounds = pool(context)
        val id = loaded.getOrPut(cue) { sounds.load(context.applicationContext, cue.resource, 1) }
        if (id in ready) sounds.play(id, 1f, 1f, 1, 0, 1f) else pendingPlay = id
    }

    private fun pool(context: Context): SoundPool = pool ?: SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .also { created ->
            // El primer cop es llegeix del disc: sona en acabar de carregar, no es perd.
            created.setOnLoadCompleteListener { sounds, id, status ->
                if (status != 0) return@setOnLoadCompleteListener
                ready += id
                if (pendingPlay == id) {
                    pendingPlay = null
                    sounds.play(id, 1f, 1f, 1, 0, 1f)
                }
            }
            pool = created
        }
}
