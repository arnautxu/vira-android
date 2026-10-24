package cat.vira.android.features.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/// On és cada peça a la pantalla, en coordenades de l'arrel (`ViraTutorialTargets` de l'iPhone).
///
/// Cada peça ho diu amb `Modifier.viraTutorialTarget`, des de dins de qualsevol pestanya, i l'arrel
/// ho llegeix. Mentre el tomb no hi és, les posicions es desen sense avisar ningú: moure una llista
/// no ha de redibuixar l'arrel de l'app. Quan comença, es publiquen.
@Stable
class ViraTutorialTargets {
    private data class Key(val target: ViraTutorialTarget, val part: Int)

    /// La tasca d'Avui que es pot marcar des del tomb: el títol i què fa tocar-la. La registra la
    /// fila mateixa (`viraTutorialTry`), i només es publica mentre el tomb hi és.
    class TryIt(val title: String, val perform: () -> Unit)

    private val latest = mutableMapOf<Key, Rect>()
    private var isTracking = false
    /// La caixa de cada peça, ja unida si en té més d'una part (la fila de cares).
    val frames = mutableStateMapOf<ViraTutorialTarget, Rect>()

    /// Una per fila (`part`), perquè una fila que deixa de ser la primera no esborri la que ho passa
    /// a ser. La que es publica és la de dalt.
    private val latestTries = mutableMapOf<Int, TryIt>()
    var tryIt by mutableStateOf<TryIt?>(null)
        private set

    fun registerTry(part: Int, value: TryIt?) {
        if (value == null) latestTries.remove(part) else latestTries[part] = value
        if (isTracking) tryIt = topTry
    }

    private val topTry: TryIt? get() = latestTries.minByOrNull { it.key }?.value

    fun report(target: ViraTutorialTarget, part: Int, frame: Rect?) {
        val key = Key(target, part)
        if (frame == null || frame.isEmpty) latest.remove(key) else latest[key] = frame
        if (!isTracking) return
        val joined = union(target)
        if (joined == null) frames.remove(target) else if (frames[target] != joined) frames[target] = joined
    }

    fun startTracking() {
        isTracking = true
        tryIt = topTry
        frames.clear()
        latest.keys.map { it.target }.toSet().forEach { target -> union(target)?.let { frames[target] = it } }
    }

    fun stopTracking() {
        isTracking = false
        frames.clear()
        tryIt = null
    }

    private fun union(target: ViraTutorialTarget): Rect? =
        latest.filterKeys { it.target == target }.values.reduceOrNull { joined, rect ->
            Rect(minOf(joined.left, rect.left), minOf(joined.top, rect.top), maxOf(joined.right, rect.right), maxOf(joined.bottom, rect.bottom))
        }
}

/// El registre de l'arrel. Sense arrel (captures d'una pantalla sola), `null`: les peces no diuen res.
val LocalViraTutorialTargets = staticCompositionLocalOf<ViraTutorialTargets?> { null }

/// Obre una sola parada del tomb des d'un estat buit («Com funciona?», `viraShowTutorialStop`).
val LocalViraShowTutorialStop = staticCompositionLocalOf<(ViraTutorialStep) -> Unit> { {} }

/// Diu al tomb on és aquesta peça. Fora del tomb només es desa la posició.
///
/// - `part`: per a una peça feta de diverses vistes (la fila de cares), cadascuna amb el seu
///   número; el forat les cobreix totes.
/// - `isActive`: `false` treu la vista de la peça sense treure-la de la pantalla.
fun Modifier.viraTutorialTarget(target: ViraTutorialTarget, part: Int = 0, isActive: Boolean = true): Modifier = composed {
    val targets = LocalViraTutorialTargets.current ?: return@composed Modifier
    DisposableEffect(targets, target, part, isActive) {
        if (!isActive) targets.report(target, part, null)
        onDispose { targets.report(target, part, null) }
    }
    if (!isActive) Modifier else Modifier.onGloballyPositioned { targets.report(target, part, it.boundsInRoot()) }
}

/// Fa que aquesta fila sigui la tasca que el tomb deixa provar: el seu cercle és `TodayCircle` i
/// tocar-lo des del tomb fa `perform`. `isActive = false` la treu (no és la primera per marcar).
fun Modifier.viraTutorialTry(title: String, isActive: Boolean, part: Int, perform: () -> Unit): Modifier = composed {
    val targets = LocalViraTutorialTargets.current
    val action by rememberUpdatedState(perform)
    if (targets != null) {
        DisposableEffect(targets, title, isActive, part) {
            targets.registerTry(part, if (isActive) ViraTutorialTargets.TryIt(title) { action() } else null)
            onDispose { targets.registerTry(part, null) }
        }
    }
    Modifier.viraTutorialTarget(ViraTutorialTarget.TodayCircle, part, isActive)
}

/// Per a les peces que ja diuen on són per un altre camí (la barra, `onItemBounds`).
@Composable
fun rememberViraTutorialReporter(): (ViraTutorialTarget, Rect) -> Unit {
    val targets = LocalViraTutorialTargets.current
    return { target, rect -> targets?.report(target, 0, rect) }
}
