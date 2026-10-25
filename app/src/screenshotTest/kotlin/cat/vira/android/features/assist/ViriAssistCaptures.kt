package cat.vira.android.features.assist

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.tokens.ViraColor
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDateTime
import java.time.ZoneId

/// Els fulls de les ajudes de Viri a l'ample d'un iPhone (393 dp).
@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 2400, locale = "es", fontScale = 2f)
annotation class ViriAssistCaptures

private val item = PlanItem(
    title = "Treball de socials",
    symbolName = "picto-plan-write",
    day = LocalDateTime.of(2026, 10, 20, 0, 0).atZone(ZoneId.of("Europe/Madrid")).toInstant(),
)

@Composable
private fun Sheet(content: @Composable () -> Unit) = ViraCapture {
    ViraTheme(reduceMotion = true) {
        Box(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped)) { content() }
    }
}

/// «Viri pensa amb Claude», la primera vegada.
@PreviewTest @ViriAssistCaptures @Composable
fun ViriAssistConsent() = Sheet { ViriAssistConsentContent(onAccept = {}, onLater = {}) }

/// Els passos que ha proposat Viri, un de marcat.
@PreviewTest @ViriAssistCaptures @Composable
fun ViriAssistSteps() = Sheet {
    val scope = rememberCoroutineScope()
    val flow = remember {
        ViriAssistStepsFlow(scope, { emptyList() }, commit = null).also {
            it.steps += AssistanceStep(text = "Triar el tema i mirar què demana la profe", isSelected = true, minutes = 10)
            it.steps += AssistanceStep(text = "Buscar tres fonts", minutes = 20)
            it.steps += AssistanceStep(text = "Fer l'esquema", minutes = 15)
        }
    }
    ViriAssistStepsContent(item, item, flow, onClose = {})
}

/// Mentre Viri pensa.
@PreviewTest @ViriAssistCaptures @Composable
fun ViriAssistThinking() = Sheet {
    val scope = rememberCoroutineScope()
    val flow = remember {
        ViriAssistStepsFlow(scope, { kotlinx.coroutines.awaitCancellation() }, commit = null).also { it.generate(item.title) }
    }
    ViriAssistStepsContent(item, item, flow, onClose = {})
}
