package cat.vira.android.app

import cat.vira.android.BuildConfig

/// Funcions construïdes que encara no poden formar part d'una build pública, calcat de
/// `ViraReleaseGate` de l'iPhone: les de proves (debug) les tenen, les de Google Play (release) no.
///
/// No és una preferència ni un interruptor remot: cada una espera una decisió de producte o una
/// revisió legal (D-29). L'escut d'apps no hi és perquè a Android no es fa.
object ViraReleaseGate {
    val friends: Boolean = BuildConfig.DEBUG
    val cards: Boolean = BuildConfig.DEBUG
    val parentPanel: Boolean = BuildConfig.DEBUG
}
