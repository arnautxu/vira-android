package cat.vira.android.features.add

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.plan.DayPeriod
import cat.vira.android.features.plan.PlanModules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// Què es pot afegir des del full «Afegir» (`ViraAddKind`). Les tres, blaves com el Pla: és on
/// acaben. El botó d'acció segueix sent lila.
enum class ViraAddKind(@StringRes val title: Int, val pictogram: String) {
    Task(R.string.add_kind_task, ViraPictogramName.planWrite),
    Habit(R.string.add_kind_habit, ViraPictogramName.habits),
    Medication(R.string.add_kind_medication, ViraPictogramName.medication),
}

/// Un sol full per crear coses (`ViraAddSheet`, Arnau, 2026-10-18: «tot ràpid, top design,
/// gamificat»). Tots els «+» obren aquest full, a la cara que toca segons d'on vinguis, i les tres
/// cares es canvien amb el selector de sempre. Cada cara fa **una pregunta cada vegada**, i a
/// partir del segon pas el selector se'n va. Editar no passa per aquí: els editors d'una pàgina
/// del Pla es queden.
///
/// Hàbits i medicació només surten si el mòdul és encès, excepte si s'hi arriba des de la seva
/// pròpia pantalla. Premium i el candau de l'iPhone no hi són: l'APK és una beta i tot és obert.
@Composable
fun ViraAddSheet(
    onDismiss: () -> Unit,
    kind: ViraAddKind = ViraAddKind.Task,
    initialDay: LocalDate? = null,
    initialPeriod: DayPeriod? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val backend = LocalViraBackend.current
        val habitsEnabled by rememberPreference(backend.preferences, PlanModules.HABITS, true)
        val medicationEnabled by rememberPreference(backend.preferences, PlanModules.MEDICATION, true)
        AddSheetContent(
            initialKind = kind,
            actions = rememberAddSheetActions(),
            onDismiss = onDismiss,
            initialDay = initialDay,
            initialPeriod = initialPeriod,
            habitsEnabled = habitsEnabled,
            medicationEnabled = medicationEnabled,
        )
    }
}

/// El full sense la finestra ni les dades: el que fan servir les captures.
@Composable
fun AddSheetContent(
    initialKind: ViraAddKind,
    actions: AddSheetActions,
    onDismiss: () -> Unit,
    initialDay: LocalDate? = null,
    initialPeriod: DayPeriod? = null,
    habitsEnabled: Boolean = true,
    medicationEnabled: Boolean = true,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var showsSwitcher by remember { mutableStateOf(true) }
    val kinds = ViraAddKind.entries.filter {
        when (it) {
            ViraAddKind.Task -> true
            ViraAddKind.Habit -> habitsEnabled || initialKind == ViraAddKind.Habit
            ViraAddKind.Medication -> medicationEnabled || initialKind == ViraAddKind.Medication
        }
    }
    val items = kinds.map { ViraSectionItem(it.name, stringResource(it.title), ViraPictogramName.drawable(it.pictogram) ?: R.drawable.picto_flag) }
    val switcher = @Composable {
        if (kinds.size > 1 && showsSwitcher) {
            ViraSectionSwitcher(
                area = ViraArea.Plan,
                items = items,
                selectedKey = kind.name,
                onSelect = { kind = ViraAddKind.valueOf(it) },
                showsAllLabels = true,
                modifier = Modifier.padding(start = ViraSpacing.screenMargin, end = ViraSpacing.screenMargin, top = ViraSpacing.xxs, bottom = ViraSpacing.xs),
            )
        }
    }
    val onFirstStep: (Boolean) -> Unit = { showsSwitcher = it }
    Box(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Plan).statusBarsPadding().imePadding()) {
        when (kind) {
            ViraAddKind.Task -> TaskAddFlow(actions, onDismiss, switcher, onFirstStep, initialDay, initialPeriod, zone = zone, now = now)
            ViraAddKind.Habit -> HabitAddFlow(actions, onDismiss, switcher, onFirstStep, zone = zone, now = now)
            ViraAddKind.Medication -> MedicationAddFlow(actions, onDismiss, switcher, onFirstStep)
        }
    }
}

/// L'esquelet d'una cara: la capçalera, el selector (només al primer pas), la pregunta, que
/// llisca, i els botons de baix. Cada pas entra des del costat; amb Treu les animacions, es fon.
@Composable
fun <S> AddStepScaffold(
    step: S,
    topBar: @Composable () -> Unit,
    switcher: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    body: @Composable ColumnScope.(S) -> Unit,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    Column(Modifier.fillMaxSize()) {
        topBar()
        switcher()
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                } else {
                    (slideInHorizontally(ViraMotion.entrance()) { it } + fadeIn(ViraMotion.entrance())) togetherWith fadeOut(ViraMotion.quick())
                }
            },
            label = "addStep",
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = ViraSpacing.screenMargin, end = ViraSpacing.screenMargin, top = ViraSpacing.xs, bottom = ViraSpacing.m),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
            ) { body(current) }
        }
        actions()
    }
}
