package cat.vira.android.app

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cat.vira.android.R
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing

/// Bloqueig opcional de l'app amb l'empremta, la cara o el codi del telèfon
/// (`ViraAppLockContainer.swift`, `USER_FLOWS.md` F25, `PRIVACY_AND_SAFETY.md` §5). Apagat de
/// fàbrica; l'encén el pas de privacitat de l'entrada o Ajustos.
///
/// Envolta l'arrel en comptes de viure-hi dins: es torna a tancar cada cop que l'app passa a segon
/// pla, no només en obrir-la, perquè és el moment real en què algú altre pot agafar el telèfon. Amb
/// l'ajust encès, el canviador d'apps tampoc n'ensenya el contingut (Android 13 o més).
@Composable
fun ViraAppLock(content: @Composable () -> Unit) {
    val backend = LocalViraBackend.current
    val activity = LocalContext.current as FragmentActivity
    val enabled by rememberPreference(backend.preferences, ViraPreferences.BIOMETRIC_LOCK_ENABLED, false)
    // Neix ja tancat si l'ajust és encès: si naixia obert i es tancava després, el primer fotograma
    // ensenyaria Inici, amb emocions i medicació.
    var locked by rememberSaveable {
        mutableStateOf(backend.preferences.getBoolean(ViraPreferences.BIOMETRIC_LOCK_ENABLED, false))
    }
    // Mentre el sistema demana l'empremta o el codi, l'activitat passa per segon pla (el codi és una
    // pantalla a part): ni s'ha de tornar a tancar ni s'ha de tornar a demanar.
    val prompt = remember(activity) { ViraUnlockPrompt(activity) { locked = false } }

    LaunchedEffect(enabled) { if (!enabled) locked = false }
    DisposableEffect(activity, enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) activity.setRecentsScreenshotEnabled(!enabled)
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) activity.setRecentsScreenshotEnabled(true)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when {
                event == Lifecycle.Event.ON_STOP && enabled && !prompt.isShowing -> locked = true
                event == Lifecycle.Event.ON_RESUME && locked && !prompt.isShowing -> prompt.show()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        // Tapat no vol dir només amagat: TalkBack tampoc pot llegir ni tocar el que hi ha a sota.
        Box(if (locked) Modifier.clearAndSetSemantics {} else Modifier) { content() }
        // Tancar és instantani (la captura del canviador es fa just ara); només obrir té fos.
        AnimatedVisibility(
            visible = locked,
            enter = EnterTransition.None,
            exit = fadeOut(ViraMotion.quick()),
        ) {
            // Els tocs fora del botó s'acaben aquí i no arriben a la pantalla de sota.
            Box(Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }) {
                ViraLockScreen(onUnlock = prompt::show)
            }
        }
    }
}

/// La demanda al sistema. Si el telèfon no té empremta, cara ni codi, no es pot deixar ningú tancat
/// fora de la seva pròpia app: es desbloqueja sense preguntar.
private class ViraUnlockPrompt(private val activity: FragmentActivity, private val onSuccess: () -> Unit) {
    var isShowing = false
        private set

    fun show() {
        if (isShowing) return
        when (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)) {
            // Cap manera d'entrar: ni empremta, ni cara, ni codi.
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            -> { onSuccess(); return }
            // El sistema no ho sap del cert: es prova igualment, com diu l'API.
            BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Unit
            // Un error passatger (sensor ocupat, actualització pendent) no és «no hi ha bloqueig»:
            // es queda tancat, amb «Desbloqueja» per tornar-ho a provar.
            else -> return
        }
        isShowing = true
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isShowing = false
                onSuccess()
            }

            // Cancel·lar o massa intents: es queda tancat, amb el botó de «Desbloqueja» a la vista.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                isShowing = false
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.settings_privacy_lock_reason))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }

    private companion object {
        // Empremta o cara, i el codi com a alternativa. `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` no
        // existeix a Android 10, que és el mínim.
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}

/// Neutra a propòsit —cap Viri, cap to lúdic (`VIRI_AND_GAMIFICATION.md` regla 6)— perquè és
/// seguretat, no una fita a celebrar.
@Composable
internal fun ViraLockScreen(onUnlock: () -> Unit) {
    val iconSize = with(LocalDensity.current) { 56.sp.toDp() }
    Column(
        Modifier
            .fillMaxSize()
            .background(ViraColor.background)
            .padding(ViraSpacing.screenMargin)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = LockIcon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(ViraColor.textSecondary),
        )
        Text(
            stringResource(R.string.settings_privacy_lock_title),
            style = ViraFont.title,
            color = ViraColor.textPrimary,
            textAlign = TextAlign.Center,
        )
        ViraActionButton(
            text = stringResource(R.string.settings_privacy_lock_button),
            onClick = onUnlock,
            modifier = Modifier.widthIn(min = 200.dp),
            textStyle = ViraFont.headline,
        )
    }
}

/// El cadenat ple (`lock.fill` de l'iPhone), dibuixat aquí: cap altra pantalla el fa servir.
private val LockIcon: ImageVector by lazy {
    ImageVector.Builder("lock", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            // Arc
            moveTo(7f, 10f); verticalLineTo(7f)
            arcTo(5f, 5f, 0f, false, true, 17f, 7f)
            verticalLineTo(10f); horizontalLineTo(15f); verticalLineTo(7f)
            arcTo(3f, 3f, 0f, false, false, 9f, 7f)
            verticalLineTo(10f); close()
            // Cos
            moveTo(6.5f, 10f); horizontalLineTo(17.5f)
            arcTo(2f, 2f, 0f, false, true, 19.5f, 12f)
            verticalLineTo(20f)
            arcTo(2f, 2f, 0f, false, true, 17.5f, 22f)
            horizontalLineTo(6.5f)
            arcTo(2f, 2f, 0f, false, true, 4.5f, 20f)
            verticalLineTo(12f)
            arcTo(2f, 2f, 0f, false, true, 6.5f, 10f)
            close()
        }
    }.build()
}
