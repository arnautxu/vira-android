package cat.vira.android.core.design.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.resources.ViraVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/// El vídeo de fons d'una celebració (`ViraCelebrationBackdrop` de l'iPhone), amb el que la
/// pantalla necessita saber-ne per col·locar-hi el text a sobre.
///
/// - `tone`: si la imatge és clara o fosca. **No és estètica**: decideix el color de tot el text.
/// - `rise`: quant puja el vídeo, en fracció de l'alçada, per obrir espai sota els peus de Viri
///   sense fer-lo més gran. La vora de baix s'esvaeix cap al degradat de tokens.
/// - `sound`: si el vídeo sona (l'hàbit, les fites de 7 i 30 dies). Qui mostra la pantalla no
///   hi ha de fer sonar res més.
/// - `textPlacement`: on hi cap el text, a part del to: `Split` (xifra a dalt, la resta a baix,
///   el vídeo respira al mig) o `Top` (tot a dalt, quan la meitat de baix no admet text).
/// - `protectsText`: base opaca sota els blocs de text quan el contrast canvia entre fotogrames.
data class ViraCelebrationBackdrop(
    val video: ViraVideo,
    val tone: Tone = Tone.Light,
    val rise: Float = 0f,
    val sound: Boolean = false,
    val textPlacement: TextPlacement = TextPlacement.Split,
    val protectsText: Boolean = false,
) {
    enum class Tone { Light, Dark }
    enum class TextPlacement { Split, Top }
}

