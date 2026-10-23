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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import cat.vira.android.app.ViraTab
import cat.vira.android.core.design.tokens.ViraRadius
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.backend.ViraGuardianRequest
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.design.ViraCapture
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
    receipt: ViraGuardianRequest? = null,
    signingUp: Boolean = false,
) = Owners {
    val services = EntryServices.preview(
        requiresAccount = true,
        user = user,
        prefs = EntryPreferences.inMemory(values),
        guardianReceipt = receipt,
    )
    EntryFlow(steps = steps, onFinish = {}, services = services, initialStep = step, initialOutcome = outcome, initialSigningUp = signingUp)
}

@Composable
private fun Owners(content: @Composable () -> Unit) = ViraCapture { ViraTheme(reduceMotion = true) {
    CompositionLocalProvider(
        LocalLifecycleOwner provides StillOwner,
        LocalActivityResultRegistryOwner provides StillOwner,
        LocalOnBackPressedDispatcherOwner provides StillOwner,
        content = content,
    )
} }

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

/// El jove que ve de «Fet»: «Guarda el que has fet», la fitxa del que es guarda i el proveïdor primer.
@PreviewTest @ViraScreenCaptures @Composable
fun EntryAccountSignUp() = Capture(EntryStep.ACCOUNT, outcome = EntryFirstActionOutcome.Task("Deures de mates"), signingUp = true)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryPasswordReset() = Capture(EntryStep.PASSWORD_RESET)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNewPassword() = Capture(EntryStep.NEW_PASSWORD, steps = listOf(EntryStep.NEW_PASSWORD), user = teen)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryConsent() = Capture(EntryStep.CONSENT, user = teen)

/// Després d'«Envia-li»: a qui ha anat el correu (`EntryConsentSentPanel`).
@PreviewTest @ViraScreenCaptures @Composable
fun EntryConsentSent() = Capture(
    EntryStep.CONSENT,
    user = teen.copy(consent = ViraConsentStatus.PENDING),
    receipt = ViraGuardianRequest(
        ViraConsentStatus.PENDING,
        guardian = "ma•••@gmail.com",
        canResend = true,
        delivery = ViraGuardianRequest.Delivery.SENT,
    ),
)

/// La funció del correu no ha respost: el permís és desat, però la fulla no diu «enviat».
@PreviewTest @ViraScreenCaptures @Composable
fun EntryConsentUnsent() = Capture(
    EntryStep.CONSENT,
    user = teen.copy(consent = ViraConsentStatus.PENDING),
    receipt = ViraGuardianRequest.unsent("mama.garcia@gmail.com"),
)

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNickname() = Capture(EntryStep.NICKNAME, user = teen.copy(consent = ViraConsentStatus.PENDING))

@PreviewTest @ViraScreenCaptures @Composable
fun EntryNotifications() = Capture(
    EntryStep.NOTIFICATIONS,
    user = teen.copy(consent = ViraConsentStatus.PENDING),
    outcome = EntryFirstActionOutcome.Task("Deures de mates"),
)

/// El tomb sobre una pantalla de mostra: cada peça és un bloc a la seva posició, perquè el forat
/// ensenyi què il·lumina. Les posicions són les d'un mòbil de 393 × 852 dp.
private val tutorialFrames: Map<ViraTutorialTarget, DpRect> = mapOf(
    ViraTutorialTarget.HelpNow to DpRect(268.dp, 12.dp, 377.dp, 56.dp),
    ViraTutorialTarget.TodayDay to DpRect(16.dp, 120.dp, 377.dp, 400.dp),
    ViraTutorialTarget.TodayCircle to DpRect(329.dp, 186.dp, 357.dp, 214.dp),
    ViraTutorialTarget.PlanSections to DpRect(16.dp, 72.dp, 377.dp, 120.dp),
    ViraTutorialTarget.PlanAdd to DpRect(321.dp, 136.dp, 369.dp, 184.dp),
    ViraTutorialTarget.EmotionFaces to DpRect(16.dp, 210.dp, 377.dp, 300.dp),
    ViraTutorialTarget.Streak to DpRect(16.dp, 250.dp, 377.dp, 350.dp),
    ViraTutorialTarget.Tab(ViraTab.Plan) to DpRect(86.dp, 784.dp, 150.dp, 832.dp),
    ViraTutorialTarget.Tab(ViraTab.Emotions) to DpRect(244.dp, 784.dp, 308.dp, 832.dp),
    ViraTutorialTarget.Tab(ViraTab.Progress) to DpRect(322.dp, 784.dp, 386.dp, 832.dp),
)

@Composable
private fun Tutorial(step: ViraTutorialStep, route: ViraTutorialRoute = ViraTutorialRoute.REPLAY, tryTitle: String? = null) = Owners {
    val density = LocalDensity.current
    val frames = tutorialFrames.mapValues { (_, it) -> with(density) { Rect(it.left.toPx(), it.top.toPx(), it.right.toPx(), it.bottom.toPx()) } }
    Box(Modifier.fillMaxSize().background(ViraColor.backgroundGrouped)) {
        step.targets.plus(if (tryTitle != null) listOf(ViraTutorialTarget.TodayCircle) else emptyList()).forEach { target ->
            val rect = tutorialFrames[target] ?: return@forEach
            Box(
                Modifier
                    .offset(rect.left, rect.top)
                    .size(rect.right - rect.left, rect.bottom - rect.top)
                    .background(if (target == ViraTutorialTarget.TodayCircle) ViraColor.controlBorder else ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m)),
            )
        }
        ViraTutorialOverlay(
            step = step,
            route = route,
            frames = frames,
            prefs = EntryPreferences.inMemory(),
            onNext = {},
            onSkip = {},
            context = ViraTutorialContext(firstAction = EntryFirstActionKind.TASK, tryTitle = tryTitle),
        )
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialIntro() = Tutorial(ViraTutorialStep.INTRO, ViraTutorialRoute.FIRST)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialToday() = Tutorial(ViraTutorialStep.TODAY, ViraTutorialRoute.FIRST, tryTitle = "Deures de mates")

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialPlan() = Tutorial(ViraTutorialStep.PLAN, ViraTutorialRoute.FIRST)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialEmotions() = Tutorial(ViraTutorialStep.EMOTIONS, ViraTutorialRoute.FIRST)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialProfile() = Tutorial(ViraTutorialStep.PROFILE)

@PreviewTest @ViraScreenCaptures @Composable
fun TutorialIntegrations() = Tutorial(ViraTutorialStep.INTEGRATIONS)
