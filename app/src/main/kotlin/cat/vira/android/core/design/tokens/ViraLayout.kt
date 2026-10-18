package cat.vira.android.core.design.tokens

import androidx.compose.ui.unit.dp

/// Espaiat, radis i mides tàctils, calcats de `ViraLayout.swift`. Escala de 4 dp.
object ViraSpacing {
    /// Només entre dues línies d'una mateixa etiqueta (icona i nom d'un selector).
    val hairline = 2.dp
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp

    /// Títol i metadada del mateix element.
    val textRelated = xxs
    /// Etiqueta persistent i camp, o icona i text.
    val labelGap = xs
    /// Elements veïns dins d'un mateix grup.
    val rowGap = s
    /// Contingut respecte de la vora d'una targeta.
    val contentInset = m
    /// Grups de contingut independents.
    val sectionGap = xl
    /// Canvi de bloc principal dins d'una pantalla.
    val majorSectionGap = xxl

    /// Marge lateral estàndard de pantalla.
    val screenMargin = 16.dp
}

object ViraRadius {
    val s = 12.dp
    val m = 16.dp
    val l = 22.dp
    /// Cantonada exagerada d'un sol vèrtex: el recurs que trenca la graella dels mòduls.
    val dramatic = 46.dp
}

/// Mides tàctils. A l'iPhone el mínim és 44 pt; a Android, 48 dp
/// (`ACCESSIBILITY_REQUIREMENTS.md` i `CLAUDE.md` del repositori: «tàctils ≥ 48 dp»).
object ViraTouch {
    val minimum = 48.dp
    /// Objectiu per a accions freqüents (marcar hàbit, presa, emoció).
    val comfortable = 56.dp
    /// Cares emocionals.
    val emotion = 88.dp
}
