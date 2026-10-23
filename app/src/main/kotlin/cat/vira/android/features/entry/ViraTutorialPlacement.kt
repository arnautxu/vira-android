package cat.vira.android.features.entry

import androidx.compose.ui.geometry.Rect

/// On va la targeta del tomb: a la franja lliure més gran entre les peces il·luminades
/// (`ViraTutorialPlacement.swift`, Arnau, 2026-10-21).
///
/// Es miren totes les peces com a obstacles, de dalt a baix, i la targeta va al forat més gran:
/// - Si a dalt de la franja hi ha una peça, la targeta s'hi enganxa per sota (`TOP`).
/// - Si només n'hi ha a sota (la pestanya), s'hi enganxa per sobre (`BOTTOM`).
/// - Sense peces, al mig.
///
/// Si cap franja arriba al mínim, la targeta va a dalt de tot i tapa el que calgui: amb lletra
/// gran val més llegir-ho que veure-ho tot. És una funció pura, en píxels, i es prova sola.
data class ViraTutorialPlacement(val minY: Float, val maxY: Float, val anchor: Anchor) {
    enum class Anchor { TOP, BOTTOM, CENTER }

    val height: Float get() = maxY - minY

    companion object {
        /// - `obstacles`: les peces il·luminades (i la píndola «Toca aquí»).
        /// - `top`, `bottom`: la franja útil de la pantalla.
        /// - `margin`: l'aire entre la targeta i cada peça.
        /// - `minimum`: la franja més petita on encara cap una targeta llegible.
        fun place(obstacles: List<Rect>, top: Float, bottom: Float, margin: Float, minimum: Float): ViraTutorialPlacement {
            val blocking = obstacles
                .filter { it.bottom > top && it.top < bottom }
                .sortedBy { it.top }
            if (blocking.isEmpty()) return ViraTutorialPlacement(top, bottom, Anchor.CENTER)

            val gaps = mutableListOf<ViraTutorialPlacement>()
            var cursor = top
            var cursorIsPiece = false
            for (piece in blocking) {
                if (piece.top > cursor) {
                    val start = cursor + if (cursorIsPiece) margin else 0f
                    val end = piece.top - margin
                    if (end > start) gaps += ViraTutorialPlacement(start, end, if (cursorIsPiece) Anchor.TOP else Anchor.BOTTOM)
                }
                if (piece.bottom > cursor) {
                    cursor = piece.bottom
                    cursorIsPiece = true
                }
            }
            if (bottom - margin > cursor + margin) gaps += ViraTutorialPlacement(cursor + margin, bottom, Anchor.TOP)

            val best = gaps.maxByOrNull { it.height }
            if (best != null && best.height >= minimum) return best
            // Cap franja hi arriba: a dalt de tot, fins a la peça de baix de tot (la pestanya), que
            // és la que no es pot tapar perquè és la que es toca.
            val floor = blocking.last().top - margin
            return ViraTutorialPlacement(top, maxOf(top + minimum, floor), Anchor.TOP)
        }
    }
}
