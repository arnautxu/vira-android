package cat.vira.android.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import cat.vira.android.R
import cat.vira.android.core.backend.ViraAccountRole
import cat.vira.android.core.backend.ViraConsentStatus
import cat.vira.android.core.data.ViraPreferences
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraSectionItem
import cat.vira.android.core.design.components.ViraSectionSwitcher
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

private val cardShape = RoundedCornerShape(ViraRadius.l)

/// Mòdul protagonista de la portada d'Ajustos (`SettingsAccountCard`, `DESIGN_RULES.md` §3): qui
/// ets, com va la sincronització i l'aparença, en una targeta lila. L'aparença és aquí, a un toc:
/// és l'ajust que més es toca. Tocar la part de dalt obre el compte sencer.
@Composable
fun SettingsAccountCard(onOpenAccount: () -> Unit, modifier: Modifier = Modifier) {
    val ink = ViraColor.onCard(ViraArea.Today)
    var appearance by rememberStringPreference(ViraPreferences.APPEARANCE, ViraAppearance.AUTO.raw)
    val hint = stringResource(R.string.settings_account_card_hint)
    Column(
        modifier.fillMaxWidth().background(ViraGradient.card(ViraArea.Today), cardShape).padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.m),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .viraPressable(onClickLabel = hint, onClick = onOpenAccount)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIdentity(Modifier.weight(1f))
                SettingsGlyph(SettingsGlyphKind.Chevron, ink)
            }
            SettingsSyncTag()
        }
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            Text(stringResource(R.string.settings_appearance_section), style = ViraFont.footnoteEmphasis, color = ink)
            ViraSectionSwitcher(
                area = ViraArea.Today,
                items = listOf(
                    ViraSectionItem(ViraAppearance.AUTO.raw, stringResource(R.string.settings_appearance_auto), R.drawable.picto_set_appearance),
                    ViraSectionItem(ViraAppearance.LIGHT.raw, stringResource(R.string.settings_appearance_light), R.drawable.picto_sun),
                    ViraSectionItem(ViraAppearance.DARK.raw, stringResource(R.string.settings_appearance_dark), R.drawable.picto_moon),
                ),
                selectedKey = ViraAppearance.from(appearance).raw,
                onSelect = { appearance = it },
                showsAllLabels = true,
            )
        }
    }
}

/// La targeta lila de dalt del compte (`SettingsAccountHero`): qui ets i, en píndoles, com va la
/// sincronització i el permís del tutor. El permís només surt quan **falta**: `GRANTED` també vol
/// dir «no cal» (16 anys o més). Un compte de pare o mare porta el rol en lloc de la sincronització.
@Composable
fun SettingsAccountHero(modifier: Modifier = Modifier) {
    val user = rememberSettingsUser()
    Column(
        modifier.fillMaxWidth().background(ViraGradient.card(ViraArea.Today), cardShape).padding(ViraSpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        SettingsIdentity()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs), verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
            if (user?.role == ViraAccountRole.PARENT) {
                ViraTag(stringResource(R.string.account_role_parent), pictogram = R.drawable.picto_habit_friends)
            } else {
                SettingsSyncTag()
                val consent = when {
                    user == null || user.role != ViraAccountRole.TEEN -> null
                    user.consent == ViraConsentStatus.PENDING -> R.string.settings_account_consent_pending
                    user.consent == ViraConsentStatus.DECLINED -> R.string.settings_account_consent_declined
                    user.consent == ViraConsentStatus.MISSING -> R.string.settings_account_consent_missing
                    else -> null
                }
                consent?.let { ViraTag(stringResource(it), pictogram = R.drawable.picto_shield) }
            }
        }
    }
}

/// La targeta de dalt de cada família (`SettingsCategoryHero`): avisos, l'app i motivació porten
/// el resum —quants interruptors hi ha encesos, en text i en barra, mai només la barra—;
/// privacitat porta Viri a la bombolla protectora, l'única aparició expressiva d'Ajustos.
@Composable
fun SettingsCategoryHero(category: SettingsCategory, tally: SettingsTally, modifier: Modifier = Modifier) {
    when (category) {
        SettingsCategory.ACCOUNT -> SettingsAccountHero(modifier)
        SettingsCategory.PRIVACY -> SettingsSceneCard(
            scene = R.drawable.viri_scene_privacy,
            message = stringResource(R.string.settings_privacy_viri),
            area = category.area,
            modifier = modifier,
        ) {
            ViraTag(tally.summary(category).orEmpty(), pictogram = R.drawable.picto_key)
        }
        else -> tally.count(category)?.let { count ->
            val ink = ViraColor.onCard(category.area)
            Column(
                modifier.fillMaxWidth().background(ViraGradient.card(category.area), cardShape).padding(ViraSpacing.m)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    Text(stringResource(category.heroTitle), style = ViraFont.headline, color = ink, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.challenge_progress, count.on, count.total), style = ViraFont.headline, color = ink)
                }
                val track = 8.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
                Box(Modifier.fillMaxWidth().height(track).clip(CircleShape).background(ink.copy(alpha = 0.18f)).clearAndSetSemantics {}) {
                    Box(Modifier.fillMaxWidth(count.fraction).fillMaxHeight().background(ink, CircleShape))
                }
                Text(stringResource(category.detail), style = ViraFont.footnote, color = ink)
            }
        }
    }
}

/// Escena de Viri a dalt i la bombolla que en surt a sota (`RewardsSceneCard`): el text mai va
/// sobre la il·lustració.
@Composable
fun SettingsSceneCard(
    scene: Int,
    message: String,
    area: ViraArea,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bubbleShape = RoundedCornerShape(ViraRadius.l)
    val sceneHeight = if (LocalViraEnvironment.current.isAccessibilitySize) 120.dp else 184.dp
    val spoken = stringResource(R.string.today_viri_a11y, message)
    Column(
        modifier.fillMaxWidth()
            .shadow(14.dp, cardShape, ambientColor = ViraColor.accent(area).copy(alpha = 0.18f), spotColor = ViraColor.accent(area).copy(alpha = 0.18f))
            .clip(cardShape)
            .background(ViraGradient.card(area)),
    ) {
        Image(
            painterResource(scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(sceneHeight),
        )
        Column(
            // La bombolla trepitja la vora de l'escena: es llegeix com que surt de Viri.
            Modifier.pullUp(ViraSpacing.xxl).padding(horizontal = ViraSpacing.m).padding(bottom = ViraSpacing.m),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
        ) {
            Text(
                message,
                style = ViraFont.headline,
                color = ViraColor.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, bubbleShape, ambientColor = ViraColor.accent(area).copy(alpha = 0.16f), spotColor = ViraColor.accent(area).copy(alpha = 0.16f))
                    .background(ViraColor.surfaceElevated, bubbleShape)
                    .padding(horizontal = ViraSpacing.m, vertical = ViraSpacing.s)
                    .semantics { contentDescription = spoken },
            )
            content()
        }
    }
}

/// Puja la peça `distance` sense deixar-ne el forat a sota (el `padding` negatiu de l'iPhone).
private fun Modifier.pullUp(distance: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = distance.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}
