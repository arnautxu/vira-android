package cat.vira.android.core.design.tokens

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cat.vira.android.core.design.LocalViraEnvironment
import kotlin.math.PI

/// Corbes d'animació, calcades de `ViraMotion.swift`, i la seva traducció quan l'usuari demana
/// menys moviment (a Android, «Treu les animacions» a Accessibilitat).
///
/// Les dues respostes a Reduir moviment, com a l'iPhone:
/// - **Canvi d'estat** (una fila que s'esborra, un valor que canvia): un fos curt, `reduced`.
///   Suprimir-lo faria saltar el contingut, i un salt és pitjor que un fos.
/// - **Moviment decoratiu** (un bot, una respiració, una ràfega): res.
///
/// Les molles de SwiftUI es descriuen per `response` (el període) i esmorteïment; aquí es tradueixen
/// a la rigidesa equivalent, `(2π / response)²`, amb el mateix esmorteïment.
object ViraMotion {

    /// Resposta immediata a un toc: premuts, commutadors, selecció.
    fun <T> quick(): FiniteAnimationSpec<T> = tween(150, easing = LinearOutSlowInEasing)

    /// El canvi d'estat estàndard (`.snappy(duration: 0.25)`).
    fun <T> standard(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        if (delayMillis == 0) spring(0.85f, stiffness(0.25f)) else tween(250, delayMillis, FastOutSlowInEasing)

    /// Celebracions i fites. L'única corba amb rebot visible (`.bouncy(duration: 0.35)`).
    fun <T> emphasis(): FiniteAnimationSpec<T> = spring(0.7f, stiffness(0.35f))

    /// Alguna cosa que arriba i ha d'aterrar, no rebotar.
    fun <T> entrance(): FiniteAnimationSpec<T> = spring(0.85f, stiffness(0.4f))

    /// Alguna cosa que cau al seu lloc: una barra que s'omple.
    fun <T> settle(): FiniteAnimationSpec<T> = spring(0.7f, stiffness(0.5f))

    /// Crida d'atenció suau i curta. Mai per insistir que algú toqui res.
    fun <T> nudge(): FiniteAnimationSpec<T> = spring(0.4f, stiffness(0.3f))

    /// Mitja volta d'un cicle lent: respiració, Viri en repòs. Quatre segons a posta.
    const val BREATH_MILLIS = 4000

    /// El fos que substitueix un canvi d'estat amb Reduir moviment.
    fun <T> reduced(): FiniteAnimationSpec<T> = tween(200, easing = LinearOutSlowInEasing)

    private fun stiffness(response: Float): Float {
        val omega = 2f * PI.toFloat() / response
        return omega * omega
    }
}

/// La corba que toca ara mateix: amb Reduir moviment, el fos (canvi d'estat) o res (decoració).
@Composable @ReadOnlyComposable
fun <T> viraSpec(spec: AnimationSpec<T>, decorative: Boolean = false): AnimationSpec<T> =
    when {
        !LocalViraEnvironment.current.reduceMotion -> spec
        decorative -> snap()
        else -> ViraMotion.reduced()
    }
