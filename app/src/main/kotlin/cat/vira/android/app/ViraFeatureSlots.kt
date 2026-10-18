package cat.vira.android.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.filterNotNull
import cat.vira.android.features.parent.ParentRootScreen
import cat.vira.android.features.parent.ParentPanelPlaceholder
import cat.vira.android.features.parent.ParentLinkUrl
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.emotions.EmotionEntryCheckIn
import cat.vira.android.features.entry.EntryFirstActionHosts
import cat.vira.android.core.data.ViraRewards
import cat.vira.android.core.data.ViraStore
import cat.vira.android.features.progress.logic.ViraProgress
import java.time.Instant
import cat.vira.android.features.entry.EntryRequest
import cat.vira.android.features.entry.LocalEntryFirstActionHosts
import cat.vira.android.features.focus.FocusProfileEditorScreen
import cat.vira.android.features.focus.FocusScreen
import cat.vira.android.features.focus.FocusStartRequest
import cat.vira.android.features.help.HelpBreatheScreen
import cat.vira.android.features.help.HelpScreen
import cat.vira.android.features.settings.AccountSettingsScreen
import cat.vira.android.features.settings.SettingsScreen
import java.util.UUID

/// Quin tros del flux d'entrada demana l'arrel (`EntryFlowView(steps:)` a l'iPhone).
enum class ViraEntryRequest {
    /// El flux sencer de la primera obertura.
    Full,
    /// Només el compte: hi ha backend i no hi ha sessió.
    Account,
    /// Només el permís del tutor: sessió oberta i permís encara no demanat.
    Consent,
    /// Una recuperació de contrasenya a mig fer passa per davant de tot.
    NewPassword,
}

/// On encaixa cada funció a l'arrel. **És l'única peça d'`app/` que els altres fils toquen**: quan
/// una pantalla arriba a `main`, el seu fil substitueix aquí la línia que li toca per la crida a la
/// seva pantalla, i res més. Fins aleshores, cada lloc ensenya el marcador de l'esquelet.
internal object ViraFeatureSlots {

    /// Fil Entrada: posa-ho a `true` quan `Entry` cridi el flux de debò. Mentre és `false`, les
    /// portes deixen passar (sense flux no hi hauria manera d'entrar a l'app de proves).
    const val ENTRY_READY = true

    /// Fil Entrada (`features/entry`). En acabar, el flux marca la seva preferència i crida
    /// `onFinish`.
    ///
    /// La primera acció (E5) fa servir les pantalles de debò: el check-in sencer d'Emocions i
    /// Concentra't arrencat sol amb els segons triats. L'entrada no les coneix; les hi passa l'arrel.
    @Composable
    fun Entry(request: ViraEntryRequest, onFinish: () -> Unit) {
        CompositionLocalProvider(LocalEntryFirstActionHosts provides entryFirstActionHosts) {
            cat.vira.android.features.entry.Entry(
                request = when (request) {
                    ViraEntryRequest.Full -> EntryRequest.FULL
                    ViraEntryRequest.Account -> EntryRequest.ACCOUNT
                    ViraEntryRequest.Consent -> EntryRequest.CONSENT
                    ViraEntryRequest.NewPassword -> EntryRequest.NEW_PASSWORD
                },
                onFinish = onFinish,
            )
        }
    }

    private val entryFirstActionHosts = EntryFirstActionHosts(
        checkIn = { onRecorded, onClose -> EmotionEntryCheckIn(onRecorded, onClose) },
        focus = { seconds, onClose -> FocusScreen(FocusStartRequest(seconds = seconds, autostart = true), onClose) },
    )

    /// Fil Panell de pares (`features/parent`): l'arrel d'un compte de pare o mare. Els enllaços de
    /// vinculació arriben per `LocalViraIncomingLinks.current.pending`; un cop fet servir,
    /// `consume(uri)`.
    @Composable
    fun ParentRoot() {
        val incoming = LocalViraIncomingLinks.current
        // Només els enllaços de vinculació; es consumeixen en arribar al camp, mai s'envien sols.
        val codes = remember(incoming) {
            incoming.pending.filterNotNull().mapNotNull { uri ->
                ParentLinkUrl.code(uri.toString())?.also { incoming.consume(uri) }
            }
        }
        ParentRootScreen(account = { onClose -> AccountSettingsScreen(onClose) }, linkCodes = codes)
    }

