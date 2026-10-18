package cat.vira.android.features.entry

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R

/// Per on vol començar qui entra. **Només** tria la primera acció: no s'envia enlloc, no se
/// sincronitza i no amaga res de l'app.
enum class EntryStartIntent(val raw: String) {
    ORGANIZE("organize"), EMOTIONS("emotions"), FOCUS("focus");

    @get:StringRes
    val title: Int
        get() = when (this) {
            ORGANIZE -> R.string.onboarding_start_organize
            EMOTIONS -> R.string.onboarding_start_emotions
            FOCUS -> R.string.onboarding_start_focus
        }

    @get:StringRes
    val detail: Int
        get() = when (this) {
            ORGANIZE -> R.string.onboarding_start_organize_detail
            EMOTIONS -> R.string.onboarding_start_emotions_detail
            FOCUS -> R.string.onboarding_start_focus_detail
        }

    @get:DrawableRes
    val pictogram: Int
        get() = when (this) {
            ORGANIZE -> R.drawable.picto_cal_task
            EMOTIONS -> R.drawable.viri_emotion_content
            FOCUS -> R.drawable.viri_focused
        }

    /// La frase de Viri a la primera acció.
    @get:StringRes
    val viriLine: Int
        get() = when (this) {
            ORGANIZE -> R.string.onboarding_viri_task
            EMOTIONS -> R.string.onboarding_viri_emotion
            FOCUS -> R.string.onboarding_viri_focus
        }

    /// El títol de la primera acció.
    @get:StringRes
    val actionTitle: Int
        get() = when (this) {
            ORGANIZE -> R.string.onboarding_task_title
            EMOTIONS -> R.string.onboarding_emotion_title
            FOCUS -> R.string.onboarding_focus_title
        }

    companion object {
        /// El que val quan no s'ha triat res: "Saltar" a "Per on comencem?" vol dir organitzar-se.
        val fallback = ORGANIZE

        fun from(stored: String): EntryStartIntent = entries.firstOrNull { it.raw == stored } ?: fallback
    }
}

/// Rang d'edat orientatiu (`ViraAgeRange` de l'iPhone). El `raw` és el que es desa.
///
/// El primer tram **no es toca**: `EARLY` és el que dispara el permís del tutor
/// (`EntryConsentWriter.plan`), perquè la franja 12–15 trepitja el límit legal dels 14 anys
/// (`PRIVACY_AND_SAFETY.md` §4.5b). Els trams es toquen: no hi ha cap edat sense casella.
enum class ViraAgeRange(val raw: String) {
    EARLY("12-15"), MID("16-20"), LATE("21+");

    @get:StringRes
    val title: Int
        get() = when (this) {
            EARLY -> R.string.onboarding_age_range_early
            MID -> R.string.onboarding_age_range_mid
            LATE -> R.string.onboarding_age_range_late
        }

    companion object {
        fun from(raw: String): ViraAgeRange? = entries.firstOrNull { it.raw == raw }
    }
}
