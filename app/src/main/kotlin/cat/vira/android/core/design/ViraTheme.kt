package cat.vira.android.core.design

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraPalette

/// L'entorn que els components de Vira consulten, l'equivalent del que a l'iPhone llegeixen de
/// `@Environment`: aparença, Reduir moviment, Augmentar contrast i mida de lletra d'accessibilitat.
@Immutable
data class ViraEnvironment(
    val isDark: Boolean = false,
    /// «Treu les animacions» d'Android (`accessibilityReduceMotion` a l'iPhone).
    val reduceMotion: Boolean = false,
    /// Contrast alt d'Android 14 o posterior (`colorSchemeContrast == .increased`).
    val increasedContrast: Boolean = false,
    /// Lletra prou gran perquè els components passin a la seva forma apilada
    /// (`dynamicTypeSize.isAccessibilitySize`). A l'iPhone comença a AX1, un 65 % per sobre del
    /// cos; a Android, a partir de l'esglaó del 150 %.
    val isAccessibilitySize: Boolean = false,
)

val LocalViraEnvironment = staticCompositionLocalOf { ViraEnvironment() }

/// El llindar d'escala de lletra on Vira passa a les formes d'accessibilitat.
const val VIRA_ACCESSIBILITY_FONT_SCALE = 1.5f

/// Tema de l'app. Posa l'entorn de Vira i fa que Material, per sota, faci servir els mateixos
/// tokens i la mateixa lletra. Els components de Vira no llegeixen `MaterialTheme`: llegeixen
/// `ViraColor` i `ViraFont`.
///
/// Els paràmetres existeixen per a les captures, que han de poder forçar fosc, contrast i
/// moviment sense tocar el sistema.
@Composable
fun ViraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean? = null,
    increasedContrast: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val environment = ViraEnvironment(
        isDark = darkTheme,
        reduceMotion = reduceMotion ?: rememberSystemReducesMotion(context),
        increasedContrast = increasedContrast ?: rememberSystemIncreasesContrast(context),
        isAccessibilitySize = LocalDensity.current.fontScale >= VIRA_ACCESSIBILITY_FONT_SCALE,
    )
    CompositionLocalProvider(LocalViraEnvironment provides environment) {
        MaterialTheme(colorScheme = materialScheme(darkTheme)) {
            CompositionLocalProvider(
                LocalTextStyle provides ViraFont.body.copy(color = ViraColor.textPrimary),
                content = content,
            )
        }
    }
}

/// Material només per sota: els seus controls (menús, diàlegs, camps) agafen els tokens de Vira
/// en lloc dels colors per defecte.
private fun materialScheme(isDark: Boolean) = with(ViraPalette) {
    fun c(tone: cat.vira.android.core.design.tokens.ViraTone) = Color(tone.pick(isDark))
    val accent = c(accent(ViraArea.Today))
    if (isDark) {
        darkColorScheme(
            primary = accent, onPrimary = c(onAccent),
            primaryContainer = c(brandSurface), onPrimaryContainer = c(brandOnSurface),
            background = c(background), onBackground = c(textPrimary),
            surface = c(surfaceElevated), onSurface = c(textPrimary),
            surfaceContainer = c(surfaceElevated), onSurfaceVariant = c(textSecondary),
            outline = c(controlBorder), outlineVariant = c(separator), error = c(error),
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = c(onAccent),
            primaryContainer = c(brandSurface), onPrimaryContainer = c(brandOnSurface),
            background = c(background), onBackground = c(textPrimary),
            surface = c(surfaceElevated), onSurface = c(textPrimary),
            surfaceContainer = c(surfaceElevated), onSurfaceVariant = c(textSecondary),
            outline = c(controlBorder), outlineVariant = c(separator), error = c(error),
        )
    }
}

/// Els dos ajustos es poden canviar amb l'app oberta: s'escolten perquè l'entorn canviï a l'instant,
/// com a l'iPhone, i no només quan l'activitat es torna a crear.
@Composable
private fun rememberSystemReducesMotion(context: Context): Boolean {
    var value by remember(context) { mutableStateOf(systemReducesMotion(context)) }
    if (!LocalInspectionMode.current) {
        DisposableEffect(context) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    value = systemReducesMotion(context)
                }
            }
            val registered = runCatching {
                context.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
                )
            }.isSuccess
            onDispose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
        }
    }
    return value
}

@Composable
private fun rememberSystemIncreasesContrast(context: Context): Boolean {
    var value by remember(context) { mutableStateOf(systemIncreasesContrast(context)) }
    if (!LocalInspectionMode.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        DisposableEffect(context) {
            val uiMode = context.getSystemService(UiModeManager::class.java)
            val listener = UiModeManager.ContrastChangeListener { contrast -> value = contrast >= 0.5f }
            uiMode?.addContrastChangeListener(context.mainExecutor, listener)
            onDispose { uiMode?.removeContrastChangeListener(listener) }
        }
    }
    return value
}

private fun systemReducesMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

private fun systemIncreasesContrast(context: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val uiMode = context.getSystemService(UiModeManager::class.java)
        (uiMode?.contrast ?: 0f) >= 0.5f
    } else {
        false
    }
}.getOrDefault(false)
