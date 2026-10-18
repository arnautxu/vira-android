package cat.vira.android.features.emotions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.design.components.ViraInlineError
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import kotlinx.coroutines.launch

/// Check-in emocional: la pantalla més important del producte (`EmotionCheckInView` de l'iPhone).
///
/// Dona vocabulari visual a algú que sovint no sap nomenar el que sent. Tocar una cara obre el
/// detall; el registre **només existeix** quan es confirma dins d'aquell full. Intensitat i nota
/// viuen en un valor temporal fins aleshores, i tancar el full no desa res.
///
/// `onRecorded`: avís que s'acaba de confirmar un registre i el full ja s'ha tancat. El fa servir
/// la primera acció del flux d'entrada, que presenta **aquesta mateixa pantalla**.
@Composable
fun EmotionCheckInScreen(
    modifier: Modifier = Modifier,
    onRecorded: (() -> Unit)? = null,
) {
    val preferences = LocalViraBackend.current.preferences
    val store = rememberEmotionStore()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var primary by remember { mutableStateOf(EmotionPreferences.primary(preferences)) }
    var draft by remember { mutableStateOf<EmotionCheckInDraft?>(null) }
    var showsOther by remember { mutableStateOf(false) }
    var showsCustomization by remember { mutableStateOf(false) }
    var hasSaveError by remember { mutableStateOf(false) }

    fun open(emotion: Emotion) {
        if (draft != null) return
        hasSaveError = false
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        draft = EmotionCheckInDraft(emotion)
    }

    EmotionCheckInContent(
        primary = primary,
        hasSaveError = hasSaveError,
        onSelect = ::open,
        onOther = { showsOther = true },
        onCustomize = { showsCustomization = true },
        modifier = modifier,
    )

    draft?.let { current ->
        EmotionDetailSheet(
            initial = current,
            onDismiss = { draft = null },
            onConfirm = { value, finished ->
                scope.launch {
                    val saved = runCatching {
                        ViraEmotionCheckIn.record(store, value.emotion, value.intensity, value.normalizedNote)
                    }.isSuccess
                    hasSaveError = !saved
                    if (saved) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        draft = null
                        onRecorded?.invoke()
                    }
                    finished(saved)
                }
            },
        )
    }
    if (showsOther) {
        EmotionOtherPickerSheet(
            emotions = EmotionPreferences.other(primary),
            onDismiss = { showsOther = false },
            onSelect = { showsOther = false; open(it) },
        )
    }
    if (showsCustomization) {
        EmotionPrimaryPickerSheet(
            initialSelection = primary,
            onDismiss = { showsCustomization = false },
            onSave = { selection ->
                if (EmotionPreferences.setPrimary(selection, preferences)) primary = EmotionPreferences.primary(preferences)
                showsCustomization = false
            },
        )
    }
}

/// La pantalla sense estat, perquè les captures la puguin pintar tal com és.
@Composable
internal fun EmotionCheckInContent(
    primary: List<Emotion>,
    hasSaveError: Boolean,
    onSelect: (Emotion) -> Unit,
    onOther: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val other = EmotionPreferences.other(primary)
    Column(
        modifier = modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(bottom = viraTabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.emotions_breathe),
            style = ViraFont.callout,
            color = ViraColor.textSecondary,
            textAlign = TextAlign.Center,
        )
        CustomizeAction(onCustomize)
        if (hasSaveError) {
            ViraInlineError(stringResource(R.string.emotions_save_error), Modifier.fillMaxWidth())
        }
        EmotionFaceGrid(
            count = primary.size + if (other.isEmpty()) 0 else 1,
        ) { index, size ->
            if (index < primary.size) {
                EmotionButton(primary[index], size) { onSelect(primary[index]) }
            } else {
                OtherEmotionButton(size, onOther)
            }
        }
    }
}

