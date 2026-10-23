package cat.vira.android.features.shop

/// Les preferències de la Botiga, amb els mateixos noms que a l'iPhone (`ViraPreferences.shopWish`…).
/// Viuen al telèfon i prou: són de pantalla, no dades de l'usuari que hagin de viatjar. Porten el
/// prefix `settings.`, i per això s'esborren en canviar de compte.
object ShopPreferences {
    /// La skin marcada amb «La vull». Buida si no n'hi ha cap.
    const val WISH = "settings.shopWish"
    /// L'última skin de la qual l'avís d'objectiu ja ha dit «ja hi arribes»: un sol cop per objectiu.
    const val GOAL_ANNOUNCED = "settings.shopGoalAnnounced"
    /// L'avís d'objectiu demana a la Botiga que obri el provador del teu objectiu.
    const val OPEN_WISH = "settings.shopOpenWish"
}
