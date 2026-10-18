package cat.vira.android.core.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/// Els textos exportats de l'iPhone han de dir el mateix en els tres idiomes: les mateixes claus
/// i els mateixos arguments. Si divergeixen, una pantalla petaria només en un idioma.
class ExportedStringsTest {
    private val folders = listOf("values", "values-es", "values-en")

    private fun read(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings_vira.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val entries = mutableMapOf<String, String>()
        for (index in 0 until root.childNodes.length) {
            val node = root.childNodes.item(index) as? Element ?: continue
            entries[node.tagName + ":" + node.getAttribute("name")] = node.textContent
        }
        return entries
    }

    private fun arguments(text: String): Set<String> =
        Regex("""%(\d+)\$[sdf]""").findAll(text).map { it.value }.toSet()

    @Test
    fun elsTresIdiomesTenenLesMateixesClaus() {
        val catalan = read("values")
        assertTrue("el català no hauria de ser buit", catalan.size > 1000)
        folders.drop(1).forEach { folder ->
            assertEquals("claus de $folder", catalan.keys, read(folder).keys)
        }
    }

    @Test
    fun elsTresIdiomesTenenElsMateixosArguments() {
        val catalan = read("values")
        folders.drop(1).forEach { folder ->
            val other = read(folder)
            catalan.forEach { (key, text) ->
                assertEquals("arguments de $key a $folder", arguments(text), arguments(other.getValue(key)))
            }
        }
    }
}
