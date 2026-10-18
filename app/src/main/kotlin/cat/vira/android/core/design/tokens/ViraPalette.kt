package cat.vira.android.core.design.tokens

/// Els valors de color de Vira, **calcats d'un en un** de `ViraColor.swift`, `ViraGradient.swift`
/// i `ViraColor+Intensity.swift` de l'iPhone. Cap valor nou: `DESIGN_RULES.md` §0 ho prohibeix, i
/// si un d'aquí canvia, primer ha de canviar a l'iPhone.
///
/// Viuen com a dades pures (sense Compose) perquè les proves de contrast (`ViraPaletteContrastTest`)
/// els puguin mesurar a la JVM, i perquè la mateixa xifra no s'escrigui dues vegades. Les pantalles
/// no les fan servir mai directament: passen per `ViraColor`, que tria el to clar o fosc.

/// Un to amb les seves dues aparences. El mode fosc no és una inversió: les superfícies de color
/// baixen de lluminositat, no de saturació.
data class ViraTone(val light: Long, val dark: Long) {
    constructor(both: Long) : this(both, both)

    fun pick(isDark: Boolean): Long = if (isDark) dark else light
}

/// Les cinc àrees del producte. Un color per àrea (`DESIGN_RULES.md` §7): Inici lila, Organització
/// blau, Emocions rosa, Focus verd, Viri i recompenses groc.
enum class ViraArea { Today, Plan, Emotions, Focus, Rewards }

/// Fons d'un ítem de la Botiga (`ViraColor.ShopBackdrop`).
enum class ViraShopBackdrop { Neutral, Coral, Ocean, Mint, Amber, Indigo, Violet }

object ViraPalette {

    // Marca

    /// Lila corporatiu. Només per a identitat i grans superfícies decoratives: mai sota text petit.
    val brand = ViraTone(0xFFBB77FF)
    val brandAccent = ViraTone(0xFF8B3FD9, 0xFFC79AFF)
    val brandSurface = ViraTone(0xFFF4ECFF, 0xFF241A33)
    val brandOnSurface = ViraTone(0xFF5B2494, 0xFFD9BDFF)

    // Àrees, en tres rols: accent, superfície i text sobre superfície (`DESIGN_DIRECTION.md` §4).

    fun accent(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> brandAccent
        ViraArea.Plan -> ViraTone(0xFF2E7D9A, 0xFF7FC4DE)
        ViraArea.Emotions -> ViraTone(0xFFC9345C, 0xFFFF9DB3)
        ViraArea.Focus -> ViraTone(0xFF5A7A1E, 0xFFAFD063)
        ViraArea.Rewards -> ViraTone(0xFF9A6E00, 0xFFF0C24E)
    }

    fun surface(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> brandSurface
        ViraArea.Plan -> ViraTone(0xFFE8F4F9, 0xFF12262E)
        ViraArea.Emotions -> ViraTone(0xFFFFEEF2, 0xFF2E1219)
        ViraArea.Focus -> ViraTone(0xFFF1F7E2, 0xFF1B2410)
        ViraArea.Rewards -> ViraTone(0xFFFFF6E0, 0xFF2B2107)
    }

