package cat.vira.android.features.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraTheme
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.util.UUID

/// Les pantalles d'Ajustos senceres, a l'ample d'un iPhone i prou altes per veure-les de dalt a baix.
@Preview(name = "clar", widthDp = 393, heightDp = 1500, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 1500, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 3200, locale = "es", fontScale = 2f)
annotation class SettingsPageCaptures

private val signedIn = ViraSession.State.SignedIn(
    ViraUser(UUID(0, 1), "nora@example.com", consent = ViraConsentStatus.PENDING),
)

private fun backend(session: ViraSession.State = signedIn) = PreviewSettingsBackend(
    sessionState = session,
    sync = ViraSyncEngine.State.Succeeded(Instant.EPOCH),
    name = "Nora",
    // Sense dates: «fa 2 dies» canviaria cada dia i la captura deixaria de coincidir.
    guardian = ViraGuardianRequest(ViraConsentStatus.PENDING, guardian = "ma•••@gmail.com", canResend = true),
    profiles = listOf(
        FocusProfile(name = "Deures", focusMinutes = 30, breakMinutes = 5),
        FocusProfile(name = "Lectura", focusMinutes = 20, breakMinutes = 5, muteViraNotifications = true),
    ),
)

@Composable
private fun Capture(stack: List<SettingsPage> = emptyList(), session: ViraSession.State = signedIn) {
    ViraCapture {
        ViraTheme(reduceMotion = true) {
            SettingsHost(backend(session)) {
                SettingsNavigation(onClose = {}, initialStack = stack)
            }
        }
    }
}

private fun category(category: SettingsCategory) = listOf(SettingsPage.Category(category))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsHome() = Capture()

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsAccount() = Capture(category(SettingsCategory.ACCOUNT))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsNotifications() = Capture(category(SettingsCategory.NOTIFICATIONS))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsApp() = Capture(category(SettingsCategory.APP))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsMotivation() = Capture(category(SettingsCategory.MOTIVATION))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsPrivacy() = Capture(category(SettingsCategory.PRIVACY))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsAbout() = Capture(category(SettingsCategory.ABOUT))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsFocusModes() = Capture(listOf(SettingsPage.Category(SettingsCategory.APP), SettingsPage.FocusModes))

@PreviewTest @SettingsPageCaptures @Composable
fun SettingsParentAccount() = ViraCapture {
    ViraTheme(reduceMotion = true) {
        SettingsHost(backend()) { AccountSettingsContent(onClose = {}) }
    }
}
