package cat.vira.android.core.notifications

import android.content.SharedPreferences
import java.time.Instant
import java.time.ZoneId

/// Hores de descans per al tram de 12 a 15 anys (`ViraRestHours` de l'iPhone, Arnau, 2026-10-23).
///
/// Per a qui va triar `12-15` a l'entrada, **cap avís que no hagi demanat a partir de les 21:30**:
/// ni el de Viri ni l'emocional. Els que ha programat ell (tasques, hàbits, medicació) surten a
/// l'hora que va triar. Automàtic i sense cap fila a Ajustos.
object ViraRestHours {
    /// Minuts des de les 0:00 a partir dels quals el tram jove ja no rep res que no hagi demanat.
    const val START = 21 * 60 + 30

    /// La mateixa clau i el mateix valor que l'entrada (`EntryPreferenceKeys.AGE_RANGE`).
    private const val AGE_RANGE = "settings.ageRange"
    private const val YOUNG = "12-15"

    fun isYoung(p: SharedPreferences) = p.getString(AGE_RANGE, null) == YOUNG

    /// Si un avís que ningú no ha demanat pot sortir a `fire`.
    fun allows(fire: Instant, young: Boolean, zone: ZoneId): Boolean {
        if (!young) return true
        val clock = fire.atZone(zone)
        return clock.hour * 60 + clock.minute < START
    }
}
