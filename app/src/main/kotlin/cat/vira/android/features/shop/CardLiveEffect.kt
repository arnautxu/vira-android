package cat.vira.android.features.shop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/// La part de la carta que es mou (`CardLiveEffect`, Arnau, 2026-10-22: «la carta estàtica però una
/// part animada»).
///
/// La carta es queda quieta i, a sobre de la il·lustració i dins del marc, s'hi mou el que li dona
/// la raresa:
/// - **Comuna**: un sol reflex que la creua en obrir-la.
/// - **Rara**: una làmina holo que segueix la inclinació del mòbil.
/// - **Èpica** i **llegendària**: la làmina i partícules del seu tema (bombolles a les d'emocions,
///   estrelles a les de concentració, gemmes a la resta).
///
/// El reflex són ratlles nítides, mai una franja difuminada.
///
/// Només a la carta oberta i a la de «Carta nova!»: a la graella totes són quietes. **La raresa no
/// depèn de l'efecte**: es diu amb el nom i el gruix de la vora.
/// - Reduir moviment: res.
/// - Estalvi de bateria: la làmina queda fixa i les partícules s'aturen.
/// - El sensor només corre mentre la carta és a la pantalla i l'app és davant.
///
/// Els colors són els farcits d'àrea resolts sempre en clar: la il·lustració és la mateixa en clar i
/// en fosc, i el reflex també.
@Composable
fun CardLiveEffect(card: CardItem, modifier: Modifier = Modifier) {
    if (LocalViraEnvironment.current.reduceMotion) return
    val context = LocalContext.current
    val lowPower by rememberPowerSaving(context)
    val isActive by rememberIsStarted()
    val rare = card.rarity >= CardItem.Rarity.Rare
    val tilt = rememberCardTilt(context, follows = rare && !lowPower && isActive)
    val sweep = remember { Animatable(-1f) }
    LaunchedEffect(card.key) {
        if (card.rarity == CardItem.Rarity.Common) {
            kotlinx.coroutines.delay(350)
            sweep.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val holoTilt = when {
            !rare -> Offset(sweep.value, 0f)
            lowPower -> Offset.Zero
            else -> tilt
        }
        Canvas(Modifier.fillMaxSize()) { drawHolo(holoTilt, fadesAtEdges = !rare) }
        if (card.rarity >= CardItem.Rarity.Epic && !lowPower) {
            CardParticles(card.particleStyle, if (card.rarity == CardItem.Rarity.Legendary) 18 else 12)
        }
    }
}

private val HOLO_PALETTE = listOf(ViraArea.Emotions, ViraArea.Rewards, ViraArea.Focus, ViraArea.Plan, ViraArea.Today)
    .map { Color(ViraPalette.cardFill(it).light) }
private val LIGHT_BACKGROUND = Color(ViraPalette.background.light)
private val PARTICLE_COLORS = listOf(
    LIGHT_BACKGROUND, Color(ViraPalette.cardFill(ViraArea.Rewards).light), Color(ViraPalette.surface(ViraArea.Today).light),
)

/// La làmina holo: ratlles fines dels cinc colors d'àrea, en diagonal, que es desplacen amb la
/// inclinació, i uns quants destells que s'encenen quan l'angle els toca.
private fun DrawScope.drawHolo(tilt: Offset, fadesAtEdges: Boolean) {
    val lean = min(1f, hypot(tilt.x, tilt.y))
    val strength = if (fadesAtEdges) 0.3f * (1 - lean) else 0.13f + 0.12f * lean
    if (strength <= 0.01f) return
    val stripe = max(4f, size.width / 22)
    val period = stripe * HOLO_PALETTE.size
    val reach = hypot(size.width, size.height)
    val shift = (tilt.x * 0.6f + tilt.y * 0.3f) * size.width
    rotate(-28f, pivot = center) {
        var x = center.x - reach + (shift % period) - period
        var index = 0
        while (x < center.x + reach) {
            drawRect(
                HOLO_PALETTE[index % HOLO_PALETTE.size], topLeft = Offset(x, center.y - reach),
                size = Size(stripe, reach * 2), alpha = strength, blendMode = BlendMode.ColorDodge,
            )
            x += stripe
            index++
        }
    }
    for (glint in 0 until 8) {
        val phase = cardNoise(glint, 1) * 2 - 1
        val light = max(0.0, cos((tilt.x - phase) * 2.6)).toFloat()
        if (light <= 0.05f) continue
        val side = size.width * 0.06f * (0.5f + light * 0.8f)
        val point = Offset(size.width * cardNoise(glint, 2).toFloat(), size.height * cardNoise(glint, 3).toFloat())
        drawPath(sparkleStar(point, side), LIGHT_BACKGROUND, alpha = light * strength, blendMode = BlendMode.ColorDodge)
    }
}

/// Les partícules que pugen dins de la il·lustració. Posicions deterministes per índex: el mateix
/// moviment cada vegada, sense res a l'atzar.
@Composable
private fun CardParticles(style: CardItem.ParticleStyle, count: Int) {
    var time by remember { mutableFloatStateOf(0f) }
    var start by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                time = (now - start) / 1_000_000_000f
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        for (index in 0 until count) {
            val speed = 0.6 + cardNoise(index, 4) * 0.6
            val raw = time * speed / 6 + cardNoise(index, 5)
            val rise = (raw - floor(raw)).toFloat()
            val y = size.height * (1.05f - 1.1f * rise)
            val x = size.width * (0.08f + 0.84f * cardNoise(index, 6).toFloat() + sin(time + index) * 0.015f)
            val side = size.width * (0.025f + 0.035f * cardNoise(index, 7).toFloat())
            // Entren i surten fosos: cap partícula apareix o desapareix de cop a la vora.
            val alpha = min(1f, rise * 5) * min(1f, (1 - rise) * 4)
            val color = PARTICLE_COLORS[index % PARTICLE_COLORS.size]
            val point = Offset(x, y)
            when (style) {
                CardItem.ParticleStyle.Bubbles ->
                    drawCircle(color, side / 2, point, alpha = alpha, style = Stroke(max(1f, side * 0.12f)))
                CardItem.ParticleStyle.Stars -> drawPath(sparkleStar(point, side), color, alpha = alpha)
                CardItem.ParticleStyle.Gems ->
                    drawPath(sparkleGem(point, side, time * (if (index % 2 == 0) 1 else -1)), color, alpha = alpha)
            }
        }
    }
}

