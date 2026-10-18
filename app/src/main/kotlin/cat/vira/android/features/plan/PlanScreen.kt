package cat.vira.android.features.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.app.LocalViraNavigator
import cat.vira.android.app.PlanSection
import cat.vira.android.app.rememberPreference
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.LocalPresentCompletion
import cat.vira.android.core.design.components.LocalUndoCompletion
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.ViraTabTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraMotion
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch
import cat.vira.android.core.notifications.ViraNotifications

/// Les claus dels mòduls, les mateixes de l'iPhone (`ViraPreferences.moduleHabitsEnabled`).
object PlanModules {
    const val HABITS = "settings.moduleHabitsEnabled"
    const val MEDICATION = "settings.moduleMedicationEnabled"
}

/// Pestanya Pla (`PlanView`): calendari, hàbits i medicació, amb el selector visible dins de la
/// pestanya i el títol a la fila de les accions de cada cara.
///
/// Calendari és el nucli i no es pot desactivar: si la cara triada és d'un mòdul apagat des
/// d'Ajustos, s'hi torna. Canviar de cara reconstrueix la vista, i el calendari torna a **avui,
/// vista de dia**: en aquest públic, tornar a un punt conegut val més que recuperar un dia
/// consultat fa deu minuts.
@Composable
fun PlanScreen() {
    val navigator = LocalViraNavigator.current
    val backend = LocalViraBackend.current
    val habitsEnabled by rememberPreference(backend.preferences, PlanModules.HABITS, true)
    val medicationEnabled by rememberPreference(backend.preferences, PlanModules.MEDICATION, true)
    val snapshot by remember(backend) { PlanSnapshot.observe(backend.db) }.collectAsState(PlanSnapshot())

    val chrome = remember { PlanChrome() }
    // Fins que la captura ràpida d'Avui sigui a main, s'hi arriba amb l'editor en blanc.
    val actions = rememberPlanActions(onCapture = { day, period -> chrome.capture = PlanCaptureRequest(day, period) })

    PlanContent(
        snapshot = snapshot,
        actions = actions,
        chrome = chrome,
        section = navigator.planSection,
        onSelect = { navigator.planSection = it },
        habitsEnabled = habitsEnabled,
        medicationEnabled = medicationEnabled,
    )
}

/// Les escriptures de Pla connectades a l'app (la cua de "Desfer", els avisos), per a qualsevol
/// pantalla que marqui una tasca, un hàbit o una presa: Avui fa el mateix que Pla.
@Composable
fun rememberPlanActions(onCapture: (java.time.LocalDate, DayPeriod?) -> Unit = { _, _ -> }): PlanActions {
    val backend = LocalViraBackend.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val present = LocalPresentCompletion.current
    val undo = LocalUndoCompletion.current
    return remember(backend, present, undo) {
        PlanActions(
            store = ViraStore(backend.db),
            scope = scope,
            host = PlanHost(
                present = present,
                undo = undo,
                onCapture = onCapture,
                onScheduleChanged = { ViraNotifications.reschedule(context.applicationContext) },
            ),
        )
    }
}

/// L'editor d'una tasca des de fora de Pla (el "Edita" d'Avui).
@Composable
fun PlanItemEditorScreen(item: cat.vira.android.core.data.model.PlanItem, onDismiss: () -> Unit) =
    PlanItemEditor(item, rememberPlanActions(), onDismiss)

/// L'estat de la fila del títol, compartit entre `PlanContent` i la cara activa: els botons de la
/// barra (afegir, reendreçar) són de cada cara però viuen a la fila de dalt, com a l'iPhone.
@Stable
class PlanChrome {
    /// Una petició del "+" o d'un "Afegir aquí", per a la cara que la sap obrir.
    var adding by mutableStateOf(false)
    var reordering by mutableStateOf(false)
    var canReorder by mutableStateOf(false)
    var capture by mutableStateOf<PlanCaptureRequest?>(null)
}

data class PlanCaptureRequest(val day: java.time.LocalDate, val period: DayPeriod?)

