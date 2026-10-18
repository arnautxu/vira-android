package cat.vira.android.features.progress

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cat.vira.android.features.progress.logic.StreakMessageCatalog
import cat.vira.android.features.progress.logic.StreakMessageCatalog.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/// La meitat de la targeta de `StreakCopyTests` de l'iPhone (la de les notificacions és de
/// Notificacions). Evita que la varietat es desfaci sola: una frase sense text, una de repetida o
/// una que el primer dia afirma una cosa falsa no es veuen llegint el codi.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StreakMessageCatalogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = object : StreakMessageCatalog.Storage {
        val values = mutableMapOf<String, String>()
        override fun get(key: String) = values[key]
        override fun set(key: String, value: String) { values[key] = value }
    }
    private val catalog = StreakMessageCatalog(storage, Random(7))

    private fun text(res: Int) = context.getString(res)

    private fun distinct(state: State, days: Int = 1, record: Boolean = false, rounds: Int = 1_000) =
        (0 until rounds).map { catalog.advance(state, days, record) }.toSet()

    @Test fun laTargetaTeLes31FrasesITotesTraduides() {
        val messages = distinct(State.RUNNING, days = 100, record = true)
        assertEquals(31, messages.size)
        assertTrue(messages.map(::text).all { it.isNotBlank() && !it.contains(".message") })
        assertEquals(31, messages.map(::text).toSet().size)
    }

    @Test fun elsAltresTresEstatsSonComplets() {
        assertEquals(14, distinct(State.FROZEN, rounds = 200).size)
        assertEquals(14, distinct(State.RESET, rounds = 200).size)
        assertEquals(13, distinct(State.NEVER, rounds = 200).size)
    }

    @Test fun laFraseRetiradaNoTornaASortir() {
        distinct(State.NEVER, rounds = 400).map(::text).map(String::lowercase).forEach {
            assertFalse(it, it.contains("barat") || it.contains("barato") || it.contains("cheapest"))
        }
    }

    @Test fun elPrimerDiaCapFraseParlaDelQueEncaraNoHaPassat() {
        val untrue = listOf(
            "ahir", "ayer", "yesterday", "rècord", "récord", "record", "casualitat", "casualidad", "costum",
            "costumbre", "patró", "patrón", "pattern", "constància", "constancia", "consistency",
            "un altre dia", "otro día", "another day", "has tornat", "has vuelto", "segueix viva",
            "sigue viva", "lives on", "continua", "sigue", "l'anterior", "el anterior", "respecte", "habit",
        )
        val messages = distinct(State.RUNNING, days = 1, record = false, rounds = 400)
        assertTrue(messages.size >= 8)
        // En els tres idiomes, com que els graons són per frase i no per idioma.
        for (locale in listOf("ca", "es", "en")) {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(java.util.Locale.forLanguageTag(locale))
            val localized = context.createConfigurationContext(config)
            messages.map { localized.getString(it).lowercase() }.forEach { message ->
                untrue.forEach { word -> assertFalse("el dia 1 no pot dir «$message»", message.contains(word)) }
            }
        }
    }

    @Test fun elCatalegSObreAMesuraQueLaRatxaCreix() {
        assertTrue(distinct(State.RUNNING, days = 1).size < distinct(State.RUNNING, days = 7).size)
        assertEquals(31, distinct(State.RUNNING, days = 100, record = true).size)
        assertEquals(29, distinct(State.RUNNING, days = 100).size)
    }

    @Test fun rotarMaiTornaLaFraseQueHiHavia() {
        for (state in State.entries) {
            var previous = catalog.message(state)
            repeat(60) {
                val next = catalog.advance(state)
                assertNotEquals("$state ha repetit", previous, next)
                previous = next
            }
        }
    }

    @Test fun llegirLaFraseNoLaCanvia() {
        catalog.advance(State.RUNNING)
        val first = catalog.message(State.RUNNING)
        repeat(20) { assertEquals(first, catalog.message(State.RUNNING)) }
    }

    @Test fun rotarLosTotsAlhoraElsMouTots() {
        val before = State.entries.map(catalog::message)
        catalog.advanceAll(days = 100, hasRecordAhead = true)
        val after = State.entries.map(catalog::message)
        before.zip(after).forEach { (b, a) -> assertNotEquals(b, a) }
    }

    @Test fun esDesaLaClauDeLIphone() {
        catalog.advance(State.FROZEN)
        assertTrue(storage.values.getValue("streak.message.key.frozen").startsWith("streak.frozen.message"))
    }
}
