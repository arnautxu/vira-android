package cat.vira.android.features.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch

/// Les dues cares del detall d'un fill o filla, al selector de `DESIGN_RULES.md` §2.
internal enum class ParentChildSection { SUMMARY, SEND }

/// El que un pare/mare veu d'un fill/a (`ParentChildView` de l'iPhone).
///
/// Cada àmbit diu si està obert o tancat. Un àmbit tancat no s'amaga: es diu. Amagar-lo faria que
/// el panell semblés complet quan no ho és, i un pare/mare que no sap què no veu treu conclusions
/// equivocades.
///
/// Subpantalla empesa (§1): enrere a la barra i el nom en gran, en lila. Sota, el selector a tot
/// l'ample amb **Resum** (els recomptes) i **Envia** (ànim o recordatori).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentChildScreen(link: ParentLink, knownName: String?, deps: ParentDeps, onBack: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(ParentChildSection.SUMMARY) }
    var summary by remember { mutableStateOf(ParentChildSummary()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    var compose by rememberSaveable { mutableStateOf<ParentComposeMode?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        parentAttempt({ error = it }) { summary = deps.loader.load(link, deps.session.accessToken()) }
        isLoading = false
    }
    LaunchedEffect(link.id) { load() }

    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
        ParentBackBar(stringResource(R.string.parent_family_title), onBack)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { isRefreshing = true; load(); isRefreshing = false } },
            modifier = Modifier.weight(1f),
        ) {
            ParentChildContent(
                link = link,
                // El nom que l'arrel ja coneix, perquè el títol no salti de «Fill o filla» al nom.
                name = summary.displayName ?: knownName,
                summary = summary,
                isLoading = isLoading,
                error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
                section = section,
                onSection = { section = it },
                onCompose = { compose = it },
            )
        }
    }

    compose?.let { mode ->
        ParentComposeSheet(mode, link.childUserId, deps, onDismiss = { compose = null })
    }
}

/// El detall sense estat, per a les captures.
@Composable
internal fun ParentChildContent(
    link: ParentLink,
    name: String?,
    summary: ParentChildSummary,
    isLoading: Boolean,
    error: String?,
    section: ParentChildSection,
    onSection: (ParentChildSection) -> Unit,
    onCompose: (ParentComposeMode) -> Unit,
) {
    val items = listOf(
        ViraSectionItem(ParentChildSection.SUMMARY.name, stringResource(R.string.parent_section_summary), R.drawable.picto_plan_check),
        ViraSectionItem(ParentChildSection.SEND.name, stringResource(R.string.parent_section_send), R.drawable.picto_plan_reminder),
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = ViraSpacing.majorSectionGap),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap),
    ) {
        ViraPageTitle(name ?: stringResource(R.string.parent_child_title), ViraArea.Today)
        ViraSectionSwitcher(
            area = ViraArea.Today,
            items = items,
            selectedKey = section.name,
            onSelect = { key -> onSection(ParentChildSection.valueOf(key)) },
            showsAllLabels = true,
        )
        if (error != null) ViraInlineError(error)
        when (section) {
            ParentChildSection.SUMMARY -> ParentChildSummaryContent(link, summary, isLoading)
            ParentChildSection.SEND -> ParentChildSendContent(onCompose)
        }
    }
}
