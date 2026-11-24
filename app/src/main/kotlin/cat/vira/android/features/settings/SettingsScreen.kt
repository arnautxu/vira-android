package cat.vira.android.features.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.app.LocalViraBackend
import cat.vira.android.core.data.model.FocusProfile
import cat.vira.android.core.design.components.ViraPageTitle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.components.viraSectionBackground
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Ajustos (`SettingsView`): una sola llista, de dalt a baix (Arnau, 2026-11-24). L'avatar de la
/// capçalera hi porta d'un toc. Les pàgines de família (`SettingsCategoryPage`) es queden per al
/// compte, Sobre Vira i les captures.
///
/// Les dues pantalles que són d'altres fils entren com a portes: el panell de pares
/// (`onOpenParentPanel`) i l'editor de modes de concentració (`onEditFocusProfile`, `null` per a un
/// mode nou). Sense porta, les fitxes de mode no s'obren.
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
    val backend = LocalViraBackend.current
    return remember(context, backend) { ViraSettingsBackend(context, backend) }
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

    companion object {
        private const val FOCUS_MODES = "focusModes"

        /// La pila com a noms, que és el que sap desar el sistema (i sobreviu si es recrea el procés).
        val stackSaver = Saver<List<SettingsPage>, ArrayList<String>>(
            save = { stack ->
                ArrayList(stack.map { page -> if (page is Category) page.category.name else FOCUS_MODES })
            },
            restore = { names ->
                names.mapNotNull { name ->
                    if (name == FOCUS_MODES) FocusModes
                    else SettingsCategory.entries.firstOrNull { it.name == name }?.let(::Category)
                }
            },
        )
    }
}

@Composable
fun SettingsNavigation(
    onClose: () -> Unit,
    onOpenParentPanel: (() -> Unit)? = null,
    onEditFocusProfile: ((FocusProfile?) -> Unit)? = null,
    initialStack: List<SettingsPage> = emptyList(),
) {
    // Desable: obrir l'editor d'un mode treu Ajustos de la composició, i en tornar s'ha de ser on s'era.
    var stack by rememberSaveable(stateSaver = SettingsPage.stackSaver) { mutableStateOf(initialStack) }
    fun push(page: SettingsPage) { stack = stack + page }
    fun pop() { stack = stack.dropLast(1) }
    BackHandler { if (stack.isEmpty()) onClose() else pop() }

    when (val page = stack.lastOrNull()) {
        null -> SettingsHomePage(
            onClose,
            onOpen = { push(SettingsPage.Category(it)) },
            onOpenParentPanel = onOpenParentPanel,
            onOpenFocusModes = { push(SettingsPage.FocusModes) },
        )
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

/// La portada, en una sola llista (Arnau, 2026-11-24: «la gent no troba les coses»): el compte,
/// «Connecta Vira amb el teu mòbil», com es veu i com sona, avisos, què surt a Vira, privacitat i
/// família, les teves dades, la sessió i, al final, Sobre Vira. Cap fitxa: tot a la vista baixant.
@Composable
private fun SettingsHomePage(
    onClose: () -> Unit,
    onOpen: (SettingsCategory) -> Unit,
    onOpenParentPanel: (() -> Unit)?,
    onOpenFocusModes: () -> Unit,
) {
    SettingsPageScaffold(ViraArea.Today, onDone = onClose) {
        ViraPageTitle(stringResource(R.string.settings_title), ViraArea.Today)
        SettingsAccountCard(onOpenAccount = { onOpen(SettingsCategory.ACCOUNT) })
        SettingsConnectSection()
        SettingsLookSection()
        SettingsNotificationsSection()
        SettingsModulesSection(onOpenFocusModes)
        SettingsPrivacySection(onOpenParentPanel = { onOpenParentPanel?.invoke() })
        // Esborrar-ho tot o tancar la sessió tanca Ajustos sencer.
        SettingsDataSection(onErased = onClose)
        SettingsSessionSection(onFinished = onClose)
        SettingsViraSection(onOpenAbout = { onOpen(SettingsCategory.ABOUT) })
    }
}

/// Vira: Sobre Vira, l'última fila de la llista.
@Composable
private fun SettingsViraSection(onOpenAbout: () -> Unit) {
    val title = stringResource(SettingsCategory.ABOUT.title)
    val detail = stringResource(SettingsCategory.ABOUT.detail)
    SettingsGroup(header = stringResource(R.string.settings_vira_section)) {
        row {
            SettingsNavigationRow(onOpenAbout) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
                    Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        SettingsGlyph(SettingsGlyphKind.Info, ViraColor.accent(ViraArea.Today), extent = 24.dp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
                        Text(title, style = ViraFont.body, color = ViraColor.textPrimary)
                        Text(detail, style = ViraFont.footnote, color = ViraColor.textSecondary)
                    }
                }
            }
        }
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
        // Avisos no en porta: amb un sol interruptor, una barra de «quants n'hi ha d'encesos» no
        // diria res (Arnau, 2026-10-23).
        if (category != SettingsCategory.NOTIFICATIONS && category != SettingsCategory.ABOUT) {
            SettingsCategoryHero(category, tally)
        }
        when (category) {
            // Qui ets a dalt, el permís del tutor i què es copia. El panell de pares i la sessió
            // són a la portada (Arnau, 2026-11-24).
            SettingsCategory.ACCOUNT -> {
                SettingsGuardianSection()
                SettingsSyncSection()
            }
            SettingsCategory.NOTIFICATIONS -> SettingsNotificationsSection()
            // L'aparença és a la portada, a la targeta del compte. El so i la vibració hi han
            // arribat des de «Com et vull motivar» (Arnau, 2026-10-23).
            SettingsCategory.APP -> {
                SettingsModulesSection()
                SettingsSoundsSection()
                SettingsFocusModesSection(onOpenFocusModes)
            }
            SettingsCategory.PRIVACY -> {
                SettingsPrivacySection()
                SettingsDataSection(onErased)
            }
            SettingsCategory.ABOUT -> SettingsAboutSection(onReplayTour = onErased)
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
