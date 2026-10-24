package cat.vira.android.core.sound

import android.content.Context
import android.os.SystemClock
import cat.vira.android.core.data.ViraPreferences
import kotlin.random.Random

/// El vocabulari sonor de Vira (`ViraSound` de l'iPhone, proposta 2 de l'Arnau, 2026-10-18).
///
/// Cada moment té un so curt, i aquest fitxer és l'únic lloc que els fa sonar, com `ViraHaptics`
/// ho és de la vibració. Qui els toca és `ViraSoundEngine`.
///
/// ## Les regles que no es toquen
/// - **Tot en Do pentatònic.** Dos sons alhora sempre encaixen.
/// - **Sona com un so de l'app, no com música**: el volum de notificacions del mòbil el talla i la
///   música de l'usuari no s'atura mai (`ViraSoundEngine`).
/// - **Sona el que és teu.** Navegar i triar només vibren. **Medicació, Ajuda ara i errors no
///   tenen cap so**, i totes les emocions sonen igual (`EMOTION_SAVED`).
/// - **So i vibració van junts.** Si un so té la seva ona (`ViraHapticPatternData`), la vibració és
///   aquella i no la genèrica (`ViraHaptics.claim`).
/// - **Ajustos → Sons**: `settings.soundMode` (tots · cap; «només celebracions» es llegeix encara).
/// - **La skin posada no canvia res aquí** fins a Google Play: els paquets de la skin no viatgen a
///   l'APK, i l'iPhone sense paquet sona igual, amb els sons de sempre.
object ViraSound {

    /// Què pot sonar. `settings.soundMode`, la mateixa clau i valors que l'iPhone.
    enum class Mode(val raw: String) {
        ALL("all"), CELEBRATIONS("celebrations"), OFF("off");

        fun allows(cue: Cue): Boolean = when (this) {
            ALL -> true
            CELEBRATIONS -> cue.isCelebration
            OFF -> false
        }

        companion object {
            fun from(raw: String?): Mode = entries.firstOrNull { it.raw == raw } ?: ALL
        }
    }

    /// Un moment, no un fitxer. `file` és el nom del so a l'iPhone (i de la seva ona).
    enum class Cue(val file: String, val isCelebration: Boolean) {
        UNDO("sound-desfer", false),
        SAVED("sound-desat", false),
        EMOTION_SAVED("sound-emocio-desada", false),
        DAY_COMPLETE("sound-dia-complet", true),
        /// La moneda gran que surt girant quan guanyes ViriCoins.
        COINS("sound-moneda-esclat", false),
        /// L'última de les sis monedes ja és a la capçalera.
        COINS_LANDED("sound-moneda-final", false),
        COUNT_UP("sound-comptador", true),
        LEVEL_UP("sound-nivell", true),
        CHALLENGE("sound-repte", true),
        STREAK_TODAY("sound-ratxa-avui", true),
        SAVED_DAY("sound-dia-salvat", true),
        FOCUS_START("sound-focus-comenca", false),
        FOCUS_PAUSE("sound-focus-pausa", false),
        FOCUS_RESUME("sound-focus-represa", false),
        FOCUS_END("sound-focus-final", true),
        BREAK_END("sound-descans-final", true),
        SHOP_COMMON("sound-botiga-comu", true),
        SHOP_SPECIAL("sound-botiga-rara", true),
        SHOP_EPIC("sound-botiga-epica", true),
        SHOP_LEGENDARY("sound-botiga-llegendaria", true),
        EQUIP("sound-equipar", false),
        /// Triar una opció al flux d'entrada: quatre notes, cada tria toca la següent.
        ENTRY_CHOICE("sound-entrada-1", false),
    }

    /// L'escala del dia: cada cosa feta sona una nota més amunt, i torna a començar a l'onzena.
    val scale: List<String> = (1..10).map { "sound-tasca-$it" }
    /// La mateixa nota amb una espurna: si l'anterior s'ha marcat fa menys de `COMBO_WINDOW_MS`.
    val scaleCombo: List<String> = (1..10).map { "sound-tasca-$it-combo" }
    const val COMBO_WINDOW_MS = 2_500L
    /// Les sis monedes que aterren a la capçalera, una nota més amunt cada una.
    val coinLandings: List<String> = (1..6).map { "sound-moneda-$it" }
    /// La veu de Viri: dues xerrameques i un riure, que roten.
    val viriVoices: List<String> = (1..3).map { "sound-viri-$it" }
    val entryNotes: List<String> = (1..4).map { "sound-entrada-$it" }

