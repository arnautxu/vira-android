package cat.vira.android.core.resources

import android.content.Context

/// L'idioma en què Vira ensenya els textos: `ca`, `es` o `en` (`Bundle.main.preferredLocalizations`
/// de l'iPhone).
///
/// Android tria els recursos recorrent la llista d'idiomes del telèfon, i el primer que Vira té
/// guanya. Si no en té cap, l'anglès: és l'idioma de reserva, com a l'iPhone (Arnau, 2026-10-21).
/// Serveix perquè el que no són recursos (la veu de Mindfulness, els correus de Supabase) vagi en
/// el mateix idioma que la pantalla.
object ViraLanguage {
    val supported = listOf("ca", "es", "en")
    const val FALLBACK = "en"

    fun resolve(languages: List<String>): String = languages.firstOrNull { it in supported } ?: FALLBACK

    fun current(context: Context): String {
        val locales = context.resources.configuration.locales
        return resolve((0 until locales.size()).map { locales[it].language })
    }
}
