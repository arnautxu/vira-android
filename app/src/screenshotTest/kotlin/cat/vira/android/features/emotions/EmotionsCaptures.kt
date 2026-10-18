package cat.vira.android.features.emotions

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.app.EmotionsSection
import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.design.ViraCapture
import cat.vira.android.core.design.ViraScreenCaptures
import cat.vira.android.core.design.ViraTheme
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDateTime
import java.time.ZoneOffset

/// Les pantalles d'Emocions en clar, fosc i lletra al 200 % (castellà), a l'ample d'un iPhone, per
/// posar-les al costat de les captures de l'iPhone. Data i zona fixes: una captura que canviés cada
/// dia no serviria de prova de regressió.

private val zone = ZoneOffset.UTC
private val now = LocalDateTime.of(2026, 10, 14, 18, 30).toInstant(zone)

private fun at(daysAgo: Long, hour: Int, minute: Int = 0) =
    LocalDateTime.of(2026, 10, 14, hour, minute).minusDays(daysAgo).toInstant(zone)

private val sample = listOf(
    EmotionEntry(emotionRaw = Emotion.CONTENT.raw, intensityRaw = 3, note = "Partit guanyat amb l'equip.", createdAt = at(0, 17, 40)),
    EmotionEntry(emotionRaw = Emotion.TIRED.raw, createdAt = at(0, 13, 5)),
    EmotionEntry(emotionRaw = Emotion.ANXIOUS.raw, intensityRaw = 2, createdAt = at(0, 8, 15)),
    EmotionEntry(emotionRaw = Emotion.SAD.raw, intensityRaw = 4, createdAt = at(1, 21, 0)),
    EmotionEntry(emotionRaw = Emotion.CONTENT.raw, createdAt = at(3, 10, 30)),
    EmotionEntry(emotionRaw = Emotion.ANGRY.raw, intensityRaw = 5, note = "No vull parlar-ne ara.", createdAt = at(5, 19, 0)),
    EmotionEntry(emotionRaw = Emotion.ANXIOUS.raw, createdAt = at(12, 9, 0)),
)

@Composable
private fun Screen(content: @Composable () -> Unit) = ViraCapture { ViraTheme(reduceMotion = true) { content() } }

@PreviewTest @ViraScreenCaptures @Composable
fun CheckIn() = Screen {
    EmotionsTab(EmotionsSection.entries, EmotionsSection.CheckIn, {}) {
        EmotionCheckInContent(Emotion.primary, hasSaveError = false, onSelect = {}, onOther = {}, onCustomize = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun DetailNoIntensity() = Screen {
    EmotionSheetPreviewFrame {
        EmotionDetailContent(EmotionCheckInDraft(Emotion.ANXIOUS), {}, saveFailed = false, isSaving = false, onCancel = {}, onHelpNow = {}, onConfirm = {})
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun DetailWithSupport() = Screen {
    EmotionSheetPreviewFrame {
        EmotionDetailContent(
            EmotionCheckInDraft(Emotion.SAD, EmotionIntensity.VERY_MUCH, "Dia difícil"),
            {}, saveFailed = false, isSaving = false, onCancel = {}, onHelpNow = {}, onConfirm = {},
        )
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun CustomizeFaces() = Screen {
    EmotionSheetPreviewFrame { EmotionPrimaryPickerContent(Emotion.primary, {}, {}, {}) }
}

@PreviewTest @ViraScreenCaptures @Composable
fun OtherEmotion() = Screen {
    EmotionSheetPreviewFrame { EmotionOtherPickerContent(EmotionPreferences.other(Emotion.primary), {}, {}) }
}

@PreviewTest @ViraScreenCaptures @Composable
fun HistoryWeek() = Screen {
    EmotionsTab(EmotionsSection.entries, EmotionsSection.History, {}) {
        EmotionHistoryContent(sample, EmotionHistoryRange.WEEK, {}, {}, now = now, zone = zone)
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun HistoryMonth() = Screen {
    EmotionsTab(EmotionsSection.entries, EmotionsSection.History, {}) {
        EmotionHistoryContent(sample, EmotionHistoryRange.MONTH, {}, {}, now = now, zone = zone)
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun HistoryEmpty() = Screen {
    EmotionsTab(EmotionsSection.entries, EmotionsSection.History, {}) {
        EmotionHistoryContent(emptyList(), EmotionHistoryRange.WEEK, {}, {}, now = now, zone = zone)
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun Mindfulness() = Screen {
    EmotionsTab(EmotionsSection.entries, EmotionsSection.Mindfulness, {}) { MindfulnessContent({}, {}) }
}

@PreviewTest @ViraScreenCaptures @Composable
fun MindfulnessCategoryAnxiety() = Screen {
    Column {
        PushedHeader(stringResource(MindfulnessCategory.ANXIETY.title)) {}
        MindfulnessCategoryContent(MindfulnessCategory.ANXIETY, {}, Modifier.weight(1f))
    }
}

/// Un guió curt amb el format dels de l'iPhone: dos passos amb nom i una respiració a mig fer.
private val script = MindfulnessScript(
    id = "senses", language = "ca", duration = 168.0, steps = 2,
    segments = listOf(
        MindfulnessScript.Segment(0.0, 8.0, MindfulnessScript.Kind.LINE, text = "mindfulness.senses.line1"),
        MindfulnessScript.Segment(8.0, 30.0, MindfulnessScript.Kind.LINE, step = 0, stepTitle = "mindfulness.senses.step1", text = "mindfulness.senses.line2"),
        MindfulnessScript.Segment(30.0, 34.0, MindfulnessScript.Kind.BREATH, step = 1, stepTitle = "mindfulness.senses.step2", text = "help.breathe.inhale", phase = MindfulnessScript.Phase.INHALE),
        MindfulnessScript.Segment(34.0, 168.0, MindfulnessScript.Kind.LINE, step = 1, stepTitle = "mindfulness.senses.step2", text = "mindfulness.senses.line3"),
    ),
)

@PreviewTest @ViraScreenCaptures @Composable
fun MindfulnessRunning() = Screen {
    Column {
        PushedHeader(mindfulnessString(MindfulnessExercise.SENSES.titleKey), trailing = stringResource(R.string.mindfulness_stop) to {}) {}
        MindfulnessSessionContent(
            MindfulnessExercise.SENSES, script, currentTime = 20.0, isPlaying = true, isFinished = false, failed = false,
            onToggle = {}, onAnother = {}, onCheckIn = {}, onFocus = {}, modifier = Modifier.weight(1f),
        )
    }
}

@PreviewTest @ViraScreenCaptures @Composable
fun MindfulnessDone() = Screen {
    MindfulnessSessionContent(
        MindfulnessExercise.SENSES, script, currentTime = 168.0, isPlaying = false, isFinished = true, failed = false,
        onToggle = {}, onAnother = {}, onCheckIn = {}, onFocus = {},
    )
}
