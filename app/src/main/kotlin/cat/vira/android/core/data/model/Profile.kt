package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Perfil local: el pseudònim i les insígnies fixades. Singleton (`ViraSingletons.profile`).
///
/// Mai nom real, cognoms ni data de naixement (`PRIVACY_AND_SAFETY.md` §3). Només
/// `displayName` viatja al servidor; `pinnedBadgeKeys` és del telèfon.
@Entity(tableName = "ViraProfile")
data class ViraProfile(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val displayName: String = "",
    val pinnedBadgeKeys: List<String> = emptyList(),
) : ViraSyncable<ViraProfile> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}
