package cat.vira.android.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R

/// Les quatre pestanyes, calcades de `ViraTab` de l'iPhone (`Vira/App/ViraNavigationTypes.swift`).
/// Concentra't no és una pestanya: és l'acció central de la barra i obre una pantalla sencera.
enum class ViraTab(@StringRes val title: Int, @DrawableRes val icon: Int) {
    Today(R.string.tab_today, R.drawable.tab_today),
    Plan(R.string.tab_plan, R.drawable.tab_plan),
    Emotions(R.string.tab_emotions, R.drawable.tab_emotions),
    Progress(R.string.tab_viri, R.drawable.tab_viri);

    /// Nom de ruta estable per a la navegació. No es tradueix ni es canvia.
    val route: String get() = name.lowercase()
}
