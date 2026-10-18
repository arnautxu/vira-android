package cat.vira.android.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.ViraApp
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.components.ViraEmptyState
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileGrid
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Ajustos (`SettingsView`): el compte a dalt, les quatre famílies en fitxes i Sobre Vira al peu.
///
/// - **Mòdul protagonista**: la targeta lila del compte, amb la sincronització i l'aparença a un toc.
/// - **Fitxes de color** per a avisos, l'app, motivació i privacitat, cadascuna amb una píndola
///   que diu com està («4 de 5 encesos»).
/// - **Cerca** per paraules de cada família, sense majúscules ni accents.
///
/// Les dues pantalles que són d'altres fils entren com a portes: el panell de pares
/// (`onOpenParentPanel`) i l'editor de modes de concentració (`onEditFocusProfile`, `null` per a un
/// mode nou). Sense porta, la fila del panell surt com a pendent i les fitxes de mode no s'obren.
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onOpenParentPanel: (() -> Unit)? = null,
    onEditFocusProfile: ((FocusProfile?) -> Unit)? = null,
) {
    SettingsHost(rememberAppSettingsBackend()) {
        SettingsNavigation(onClose, onOpenParentPanel, onEditFocusProfile)
    }
}

/// El compte del pare o la mare (`AccountSettingsView`): qui ets i la sessió, amb les mateixes peces
/// que el compte del jove perquè les dues cares es vegin iguals.
@Composable
fun AccountSettingsScreen(onClose: () -> Unit) {
    SettingsHost(rememberAppSettingsBackend()) { AccountSettingsContent(onClose) }
}

@Composable
private fun rememberAppSettingsBackend(): SettingsBackend {
    val context = LocalContext.current.applicationContext
    return remember(context) { ViraSettingsBackend(context, (context as ViraApp).backend) }
}

/// Posa el backend i les preferències a l'abast de totes les seccions.
@Composable
fun SettingsHost(backend: SettingsBackend, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSettingsBackend provides backend,
        LocalSettingsPreferences provides backend.preferences,
        content = content,
    )
}

/// On és l'usuari dins d'Ajustos. La portada és la llista buida.
sealed interface SettingsPage {
    data class Category(val category: SettingsCategory) : SettingsPage
    data object FocusModes : SettingsPage
}

@Composable
fun SettingsNavigation(
    onClose: () -> Unit,
    onOpenParentPanel: (() -> Unit)? = null,
    onEditFocusProfile: ((FocusProfile?) -> Unit)? = null,
    initialStack: List<SettingsPage> = emptyList(),
) {
    var stack by remember { mutableStateOf(initialStack) }
    fun push(page: SettingsPage) { stack = stack + page }
    fun pop() { stack = stack.dropLast(1) }
    BackHandler { if (stack.isEmpty()) onClose() else pop() }

    when (val page = stack.lastOrNull()) {
        null -> SettingsHomePage(onClose, onOpen = { push(SettingsPage.Category(it)) })
        is SettingsPage.Category -> key(page) {
            SettingsCategoryPage(
                category = page.category,
                onBack = ::pop,
                // Esborrar-ho tot tanca Ajustos sencer, no només aquesta pantalla.
                onErased = onClose,
                onOpenParentPanel = onOpenParentPanel,
                onOpenFocusModes = { push(SettingsPage.FocusModes) },
            )
        }
        SettingsPage.FocusModes -> SettingsPageScaffold(ViraArea.Focus, onBack = ::pop) {
            ViraPageTitle(stringResource(R.string.focus_modes_title), ViraArea.Focus)
            FocusProfilesContent(onEdit = { onEditFocusProfile?.invoke(it) })
        }
    }
}

/// El marc comú: fons de l'àrea, la barra amb «Enrere» o «Fet» i el contingut desplaçable.
@Composable
fun SettingsPageScaffold(
    area: ViraArea,
    onBack: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalSettingsArea provides area) {
        Column(Modifier.fillMaxSize().viraSectionBackground(area).safeDrawingPadding()) {
            SettingsBar(area, onBack, onDone)
            header?.invoke()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ViraSpacing.screenMargin)
                    .padding(bottom = ViraSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(ViraSpacing.l),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsBar(area: ViraArea, onBack: (() -> Unit)?, onDone: (() -> Unit)?) {
    val accent = ViraColor.accent(area)
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = ViraTouch.minimum).padding(horizontal = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Row(
                Modifier.viraPressable(role = Role.Button, onClick = onBack)
                    .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
                    .padding(horizontal = ViraSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
            ) {
                SettingsGlyph(SettingsGlyphKind.Back, accent)
                Text(stringResource(R.string.action_back), style = ViraFont.body, color = accent)
            }
        }
        Spacer(Modifier.weight(1f))
        if (onDone != null) {
            Box(
                Modifier.viraPressable(role = Role.Button, onClick = onDone)
                    .defaultMinSize(minWidth = ViraTouch.minimum, minHeight = ViraTouch.minimum)
                    .padding(horizontal = ViraSpacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.action_done), style = ViraFont.headline, color = accent)
            }
        }
    }
}

@Composable
private fun SettingsHomePage(onClose: () -> Unit, onOpen: (SettingsCategory) -> Unit) {
    var query by remember { mutableStateOf("") }
    val tally = rememberSettingsTally()
    SettingsPageScaffold(ViraArea.Today, onDone = onClose) {
        ViraPageTitle(stringResource(R.string.settings_title), ViraArea.Today)
        SettingsSearchField(query) { query = it }
        if (query.isBlank()) {
            SettingsAccountCard(onOpenAccount = { onOpen(SettingsCategory.ACCOUNT) })
            SettingsFamilies(tally, onOpen)
            SettingsFooterRows(SettingsCategory.footer, onOpen)
        } else {
            SettingsSearchResults(query, onOpen)
        }
    }
}

