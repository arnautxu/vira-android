package cat.vira.android.features.progress.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import cat.vira.android.R
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTouchTarget
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.progress.logic.ViraLevel
import java.text.NumberFormat

/// Peces petites que només fa servir la pestanya Viri. Si una segona funció les necessités, el
/// lloc seria `core/design` (`CLAUDE.md`: global només si ≥ 2 funcions).

/// El pictograma del marc de nivell (`LevelFrame.assetName` de l'iPhone).
@get:DrawableRes
internal val ViraLevel.LevelFrame.pictogram: Int
    get() = when (this) {
        ViraLevel.LevelFrame.BRONZE -> R.drawable.picto_level_frame_bronze
        ViraLevel.LevelFrame.SILVER -> R.drawable.picto_level_frame_silver
        ViraLevel.LevelFrame.GOLD -> R.drawable.picto_level_frame_gold
        ViraLevel.LevelFrame.DIAMOND -> R.drawable.picto_level_frame_diamond
    }

/// Xifra amb el separador de milers de l'idioma (`Int.formatted()`).
internal fun Int.formatted(): String = NumberFormat.getIntegerInstance().format(this)

/// Puja la peça `amount` per sobre de la vora d'adalt **i** li treu aquest espai a la columna, com
/// el `padding(.top, -x)` de l'iPhone: la bombolla trepitja l'escena i el que ve després no queda
/// separat per un forat.
internal fun Modifier.overlapTop(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val lift = amount.roundToPx()
    layout(placeable.width, (placeable.height - lift).coerceAtLeast(0)) { placeable.place(0, -lift) }
}

/// La flama de la ratxa (`ViraStreakFlame`). Apagada, en gris i a mitja opacitat: descriu que no
/// hi ha ratxa, no es trenca ni canvia de forma.
@Composable
internal fun ViraStreakFlame(size: Dp, modifier: Modifier = Modifier, isLit: Boolean = true) {
    Image(
        painter = painterResource(R.drawable.picto_flame),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = if (isLit) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier.size(size).alpha(if (isLit) 1f else 0.45f),
    )
}

/// La fletxa d'un botó de navegació. Dibuixada i no d'un joc d'icones: l'app no en porta cap.
internal enum class ChevronDirection { Left, Right }

@Composable
internal fun Chevron(direction: ChevronDirection, color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val side = size * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(modifier.size(side)) {
        val w = this.size.width
        val path = Path().apply {
            if (direction == ChevronDirection.Right) {
                moveTo(w * 0.32f, w * 0.12f); lineTo(w * 0.72f, w * 0.5f); lineTo(w * 0.32f, w * 0.88f)
            } else {
                moveTo(w * 0.68f, w * 0.12f); lineTo(w * 0.28f, w * 0.5f); lineTo(w * 0.68f, w * 0.88f)
            }
        }
        drawPath(path, color, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/// L'exclamació d'«avui, pendent»: la forma diu l'estat, no el color.
@Composable
internal fun ExclamationMark(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.2f
        drawLine(color, Offset(w / 2, w * 0.12f), Offset(w / 2, w * 0.6f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(color, radius = stroke * 0.62f, center = Offset(w / 2, w * 0.88f))
    }
}

/// Enrere dins de la pestanya Viri: el botó de la barra de navegació de l'iPhone («‹ Viri»). El
/// gest d'enrere del sistema fa el mateix.
@Composable
internal fun ProgressBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.tab_viri_short)
    val back = stringResource(R.string.action_back)
    Row(
        modifier = modifier
            .viraTouchTarget()
            .viraPressable(onClick = onBack)
            .padding(end = ViraSpacing.xs)
            .clearAndSetSemantics {
                contentDescription = back
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        Chevron(ChevronDirection.Left, ViraColor.accent(ViraArea.Rewards), size = 18.dp)
        Text(label, style = ViraFont.headline, color = ViraColor.accent(ViraArea.Rewards))
    }
}

/// El saldo a la barra de Reptes (`ShopWallet`): monedes en una píndola groga.
@Composable
internal fun ProgressWallet(balance: Int, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.progress_coins, balance.formatted())
    Row(
        modifier = modifier
            .background(ViraGradient.card(ViraArea.Rewards), CircleShape)
            .padding(horizontal = ViraSpacing.xs, vertical = ViraSpacing.xxs)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        cat.vira.android.core.design.components.ViraPictogram(R.drawable.picto_shop, size = 24.dp, scalesWithText = true)
        Text(balance.formatted(), style = ViraFont.headline, color = ViraColor.onCard(ViraArea.Rewards))
    }
}

/// Capçalera d'una secció de la pantalla («El que has fet», «Per a tu»).
@Composable
internal fun ProgressSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ViraFont.headline, color = ViraColor.textPrimary, modifier = modifier.semantics { heading() })
}

/// Tres barres amb un pom (`slider.horizontal.3`): «personalitza».
@Composable
internal fun SlidersMark(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.1f
        listOf(0.22f to 0.66f, 0.5f to 0.3f, 0.78f to 0.58f).forEach { (y, knob) ->
            drawLine(color, Offset(w * 0.08f, w * y), Offset(w * 0.92f, w * y), strokeWidth = stroke, cap = StrokeCap.Round)
            drawCircle(color, radius = w * 0.11f, center = Offset(w * knob, w * y))
        }
    }
}
