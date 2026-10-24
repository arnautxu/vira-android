package cat.vira.android.features.assist

import java.text.Normalizer

/// Si una tasca és gran per a «La partim?» (`ViriTaskSize` de l'iPhone): ho diu Jev i, si no
/// respon, la regla del telèfon (`looksBig`). Arnau, 2026-10-22: «Agafar cartera per treballar» li
/// va semblar gran i va triar «Reactivar Jev». A Jev només hi va el títol i l'idioma, i només quan
/// «La partim?» ja es podria proposar (`ViriAssist.canUse`).
enum class ViriTaskSize {
    BIG, SMALL;

    companion object {
        /// El que s'espera Jev quan no queda cap pas per preguntar abans de desar. Si tarda més,
        /// mana la regla i la persona no s'espera.
        const val PATIENCE_MILLIS = 1_500L

        /// «broad» és gran, «concrete» és petita; «unknown» no diu res i decideix la regla.
        fun fromJev(value: String?): ViriTaskSize? = when (value) {
            "broad" -> BIG
            "concrete" -> SMALL
            else -> null
        }

        /// Una tasca de dues lletres no es pregunta: ja és el pas.
        fun isWorthAsking(title: String): Boolean = title.trim().length >= 5

        /// Jev, si ha respost per a aquest títol; si no, la regla.
        fun looksBig(title: String, jev: ViriTaskSize?): Boolean {
            if (!isWorthAsking(title)) return false
            return jev?.let { it == BIG } ?: looksBig(title)
        }

        // La regla del telèfon (`ViriAssistProposal.looksBig`)

        /// Paraules de feina llarga, en ca/es/en, que es comparen **senceres** (amb el plural):
        /// «treball» no ha de caçar «treballar», ni «exam», «example». Sense accents ni majúscules.
        private val bigWords = setOf(
            // Català
            "treball", "treballs", "projecte", "projectes", "examen", "examens", "presentacio",
            "presentacions", "redaccio", "redaccions", "exposicio", "exposicions", "informe", "informes",
            "memoria", "recerca", "dossier", "tdr", "tfg", "maqueta", "maquetes", "mural",
            // Castellà
            "trabajo", "trabajos", "proyecto", "proyectos", "presentacion", "presentaciones",
            "exposicion", "exposiciones", "investigacion", "redaccion", "examenes",
            // Anglès
            "project", "projects", "essay", "essays", "exam", "exams", "presentation", "presentations",
            "report", "research", "assignment", "assignments", "coursework", "study", "studying",
            "revise", "revising",
        )

        /// Començaments que ja diuen «feina de diverses passes»: «estudia» caça «estudiar».
        private val bigStarts = listOf(
            "estudia", "repassa", "repasa", "endreca", "ordenar l", "ordenar la", "ordenar el",
            "ordenar mi", "comentari de text", "comentario de texto", "clean my room",
        )

        /// Davant d'una paraula de feina diuen on vas, no què has de fer: «anar al treball».
        private val placeWords = setOf("al", "del", "pel")

        /// Verbs d'una sola acció ràpida: «Enviar el treball» és un encàrrec.
        private val quickVerbs = setOf(
            // Català
            "agafar", "agafa", "comprar", "compra", "portar", "porta", "trucar", "truca", "enviar",
            "envia", "pagar", "paga", "recollir", "recull", "treure", "treu", "posar", "posa",
            "imprimir", "imprimeix", "entregar", "entrega", "lliurar", "penjar", "penja", "tornar",
            "retornar", "demanar", "signar", "firmar", "regar",
            // Castellà
            "coger", "coge", "llevar", "lleva", "llamar", "llama", "mandar", "manda", "recoger",
            "recoge", "sacar", "saca", "poner", "pon", "devolver", "pedir", "traer", "trae",
            // Anglès
            "take", "grab", "buy", "bring", "call", "phone", "text", "send", "pay", "pick", "print",
            "hand", "submit", "return", "sign", "email",
        )

        /// Dues coses en una: set paraules o més i un «i», una coma o un «després» que les uneix.
        private val joiners = setOf("i", "y", "and", "despres", "luego", "then")

        /// Pistes del text i prou, al telèfon: dues coses en una, o una paraula de feina llarga que
        /// no ve darrere d'un verb ràpid.
        fun looksBig(title: String): Boolean {
            val folded = fold(title.trim())
            if (folded.length < 5) return false
            val words = folded.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
                .split(' ').filter { it.isNotEmpty() }
            if (folded.split(Regex("\\s+")).count { it.isNotEmpty() } >= 7 &&
                (',' in folded || words.any { it in joiners })
            ) return true
            if (words.firstOrNull() in quickVerbs) return false
            val padded = " " + words.joinToString(" ")
            if (bigStarts.any { padded.contains(" $it") }) return true
            return words.indices.any { index ->
                words[index] in bigWords && !(index > 0 && words[index - 1] in placeWords)
            }
        }

        private fun fold(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    }
}
