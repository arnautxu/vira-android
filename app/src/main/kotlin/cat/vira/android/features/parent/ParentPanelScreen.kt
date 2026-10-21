package cat.vira.android.features.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch

/// El panell de pares tal com el veu el fill o la filla (`ParentPanelView` de l'iPhone), des
/// d'Ajustos, a totes les builds (Arnau, 2026-10-18). Ajustos només l'ofereix amb sessió.
///
/// L'ordre de la pantalla és el de qui mana: primer el codi —que és l'única manera que algú
/// entri—, després qui hi ha connectat i què veu, i al final el que li han enviat. Trencar el
/// vincle és sempre a un toc de distància i no demana permís a ningú (`PRIVACY_AND_SAFETY.md` §4.5b).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPanelScreen(onClose: () -> Unit) {
    val deps = rememberParentDeps()
    var links by remember { mutableStateOf<List<ParentLink>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    var openScopes by rememberSaveable { mutableStateOf<String?>(null) }
    var showingInbox by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        parentAttempt({ error = it }) { links = deps.client.links(deps.session.accessToken()); loaded = true }
    }
    LaunchedEffect(Unit) { load() }

    val scopesLink = links.firstOrNull { it.id.toString() == openScopes && it.isActive }
    when {
        scopesLink != null -> {
            BackHandler { openScopes = null }
            ParentPanelScopesScreen(scopesLink, deps, onBack = { openScopes = null }, onChange = { scope.launch { load() } })
        }
        showingInbox -> {
            BackHandler { showingInbox = false }
            ParentPanelInboxScreen(deps, onBack = { showingInbox = false })
        }
        else -> Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
            BackHandler(onBack = onClose)
            ParentBackBar(stringResource(R.string.action_settings), onClose)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { isRefreshing = true; load(); isRefreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                ParentPanelContent(
                    links = links, loaded = loaded, code = code, isWorking = isWorking,
                    error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
                    onCreateCode = {
                        error = null
                        isWorking = true
                        scope.launch {
                            parentAttempt({ error = it }) { code = deps.client.createInvite(deps.session.accessToken()) }
                            isWorking = false
                        }
                    },
                    onOpenLink = { openScopes = it.id.toString() },
                    onOpenInbox = { showingInbox = true },
                )
                if (isWorking) ParentProgress()
            }
        }
    }
}

/// La pantalla del jove sense estat, per a les captures.
@Composable
internal fun ParentPanelContent(
    links: List<ParentLink>,
    code: String?,
    isWorking: Boolean,
    error: String?,
    onCreateCode: () -> Unit,
    onOpenLink: (ParentLink) -> Unit,
    onOpenInbox: () -> Unit,
    loaded: Boolean = true,
) {
    val active = links.filter { it.isActive }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = ViraSpacing.majorSectionGap),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        ViraPageTitle(stringResource(R.string.settings_parent_panel_title), ViraArea.Today)

        ParentGroup(
            header = stringResource(R.string.parent_code_section),
            footer = stringResource(R.string.parent_code_footnote),
            rows = { row {
                if (code != null) {
                    val label = stringResource(R.string.parent_code_accessibility)
                    val spelled = code.toList().joinToString(" ")
                    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                        Text(code, style = ViraFont.title, color = ViraColor.textPrimary, modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $spelled" })
                        Text(stringResource(R.string.parent_code_expiry), style = ViraFont.footnote, color = ViraColor.textSecondary)
                    }
                } else {
                    ViraActionButton(stringResource(R.string.parent_code_create), onCreateCode, Modifier.fillMaxWidth(), enabled = !isWorking)
                }
            } },
        )

        ParentGroup(
            header = stringResource(R.string.parent_links_section),
            rows = {
                // «Ningú» només quan el servidor ho ha dit: amb la llista sense carregar o amb un
                // error, dir-ho faria creure que no hi ha cap porta oberta.
                if (active.isEmpty()) {
                    if (loaded) row { Text(stringResource(R.string.parent_links_empty), style = ViraFont.footnote, color = ViraColor.textSecondary) }
                    else row { ParentProgressInline() }
                } else {
                    active.forEach { link -> row { NavigationRow(stringResource(R.string.parent_links_row), withIcon = true) { onOpenLink(link) } } }
                }
            },
        )

        ParentGroup(rows = { row { NavigationRow(stringResource(R.string.parent_inbox_title), onClick = onOpenInbox) } })

        if (error != null) ViraInlineError(error)
    }
}

/// Una fila que porta a una altra pantalla: tota es toca, amb el «›» al final.
@Composable
private fun NavigationRow(label: String, withIcon: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().viraPressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        if (withIcon) ViraPictogram(R.drawable.picto_habit_friends, size = 28.dp, scalesWithText = true)
        Text(label, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
        ParentChevron()
    }
}
