package cat.vira.android.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cat.vira.android.R
import cat.vira.android.core.backend.ViraBackend
import cat.vira.android.core.data.model.CoinReason
import cat.vira.android.core.design.PendingScreen
import cat.vira.android.core.design.components.ViraActionButton
import cat.vira.android.core.design.components.ViraActionHierarchy
import cat.vira.android.core.design.tokens.ViraSpacing
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
    const val ENTRY_READY = false

    /// Fil Entrada (`features/entry`). En acabar, el flux marca la seva preferència i crida
    /// `onFinish`.
    @Composable
    fun Entry(request: ViraEntryRequest, onFinish: () -> Unit) = PendingScreen()

    /// Fil Panell de pares (`features/parent`): l'arrel d'un compte de pare o mare. Els enllaços de
    /// vinculació arriben per `LocalViraIncomingLinks.current` (l'últim es conserva).
    @Composable
    fun ParentRoot() = PendingScreen()

    /// Fil Panell de pares: el que veu un pare o mare a les builds sense panell (Google Play).
    @Composable
    fun ParentReleasePlaceholder() = PendingScreen()

    /// Fil Concentra't (`features/focus`): la preparació a pantalla completa, sense la barra
    /// (`FocusScreen(onClose = onClose)`); `link` és la tasca triada, si s'hi ha arribat des d'una
    /// fila. Si l'app s'obre des de l'avís fix amb una sessió en
    /// marxa, el mateix fil hi afegeix aquí la represa (`FocusRuntime.isActive`).
    @Composable
    fun Focus(link: ViraFocusLink?, onClose: () -> Unit) = PendingModal(onClose)

    /// Fil Ajuda ara (`features/help`): `HelpScreen(onClose, onOpenCheckIn)`. `onOpenCheckIn`
    /// tanca Ajuda ara i obre el check-in d'Emocions.
    @Composable
    fun Help(onClose: () -> Unit, onOpenCheckIn: () -> Unit) = PendingModal(onClose)

    /// Fil Ajuda ara: la respiració directa (`HelpBreatheScreen(onClose)`), des dels widgets, el
    /// panell ràpid i Mindfulness.
    @Composable
    fun Breathe(onClose: () -> Unit) = PendingModal(onClose)

    /// Fil Viri i ratxa (`features/progress`): concedeix la recompensa d'una acció feta i en torna
    /// (monedes, XP). Ha de passar per `ViraProgress.record`, que també compta el dia per a la ratxa;
    /// l'XP és `ViraRewards.rule(reason)?.coins`. Mai des de medicació.
    suspend fun commitReward(backend: ViraBackend, reason: CoinReason): Pair<Int, Int> = 0 to 0

    /// Fil Pla (`HabitCelebrationView`): la celebració a pantalla completa del primer hàbit del dia.
    /// Mentre no hi és, es tanca sola.
    @Composable
    fun HabitCelebration(title: String, coins: Int, xp: Int, onContinue: () -> Unit) {
        LaunchedEffect(Unit) { onContinue() }
    }

    /// Fil Concentra't: `FocusProfileEditorScreen` amb el mode `profileId` (`null`, un de nou). Es desa
    /// sol.
    @Composable
    fun FocusProfileEditor(profileId: UUID?, onClose: () -> Unit) = PendingModal(onClose)

    /// Fil Ajustos i compte (`features/settings`). Per editar un mode de Concentra't,
    /// `LocalViraNavigator.current.editFocusProfile(id)`.
    @Composable
    fun Settings(onClose: () -> Unit) = PendingModal(onClose)
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
