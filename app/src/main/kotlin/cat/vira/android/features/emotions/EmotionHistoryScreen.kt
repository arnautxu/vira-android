package cat.vira.android.features.emotions

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cat.vira.android.R
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.ViraTheme
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.components.viraTabBarClearance
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.resources.viraPluralString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/// Historial emocional: què has registrat i quan (`EmotionHistoryView` de l'iPhone).
///
/// Escena i període a dalt, una cara per dia, fitxes per emoció i un mòdul per dia. Cap resum
/// interpretatiu, cap tendència, cap consell: Vira no diagnostica ni llegeix les notes.
///
/// - **No puntua els dies.** No hi ha "bona setmana" ni cap eix de millor a pitjor.
/// - **No reclama constància.** Un dia sense registres es dibuixa buit i prou.
/// - **Esborrar sí, i sense modal**: mantenir premut un registre (o l'acció de TalkBack). No toca
///   ni l'XP, ni les monedes, ni la ratxa: el dia ja va comptar quan es va registrar.
///
/// La línia de son de l'iPhone (HealthKit) no hi és: a Android no hi ha cap font de son connectada.
@Composable
fun EmotionHistoryScreen(modifier: Modifier = Modifier) {
    val store = rememberEmotionStore()
    val scope = rememberCoroutineScope()
    val flow = remember(store) { store.db.emotionEntries().observeAll() }
    val entries by flow.collectAsState(initial = null)
    var range by rememberSaveable { mutableStateOf(EmotionHistoryRange.WEEK) }
    // «Avui» es torna a mirar en tornar a l'app i a mitjanit: si no, una pantalla que ha passat la
    // nit oberta seguiria acabant en el dia d'ahir.
    var now by remember { mutableStateOf(Instant.now()) }
    LifecycleResumeEffect(Unit) {
        now = Instant.now()
        onPauseOrDispose {}
    }
    LaunchedEffect(now) {
        val zone = ZoneId.systemDefault()
        val midnight = now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        delay(Duration.between(Instant.now(), midnight).toMillis().coerceAtLeast(0) + 1_000)
        now = Instant.now()
    }
    entries?.let { all ->
        EmotionHistoryContent(
            entries = all,
            range = range,
            onRange = { range = it },
            onDelete = { entry -> scope.launch { store.delete(entry) } },
            modifier = modifier,
            now = now,
        )
    } ?: Box(modifier.fillMaxSize().viraSectionBackground(ViraArea.Emotions))
}

@Composable
internal fun EmotionHistoryContent(
    entries: List<EmotionEntry>,
    range: EmotionHistoryRange,
    onRange: (EmotionHistoryRange) -> Unit,
    onDelete: (EmotionEntry) -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val history = remember(entries, range, now, zone) { EmotionHistory(entries, range, now, zone) }
    val today = now.atZone(zone).toLocalDate()
    Column(
        modifier = modifier
            .fillMaxSize()
            .viraSectionBackground(ViraArea.Emotions)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ViraSpacing.screenMargin)
            .padding(top = ViraSpacing.xs, bottom = viraTabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.sectionGap),
    ) {
        if (entries.isEmpty()) {
            // Encara no hi ha cap registre enlloc.
            HistoryEmpty(R.string.emotions_history_empty_message, Modifier.padding(top = ViraSpacing.xxl))
            return@Column
        }
        EmotionHistoryHero(history, range, onRange, now, zone)
        if (history.isEmpty) {
            // Hi ha registres, però no en aquests dies. No és cap fracàs.
            HistoryEmpty(R.string.emotions_history_empty_range)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                Text(
                    stringResource(R.string.emotions_history_top_title),
                    style = ViraFont.headline,
                    color = ViraColor.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                EmotionHistoryTiles(history.counts)
            }
            history.daysWithEntries.forEach { day -> EmotionHistoryDayModule(day, onDelete, today) }
        }
    }
}

@Composable
private fun HistoryEmpty(message: Int, modifier: Modifier = Modifier) {
    ViraEmptyState(
        title = stringResource(R.string.emotions_empty_title),
        message = stringResource(message),
        modifier = modifier,
        icon = { ViraPictogram(R.drawable.picto_journal, size = 56.dp) },
    )
}

