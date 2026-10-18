package cat.vira.android.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.ViraProfileSummary
import com.android.tools.screenshot.PreviewTest

/// L'arrel sola: capçalera, pestanya i barra, a l'ample i l'alt d'un iPhone (393 × 852 dp).
@Preview(name = "clar", widthDp = 393, heightDp = 852, locale = "ca")
@Preview(name = "fosc", widthDp = 393, heightDp = 852, locale = "ca", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "lletra-gran", widthDp = 393, heightDp = 852, locale = "es", fontScale = 2f)
annotation class RootCaptures

@Composable
private fun Root(tab: ViraTab) {
    ViraTheme(reduceMotion = true) {
        ViraRootLayout(
            selectedTab = tab,
            profile = ViraProfileSummary(name = "Nil", coins = 120, streakDays = 4),
            onTabTap = {}, onFocus = {}, onProfile = {}, onSettings = {}, onHelp = {},
        ) { PendingScreen() }
    }
}

@PreviewTest @RootCaptures @Composable fun RootToday() = Root(ViraTab.Today)
@PreviewTest @RootCaptures @Composable fun RootPlan() = Root(ViraTab.Plan)

/// La pantalla del bloqueig de l'app (Ajustos → Privacitat), neutra i sense Viri.
@PreviewTest @RootCaptures @Composable fun AppLock() = ViraTheme(reduceMotion = true) { ViraLockScreen(onUnlock = {}) }
