package cat.vira.android.features.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/// Què veu aquesta persona, decidit pel fill o la filla (`ParentPanelScopesView` de l'iPhone).
///
/// Quatre interruptors separats, no un de sol. Emocions i medicació estan apagats de fàbrica i
/// porten el seu propi avís. Tancar-los té efecte a l'instant, i no per un filtre d'aquesta
/// pantalla sinó per la política del servidor.
@Composable
internal fun ParentPanelScopesScreen(link: ParentLink, deps: ParentDeps, onBack: () -> Unit, onChange: () -> Unit) {
    var draft by remember(link.id) { mutableStateOf(link) }
    var persisted by remember(link.id) { mutableStateOf(link) }
    var isWorking by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val scope = rememberCoroutineScope()
    val saving = remember { Mutex() }

    // Es desa a cada canvi, sense botó: tancar un àmbit ha de tenir efecte quan es toca, no quan
    // es confirma. Un «Desa» que algú no premi deixaria la porta oberta.
    // Els desats van d'un en un i sempre amb l'últim estat: dos tocs ràpids no poden arribar
    // desordenats i tornar a obrir un àmbit que ja s'ha tancat.
    fun change(updated: ParentLink) {
        draft = updated
        error = null
        parentSaveScope.launch {
            saving.withLock {
                val latest = draft
                val saved = parentAttempt({ error = it }) { deps.client.updateScopes(latest, deps.session.accessToken()) }
                // Si no s'ha desat, l'interruptor torna a com és al servidor: ensenyar un àmbit
                // tancat que encara és obert faria creure que la porta ja és tancada.
                if (saved != null) { persisted = latest; onChange() } else if (draft == latest) draft = persisted
            }
        }
    }

    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
        ParentBackBar(stringResource(R.string.settings_parent_panel_title), onBack)
        ParentPanelScopesContent(
            link = draft,
            error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
            onChange = ::change,
            onRevoke = { confirming = true },
        )
    }
    if (isWorking) ParentProgress()

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.parent_link_revoke_confirm), style = ViraFont.headline) },
            containerColor = ViraColor.surfaceElevated,
            confirmButton = {
                Text(
                    stringResource(R.string.parent_link_revoke), style = ViraFont.callout, color = ViraColor.error,
                    modifier = Modifier
                        .viraPressable {
                            confirming = false
                            isWorking = true
                            scope.launch {
                                val done = saving.withLock { parentAttempt({ error = it }) { deps.client.revoke(draft, deps.session.accessToken()) } }
                                isWorking = false
                                if (done != null) { onChange(); onBack() }
                            }
                        }
                        .semantics { role = Role.Button }
                        .padding(ViraSpacing.s),
                )
            },
            dismissButton = {
                ViraActionButton(stringResource(R.string.action_cancel), { confirming = false }, hierarchy = ViraActionHierarchy.Textual)
            },
        )
    }
}

/// Sense estat, per a les captures.
@Composable
internal fun ParentPanelScopesContent(
    link: ParentLink,
    error: String?,
    onChange: (ParentLink) -> Unit,
    onRevoke: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = ViraSpacing.majorSectionGap),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        ViraPageTitle(stringResource(R.string.parent_scopes_title), ViraArea.Today)
        ParentGroup(
            header = stringResource(R.string.parent_scopes_section),
            rows = {
                row { ParentToggleRow(stringResource(R.string.parent_scope_usage), link.scopeUsage, { onChange(link.copy(scopeUsage = it)) }) }
                row { ParentToggleRow(stringResource(R.string.parent_scope_habits), link.scopeHabits, { onChange(link.copy(scopeHabits = it)) }) }
            },
        )
        ParentGroup(
            footer = stringResource(R.string.parent_scopes_sensitive_footnote),
            rows = {
                row { ParentToggleRow(stringResource(R.string.parent_scope_emotions), link.scopeEmotions, { onChange(link.copy(scopeEmotions = it)) }) }
                row { ParentToggleRow(stringResource(R.string.parent_scope_medication), link.scopeMedication, { onChange(link.copy(scopeMedication = it)) }) }
            },
        )
        ParentGroup(
            footer = stringResource(R.string.parent_link_revoke_footnote),
            rows = {
                row {
                    Text(
                        stringResource(R.string.parent_link_revoke), style = ViraFont.body, color = ViraColor.error,
                        modifier = Modifier.fillMaxWidth().viraPressable(onClick = onRevoke),
                    )
                }
            },
        )
        if (error != null) ViraInlineError(error)
    }
}