    /// La nota que toca a la `doneToday`-ena cosa feta avui (comptant-la, començant per 1).
    fun scaleIndex(doneToday: Int): Int = maxOf(doneToday - 1, 0) % scale.size

    /// Amb espurna si s'ha marcat seguit de l'anterior. La primera del dia mai en porta.
    fun scaleFile(doneToday: Int, sinceLastMs: Long?): String {
        val index = scaleIndex(doneToday)
        if (doneToday <= 1 || sinceLastMs == null || sinceLastMs >= COMBO_WINDOW_MS) return scale[index]
        return scaleCombo[index]
    }

    /// La mostra curta del provador de la Botiga.
    fun sampleName(skin: String): String = "sound-mostra-$skin"

    // Reproducció

    fun play(context: Context, cue: Cue, delayMs: Long = 0) {
        if (!mode(context).allows(cue)) return
        if (cue == Cue.ENTRY_CHOICE) {
            entryTurn = (entryTurn + 1) % entryNotes.size
            play(context, entryNotes[entryTurn], delayMs, variation())
        } else {
            play(context, cue.file, delayMs, celebration = cue.isCelebration)
        }
    }

    /// Desar el que has escrit: la vibració de desar i el seu so, junts (`ViraHaptics.play(.saved)`
    /// i `ViraSound.play(.saved)` de l'iPhone).
    fun saved(context: Context) {
        ViraHaptics.play(context, ViraHaptics.Feedback.Saved)
        play(context, Cue.SAVED)
    }

    /// Una cosa marcada com a feta: la nota de l'escala o, si tanca el dia, el dia sencer.
    fun playDone(context: Context, doneToday: Int, closesDay: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val since = lastDone?.let { now - it }
        lastDone = now
        if (closesDay) {
            play(context, Cue.DAY_COMPLETE)
        } else if (mode(context) == Mode.ALL) {
            play(context, scaleFile(doneToday, since), cents = variation())
        }
    }

    /// La `index`-ena moneda (0…5) acaba d'aterrar a la capçalera.
    fun playCoinLanding(context: Context, index: Int) {
        if (!mode(context).allows(Cue.COINS)) return
        play(context, coinLandings[index.coerceIn(0, coinLandings.lastIndex)])
    }

    /// «Com sona» al provador: l'has demanat tu, sona amb tot menys «Cap».
    fun playSample(context: Context, skin: String) {
        if (mode(context) == Mode.OFF) return
        play(context, sampleName(skin))
    }

    fun playViri(context: Context) {
        if (mode(context) != Mode.ALL) return
        viriTurn = (viriTurn + 1) % viriVoices.size
        play(context, viriVoices[viriTurn], cents = variation())
    }

    fun mode(context: Context): Mode =
        Mode.from(ViraPreferences.open(context).getString(SOUND_MODE, null))

    /// Deixa a punt els sons més freqüents: l'escala sona deu cops al dia i les monedes van
    /// lligades a la imatge; un so que es llegeix del disc en el moment arriba tard.
    fun prepare(context: Context) {
        ViraSoundEngine.preload(context, scale + scaleCombo + coinLandings)
    }

    /// Mateixa clau que l'iPhone (`ViraPreferences.soundMode`); l'interruptor és d'Ajustos.
    const val SOUND_MODE = "settings.soundMode"

    // Motor

    private val lastPlayed = mutableMapOf<String, Long>()
    private var lastDone: Long? = null
    private var viriTurn = 0
    private var entryTurn = -1

    /// ±8 cèntims: el mateix so mai surt dues vegades idèntic, i segueix dins de l'escala.
    private fun variation(): Double = Random.nextDouble(-8.0, 8.0)

    private fun play(context: Context, name: String, delayMs: Long = 0, cents: Double = 0.0, celebration: Boolean = false) {
        // Desmarcar dins de la finestra de «Desfer» pot passar per dos camins alhora; sense això,
        // el mateix so sortia dues vegades encavalcat.
        val now = SystemClock.elapsedRealtime()
        lastPlayed[name]?.let { if (now - it < 120) return }
        lastPlayed[name] = now

        val vibrates = ViraHaptics.claim(context, name, celebration)
        ViraSoundEngine.play(context, name, cents, delayMs) {
            if (vibrates) ViraHaptics.playPattern(context, name)
        }
    }
}
