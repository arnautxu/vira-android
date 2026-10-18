package cat.vira.android.features.focus

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.resources.ViraVideo
import java.util.UUID

/// Identitat d'una entrada del carrusel (`FocusModeID`).
///
/// No n'hi ha prou amb `FocusKind`: la fase és un camp desat (`FocusSession.kindRaw`) i un mode creat
/// per l'usuari no pot ser-ne un cas nou. La identitat és un tipus a part i la fase es queda com a
/// atribut: un mode personalitzat sempre és de concentració, i les recompenses i el descans
/// suggerit li apliquen sense tocar res.
sealed interface FocusModeID {
    data class BuiltIn(val kind: FocusKind) : FocusModeID
    data class Profile(val id: UUID) : FocusModeID
}

/// Una entrada del carrusel: un dels tres modes integrats o un perfil de l'usuari (`FocusMode.swift`).
data class FocusMode(
    val id: FocusModeID,
    /// La fase de la sessió: mana sobre recompenses i descans suggerit.
    val kind: FocusKind,
    /// El nom d'un integrat: una clau del catàleg, que es resol a cada dibuix.
    @StringRes val labelRes: Int?,
    /// El nom d'un perfil: text de l'usuari, que no es tradueix ni es posa en majúscules.
    val name: String?,
    /// El nom del pictograma a l'iPhone (`picto-focus-modes`, `viri-focused`…).
    val pictogram: String,
    val isScene: Boolean,
    /// Durades que ofereix el mode, en minuts.
    val durations: List<Int>,
    /// Fins on arriben el − i el + del panell, de 5 en 5. `null` per als perfils: porten una sola
    /// durada, i es canvia a l'editor.
    val minuteRange: IntRange?,
    val defaultMinutes: Int,
    /// Un perfil té **un sol descans**, i hi posa el mateix valor als dos.
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val mutesNotifications: Boolean,
    val showsLinkedTask: Boolean,
) {
    val defaultSeconds: Int get() = defaultMinutes * 60

    val profileID: UUID? get() = (id as? FocusModeID.Profile)?.id

    fun breakMinutes(kind: FocusKind): Int = if (kind == FocusKind.LONG_BREAK) longBreakMinutes else shortBreakMinutes

    /// El nom que es mostra: la clau traduïda dels integrats, el nom escrit per l'usuari als perfils.
    @Composable
    fun label(): String = name ?: stringResource(labelRes ?: R.string.focus_kind_focus)

    fun label(context: Context): String = name ?: context.getString(labelRes ?: R.string.focus_kind_focus)

    // Identitat per `id`, com a l'iPhone: dues fotos del mateix perfil són el mateix mode.
    override fun equals(other: Any?): Boolean = other is FocusMode && other.id == id
    override fun hashCode(): Int = id.hashCode()

    companion object {

        /// Els tres modes de `USER_FLOWS.md` F16, en el seu ordre. Es conserven sempre.
        fun builtIns(): List<FocusMode> = listOf(
            builtIn(FocusKind.FOCUS, R.string.focus_pomodoro_title, FocusPictogram.VIRI_FOCUSED,
                listOf(15, 25, 45), 5..60, 25),
            builtIn(FocusKind.SHORT_BREAK, R.string.focus_kind_short_break, FocusPictogram.VIRI_CHILL,
                listOf(5, 10), 5..30, 5),
            builtIn(FocusKind.LONG_BREAK, R.string.focus_kind_long_break, FocusPictogram.VIRI_MEDITATING,
                listOf(15, 20, 30), 5..30, 15),
        )

        private fun builtIn(
            kind: FocusKind, @StringRes label: Int, pictogram: String,
            durations: List<Int>, range: IntRange, default: Int,
        ) = FocusMode(
            id = FocusModeID.BuiltIn(kind), kind = kind, labelRes = label, name = null,
            pictogram = pictogram, isScene = true, durations = durations, minuteRange = range,
            defaultMinutes = default, shortBreakMinutes = 5, longBreakMinutes = 15,
            mutesNotifications = false, showsLinkedTask = true,
        )

        /// L'entrada d'un perfil. Fase de concentració sempre.
        fun of(profile: FocusProfile) = FocusMode(
            id = FocusModeID.Profile(profile.id),
            kind = FocusKind.FOCUS,
            labelRes = null,
            name = profile.name,
            pictogram = profile.iconName,
            isScene = profile.iconName.startsWith("viri-"),
            durations = listOf(profile.focusMinutes),
            minuteRange = null,
            defaultMinutes = profile.focusMinutes,
            shortBreakMinutes = profile.breakMinutes,
            longBreakMinutes = profile.breakMinutes,
            mutesNotifications = profile.muteViraNotifications,
            showsLinkedTask = profile.showsLinkedTask,
        )

        /// La llista sencera del carrusel: integrats primer, perfils després, en l'ordre rebut.
        /// Els arxivats no hi arriben: el filtre és de qui consulta.
        fun all(profiles: List<FocusProfile>): List<FocusMode> = builtIns() + profiles.map(::of)
    }
}

