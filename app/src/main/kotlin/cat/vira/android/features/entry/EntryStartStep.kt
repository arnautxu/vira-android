package cat.vira.android.features.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraSpacing

/// E2 · Per on comencem? Tres fitxes amb pictograma (`EntryStartStep.swift`). La resposta decideix
/// quina acció de debò es fa abans del compte. "Continuar" espera una tria; "Saltar" (a la
/// capçalera) deixa `EntryStartIntent.fallback`.
@Composable
fun EntryStartStep(prefs: EntryPreferences, onChoose: () -> Unit, onContinue: () -> Unit) {
    val stored = prefs.startIntent
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            EntryStartIntent.entries.forEach { intent ->
                EntryChoiceTile(
                    title = stringResource(intent.title),
                    detail = stringResource(intent.detail),
                    pictogram = intent.pictogram,
                    isSelected = stored.value == intent.raw,
                    onClick = {
                        stored.value = intent.raw
                        onChoose()
                    },
                )
            }
        }
        EntryPrimaryButton(stringResource(R.string.action_continue), onContinue, enabled = stored.value.isNotEmpty())
    }
}

/// E3 · Quants anys tens? **Amb backend** "Continuar" espera una tria: el tram decideix si cal el
/// permís del tutor (12–15). **Sense backend** és orientatiu i opcional.
@Composable
fun EntryAgeStep(prefs: EntryPreferences, requiresChoice: Boolean, onChoose: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
        EntryAgeChoice(prefs.ageRange, allowsClearing = !requiresChoice, onChoose = onChoose)
        EntryPrimaryButton(
            stringResource(R.string.action_continue),
            onContinue,
            enabled = !requiresChoice || prefs.ageRange.value.isNotEmpty(),
        )
    }
}

/// Les tres fitxes d'edat. Compartides amb el permís del tutor, que les torna a demanar si algú
/// hi arriba sense tram. Sense permís pel mig la tria es pot desfer: és orientativa.
@Composable
fun EntryAgeChoice(ageRange: EntryPreference<String>, allowsClearing: Boolean, onChoose: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        ViraAgeRange.entries.forEach { range ->
            EntryChoiceTile(
                title = stringResource(range.title),
                isSelected = ageRange.value == range.raw,
                onClick = {
                    val isSame = ageRange.value == range.raw
                    ageRange.value = if (isSame && allowsClearing) "" else range.raw
                    onChoose()
                },
            )
        }
    }
}
