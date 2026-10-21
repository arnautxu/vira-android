package cat.vira.android.features.settings

import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.resources.ViraLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/// El permís del tutor, mentre falta (`SettingsGuardianSection.swift`, Arnau, 2026-10-20): a qui
/// s'ha demanat, quan va sortir el correu i quan caduca, amb «Torna'l a enviar» i «Canvia el
/// correu». Quan la persona adulta diu que sí, la secció desapareix sola.
///
/// Si diu que no, aquí no hi ha cap retret ni cap Viri: només «Permís no donat», què vol dir, i
/// «Demana-ho a una altra persona». L'app segueix igual; només emocions i medicació es queden al
/// telèfon, com abans de demanar-ho.
@Composable
fun SettingsGuardianSection() {
    val backend = LocalSettingsBackend.current
    val user = rememberSettingsUser() ?: return
    if (user.role != ViraAccountRole.TEEN || user.consent == ViraConsentStatus.GRANTED) return
    val consent = user.consent
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var summary by remember { mutableStateOf(backend.initialGuardianSummary) }
    var isLoading by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var note by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<ViraBackendError?>(null) }
    var attempt by remember { mutableStateOf(0) }
    val sentAgain = stringResource(R.string.account_consent_sent_again)

    LaunchedEffect(consent, attempt) {
        isLoading = true
        try {
            summary = backend.guardianConsentSummary()
            error = null
        } catch (failure: CancellationException) {
            // Cancel·lada en sortir de la pantalla: no hi ha res a dir.
            throw failure
        } catch (failure: ViraBackendException) {
            error = failure.error
        } catch (_: Exception) {
            error = ViraBackendError.UNAVAILABLE
        } finally {
            isLoading = false
        }
    }

    fun send(to: String?) {
        if (isSending) return
        isSending = true
        error = null
        note = null
        scope.launch {
            try {
                val result = backend.requestGuardianConsent(to)
                summary = result
                isEditing = false
                note = when (result.delivery) {
                    ViraGuardianRequest.Delivery.SENT -> R.string.account_consent_sent_again
                    ViraGuardianRequest.Delivery.LIMIT_REACHED -> R.string.settings_consent_limit
                    ViraGuardianRequest.Delivery.NOT_SENT -> null
                }
                if (result.delivery == ViraGuardianRequest.Delivery.SENT) view.announceForAccessibility(sentAgain)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: ViraBackendException) {
                error = failure.error
            } catch (_: Exception) {
                error = ViraBackendError.UNAVAILABLE
            }
            isSending = false
        }
    }

    val loading = stringResource(R.string.account_loading)
    val label = stringResource(R.string.account_consent_email_label)
    SettingsGroup(
        header = stringResource(R.string.settings_consent_section),
        footer = stringResource(R.string.account_consent_footnote),
    ) {
        when {
            isLoading && summary == null -> row { SettingsProgressRow(loading) }
            summary == null && error != null -> row { SettingsActionRow(stringResource(R.string.action_retry), { attempt += 1 }) }
            else -> {
                row { GuardianStatusRow(consent, summary) }
                if (isEditing || consent == ViraConsentStatus.MISSING) {
                    row {
                        // Etiqueta visible a sobre: el text de dins del camp desapareix en escriure.
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
                        ) {
                            Text(label, style = ViraFont.footnote, color = ViraColor.textSecondary)
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text(label, style = ViraFont.body) },
                                singleLine = true,
                                textStyle = ViraFont.body,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum),
                            )
                        }
                    }
                    row {
                        SettingsActionRow(
                            stringResource(R.string.account_consent_submit),
                            { send(email.trim()) },
                            enabled = email.contains("@") && !isSending,
                        )
                    }
                } else if (consent == ViraConsentStatus.DECLINED) {
                    row { SettingsActionRow(stringResource(R.string.settings_consent_ask_another), { isEditing = true }) }
                } else {
                    row {
                        SettingsActionRow(
                            stringResource(R.string.account_consent_sent_resend),
                            { send(null) },
                            enabled = summary?.canResend == true && !isSending,
                        )
                    }
                    row { SettingsActionRow(stringResource(R.string.account_consent_sent_change), { isEditing = true }) }
                }
            }
        }
        if (isSending) row { SettingsProgressRow(loading) }
        note?.let { row { SettingsNoteRow(stringResource(it)) } }
        error?.takeIf { summary != null }?.let { row { SettingsNoteRow(backendErrorMessage(it), color = ViraColor.error) } }
    }
}

/// La fila de dalt: el pictograma de l'escut, què passa i, si n'hi ha, les dates. TalkBack la llegeix
/// d'una tirada.
@Composable
private fun GuardianStatusRow(consent: ViraConsentStatus, summary: ViraGuardianRequest?) {
    val context = LocalContext.current
    val now = Instant.now()
    fun relative(instant: Instant) = relativeDate(instant, now, ViraLanguage.current(context))
    val lines = buildList {
        when (consent) {
            ViraConsentStatus.DECLINED -> {
                add(stringResource(R.string.settings_account_consent_declined))
                add(stringResource(R.string.settings_consent_declined_hint))
            }
            ViraConsentStatus.MISSING -> add(stringResource(R.string.settings_consent_missing_title))
            else -> {
                add(stringResource(R.string.settings_consent_pending_title, summary?.guardian.orEmpty()))
                summary?.sentAt?.let { add(stringResource(R.string.settings_consent_sent, relative(it))) }
                // Un enllaç de més de set dies encara surt al resum amb la data: ja ha caducat.
                val expiresAt = summary?.expiresAt
                if (expiresAt != null && expiresAt.isAfter(now)) {
                    add(stringResource(R.string.settings_consent_expires, relative(expiresAt)))
                } else if (summary != null) {
                    add(stringResource(R.string.settings_consent_expired))
                }
                if (summary?.canResend == false) add(stringResource(R.string.settings_consent_limit))
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(R.drawable.picto_shield, size = 32.dp, scalesWithText = true)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            lines.forEachIndexed { index, line ->
                if (index == 0) Text(line, style = ViraFont.body, color = ViraColor.textPrimary)
                else Text(line, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
        }
    }
}

@Composable
private fun SettingsProgressRow(label: String) {
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = ViraColor.textSecondary, modifier = Modifier.size(24.dp))
    }
}

/// «fa 2 dies», «d'aquí a 5 dies», «ahir»: la data amb paraules, en l'idioma de l'app
/// (`.relative(presentation: .named)` de l'iPhone).
internal fun relativeDate(instant: Instant, now: Instant, language: String): String {
    val formatter = RelativeDateTimeFormatter.getInstance(ULocale.forLanguageTag(language))
    val seconds = Duration.between(now, instant).seconds
    val direction = if (seconds < 0) RelativeDateTimeFormatter.Direction.LAST else RelativeDateTimeFormatter.Direction.NEXT
    val magnitude = kotlin.math.abs(seconds)
    val days = magnitude / 86_400
    val hours = magnitude / 3_600
    val minutes = magnitude / 60
    return when {
        days == 1L -> formatter.format(direction, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
        days > 1 -> formatter.format(days.toDouble(), direction, RelativeDateTimeFormatter.RelativeUnit.DAYS)
        hours >= 1 -> formatter.format(hours.toDouble(), direction, RelativeDateTimeFormatter.RelativeUnit.HOURS)
        minutes >= 1 -> formatter.format(minutes.toDouble(), direction, RelativeDateTimeFormatter.RelativeUnit.MINUTES)
        else -> formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
    }
}
