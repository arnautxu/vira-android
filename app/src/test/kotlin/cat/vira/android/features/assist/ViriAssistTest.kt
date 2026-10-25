package cat.vira.android.features.assist

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/// «La partim?» (`ViriAssistProposalTests` i `ViriAssistTests` de l'iPhone).
class ViriAssistTest {

    @Test
    fun unaTascaDeFeinaLlargaSemblaGranUnaDePetitaNo() {
        listOf(
            "Treball de socials", "estudiar per l'examen de mates", "Presentación de historia", "History essay",
            "ordenar l'habitació", "anar a buscar la germana a l'escola i després passar pel súper",
            "Preparar l'exposició de naturals", "Study for the biology exam",
            "Comprar pa i després trucar a l'àvia i passar pel banc",
        ).forEach { assertTrue(it, ViriTaskSize.looksBig(it)) }
        listOf(
            "comprar pa", "trucar a l'àvia", "comprar un ordenador", "exam",
            "Agafar cartera per treballar", "Coger la cartera para ir al trabajo", "Anar al treball",
            "Enviar el treball de socials", "Comprar material pel projecte", "Preparar la motxilla",
            "Look at the example", "Agafar la cartera abans de sortir de casa",
        ).forEach { assertFalse(it, ViriTaskSize.looksBig(it)) }
    }

    @Test
    fun jevManaISiNoResponLaRegla() {
        assertEquals(ViriTaskSize.BIG, ViriTaskSize.fromJev("broad"))
        assertEquals(ViriTaskSize.SMALL, ViriTaskSize.fromJev("concrete"))
        assertNull(ViriTaskSize.fromJev("unknown"))
        assertFalse(ViriTaskSize.looksBig("Treball de socials", ViriTaskSize.SMALL))
        assertTrue(ViriTaskSize.looksBig("Netejar la cuina", ViriTaskSize.BIG))
        assertTrue(ViriTaskSize.looksBig("Treball de socials", null))
        assertFalse(ViriTaskSize.looksBig("Agafar cartera per treballar", null))
        assertFalse("massa curt per preguntar-ho", ViriTaskSize.looksBig("TDR", ViriTaskSize.BIG))
    }

    @Test
    fun laPeticioDePassosNomesPortaElTitolIElMoment() {
        val request = ViriAssist.stepsRequest("Treball\u0007 de socials  ", LocalDateTime.of(2026, 10, 20, 9, 5), "fr")
        assertEquals(setOf("kind", "locale", "today", "weekday", "now", "text"), request.keys)
        assertEquals("steps", request["kind"]!!.jsonPrimitive.content)
        assertEquals("en", request["locale"]!!.jsonPrimitive.content)
        assertEquals("2026-10-20", request["today"]!!.jsonPrimitive.content)
        assertEquals("tuesday", request["weekday"]!!.jsonPrimitive.content)
        assertEquals("09:05", request["now"]!!.jsonPrimitive.content)
        assertEquals("Treball de socials", request["text"]!!.jsonPrimitive.content)
        assertEquals(200, ViriAssist.clean("x".repeat(300), 200).length)
    }

    @Test
    fun nomesEsGuardenLesUltimesCentRespostes() {
        var stored = ""
        repeat(105) { stored = ViriAssist.declining("id$it", stored) }
        val list = stored.split(",")
        assertEquals(100, list.size)
        assertEquals("id5", list.first())
        assertEquals(stored, ViriAssist.declining("id50", stored))
    }

    @Test
    fun laRespostaDelServidorEsTornaAValidar() {
        fun json(text: String) = Json.parseToJsonElement(text).jsonObject
        val good = json("""{"policy_version":"viri-assist-1","kind":"steps","steps":[{"title":"Triar el tema","minutes":10},{"title":"","minutes":5}]}""")
        assertEquals(listOf(ViriAssist.StepProposal("Triar el tema", 10), ViriAssist.StepProposal("", 5)), ViriAssistClient.parseSteps(good))
        assertNull(ViriAssistClient.parseSteps(json("""{"policy_version":"viri-assist-0","kind":"steps","steps":[]}""")))
        assertNull(ViriAssistClient.parseSteps(json("""{"policy_version":"viri-assist-1","kind":"capture","steps":[]}""")))

        val id = "0b5c2f0e-8f53-4c49-9a52-3e9f3f2b6a11"
        fun scope(value: String, requestId: String = id.uppercase(), model: String = "jev-1.13.0") = json(
            """{"request_id":"$requestId","contract_version":2,"case_id":"C01","policy_version":"assistance-catalog-1","model_version":"$model","results":[{"id":"result","value":"$value","score":null}]}""",
        )
        assertEquals("broad", ViriAssistClient.parseScope(scope("broad"), id))
        assertNull(ViriAssistClient.parseScope(scope("maybe"), id))
        assertNull(ViriAssistClient.parseScope(scope("broad", requestId = "another"), id))
        assertNull(ViriAssistClient.parseScope(scope("broad", model = "jev-0"), id))
    }

    @Test
    fun elsPassosDeViriArribenDesmarcatsISenseRepetits() {
        val proposals = listOf(
            ViriAssist.StepProposal("Triar el tema", 10),
            ViriAssist.StepProposal("triar el téma", 5),
            ViriAssist.StepProposal("Treball de socials", 5),
            ViriAssist.StepProposal("  ", 5),
            ViriAssist.StepProposal("Buscar fonts", 20),
        )
        val steps = AssistanceDraft.viriSteps(proposals, excluding = listOf("Treball de socials"))
        assertEquals(listOf("Triar el tema", "Buscar fonts"), steps.map { it.text })
        assertTrue(steps.none { it.isSelected })

        // «Uns altres» conserva els marcats i els escrits a mà.
        val kept = listOf(steps[0].copy(isSelected = true), steps[1], AssistanceStep(text = "El meu", isSelected = true))
        val merged = AssistanceDraft.merged(kept, listOf(ViriAssist.StepProposal("Fer l'índex", 15)), "Treball de socials")!!
        assertEquals(listOf("Triar el tema", "El meu", "Fer l'índex"), merged.map { it.text })
        assertNull(AssistanceDraft.merged(kept, listOf(ViriAssist.StepProposal("triar el tema", 3)), "Treball"))
    }

    @Test
    fun nomesEsDesenElsMarcatsINets() {
        val steps = listOf(AssistanceStep(text = " A ", isSelected = true), AssistanceStep(text = "B"))
        assertEquals(listOf("A"), AssistanceDraft.selectedTitles(steps))
        assertEquals(AssistanceProblem.EMPTY, AssistanceDraft.problem(listOf(AssistanceStep(text = "B"))))
        assertEquals(AssistanceProblem.EMPTY, AssistanceDraft.problem(listOf(AssistanceStep(text = " ", isSelected = true))))
        assertEquals(
            AssistanceProblem.DUPLICATE,
            AssistanceDraft.problem(listOf(AssistanceStep(text = "Pas", isSelected = true), AssistanceStep(text = "pàs", isSelected = true))),
        )
        assertEquals(AssistanceProblem.TOO_LONG, AssistanceDraft.problem(listOf(AssistanceStep(text = "x".repeat(501), isSelected = true))))
    }
}
