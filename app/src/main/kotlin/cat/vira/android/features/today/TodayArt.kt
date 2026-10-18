package cat.vira.android.features.today

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.R

/// Pictogrames i escenes pel nom que fa servir l'iPhone (`picto-flag`, `viri-suggest-capture-1`).
///
/// Les tasques i els hàbits desen el nom de la icona, no un recurs: és el que viatja al servidor i
/// el que entén l'iPhone. Els recursos es diuen igual amb guions baixos (`export-assets.py`), així
/// que es busquen pel nom. Si un nom no hi és (un pictograma nou de l'iPhone que encara no s'ha
/// exportat), surt el de reserva i no un forat.
internal object TodayArt {

    @SuppressLint("DiscouragedApi")
    @DrawableRes
    fun resolve(context: android.content.Context, name: String, @DrawableRes fallback: Int = R.drawable.picto_flag): Int {
        val resource = resourceName(name)
        val id = context.resources.getIdentifier(resource, "drawable", context.packageName)
        return if (id != 0) id else fallback
    }

    /// La mateixa regla que `resource_name` d'`export-assets.py`: `viri-suggest-windDown-2` →
    /// `viri_suggest_wind_down_2`.
    fun resourceName(name: String): String = name
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}

@Composable
@DrawableRes
internal fun todayDrawable(name: String, @DrawableRes fallback: Int = R.drawable.picto_flag): Int {
    val context = LocalContext.current
    return remember(name, fallback) { TodayArt.resolve(context, name, fallback) }
}
