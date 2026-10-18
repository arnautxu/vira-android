@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.features.parent

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

/// Vincle entre un compte de fill/a i un de pare/mare (`ViraParentLink` de l'iPhone).
///
/// Els àmbits són quatre interruptors separats i el menor n'és l'amo: el servidor no deixa que
/// el pare/mare els toqui (`0003_parent_link.sql`). Emocions i medicació **neixen tancats**.
@Serializable
data class ParentLink(
    val id: UUID,
    @SerialName("child_user_id") val childUserId: UUID,
    @SerialName("parent_user_id") val parentUserId: UUID,
    @SerialName("scope_usage") val scopeUsage: Boolean,
    @SerialName("scope_habits") val scopeHabits: Boolean,
    @SerialName("scope_emotions") val scopeEmotions: Boolean,
    @SerialName("scope_medication") val scopeMedication: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("revoked_at") val revokedAt: Instant? = null,
) {
    val isActive: Boolean get() = revokedAt == null
}

/// Missatge d'ànim del pare/mare. **Un sol sentit**: no hi ha resposta del menor, perquè un xat
/// entre un menor i un adult necessita moderació i verificació que Vira no té
/// (`PRIVACY_AND_SAFETY.md` §4.1).
@Serializable
data class ParentMessage(
    val id: UUID,
    @SerialName("child_user_id") val childUserId: UUID,
    @SerialName("parent_user_id") val parentUserId: UUID,
    val body: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("read_at") val readAt: Instant? = null,
)

/// Recordatori **proposat**. No entra al pla del menor fins que ell ho digui.
@Serializable
data class ParentReminder(
    val id: UUID,
    @SerialName("child_user_id") val childUserId: UUID,
    @SerialName("parent_user_id") val parentUserId: UUID,
    val title: String,
    val day: Instant? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("accepted_at") val acceptedAt: Instant? = null,
    @SerialName("dismissed_at") val dismissedAt: Instant? = null,
) {
    val isPending: Boolean get() = acceptedAt == null && dismissedAt == null
}

/// Els quatre àmbits que un fill o una filla pot obrir a un adult.
///
/// Es llisten **sempre tots quatre**, oberts o no: un pare o una mare que no sap què no veu
/// treu conclusions equivocades (`ParentChildScreen`).
enum class ParentScope(@StringRes val title: Int, @DrawableRes val pictogram: Int) {
    USAGE(R.string.parent_scope_usage, R.drawable.picto_flame),
    HABITS(R.string.parent_scope_habits, R.drawable.picto_habits),
    EMOTIONS(R.string.parent_scope_emotions, R.drawable.picto_journal),
    MEDICATION(R.string.parent_scope_medication, R.drawable.picto_medication);

    fun isOpen(link: ParentLink): Boolean = when (this) {
        USAGE -> link.scopeUsage
        HABITS -> link.scopeHabits
        EMOTIONS -> link.scopeEmotions
        MEDICATION -> link.scopeMedication
    }
}

/// El que un pare/mare arriba a veure d'un fill/a, segons els àmbits oberts.
///
/// Cada camp és opcional i no per estètica: `null` vol dir "aquest àmbit està tancat", i la vista
/// ho ha de dir amb totes les lletres. Un zero i un "no ho pots veure" no són el mateix, i
/// confondre'ls faria que un pare/mare es pensés que no ha passat res quan simplement no li han
/// obert la porta.
data class ParentChildSummary(
    val displayName: String? = null,
    val totalXP: Int? = null,
    val streakDays: Int? = null,
    val focusSessionsLastWeek: Int? = null,
    val habitsDoneLastWeek: Int? = null,
    val tasksPending: Int? = null,
    val emotionCheckinsLastWeek: Int? = null,
    val activeMedications: Int? = null,
)
