package cat.vira.android.features.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences

/// Sobre Vira: versió, tornar a veure l'entrada, documents legals i l'avís clínic.
///
/// Les adreces arriben de `ViraLegalDocuments`: sense configurar, la fila ho diu i no obre res.
@Composable
fun SettingsAboutSection() {
    val backend = LocalSettingsBackend.current
    val context = LocalContext.current
    // Posar-la a `false` n'hi ha prou: l'arrencada (`app/`) hi reacciona i torna al flux d'entrada.
    // No tanca la sessió ni esborra res.
    var onboardingCompleted by rememberBooleanPreference(ViraPreferences.ONBOARDING_COMPLETED, true)
    val links = remember { ViraLegalDocument.entries.associateWith { ViraLegalDocuments.uri(context, it) } }
    val versionTitle = stringResource(R.string.settings_about_version)
    val notPublished = stringResource(R.string.legal_not_published)

    SettingsGroup(header = stringResource(R.string.settings_about_section)) {
        row { SettingsValueRow(backend.appVersion) { SettingsLabel(versionTitle) } }
        row { SettingsActionRow(stringResource(R.string.settings_about_replay_onboarding), { onboardingCompleted = false }) }
        ViraLegalDocument.entries.forEach { document ->
            val link = links[document]
            row {
                val title = stringResource(document.title)
                if (link != null) {
                    SettingsActionRow(title, {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }, glyph = SettingsGlyphKind.Link)
                } else {
                    SettingsValueRow(notPublished) { SettingsLabel(title) }
                }
            }
        }
        row { SettingsNoteRow(stringResource(R.string.settings_about_medical)) }
    }
}
