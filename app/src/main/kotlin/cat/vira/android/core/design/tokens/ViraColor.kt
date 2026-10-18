package cat.vira.android.core.design.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import cat.vira.android.core.design.LocalViraEnvironment

/// Tokens de color de Vira, calcats de `ViraColor` de l'iPhone. Cada àrea existeix en tres rols
/// (accent, superfície, text sobre superfície), i cada to tria sol la seva aparença clara o fosca.
///
/// Cap pantalla pot fer servir un color literal (`Color(0x…)`, `Color.White`, `Color.Black`):
/// sempre a través d'aquests tokens (`DESIGN_RULES.md` §7). Els valors són a `ViraPalette`.
object ViraColor {

    val brand: Color @Composable @ReadOnlyComposable get() = ViraPalette.brand.resolve()
    val brandAccent: Color @Composable @ReadOnlyComposable get() = ViraPalette.brandAccent.resolve()
    val brandSurface: Color @Composable @ReadOnlyComposable get() = ViraPalette.brandSurface.resolve()
    val brandOnSurface: Color @Composable @ReadOnlyComposable get() = ViraPalette.brandOnSurface.resolve()

    @Composable @ReadOnlyComposable
    fun accent(area: ViraArea): Color = ViraPalette.accent(area).resolve()

    @Composable @ReadOnlyComposable
    fun surface(area: ViraArea): Color = ViraPalette.surface(area).resolve()

    @Composable @ReadOnlyComposable
    fun cardFill(area: ViraArea): Color = ViraPalette.cardFill(area).resolve()

    /// Text sobre una targeta modular. Mai blanc sobre el degradat.
    @Composable @ReadOnlyComposable
    fun onCard(area: ViraArea): Color = ViraPalette.onCard(area).resolve()

    @Composable @ReadOnlyComposable
    fun onSurface(area: ViraArea): Color = ViraPalette.onSurface(area).resolve()

    @Composable @ReadOnlyComposable
    fun surfaceShift(area: ViraArea): Color = ViraPalette.surfaceShift(area).resolve()

    val onAccent: Color @Composable @ReadOnlyComposable get() = ViraPalette.onAccent.resolve()
    val onBrandArtwork: Color @Composable @ReadOnlyComposable get() = ViraPalette.onBrandArtwork.resolve()
    val lightArtworkVeil: Color @Composable @ReadOnlyComposable get() = ViraPalette.lightArtworkVeil.resolve()
    val onDarkArtwork: Color @Composable @ReadOnlyComposable get() = ViraPalette.onDarkArtwork.resolve()
    val providerSurface: Color @Composable @ReadOnlyComposable get() = ViraPalette.providerSurface.resolve()
    val onProviderSurface: Color @Composable @ReadOnlyComposable get() = ViraPalette.onProviderSurface.resolve()

    @Composable @ReadOnlyComposable
    fun shopBackdrop(backdrop: ViraShopBackdrop): Color = ViraPalette.shopBackdrop(backdrop).resolve()

    @Composable @ReadOnlyComposable
    fun onShopBackdrop(backdrop: ViraShopBackdrop): Color = ViraPalette.onShopBackdrop(backdrop).resolve()

    val background: Color @Composable @ReadOnlyComposable get() = ViraPalette.background.resolve()
    val backgroundGrouped: Color @Composable @ReadOnlyComposable get() = ViraPalette.backgroundGrouped.resolve()
    val surfaceElevated: Color @Composable @ReadOnlyComposable get() = ViraPalette.surfaceElevated.resolve()
    val textPrimary: Color @Composable @ReadOnlyComposable get() = ViraPalette.textPrimary.resolve()
    val textSecondary: Color @Composable @ReadOnlyComposable get() = ViraPalette.textSecondary.resolve()
    val separator: Color @Composable @ReadOnlyComposable get() = ViraPalette.separator.resolve()

    /// Contorn d'un control sense farciment propi (un camp, una píndola sense triar).
    val controlBorder: Color @Composable @ReadOnlyComposable get() = ViraPalette.controlBorder.resolve()

    /// El mateix, sobre una superfície de color: la tinta del text d'aquella superfície al 80 %.
    fun controlBorder(on: Color): Color = on.copy(alpha = on.alpha * 0.8f)

    val success: Color @Composable @ReadOnlyComposable get() = ViraPalette.success.resolve()
    val warning: Color @Composable @ReadOnlyComposable get() = ViraPalette.warning.resolve()
    val error: Color @Composable @ReadOnlyComposable get() = ViraPalette.error.resolve()

    @Composable @ReadOnlyComposable
    fun intensityTop(level: Int): Color = ViraPalette.intensityTop(level).resolve()

    @Composable @ReadOnlyComposable
    fun intensityBottom(level: Int): Color = ViraPalette.intensityBottom(level).resolve()

    @Composable @ReadOnlyComposable
    fun onIntensity(level: Int): Color = ViraPalette.onIntensity(level).resolve()

    /// Ombra suau de les peces aixecades (la capçalera, la barra, la cara triada). A l'iPhone és
    /// `.black.opacity(…)` sobre la forma; aquí és el mateix negre, i només per a ombres.
    internal val shadow = Color(0xFF000000)
}

/// El to que toca segons l'aparença de l'entorn de Vira (`ViraTheme`).
@Composable @ReadOnlyComposable
fun ViraTone.resolve(): Color = Color(pick(LocalViraEnvironment.current.isDark))
