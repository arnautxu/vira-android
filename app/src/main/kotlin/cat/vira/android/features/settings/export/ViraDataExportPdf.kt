package cat.vira.android.features.settings.export

import android.content.Context
import cat.vira.android.core.data.ViraDataExport
import cat.vira.android.core.data.ViraDataExportSnapshot
import cat.vira.android.core.data.ViraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

/// La segona forma de l'exportació que demana `PRIVACY_AND_SAFETY.md` §5: **un PDF llegible**, al
/// costat del JSON complet (`ViraDataExportPDF` de l'iPhone).
///
/// Es construeix des del mateix `ViraDataExportSnapshot` que el JSON: si una dada surt al JSON, surt
/// aquí, i les dues formes no poden divergir.
///
/// **No és un informe clínic i no ho pot semblar.** Cap càlcul d'adherència, cap mitjana d'emocions,
/// cap tendència: només el que hi ha, ordenat i amb noms de persona en comptes de claus.
object ViraDataExportPdf {

    /// Escriu el PDF i el retorna. Mateix nom que el JSON (`vira-AAAA-MM-DD.pdf`), en una carpeta
    /// pròpia perquè dues exportacions del mateix dia no es trepitgin.
    suspend fun writeSnapshotFile(
        context: Context,
        db: ViraDatabase,
        directory: File,
        appVersion: String,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): File {
        val snapshot = ViraDataExport.snapshot(db, appVersion)
        val document = document(snapshot, ExportText.of(context))
        return withContext(Dispatchers.IO) {
            val folder = File(directory, "export-${UUID.randomUUID()}").apply { mkdirs() }
            val file = File(folder, "vira-${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.pdf")
            ViraPdfRenderer.write(context, document, file)
        }
    }

    /// Els textos del catàleg per clau de l'iPhone, i com s'escriuen dates i números.
    class ExportText(
        private val lookup: (String) -> String?,
        val locale: Locale = Locale.getDefault(),
        val zone: ZoneId = ZoneId.systemDefault(),
    ) {
        /// Les claus es componen en temps d'execució (`emotion.$raw`): sense text, la clau mateixa.
        fun local(key: String): String = lookup(key) ?: key
        fun find(key: String): String? = lookup(key)
        fun format(key: String, vararg args: Any): String = String.format(locale, local(key), *args)
        fun number(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)
        fun day(instant: Instant): String =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(instant.atZone(zone))
        fun time(instant: Instant): String =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(instant.atZone(zone))
        fun dateTime(instant: Instant): String =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(instant.atZone(zone)) + ", " + time(instant)

        companion object {
            fun of(context: Context) = ExportText(
                lookup = { ViraResourceNames.find(context, it) },
                locale = context.resources.configuration.locales[0],
            )
        }
    }

    fun document(snapshot: ViraDataExportSnapshot, text: ExportText): ViraPdfDocument = with(text) {
        ViraPdfDocument(
            title = local("export.pdf.title"),
            subtitle = format("export.pdf.subtitle", dateTime(snapshot.exportedAt), snapshot.appVersion),
            sections = listOf(
                summary(snapshot),
                plan(snapshot),
                ViraPdfDocument.Section(
                    title = local("assistance.resume.saved"),
                    rows = snapshot.resumeNotes.map { note ->
                        val task = note.taskID?.let { id -> snapshot.planItems.firstOrNull { it.id == id }?.title }
                        ViraPdfDocument.Row(task ?: local("assistance.resume.title"), note.text)
                    },
                    emptyText = local("export.empty"),
                ),
                habits(snapshot),
                medication(snapshot),
                emotions(snapshot),
                focus(snapshot),
                rewards(snapshot),
            ),
        )
    }

    private fun ExportText.summary(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val rows = mutableListOf<ViraPdfDocument.Row>()
        snapshot.profile?.displayName?.takeIf { it.isNotEmpty() }?.let { rows += ViraPdfDocument.Row(local("export.row.name"), it) }
        snapshot.progress?.let { progress ->
            rows += ViraPdfDocument.Row(local("progress.xp"), number(progress.totalXP))
            rows += ViraPdfDocument.Row(local("export.row.coins"), number(progress.coinBalance))
            rows += ViraPdfDocument.Row(local("progress.checkins"), number(progress.emotionCheckinsCount))
            rows += ViraPdfDocument.Row(local("progress.focus_sessions"), number(progress.focusSessionsCount))
            rows += ViraPdfDocument.Row(local("progress.habits_done"), number(progress.habitsCompletedCount))
        }
        snapshot.streak?.let { streak ->
            rows += ViraPdfDocument.Row(local("export.row.streak"), number(streak.currentDays))
            rows += ViraPdfDocument.Row(local("export.row.streak.best"), number(streak.bestDays))
        }
        return ViraPdfDocument.Section(local("export.section.summary"), rows = rows, emptyText = local("export.empty"))
    }