/// Capçalera de l'historial: escena de Viri, el període i una cara per dia (`EmotionHistoryHero`).
/// El text sempre a la targeta blanca que trepitja l'escena, mai sobre la il·lustració.
@Composable
private fun EmotionHistoryHero(
    history: EmotionHistory,
    range: EmotionHistoryRange,
    onRange: (EmotionHistoryRange) -> Unit,
    now: Instant,
    zone: ZoneId,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val artHeight = 160.dp * LocalDensity.current.fontScale
    Box(Modifier.fillMaxWidth().clip(shape).background(ViraColor.surfaceElevated, shape)) {
        Image(
            painterResource(EmotionHistoryArt.drawable(now.atZone(zone).hour)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(artHeight),
        )
        RangeSwitch(range, onRange, Modifier.align(Alignment.TopEnd).padding(ViraSpacing.xs))
        Column(Modifier.padding(top = artHeight - ViraSpacing.xl)) {
            HeroSummary(history, range, zone, Modifier.padding(horizontal = ViraSpacing.s))
            EmotionHistoryDayGrid(
                history, range, now.atZone(zone).toLocalDate(),
                Modifier.padding(horizontal = ViraSpacing.s).padding(top = ViraSpacing.m, bottom = ViraSpacing.m),
            )
        }
    }
}

@Composable
private fun HeroSummary(history: EmotionHistory, range: EmotionHistoryRange, zone: ZoneId, modifier: Modifier) {
    val context = LocalContext.current
    val count = viraPluralString(R.plurals.emotions_history_count, history.entries.size, null, history.entries.size)
    // Migdia i no mitjanit: `formatDateRange` llegeix una fi a mitjanit com a exclosa i
    // diria "8–13" per a una setmana que acaba avui, dia 14.
    val first = history.days.first().date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    val last = history.days.last().date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    // "9 registres · 5–11 oct.": el període en dates, que s'entén d'un cop d'ull sense comptar.
    val locale = viraLocale()
    val interval = DateUtils.formatDateRange(
        context, java.util.Formatter(StringBuilder(), locale), first, last,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
        zone.id,
    ).toString()
    val shape = RoundedCornerShape(ViraRadius.m)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = ViraColor.textPrimary.copy(alpha = 0.10f), spotColor = ViraColor.textPrimary.copy(alpha = 0.10f))
            .background(ViraColor.surfaceElevated, shape)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated),
    ) {
        Text(
            stringResource(range.heroTitle),
            style = ViraFont.title,
            color = ViraColor.onSurface(ViraArea.Emotions),
            modifier = Modifier.semantics { heading() },
        )
        Text("$count · $interval", style = ViraFont.footnote, color = ViraColor.textSecondary)
    }
}

/// 7 dies / 30 dies, a sobre de la il·lustració. Dos botons sobre una càpsula fosca i no un
/// control segmentat: és el que garanteix el contrast sigui quina sigui l'escena. Sempre en
/// l'aparença fosca, com a l'iPhone.
@Composable
private fun RangeSwitch(range: EmotionHistoryRange, onRange: (EmotionHistoryRange) -> Unit, modifier: Modifier) {
    val environment = LocalViraEnvironment.current
    ViraTheme(darkTheme = true, reduceMotion = environment.reduceMotion, increasedContrast = environment.increasedContrast) {
        Row(modifier.background(ViraColor.background.copy(alpha = 0.62f), CircleShape)) {
            EmotionHistoryRange.entries.forEach { option ->
                val isSelected = option == range
                Box(
                    Modifier
                        .heightIn(min = ViraTouch.minimum)
                        .padding(ViraSpacing.xxs)
                        .then(if (isSelected) Modifier.background(ViraColor.surfaceElevated, CircleShape) else Modifier)
                        .viraPressable(role = Role.Tab) { onRange(option) }
                        .semantics { selected = isSelected }
                        .padding(horizontal = ViraSpacing.s),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(option.title),
                        style = ViraFont.footnote.copy(fontWeight = FontWeight.Medium),
                        color = if (isSelected) ViraColor.textPrimary else ViraColor.onDarkArtwork,
                    )
                }
            }
        }
    }
}
