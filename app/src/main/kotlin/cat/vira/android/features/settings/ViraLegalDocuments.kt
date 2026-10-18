package cat.vira.android.features.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.StringRes
import cat.vira.android.R
import java.net.URI

/// Documents legals que l'app pot obrir des d'Ajustos.
///
/// Les URL no viuen al codi: una adreça inventada o provisional és pitjor que dir que encara no
/// s'ha publicat. La configuració final les posa com a `meta-data` del manifest amb aquestes dues
/// claus (les de l'iPhone), i només s'accepten adreces HTTPS completes.
enum class ViraLegalDocument(val key: String, @StringRes val title: Int) {
    PRIVACY_POLICY("ViraPrivacyPolicyURL", R.string.legal_privacy_title),
    TERMS_OF_USE("ViraTermsOfUseURL", R.string.legal_terms_title),
}

object ViraLegalDocuments {
    /// L'adreça si és HTTPS i té servidor; si no, `null`.
    fun url(document: ViraLegalDocument, values: Map<String, Any?>): String? {
        val raw = values[document.key] as? String ?: return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() != "https" || uri.host.isNullOrEmpty()) return null
        return raw
    }

    fun uri(context: Context, document: ViraLegalDocument): Uri? =
        url(document, metaData(context))?.let(Uri::parse)

    private fun metaData(context: Context): Map<String, Any?> {
        val bundle = runCatching {
            context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
        }.getOrNull() ?: return emptyMap()
        return ViraLegalDocument.entries.associate { it.key to bundle.getString(it.key) }
    }
}
