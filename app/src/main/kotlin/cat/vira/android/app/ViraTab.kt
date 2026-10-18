package cat.vira.android.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea

/// Les quatre pestanyes, calcades de `ViraTab` de l'iPhone (`Vira/App/ViraNavigationTypes.swift`).
/// El nom és el curt de la barra (`shortTitleKey` a l'iPhone).
/// Concentra't no és una pestanya: és l'acció central de la barra i obre una pantalla sencera.
enum class ViraTab(@StringRes val title: Int, @DrawableRes val icon: Int, val area: ViraArea) {
    Today(R.string.tab_today_short, R.drawable.tab_today, ViraArea.Today),
    Plan(R.string.tab_plan_short, R.drawable.tab_plan, ViraArea.Plan),
    Emotions(R.string.tab_emotions_short, R.drawable.tab_emotions, ViraArea.Emotions),
    /// Com a l'iPhone, la icona és el ViriCoin de la Botiga i no un disseny propi.
    Progress(R.string.tab_viri_short, R.drawable.picto_shop, ViraArea.Rewards);

    /// Nom de ruta estable per a la navegació. No es tradueix ni es canvia.
    val route: String get() = name.lowercase()

    companion object {
        /// La icona i el nom de l'acció central, que no és cap destí.
        @DrawableRes val focusIcon: Int = R.drawable.picto_focus
        @StringRes val focusTitle: Int = R.string.tab_focus_short

        fun fromRoute(route: String): ViraTab? = entries.firstOrNull { it.route == route }
    }
}
