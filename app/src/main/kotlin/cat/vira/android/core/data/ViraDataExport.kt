@file:UseSerializers(ViraDataExport.DateSerializer::class, ViraDataExport.IdSerializer::class)

package cat.vira.android.core.data

import androidx.room.withTransaction
import cat.vira.android.core.data.model.AccessoryOwnership
import cat.vira.android.core.data.model.AssistanceLocalNote
import cat.vira.android.core.data.model.BadgeOwnership
import cat.vira.android.core.data.model.ChallengeClaim
import cat.vira.android.core.data.model.CoinTransaction
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.data.model.FocusSession
import cat.vira.android.core.data.model.Habit
import cat.vira.android.core.data.model.HabitLog
import cat.vira.android.core.data.model.Medication
import cat.vira.android.core.data.model.MedicationIntake
import cat.vira.android.core.data.model.PersonalReward
import cat.vira.android.core.data.model.PlanAssistanceDetail
import cat.vira.android.core.data.model.PlanAssistanceLink
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.ProgressCounter
import cat.vira.android.core.data.model.SkinOwnership
import cat.vira.android.core.data.model.Streak
import cat.vira.android.core.data.model.ViraProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/// Fotografia completa de les dades de l'usuari, en JSON llegible i generat **al telèfon**
/// (`PRIVACY_AND_SAFETY.md` §5, `USER_FLOWS.md` F25). Cap xarxa: només s'escriu un fitxer que
/// l'usuari comparteix on vulgui.
///
/// **Les claus i la forma són les de l'iPhone** (`ViraDataExportSnapshot`), perquè una exportació
/// d'un telèfon es llegeixi igual que la de l'altre: camelCase, dates ISO 8601 sense fraccions,
/// claus ordenades.
@Serializable
data class ViraDataExportSnapshot(
    val exportedAt: Instant,
    val appVersion: String,
    val profile: ViraDataExport.ProfileDTO?,
    val progress: ViraDataExport.ProgressCounterDTO?,
    val streak: ViraDataExport.StreakDTO?,
    val planItems: List<ViraDataExport.PlanItemDTO>,
    val assistanceDetails: List<ViraDataExport.AssistanceDetailDTO>,
    val assistanceLinks: List<ViraDataExport.AssistanceLinkDTO>,
    val resumeNotes: List<ViraDataExport.ResumeNoteDTO>,
    val habits: List<ViraDataExport.HabitDTO>,
    val habitLogs: List<ViraDataExport.HabitLogDTO>,
    val medications: List<ViraDataExport.MedicationDTO>,
    val medicationIntakes: List<ViraDataExport.MedicationIntakeDTO>,
    val emotionEntries: List<ViraDataExport.EmotionEntryDTO>,
    val focusSessions: List<ViraDataExport.FocusSessionDTO>,
    val focusProfiles: List<ViraDataExport.FocusProfileDTO>,
    val coinTransactions: List<ViraDataExport.CoinTransactionDTO>,
    val personalRewards: List<ViraDataExport.PersonalRewardDTO>,
    val ownedSkins: List<ViraDataExport.OwnershipDTO>,
    val ownedAccessories: List<ViraDataExport.OwnershipDTO>,
    val ownedBadges: List<ViraDataExport.BadgeDTO>,
    val challengeClaims: List<ViraDataExport.ChallengeClaimDTO>,
)

object ViraDataExport {

    /// Llegeix totes les taules. Sense filtres ni límits: l'exportació és, per definició, **tot**.
    /// Emocions i medicació hi surten: l'usuari és qui ho demana i és seu.
    /// Totes les lectures en una transacció: si l'usuari esborra alguna cosa a mitja exportació, la
    /// fotografia és d'abans o de després, mai una barreja.
    suspend fun snapshot(db: ViraDatabase, appVersion: String, now: Instant = ViraTime.now()) = db.withTransaction {
        read(db, appVersion, now)
    }

    private suspend fun read(db: ViraDatabase, appVersion: String, now: Instant) = ViraDataExportSnapshot(
        exportedAt = now,
        appVersion = appVersion,
        profile = db.profiles().all().firstOrNull()?.let(::ProfileDTO),
        progress = db.progressCounters().all().firstOrNull()?.let(::ProgressCounterDTO),
        streak = db.streaks().all().firstOrNull()?.let(::StreakDTO),
        planItems = db.planItems().all().map(::PlanItemDTO),
        assistanceDetails = db.assistanceDetails().all().map(::AssistanceDetailDTO),
        assistanceLinks = db.assistanceLinks().all().map(::AssistanceLinkDTO),
        resumeNotes = db.assistanceNotes().all().filter { it.text.isNotEmpty() }.map(::ResumeNoteDTO),
        habits = db.habits().all().map(::HabitDTO),
        habitLogs = db.habitLogs().all().map(::HabitLogDTO),
        medications = db.medications().all().map(::MedicationDTO),
        medicationIntakes = db.medicationIntakes().all().map(::MedicationIntakeDTO),
        emotionEntries = db.emotionEntries().all().map(::EmotionEntryDTO),
        focusSessions = db.focusSessions().all().map(::FocusSessionDTO),
        focusProfiles = db.focusProfiles().all().map(::FocusProfileDTO),
        coinTransactions = db.coinTransactions().all().map(::CoinTransactionDTO),
        personalRewards = db.personalRewards().all().map(::PersonalRewardDTO),
        ownedSkins = db.skinOwnerships().all().map(::OwnershipDTO),
        ownedAccessories = db.accessoryOwnerships().all().map(::OwnershipDTO),
        ownedBadges = db.badgeOwnerships().all().map(::BadgeDTO),
        challengeClaims = db.challengeClaims().all().map(::ChallengeClaimDTO),
    )

