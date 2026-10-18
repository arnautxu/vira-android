package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Pictograma 3D de Vira (`ViraPictogram` de l'iPhone): la iconografia del producte, no decoració.
/// Els fitxers són els de `res/drawable*/picto_*`, exportats de l'iPhone pel fil de Textos i recursos.
///
/// - `scalesWithText`: escala amb la mida de lletra quan acompanya text en línia; fixa quan es
///   dimensiona respecte del contenidor (mòduls).
/// - `isScene`: les il·lustracions amb escena (Viri, les mans) s'arrodoneixen; els objectes
///   retallats no.
///
/// Sempre amagat a TalkBack: acompanya el text, no hi afegeix informació.
@Composable
fun ViraPictogram(
    @DrawableRes drawable: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isScene: Boolean = false,
    scalesWithText: Boolean = false,
) {
    val side = if (scalesWithText) size * LocalDensity.current.fontScale else size
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(side)
            .clip(RoundedCornerShape(if (isScene) side * 0.22f else 0.dp)),
    )
}
