package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R

/// Un accessori (`AccessoryItem`): un adhesiu de posició fixa al racó de la capçalera del Perfil,
/// independent de la skin. Van sortir de la Botiga el 2026-08-06 a l'iPhone; es conserven perquè
/// qui ja en té un el segueixi veient posat.
data class AccessoryItem(val key: String, @DrawableRes val art: Int, @StringRes val nameRes: Int, val price: Int) {
    companion object {
        val catalog = listOf(
            AccessoryItem("glasses", R.drawable.viri_accessory_glasses, R.string.accessory_glasses, 300),
            AccessoryItem("scarf", R.drawable.viri_accessory_scarf, R.string.accessory_scarf, 600),
        )

        fun item(key: String): AccessoryItem? = catalog.firstOrNull { it.key == key }
    }
}
