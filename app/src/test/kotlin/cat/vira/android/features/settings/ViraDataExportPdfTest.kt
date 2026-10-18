package cat.vira.android.features.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.R
import cat.vira.android.core.data.ViraDataExport
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.features.settings.export.ViraDataExportPdf
import cat.vira.android.features.settings.export.ViraPdfDocument
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/// L'exportació en PDF (`PRIVACY_AND_SAFETY.md` §5), calcada de `ViraDataExportPDFTests`: es prova
/// el **document**: què hi surt i què no.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraDataExportPdfTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = ViraDatabase.inMemory(context)
    private val text = ViraDataExportPdf.ExportText.of(context)

    @After fun close() = db.close()

    private fun document(): ViraPdfDocument = runBlocking {
        ViraDataExportPdf.document(ViraDataExport.snapshot(db, "1.0 (1)"), text)
    }

    private fun section(document: ViraPdfDocument, title: Int) = document.sections.first { it.title == context.getString(title) }

    @Test
    fun unaAppBuidaExportaTotesLesSeccionsAmbElTextDeBuit() {
        val document = document()
        assertEquals(8, document.sections.size)
        document.sections.filter { it.title != context.getString(R.string.export_section_summary) }.forEach {
            assertTrue(it.title, it.rows.isEmpty())
            assertTrue(it.emptyText.isNotEmpty())
        }
    }

    @Test
    fun elPlaSurtAmbElTitolILEstatDeCadaCosa() = runBlocking {
        val store = ViraStore(db)
        store.create(db.planItems(), PlanItem(title = "Deures de mates", statusRaw = PlanItemStatus.DONE.raw))
        store.create(db.planItems(), PlanItem(title = "Trucar a l'àvia"))

        val plan = section(document(), R.string.export_section_plan)

        assertEquals(2, plan.rows.size)
        assertTrue(plan.rows.any { it.label == "Deures de mates" })
        assertTrue(plan.rows.any { it.value == context.getString(R.string.export_status_done) })
        assertTrue(plan.rows.any { it.value == context.getString(R.string.export_status_pending) })
    }

    @Test
    fun emocionsIMedicacioPortenLAvisDeDadesPrivades() {
        val document = document()
        listOf(R.string.export_section_emotions, R.string.export_section_medication).forEach {
            assertEquals(context.getString(R.string.export_sensitive_note), section(document, it).note)
        }
    }

    @Test
    fun unaEmocioSurtAmbElSeuNomINoAmbLaClau() = runBlocking {
        ViraStore(db).create(db.emotionEntries(), EmotionEntry(emotionRaw = "anxious", intensityRaw = 3, note = "examen"))

        val first = section(document(), R.string.export_section_emotions).rows.first()

        assertEquals(context.getString(R.string.emotion_anxious), first.label)
        assertNotEquals("anxious", first.label)
        assertTrue(first.detail.orEmpty().contains("examen"))
    }

    // L'escriptura real del fitxer (`ViraPdfRenderer`) no es prova aquí: Robolectric no implementa
    // `PdfDocument`. Es comprova exportant al mòbil o a l'emulador.
}
