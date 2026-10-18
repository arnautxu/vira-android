package cat.vira.android.features.emotions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.EmotionsSection
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraNavigator
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Pestanya Emocions: check-in, historial i mindfulness (`EmotionsView` de l'iPhone), triats amb
/// el selector visible de dalt, amb el nom a cada cara.
///
/// El check-in és el nucli i no es pot apagar; Mindfulness sí (Ajustos → Mòduls), i si la cara
/// triada era aquesta, es torna al check-in.
@Composable
fun EmotionsScreen() {
    val navigator = LocalViraNavigator.current
    val preferences = LocalViraBackend.current.preferences
    val mindfulnessEnabled by rememberPreference(preferences, MODULE_MINDFULNESS_ENABLED, true)
    val section = navigator.emotionsSection
    val available = EmotionsSection.entries.filter { it != EmotionsSection.Mindfulness || mindfulnessEnabled }
    val resolved = if (section in available) section else EmotionsSection.CheckIn

    var category by rememberSaveable { mutableStateOf<MindfulnessCategory?>(null) }
    var exercise by rememberSaveable { mutableStateOf<MindfulnessExercise?>(null) }
    // Si una altra pantalla (Ajuda ara, un avís) porta a una altra cara, la pila de Mindfulness
    // se'n va: si no, en tornar es veuria la categoria i no el que s'ha demanat.
    LaunchedEffect(resolved) {
        if (resolved != EmotionsSection.Mindfulness) {
            exercise = null
            category = null
        }
    }

    // Mindfulness empeny dins de la pestanya: categoria i, a sobre, la sessió.
    exercise?.let { current ->
        BackHandler { exercise = null }
        MindfulnessSession(
            current,
            onClose = { exercise = null },
            onCheckIn = {
                exercise = null
                category = null
                navigator.emotionsSection = EmotionsSection.CheckIn
            },
            onFocus = {
                exercise = null
                navigator.startFocus()
            },
        )
        return
    }
    category?.let { current ->
        BackHandler { category = null }
        Column(Modifier.fillMaxSize()) {
            // El títol gran ja és a la pàgina (`ViraPageTitle`): la barra només porta l'enrere.
            PushedHeader("") { category = null }
            MindfulnessCategoryContent(current, onExercise = { exercise = it }, modifier = Modifier.weight(1f))
        }
        return
    }

    EmotionsTab(available, resolved, onSelect = { navigator.emotionsSection = it }) { current ->
        when (current) {
            EmotionsSection.CheckIn -> EmotionCheckInScreen()
            EmotionsSection.History -> EmotionHistoryScreen()
            EmotionsSection.Mindfulness -> MindfulnessContent(
                onBreathe = navigator::openBreathe,
                onCategory = { category = it },
            )
        }
    }
}

/// El selector i la cara triada, amb el lliscament cap al costat on és la cara nova.
@Composable
internal fun EmotionsTab(
    available: List<EmotionsSection>,
    selected: EmotionsSection,
    onSelect: (EmotionsSection) -> Unit,
    content: @Composable (EmotionsSection) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // Navegació compartida, no part de l'escena de la cara activa. Amb el to de la capçalera
        // de sobre, perquè no quedi una franja neutra entre la capçalera i el fons rosa.
        ViraSectionSwitcher(
            area = ViraArea.Emotions,
            items = available.map { it.item() },
            selectedKey = selected.name,
            onSelect = { key -> onSelect(EmotionsSection.valueOf(key)) },
            showsAllLabels = true,
            modifier = Modifier
                .background(ViraColor.surfaceShift(ViraArea.Emotions))
                .padding(horizontal = ViraSpacing.screenMargin, vertical = ViraSpacing.xxs),
        )
        val reduceMotion = LocalViraEnvironment.current.reduceMotion
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                } else {
                    val forward = targetState.ordinal >= initialState.ordinal
                    (slideInHorizontally(ViraMotion.entrance()) { if (forward) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally(ViraMotion.entrance()) { if (forward) -it else it } + fadeOut())
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "emotionsSection",
        ) { current -> content(current) }
    }
}

@Composable
private fun EmotionsSection.item(): ViraSectionItem = when (this) {
    EmotionsSection.CheckIn -> ViraSectionItem(name, stringResource(R.string.emotions_section_now), R.drawable.viri_phone)
    // `journal` és el més proper del catàleg a "mirar enrere com t'has sentit".
    EmotionsSection.History -> ViraSectionItem(name, stringResource(R.string.emotions_history_title), R.drawable.picto_journal)
    // El retall sense fons: l'escena sencera no encaixa amb els altres pictogrames.
    EmotionsSection.Mindfulness -> ViraSectionItem(name, stringResource(R.string.emotions_mindfulness_title), R.drawable.viri_meditating_cutout)
}

/// La barra d'una pantalla empesa dins de la pestanya (`navigationTitle` en línia amb el botó
/// d'enrere). El gest d'enrere del sistema fa el mateix.
@Composable
internal fun PushedHeader(title: String, trailing: Pair<String, () -> Unit>? = null, onBack: () -> Unit) {
    val back = stringResource(R.string.action_back)
    Box(
        Modifier
            .fillMaxWidth()
            .background(ViraColor.surfaceShift(ViraArea.Emotions))
            .padding(horizontal = ViraSpacing.xs),
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .heightIn(min = ViraTouch.minimum)
                .viraPressable(onClickLabel = back, onClick = onBack)
                .semantics { contentDescription = back }
                .padding(horizontal = ViraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmotionGlyphIcon(
                EmotionGlyph.Chevron, ViraColor.accent(ViraArea.Emotions), 18.dp * LocalDensity.current.fontScale,
                Modifier.rotate(180f),
            )
        }
        Text(
            title,
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.55f).semantics { heading() },
            textAlign = TextAlign.Center,
        )
        if (trailing != null) {
            ViraActionButton(
                trailing.first, trailing.second,
                modifier = Modifier.align(Alignment.CenterEnd),
                hierarchy = ViraActionHierarchy.Textual, area = ViraArea.Emotions,
            )
        }
    }
}

/// La base de dades de l'app, per escriure-hi i llegir-ne amb `ViraStore`.
@Composable
internal fun rememberEmotionStore(): ViraStore {
    val db = LocalViraBackend.current.db
    return remember(db) { ViraStore(db) }
}

/// `ViraPreferences.moduleMindfulnessEnabled` de l'iPhone. Encès de fàbrica.
const val MODULE_MINDFULNESS_ENABLED = "settings.moduleMindfulnessEnabled"
