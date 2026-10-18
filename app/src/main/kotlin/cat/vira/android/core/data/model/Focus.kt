package cat.vira.android.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cat.vira.android.core.data.ViraTime
import java.time.Instant
import java.util.UUID

/// Mode de concentració personalitzat (`FocusProfile` de l'iPhone).
@Entity(tableName = "FocusProfile")
data class FocusProfile(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val name: String = "",
    val iconName: String = "picto-focus-modes",
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val ambientSound: String? = null,
    val muteViraNotifications: Boolean = false,
    val showsLinkedTask: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Instant = ViraTime.now(),
) : ViraSyncable<FocusProfile> {
    override fun stamped(at: Instant) = copy(updatedAt = at)
}

/// Sessió de concentració. Historial pur: es crea i en acabar es tanca.
@Entity(tableName = "FocusSession", indices = [Index("startedAt")])
data class FocusSession(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    override val updatedAt: Instant = ViraTime.now(),
    val startedAt: Instant = ViraTime.now(),
    val endedAt: Instant? = null,
    val plannedSeconds: Int = 25 * 60,
    val actualSeconds: Int = 0,
    val completed: Boolean = false,
    val kindRaw: String = FocusKind.FOCUS.raw,
    val linkedItemTitle: String? = null,
    val profileID: UUID? = null,
    val profileName: String? = null,
    val mutesNotifications: Boolean = false,
    val profileBreakMinutes: Int? = null,
) : ViraSyncable<FocusSession> {
    override fun stamped(at: Instant) = copy(updatedAt = at)

    /// `FocusSession.finish` de l'iPhone.
    fun finished(at: Instant, completed: Boolean) = copy(
        endedAt = at,
        actualSeconds = maxOf(0L, java.time.Duration.between(startedAt, at).seconds).toInt(),
        completed = completed,
    )
}

val FocusSession.kind: FocusKind get() = FocusKind.from(kindRaw)

/// La fase: concentració, descans curt o llarg (`ViraShared/FocusKind.swift`).
enum class FocusKind(val raw: String) {
    FOCUS("focus"), SHORT_BREAK("shortBreak"), LONG_BREAK("longBreak");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: FOCUS
    }
}
