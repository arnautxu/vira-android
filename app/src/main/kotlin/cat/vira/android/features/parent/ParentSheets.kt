package cat.vira.android.features.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Un full que puja des de baix (`.sheet` de l'iPhone), obert del tot.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ViraColor.background,
        content = content,
    )
}

/// La fila de dalt d'un full: «Cancel·la» a l'esquerra, el títol al mig i, si n'hi ha, l'acció
/// principal a la dreta (`.toolbar` amb `cancellationAction` i `confirmationAction`).
@Composable
internal fun ParentSheetBar(title: String, onCancel: () -> Unit, confirm: String? = null, confirmEnabled: Boolean = true, onConfirm: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = ViraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        ViraActionButton(stringResource(R.string.action_cancel), onCancel, hierarchy = ViraActionHierarchy.Textual)
        Text(
            title, style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (confirm != null) {
            ViraActionButton(confirm, onConfirm, hierarchy = ViraActionHierarchy.Textual, enabled = confirmEnabled)
        } else {
            // El títol queda centrat igualment.
            Box(Modifier.defaultMinSize(minWidth = ViraTouch.minimum * 2))
        }
    }
}

/// El pare/mare introdueix el codi que el fill/a li ha donat (`ParentLinkRedeemView`).
///
/// El codi surt del telèfon del menor i dura quinze minuts: no hi ha manera de vincular ningú sense
/// que ell ho faci en aquell moment. El que arriba per enllaç només **omple** el camp: prémer
/// «Vincula» segueix sent de qui vincula.
@Composable
internal fun ParentRedeemContent(
    code: String,
    onCodeChange: (String) -> Unit,
    isWorking: Boolean,
    error: String?,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val fieldLabel = stringResource(R.string.parent_redeem_field)
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        ParentSheetBar(stringResource(R.string.parent_redeem_title), onCancel)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(ViraSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
        ) {
            Text(stringResource(R.string.parent_redeem_message), style = ViraFont.body, color = ViraColor.textSecondary)
            ParentTextField(
                value = code,
                onChange = onCodeChange,
                placeholder = fieldLabel,
                style = ViraFont.title.copy(textAlign = TextAlign.Center),
                keyboard = KeyboardType.Number,
                singleLine = true,
                modifier = Modifier.semantics { contentDescription = fieldLabel },
            )
            ViraActionButton(
                stringResource(R.string.parent_redeem_submit), onSubmit, Modifier.fillMaxWidth(),
                enabled = code.length == 6 && !isWorking,
            )
            if (error != null) ViraInlineError(error)
        }
        if (isWorking) ParentProgress(Modifier.padding(ViraSpacing.m))
    }
}

/// Camp de text de Vira: contorn arrodonit amb el color dels controls, sense el subratllat de
/// Material.
@Composable
internal fun ParentTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ViraFont.body,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
) {
    val shape = RoundedCornerShape(ViraRadius.m)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style.copy(color = ViraColor.textPrimary),
        cursorBrush = SolidColor(ViraColor.brandAccent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else maxLines,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ViraTouch.comfortable)
                    .background(ViraColor.surfaceElevated, shape)
                    .border(1.5.dp, ViraColor.controlBorder, shape)
                    .padding(ViraSpacing.m),
                contentAlignment = if (singleLine) Alignment.Center else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = style, color = ViraColor.textSecondary, modifier = Modifier.fillMaxWidth())
                inner()
            }
        },
    )
}
