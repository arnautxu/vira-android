package cat.vira.android.app

import cat.vira.android.BuildConfig

/// Funcions construïdes que encara no poden formar part d'una build pública, calcat de
/// `ViraReleaseGate` de l'iPhone: les de proves (debug) les tenen, les de Google Play (release) no.
///
/// No és una preferència ni un interruptor remot: cada una espera una decisió de producte o una
/// revisió legal (D-29). L'escut d'apps no hi és perquè a Android no es fa.
object ViraReleaseGate {
    val friends: Boolean = BuildConfig.DEBUG
    /// Obertes a totes les builds, com a l'iPhone (Arnau, 2026-10-18: «vull que surti per poder-lo
    /// ensenyar, i les cartes també»). El panell de pares ja no passa per aquí: no té porta.
    val cards: Boolean = true
}
