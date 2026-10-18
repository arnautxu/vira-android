@file:UseSerializers(InstantSerializer::class, UuidSerializer::class)

package cat.vira.android.core.backend.sync

import cat.vira.android.core.backend.InstantSerializer
import cat.vira.android.core.backend.UuidSerializer
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.FocusSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

/// Modes de concentració personalitzats.
@Serializable
data class FocusProfileSyncRow(
    val id: UUID,
    val name: String,
    @SerialName("icon_name") val iconName: String,
    @SerialName("focus_minutes") val focusMinutes: Int,
    @SerialName("break_minutes") val breakMinutes: Int,
    @SerialName("ambient_sound") val ambientSound: String?,
    @SerialName("mute_vira_notifications") val muteViraNotifications: Boolean,
    @SerialName("shows_linked_task") val showsLinkedTask: Boolean,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: FocusProfile) : this(
        model.id, model.name, model.iconName, model.focusMinutes, model.breakMinutes, model.ambientSound,
        model.muteViraNotifications, model.showsLinkedTask, model.isArchived, model.createdAt, model.updatedAt,
    )

    fun writeTo(model: FocusProfile) = model.copy(
        name = name, iconName = iconName, focusMinutes = focusMinutes, breakMinutes = breakMinutes,
        ambientSound = ambientSound, muteViraNotifications = muteViraNotifications, showsLinkedTask = showsLinkedTask,
        isArchived = isArchived, createdAt = createdAt, updatedAt = updatedAt,
    )
}

object FocusProfileSyncTable :
    ViraSyncTable<FocusProfileSyncRow>("focus_profiles", ViraSyncLayer.CORE, FocusProfileSyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.focusProfiles().all().map(::FocusProfileSyncRow)

    override suspend fun apply(rows: List<FocusProfileSyncRow>, db: ViraDatabase) {
        val existing = db.focusProfiles().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            when {
                model == null -> row.writeTo(FocusProfile(id = row.id))
                shouldOverwrite(row, model) -> row.writeTo(model)
                else -> null
            }
        }
        db.focusProfiles().upsertRaw(changed)
    }
}

/// Sessions de concentració. Historial pur.
@Serializable
data class FocusSessionSyncRow(
    val id: UUID,
    @SerialName("started_at") val startedAt: Instant,
    @SerialName("ended_at") val endedAt: Instant?,
    @SerialName("planned_seconds") val plannedSeconds: Int,
    @SerialName("actual_seconds") val actualSeconds: Int,
    val completed: Boolean,
    val kind: String,
    @SerialName("linked_item_title") val linkedItemTitle: String?,
    @SerialName("profile_id") val profileId: UUID?,
    @SerialName("profile_name") val profileName: String?,
    @SerialName("mutes_notifications") val mutesNotifications: Boolean,
    @SerialName("profile_break_minutes") val profileBreakMinutes: Int?,
    @SerialName("updated_at") override val updatedAt: Instant,
) : ViraSyncRow {
    constructor(model: FocusSession) : this(
        model.id, model.startedAt, model.endedAt, model.plannedSeconds, model.actualSeconds, model.completed,
        model.kindRaw, model.linkedItemTitle, model.profileID, model.profileName, model.mutesNotifications,
        model.profileBreakMinutes, model.updatedAt,
    )

    fun writeTo(model: FocusSession) = model.copy(
        startedAt = startedAt, endedAt = endedAt, plannedSeconds = plannedSeconds, actualSeconds = actualSeconds,
        completed = completed, kindRaw = kind, linkedItemTitle = linkedItemTitle, profileID = profileId,
        profileName = profileName, mutesNotifications = mutesNotifications, profileBreakMinutes = profileBreakMinutes,
        updatedAt = updatedAt,
    )
}

object FocusSessionSyncTable :
    ViraSyncTable<FocusSessionSyncRow>("focus_sessions", ViraSyncLayer.CORE, FocusSessionSyncRow.serializer()) {

    override suspend fun local(db: ViraDatabase) = db.focusSessions().all().map(::FocusSessionSyncRow)

    override suspend fun apply(rows: List<FocusSessionSyncRow>, db: ViraDatabase) {
        val existing = db.focusSessions().all().associateBy { it.id }
        val changed = rows.mapNotNull { row ->
            val model = existing[row.id]
            when {
                model == null -> row.writeTo(FocusSession(id = row.id))
                shouldOverwrite(row, model) -> row.writeTo(model)
                else -> null
            }
        }
        db.focusSessions().upsertRaw(changed)
    }
}
