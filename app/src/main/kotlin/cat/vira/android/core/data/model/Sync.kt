package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Model que la sincronització sap datar (`ViraSyncable` de l'iPhone).
///
/// `updatedAt` decideix qui guanya quan el mateix registre s'ha tocat en dos dispositius.
/// No l'escriu cada pantalla: el posa `ViraStore` en desar (`ViraSyncStamp`), perquè un oblit aquí
/// no peta, fa que un canvi no viatgi.
interface ViraSyncable<T : ViraSyncable<T>> {
    val id: UUID
    val updatedAt: Instant

    /// Còpia amb un segell nou. Els models són immutables: datar és tornar-ne un altre.
    fun stamped(at: Instant): T
}

/// Singleton creat per defecte que ningú ha tocat en aquest telèfon. No s'envia mai al servidor.
val ViraSyncable<*>.isPlaceholder: Boolean get() = updatedAt == ViraTime.distantPast

/// Esborrat pendent de propagar. Sense làpida, un registre esborrat en un telèfon tornaria des de
/// l'altre. Es crea a `ViraStore.delete`, mai a mà.
@Entity(tableName = "SyncTombstone")
data class SyncTombstone(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    /// Nom de la taula **al servidor** (`plan_items`, `habit_logs`…).
    val table: String = "",
    val recordID: UUID = UUID.randomUUID(),
    val deletedAt: Instant = ViraTime.now(),
)