    /// Fons de targeta modular: els colors saturats originals de la identitat.
    fun cardFill(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFFBB77FF, 0xFF4A2178)
        ViraArea.Plan -> ViraTone(0xFF83BCD4, 0xFF1D4C5E)
        ViraArea.Emotions -> ViraTone(0xFFFF7797, 0xFF6E2237)
        ViraArea.Focus -> ViraTone(0xFFB1D05E, 0xFF3E5417)
        ViraArea.Rewards -> ViraTone(0xFFFFC33E, 0xFF6A4C08)
    }

    /// Text sobre una targeta modular. Fosc del mateix to, **mai blanc**.
    fun onCard(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFF2A0559, 0xFFEADCFF)
        ViraArea.Plan -> ViraTone(0xFF0B2C36, 0xFFD6EEF7)
        ViraArea.Emotions -> ViraTone(0xFF4A0718, 0xFFFFDDE4)
        ViraArea.Focus -> ViraTone(0xFF2A3A08, 0xFFE7F3C8)
        ViraArea.Rewards -> ViraTone(0xFF452D00, 0xFFFFEFC4)
    }

    fun onSurface(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> brandOnSurface
        ViraArea.Plan -> ViraTone(0xFF1B5468, 0xFFB9E0EE)
        ViraArea.Emotions -> ViraTone(0xFF8E1F3C, 0xFFFFC7D3)
        ViraArea.Focus -> ViraTone(0xFF3D5413, 0xFFD3E8A4)
        ViraArea.Rewards -> ViraTone(0xFF6B4C00, 0xFFF7DFA0)
    }

    /// Text i icones a sobre d'un farciment d'accent. S'inverteix amb el mode: en fosc els accents
    /// són tons clars, i blanc a sobre no es llegiria.
    val onAccent = ViraTone(0xFFFFFFFF, 0xFF0F0D12)

    /// Text sobre una il·lustració de marca clara d'aparença fixa. No s'inverteix: la il·lustració tampoc.
    val onBrandArtwork = ViraTone(0xFF2A0559)

    /// Vel clar sobre una il·lustració clara, just on hi va text fosc.
    val lightArtworkVeil = ViraTone(0xFFFFFFFF)

    /// Text sobre una il·lustració fosca d'aparença fixa.
    val onDarkArtwork = ViraTone(0xFFF6F2F8)

    /// Fons i text d'un botó d'un proveïdor extern (Google, Apple): les normes de marca només
    /// admeten aquests dos contenidors.
    val providerSurface = ViraTone(0xFF131314, 0xFFFFFFFF)
    val onProviderSurface = ViraTone(0xFFFFFFFF, 0xFF1F1F1F)

    // Botiga

    fun shopBackdrop(backdrop: ViraShopBackdrop): ViraTone = when (backdrop) {
        ViraShopBackdrop.Neutral -> ViraTone(0xFFFFFFFF, 0xFF231F2B)
        ViraShopBackdrop.Coral -> ViraTone(0xFFFFEDE6, 0xFF33180F)
        ViraShopBackdrop.Ocean -> ViraTone(0xFFE4F1FA, 0xFF102632)
        ViraShopBackdrop.Mint -> ViraTone(0xFFE3F5EC, 0xFF0E2A1E)
        ViraShopBackdrop.Amber -> ViraTone(0xFFFFF3DC, 0xFF33240A)
        ViraShopBackdrop.Indigo -> ViraTone(0xFFE9ECFB, 0xFF161A33)
        ViraShopBackdrop.Violet -> brandSurface
    }

    fun onShopBackdrop(backdrop: ViraShopBackdrop): ViraTone = when (backdrop) {
        ViraShopBackdrop.Neutral -> textPrimary
        ViraShopBackdrop.Coral -> ViraTone(0xFF8A3111, 0xFFFFC7AF)
        ViraShopBackdrop.Ocean -> ViraTone(0xFF14506B, 0xFFB6DEF2)
        ViraShopBackdrop.Mint -> ViraTone(0xFF14573A, 0xFFAEE3C7)
        ViraShopBackdrop.Amber -> ViraTone(0xFF6B4A00, 0xFFF7DFA0)
        ViraShopBackdrop.Indigo -> ViraTone(0xFF2E3B8F, 0xFFBCC5F7)
        ViraShopBackdrop.Violet -> brandOnSurface
    }

    // Neutres

    val background = ViraTone(0xFFFFFFFF, 0xFF0F0D12)
    val backgroundGrouped = ViraTone(0xFFF6F4F8, 0xFF1A1720)
    val surfaceElevated = ViraTone(0xFFFFFFFF, 0xFF231F2B)

    /// Negre al 75 %, no negre pur: decisió heretada de la identitat original.
    val textPrimary = ViraTone(0xFF231F20, 0xFFF4F2F6)
    val textSecondary = ViraTone(0xFF6B6570, 0xFFA8A2B0)
    val separator = ViraTone(0xFFE6E2EA, 0xFF332E3C)

    /// Contorn d'un control sense farciment propi: 3:1 contra el fons (WCAG 1.4.11).
    val controlBorder = ViraTone(0xFF857E8B, 0xFF777083)

    // Estats

    val success = ViraTone(0xFF2E7D32, 0xFF81C784)
    val warning = ViraTone(0xFFB06000, 0xFFFFB74D)
    val error = ViraTone(0xFFC62828, 0xFFEF9A9A)

    // Intensitat del check-in: cinc tons de lila, el mateix en clar i en fosc a posta.

    private val intensityTops = longArrayOf(0xFFE5D4F5, 0xFFD7BCEF, 0xFFC59CE8, 0xFF8D52C5, 0xFF7833B7)
    private val intensityBottoms = longArrayOf(0xFFCDB3E8, 0xFFB799DE, 0xFFA57AD2, 0xFF673497, 0xFF4D1D84)

    fun intensityTop(level: Int): ViraTone = ViraTone(intensityTops[clampedLevel(level)])
    fun intensityBottom(level: Int): ViraTone = ViraTone(intensityBottoms[clampedLevel(level)])

    /// Els tres primers porten tinta fosca i els dos últims blanca, que és on cada un passa 4,5:1.
    fun onIntensity(level: Int): ViraTone = ViraTone(if (level < 4) 0xFF241135 else 0xFFFFFFFF)

    private fun clampedLevel(level: Int) = level.coerceIn(1, 5) - 1

    // Tons dels degradats (`ViraGradient.swift`). Cada un s'allunya sempre del color del text, i
    // per això cap degradat baixa cap contrast ja mesurat.

    /// Extrem il·luminat de la targeta: el mateix to, girat i portat cap al costat segur.
    fun cardFillStart(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFFB79DFF, 0xFF2D1C65)
        ViraArea.Plan -> ViraTone(0xFFAEC5E3, 0xFF1B3859)
        ViraArea.Emotions -> ViraTone(0xFFFF939E, 0xFF561A23)
        ViraArea.Focus -> ViraTone(0xFFB2DE8D, 0xFF254312)
        ViraArea.Rewards -> ViraTone(0xFFFFD19E, 0xFF5E3507)
    }

    /// Extrem inferior de la targeta: un 10 % més fosc que la base en clar.
    fun cardFillEnd(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFFA86BE6, 0xFF371858)
        ViraArea.Plan -> ViraTone(0xFF76A9BF, 0xFF143846)
        ViraArea.Emotions -> ViraTone(0xFFE66B88, 0xFF531828)
        ViraArea.Focus -> ViraTone(0xFF9FBB55, 0xFF2D3E0F)
        ViraArea.Rewards -> ViraTone(0xFFE6B038, 0xFF4E3805)
    }

    /// Extrem del degradat d'accent: més fosc en clar (etiqueta blanca), més clar en fosc.
    fun accentEnd(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFF582CD5, 0xFFC9BBFF)
        ViraArea.Plan -> ViraTone(0xFF2D5F97, 0xFFBDD4EE)
        ViraArea.Emotions -> ViraTone(0xFFA22A3A, 0xFFFFC0C6)
        ViraArea.Focus -> ViraTone(0xFF366419, 0xFFC6E6AD)
        ViraArea.Rewards -> ViraTone(0xFF8E4E00, 0xFFF9DCB9)
    }

    /// Superfície de secció amb el to girat i la mateixa lluminància que `surface`.
    fun surfaceShift(area: ViraArea): ViraTone = when (area) {
        ViraArea.Today -> ViraTone(0xFFF0EDFF, 0xFF201B35)
        ViraArea.Plan -> ViraTone(0xFFEDF3FA, 0xFF152437)
        ViraArea.Emotions -> ViraTone(0xFFFFEEF0, 0xFF2E1215)
        ViraArea.Focus -> ViraTone(0xFFEDF8E5, 0xFF162510)
        ViraArea.Rewards -> ViraTone(0xFFFFF5E9, 0xFF311F08)
    }

    fun shopBackdropEnd(backdrop: ViraShopBackdrop): ViraTone = when (backdrop) {
        ViraShopBackdrop.Neutral -> ViraTone(0xFFF6F4F8, 0xFF1A1720)
        ViraShopBackdrop.Coral -> ViraTone(0xFFFFDFD2, 0xFF26110A)
        ViraShopBackdrop.Ocean -> ViraTone(0xFFD3E7F5, 0xFF0B1B24)
        ViraShopBackdrop.Mint -> ViraTone(0xFFD2EDE0, 0xFF091E15)
        ViraShopBackdrop.Amber -> ViraTone(0xFFFCE8C6, 0xFF261A06)
        ViraShopBackdrop.Indigo -> ViraTone(0xFFDADFF6, 0xFF101326)
        ViraShopBackdrop.Violet -> ViraTone(0xFFE9DDFC, 0xFF1A1226)
    }
}
