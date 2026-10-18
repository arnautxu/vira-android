package cat.vira.android.features.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/// El que un pare o mare t'ha enviat (`ParentPanelInboxView` de l'iPhone).
///
/// Missatges i recordatoris proposats, en dues seccions. Cap dels dos entra enlloc sense que tu
/// ho diguis: el recordatori es converteix en una tasca del teu pla només si el vols. No s'hi pot
/// respondre: és un sol sentit.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentPanelInboxScreen(deps: ParentDeps, onBack: () -> Unit) {
    var messages by remember { mutableStateOf<List<ParentMessage>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<ParentReminder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var remindersLoaded by remember { mutableStateOf(false) }
    var messagesLoaded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf<java.util.UUID?>(null) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        parentAttempt({ error = it }) {
            val token = deps.session.accessToken()
            messages = deps.client.messages(token)
            messagesLoaded = true
            reminders = deps.client.reminders(token)
            remindersLoaded = true
            // Marcar-los com a llegits en obrir la llista: no hi ha cap acció que ho hagi de fer a
            // mà, i el pare/mare només en veu la data, no si els has obert un per un.
            // A la vegada i no d'un en un: amb molts sense llegir, la llista no es pot quedar
            // carregant esperant cada escriptura.
            coroutineScope {
                messages.filter { it.readAt == null }
                    .map { async { runCatching { deps.client.markRead(it, token) } } }
                    .awaitAll()
            }
        }
        isLoading = false
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) {
        ParentBackBar(stringResource(R.string.settings_parent_panel_title), onBack)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { isRefreshing = true; load(); isRefreshing = false } },
            modifier = Modifier.weight(1f),
        ) {
            ParentPanelInboxContent(
                messages = messages,
                reminders = reminders,
                remindersLoaded = remindersLoaded,
                messagesLoaded = messagesLoaded,
                error = error?.takeIf { it != ViraBackendError.CANCELLED }?.let { stringResource(it.message()) },
                onResolve = resolve@{ reminder, accepted ->
                    // Un recordatori a la vegada: dos tocs ràpids a «Accepta» no han de crear dues tasques.
                    if (resolving != null) return@resolve
                    resolving = reminder.id
                    scope.launch {
                        // Acceptar-lo el converteix en una tasca teva, igual que si l'haguessis
                        // apuntat tu. A partir d'aquí ja no és de ningú altre.
                        // Primer la tasca i després el servidor, i la tasca porta l'identificador del
                        // recordatori: si es talla entre els dos passos, tornar a acceptar-lo troba la
                        // mateixa tasca i no en fa una altra; i un recordatori ja tancat al servidor
                        // sempre té la seva tasca.
                        if (accepted && deps.store.db.planItems().byId(reminder.id) == null) {
                            val day = startOfDay(reminder.day ?: ViraTime.now())
                            deps.store.create(deps.store.db.planItems(), PlanItem(id = reminder.id, title = reminder.title, day = day))
                        }
                        val resolved = parentAttempt({ error = it }) { deps.client.resolve(reminder, accepted, deps.session.accessToken()) }
                        // Un «Accepta» que no va arribar al servidor i després un «Descarta»: la tasca
                        // d'aquell primer intent se'n va, perquè el recordatori ha quedat descartat.
                        if (resolved != null && !accepted) {
                            deps.store.db.planItems().byId(reminder.id)?.let { deps.store.delete(it) }
                        }
                        // Si ha fallat, l'error es queda a la vista: tornar a carregar l'esborraria.
                        if (resolved != null) load()
                        resolving = null
                    }
                },
            )
            if (isLoading && messages.isEmpty() && reminders.isEmpty()) ParentProgress()
        }
    }
}

/// Sense estat, per a les captures.
@Composable
internal fun ParentPanelInboxContent(
    messages: List<ParentMessage>,
    reminders: List<ParentReminder>,
    error: String?,
    onResolve: (ParentReminder, Boolean) -> Unit,
    remindersLoaded: Boolean = true,
    messagesLoaded: Boolean = true,
) {
    val locale = viraLocale()
    val pending = reminders.filter { it.isPending }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = ViraSpacing.majorSectionGap),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        ViraPageTitle(stringResource(R.string.parent_inbox_title), ViraArea.Today)

        ParentGroup(
            header = stringResource(R.string.parent_inbox_reminders),
            rows = {
                // «Cap» només quan el servidor ho ha dit; abans, o amb un error, no se sap.
                if (pending.isEmpty() && !remindersLoaded) {
                    row { ParentProgressInline() }
                } else if (pending.isEmpty()) {
                    row { Text(stringResource(R.string.parent_inbox_reminders_empty), style = ViraFont.footnote, color = ViraColor.textSecondary) }
                }
                pending.forEach { reminder ->
                    row {
                        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                            Text(reminder.title, style = ViraFont.body, color = ViraColor.textPrimary)
                            reminder.day?.let { Text(longDate(it, locale), style = ViraFont.footnote, color = ViraColor.textSecondary) }
                            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                                ViraActionButton(stringResource(R.string.parent_inbox_accept), { onResolve(reminder, true) })
                                ViraActionButton(stringResource(R.string.parent_inbox_dismiss), { onResolve(reminder, false) }, hierarchy = ViraActionHierarchy.Secondary)
                            }
                        }
                    }
                }
            },
        )

        ParentGroup(
            header = stringResource(R.string.parent_inbox_messages),
            footer = stringResource(R.string.parent_inbox_footnote),
            rows = {
                if (messages.isEmpty() && !messagesLoaded) {
                    row { ParentProgressInline() }
                } else if (messages.isEmpty()) {
                    row { Text(stringResource(R.string.parent_inbox_messages_empty), style = ViraFont.footnote, color = ViraColor.textSecondary) }
                }
                messages.forEach { message ->
                    row {
                        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
                            Text(message.body, style = ViraFont.body, color = ViraColor.textPrimary)
                            Text(longDate(message.createdAt, locale), style = ViraFont.footnote, color = ViraColor.textSecondary)
                        }
                    }
                }
            },
        )

        if (error != null) ViraInlineError(error)
    }
}
