package cat.vira.android.features.entry

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.viraSpec
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/// L'escena del pas (`EntryScene.swift`): una il·lustració sencera del món de Viri, a sang, que omple
/// l'espai que la fulla deixa lliure, també per sota de la capçalera, i hi queda una mica per sota
/// (`EntrySheetOverlap`).
///
/// La il·lustració **es centra a la part que es veu** (`coveredTop`, `coveredBottom`): si no, la
/// píndola del progrés tapava el cap de Viri i la bombolla, la boca. Si la franja que queda és més
/// estreta que `minimumRoom` (lletra gran, on la fulla ho ocupa gairebé tot), hi queda només el color.
///
/// És decoració: TalkBack no la llegeix. En canviar de pas es fon amb la següent i s'assenta amb un
/// zoom molt lleu; quan l'usuari tria alguna cosa, fa un batec. Amb Reduir moviment, només el fos.
/// No es vesteix amb la skin: el flux d'entrada mai porta skin.
@Composable
fun EntryScene(
    @DrawableRes scene: Int,
    modifier: Modifier = Modifier,
    coveredTop: Dp = 0.dp,
    coveredBottom: Dp = 0.dp,
    minimumRoom: Dp = 0.dp,
    celebrates: Boolean = false,
    reaction: Int = 0,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    Box(modifier.background(ViraColor.cardFill(ViraArea.Today)).clearAndSetSemantics { }) {
        AnimatedContent(
            targetState = scene,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                } else {
                    (fadeIn(ViraMotion.standard()) + scaleIn(ViraMotion.standard(), initialScale = 1.04f)) togetherWith
                        fadeOut(ViraMotion.standard())
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "entryScene",
        ) { shown ->
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val top = coveredTop
                val bottom = maxHeight - coveredBottom
                if (bottom - top >= minimumRoom) {
                    val center = (top + bottom) / 2
                    val half = maxOf(center, maxHeight - center)
                    Image(
                        painter = painterResource(shown),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            // Un marc centrat a la part visible i prou alt per cobrir de dalt a baix:
                            // la imatge hi omple a sang.
                            .layout { measurable, constraints ->
                                val width = constraints.maxWidth
                                val height = (half * 2).roundToPx()
                                val placeable = measurable.measure(Constraints.fixed(width, height))
                                layout(width, constraints.maxHeight) {
                                    placeable.place(0, (center - half).roundToPx())
                                }
                            }
                            .entryBeat(reaction, enabled = !reduceMotion),
                    )
                }
            }
        }
        EntryCelebrationLayer(celebrates, Modifier.matchParentSize())
    }
}

/// El batec de l'escena quan es tria una opció: creix un 3 % i torna. Ara Viri és dins de l'escena
/// i el que reacciona és tota la casa.
@Composable
private fun Modifier.entryBeat(trigger: Int, enabled: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger, enabled) {
        if (!enabled || trigger == 0) return@LaunchedEffect
        scale.animateTo(1.03f, tween(140, easing = FastOutSlowInEasing))
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/// L'escenari del flux (`EntryStage.swift`): l'espai de l'escena que la fulla deixa lliure, amb la
/// **bombolla de Viri** (Arnau, 2026-10-21).
///
/// - La bombolla va **a baix**, tocant la fulla, amb la cua cap amunt cap a Viri. A la benvinguda
///   va **a dalt a la dreta**, amb la cua avall, perquè Viri hi és sencer i saluda.
/// - **Cap text sobre la il·lustració**: la bombolla és una superfície (`EntryViriLine`).
/// - Segueix sent **una sola aparició expressiva** per pantalla: l'escena.
///
/// `onLineHeight` diu quant ocupa la bombolla amb el seu marge, perquè l'escena centri Viri fora
/// d'ella. A mides d'accessibilitat la bombolla torna a dins de la fulla (`EntryFlow`).
@Composable
fun EntryStage(line: String?, onTop: Boolean, modifier: Modifier = Modifier, onLineHeight: (Int) -> Unit = {}) {
    Column(
        modifier
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = if (onTop) EntryStageTopInset else ViraSpacing.xs, bottom = ViraSpacing.s)
            .semantics { isTraversalGroup = true },
    ) {
        if (!onTop) Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().onSizeChanged { onLineHeight(it.height) }) {
            if (line != null) {
                // Que la bombolla no faci tota l'amplada: és una frase, no una franja.
                EntryViriLine(
                    line,
                    tailOnTop = !onTop,
                    alignEnd = onTop,
                    modifier = Modifier.padding(start = if (onTop) ViraSpacing.xxl * 2 else 0.dp, end = if (onTop) 0.dp else ViraSpacing.xxl * 2),
                )
            }
        }
        if (onTop) Spacer(Modifier.weight(1f))
    }
}

