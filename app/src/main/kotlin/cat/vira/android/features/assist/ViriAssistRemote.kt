package cat.vira.android.features.assist

import cat.vira.android.core.backend.ViraSession
import kotlinx.coroutines.CancellationException
import java.time.LocalDateTime

/// Qui fa les preguntes a fora (`ViriAssist+Session` i `jevTaskSize` de l'iPhone): demana el
/// testimoni a la sessió, pregunta i descarta la resposta si mentrestant ha canviat el compte.
class ViriAssistRemote(
    private val session: ViraSession,
    private val language: () -> String,
    private val client: ViriAssistClient = ViriAssistClient(),
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    val canUse: Boolean get() = ViriAssist.canUse(session)

    /// Els passos per a un títol. Els errors de xarxa es diuen igual que els del servidor: la
    /// persona només necessita saber que ara no ha anat.
    suspend fun steps(title: String): List<ViriAssist.StepProposal> {
        val generation = session.dataGeneration
        val token = try {
            session.accessToken()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            throw ViriAssist.FailureException(ViriAssist.Failure.SESSION)
        }
        val proposals = client.steps(ViriAssist.stepsRequest(title, now(), language()), token)
        if (generation != session.dataGeneration) throw CancellationException("Ha canviat el compte")
        return proposals
    }

    /// Pregunta a Jev si la tasca és gran. `null` —mai llança— sense permís, sense sessió, sense
    /// quota o amb una resposta que no encaixa: llavors mana la regla del telèfon.
    suspend fun jevTaskSize(title: String): ViriTaskSize? {
        if (!canUse || !ViriTaskSize.isWorthAsking(title)) return null
        return try {
            ViriTaskSize.fromJev(client.scope(title, language(), session.accessToken()))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
}