/// El cos de la pestanya sense dependre de l'arrel: és el que fotografien les captures.
@Composable
fun PlanContent(
    snapshot: PlanSnapshot,
    actions: PlanActions,
    chrome: PlanChrome,
    section: PlanSection,
    onSelect: (PlanSection) -> Unit,
    habitsEnabled: Boolean = true,
    medicationEnabled: Boolean = true,
    today: java.time.LocalDate = PlanDay.today(),
    calendarMode: CalendarMode = CalendarMode.Day,
) {
    val reduceMotion = LocalViraEnvironment.current.reduceMotion
    val available = PlanSection.entries.filter {
        when (it) {
            PlanSection.Calendar -> true
            PlanSection.Habits -> habitsEnabled
            PlanSection.Medication -> medicationEnabled
        }
    }
    val resolved = if (section in available) section else PlanSection.Calendar
    val items = available.map { ViraSectionItem(it.name, stringResource(it.title), it.pictogram) }

    Column(Modifier.fillMaxSize().viraSectionBackground(ViraArea.Plan)) {
        TitleRow(resolved, chrome)
        ViraSectionSwitcher(
            area = ViraArea.Plan,
            items = items,
            selectedKey = resolved.name,
            onSelect = { key ->
                val next = PlanSection.valueOf(key)
                if (next != resolved) {
                    chrome.reordering = false
                    chrome.adding = false
                    onSelect(next)
                }
            },
            showsAllLabels = true,
            modifier = Modifier.padding(horizontal = ViraSpacing.screenMargin),
        )
        Spacer(Modifier.padding(top = ViraSpacing.s))
        AnimatedContent(
            targetState = resolved,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(ViraMotion.reduced()) togetherWith fadeOut(ViraMotion.reduced())
                } else {
                    val forward = available.indexOf(targetState) >= available.indexOf(initialState)
                    (slideInHorizontally(ViraMotion.entrance()) { if (forward) it else -it } + fadeIn(ViraMotion.entrance())) togetherWith
                        (slideOutHorizontally(ViraMotion.entrance()) { if (forward) -it else it } + fadeOut(ViraMotion.entrance()))
                }
            },
            label = "planSection",
        ) { current ->
            Box(Modifier.fillMaxSize()) {
                when (current) {
                    PlanSection.Calendar -> CalendarSection(snapshot, actions, chrome, today, initialMode = calendarMode)
                    PlanSection.Habits -> HabitsSection(snapshot, actions, chrome, today)
                    PlanSection.Medication -> MedicationSection(snapshot, actions, chrome, today)
                }
            }
        }
    }
}

/// "Organització" a l'esquerra i, a la dreta, les accions de la cara: reendreçar (només al
/// calendari i només si hi ha res a moure) i afegir.
@Composable
private fun TitleRow(section: PlanSection, chrome: PlanChrome) {
    Row(
        Modifier.fillMaxWidth().padding(start = ViraSpacing.screenMargin, end = ViraSpacing.xs, top = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViraTabTitle(stringResource(R.string.tab_plan), Modifier.weight(1f))
        if (section == PlanSection.Calendar && chrome.canReorder) {
            BarIcon(
                icon = if (chrome.reordering) PlanGlyphShape.Check else PlanGlyphShape.List,
                label = stringResource(if (chrome.reordering) R.string.action_done else R.string.plan_reorder),
            ) { chrome.reordering = !chrome.reordering }
        }
        BarIcon(
            icon = PlanGlyphShape.Plus,
            label = stringResource(
                when (section) {
                    PlanSection.Calendar -> R.string.action_add
                    PlanSection.Habits -> R.string.habits_new
                    PlanSection.Medication -> R.string.medication_new
                },
            ),
        ) { chrome.adding = true }
    }
}

@Composable
private fun BarIcon(icon: PlanGlyphShape, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(ViraTouch.minimum).viraPressable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        PlanGlyph(icon, ViraColor.accent(ViraArea.Plan), size = 22.dp)
    }
}

/// Nom i pictograma de cada cara, com `PlanSection.titleKey` i `.pictogramName`.
private val PlanSection.title: Int
    get() = when (this) {
        PlanSection.Calendar -> R.string.plan_section_calendar
        PlanSection.Habits -> R.string.plan_section_habits
        PlanSection.Medication -> R.string.plan_section_medication
    }

private val PlanSection.pictogram: Int
    get() = when (this) {
        PlanSection.Calendar -> R.drawable.picto_calendar
        PlanSection.Habits -> R.drawable.picto_habits
        PlanSection.Medication -> R.drawable.picto_medication
    }
