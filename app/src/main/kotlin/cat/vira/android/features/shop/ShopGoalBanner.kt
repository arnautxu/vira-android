package cat.vira.android.features.shop

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.vira.android.R
import cat.vira.android.core.sound.ViraHaptics
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.delay

/// L'avís d'objectiu fora de la Botiga (`ViraShopGoalBanner`, Arnau, 2026-10-22: «li falta un push
/// al procés»).
///
/// Quan les monedes que acabes de guanyar et fan arribar al teu objectiu («La vull»), surt una
/// targeta a sota, damunt de la barra de pestanyes: «Ja la pots tenir!» amb la skin i «Veure», que
/// obre la Botiga amb el provador d'aquella skin.
///
/// Les regles:
/// - **Dins de l'app, mai notificació.** No és un avís del sistema i no entra al sostre de 64.
/// - **Un sol cop per objectiu** (`ShopPreferences.GOAL_ANNOUNCED`), i només en **creuar** el preu.
/// - Sense objectiu triat, no surt res.
/// - Cedeix el pas a les celebracions: surt quan s'han tancat.
/// - Se'n va sola al cap de 8 segons, menys amb TalkBack, que la llegeix i espera el toc.
///
/// Va a l'arrel, dins de l'espai de les pestanyes, perquè les monedes es guanyen a qualsevol lloc.
@Composable
fun ShopGoalBanner(celebrating: Boolean, onOpen: () -> Unit) {
    val data = rememberShopData()
    val context = LocalContext.current
    val view = LocalView.current
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val counter by data.counter.collectAsStateWithLifecycle(null)
    val skins by data.skins.collectAsStateWithLifecycle(emptyList())
    val wishKey by rememberPreference(data.preferences, ShopPreferences.WISH, "")
    val announced by rememberPreference(data.preferences, ShopPreferences.GOAL_ANNOUNCED, "")
    var pending by remember { mutableStateOf<SkinItem?>(null) }
    /// L'última que ha sortit, que es queda mentre la targeta s'en va.
    var shown by remember { mutableStateOf<SkinItem?>(null) }
    var visible by remember { mutableStateOf(false) }
    val readyText = stringResource(R.string.shop_featured_ready)
    val talkBack = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
    }

    val balance = counter?.coinBalance
    LaunchedEffect(balance) {
        val new = balance ?: return@LaunchedEffect
        val old = LastSeenBalance.value
        LastSeenBalance.value = new
        // La primera lectura no és un canvi: si ja hi arribaves en obrir l'app, no cal dir-t'ho.
        if (old == null) return@LaunchedEffect
        val item = ShopCollection.goalReached(wishKey, old, new, skins.map { it.key }.toSet(), announced) ?: return@LaunchedEffect
        data.preferences.edit { putString(ShopPreferences.GOAL_ANNOUNCED, item.key) }
        pending = item
    }
    LaunchedEffect(pending, celebrating) {
        val item = pending ?: return@LaunchedEffect
        if (celebrating) return@LaunchedEffect
        pending = null
        ViraHaptics.play(context, ViraHaptics.Feedback.Done)
        shown = item
        visible = true
        if (talkBack) view.announceForAccessibility(readyText)
    }
    LaunchedEffect(visible, shown?.key) {
        if (!visible || talkBack) return@LaunchedEffect
        delay(VISIBLE_FOR_MILLIS)
        visible = false
    }

    Box(Modifier.fillMaxSize().padding(bottom = viraTabBarClearance()), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduceMotion) fadeIn() else slideInVertically { it } + fadeIn(),
            exit = if (reduceMotion) fadeOut() else slideOutVertically { it } + fadeOut(),
        ) {
            val item = shown ?: return@AnimatedVisibility
            ShopGoalBannerCard(
                item,
                onOpen = {
                    visible = false
                    data.preferences.edit { putBoolean(ShopPreferences.OPEN_WISH, true) }
                    onOpen()
                },
                onClose = { visible = false },
                modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xs),
            )
        }
    }
}

/// La targeta de l'avís: daurada com el moneder, amb la skin, el text i «Veure». Amb la lletra gran,
/// el text a dalt i els botons a sota: en fila no hi caben.
@Composable
fun ShopGoalBannerCard(item: SkinItem, onOpen: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val large = LocalViraEnvironment.current.isAccessibilitySize
    val ink = ViraColor.onCard(ViraArea.Rewards)
    val title = stringResource(R.string.shop_featured_ready)
    val name = stringResource(item.nameRes)
    val shape = RoundedCornerShape(ViraRadius.l)
    val texts = @Composable { m: Modifier ->
        Column(m.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ViraFont.headline, color = ink)
            Text(name, style = ViraFont.footnote, color = ink)
        }
    }
    val buttons = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Box(
                Modifier.defaultMinSize(minHeight = ViraTouch.minimum)
                    .background(ink, RoundedCornerShape(ViraRadius.s))
                    .viraPressable(onClick = onOpen)
                    .padding(horizontal = ViraSpacing.s),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.shop_goal_open), style = ViraFont.headline, color = ViraColor.cardFill(ViraArea.Rewards)) }
            val close = stringResource(R.string.action_close)
            Box(
                Modifier.size(ViraTouch.minimum).viraPressable(onClick = onClose).clearAndSetSemantics { contentDescription = close },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(12.dp)) {
                    val w = size.width
                    drawLine(ink, Offset(0f, 0f), Offset(w, w), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(ink, Offset(w, 0f), Offset(0f, w), 2.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
    val frame = modifier.fillMaxWidth().wrapContentHeight()
        .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
        .background(ViraGradient.card(ViraArea.Rewards), shape)
        .padding(ViraSpacing.xs)
    if (large) {
        Column(frame, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            texts(Modifier.fillMaxWidth())
            buttons()
        }
    } else {
        Row(frame, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(ViraRadius.s))) {
                SkinPortrait(item.backdropArt, item.cutoutArt, Modifier.matchParentSize())
            }
            texts(Modifier.weight(1f))
            buttons()
        }
    }
}

/// L'últim saldo que ha vist l'avís, fora de la composició: les pestanyes i Concentra't són
/// destinacions diferents, i les monedes d'una sessió arriben quan l'avís no hi és. En tornar, el
/// creuament es veu igual; només la primera lectura de l'app no compta com a canvi.
private object LastSeenBalance {
    @Volatile var value: Int? = null
}

/// Quant es queda a la pantalla si ningú no la toca.
private const val VISIBLE_FOR_MILLIS = 8_000L
