package cat.vira.android.features.shop

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraShopBackdrop

/// Nivell de preu d'una skin (`SkinTier`, `VIRI_AND_GAMIFICATION.md` §2.2.1). El sostre és 3.000.
///
/// La raresa té color (l'accent d'una àrea que ja existeix, cap token nou), però **mai sol**: cada
/// graó porta el seu símbol i el seu nom escrit (`ShopGameKit` de l'iPhone).
enum class SkinTier(val price: Int, @StringRes val nameRes: Int, @StringRes val sectionRes: Int, val symbol: ShopSymbol) {
    Common(500, R.string.skin_tier_common, R.string.shop_tier_section_common, ShopSymbol.Circle),
    Special(1_500, R.string.skin_tier_special, R.string.shop_tier_section_special, ShopSymbol.Sparkles),
    Epic(2_200, R.string.skin_tier_epic, R.string.shop_tier_section_epic, ShopSymbol.Diamond),
    Legendary(3_000, R.string.skin_tier_legendary, R.string.shop_tier_section_legendary, ShopSymbol.Crown);

    val shopArea: ViraArea
        get() = when (this) {
            Common -> ViraArea.Plan
            Special -> ViraArea.Focus
            Epic -> ViraArea.Today
            Legendary -> ViraArea.Rewards
        }

    /// Gruix de la vora de la píndola del detall: la part de la distinció que no és color.
    val borderWidth: Float
        get() = when (this) {
            Common -> 1f
            Special -> 1.5f
            Epic -> 2f
            Legendary -> 3f
        }
}