/// Marge de dalt de la bombolla a la benvinguda, on va a sobre de Viri.
val EntryStageTopInset = ViraSpacing.xxl * 2

/// L'escena de cada pas (Arnau, 2026-10-21). La primera acció la tria segons el que s'ha triat a
/// "Per on comencem?".
@DrawableRes
fun EntryStep.scene(intent: EntryStartIntent): Int = when (this) {
    EntryStep.WELCOME -> R.drawable.viri_home_morning
    EntryStep.START -> R.drawable.viri_suggest_capture_1
    EntryStep.AGE -> R.drawable.viri_scene_friends
    EntryStep.PRIVACY, EntryStep.CONSENT -> R.drawable.viri_scene_privacy
    EntryStep.FIRST_ACTION -> when (intent) {
        EntryStartIntent.ORGANIZE -> R.drawable.viri_suggest_habit_1
        EntryStartIntent.EMOTIONS -> R.drawable.viri_suggest_check_in_1
        EntryStartIntent.FOCUS -> R.drawable.viri_suggest_focus_1
    }
    EntryStep.DONE -> R.drawable.viri_scene_launch
    EntryStep.ACCOUNT, EntryStep.PASSWORD_RESET, EntryStep.NEW_PASSWORD -> R.drawable.viri_scene_plan_empty
    EntryStep.NOTIFICATIONS -> R.drawable.viri_suggest_wind_down_1
    EntryStep.NICKNAME, EntryStep.CONNECTIONS -> R.drawable.viri_scene_calendar
}

/// La celebració del pas «Fet», a sobre de l'escena (`EntryCelebrationLayer.swift`): dues anelles
/// que s'obren i confeti. Decoració pura; amb les celebracions apagades no es dibuixa res, i amb
/// Reduir moviment surt ja obert.
@Composable
private fun EntryCelebrationLayer(celebrates: Boolean, modifier: Modifier) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val veil = ViraColor.lightArtworkVeil
    val open by animateFloatAsState(
        targetValue = if (celebrates) 1f else 0f,
        animationSpec = viraSpec(ViraMotion.settle(), decorative = true),
        label = "entryCelebration",
    )
    val shown = if (celebrates && reduceMotion) 1f else open
    val confettiColors = CONFETTI.map { ViraColor.cardFill(it.area) }
    Canvas(modifier.clearAndSetSemantics { }) {
        if (shown <= 0f) return@Canvas
        val side = min(size.width, size.height) * 1.1f
        val center = Offset(size.width / 2f, size.height * 0.5f)
        listOf(0.62f, 0.9f).forEach { ratio ->
            val alpha = if (ratio < 0.8f) 0.5f else 0.25f
            drawCircle(
                veil.copy(alpha = alpha * shown),
                radius = side * ratio / 2f * (0.4f + 0.6f * shown),
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        CONFETTI.forEachIndexed { index, piece ->
            val target = Offset(size.width * piece.x, size.height * piece.y)
            val at = center + (target - center) * shown
            val pieceSize = Size(8.dp.toPx(), (if (piece.long) 18 else 10).dp.toPx())
            rotate(piece.angle * shown, at) {
                drawRoundRect(
                    confettiColors[index].copy(alpha = max(0f, shown)),
                    topLeft = at - Offset(pieceSize.width / 2f, pieceSize.height / 2f),
                    size = pieceSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

/// Rectangles petits amb els quatre colors de mòdul que ja té el sistema.
private data class ConfettiPiece(val x: Float, val y: Float, val angle: Float, val area: ViraArea, val long: Boolean)

private val CONFETTI = listOf(
    ConfettiPiece(0.1f, 0.22f, 20f, ViraArea.Rewards, true),
    ConfettiPiece(0.22f, 0.1f, -30f, ViraArea.Emotions, false),
    ConfettiPiece(0.78f, 0.14f, 40f, ViraArea.Plan, true),
    ConfettiPiece(0.9f, 0.3f, 10f, ViraArea.Focus, false),
    ConfettiPiece(0.14f, 0.5f, -50f, ViraArea.Plan, true),
    ConfettiPiece(0.86f, 0.56f, 60f, ViraArea.Rewards, true),
    ConfettiPiece(0.5f, 0.04f, 0f, ViraArea.Focus, false),
    ConfettiPiece(0.66f, 0.08f, -20f, ViraArea.Emotions, true),
    ConfettiPiece(0.32f, 0.36f, 70f, ViraArea.Rewards, false),
    ConfettiPiece(0.7f, 0.42f, 15f, ViraArea.Emotions, false),
    ConfettiPiece(0.04f, 0.4f, 0f, ViraArea.Focus, false),
    ConfettiPiece(0.95f, 0.12f, -35f, ViraArea.Plan, false),
)
