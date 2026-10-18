package cat.vira.android.features.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTabTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.util.UUID

/// L'app tal com la veu un pare o una mare (`ParentRootView` de l'iPhone).
///
/// **No són les quatre pestanyes.** Un compte de pare/mare no té pla, ni emocions, ni Viri: només
/// la llista de fills o filles que l'han vinculat i el que cadascun li hagi obert.
///
/// - `account`: la pantalla del compte (la fa el fil d'Ajustos), oberta des de la pastilla.
/// - `linkCodes`: els codis que arriben per enllaç (`ParentLinkUrl`). Omplen el camp del full de
///   vinculació si és obert, i **mai l'envien**; si no hi ha full obert, no passa res.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentRootScreen(
    account: @Composable (onClose: () -> Unit) -> Unit,
    linkCodes: Flow<String> = emptyFlow(),
) {
    val deps = rememberParentDeps()
    var links by remember { mutableStateOf<List<ParentLink>>(emptyList()) }
    var names by remember { mutableStateOf<Map<UUID, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    var isRedeeming by rememberSaveable { mutableStateOf(false) }
    var showingAccount by rememberSaveable { mutableStateOf(false) }
    var openChild by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        parentAttempt({ error = it }) {
            val token = deps.session.accessToken()
            val fetched = deps.client.links(token)
            links = fetched
            names = deps.loader.displayNames(fetched.filter { it.isActive }, token)
        }
        isLoading = false
    }
    LaunchedEffect(Unit) { load() }

    val child = links.firstOrNull { it.id.toString() == openChild && it.isActive }
    when {
        showingAccount -> {
            BackHandler { showingAccount = false }
            account { showingAccount = false }
        }
        child != null -> {
            BackHandler { openChild = null }
            ParentChildScreen(child, names[child.childUserId], deps, onBack = { openChild = null })
        }
        else -> Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
            ParentTopBar(links.count { it.isActive }, onAccount = { showingAccount = true }, onLink = { isRedeeming = true })
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { isRefreshing = true; load(); isRefreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                ParentRootContent(
                    links = links, names = names, isLoading = isLoading,
                    error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
                    onOpenChild = { openChild = it.id.toString() },
                    onLink = { isRedeeming = true },
                )
                if (isLoading && links.isEmpty()) ParentProgress()
            }
        }
    }

    if (isRedeeming) {
        ParentRedeemSheet(deps, linkCodes, onDismiss = { isRedeeming = false }, onLinked = { scope.launch { load() } })
    }
}

/// L'arrel sense estat, perquè les captures la puguin dibuixar sola. Ordre de `DESIGN_RULES.md`
/// §3: Viri (protagonista) i després una targeta per fill o filla.
@Composable
internal fun ParentRootContent(
    links: List<ParentLink>,
    names: Map<UUID, String>,
    isLoading: Boolean,
    error: String?,
    onOpenChild: (ParentLink) -> Unit,
    onLink: () -> Unit,
) {
    val active = links.filter { it.isActive }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = ViraSpacing.majorSectionGap),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.rowGap),
    ) {
        ViraTabTitle(stringResource(R.string.parent_family_title), Modifier.padding(top = ViraSpacing.s))
        ParentViriCard(stringResource(R.string.parent_viri_root), R.drawable.viri_entry_waving, Modifier.padding(top = ViraSpacing.xs))

        if (error != null) ViraInlineError(error)

        if (active.isEmpty() && !isLoading) {
            ViraEmptyState(
                title = stringResource(R.string.parent_children_empty_title),
                message = stringResource(R.string.parent_children_empty_message),
                icon = { ViraPictogram(R.drawable.picto_habit_friends, size = 64.dp) },
            )
        } else if (active.isNotEmpty()) {
            ParentSectionHeader(
                stringResource(R.string.parent_children_section),
                Modifier.padding(top = ViraSpacing.s),
                trailing = active.size.toString(),
            )
            active.forEach { link -> ParentChildCard(link, names[link.childUserId], onOpen = { onOpenChild(link) }) }
        }

        AddLinkButton(onLink)
        ParentFootnote(stringResource(R.string.parent_link_footnote))
    }
}

/// La franja buida translúcida de §4: el lloc on aniria el pròxim vincle.
@Composable
private fun AddLinkButton(onLink: () -> Unit) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val accent = ViraColor.brandAccent
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .background(ViraColor.surfaceElevated.copy(alpha = 0.55f), shape)
            .drawBehind {
                val inset = 1.dp.toPx()
                drawRoundRect(
                    accent.copy(alpha = 0.45f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(ViraRadius.l.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .viraPressable(onClick = onLink)
            .padding(ViraSpacing.s),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusGlyph(accent)
        Text(stringResource(R.string.parent_link_add_code), style = ViraFont.headline, color = accent)
    }
}

/// El full de vinculació amb el seu estat.
@Composable
private fun ParentRedeemSheet(deps: ParentDeps, linkCodes: Flow<String>, onDismiss: () -> Unit, onLinked: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val scope = rememberCoroutineScope()
    // El codi que arribi per enllaç omple el camp. **No l'envia sol**: vincular vol dir donar
    // accés a dades d'un menor i no pot passar per haver tocat una cosa a WhatsApp.
    LaunchedEffect(linkCodes) { linkCodes.collect { code = it } }

    ParentSheet(onDismiss) {
        ParentRedeemContent(
            code = code, onCodeChange = { code = it }, isWorking = isWorking,
            error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
            onCancel = onDismiss,
            onSubmit = {
                error = null
                isWorking = true
                scope.launch {
                    val linked = parentAttempt({ error = it }) { deps.client.redeem(code, deps.session.accessToken()) }
                    isWorking = false
                    if (linked != null) { onLinked(); onDismiss() }
                }
            },
        )
    }
}
