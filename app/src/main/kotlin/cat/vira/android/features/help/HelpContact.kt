package cat.vira.android.features.help

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Contacte de confiança (`HelpContactView`, `USER_FLOWS.md` F21/F23): sense demanar accés a la
/// llibreta ni desar-hi res. El selector de contactes del sistema no necessita permís perquè
/// l'usuari tria explícitament, i només dona el telèfon triat. Vira no guarda cap contacte ni
/// conversa: només obre Missatges amb qui triïs.
///
/// La pantalla es veu primer i el selector s'obre amb el botó (Arnau, 2026-10-10): hi ha la nota
/// que Vira no en desa res, i és el moment de llegir-la, abans de triar ningú.
@Composable
fun HelpContactContent(onBack: () -> Unit) {
    val context = LocalContext.current
    // Les captures no tenen on tornar el resultat: allà el botó no fa res.
    val picker = if (LocalActivityResultRegistryOwner.current != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                pickedNumber(context, uri)?.let { openMessages(context, it) }
            }
        }
    } else {
        null
    }

    HelpPage(stringResource(R.string.help_contact_title), onBack = onBack) {
        ContactCard()
        ViraActionButton(
            stringResource(R.string.help_contact_pick),
            onClick = {
                // Només els contactes amb telèfon, i la fitxa limitada al número.
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                runCatching { picker?.launch(intent) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        HelpInfoRow(
            R.drawable.picto_shield,
            stringResource(R.string.help_contact_private),
            detail = stringResource(R.string.help_contact_private_detail),
        )
    }
}

/// Viri amb el telèfon a dalt i el que passarà en una placa blanca a sota: el text mai va sobre el
/// degradat (`DESIGN_RULES.md` §4–§5). Viri no hi parla.
@Composable
private fun ContactCard() {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(ViraGradient.card(ViraArea.Today), shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.viri_phone),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(top = ViraSpacing.s).widthIn(max = 220.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(ViraColor.surfaceElevated)
                .semantics(mergeDescendants = true) {}
                .padding(ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
        ) {
            Text(stringResource(R.string.help_contact_heading), style = ViraFont.title, color = ViraColor.textPrimary)
            Text(stringResource(R.string.help_contact_message), style = ViraFont.body, color = ViraColor.textSecondary)
        }
    }
}

/// El telèfon de la fila triada. El selector dona permís de lectura només sobre aquella fila.
private fun pickedNumber(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

private fun openMessages(context: Context, phoneNumber: String) {
    val digits = smsDigits(phoneNumber)
    if (digits.isEmpty()) return
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$digits")))
    } catch (_: ActivityNotFoundException) {
        // Sense app de missatges no hi ha res a obrir; la pantalla es queda com estava.
    }
}
