package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Hàbit (`Habit` de l'iPhone). No s'esborra mai des de la interfície: s'arxiva.
///
/// **`weekdays` fa servir la numeració de l'iPhone**: 1 = diumenge … 7 = dissabte
/// (`Calendar.weekday`), **no** la de `java.time.DayOfWeek` (1 = dilluns). Viatja tal qual al
/// servidor; canviar-la aquí faria que un hàbit de dilluns sortís en diumenge a l'altre telèfon.
/// Llista buida = cada dia.
@Entity(tableName = "Habit")
data class Habit(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val title: String = "",
    val notes: String? = null,
    val symbolName: String = "picto-habits",
    val weekdays: List<Int> = emptyList(),
    val reminderTime: Instant? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant = ViraTime.now(),
) : ViraSyncable<Habit> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// El registre d'un hàbit fet. Esborrar l'hàbit se n'emporta els registres, com a l'iPhone.
@Entity(
    tableName = "HabitLog",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId"), Index("day")],
)
data class HabitLog(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    /// Inici del dia en hora local, com `PlanItem.day`.
    val day: Instant = ViraTime.now(),
    val completedAt: Instant = ViraTime.now(),
    /// La relació `habit` de SwiftData.
    val habitId: UUID? = null,
) : ViraSyncable<HabitLog> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Converteix un `DayOfWeek` a la numeració de l'iPhone (1 = diumenge).
fun java.time.DayOfWeek.iosWeekday(): Int = value % 7 + 1