    /// El JSON, amb sagnat i claus ordenades com el de l'iPhone.
    fun encode(snapshot: ViraDataExportSnapshot): String =
        pretty.encodeToString(JsonElement.serializer(), sorted(json.encodeToJsonElement(ViraDataExportSnapshot.serializer(), snapshot)))

    /// Escriu el fitxer a `directory` (la memòria cau de l'app, per compartir-lo) i el retorna. El nom
    /// és el de l'iPhone (`vira-AAAA-MM-DD.json`); cada exportació va a una carpeta pròpia perquè dues
    /// del mateix dia no es trepitgin mentre el full de compartir encara en té una d'oberta.
    suspend fun writeSnapshotFile(
        db: ViraDatabase,
        directory: File,
        appVersion: String,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): File {
        val snapshot = snapshot(db, appVersion)
        // Sense límits, el JSON pot ser gran: ni codificar-lo ni escriure'l bloqueja la pantalla.
        return withContext(Dispatchers.IO) {
            val folder = File(directory, "export-${UUID.randomUUID()}").apply { mkdirs() }
            File(folder, "vira-${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json").apply { writeText(encode(snapshot)) }
        }
    }

    private val json = Json { encodeDefaults = true; explicitNulls = false }
    private val pretty = Json { prettyPrint = true }

