package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraShopBackdrop

/// El Viri d'una skin, dibuixat allà on s'ha de veure sencer (`SkinPortrait`): les fitxes de la
/// Botiga, la revelació, la capçalera del Perfil. Amb una peça per capes, el decorat omple el marc i
/// Viri hi va a sobre amb ombra pròpia; amb una peça plana, l'escena omple el marc.
///
/// Omple el que li donen: qui la posa decideix la mida i el retall.
@Composable
fun SkinPortrait(@DrawableRes backdrop: Int?, @DrawableRes cutout: Int? = null, modifier: Modifier = Modifier) {
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (backdrop != null) {
            Image(painterResource(backdrop), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            // Una clau sense art: un fons tranquil, mai un quadre blanc buit.
            Box(Modifier.fillMaxSize().background(ViraGradient.shopBackdrop(ViraShopBackdrop.Neutral)))
        }
        if (cutout != null) {
            Image(
                painterResource(cutout), null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/// La skin posada, o el Viri de sempre (`SkinPortrait(equipped:)`).
@Composable
fun SkinPortrait(equipped: SkinItem?, modifier: Modifier = Modifier) {
    SkinPortrait(
        backdrop = equipped?.backdropArt ?: R.drawable.viri_entry_waving,
        cutout = equipped?.cutoutArt,
        modifier = modifier,
    )
}

/// L'avatar rodó de la capçalera (`ViraTopBar(avatar = …)`): la skin posada retallada en cercle.
@Composable
fun SkinAvatar(equipped: SkinItem?, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    SkinPortrait(
        equipped,
        modifier.size(size).clip(CircleShape).background(ViraColor.brandSurface, CircleShape),
    )
}
