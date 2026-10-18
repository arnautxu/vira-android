package cat.vira.android.features.settings.export

/// Document de text pla, paginable: títol, seccions i files d'etiqueta + valor (`ViraPDFDocument`).
///
/// Deliberadament **pobre en tipus de contingut**: l'exportació en PDF de `PRIVACY_AND_SAFETY.md` §5
/// és «llegible per una persona», no un informe amb gràfics. Qui necessita les dades exactes ja té
/// el JSON.
data class ViraPdfDocument(
    val title: String,
    val subtitle: String,
    val sections: List<Section>,
) {
    data class Section(
        val title: String,
        /// Text sota el títol, quan la llista sola no s'explica (l'avís de les dades sensibles).
        val note: String? = null,
        val rows: List<Row>,
        /// Substitueix la llista quan no hi ha res. Una secció buida es dibuixa igualment: «no en
        /// tens» és informació, i fer-la desaparèixer fa dubtar de si s'ha exportat tot.
        val emptyText: String,
    )

    data class Row(
        val label: String,
        val value: String? = null,
        /// Segona línia, més petita: notes, dosis, detalls que no caben a l'etiqueta.
        val detail: String? = null,
    )
}
