package cat.vira.android.core.design.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

/// Degradats del sistema, calcats de `ViraGradient.swift`.
///
/// La norma que els fa segurs: un degradat només afegeix tons **cap al costat que allunya el
/// color del text**, mai cap al text. Per això cap degradat baixa cap contrast ja mesurat; els
/// tons extrems són a `ViraPalette`.
object ViraGradient {

    /// Direcció del degradat. `Diagonal` és la de les peces amb superfície (la llum entra per un
    /// vèrtex); `Horizontal` és per a les barres de progrés.
    enum class Axis { Diagonal, Horizontal, Vertical }

    /// Targeta modular: tres tons en diagonal (il·luminat, base al 48 %, carregat).
    @Composable @ReadOnlyComposable
    fun card(area: ViraArea, axis: Axis = Axis.Diagonal): Brush = axisBrush(
        axis,
        0f to ViraPalette.cardFillStart(area).resolve(),
        0.48f to ViraPalette.cardFill(area).resolve(),
        1f to ViraPalette.cardFillEnd(area).resolve(),
    )

    /// Farciment d'accent: píndola triada, botó ple, barra de progrés.
    @Composable @ReadOnlyComposable
    fun accent(area: ViraArea, axis: Axis = Axis.Diagonal): Brush = axisBrush(
        axis,
        0f to ViraPalette.accent(area).resolve(),
        1f to ViraPalette.accentEnd(area).resolve(),
    )

    /// Fons tenyit d'una pantalla de secció. La diagonal no arriba al vèrtex oposat sinó fins a
    /// baix: l'ull llegeix la pantalla de dalt a baix.
    @Composable @ReadOnlyComposable
    fun section(area: ViraArea): Brush = pointsBrush(
        start = { Offset.Zero },
        end = { Offset(it.width / 2f, it.height) },
        0f to ViraPalette.surfaceShift(area).resolve(),
        0.34f to ViraPalette.surface(area).resolve(),
        1f to ViraPalette.backgroundGrouped.resolve(),
    )

    /// Fons d'un ítem de la Botiga: dos tons en diagonal.
    @Composable @ReadOnlyComposable
    fun shopBackdrop(backdrop: ViraShopBackdrop): Brush = axisBrush(
        Axis.Diagonal,
        0f to ViraPalette.shopBackdrop(backdrop).resolve(),
        1f to ViraPalette.shopBackdropEnd(backdrop).resolve(),
    )

    private fun axisBrush(axis: Axis, vararg stops: Pair<Float, Color>): Brush = when (axis) {
        Axis.Diagonal -> pointsBrush({ Offset.Zero }, { Offset(it.width, it.height) }, *stops)
        Axis.Horizontal -> pointsBrush({ Offset.Zero }, { Offset(it.width, 0f) }, *stops)
        Axis.Vertical -> pointsBrush({ Offset.Zero }, { Offset(0f, it.height) }, *stops)
    }

    /// Com els `UnitPoint` de SwiftUI: els extrems es calculen sobre la mida real de la peça.
    private fun pointsBrush(
        start: (Size) -> Offset,
        end: (Size) -> Offset,
        vararg stops: Pair<Float, Color>,
    ): Brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = LinearGradientShader(
            from = start(size),
            to = end(size),
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )

        override fun equals(other: Any?): Boolean = other === this
        override fun hashCode(): Int = System.identityHashCode(this)
    }
}
