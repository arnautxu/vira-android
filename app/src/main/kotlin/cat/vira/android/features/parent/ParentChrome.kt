package cat.vira.android.features.parent

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/// Les peces que comparteixen les pantalles del panell i que no són del sistema de disseny: la
/// barra d'enrere d'una subpantalla (`viraPageChrome`), les seccions agrupades (`List` amb
/// `.insetGrouped`) i el text dels errors.

/// Enrere a la barra, amb el nom de la pantalla d'on es ve («‹ Família»).
@Composable
internal fun ParentBackBar(backLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ink = ViraColor.brandAccent
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = ViraSpacing.xs)
            .defaultMinSize(minHeight = ViraTouch.minimum)
            .viraPressable(onClick = onBack)
            .padding(horizontal = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        val side = 18.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
        Canvas(Modifier.size(side)) {
            val path = Path().apply {
                moveTo(size.width * 0.65f, size.height * 0.12f)
                lineTo(size.width * 0.3f, size.height * 0.5f)
                lineTo(size.width * 0.65f, size.height * 0.88f)
            }
            drawPath(path, ink, style = Stroke(size.width * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Text(backLabel, style = ViraFont.body, color = ink)
    }
}

/// Capçalera de secció: titular fosc, marcat com a capçalera per a TalkBack.
@Composable
internal fun ParentSectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xxs), verticalAlignment = Alignment.Bottom) {
        Text(
            text, style = ViraFont.headline, color = ViraColor.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (trailing != null) Text(trailing, style = ViraFont.footnote, color = ViraColor.textSecondary)
    }
}

@Composable
internal fun ParentFootnote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ViraFont.footnote, color = ViraColor.textSecondary, modifier = modifier.padding(horizontal = ViraSpacing.xxs))
}

/// Una secció agrupada: capçalera, targeta blanca amb les files separades i peu.
@Composable
internal fun ParentGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    rows: ParentRows.() -> Unit,
) {
    val items = ParentRows().apply(rows).items
    val shape = RoundedCornerShape(ViraRadius.m)
    val contrast = LocalViraEnvironment.current.increasedContrast
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        if (header != null) ParentSectionHeader(header)
        Column(
            Modifier
                .fillMaxWidth()
                .background(ViraColor.surfaceElevated, shape)
                .then(if (contrast) Modifier.border(2.dp, ViraColor.textPrimary, shape) else Modifier),
        ) {
            items.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.separator)
                Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum).padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)) { row() }
            }
        }
        if (footer != null) ParentFootnote(footer)
    }
}

/// Les files d'una secció agrupada, en ordre.
internal class ParentRows {
    val items = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { items += content }
}

/// Interruptor en una fila: tota la fila es toca, i TalkBack diu el nom i l'estat.
@Composable
internal fun ParentToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ViraFont.body, color = ViraColor.textPrimary, modifier = Modifier.weight(1f).padding(end = ViraSpacing.s))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ViraColor.brandAccent,
                checkedThumbColor = ViraColor.onAccent,
                uncheckedTrackColor = ViraColor.separator,
                uncheckedThumbColor = ViraColor.surfaceElevated,
                uncheckedBorderColor = ViraColor.controlBorder,
            ),
        )
    }
}

/// Indicador d'«un moment» al centre, sobre el contingut.
@Composable
internal fun ParentProgress(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ViraColor.brandAccent)
    }
}

/// Indicador petit dins d'una fila, mentre la llista encara no ha arribat.
@Composable
internal fun ParentProgressInline() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ViraColor.brandAccent, modifier = Modifier.size(ViraTouch.minimum / 2))
    }
}

/// El text de cada error, del catàleg (`backend.error.*`).
@StringRes
internal fun ViraBackendError.message(): Int = when (this) {
    ViraBackendError.NOT_CONFIGURED -> R.string.backend_error_not_configured
    ViraBackendError.OFFLINE -> R.string.backend_error_offline
    ViraBackendError.INVALID_CREDENTIALS -> R.string.backend_error_credentials
    ViraBackendError.EMAIL_ALREADY_REGISTERED -> R.string.backend_error_email_taken
    ViraBackendError.WEAK_PASSWORD -> R.string.backend_error_weak_password
    ViraBackendError.INVALID_EMAIL -> R.string.backend_error_invalid_email
    ViraBackendError.EMAIL_NOT_CONFIRMED -> R.string.backend_error_email_not_confirmed
    ViraBackendError.TOO_MANY_ATTEMPTS -> R.string.backend_error_too_many_attempts
    ViraBackendError.SESSION_EXPIRED -> R.string.backend_error_session_expired
    ViraBackendError.NOT_ALLOWED -> R.string.backend_error_not_allowed
    ViraBackendError.CANCELLED -> R.string.backend_error_cancelled
    ViraBackendError.REJECTED -> R.string.backend_error_rejected
    ViraBackendError.GUARDIAN_IS_YOU -> R.string.backend_error_guardian_is_you
    ViraBackendError.UNAVAILABLE -> R.string.backend_error_unavailable
}

/// Fa la crida i, si falla, en dona l'error de la interfície. Qualsevol cosa que no sigui del
/// servidor es diu «no està disponible», com l'iPhone.
internal suspend fun <T> parentAttempt(onError: (ViraBackendError) -> Unit, block: suspend () -> T): T? = try {
    block()
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: ViraBackendException) {
    onError(failure.error); null
} catch (_: Exception) {
    onError(ViraBackendError.UNAVAILABLE); null
}

/// «12 de setembre» (`.dateTime.day().month(.wide)`).
internal fun dayMonth(instant: Instant, locale: Locale): String =
    android.icu.text.DateFormat.getInstanceForSkeleton("dMMMM", locale).format(Date.from(instant))

/// La data llarga d'iOS (`Text(date, style: .date)`).
internal fun longDate(instant: Instant, locale: Locale): String =
    android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.LONG, locale).format(Date.from(instant))

/// «1.240» (`.formatted()`).
internal fun number(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

internal fun startOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
    instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
