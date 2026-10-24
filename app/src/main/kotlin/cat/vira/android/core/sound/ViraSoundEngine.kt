package cat.vira.android.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Calendar
import kotlin.math.pow

/// El motor que fa sonar `ViraSound` (`ViraSoundEngine` de l'iPhone).
///
/// L'iPhone fa servir un `AVAudioEngine` per tres coses; aquí les dona un `SoundPool`:
/// - **Sons que se solapen.** Sis veus: marcar tres tasques seguides fa sonar tres notes que es
///   trepitgen, sense que la segona talli la primera.
/// - **Mai dues vegades igual.** La velocitat de reproducció desafina ±8 cèntims.
/// - **Mode nit.** De 22 a 7 h tot surt més fluix (0,4). L'iPhone també hi treu els aguts amb un
///   filtre; `SoundPool` no en té, i el volum sol ja fa el gruix de la feina.
///
/// Sona com un so de l'app (`USAGE_ASSISTANCE_SONIFICATION`): el mode silenci del mòbil el talla i
/// la música de l'usuari no s'atura mai. Si un so no carrega, es perd sense dir res: cap so de
/// Vira porta informació que no hi sigui també a la pantalla.
object ViraSoundEngine {

    private const val VOICES = 6
    private const val TAG = "ViraSound"

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<String, Int>()
    private val ready = mutableSetOf<Int>()
    /// Els sons demanats abans d'acabar de carregar: sonen en ser a punt, no es perden.
    private val pending = mutableMapOf<Int, () -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    /// A les proves (Robolectric) i a les captures (layoutlib) no hi ha àudio, i res del que es prova
    /// depèn que el so surti.
    val isUnderTest: Boolean = Build.FINGERPRINT == "robolectric" || Build.DEVICE == "layoutlib" ||
        Build.FINGERPRINT.isNullOrEmpty()

    /// De 22 a 7 h. Funció pura perquè es pugui provar sense rellotge.
    fun isNight(hour: Int): Boolean = hour >= 22 || hour < 7

    /// La velocitat que desafina `cents` cèntims.
    fun rate(cents: Double): Float = 2.0.pow(cents / 1200).toFloat()

    /// Toca `name` (el nom de l'iPhone, `sound-desat`) després de `delayMs`. `onStart` corre just
    /// quan comença a sonar: és on s'engega la seva vibració, perquè vagin juntes.
    fun play(context: Context, name: String, cents: Double = 0.0, delayMs: Long = 0, onStart: () -> Unit = {}) {
        if (isUnderTest) return
        val app = context.applicationContext
        val start = {
            val id = load(app, name)
            if (id != null) {
                val go = {
                    val volume = if (isNight(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) 0.4f else 1f
                    pool?.play(id, volume, volume, 1, 0, rate(cents))
                    onStart()
                }
                if (id in ready) go() else pending[id] = go
            }
        }
        // Un so que falla es perd sense dir res, mai no tanca l'app.
        val safe = { runCatching { start() }.onFailure { Log.w(TAG, "So fallit $name", it) }; Unit }
        if (delayMs > 0) main.postDelayed(safe, delayMs) else safe()
    }

    /// Carrega els fitxers abans que calguin: el primer toc d'una sessió no pot esperar el disc.
    fun preload(context: Context, names: List<String>) {
        if (isUnderTest) return
        val app = context.applicationContext
        runCatching { names.forEach { load(app, it) } }
    }

    private fun load(context: Context, name: String): Int? {
        ids[name]?.let { return it }
        val resource = resourceId(context, name)
        if (resource == 0) {
            Log.w(TAG, "Falta el so $name")
            return null
        }
        return pool(context).load(context, resource, 1).also { ids[name] = it }
    }

    /// `sound-desat` → `R.raw.sound_desat`, com fa `export-assets.py` (`res/raw/keep.xml` els guarda).
    private fun resourceId(context: Context, name: String): Int =
        context.resources.getIdentifier(name.replace('-', '_'), "raw", context.packageName)

    private fun pool(context: Context): SoundPool = pool ?: SoundPool.Builder()
        .setMaxStreams(VOICES)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .also { created ->
            created.setOnLoadCompleteListener { _, id, status ->
                if (status != 0) return@setOnLoadCompleteListener
                ready += id
                pending.remove(id)?.invoke()
            }
            pool = created
        }
}
