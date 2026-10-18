package cat.vira.android.features.parent

import java.net.URI

/// Els enllaços de vinculació del panell de pares (`ViraUniversalLink` de l'iPhone).
///
/// Vincular vol dir que el menor passi un codi de sis caràcters i que el pare o la mare l'escrigui
/// bé. Amb un enllaç, el codi el porta l'enllaç: s'envia per WhatsApp, es toca, i el camp ja arriba
/// omplert. **Mai s'envia sol**: prémer «Vincula» segueix sent de qui vincula (`ParentLinkRedeemSheet`).
///
/// Dues formes del mateix enllaç:
/// - `https://vira.cat/link/A1B2C3` — l'enllaç d'app. És el que s'envia: si l'app hi és, s'obre;
///   si no, s'obre la web.
/// - `vira://link/A1B2C3` — l'esquema propi, que ja existeix per a l'auth. No s'envia mai a ningú.
///
/// **El codi no és cap secret que valgui res per si sol**: caduca, és d'un sol ús i sense sessió
/// iniciada no fa res. El que autoritza la vinculació és el compte de qui el fa servir.
///
/// Perquè Android obri l'app amb l'enllaç `https` cal el filtre d'intents al manifest i el fitxer
/// `assetlinks.json` servit a `https://vira.cat/.well-known/`. Fins que hi sigui, l'enllaç obre el
/// navegador: es degrada, no es trenca.
object ParentLinkUrl {

    const val HOST = "vira.cat"
    private const val LINK_PATH = "link"

    /// El codi de vinculació que porti l'URL, si en porta.
    ///
    /// Accepta les dues formes i normalitza a majúscules, que és com es generen. No valida la
    /// longitud: qui sap què és un codi vàlid és el servidor.
    fun code(from url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val segments = uri.rawPath.orEmpty().split('/').filter { it.isNotEmpty() }
        return when {
            // https://vira.cat/link/CODI
            uri.scheme == "https" && uri.host == HOST -> {
                val index = segments.indexOf(LINK_PATH)
                if (index >= 0) normalised(segments.getOrNull(index + 1)) else null
            }
            // vira://link/CODI — aquí el primer segment és l'`host`, no el camí.
            uri.scheme == "vira" && uri.host == LINK_PATH -> normalised(segments.lastOrNull())
            else -> null
        }
    }

    /// L'enllaç que es comparteix, a partir d'un codi.
    fun url(forCode code: String): String = "https://$HOST/$LINK_PATH/${code.uppercase()}"

    private fun normalised(raw: String?): String? = raw?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
}
