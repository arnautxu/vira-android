package cat.vira.android.features.focus

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import cat.vira.android.R
import cat.vira.android.core.data.model.FocusKind
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraAlphaVideo
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.tokens.viraSpec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/// Selector del mode (`FocusModeCarousel`): medallons que llisquen i s'enganxen al centre.
///
/// L'efecte visual depèn només de la pàgina assentada, mai de la geometria en viu del gest: dues
/// fonts de veritat es desincronitzaven. Crear un mode **no** és una posició del carrusel: el + viu
/// a la capçalera. Els veïns no porten text: són una pista del que hi ha al costat.
@Composable
fun FocusModeCarousel(modes: List<FocusMode>, selectedID: FocusModeID, side: Dp, onSelect: (FocusMode) -> Unit) {
    val selectedIndex = modes.indexOfFirst { it.id == selectedID }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = selectedIndex) { modes.size }
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    // Els veïns trepitgen el medalló un 16 %: prou petits per no competir, prou a prop per veure's.
    val pageWidth = side * 0.84f
    val latestModes by rememberUpdated(modes)
    val latestSelected by rememberUpdated(selectedID)

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            val mode = latestModes.getOrNull(page) ?: return@collect
            if (mode.id != latestSelected) onSelect(mode)
        }
    }
    // Un mode acabat de crear entra a la llista i queda triat alhora: el carrusel hi va.
    LaunchedEffect(selectedIndex, modes.size) {
        if (pager.currentPage != selectedIndex) {
            if (reduceMotion) pager.scrollToPage(selectedIndex) else pager.animateScrollToPage(selectedIndex)
        }
    }

    // Les captures no saben dibuixar un `HorizontalPager`: allà, el triat i els seus dos veïns, quiets
    // i al mateix lloc on el carrusel els deixaria.
    if (LocalInspectionMode.current) {
        Box(Modifier.fillMaxWidth().height(side), contentAlignment = Alignment.Center) {
            (-1..1).forEach { step ->
                modes.getOrNull(selectedIndex + step)?.let { mode ->
                    FocusMedallionButton(mode, side, step == 0, Modifier.offset(x = pageWidth * step)) {}
                }
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(side)) {
        val edge = ((maxWidth - pageWidth) / 2).coerceAtLeast(ViraSpacing.screenMargin)
        val mask = ViraColor.shadow
        val inspection = LocalInspectionMode.current
        HorizontalPager(
            state = pager,
            pageSize = PageSize.Fixed(pageWidth),
            contentPadding = PaddingValues(horizontal = edge),
            modifier = Modifier
                .fillMaxWidth()
                .height(side)
                // Els veïns es fonen contra les vores en comptes de quedar tallats en sec. Les
                // captures no saben compondre fora de pantalla: allà, sense fos.
                .then(
                    if (inspection) Modifier else Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.horizontalGradient(
                                    0f to mask.copy(alpha = 0f), 0.06f to mask, 0.94f to mask, 1f to mask.copy(alpha = 0f),
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ),
        ) { page ->
            val mode = modes[page]
            val selected = page == pager.settledPage && mode.id == selectedID
            FocusMedallionButton(mode, side, selected) {
                scope.launch { if (reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
                onSelect(mode)
            }
        }
    }
}

@Composable
private fun <T> rememberUpdated(value: T) = androidx.compose.runtime.rememberUpdatedState(value)

@Composable
private fun FocusMedallionButton(mode: FocusMode, side: Dp, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.5f, viraSpec(ViraMotion.standard(), decorative = true), label = "medallion")
    val opacity by animateFloatAsState(if (selected) 1f else 0.62f, viraSpec(ViraMotion.standard(), decorative = true), label = "opacity")
    val disc = FocusMedallionMetrics.discSide(side)
    val label = mode.label()
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    Box(modifier.size(side * 0.84f, side), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(side)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }
                .clip(CircleShape)
                .viraPressable(onClick = onClick)
                .semantics {
                    contentDescription = label
                    this.selected = selected
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            FocusDisc(disc)
            Box(Modifier.size(disc).clip(CircleShape), contentAlignment = Alignment.Center) {
                // Només el medalló triat es mou: tres escenes movent-se alhora farien del selector
                // el més sorollós d'una pantalla que existeix per concentrar-se.
                val loop = FocusPictogram.loop(mode.pictogram)
                if (selected && !reduceMotion && loop != null) {
                    ViraAlphaVideo(loop.first, Modifier.size(disc), loopStart = loop.second, fill = true)
                } else {
                    ViraPictogram(
                        FocusPictogram.drawable(mode.pictogram),
                        size = if (mode.isScene) disc * 1.02f else disc * 0.56f,
                        modifier = Modifier.wrapContentSize(unbounded = true),
                    )
                }
            }
        }
    }
}

/// El temps de la sessió (`FocusTimeStepper`): −, la xifra i +, de 5 en 5. Cada toc encén o apaga
/// un bloc de l'esfera. Un perfil porta una sola durada, i aquí només es llegeix.
///
/// Per a TalkBack és **un sol element ajustable**: el nom, els minuts, i amunt o avall.
@Composable
fun FocusTimeStepper(mode: FocusMode, minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val value = stringResource(R.string.focus_minutes, minutes)
    val caption = stringResource(if (mode.kind == FocusKind.FOCUS) R.string.focus_panel_time_focus else R.string.focus_panel_time_break)
    val range = mode.minuteRange
    val stacked = LocalViraEnvironment.current.isAccessibilitySize
    val texts: @Composable (Modifier) -> Unit = { slot ->
        Column(slot, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xxs)) {
            Text(value, style = ViraFont.title.copy(fontFeatureSettings = "tnum"), color = ViraColor.textPrimary, textAlign = TextAlign.Center)
            Text(caption, style = ViraFont.footnote, color = ViraColor.textSecondary, textAlign = TextAlign.Center)
        }
    }
    val minus: @Composable () -> Unit = { StepButton(FocusGlyphKind.Minus, FocusTimeStep.next(minutes, -FocusTimeStep.STEP, range), onChange) }
    val plus: @Composable () -> Unit = { StepButton(FocusGlyphKind.Plus, FocusTimeStep.next(minutes, FocusTimeStep.STEP, range), onChange) }
    val container = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = ViraTouch.comfortable)
        .background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.m))
        .padding(ViraSpacing.xs)
        .clearAndSetSemantics {
            contentDescription = caption
            stateDescription = value
            if (range != null) {
                progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), range.first.toFloat()..range.last.toFloat(), (range.last - range.first) / FocusTimeStep.STEP - 1)
                setProgress { target ->
                    val delta = if (target > minutes) FocusTimeStep.STEP else -FocusTimeStep.STEP
                    FocusTimeStep.next(minutes, delta, range)?.let(onChange) != null
                }
            }
        }
    if (stacked) {
        // A lletra gran, els botons baixen sota la xifra: entre dos cercles, el text es partia.
        Column(container, verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            texts(Modifier.fillMaxWidth())
            if (range != null) {
                Row(Modifier.fillMaxWidth()) { minus(); Spacer(Modifier.weight(1f)); plus() }
            }
        }
    } else {
        Row(container, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            if (range != null) minus()
            texts(Modifier.weight(1f))
            if (range != null) plus()
        }
    }
}

@Composable
private fun StepButton(glyph: FocusGlyphKind, next: Int?, onChange: (Int) -> Unit) {
    val enabled = next != null
    Box(
        Modifier
            .size(ViraTouch.minimum)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(ViraColor.accent(ViraArea.Focus), CircleShape)
            .viraPressable(enabled = enabled) { next?.let(onChange) },
        contentAlignment = Alignment.Center,
    ) {
        FocusGlyph(glyph, ViraColor.onAccent)
    }
}

/// Els passos del − i el +, separats de la vista perquè es puguin provar sols.
object FocusTimeStep {
    const val STEP = 5

    /// El pas següent, o `null` si no n'hi ha. Un valor fora de la graella de 5 (el botó de prova,
    /// un perfil antic) primer s'arrodoneix al bloc més proper en la direcció demanada.
    fun next(minutes: Int, delta: Int, range: IntRange?): Int? {
        range ?: return null
        val snapped = if (delta > 0) (minutes / STEP) * STEP + STEP else ((minutes + STEP - 1) / STEP) * STEP - STEP
        val value = if (minutes % STEP == 0) minutes + delta else snapped
        val clamped = value.coerceIn(range.first, range.last)
        return if (clamped == minutes) null else clamped
    }
}