/// La inclinació del mòbil, de –1 a 1 a cada eix, suavitzada. El sensor només existeix mentre hi ha
/// una carta viva a la pantalla.
@Composable
private fun rememberCardTilt(context: Context, follows: Boolean): Offset {
    var value by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(follows) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (!follows || manager == null || sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Android dona la gravetat en m/s² i amb els eixos al revés de l'iPhone: es passa a
                // la mateixa escala (–1…1) i al mateix sentit. Amb el mòbil a la mà, inclinat cap a
                // tu, `y` val uns –0,55: aquell és el zero.
                val gx = -event.values[0] / SensorManager.GRAVITY_EARTH
                val gy = -event.values[1] / SensorManager.GRAVITY_EARTH
                val target = Offset(clampUnit(gx * 1.6f), clampUnit((gy + 0.55f) * 1.6f))
                value = Offset(value.x + (target.x - value.x) * 0.25f, value.y + (target.y - value.y) * 0.25f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return value
}

@Composable
private fun rememberPowerSaving(context: Context) = remember { mutableStateOf(context.isPowerSaving()) }.also { state ->
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                state.value = context.isPowerSaving()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
}

@Composable
private fun rememberIsStarted() = remember { mutableStateOf(true) }.also { state ->
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> state.value = true
                Lifecycle.Event.ON_STOP -> state.value = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun Context.isPowerSaving(): Boolean = getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

private fun clampUnit(value: Float): Float = value.coerceIn(-1f, 1f)

/// Un nombre de 0 a 1 fix per a cada parella (índex, sal).
private fun cardNoise(index: Int, salt: Int): Double {
    val value = sin((index * 127 + salt * 311).toDouble()) * 43_758.5453
    return value - floor(value)
}

/// Estrella de quatre puntes.
private fun sparkleStar(center: Offset, size: Float) = Path().apply {
    val r = size / 2
    val inner = r * 0.22f
    for (point in 0 until 8) {
        val angle = point * PI / 4 - PI / 2
        val radius = if (point % 2 == 0) r else inner
        val x = center.x + cos(angle).toFloat() * radius
        val y = center.y + sin(angle).toFloat() * radius
        if (point == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/// Rombe que gira sobre si mateix.
private fun sparkleGem(center: Offset, size: Float, turn: Float) = Path().apply {
    val r = size / 2
    for (point in 0 until 4) {
        val angle = point * PI / 2 + turn
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r * 1.3f
        if (point == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
