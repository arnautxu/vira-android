package cat.vira.android.features.help

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.design.ViraTheme
import com.android.tools.screenshot.PreviewTest

/// Captures d'Ajuda ara al costat de les de l'iPhone: clar, fosc i lletra al 200 %, a l'ample d'un
/// iPhone (393 dp). Alçada de sobres perquè la pàgina sencera hi càpiga sense lliscar.
@Preview(name = "clar", widthDp = 393, heightDp = 1300, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 1300, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 3200, locale = "es", fontScale = 2f)
annotation class HelpScreenCaptures

@Composable
private fun Frame(content: @Composable () -> Unit) {
    ViraTheme(reduceMotion = true) { Box(Modifier.fillMaxSize()) { content() } }
}

@PreviewTest @HelpScreenCaptures @Composable
fun HelpHome() = Frame { HelpScreen(onClose = {}) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpBreathe() = Frame { HelpScreen(onClose = {}, initialDestination = HelpDestination.Breathe) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpContact() = Frame { HelpScreen(onClose = {}, initialDestination = HelpDestination.Contact) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpGroundingFirstStep() = Frame { HelpGroundingContent(onBack = {}, onCheckIn = {}) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpGroundingThirdStep() = Frame {
    HelpGroundingContent(onBack = {}, onCheckIn = {}, initialStep = 2, initialAnswers = listOf("La finestra, el llum, la tassa", ""))
}

@PreviewTest @HelpScreenCaptures @Composable
fun HelpGroundingFinished() = Frame { HelpGroundingContent(onBack = {}, onCheckIn = {}, initialStep = 5) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpWrite() = Frame { HelpWriteContent(onBack = {}, onCheckIn = {}) }

@PreviewTest @HelpScreenCaptures @Composable
fun HelpPhones() = Frame { HelpScreen(onClose = {}, initialDestination = HelpDestination.Phones) }
