package cat.vira.android.features.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.data.ViraTime
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/// Curt a propòsit. Un missatge d'ànim de dos-cents caràcters ja no és un missatge d'ànim.
internal fun ParentComposeMode.limit(): Int = if (this == ParentComposeMode.MESSAGE) 200 else 120

internal fun isValidCompose(text: String, mode: ParentComposeMode): Boolean =
    text.trim().let { it.isNotEmpty() && it.length <= mode.limit() }

/// Enviar un missatge d'ànim o proposar un recordatori (`ParentComposeView`).
///
/// Les dues coses caben en un full perquè són el mateix gest: escriure poc i enviar-ho. El que
/// canvia és on va a parar — el missatge arriba tal qual; el recordatori es queda esperant que el
/// fill/a el vulgui.
@Composable
internal fun ParentComposeSheet(mode: ParentComposeMode, childId: UUID, deps: ParentDeps, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var day by remember { mutableStateOf<Instant?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    val scope = rememberCoroutineScope()

    ParentSheet(onDismiss) {
        ParentComposeContent(
            mode = mode, text = text, onTextChange = { text = it },
            day = day, onDayChange = { day = it },
            isWorking = isWorking, error = error?.let { stringResource(it.message()) },
            onCancel = onDismiss,
            onSend = {
                if (!isValidCompose(text, mode)) return@ParentComposeContent
                error = null
                isWorking = true
                scope.launch {
                    val sent = parentAttempt({ error = it }) {
                        val token = deps.session.accessToken()
                        when (mode) {
                            ParentComposeMode.MESSAGE -> deps.client.send(text.trim(), childId, token)
                            ParentComposeMode.REMINDER -> deps.client.propose(text.trim(), day, childId, token)
                        }
                    }
                    isWorking = false
                    if (sent != null) onDismiss()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentComposeContent(
    mode: ParentComposeMode,
    text: String,
    onTextChange: (String) -> Unit,
    day: Instant?,
    onDayChange: (Instant?) -> Unit,
    isWorking: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    val isMessage = mode == ParentComposeMode.MESSAGE
    var picking by remember { mutableStateOf(false) }
    val locale = currentLocale()

    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        ParentSheetBar(
            title = stringResource(if (isMessage) R.string.parent_compose_message else R.string.parent_compose_reminder),
            onCancel = onCancel,
            confirm = stringResource(R.string.parent_compose_send),
            confirmEnabled = isValidCompose(text, mode) && !isWorking,
            onConfirm = onSend,
        )
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            ParentTextField(
                value = text,
                onChange = onTextChange,
                placeholder = stringResource(if (isMessage) R.string.parent_compose_message_field else R.string.parent_compose_reminder_field),
                minLines = 2,
                maxLines = 5,
            )
            if (!isMessage) {
                ParentGroup(rows = {
                    row {
                        ParentToggleRow(stringResource(R.string.parent_compose_day), day != null, { on ->
                            onDayChange(if (on) startOfDay(ViraTime.now()) else null)
                        })
                    }
                    if (day != null) {
                        row {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.parent_compose_day), style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f))
                                ViraActionButton(longDate(day, locale), { picking = true }, hierarchy = ViraActionHierarchy.Secondary)
                            }
                        }
                    }
                })
            }
            ParentFootnote(stringResource(if (isMessage) R.string.parent_compose_message_footnote else R.string.parent_compose_reminder_footnote))
            if (error != null) ViraInlineError(error)
        }
        if (isWorking) ParentProgress(Modifier.padding(ViraSpacing.m))
    }

    if (picking && day != null) {
        // El selector de Material treballa en dies UTC; el dia triat es torna a l'inici del dia local.
        val utcMillis = day.atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = utcMillis)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                ViraActionButton(stringResource(R.string.action_done), {
                    state.selectedDateMillis?.let { millis ->
                        val local = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDayChange(local.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                    }
                    picking = false
                }, hierarchy = ViraActionHierarchy.Textual)
            },
            dismissButton = {
                ViraActionButton(stringResource(R.string.action_cancel), { picking = false }, hierarchy = ViraActionHierarchy.Textual)
            },
        ) { DatePicker(state) }
    }
}
