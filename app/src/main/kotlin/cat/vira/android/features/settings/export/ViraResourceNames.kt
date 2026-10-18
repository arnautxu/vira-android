package cat.vira.android.features.settings.export

import android.annotation.SuppressLint
import android.content.Context

/// Claus de l'iPhone que es componen en temps d'execució (`emotion.\(raw)`, `skin.\(key)`,
/// `viri-skin-\(key)`) i el recurs d'Android que els fa d'equivalent.
///
/// El nom es tradueix amb **la mateixa regla** que `scripts/export-strings.py` i `export-assets.py`
/// (`resource_name`): camelCase a guions baixos, tot en minúscules, qualsevol altre signe a `_`.
object ViraResourceNames {

    private val javaKeywords = (
        "abstract assert boolean break byte case catch char class const continue default do double else " +
            "enum extends final finally float for goto if implements import instanceof int interface long " +
            "native new package private protected public return short static strictfp super switch " +
            "synchronized this throw throws transient try void volatile while true false null"
        ).split(" ").toSet()

    fun of(key: String): String {
        var name = key.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_").lowercase()
        name = name.replace(Regex("[^a-z0-9]+"), "_").trim('_')
        if (name.firstOrNull()?.isDigit() == true || name in javaKeywords) name = "s_$name"
        return name
    }

    /// El text d'una clau del catàleg. Si no hi és, la clau mateixa: millor una clau visible que un
    /// forat que faci dubtar de si s'ha exportat tot.
    @SuppressLint("DiscouragedApi")
    fun string(context: Context, key: String): String = find(context, key) ?: key

    /// El text d'una clau del catàleg, o `null` si no hi és.
    @SuppressLint("DiscouragedApi")
    fun find(context: Context, key: String): String? {
        val id = context.resources.getIdentifier(of(key), "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    /// Una imatge del catàleg pel seu nom de l'iPhone, o 0 si no hi és.
    @SuppressLint("DiscouragedApi")
    fun drawable(context: Context, assetName: String): Int =
        context.resources.getIdentifier(of(assetName), "drawable", context.packageName)
}