    private fun ExportText.plan(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val details = snapshot.assistanceDetails.associateBy { it.id }
        val titles = snapshot.planItems.associate { it.id to it.title }
        val rows = snapshot.planItems.sortedBy { it.day }.map { item ->
            val metadata = details[item.id]
            val parent = metadata?.parentPlanItemId?.let { titles[it] }?.let { local("assistance.parent") + ": " + it }
            val minutes = metadata?.estimatedMinutes?.let { local("assistance.context.time") + ": " + it }
            val contexts = metadata?.contextIds?.joinToString(", ") { local("assistance.context.$it") }
            val prerequisite = snapshot.assistanceLinks.firstOrNull { it.id == item.id }?.beforePlanItemId
                ?.let { titles[it] }?.let { local("assistance.case.C11") + ": " + it }
            ViraPdfDocument.Row(
                label = item.title,
                value = local("export.status.${item.status}"),
                detail = listOfNotNull(day(item.day), item.time?.let(::time), item.notes, parent, minutes, contexts, prerequisite)
                    .joinToString(" · "),
            )
        }
        return ViraPdfDocument.Section(local("export.section.plan"), rows = rows, emptyText = local("export.empty"))
    }

    private fun ExportText.habits(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val counts = snapshot.habitLogs.groupingBy { it.habitID }.eachCount()
        val rows = snapshot.habits.map { habit ->
            ViraPdfDocument.Row(
                label = habit.title,
                value = format("export.habit.logs", counts[habit.id] ?: 0),
                detail = if (habit.isArchived) local("export.archived") else null,
            )
        }
        return ViraPdfDocument.Section(local("export.section.habits"), rows = rows, emptyText = local("export.empty"))
    }

    /// Medicació i emocions porten la mateixa nota: són les dues dades que no surten del telèfon
    /// sense permís. Al PDF hi són —l'usuari l'ha demanat— però qui el comparteixi ha de saber què hi va.
    private fun ExportText.medication(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val intakes = snapshot.medicationIntakes.groupBy { it.medicationID }
        val rows = snapshot.medications.map { medication ->
            val taken = intakes[medication.id].orEmpty().count { it.status == "taken" }
            ViraPdfDocument.Row(
                label = medication.name,
                value = format("export.medication.intakes", taken),
                detail = listOfNotNull(
                    medication.dose,
                    local("medication.form.${medication.form}"),
                    if (medication.isActive) null else local("medication.ended"),
                ).joinToString(" · "),
            )
        }
        return ViraPdfDocument.Section(
            local("export.section.medication"), note = local("export.sensitive.note"), rows = rows, emptyText = local("export.empty"),
        )
    }

    private fun ExportText.emotions(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val rows = snapshot.emotionEntries.sortedByDescending { it.createdAt }.map { entry ->
            ViraPdfDocument.Row(
                label = local("emotion.${entry.emotion}"),
                value = day(entry.createdAt),
                detail = listOfNotNull(entry.intensity?.let { local("intensity.$it") }, entry.note).joinToString(" · "),
            )
        }
        return ViraPdfDocument.Section(
            local("export.section.emotions"), note = local("export.sensitive.note"), rows = rows, emptyText = local("export.empty"),
        )
    }

    private fun ExportText.focus(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val rows = snapshot.focusSessions.sortedByDescending { it.startedAt }.map { session ->
            ViraPdfDocument.Row(
                label = session.linkedItemTitle ?: local("focus.title"),
                value = format("export.focus.minutes", session.actualSeconds / 60),
                detail = listOfNotNull(day(session.startedAt), session.profileName).joinToString(" · "),
            )
        }
        return ViraPdfDocument.Section(local("export.section.focus"), rows = rows, emptyText = local("export.empty"))
    }

    /// Premis personals i col·lecció en una sola secció: el PDF ja té prou capçaleres.
    private fun ExportText.rewards(snapshot: ViraDataExportSnapshot): ViraPdfDocument.Section {
        val rows = snapshot.personalRewards.map { reward ->
            ViraPdfDocument.Row(
                label = reward.title,
                value = format("export.reward.redeemed", reward.redeemedCount),
                detail = if (reward.isArchived) local("export.archived") else null,
            )
        }.toMutableList()
        snapshot.ownedSkins.forEach { skin ->
            rows += ViraPdfDocument.Row(
                label = find("skin.${skin.key}") ?: skin.key,
                value = local(if (skin.isEquipped) "export.equipped" else "export.owned"),
            )
        }
        if (snapshot.ownedBadges.isNotEmpty()) rows += ViraPdfDocument.Row(local("export.row.badges"), number(snapshot.ownedBadges.size))
        if (snapshot.ownedAccessories.isNotEmpty()) {
            rows += ViraPdfDocument.Row(local("export.row.accessories"), number(snapshot.ownedAccessories.size))
        }
        return ViraPdfDocument.Section(local("export.section.rewards"), rows = rows, emptyText = local("export.empty"))
    }
}
