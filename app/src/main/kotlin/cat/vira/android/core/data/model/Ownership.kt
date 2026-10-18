package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

// Les quatre coses que es compren o es guanyen i es tenen per clau, no per `id`: comprar la mateixa
// skin des de dos telèfons ha de donar una fila, no dues. Per això la clau és única.

@Entity(tableName = "SkinOwnership", indices = [Index("key", unique = true)])
data class SkinOwnership(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val key: String = "",
    val purchasedAt: Instant = ViraTime.now(),
    val isEquipped: Boolean = false,
) : ViraSyncable<SkinOwnership> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

@Entity(tableName = "AccessoryOwnership", indices = [Index("key", unique = true)])
data class AccessoryOwnership(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val key: String = "",
    val purchasedAt: Instant = ViraTime.now(),
    val isEquipped: Boolean = false,
) : ViraSyncable<AccessoryOwnership> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Insígnies. **No se sincronitzen** (a l'iPhone tampoc): no hi ha taula al servidor.
@Entity(tableName = "BadgeOwnership", indices = [Index("key", unique = true)])
data class BadgeOwnership(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val key: String = "",
    val purchasedAt: Instant = ViraTime.now(),
) : ViraSyncable<BadgeOwnership> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Repte ja cobrat. Clau natural `(repte, inici del cicle)`.
@Entity(tableName = "ChallengeClaim", indices = [Index("key", "cycleStart", unique = true)])
data class ChallengeClaim(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val key: String = "",
    val cycleStart: Instant = ViraTime.now(),
    val claimedAt: Instant = ViraTime.now(),
) : ViraSyncable<ChallengeClaim> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}