@Composable
private fun SettingsSearchField(query: String, onChange: (String) -> Unit) {
    val prompt = stringResource(R.string.settings_search_prompt)
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text(prompt, style = ViraFont.body, color = ViraColor.textSecondary) },
        singleLine = true,
        textStyle = ViraFont.body,
        shape = RoundedCornerShape(ViraRadius.m),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ViraColor.surfaceElevated,
            unfocusedContainerColor = ViraColor.surfaceElevated,
            focusedTextColor = ViraColor.textPrimary,
            unfocusedTextColor = ViraColor.textPrimary,
            focusedBorderColor = ViraColor.accent(ViraArea.Today),
            unfocusedBorderColor = ViraColor.separator,
            cursorColor = ViraColor.accent(ViraArea.Today),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsFamilies(tally: SettingsTally, onOpen: (SettingsCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
        Text(
            stringResource(R.string.settings_families_title),
            style = ViraFont.headline,
            color = ViraColor.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        ViraTileGrid(SettingsCategory.families) { family, modifier ->
            val summary = tally.summary(family)
            ViraTile(
                area = family.area,
                pictogram = family.pictogram ?: R.drawable.picto_set_general,
                title = stringResource(family.title),
                subtitle = stringResource(family.detail),
                modifier = modifier
                    .viraPressable(role = Role.Button, onClick = { onOpen(family) })
                    .semantics(mergeDescendants = true) {},
                foot = if (summary != null) { { ViraTag(summary) } } else null,
            )
        }
    }
}

/// Sobre Vira, i els resultats de la cerca: files blanques, no fitxes de color.
@Composable
private fun SettingsFooterRows(items: List<SettingsCategory>, onOpen: (SettingsCategory) -> Unit) {
    Column(Modifier.fillMaxWidth().background(ViraColor.surfaceElevated, RoundedCornerShape(ViraRadius.l))) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = ViraSpacing.m), color = ViraColor.separator)
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ViraTouch.comfortable)
                    .viraPressable(role = Role.Button, onClick = { onOpen(item) })
                    .semantics(mergeDescendants = true) {}
                    .padding(ViraSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
            ) {
                val pictogram = item.pictogram
                if (pictogram != null) {
                    ViraPictogram(pictogram, size = 36.dp, scalesWithText = true)
                } else {
                    Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                        SettingsGlyph(SettingsGlyphKind.Info, ViraColor.accent(item.area), extent = 24.dp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                    Text(stringResource(item.title), style = ViraFont.headline, color = ViraColor.textPrimary)
                    Text(stringResource(item.detail), style = ViraFont.footnote, color = ViraColor.textSecondary)
                }
                SettingsGlyph(SettingsGlyphKind.Chevron, ViraColor.textSecondary)
            }
        }
    }
}

@Composable
private fun SettingsSearchResults(query: String, onOpen: (SettingsCategory) -> Unit) {
    val matches = SettingsCategory.entries.filter { SettingsCategory.matches(query, it.searchKeywords()) }
    if (matches.isEmpty()) {
        ViraEmptyState(title = query.trim(), message = stringResource(R.string.settings_search_prompt))
    } else {
        SettingsFooterRows(matches, onOpen)
    }
}

/// El contingut d'una família (`SettingsCategoryView`): un repartidor de les seccions que ja
/// existeixen, sense lògica pròpia.
@Composable
fun SettingsCategoryPage(
    category: SettingsCategory,
    onBack: () -> Unit,
    onErased: () -> Unit,
    onOpenParentPanel: (() -> Unit)?,
    onOpenFocusModes: () -> Unit,
) {
    val tally = rememberSettingsTally()
    SettingsPageScaffold(category.area, onBack = onBack) {
        ViraPageTitle(stringResource(category.title), category.area)
        if (category in SettingsCategory.families || category == SettingsCategory.ACCOUNT) {
            SettingsCategoryHero(category, tally)
        }
        when (category) {
            // Una sola pantalla: qui ets a dalt, què es copia, la família i, al final, la sessió.
            SettingsCategory.ACCOUNT -> {
                SettingsSyncSection()
                SettingsParentPanelSection(isAvailable = onOpenParentPanel != null, onOpen = { onOpenParentPanel?.invoke() })
                SettingsSessionSection(onFinished = onErased)
            }
            SettingsCategory.NOTIFICATIONS -> SettingsNotificationsSection()
            // L'aparença és a la portada, a la targeta del compte.
            SettingsCategory.APP -> {
                SettingsModulesSection()
                SettingsFocusModesSection(onOpenFocusModes)
            }
            SettingsCategory.MOTIVATION -> SettingsMotivationSection()
            SettingsCategory.PRIVACY -> {
                SettingsPrivacySection()
                SettingsDataSection(onErased)
            }
            SettingsCategory.ABOUT -> SettingsAboutSection()
        }
    }
}

@Composable
fun AccountSettingsContent(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    SettingsPageScaffold(ViraArea.Today, onBack = onClose) {
        ViraPageTitle(stringResource(R.string.account_settings_title), ViraArea.Today)
        SettingsAccountHero()
        SettingsSessionSection(onFinished = onClose)
    }
}