/// Els pictogrames que fa servir Concentra't, amb el nom de l'iPhone (`ViraPictogram.Name`).
object FocusPictogram {
    const val FOCUS = "picto-focus"
    const val FOCUS_MODES = "picto-focus-modes"
    const val POMODORO = "picto-pomodoro"
    const val TIMER = "picto-timer"
    const val ORGANIZATION = "picto-organization"
    const val JOURNAL = "picto-journal"
    const val VIRI_FOCUSED = "viri-focused"
    const val VIRI_STUDYING = "viri-studying"
    const val VIRI_CHILL = "viri-chill"
    const val VIRI_MEDITATING = "viri-meditating"

    /// Les icones que es poden triar per a un mode (`ViraPictogram.Name.focusModeIcons`).
    val modeIcons = listOf(FOCUS_MODES, POMODORO, TIMER, FOCUS, ORGANIZATION, JOURNAL, VIRI_FOCUSED, VIRI_STUDYING)

    @DrawableRes
    fun drawable(name: String): Int = when (name) {
        FOCUS -> R.drawable.picto_focus
        POMODORO -> R.drawable.picto_pomodoro
        TIMER -> R.drawable.picto_timer
        ORGANIZATION -> R.drawable.picto_organization
        JOURNAL -> R.drawable.picto_journal
        VIRI_FOCUSED -> R.drawable.viri_focused
        VIRI_STUDYING -> R.drawable.viri_studying
        VIRI_CHILL -> R.drawable.viri_chill
        VIRI_MEDITATING -> R.drawable.viri_meditating
        // Un nom desconegut (un perfil fet en una versió amb més icones) cau a la icona de modes.
        else -> R.drawable.picto_focus_modes
    }

    /// El nom per a TalkBack (`pictogram.<nom>` del catàleg).
    @StringRes
    fun label(name: String): Int = when (name) {
        FOCUS -> R.string.pictogram_picto_focus
        POMODORO -> R.string.pictogram_picto_pomodoro
        TIMER -> R.string.pictogram_picto_timer
        ORGANIZATION -> R.string.pictogram_picto_organization
        JOURNAL -> R.string.pictogram_picto_journal
        VIRI_FOCUSED -> R.string.pictogram_viri_focused
        VIRI_STUDYING -> R.string.pictogram_viri_studying
        else -> R.string.pictogram_picto_focus_modes
    }

    /// El bucle animat d'una escena, i el segon on torna (`ViraAlphaVideo.Name.loop(for:)`).
    fun loop(name: String): Pair<ViraVideo, Double>? = when (name) {
        VIRI_FOCUSED -> ViraVideo.ViriFocusedLoop to 10.0
        VIRI_CHILL -> ViraVideo.ViriChillLoop to 10.0
        VIRI_MEDITATING -> ViraVideo.ViriMeditatingLoop to 3.0
        else -> null
    }
}
