package cat.vira.android.core.design.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cat.vira.android.R

/// Tokens tipogràfics, calcats de `ViraFont.swift`: **Satoshi** i cinc mides.
///
/// | Mida | Tokens | Paper |
/// |---|---|---|
/// | 34 | `largeTitle` | Salutació d'Inici, celebracions |
/// | 22 | `title` | Títol de secció i de targeta |
/// | 17 | `headline` · `body` · `callout` | Cos i èmfasi: l'esglaó el fa el pes |
/// | 14 | `footnote` · `footnoteEmphasis` | Text secundari |
/// | 12 | `caption` | Etiquetes. Mínim absolut |
///
/// Tot va en `sp`, que és el que escala amb la mida de lletra del sistema (l'equivalent del
/// Dynamic Type). Mai `dp` per a text: no creixeria (`DESIGN_RULES.md` §8).
object ViraFont {

    val satoshi = FontFamily(
        Font(R.font.satoshi_regular, FontWeight.Normal),
        Font(R.font.satoshi_medium, FontWeight.Medium),
        Font(R.font.satoshi_bold, FontWeight.Bold),
    )

    private fun style(size: Int, weight: FontWeight) = TextStyle(
        fontFamily = satoshi,
        fontWeight = weight,
        fontSize = size.sp,
    )

    val largeTitle = style(34, FontWeight.Bold)
    val title = style(22, FontWeight.Bold)
    val headline = style(17, FontWeight.Bold)
    val body = style(17, FontWeight.Normal)
    /// Cos amb èmfasi. Mateixa mida que `body`: el que el separa és el pes.
    val callout = style(17, FontWeight.Medium)
    /// Tot el text secundari: subtítols, peus, valors sota una etiqueta.
    val footnote = style(14, FontWeight.Normal)
    /// `footnote` en negreta: hores d'agenda, on la xifra ha de destacar sense créixer.
    val footnoteEmphasis = style(14, FontWeight.Bold)
    /// Mínim absolut. Res per sota d'això.
    val caption = style(12, FontWeight.Medium)

    /// Xifres grans del temporitzador, amb dígits d'amplada fixa.
    val timer = style(64, FontWeight.Bold).copy(fontFeatureSettings = "tnum")
}