/// Una skin del catàleg (`SkinItem`). **Contingut de l'app, no dades d'usuari**: de l'usuari només
/// se'n desa la propietat (`SkinOwnership`), per clau.
///
/// Les tres condicions de §2.5 es compleixen per construcció: cap IP de tercers, cap data de
/// caducitat ni «edició limitada», i una skin no desbloqueja mai res.
///
/// `art` és l'escena sencera, o Viri retallat si la peça ve per capes (`backdrop` no nul).
data class SkinItem(
    val key: String,
    @DrawableRes val art: Int?,
    @StringRes val nameRes: Int,
    val tier: SkinTier,
    @DrawableRes val backdrop: Int? = null,
    /// Retirada de la venda (Arnau, 2026-10-19): qui ja la té la conserva i la pot posar, però la
    /// Botiga només l'ensenya a qui la té (`ShopCollection.catalog`).
    val isRetired: Boolean = false,
) {
    val price: Int get() = tier.price

    /// Només es ven una peça que té l'art a l'app: una clau antiga pot seguir al catàleg (perquè una
    /// compra sincronitzada no perdi la identitat) sense que mai es vengui un espai buit.
    val artIsAvailable: Boolean get() = art != null

    val isLayered: Boolean get() = backdrop != null

    /// La il·lustració de fons: el decorat sol si va per capes, l'escena sencera si no.
    @get:DrawableRes val backdropArt: Int? get() = backdrop ?: art

    /// Viri retallat, o `null` si la peça és plana.
    @get:DrawableRes val cutoutArt: Int? get() = if (isLayered) art else null

    /// Color de la peça al detall. Va amb la peça, no amb el preu.
    val shopBackdrop: ViraShopBackdrop
        get() = when (key) {
            "chef" -> ViraShopBackdrop.Coral
            "diver" -> ViraShopBackdrop.Ocean
            "surfer" -> ViraShopBackdrop.Mint
            "pharaoh", "explorer" -> ViraShopBackdrop.Amber
            "astronaut" -> ViraShopBackdrop.Indigo
            else -> ViraShopBackdrop.Violet
        }

    companion object {
        /// Les skins, en l'ordre de l'iPhone (`SkinItem.catalog`). Les claus i els preus no es
        /// toquen mai: són els que viatgen amb el compte.
        val catalog: List<SkinItem> = listOf(
            SkinItem("chef", R.drawable.viri_skin_chef, R.string.skin_chef, SkinTier.Common),
            SkinItem("diver", R.drawable.viri_skin_diver, R.string.skin_diver, SkinTier.Common, isRetired = true),
            SkinItem("zombie", R.drawable.viri_skin_zombie, R.string.skin_zombie, SkinTier.Common),
            SkinItem("surfer", R.drawable.viri_skin_surfer, R.string.skin_surfer, SkinTier.Special, isRetired = true),
            // Primera peça per capes: Viri retallat i el decorat de la tomba a part.
            SkinItem(
                "explorer", R.drawable.viri_skin_explorer, R.string.skin_explorer, SkinTier.Special,
                backdrop = R.drawable.viri_skin_explorer_backdrop,
            ),
            SkinItem("mummy", R.drawable.viri_skin_mummy, R.string.skin_mummy, SkinTier.Epic),
            SkinItem("hacker", R.drawable.viri_skin_hacker, R.string.skin_hacker, SkinTier.Epic),
            SkinItem("pharaoh", R.drawable.viri_skin_pharaoh, R.string.skin_pharaoh, SkinTier.Legendary),
            SkinItem("gladiator", R.drawable.viri_skin_gladiator, R.string.skin_gladiator, SkinTier.Legendary),
            SkinItem("astronaut", R.drawable.viri_skin_astronaut, R.string.skin_astronaut, SkinTier.Special),
            SkinItem("wizard", R.drawable.viri_skin_wizard, R.string.skin_wizard, SkinTier.Legendary, isRetired = true),
            SkinItem("tennis", R.drawable.viri_skin_tennis, R.string.skin_tennis, SkinTier.Common, isRetired = true),
            SkinItem("hula", R.drawable.viri_skin_hula, R.string.skin_hula, SkinTier.Common),
            SkinItem("firefighter", R.drawable.viri_skin_firefighter, R.string.skin_firefighter, SkinTier.Special),
            SkinItem("disco", R.drawable.viri_skin_disco, R.string.skin_disco, SkinTier.Special, isRetired = true),
            SkinItem("mime", R.drawable.viri_skin_mime, R.string.skin_mime, SkinTier.Epic, isRetired = true),
            SkinItem("philosopher", R.drawable.viri_skin_philosopher, R.string.skin_philosopher, SkinTier.Epic),
            SkinItem("saint-george", R.drawable.viri_skin_saint_george, R.string.skin_saint_george, SkinTier.Legendary, isRetired = true),
            SkinItem("crystal", R.drawable.viri_skin_crystal, R.string.skin_crystal, SkinTier.Legendary, isRetired = true),
            // Retirades el 2026-10-19 (Arnau, en triar les quinze de sota): submarinista, tennista,
            // surfista, disco, mim, mag, Sant Jordi i cristall. Es queden aquí amb `isRetired`.
            // Quinze peces de `03_Viri/skins` (Arnau, 2026-10-19: «selecciona 15 tops»).
            SkinItem("panda", R.drawable.viri_skin_panda, R.string.skin_panda, SkinTier.Common),
            SkinItem("bee", R.drawable.viri_skin_bee, R.string.skin_bee, SkinTier.Common),
            SkinItem("skater", R.drawable.viri_skin_skater, R.string.skin_skater, SkinTier.Common),
            SkinItem("basketball", R.drawable.viri_skin_basketball, R.string.skin_basketball, SkinTier.Common),
            SkinItem("shark", R.drawable.viri_skin_shark, R.string.skin_shark, SkinTier.Special),
            SkinItem("dinosaur", R.drawable.viri_skin_dinosaur, R.string.skin_dinosaur, SkinTier.Special),
            SkinItem("detective", R.drawable.viri_skin_detective, R.string.skin_detective, SkinTier.Special),
            SkinItem("casteller", R.drawable.viri_skin_casteller, R.string.skin_casteller, SkinTier.Special),
            SkinItem("pirate", R.drawable.viri_skin_pirate, R.string.skin_pirate, SkinTier.Epic),
            SkinItem("rockstar", R.drawable.viri_skin_rockstar, R.string.skin_rockstar, SkinTier.Epic),
            SkinItem("vampire", R.drawable.viri_skin_vampire, R.string.skin_vampire, SkinTier.Epic),
            SkinItem("mad-scientist", R.drawable.viri_skin_mad_scientist, R.string.skin_mad_scientist, SkinTier.Epic),
            SkinItem("dragon", R.drawable.viri_skin_dragon, R.string.skin_dragon, SkinTier.Legendary),
            SkinItem("phoenix", R.drawable.viri_skin_phoenix, R.string.skin_phoenix, SkinTier.Legendary),
            SkinItem("superhero", R.drawable.viri_skin_superhero, R.string.skin_superhero, SkinTier.Legendary),
        )

        fun item(key: String): SkinItem? = catalog.firstOrNull { it.key == key }

        /// La següent que pots tenir (`ShopFeaturedCard.next`): la més barata que encara no és
        /// teva, i a igual preu la de clau més petita. `null` si ja les tens totes.
        fun next(catalog: List<SkinItem>, owned: Set<String>): SkinItem? =
            catalog.filter { it.artIsAvailable && it.key !in owned }
                .minWithOrNull(compareBy<SkinItem>({ it.price }, { it.key }))
    }
}