    private fun sorted(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.toSortedMap().mapValues { sorted(it.value) })
        is JsonArray -> JsonArray(element.map(::sorted))
        else -> element
    }

    // Formats de l'iPhone: `.iso8601` sense fraccions i UUID en majúscules (`uuidString`).

    object DateSerializer : KSerializer<Instant> {
        override val descriptor = PrimitiveSerialDescriptor("ViraExportDate", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Instant) =
            encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.SECONDS)))
        override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
    }

    object IdSerializer : KSerializer<UUID> {
        override val descriptor = PrimitiveSerialDescriptor("ViraExportId", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString().uppercase())
        override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    }

    // Una còpia neta per taula, amb les relacions reduïdes a l'`id` del pare (`ViraDataExportDTOs`).

    @Serializable
    data class PlanItemDTO(
        val id: UUID, val title: String, val notes: String?, val day: Instant, val time: Instant?,
        val status: String, val completedAt: Instant?, val reminderOffsetMinutes: Int?, val createdAt: Instant,
    ) {
        constructor(item: PlanItem) : this(
            item.id, item.title, item.notes, item.day, item.time, item.statusRaw, item.completedAt,
            item.reminderOffsetMinutes, item.createdAt,
        )
    }

    /// A l'iPhone surt la mateixa fila que es sincronitza, amb les claus en camelCase.
    @Serializable
    data class AssistanceDetailDTO(
        val id: UUID, val parentPlanItemId: UUID?, val stepOrder: Int?, val estimatedMinutes: Int?,
        val contextIds: List<String>, val updatedAt: Instant,
    ) {
        constructor(detail: PlanAssistanceDetail) : this(
            detail.id, detail.parentPlanItemID, detail.stepOrder, detail.estimatedMinutes, detail.contextIDs,
            detail.updatedAt,
        )
    }

    @Serializable
    data class AssistanceLinkDTO(val id: UUID, val beforePlanItemId: UUID?, val updatedAt: Instant) {
        constructor(link: PlanAssistanceLink) : this(link.id, link.beforePlanItemID, link.updatedAt)
    }

    @Serializable
    data class ResumeNoteDTO(val id: UUID, val taskID: UUID?, val text: String, val updatedAt: Instant) {
        constructor(note: AssistanceLocalNote) : this(note.id, note.taskID, note.text, note.updatedAt)
    }

    @Serializable
    data class HabitDTO(
        val id: UUID, val title: String, val notes: String?, val symbolName: String, val weekdays: List<Int>,
        val reminderTime: Instant?, val isArchived: Boolean, val createdAt: Instant,
    ) {
        constructor(habit: Habit) : this(
            habit.id, habit.title, habit.notes, habit.symbolName, habit.weekdays, habit.reminderTime,
            habit.isArchived, habit.createdAt,
        )
    }

    @Serializable
    data class HabitLogDTO(val id: UUID, val habitID: UUID?, val day: Instant, val completedAt: Instant) {
        constructor(log: HabitLog) : this(log.id, log.habitId, log.day, log.completedAt)
    }

    @Serializable
    data class MedicationDTO(
        val id: UUID, val name: String, val dose: String?, val form: String, val isActive: Boolean,
        val createdAt: Instant, val endedAt: Instant?, val timesInMinutes: List<Int>, val weekdays: List<Int>,
    ) {
        constructor(medication: Medication) : this(
            medication.id, medication.name, medication.dose, medication.formRaw, medication.isActive,
            medication.createdAt, medication.endedAt, medication.timesInMinutes, medication.weekdays,
        )
    }

    @Serializable
    data class MedicationIntakeDTO(
        val id: UUID, val medicationID: UUID?, val scheduledAt: Instant, val status: String, val loggedAt: Instant?,
    ) {
        constructor(intake: MedicationIntake) : this(
            intake.id, intake.medicationId, intake.scheduledAt, intake.statusRaw, intake.loggedAt,
        )
    }

    @Serializable
    data class EmotionEntryDTO(
        val id: UUID, val emotion: String, val intensity: Int?, val note: String?, val createdAt: Instant,
    ) {
        constructor(entry: EmotionEntry) : this(entry.id, entry.emotionRaw, entry.intensityRaw, entry.note, entry.createdAt)
    }

    @Serializable
    data class FocusSessionDTO(
        val id: UUID, val startedAt: Instant, val endedAt: Instant?, val plannedSeconds: Int, val actualSeconds: Int,
        val completed: Boolean, val kind: String, val linkedItemTitle: String?, val profileName: String?,
    ) {
        constructor(session: FocusSession) : this(
            session.id, session.startedAt, session.endedAt, session.plannedSeconds, session.actualSeconds,
            session.completed, session.kindRaw, session.linkedItemTitle, session.profileName,
        )
    }

    @Serializable
    data class FocusProfileDTO(
        val id: UUID, val name: String, val focusMinutes: Int, val breakMinutes: Int,
        val muteViraNotifications: Boolean, val isArchived: Boolean, val createdAt: Instant,
    ) {
        constructor(profile: FocusProfile) : this(
            profile.id, profile.name, profile.focusMinutes, profile.breakMinutes, profile.muteViraNotifications,
            profile.isArchived, profile.createdAt,
        )
    }

    @Serializable
    data class ProgressCounterDTO(
        val totalXP: Int, val coinBalance: Int, val coinsEarnedTotal: Int, val emotionCheckinsCount: Int,
        val focusSessionsCount: Int, val habitsCompletedCount: Int, val daysActiveCount: Int,
    ) {
        constructor(counter: ProgressCounter) : this(
            counter.totalXP, counter.coinBalance, counter.coinsEarnedTotal, counter.emotionCheckinsCount,
            counter.focusSessionsCount, counter.habitsCompletedCount, counter.daysActiveCount,
        )
    }

    @Serializable
    data class StreakDTO(
        val currentDays: Int, val bestDays: Int, val lastActiveDay: Instant?, val freezesAvailable: Int,
        val isVisible: Boolean,
    ) {
        constructor(streak: Streak) : this(
            streak.currentDays, streak.bestDays, streak.lastActiveDay, streak.freezesAvailable, streak.isVisible,
        )
    }

    @Serializable
    data class CoinTransactionDTO(val id: UUID, val amount: Int, val reason: String, val createdAt: Instant) {
        constructor(transaction: CoinTransaction) : this(
            transaction.id, transaction.amount, transaction.reasonRaw, transaction.createdAt,
        )
    }

    @Serializable
    data class PersonalRewardDTO(
        val id: UUID, val title: String, val cost: Int, val isArchived: Boolean, val redeemedCount: Int,
        val createdAt: Instant,
    ) {
        constructor(reward: PersonalReward) : this(
            reward.id, reward.title, reward.cost, reward.isArchived, reward.redeemedCount, reward.createdAt,
        )
    }

    /// Skins i accessoris tenen la mateixa forma.
    @Serializable
    data class OwnershipDTO(val key: String, val purchasedAt: Instant, val isEquipped: Boolean) {
        constructor(skin: SkinOwnership) : this(skin.key, skin.purchasedAt, skin.isEquipped)
        constructor(accessory: AccessoryOwnership) : this(accessory.key, accessory.purchasedAt, accessory.isEquipped)
    }

    @Serializable
    data class BadgeDTO(val key: String, val purchasedAt: Instant) {
        constructor(badge: BadgeOwnership) : this(badge.key, badge.purchasedAt)
    }

    @Serializable
    data class ChallengeClaimDTO(val key: String, val cycleStart: Instant, val claimedAt: Instant) {
        constructor(claim: ChallengeClaim) : this(claim.key, claim.cycleStart, claim.claimedAt)
    }

    @Serializable
    data class ProfileDTO(val displayName: String, val pinnedBadges: List<String>) {
        constructor(profile: ViraProfile) : this(profile.displayName, profile.pinnedBadgeKeys)
    }
}