@Composable
private fun CustomizeAction(onClick: () -> Unit) {
    val hint = stringResource(R.string.emotions_customize_hint)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ViraTouch.minimum)
            .viraPressable(onClickLabel = hint, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.labelGap),
    ) {
        val ink = ViraColor.accent(ViraArea.Emotions)
        EmotionGlyphIcon(EmotionGlyph.Sliders, ink, 18.dp * LocalDensity.current.fontScale)
        Text(stringResource(R.string.emotions_customize), style = ViraFont.callout, color = ink)
    }
}

/// Graella que reflueix en lloc de tres columnes fixes (`columns` de l'iPhone).
///
/// La columna **no baixa mai** del diàmetre de la cara, així que la graella passa sola a dues i a
/// una columna quan la lletra creix. I el mínim tampoc baixa d'**un terç de l'ample**: una cara
/// petita deixaria entrar-ne una quarta, i la graella deixaria de ser de 3 × 3.
@Composable
internal fun EmotionFaceGrid(count: Int, cell: @Composable (index: Int, size: Dp) -> Unit) {
    val scaled = ViraTouch.emotion * LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = ViraSpacing.xs
        val face = minOf(scaled, maxWidth)
        val minimum = maxOf(face, (maxWidth - gap * 2) / 3)
        val columns = ((maxWidth + gap) / (minimum + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.l)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                    row.forEach { index -> Column(Modifier.weight(1f)) { cell(index, face) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/// Cara emocional. Objectiu tàctil de 88 dp, molt per sobre del mínim: és l'acció més freqüent
/// de l'app i es fa en moments de desregulació, on la precisió del dit baixa.
@Composable
internal fun EmotionButton(emotion: Emotion, size: Dp, onSelect: () -> Unit) {
    val name = stringResource(emotion.label)
    FaceButton(
        name = name,
        hint = stringResource(R.string.emotions_checkin_face_hint),
        onClick = onSelect,
    ) { EmotionFace(emotion, size) }
}

/// Entrada al vocabulari que no cal tenir sempre davant. No és una emoció ni crea un registre.
@Composable
internal fun OtherEmotionButton(size: Dp, onClick: () -> Unit) {
    FaceButton(
        name = stringResource(R.string.emotions_other),
        hint = stringResource(R.string.emotions_other_hint),
        onClick = onClick,
    ) {
        Box(
            Modifier.size(size).background(ViraGradient.card(ViraArea.Emotions), CircleShape),
            contentAlignment = Alignment.Center,
        ) { EmotionGlyphIcon(EmotionGlyph.Ellipsis, ViraColor.onCard(ViraArea.Emotions), size * 0.34f) }
    }
}

@Composable
private fun FaceButton(name: String, hint: String, onClick: () -> Unit, face: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .viraPressable(onClickLabel = hint, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
    ) {
        face()
        // El to fosc d'Emocions, no el rosa clar: el nom és el que desambigua cares que
        // s'assemblen, i s'ha de poder llegir.
        Text(name, style = ViraFont.headline, color = ViraColor.onCard(ViraArea.Emotions), textAlign = TextAlign.Center)
    }
}

/// El check-in sencer com a primera acció del flux d'entrada (`EntryFirstSessionSteps` de
/// l'iPhone): la mateixa pantalla, amb «Tanca» a dalt a la dreta. `onRecorded` arriba un cop
/// confirmat el registre; `onClose`, si es tanca sense registrar res.
@Composable
fun EmotionEntryCheckIn(onRecorded: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Emotions)) {
        EmotionCheckInScreen(Modifier.statusBarsPadding(), onRecorded = onRecorded)
        Text(
            stringResource(R.string.action_close),
            style = ViraFont.callout.copy(fontWeight = FontWeight.SemiBold),
            color = ViraColor.onCard(ViraArea.Emotions),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = ViraSpacing.xs)
                .heightIn(min = ViraTouch.minimum)
                .viraPressable(onClick = onClose)
                .padding(horizontal = ViraSpacing.m)
                .wrapContentHeight(),
        )
    }
}
