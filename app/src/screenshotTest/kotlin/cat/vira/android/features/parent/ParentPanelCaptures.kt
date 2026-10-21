package cat.vira.android.features.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.util.UUID

/// Captures del panell de pares (clar, fosc i lletra al 200 %), per posar-les al costat de les de
/// l'iPhone. Cap dada real: identificadors fixos i els noms dels previews de l'iPhone.

private val created = Instant.parse("2026-09-12T10:00:00Z")
private val marc = UUID.fromString("00000000-0000-4000-8000-00000000000a")
private val jana = UUID.fromString("00000000-0000-4000-8000-00000000000b")

private val openLink = ParentLink(UUID.fromString("00000000-0000-4000-8000-000000000001"), marc, UUID.randomUUID(), true, true, true, false, created)
private val closedLink = ParentLink(UUID.fromString("00000000-0000-4000-8000-000000000002"), jana, UUID.randomUUID(), true, false, false, false, Instant.parse("2026-10-18T09:00:00Z"))
private val week = ParentChildSummary("Marc", 1240, 12, 4, 9, 3, 5, null)

@Composable
private fun Screen(content: @Composable () -> Unit) = ViraCapture { ViraTheme(reduceMotion = true) {
    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Today)) { content() }
} }

@PreviewTest @ViraScreenCaptures @Composable
fun ParentRoot() = Screen {
    ParentTopBar(2, onAccount = {}, onLink = {})
    ParentRootContent(listOf(openLink, closedLink), mapOf(marc to "Marc", jana to "Jana"), false, null, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun ParentRootEmpty() = Screen {
    ParentTopBar(0, onAccount = {}, onLink = {})
    ParentRootContent(emptyList(), emptyMap(), false, null, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun ParentChildWeek() = Screen {
    ParentBackBar("Família", {})
    ParentChildContent(openLink, "Marc", week, false, null, ParentChildSection.SUMMARY, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun ParentChildClosed() = Screen {
    ParentBackBar("Família", {})
    ParentChildContent(closedLink, "Jana", week.copy(displayName = "Jana", habitsDoneLastWeek = null, tasksPending = null, emotionCheckinsLastWeek = null), false, null, ParentChildSection.SUMMARY, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun ParentChildSend() = Screen {
    ParentBackBar("Família", {})
    ParentChildContent(openLink, "Marc", week, false, null, ParentChildSection.SEND, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun ParentRedeem() = ViraCapture { ViraTheme(reduceMotion = true) {
    Column(Modifier.fillMaxSize().background(ViraColor.background)) {
        ParentRedeemContent("4829", {}, false, null, {}, {})
    }
} }

@PreviewTest @ViraScreenCaptures @Composable
fun ParentComposeReminder() = ViraCapture { ViraTheme(reduceMotion = true) {
    Column(Modifier.fillMaxSize().background(ViraColor.background)) {
        ParentComposeContent(ParentComposeMode.REMINDER, "Prepara la motxilla", {}, Instant.parse("2026-10-20T00:00:00Z"), {}, false, null, {}, {})
    }
} }

@PreviewTest @ViraScreenCaptures @Composable
fun TeenParentPanel() = Screen {
    ParentBackBar("Ajustos", {})
    ParentPanelContent(listOf(openLink), "482913", false, null, {}, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun TeenParentScopes() = Screen {
    ParentBackBar("Panell de pares", {})
    ParentPanelScopesContent(closedLink, null, {}, {})
}

@PreviewTest @ViraScreenCaptures @Composable
fun TeenParentInbox() = Screen {
    ParentBackBar("Panell de pares", {})
    ParentPanelInboxContent(
        messages = listOf(ParentMessage(UUID.randomUUID(), marc, UUID.randomUUID(), "Molt bé avui! Estic orgullosa de tu.", Instant.parse("2026-10-17T18:00:00Z"))),
        reminders = listOf(ParentReminder(UUID.randomUUID(), marc, UUID.randomUUID(), "Prepara la motxilla", Instant.parse("2026-10-20T00:00:00Z"), Instant.parse("2026-10-17T18:00:00Z"))),
        error = null,
        onResolve = { _, _ -> },
    )
}

