package cat.vira.android.features.entry

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.tokens.ViraColor
import com.android.tools.screenshot.PreviewTest
import java.util.UUID

/// Les captures del flux d'entrada: cada pas amb tot l'escenari, en clar, fosc i lletra al 200 %.
/// Els serveis són els de mostra (`EntryServices.preview`): ni xarxa ni base de dades.

private object NoResults : ActivityResultRegistry() {
    override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
}

private object StillOwner : LifecycleOwner, ActivityResultRegistryOwner, OnBackPressedDispatcherOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    override val lifecycle: Lifecycle get() = registry
    override val activityResultRegistry: ActivityResultRegistry get() = NoResults
    override val onBackPressedDispatcher = OnBackPressedDispatcher()
}

private val teen = ViraUser(UUID(0, 1), "algu@exemple.test", ViraAccountRole.TEEN, ViraConsentStatus.MISSING)

@Composable
private fun Capture(
    step: EntryStep,
    steps: List<EntryStep> = EntryStep.full,
    user: ViraUser? = null,
    values: Map<String, Any> = mapOf(EntryPreferenceKeys.ENTRY_START_INTENT to "organize", EntryPreferenceKeys.AGE_RANGE to "12-15"),
    outcome: EntryFirstActionOutcome? = null,
) = Owners {
    val services = EntryServices.preview(requiresAccount = true, user = user, prefs = EntryPreferences.inMemory(values))
    EntryFlow(steps = steps, onFinish = {}, services = services, initialStep = step, initialOutcome = outcome)
}

@Composable
private fun Owners(content: @Composable () -> Unit) = ViraTheme(reduceMotion = true) {
    CompositionLocalProvider(
        LocalLifecycleOwner provides StillOwner,
        LocalActivityResultRegistryOwner provides StillOwner,
        LocalOnBackPressedDispatcherOwner provides StillOwner,
        content = content,
    )
}

@PreviewTest @ViraScreenCaptures @Composable
fun EntryWelcome() = Capture(EntryStep.WELCOME, values = emptyMap())

@PreviewTest @ViraScreenCaptures @Composable
fun EntryStart() = Capture(EntryStep.START)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryAge() = Capture(EntryStep.AGE)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryPrivacy() = Capture(EntryStep.PRIVACY)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryFirstTask() = Capture(EntryStep.FIRST_ACTION)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryFirstFocus() = Capture(EntryStep.FIRST_ACTION, values = mapOf(EntryPreferenceKeys.ENTRY_START_INTENT to "focus"))

@PreviewTest @ViraScreenCaptures @Composable
fun EntryDone() = Capture(EntryStep.DONE, outcome = EntryFirstActionOutcome.Task("Deures de mates"))

@PreviewTest @ViraScreenCaptures @Composable
fun EntryAccount() = Capture(EntryStep.ACCOUNT)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryPasswordReset() = Capture(EntryStep.PASSWORD_RESET)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNewPassword() = Capture(EntryStep.NEW_PASSWORD, steps = listOf(EntryStep.NEW_PASSWORD), user = teen)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryConsent() = Capture(EntryStep.CONSENT, user = teen)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNickname() = Capture(EntryStep.NICKNAME, user = teen.copy(consent = ViraConsentStatus.PENDING))

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNotifications() = Capture(
    EntryStep.NOTIFICATIONS,
    user = teen.copy(consent = ViraConsentStatus.PENDING),
    outcome = EntryFirstActionOutcome.Task("Deures de mates"),
)

@Composable
private fun Tutorial(step: ViraTutorialStep) = Owners {
    val density = LocalDensity.current
    // Un forat de mostra a l'alçada de la barra de pestanyes, sobre la segona icona.
    val spotlight = Rect(110f * density.density, 770f * density.density, 170f * density.density, 822f * density.density)
    Box(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped)) {
        ViraTutorialOverlay(
            step = step,
            spotlight = if (step.tab == null) null else spotlight,
            prefs = EntryPreferences.inMemory(),
            onNext = {},
            onSkip = {},
        )
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialPlan() = Tutorial(ViraTutorialStep.PLAN)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialIntegrations() = Tutorial(ViraTutorialStep.INTEGRATIONS)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialWidgets() = Tutorial(ViraTutorialStep.WIDGETS)
