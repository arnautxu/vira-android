package cat.vira.android.core.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.core.data.model.PlanItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ViraDataEraserTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = ViraDatabase.inMemory(context)
    private val preferences = ViraPreferences.open(context).also { it.edit().clear().commit() }

    @After fun close() = db.close()

    @Test
    fun unCanviDeCompteEsBuidaTotMenysElQueEsDelTelefon() = runBlocking {
        ViraStore(db).create(db.planItems(), PlanItem(title = "De l'altre"))
        preferences.edit()
            .putString(ViraPreferences.APPEARANCE, "dark")
            .putBoolean(ViraPreferences.ONBOARDING_COMPLETED, true)
            .putBoolean(ViraPreferences.SYNC_EMOTIONS_ENABLED, false)
            .putString("sync.cursor.pull.plan_items", "2026-10-01T00:00:00.000Z")
            .commit()

        ViraDataEraser.eraseForAccountSwitch(db, preferences)

        assertTrue(db.planItems().all().isEmpty())
        assertEquals("dark", preferences.getString(ViraPreferences.APPEARANCE, null))
        assertTrue(preferences.getBoolean(ViraPreferences.ONBOARDING_COMPLETED, false))
        assertFalse(preferences.contains(ViraPreferences.SYNC_EMOTIONS_ENABLED))
        assertFalse(preferences.contains("sync.cursor.pull.plan_items"))
    }

    @Test
    fun esborrarHoTotTreuTambeLAparenca() = runBlocking {
        preferences.edit().putString(ViraPreferences.APPEARANCE, "dark").commit()
        ViraDataEraser.eraseEverything(db, preferences)
        assertFalse(preferences.contains(ViraPreferences.APPEARANCE))
    }
}