    /// Fil Panell de pares: el que veu un pare o mare a les builds sense panell (Google Play).
    @Composable
    fun ParentReleasePlaceholder() {
        var showingAccount by rememberSaveable { mutableStateOf(false) }
        if (showingAccount) AccountSettingsScreen(onClose = { showingAccount = false })
        else ParentPanelPlaceholder(onAccount = { showingAccount = true })
    }

    /// Fil Concentra't (`features/focus`): la preparació a pantalla completa, sense la barra
    /// (`FocusScreen(onClose = onClose)`); `link` és la tasca triada, si s'hi ha arribat des d'una
    /// fila. La represa des de l'avís fix és `FocusResumeEffect` a `RootScreen`.
    @Composable
    fun Focus(link: ViraFocusLink?, onClose: () -> Unit) =
        FocusScreen(FocusStartRequest(linkedTitle = link?.title, linkedItemID = link?.itemId), onClose)

    /// Fil Ajuda ara (`features/help`): `HelpScreen(onClose, onOpenCheckIn)`. `onOpenCheckIn`
    /// tanca Ajuda ara i obre el check-in d'Emocions.
    @Composable
    fun Help(onClose: () -> Unit, onOpenCheckIn: () -> Unit) = HelpScreen(onClose, onOpenCheckIn)

    /// Fil Ajuda ara: la respiració directa (`HelpBreatheScreen(onClose)`), des dels widgets, el
    /// panell ràpid i Mindfulness.
    @Composable
    fun Breathe(onClose: () -> Unit) = HelpBreatheScreen(onClose)

    /// Fil Viri i ratxa (`features/progress`): concedeix la recompensa d'una acció feta i en torna
    /// (monedes, XP). Ha de passar per `ViraProgress.record`, que també compta el dia per a la ratxa;
    /// l'XP és `ViraRewards.rule(reason)?.coins`. Mai des de medicació. `at` és quan s'ha tocat.
    suspend fun commitReward(backend: ViraBackend, reason: CoinReason, at: Instant): Pair<Int, Int> {
        val coins = ViraProgress(ViraStore(backend.db)).record(reason, now = at)
        return coins to (ViraRewards.rule(reason)?.coins ?: 0)
    }

    /// Fil Pla (`HabitCelebrationView`): la celebració a pantalla completa del primer hàbit del dia.
    /// Mentre no hi és, es tanca sola.
    @Composable
    fun HabitCelebration(title: String, coins: Int, xp: Int, onContinue: () -> Unit) {
        LaunchedEffect(Unit) { onContinue() }
    }

    /// Fil Concentra't: `FocusProfileEditorScreen` amb el mode `profileId` (`null`, un de nou). Es desa
    /// sol.
    @Composable
    fun FocusProfileEditor(profileId: UUID?, onClose: () -> Unit) {
        val backend = LocalViraBackend.current
        // Nou: l'editor de seguida. Existent: primer es llegeix, i fins aleshores no hi ha res a ensenyar.
        val profile by produceState<FocusProfile?>(null, profileId) {
            value = profileId?.let { backend.db.focusProfiles().byId(it) }
        }
        if (profileId == null || profile != null) FocusProfileEditorScreen(profile) { onClose() }
    }

    /// Fil Ajustos i compte (`features/settings`). Per editar un mode de Concentra't,
    /// `LocalViraNavigator.current.editFocusProfile(id)`.
    @Composable
    fun Settings(onClose: () -> Unit) {
        val navigator = LocalViraNavigator.current
        SettingsScreen(onClose, onEditFocusProfile = { navigator.editFocusProfile(it?.id) })
    }
}

/// El marcador de l'esquelet amb una manera de tornar enrere a més del gest.
@Composable
private fun PendingModal(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PendingScreen()
        ViraActionButton(
            text = stringResource(R.string.action_close),
            onClick = onClose,
            hierarchy = ViraActionHierarchy.Textual,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(ViraSpacing.s),
        )
    }
}
