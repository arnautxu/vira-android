package cat.vira.android.features.shop

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTouchTarget
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing

/// La barra d'una pantalla empesa de la Botiga (`navigationTitle` en línia de l'iPhone): tornar a
/// l'esquerra, el títol al mig i, a la dreta, el que la pantalla hi vulgui (el saldo).
@Composable
fun ShopNavigationBar(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Today,
    trailing: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs)) {
        if (onBack != null) {
            val back = stringResource(R.string.action_back)
            Box(
                Modifier.align(Alignment.CenterStart).viraTouchTarget()
                    .viraPressable(onClick = onBack).semantics { contentDescription = back },
                contentAlignment = Alignment.Center,
            ) {
                val ink = ViraColor.accent(area)
                Canvas(Modifier.size(20.dp)) {
                    val w = size.width
                    drawLine(ink, Offset(w * 0.65f, w * 0.1f), Offset(w * 0.25f, w * 0.5f), w * 0.13f, StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.25f, w * 0.5f), Offset(w * 0.65f, w * 0.9f), w * 0.13f, StrokeCap.Round)
                }
            }
        }
        if (title != null) {
            Text(
                title, style = ViraFont.headline, color = ViraColor.textPrimary, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp).semantics { heading() },
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) { trailing() }
    }
}

/// Les cartes encara no poden sortir en una build pública (`ViraReleaseGate.cards`): obertes en
/// proves, tancades en Release.
fun cardsEnabled(context: Context): Boolean =
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

@Composable
fun rememberCardsEnabled(): Boolean {
    val context = LocalContext.current
    return remember { cardsEnabled(context) }
}

/// «Celebracions» d'Ajustos: apagades, no hi ha revelació ni confeti.
@Composable
fun rememberCelebrationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember { ViraPreferences.open(context).getBoolean(ViraPreferences.CELEBRATIONS_ENABLED, true) }
}