/// El moment a pantalla completa d'una fita (`ViraCelebrationScreen`): ratxa, hàbit i botiga
/// (nivell, repte, premi i carta van amb `ViraCelebrationMoment`, `DESIGN_RULES.md` §5).
///
/// Fons a sang amb `cardGradient(area)` i **tot el text en `onCard(area)`, sense opacitat**: el
/// contrast va just i la jerarquia la fa la mida. Heroi amb la ràfega al darrere, píndola de
/// context, xifra gegant que compta amunt (decorativa: el peu ja la diu en paraules), missatge i
/// «Continua» ple del color del text. Confeti si `celebrationsEnabled`.
///
/// Amb `backdrop`, el vídeo va a sang i substitueix **el fons i l'heroi alhora** (el protagonista
/// ja és dins del vídeo): xifra a dalt, Viri al mig, la resta a baix. Amb un vídeo fosc el text
/// passa a `onDarkArtwork` i l'etiqueta del botó a `onBrandArtwork`. Amb Reduir moviment, i a les
/// captures, es torna al fons de tokens i a l'heroi: cap camí es queda sense imatge.
///
/// Amb Reduir moviment, tot apareix al seu lloc amb un fos i la xifra hi és directament.
/// `onShown` és on qui la mostra fa sonar el so i la vibració de la fita.
@Composable
fun ViraCelebrationScreen(
    caption: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Rewards,
    eyebrow: String? = null,
    count: Int? = null,
    message: String? = null,
    celebrationsEnabled: Boolean = true,
    backdrop: ViraCelebrationBackdrop? = null,
    onShown: () -> Unit = {},
    extra: @Composable () -> Unit = {},
    hero: @Composable () -> Unit,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val playing = backdrop?.takeIf { !reduceMotion && !LocalInspectionMode.current }
    val isDark = playing?.tone == ViraCelebrationBackdrop.Tone.Dark
    val ink = if (isDark) ViraColor.onDarkArtwork else ViraColor.onCard(area)
    val buttonLabel = if (isDark) ViraColor.onBrandArtwork else ViraColor.cardFill(area)
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val shownCount = remember { Animatable(if (reduceMotion) (count ?: 0).toFloat() else 0f) }
    val announcement = listOfNotNull(eyebrow, caption, message).joinToString(". ")
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onShown()
        launch { appear.animateTo(1f, if (reduceMotion) ViraMotion.reduced() else ViraMotion.emphasis()) }
        if (count != null && !reduceMotion) {
            delay(420)
            shownCount.animateTo(count.toFloat(), tween(500))
        }
    }
    // Amb TalkBack, la pantalla diu què has aconseguit en obrir-se.
    LaunchedEffect(Unit) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) {
            delay(600)
            view.announceForAccessibility(announcement)
        }
    }

    fun Modifier.reveal(after: Float) = alpha(((appear.value - after) / (1f - after)).coerceIn(0f, 1f))

    @Composable
    fun Eyebrow(text: String) = Text(
        text,
        style = ViraFont.callout,
        color = ink,
        textAlign = TextAlign.Center,
        // Només contorn: un farciment translúcid menjaria contrast al text.
        modifier = Modifier.reveal(0.16f).border(1.5.dp, ink, CircleShape)
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
    )

    @Composable
    fun CountBlock() = Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.reveal(0.28f)) {
        if (count != null) {
            Text(
                "${shownCount.value.roundToInt()}",
                style = ViraFont.timer,
                color = ink,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Text(
            caption,
            style = if (count == null) ViraFont.title else ViraFont.headline,
            color = ink,
            textAlign = TextAlign.Center,
        )
    }

    @Composable
    fun Message(text: String, modifier: Modifier = Modifier) =
        Text(text, style = ViraFont.headline, color = ink, textAlign = TextAlign.Center, modifier = modifier.reveal(0.44f))

    // Ple amb el color del text i l'etiqueta amb el del fons: la mateixa parella, invertida.
    @Composable
    fun ContinueButton() = Text(
        stringResource(R.string.streak_milestone_continue),
        style = ViraFont.headline,
        color = buttonLabel,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = ViraSpacing.l)
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .background(ink, CircleShape)
            .viraPressable(onClick = onContinue)
            .padding(vertical = ViraSpacing.m),
    )

    Box(modifier.fillMaxSize().background(ViraGradient.card(area))) {
        // El degradat a sota i no negre: el vídeo triga uns fotogrames a donar la primera imatge.
        if (playing != null) BackdropVideo(playing)
        // Llisca quan no hi cap (lletra al 200 % en un mòbil baix): el botó mai queda fora de la
        // pantalla. Quan hi cap, el contingut queda centrat entre dalt i el botó, com a l'iPhone.
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(ViraSpacing.screenMargin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (playing != null) {
                    val protects = Modifier.protectText(playing.protectsText, area)
                    val onTop = playing.textPlacement == ViraCelebrationBackdrop.TextPlacement.Top
                    // Tres franges: la xifra a dalt, Viri al mig (és el vídeo) i la resta a baix.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
                        modifier = protects.padding(top = ViraSpacing.s).padding(horizontal = ViraSpacing.m),
                    ) {
                        if (eyebrow != null) Box(Modifier.padding(bottom = ViraSpacing.xxs)) { Eyebrow(eyebrow) }
                        CountBlock()
                        if (onTop && message != null) Message(message, Modifier.padding(top = ViraSpacing.xs))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!onTop) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                                modifier = protects.padding(horizontal = ViraSpacing.m),
                            ) {
                                if (message != null) Message(message)
                                extra()
                            }
                        }
                        ContinueButton()
                    }
                } else {
                    Spacer(Modifier)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
                        modifier = Modifier.padding(horizontal = ViraSpacing.m),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                val s = if (reduceMotion) 1f else 0.6f + 0.4f * appear.value
                                scaleX = s; scaleY = s; alpha = appear.value.coerceIn(0f, 1f)
                            },
                        ) {
                            ViraCelebrationBurst(area = area)
                            hero()
                        }
                        if (eyebrow != null) Eyebrow(eyebrow)
                        CountBlock()
                        if (message != null) Message(message)
                        Box(Modifier.padding(top = ViraSpacing.s)) { extra() }
                    }
                    ContinueButton()
                }
            }
        }
        if (celebrationsEnabled) ViraConfettiBurst(Modifier.align(Alignment.Center), over = area)
    }
}

/// On acaba la part opaca del vídeo pujat, abans de l'esvaïment.
private const val VIDEO_FADE_START = 0.74f

/// El vídeo a sang, pujat i —si puja— amb la vora de baix esvaïda: el groc del vídeo no és
/// exactament el del degradat, i tallar-lo en sec hi marcaria una línia. L'esvaïment va amb la
/// imatge, per això es pinta abans del desplaçament.
@Composable
private fun BackdropVideo(backdrop: ViraCelebrationBackdrop) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val lift = maxHeight * backdrop.rise
        ViraAlphaVideo(
            backdrop.video,
            fill = true,
            isMuted = !backdrop.sound,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -lift.roundToPx()) }
                .then(
                    if (backdrop.rise > 0f) {
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.Black,
                                        VIDEO_FADE_START to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/// Manté la parella de colors validada encara que el vídeo canviï de llum (`CelebrationTextProtection`).
private fun Modifier.protectText(enabled: Boolean, area: ViraArea): Modifier = if (!enabled) this else composed {
    this.fillMaxWidth()
        .background(ViraColor.cardFill(area), RoundedCornerShape(ViraRadius.l))
        .padding(vertical = ViraSpacing.s)
}
